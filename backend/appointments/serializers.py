from django.utils import timezone
from rest_framework import serializers

from appointments.models import Cita
from persons.models import Medico, Paciente
from users.models import Usuario
from persons.models import Disponibilidad, MedicoDisponibilidad


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
        fecha_hora = timezone.localtime(attrs["fecha_hora"])

        dia_semana = [
            "LUNES",
            "MARTES",
            "MIERCOLES",
            "JUEVES",
            "VIERNES",
            "SABADO",
            "DOMINGO",
        ][fecha_hora.weekday()]

        tiene_disponibilidad = MedicoDisponibilidad.objects.filter(
            medico=medico,
            disponibilidad__dia_semana=dia_semana,
            disponibilidad__hora_inicio__lte=fecha_hora.time(),
            disponibilidad__hora_fin__gte=fecha_hora.time(),
        ).exists()

        if not tiene_disponibilidad:
            raise serializers.ValidationError(
                "El médico no tiene disponibilidad en ese horario."
            )

        if Cita.objects.filter(
            medico=medico,
            fecha_hora=fecha_hora,
            estado__in=["PROGRAMADA", "CONFIRMADA"],
        ).exists():
            raise serializers.ValidationError(
                "El médico ya tiene una cita en ese horario."
            )

        return attrs    