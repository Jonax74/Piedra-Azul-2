import json
import logging
import os
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from django.db import IntegrityError, transaction

from persons.models import Paciente, Persona
from users.models import Rol, Usuario, UsuarioRol

logger = logging.getLogger(__name__)


class KeycloakAdminError(Exception):
    def __init__(self, message="No fue posible conectar con Keycloak."):
        super().__init__(message)


class RegistrationConflict(Exception):
    pass


class KeycloakRequestError(Exception):
    def __init__(self, status_code, detail=""):
        self.status_code = status_code
        self.detail = detail
        super().__init__(f"Keycloak respondió con HTTP {status_code}.")


def _keycloak_url():
    port = os.getenv("KEYCLOAK_PORT", "8080")
    return os.getenv("KEYCLOAK_URL", f"http://localhost:{port}").rstrip("/")


def _request(method, path, *, token=None, payload=None, form=None):
    headers = {"Accept": "application/json"}
    data = None

    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode("utf-8")
    elif form is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        data = urlencode(form).encode("utf-8")

    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = Request(
        f"{_keycloak_url()}{path}",
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with urlopen(request, timeout=10) as response:
            body = response.read()
            return (
                response.status,
                json.loads(body) if body else None,
                response.headers,
            )
    except HTTPError as error:
        body = error.read()
        detail = ""
        if body:
            try:
                response_data = json.loads(body)
            except json.JSONDecodeError:
                detail = body.decode("utf-8", errors="replace")
            else:
                if isinstance(response_data, dict):
                    detail = " ".join(
                        str(response_data.get(key, ""))
                        for key in ("error", "errorMessage", "detail")
                    )
        raise KeycloakRequestError(error.code, detail) from error
    except URLError as error:
        if isinstance(error.reason, TimeoutError):
            raise KeycloakAdminError(
                "Keycloak tardó demasiado en responder. Inténtalo de nuevo."
            ) from error
        raise KeycloakAdminError() from error
    except TimeoutError as error:
        raise KeycloakAdminError(
            "Keycloak tardó demasiado en responder. Inténtalo de nuevo."
        ) from error


def _admin_token():
    username = os.getenv("KEYCLOAK_ADMIN")
    password = os.getenv("KEYCLOAK_ADMIN_PASSWORD")
    if not username or not password:
        raise KeycloakAdminError(
            "Falta configurar KEYCLOAK_ADMIN y KEYCLOAK_ADMIN_PASSWORD en el entorno del backend."
        )

    try:
        _, result, _ = _request(
            "POST",
            "/realms/master/protocol/openid-connect/token",
            form={
                "username": username,
                "password": password,
                "grant_type": "password",
                "client_id": "admin-cli",
            },
        )
    except KeycloakRequestError as error:
        raise KeycloakAdminError(
            "No fue posible autenticar el servicio de registro con Keycloak."
        ) from error

    if not isinstance(result, dict) or not result.get("access_token"):
        raise KeycloakAdminError(
            "Keycloak no devolvió un token para el servicio de registro."
        )

    return result["access_token"]


def _patient_role(token):
    role_path = "/admin/realms/piedrazul/roles/PACIENTE"
    try:
        _, role, _ = _request("GET", role_path, token=token)
        return role
    except KeycloakRequestError as error:
        if error.status_code != 404:
            raise KeycloakAdminError(
                "No fue posible consultar el rol de paciente en Keycloak."
            ) from error

    try:
        _request(
            "POST",
            "/admin/realms/piedrazul/roles",
            token=token,
            payload={"name": "PACIENTE"},
        )
        _, role, _ = _request("GET", role_path, token=token)
        return role
    except KeycloakRequestError as error:
        raise KeycloakAdminError(
            "No fue posible preparar el rol de paciente en Keycloak."
        ) from error


def _delete_keycloak_user(token, user_id):
    try:
        _request(
            "DELETE",
            f"/admin/realms/piedrazul/users/{user_id}",
            token=token,
        )
    except KeycloakRequestError as error:
        if error.status_code != 404:
            raise KeycloakAdminError(
                "No fue posible revertir la cuenta creada en Keycloak."
            ) from error


def _create_keycloak_user(*, username, persona, password):
    token = _admin_token()
    role = _patient_role(token)
    first_name = " ".join(
        part
        for part in (persona["primer_nombre"], persona.get("segundo_nombre"))
        if part
    )
    last_name = " ".join(
        part
        for part in (persona["primer_apellido"], persona.get("segundo_apellido"))
        if part
    )

    try:
        _, _, headers = _request(
            "POST",
            "/admin/realms/piedrazul/users",
            token=token,
            payload={
                "username": username,
                "email": persona["correo"],
                "firstName": first_name,
                "lastName": last_name,
                "enabled": True,
                "emailVerified": False,
                "credentials": [
                    {
                        "type": "password",
                        "value": password,
                        "temporary": False,
                    }
                ],
            },
        )
    except KeycloakRequestError as error:
        if error.status_code == 409:
            raise RegistrationConflict(
                "El nombre de usuario o el correo ya están registrados."
            ) from error
        if error.status_code == 400 and any(
            term in error.detail.lower()
            for term in ("password", "credential", "contraseña")
        ):
            raise KeycloakAdminError(
                "Keycloak rechazó la contraseña. Debe cumplir la política de seguridad y tener al menos 8 caracteres."
            ) from error
        raise KeycloakAdminError(
            "Keycloak rechazó los datos de la cuenta. Verifica el usuario, el correo y que la contraseña cumpla al menos 8 caracteres."
        ) from error

    location = headers.get("Location")
    user_id = location.rstrip("/").rsplit("/", 1)[-1] if location else ""
    if not user_id:
        raise KeycloakAdminError(
            "Keycloak creó la cuenta, pero no devolvió su identificador."
        )

    try:
        _request(
            "POST",
            f"/admin/realms/piedrazul/users/{user_id}/role-mappings/realm",
            token=token,
            payload=[role],
        )
    except KeycloakRequestError as error:
        try:
            _delete_keycloak_user(token, user_id)
        except KeycloakAdminError:
            logger.exception(
                "No se pudo limpiar un usuario de Keycloak sin el rol PACIENTE."
            )
        raise KeycloakAdminError(
            "La cuenta se creó, pero no fue posible asignarle el rol de paciente."
        ) from error

    return user_id, token


def registrar_paciente(*, username, password, persona):
    keycloak_user_id, token = _create_keycloak_user(
        username=username,
        persona=persona,
        password=password,
    )

    try:
        with transaction.atomic():
            persona_creada = Persona.objects.create(**persona)
            Paciente.objects.create(persona=persona_creada)
            usuario = Usuario.objects.create(
                persona=persona_creada,
                username=username,
                keycloak_user_id=keycloak_user_id,
            )
            rol, _ = Rol.objects.get_or_create(nombre="PACIENTE")
            UsuarioRol.objects.create(usuario=usuario, rol=rol)
            return usuario
    except IntegrityError as error:
        try:
            _delete_keycloak_user(token, keycloak_user_id)
        except KeycloakAdminError:
            logger.exception(
                "No se pudo limpiar una cuenta de Keycloak tras conflicto de base de datos."
            )
        raise RegistrationConflict(
            "El usuario o documento ya está registrado."
        ) from error
    except Exception:
        try:
            _delete_keycloak_user(token, keycloak_user_id)
        except KeycloakAdminError:
            logger.exception(
                "No se pudo limpiar una cuenta de Keycloak tras fallar el registro local."
            )
        raise
