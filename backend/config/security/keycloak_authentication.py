import os
from dataclasses import dataclass

import jwt
from django.core.exceptions import ImproperlyConfigured
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed


@dataclass
class KeycloakUser:
    user_id: str
    username: str | None
    roles: list[str]

    @property
    def is_authenticated(self) -> bool:
        return True

    @property
    def is_anonymous(self) -> bool:
        return False


class KeycloakJWTAuthentication(BaseAuthentication):
    def authenticate(self, request):
        authorization = request.headers.get("Authorization", "")

        if not authorization.startswith("Bearer "):
            return None

        token = authorization.removeprefix("Bearer ").strip()

        if not token:
            raise AuthenticationFailed("Token Bearer vacío.")

        issuer = os.getenv("KEYCLOAK_ISSUER")
        if not issuer:
            raise ImproperlyConfigured(
                "Falta configurar KEYCLOAK_ISSUER."
            )

        jwks_url = f"{issuer}/protocol/openid-connect/certs"

        try:
            signing_key = jwt.PyJWKClient(jwks_url).get_signing_key_from_jwt(
                token
            )

            payload = jwt.decode(
                token,
                signing_key.key,
                algorithms=["RS256"],
                issuer=issuer,
                options={
                    "verify_aud": False,
                },
            )
        except jwt.PyJWTError as error:
            raise AuthenticationFailed(
                "Token de Keycloak inválido."
            ) from error

        realm_access = payload.get("realm_access", {})
        roles = realm_access.get("roles", [])

        user = KeycloakUser(
            user_id=payload.get("sub", ""),
            username=payload.get("preferred_username"),
            roles=roles,
        )

        return user, token