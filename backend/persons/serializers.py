from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from persons.models import (
    Especialidad,
    Medico,
    Paciente,
    Persona,
    Disponibilidad,
    MedicoDisponibilidad,
)
from users.models import Usuario
from users.registration import registrar_medico


def _validar_usuario_vinculable(usuario, rol_requerido, persona_actual_id):
    if usuario is not None and not usuario.relaciones_rol.filter(
        rol__nombre=rol_requerido,
    ).exists():
        raise serializers.ValidationError(
            f"El usuario vinculado debe tener el rol {rol_requerido}."
        )

    if (
        usuario is not None
        and usuario.persona_id is not None
        and usuario.persona_id != persona_actual_id
    ):
        raise serializers.ValidationError(
            "El usuario ya está vinculado a otra persona."
        )

    return usuario


def _actualizar_vinculo_usuario(usuario, persona):
    actual = getattr(persona, "usuario", None)
    if actual is not None and actual != usuario:
        actual.persona = None
        actual.save(update_fields=["persona"])

    if usuario is not None:
        usuario.persona = persona
        usuario.save(update_fields=["persona"])


class PersonaSerializer(serializers.ModelSerializer):
    usuario = serializers.SerializerMethodField()

    class Meta:
        model = Persona
        fields = [
            "id",
            "primer_nombre",
            "segundo_nombre",
            "primer_apellido",
            "segundo_apellido",
            "genero",
            "fecha_nacimiento",
            "telefono",
            "dni",
            "correo",
            "usuario",
        ]
        read_only_fields = ["id"]

    def get_usuario(self, persona):
        usuario = getattr(persona, "usuario", None)
        if usuario is None:
            return None

        return {
            "id": usuario.id,
            "username": usuario.username,
            "roles": list(
                usuario.relaciones_rol.values_list(
                    "rol__nombre",
                    flat=True,
                ),
            ),
        }

    def validate_fecha_nacimiento(self, value):
        if value > timezone.localdate():
            raise serializers.ValidationError(
                "La fecha de nacimiento no puede ser futura."
            )

        return value


class PacienteSerializer(serializers.ModelSerializer):
    persona = PersonaSerializer()
    usuario_id = serializers.PrimaryKeyRelatedField(
        source="_usuario",
        queryset=Usuario.objects.all(),
        write_only=True,
        required=False,
        allow_null=True,
    )

    class Meta:
        model = Paciente
        fields = ["persona", "usuario_id"]

    def validate_usuario_id(self, usuario):
        current_persona_id = (
            self.instance.persona_id
            if self.instance is not None
            else None
        )

        return _validar_usuario_vinculable(
            usuario=usuario,
            rol_requerido="PACIENTE",
            persona_actual_id=current_persona_id,
        )

    @transaction.atomic
    def create(self, validated_data):
        persona_data = validated_data.pop("persona")
        usuario = validated_data.pop("_usuario", None)
        persona = Persona.objects.create(**persona_data)

        if usuario is not None:
            usuario.persona = persona
            usuario.save(update_fields=["persona"])

        return Paciente.objects.create(
            persona=persona,
            **validated_data,
        )

    def to_representation(self, instance):
        return {
            "persona": PersonaSerializer(instance.persona).data,
        }

    @transaction.atomic
    def update(self, instance, validated_data):
        usuario = validated_data.pop("_usuario", serializers.empty)
        persona_data = validated_data.pop("persona", None)

        if persona_data:
            persona = instance.persona
            for field, value in persona_data.items():
                setattr(persona, field, value)
            persona.save()

        if usuario is not serializers.empty:
            _actualizar_vinculo_usuario(
                usuario=usuario,
                persona=instance.persona,
            )

        return instance


class EspecialidadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Especialidad
        fields = ["id", "nombre"]
        read_only_fields = ["id"]


