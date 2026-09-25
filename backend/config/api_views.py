from rest_framework.response import Response
from rest_framework.views import APIView

from users.services import sincronizar_usuario_keycloak


class MeView(APIView):
    def get(self, request):
        return Response(
            {
                "user_id": request.user.user_id,
                "username": request.user.username,
                "roles": request.user.roles,
            }
        )

    

class ProfileView(APIView):
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
                "roles": list(
                    usuario.relaciones_rol.values_list(
                        "rol__nombre",
                        flat=True,
                    ),
                ),
            },
        )    