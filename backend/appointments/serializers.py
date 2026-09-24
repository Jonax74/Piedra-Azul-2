from django.utils import timezone
from rest_framework import serializers

from appointments.models import Cita
from persons.models import Medico, Paciente
from users.models import Usuario
from persons.models import Disponibilidad, MedicoDisponibilidad

from django.core.exceptions import ValidationError as DjangoValidationError
from appointments.services import validar_cita_programable
from appointments.models import ConfiguracionSistema




class CitaSerializer(serializers.ModelSerializer):
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
        ]
        read_only_fields = [
            "id",
            "usuario",
            "estado",
        ]

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

    def create(self, validated_data):
        request = self.context["request"]
        keycloak_user_id = request.user.user_id

        try:
            usuario = Usuario.objects.get(
                keycloak_user_id=keycloak_user_id,
            )
        except Usuario.DoesNotExist as error:
            raise serializers.ValidationError(
                "El usuario autenticado no está sincronizado con Django."
            ) from error

        validated_data["usuario"] = usuario

        return super().create(validated_data)

    def validate(self, attrs):
        medico = attrs["medico"]
        fecha_hora = attrs["fecha_hora"]
    
        try:
            validar_cita_programable(
                medico_id=medico.pk,
                fecha_hora=fecha_hora,
            )
        except DjangoValidationError as error:
            raise serializers.ValidationError(
                error.messages,
            ) from error
    
        return attrs
    
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