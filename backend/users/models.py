from django.db import models

from persons.models import Persona


class Rol(models.Model):
    id = models.AutoField(
        primary_key=True,
        db_column="rol_id",
    )
    nombre = models.CharField(
        max_length=50,
        unique=True,
        db_column="rol_nombre",
    )

    class Meta:
        db_table = "rol"

    def __str__(self):
        return self.nombre


class Usuario(models.Model):
    ESTADO_CHOICES = [
        ("ACTIVO", "Activo"),
        ("INACTIVO", "Inactivo"),
    ]

    id = models.AutoField(
        primary_key=True,
        db_column="usu_id",
    )
    persona = models.OneToOneField(
        Persona,
        db_column="per_id",
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="usuario",
    )
    username = models.CharField(
        max_length=20,
        unique=True,
        db_column="username",
    )
    keycloak_user_id = models.CharField(
        max_length=100,
        unique=True,
        null=True,
        blank=True,
        db_column="keycloak_user_id",
    )
    estado = models.CharField(
        max_length=20,
        choices=ESTADO_CHOICES,
        default="ACTIVO",
        db_column="usu_estado",
    )

    class Meta:
        db_table = "usuario"

    def __str__(self):
        return self.username


class UsuarioRol(models.Model):
    usuario = models.ForeignKey(
        Usuario,
        db_column="usu_id",
        on_delete=models.CASCADE,
        related_name="relaciones_rol",
    )
    rol = models.ForeignKey(
        Rol,
        db_column="rol_id",
        on_delete=models.CASCADE,
        related_name="relaciones_usuario",
    )

    class Meta:
        db_table = "usuariorol"
        constraints = [
            models.UniqueConstraint(
                fields=["usuario", "rol"],
                name="pk_usuariorol",
            ),
        ]

    def __str__(self):
        return f"{self.usuario} - {self.rol}"