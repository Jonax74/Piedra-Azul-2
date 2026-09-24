from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models

from persons.models import Medico, Paciente
from users.models import Usuario


class Cita(models.Model):
    ESTADO_CHOICES = [
        ("PROGRAMADA", "Programada"),
        ("CONFIRMADA", "Confirmada"),
        ("CANCELADA", "Cancelada"),
        ("ATENDIDA", "Atendida"),
        ("NO_ASISTIDA", "No asistida"),
    ]

    id = models.AutoField(
        primary_key=True,
        db_column="cita_id",
    )
    usuario = models.ForeignKey(
        Usuario,
        db_column="usu_id",
        on_delete=models.PROTECT,
        related_name="citas_creadas",
    )
    paciente = models.ForeignKey(
        Paciente,
        db_column="paciente_id",
        on_delete=models.PROTECT,
        related_name="citas",
    )
    medico = models.ForeignKey(
        Medico,
        db_column="medico_id",
        on_delete=models.PROTECT,
        related_name="citas",
    )
    fecha_hora = models.DateTimeField(
        db_column="fecha_hora_cita",
    )
    estado = models.CharField(
        max_length=30,
        choices=ESTADO_CHOICES,
        default="PROGRAMADA",
        db_column="cita_estado",
    )
    observacion = models.CharField(
        max_length=500,
        blank=True,
        null=True,
        db_column="observacion",
    )

    class Meta:
        db_table = "cita"
        constraints = [
            models.UniqueConstraint(
                fields=["medico", "fecha_hora"],
                name="uq_cita_medico_fecha_hora",
            ),
        ]
        indexes = [
            models.Index(
                fields=["paciente"],
                name="idx_cita_paciente",
            ),
            models.Index(
                fields=["fecha_hora"],
                name="idx_cita_fecha",
            ),
        ]

    def __str__(self):
        return f"Cita {self.id} - {self.fecha_hora}"


class HistoriaClinica(models.Model):
    id = models.AutoField(
        primary_key=True,
        db_column="hc_id",
    )
    cita = models.OneToOneField(
        Cita,
        db_column="cita_id",
        on_delete=models.PROTECT,
        related_name="historia_clinica",
    )
    fecha_hora = models.DateTimeField(
        db_column="hc_fecha_hora",
    )
    descripcion = models.CharField(
        max_length=500,
        db_column="hc_descripcion",
    )

    class Meta:
        db_table = "historiaclinica"

    def __str__(self):
        return f"Historia clínica {self.id}"


class Festivo(models.Model):
    id = models.AutoField(
        primary_key=True,
        db_column="fes_id",
    )
    fecha = models.DateField(
        unique=True,
        db_column="fes_fecha",
    )
    descripcion = models.CharField(
        max_length=100,
        blank=True,
        null=True,
        db_column="fes_descripcion",
    )

    class Meta:
        db_table = "festivo"

    def __str__(self):
        return str(self.fecha)

class ConfiguracionSistema(models.Model):
    semanas_agendamiento = models.PositiveIntegerField(
        default=4,
    )
    activo = models.BooleanField(
        default=True,
    )
    actualizado_en = models.DateTimeField(
        auto_now=True,
    )

    class Meta:
        db_table = "configuracionsistema"

    def __str__(self):
        return (
            f"Ventana de agendamiento: "
            f"{self.semanas_agendamiento} semanas"
        )    