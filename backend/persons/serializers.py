from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from persons.models import (
    Especialidad,
    Medico,
    Paciente,
    Persona,
)

from persons.models import Disponibilidad, MedicoDisponibilidad


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

    class Meta:
        model = Medico
        fields = [
            "persona",
            "tipo_profesional",
            "estado",
            "especialidades",
            "especialidad_ids",
        ]

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

        medico = Medico.objects.create(**validated_data)

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

        for field, value in validated_data.items():
            setattr(instance, field, value)

        instance.save()

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