class MedicoSerializer(serializers.ModelSerializer):
    username = serializers.CharField(
        max_length=20,
        required=False,
        write_only=True,
    )
    password = serializers.CharField(
        min_length=8,
        required=False,
        write_only=True,
        trim_whitespace=False,
        error_messages={
            "min_length": "La contraseña debe tener al menos 8 caracteres.",
        },
    )
    password_confirmation = serializers.CharField(
        min_length=8,
        required=False,
        write_only=True,
        trim_whitespace=False,
        error_messages={
            "min_length": "La contraseña debe tener al menos 8 caracteres.",
        },
    )
    persona = serializers.PrimaryKeyRelatedField(
        queryset=Persona.objects.all(),
        required=False,
    )
    persona_data = PersonaSerializer(write_only=True, required=False)

    especialidades = serializers.SerializerMethodField()

    especialidad_ids = serializers.PrimaryKeyRelatedField(
        many=True,
        queryset=Especialidad.objects.all(),
        write_only=True,
        required=False,
    )

    usuario_id = serializers.PrimaryKeyRelatedField(
        source="_usuario",
        queryset=Usuario.objects.all(),
        write_only=True,
        required=False,
        allow_null=True,
    )

    class Meta:
        model = Medico
        fields = [
            "persona",
            "persona_data",
            "tipo_profesional",
            "estado",
            "especialidades",
            "especialidad_ids",
            "usuario_id",
            "username",
            "password",
            "password_confirmation",
        ]

    def validate(self, attrs):
        persona_data = attrs.get("persona_data")
        persona = attrs.get("persona")
        username = attrs.get("username")
        password = attrs.get("password")
        password_confirmation = attrs.get("password_confirmation")
        usuario = attrs.get("_usuario")

        if persona_data is not None and persona is not None:
            raise serializers.ValidationError(
                "Envía una persona existente o los datos de una nueva, no ambos."
            )

        if self.instance is None and persona_data is None and persona is None:
            raise serializers.ValidationError(
                {"persona_data": "Debes enviar los datos del profesional."}
            )

        if any(
            value is not None
            for value in (username, password, password_confirmation)
        ):
            account_errors = {}
            for field, value in (
                ("username", username),
                ("password", password),
                ("password_confirmation", password_confirmation),
            ):
                if value is None:
                    account_errors[field] = "Este campo es obligatorio para crear la cuenta."

            if persona_data is None:
                account_errors["persona_data"] = (
                    "Debes registrar una nueva persona para crear su cuenta."
                )
            elif not persona_data.get("correo"):
                account_errors["persona_data"] = {
                    "correo": "El correo electrónico es obligatorio para crear la cuenta."
                }
            if usuario is not None:
                account_errors["usuario_id"] = (
                    "No puedes vincular una cuenta existente y crear otra a la vez."
                )
            if (
                password is not None
                and password_confirmation is not None
                and password != password_confirmation
            ):
                account_errors["password_confirmation"] = (
                    "Las contraseñas no coinciden."
                )
            if username and Usuario.objects.filter(
                username__iexact=username,
            ).exists():
                account_errors["username"] = (
                    "Este nombre de usuario ya está registrado."
                )
            if account_errors:
                raise serializers.ValidationError(account_errors)

        return attrs

    def validate_usuario_id(self, usuario):
        current_persona_id = (
            self.instance.persona_id
            if self.instance is not None
            else None
        )

        return _validar_usuario_vinculable(
            usuario=usuario,
            rol_requerido="MEDICO",
            persona_actual_id=current_persona_id,
        )

    def get_especialidades(self, medico):
        especialidades = Especialidad.objects.filter(
            medicos__medico=medico,
        )

        return EspecialidadSerializer(
            especialidades,
            many=True,
        ).data

    @transaction.atomic
    def create(self, validated_data):
        persona_data = validated_data.pop("persona_data", None)
        username = validated_data.pop("username", None)
        password = validated_data.pop("password", None)
        validated_data.pop("password_confirmation", None)
        if username is not None:
            return registrar_medico(
                username=username,
                password=password,
                persona=persona_data,
                tipo_profesional=validated_data["tipo_profesional"],
                estado=validated_data["estado"],
                especialidades=validated_data.pop("especialidad_ids", []),
            )

        if persona_data is not None:
            validated_data["persona"] = Persona.objects.create(**persona_data)

        especialidades = validated_data.pop(
            "especialidad_ids",
            [],
        )
        usuario = validated_data.pop("_usuario", None)

        medico = Medico.objects.create(**validated_data)

        if usuario is not None:
            usuario.persona = medico.persona
            usuario.save(update_fields=["persona"])

        for especialidad in especialidades:
            medico.especialidades.create(
                especialidad=especialidad,
            )

        return medico

    @transaction.atomic
    def update(self, instance, validated_data):
        especialidades = validated_data.pop(
            "especialidad_ids",
            None,
        )
        usuario = validated_data.pop("_usuario", serializers.empty)

        for field, value in validated_data.items():
            setattr(instance, field, value)

        instance.save()

        if usuario is not serializers.empty:
            _actualizar_vinculo_usuario(
                usuario=usuario,
                persona=instance.persona,
            )

        if especialidades is not None:
            instance.especialidades.all().delete()

            for especialidad in especialidades:
                instance.especialidades.create(
                    especialidad=especialidad,
                )

        return instance

class DisponibilidadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Disponibilidad
        fields = [
            "id",
            "dia_semana",
            "hora_inicio",
            "hora_fin",
            "intervalo",
        ]
        read_only_fields = ["id"]

    def validate(self, attrs):
        if attrs["hora_inicio"] >= attrs["hora_fin"]:
            raise serializers.ValidationError(
                "La hora de inicio debe ser menor que la hora de fin."
            )

        if attrs["intervalo"] <= 0:
            raise serializers.ValidationError(
                "El intervalo debe ser mayor que cero."
            )

        return attrs


class MedicoDisponibilidadSerializer(serializers.ModelSerializer):
    class Meta:
        model = MedicoDisponibilidad
        fields = [
            "medico",
            "disponibilidad",
        ]    