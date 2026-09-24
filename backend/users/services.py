from django.db import transaction

from users.models import Rol, Usuario, UsuarioRol


@transaction.atomic
def sincronizar_usuario_keycloak(
    *,
    keycloak_user_id: str,
    username: str,
    roles: list[str],
) -> Usuario:
    usuario, _ = Usuario.objects.update_or_create(
        keycloak_user_id=keycloak_user_id,
        defaults={
            "username": username[:20],
            "estado": "ACTIVO",
        },
    )

    roles_validos = {
        nombre
        for nombre in roles
        if nombre in {"ADMIN", "AGENDADOR", "MEDICO", "PACIENTE"}
    }

    roles_db = []

    for nombre in roles_validos:
        rol, _ = Rol.objects.get_or_create(nombre=nombre)
        roles_db.append(rol)

    UsuarioRol.objects.filter(usuario=usuario).exclude(
        rol__in=roles_db,
    ).delete()

    for rol in roles_db:
        UsuarioRol.objects.get_or_create(
            usuario=usuario,
            rol=rol,
        )

    return usuario