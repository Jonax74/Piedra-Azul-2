from django.core.validators import MinValueValidator
from django.db import models


class Persona(models.Model):
    GENERO_CHOICES = [
        ("HOMBRE", "Hombre"),
        ("MUJER", "Mujer"),
        ("OTRO", "Otro"),
    ]

    id = models.AutoField(primary_key=True, db_column="per_id")
    primer_nombre = models.CharField(
        max_length=50,
        db_column="per_primer_nombre",
    )
    segundo_nombre = models.CharField(
        max_length=50,
        blank=True,
        null=True,
        db_column="per_segundo_nombre",
    )
    primer_apellido = models.CharField(
        max_length=50,
        db_column="per_primer_apellido",
    )
    segundo_apellido = models.CharField(
        max_length=50,
        blank=True,
        null=True,
        db_column="per_segundo_apellido",
    )
    genero = models.CharField(
        max_length=10,
        choices=GENERO_CHOICES,
        db_column="per_genero",
    )
    fecha_nacimiento = models.DateField(db_column="per_fecha_nac")
    telefono = models.CharField(max_length=15, db_column="per_telefono")
    dni = models.IntegerField(unique=True, db_column="per_dni")
    correo = models.EmailField(
        max_length=70,
        blank=True,
        null=True,
        db_column="per_correo",
    )

    class Meta:
        db_table = "persona"

    def __str__(self):
        return f"{self.primer_nombre} {self.primer_apellido}"


class Paciente(models.Model):
    persona = models.OneToOneField(
        Persona,
        primary_key=True,
        db_column="per_id",
        on_delete=models.CASCADE,
        related_name="paciente",
    )

    class Meta:
        db_table = "paciente"

    def __str__(self):
        return str(self.persona)


class Medico(models.Model):
    TIPO_PROFESIONAL_CHOICES = [
        ("MEDICO", "Médico"),
        ("TERAPISTA", "Terapista"),
    ]

    ESTADO_CHOICES = [
        ("ACTIVO", "Activo"),
        ("INACTIVO", "Inactivo"),
    ]

    persona = models.OneToOneField(
        Persona,
        primary_key=True,
        db_column="per_id",
        on_delete=models.CASCADE,
        related_name="medico",
    )
    tipo_profesional = models.CharField(
        max_length=20,
        choices=TIPO_PROFESIONAL_CHOICES,
        db_column="med_tipo_profesional",
    )
    estado = models.CharField(
        max_length=20,
        choices=ESTADO_CHOICES,
        default="ACTIVO",
        db_column="med_estado",
    )

    class Meta:
        db_table = "medico"

    def __str__(self):
        return f"{self.persona} - {self.tipo_profesional}"


class Especialidad(models.Model):
    id = models.AutoField(primary_key=True, db_column="esp_id")
    nombre = models.CharField(
        max_length=50,
        unique=True,
        db_column="esp_nombre",
    )

    class Meta:
        db_table = "especialidad"

    def __str__(self):
        return self.nombre


class Disponibilidad(models.Model):
    DIAS_CHOICES = [
        ("LUNES", "Lunes"),
        ("MARTES", "Martes"),
        ("MIERCOLES", "Miércoles"),
        ("JUEVES", "Jueves"),
        ("VIERNES", "Viernes"),
    ]

    id = models.AutoField(primary_key=True, db_column="disp_id")
    dia_semana = models.CharField(
        max_length=20,
        choices=DIAS_CHOICES,
        db_column="disp_dia_semana",
    )
    hora_inicio = models.TimeField(db_column="disp_hora_inicio")
    hora_fin = models.TimeField(db_column="disp_hora_fin")
    intervalo = models.PositiveIntegerField(
        validators=[MinValueValidator(1)],
        db_column="disp_intervalo",
    )

    class Meta:
        db_table = "disponibilidad"

    def __str__(self):
        return f"{self.dia_semana}: {self.hora_inicio} - {self.hora_fin}"


class MedicoDisponibilidad(models.Model):
    medico = models.ForeignKey(
        Medico,
        db_column="per_id",
        on_delete=models.CASCADE,
        related_name="disponibilidades",
    )
    disponibilidad = models.ForeignKey(
        Disponibilidad,
        db_column="disp_id",
        on_delete=models.CASCADE,
        related_name="medicos",
    )

    class Meta:
        db_table = "medicodisponibilidad"
        constraints = [
            models.UniqueConstraint(
                fields=["medico", "disponibilidad"],
                name="pk_medicodisponibilidad",
            ),
        ]


class MedicoEspecialidad(models.Model):
    medico = models.ForeignKey(
        Medico,
        db_column="per_id",
        on_delete=models.CASCADE,
        related_name="especialidades",
    )
    especialidad = models.ForeignKey(
        Especialidad,
        db_column="esp_id",
        on_delete=models.CASCADE,
        related_name="medicos",
    )

    class Meta:
        db_table = "medicoespecialidad"
        constraints = [
            models.UniqueConstraint(
                fields=["medico", "especialidad"],
                name="pk_medicoespecialidad",
            ),
        ]