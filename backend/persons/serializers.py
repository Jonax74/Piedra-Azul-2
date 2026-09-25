from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from persons.models import (
    Especialidad,
    Medico,
    Paciente,
    Persona,
)
from users.models import Usuario

from persons.models import Disponibilidad, MedicoDisponibilidad


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
        if usuario is not None and not usuario.relaciones_rol.filter(
            rol__nombre="PACIENTE",
        ).exists():
            raise serializers.ValidationError(
                "El usuario vinculado debe tener el rol PACIENTE."
            )
        current_persona_id = (
            self.instance.persona_id
            if self.instance is not None
            else None
        )
        if (
            usuario is not None
            and usuario.persona_id is not None
            and usuario.persona_id != current_persona_id
        ):
            raise serializers.ValidationError(
                "El usuario ya está vinculado a otra persona."
            )

        return usuario

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
            actual = getattr(instance.persona, "usuario", None)
            if actual is not None and actual != usuario:
                actual.persona = None
                actual.save(update_fields=["persona"])
            if usuario is not None:
                usuario.persona = instance.persona
                usuario.save(update_fields=["persona"])

        return instance


class EspecialidadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Especialidad
        fields = ["id", "nombre"]
        read_only_fields = ["id"]


class MedicoSerializer(serializers.ModelSerializer):
    persona = serializers.PrimaryKeyRelatedField(
        queryset=Persona.objects.all(),
    )

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
            "tipo_profesional",
            "estado",
            "especialidades",
            "especialidad_ids",
            "usuario_id",
        ]

    def validate_usuario_id(self, usuario):
        if usuario is not None and not usuario.relaciones_rol.filter(
            rol__nombre="MEDICO",
        ).exists():
            raise serializers.ValidationError(
                "El usuario vinculado debe tener el rol MEDICO."
            )
        current_persona_id = (
            self.instance.persona_id
            if self.instance is not None
            else None
        )
        if (
            usuario is not None
            and usuario.persona_id is not None
            and usuario.persona_id != current_persona_id
        ):
            raise serializers.ValidationError(
                "El usuario ya está vinculado a otra persona."
            )

        return usuario

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
            actual = getattr(instance.persona, "usuario", None)
            if actual is not None and actual != usuario:
                actual.persona = None
                actual.save(update_fields=["persona"])
            if usuario is not None:
                usuario.persona = instance.persona
                usuario.save(update_fields=["persona"])

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