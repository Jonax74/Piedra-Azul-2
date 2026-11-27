from django.db import migrations


SPECIALTIES = (
    "Medicina General",
    "Fisioterapia",
    "Cardiología",
    "Kinesiología",
    "Pediatría",
    "Neurología",
    "Dermatología",
)


def create_specialties(apps, schema_editor):
    specialty_model = apps.get_model("persons", "Especialidad")
    for name in SPECIALTIES:
        specialty_model.objects.using(schema_editor.connection.alias).get_or_create(
            nombre=name,
        )


class Migration(migrations.Migration):
    dependencies = [
        ("persons", "0001_initial"),
    ]

    operations = [
        migrations.RunPython(create_specialties, migrations.RunPython.noop),
    ]
