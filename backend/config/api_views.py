from django.db import IntegrityError
from django.db.utils import OperationalError
import logging
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from users.registration import (
    KeycloakAdminError,
    RegistrationConflict,
    registrar_paciente,
)
from users.serializers import RegistroPacienteSerializer
from users.services import sincronizar_usuario_keycloak

logger = logging.getLogger(__name__)


class RegistroPacienteView(APIView):
    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        request=RegistroPacienteSerializer,
        responses={201: OpenApiTypes.OBJECT},
        auth=[],
    )
    def post(self, request):
        serializer = RegistroPacienteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            usuario = registrar_paciente(**serializer.validated_data)
        except RegistrationConflict as error:
            return Response(
                {"detail": str(error)},
                status=status.HTTP_409_CONFLICT,
            )
        except KeycloakAdminError as error:
            return Response(
                {"detail": str(error)},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )
        except IntegrityError:
            return Response(
                {"detail": "El usuario o documento ya está registrado."},
                status=status.HTTP_409_CONFLICT,
            )
        except OperationalError:
            logger.exception("La base de datos no pudo guardar el registro de paciente.")
            return Response(
                {
                    "detail": (
                        "No fue posible guardar el registro en la base de datos. "
                        "Verifica que PostgreSQL esté activo e inténtalo nuevamente."
                    )
                },
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        return Response(
            {
                "detail": "Tu cuenta de paciente quedó creada. Ya puedes iniciar sesión.",
                "username": usuario.username,
            },
            status=status.HTTP_201_CREATED,
        )


class MeView(APIView):
    @extend_schema(responses=OpenApiTypes.OBJECT)
    def get(self, request):
        return Response(
            {
                "user_id": request.user.user_id,
                "username": request.user.username,
                "roles": request.user.roles,
            }
        )

    

class ProfileView(APIView):
    @extend_schema(responses=OpenApiTypes.OBJECT)
    def get(self, request):
        keycloak_user = request.user

        username = keycloak_user.username or "usuario"
        user_id = keycloak_user.user_id
        roles = keycloak_user.roles

        usuario = sincronizar_usuario_keycloak(
            keycloak_user_id=user_id,
            username=username,
            roles=roles,
        )

        return Response(
            {
                "id": usuario.id,
                "username": usuario.username,
                "keycloak_user_id": usuario.keycloak_user_id,
                "estado": usuario.estado,
                "persona_id": usuario.persona_id,
                "roles": list(
                    usuario.relaciones_rol.values_list(
                        "rol__nombre",
                        flat=True,
                    ),
                ),
            },
        )    