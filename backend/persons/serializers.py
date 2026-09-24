from django.utils import timezone
from rest_framework import serializers

from persons.models import Paciente, Persona

from django.db import transaction


class PersonaSerializer(serializers.ModelSerializer):
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
        ]
        read_only_fields = ["id"]

    def validate_fecha_nacimiento(self, value):
        if value > timezone.localdate():
            raise serializers.ValidationError(
                "La fecha de nacimiento no puede ser futura."
            )
        return value


class PacienteSerializer(serializers.ModelSerializer):
    persona = PersonaSerializer()

    class Meta:
        model = Paciente
        fields = ["persona"]

    @transaction.atomic
    def create(self, validated_data):
        persona_data = validated_data.pop("persona")
        persona = Persona.objects.create(**persona_data)
        return Paciente.objects.create(
            persona=persona,
            **validated_data,
        )

    def to_representation(self, instance):
        return {
            "persona": PersonaSerializer(instance.persona).data,
        }