from django.core.management.color import no_style
from django.db import migrations


def sync_registration_sequences(apps, schema_editor):
    models = [
        apps.get_model("persons", "Persona"),
        apps.get_model("users", "Rol"),
        apps.get_model("users", "Usuario"),
        apps.get_model("users", "UsuarioRol"),
    ]
    statements = schema_editor.connection.ops.sequence_reset_sql(
        no_style(),
        models,
    )

    with schema_editor.connection.cursor() as cursor:
        for statement in statements:
            cursor.execute(statement)


class Migration(migrations.Migration):
    dependencies = [
        ("persons", "0001_initial"),
        ("users", "0002_remove_usuario_roles_usuariorol"),
    ]

    operations = [
        migrations.RunPython(
            sync_registration_sequences,
            migrations.RunPython.noop,
        ),
    ]
