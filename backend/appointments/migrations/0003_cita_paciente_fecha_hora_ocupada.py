from django.db import migrations, models
from django.db.models import Q
from django.db.models import Count


def cancelar_citas_paciente_duplicadas(apps, schema_editor):
    cita_model = apps.get_model("appointments", "Cita")
    duplicados = (
        cita_model.objects
        .filter(estado__in=["PROGRAMADA", "CONFIRMADA"])
        .values("paciente_id", "fecha_hora")
        .annotate(total=Count("id"))
        .filter(total__gt=1)
    )

    for duplicado in duplicados:
        citas = cita_model.objects.filter(
            paciente_id=duplicado["paciente_id"],
            fecha_hora=duplicado["fecha_hora"],
            estado__in=["PROGRAMADA", "CONFIRMADA"],
        ).order_by("id")
        citas.exclude(id=citas.first().id).update(estado="CANCELADA")


class Migration(migrations.Migration):

    dependencies = [
        ("appointments", "0002_configuracionsistema"),
    ]

    operations = [
        migrations.RunPython(
            cancelar_citas_paciente_duplicadas,
            migrations.RunPython.noop,
        ),
        migrations.AddConstraint(
            model_name="cita",
            constraint=models.UniqueConstraint(
                condition=Q(estado__in=["PROGRAMADA", "CONFIRMADA"]),
                fields=("paciente", "fecha_hora"),
                name="uq_cita_paciente_fecha_hora_ocupada",
            ),
        ),
    ]