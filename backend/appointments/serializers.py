from django.core.exceptions import ValidationError as DjangoValidationError
from django.utils import timezone
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import extend_schema_field
from rest_framework import serializers

from appointments.models import ESTADOS_OCUPADOS, Cita, ConfiguracionSistema
from appointments.services import validar_cita_programable
from appointments.states import obtener_estado_cita
from persons.models import Paciente
from users.models import Usuario


class CitaSerializer(serializers.ModelSerializer):
    paciente_detalle = serializers.SerializerMethodField()
    medico_detalle = serializers.SerializerMethodField()

    class Meta:
        model = Cita
        fields = [
            "id",
            "usuario",
            "paciente",
            "medico",
            "fecha_hora",
            "estado",
            "observacion",
            "paciente_detalle",
            "medico_detalle",
        ]
        read_only_fields = [
            "id",
            "usuario",
        ]

    @extend_schema_field(OpenApiTypes.OBJECT)
    def get_paciente_detalle(self, cita):
        persona = cita.paciente.persona
        return {
            "persona": {
                "id": persona.id,
                "primer_nombre": persona.primer_nombre,
                "primer_apellido": persona.primer_apellido,
            },
        }

    @extend_schema_field(OpenApiTypes.OBJECT)
    def get_medico_detalle(self, cita):
        persona = cita.medico.persona
        return {
            "persona": {
                "id": persona.id,
                "primer_nombre": persona.primer_nombre,
                "primer_apellido": persona.primer_apellido,
            },
        }

    def validate_fecha_hora(self, value):
        if value <= timezone.now():
            raise serializers.ValidationError(
                "La cita debe programarse en una fecha futura."
            )

        return value

    def validate_paciente(self, value):
        if not Paciente.objects.filter(
            persona_id=value.persona_id,
        ).exists():
            raise serializers.ValidationError(
                "El paciente no está activo o no existe."
            )

        return value

    def validate_medico(self, value):
        if value.estado != "ACTIVO":
            raise serializers.ValidationError(
                "El médico no está activo."
            )

        return value

    def _usuario_autenticado(self):
        request = self.context["request"]
        keycloak_user_id = request.user.user_id

        try:
            return Usuario.objects.get(
                keycloak_user_id=keycloak_user_id,
            )
        except Usuario.DoesNotExist as error:
            raise serializers.ValidationError(
                "El usuario autenticado no está sincronizado con Django."
            ) from error

    def _validar_alcance_del_paciente(self, paciente, usuario):
        roles = set(getattr(self.context["request"].user, "roles", []))
        roles_ampliados = {"ADMIN", "AGENDADOR", "MEDICO"}

        if roles.intersection(roles_ampliados):
            return

        if "PACIENTE" not in roles:
            raise serializers.ValidationError(
                "El usuario no tiene un rol autorizado para agendar citas."
            )

        if (
            usuario.persona_id is None
            or not Paciente.objects.filter(
                persona_id=usuario.persona_id,
            ).exists()
        ):
            raise serializers.ValidationError(
                "El paciente autenticado no está vinculado a una persona."
            )

        if paciente.persona_id != usuario.persona_id:
            raise serializers.ValidationError(
                "Un paciente solo puede agendar citas para sí mismo."
            )

    def create(self, validated_data):
        usuario = self._usuario_autenticado()
        validated_data["usuario"] = usuario

        return super().create(validated_data)

    def validate(self, attrs):
        usuario = self._usuario_autenticado()
        paciente = attrs.get(
            "paciente",
            self.instance.paciente if self.instance else None,
        )
        if paciente is not None:
            self._validar_alcance_del_paciente(
                paciente=paciente,
                usuario=usuario,
            )

        if self.instance is None and "estado" in self.initial_data:
            raise serializers.ValidationError(
                {"estado": "El estado inicial de una cita es PROGRAMADA."},
            )

        estado = attrs.get(
            "estado",
            self.instance.estado if self.instance else "PROGRAMADA",
        )
        fecha_hora = attrs.get("fecha_hora")

        if (
            self.instance is not None
            and fecha_hora is not None
            and fecha_hora != self.instance.fecha_hora
            and "estado" not in attrs
        ):
            raise serializers.ValidationError(
                {
                    "estado": (
                        "Debe indicar el estado REAGENDADA para cambiar "
                        "la fecha de una cita."
                    ),
                },
            )

        if self.instance is not None and (
            "estado" in attrs
            or (
                fecha_hora is not None
                and fecha_hora != self.instance.fecha_hora
            )
        ):
            try:
                obtener_estado_cita(
                    self.instance.estado,
                ).validar_transicion(
                    cita=self.instance,
                    nuevo_estado=estado,
                    fecha_hora=fecha_hora,
                )
            except DjangoValidationError as error:
                detail = (
                    error.message_dict
                    if hasattr(error, "message_dict")
                    else error.messages
                )
                raise serializers.ValidationError(detail) from error

        validar_disponibilidad = fecha_hora is not None
        if (
            self.instance is not None
            and estado in ESTADOS_OCUPADOS
            and self.instance.estado not in ESTADOS_OCUPADOS
        ):
            validar_disponibilidad = True

        if validar_disponibilidad:
            medico = attrs.get(
                "medico",
                self.instance.medico if self.instance else None,
            )
            fecha_hora = fecha_hora or self.instance.fecha_hora
            try:
                validar_cita_programable(
                    medico_id=medico.pk,
                    fecha_hora=fecha_hora,
                    cita_id=self.instance.pk if self.instance else None,
                )
            except DjangoValidationError as error:
                raise serializers.ValidationError(
                    error.messages,
                ) from error

        return attrs

    def update(self, instance, validated_data):
        if "estado" in validated_data:
            nuevo_estado = validated_data.pop("estado")
            nueva_fecha_hora = validated_data.pop("fecha_hora", None)
            try:
                instance.cambiar_estado(
                    nuevo_estado=nuevo_estado,
                    fecha_hora=nueva_fecha_hora,
                )
            except DjangoValidationError as error:
                detail = (
                    error.message_dict
                    if hasattr(error, "message_dict")
                    else error.messages
                )
                raise serializers.ValidationError(detail) from error

        return super().update(instance, validated_data)
    
class FranjaDisponibleSerializer(serializers.Serializer):
    fecha = serializers.DateField()
    hora = serializers.TimeField(format="%H:%M")
    fecha_hora = serializers.DateTimeField()

class ConfiguracionSistemaSerializer(
    serializers.ModelSerializer,
):
    class Meta:
        model = ConfiguracionSistema
        fields = [
            "id",
            "semanas_agendamiento",
            "activo",
            "actualizado_en",
        ]
        read_only_fields = [
            "id",
            "actualizado_en",
        ]

    def validate_semanas_agendamiento(self, value):
        if value < 1 or value > 52:
            raise serializers.ValidationError(
                "La ventana debe estar entre 1 y 52 semanas."
            )

        return value    