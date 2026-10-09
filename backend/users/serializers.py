from rest_framework import serializers

from persons.serializers import PersonaSerializer
from users.models import Usuario


class RegistroPacienteSerializer(serializers.Serializer):
    username = serializers.CharField(max_length=20)
    password = serializers.CharField(
        min_length=8,
        write_only=True,
        trim_whitespace=False,
        error_messages={
            "min_length": "La contraseña debe tener al menos 8 caracteres.",
            "required": "La contraseña es obligatoria.",
            "blank": "La contraseña es obligatoria.",
        },
    )
    password_confirmation = serializers.CharField(
        min_length=8,
        write_only=True,
        trim_whitespace=False,
        error_messages={
            "min_length": "La contraseña debe tener al menos 8 caracteres.",
            "required": "Confirma tu contraseña.",
            "blank": "Confirma tu contraseña.",
        },
    )
    persona = PersonaSerializer()

    def validate_username(self, value):
        if Usuario.objects.filter(username__iexact=value).exists():
            raise serializers.ValidationError(
                "Este nombre de usuario ya está registrado."
            )

        return value

    def validate(self, attrs):
        if attrs["password"] != attrs["password_confirmation"]:
            raise serializers.ValidationError(
                {"password_confirmation": "Las contraseñas no coinciden."}
            )

        if not attrs["persona"].get("correo"):
            raise serializers.ValidationError(
                {"persona": {"correo": "El correo electrónico es obligatorio."}}
            )

        attrs.pop("password_confirmation")
        return attrs
