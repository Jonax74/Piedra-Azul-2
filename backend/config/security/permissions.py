from rest_framework.permissions import BasePermission


class IsAgendadorOrAdmin(BasePermission):
    message = "No tienes permisos para consultar la agenda."

    def has_permission(self, request, view):
        roles = getattr(request.user, "roles", [])

        return bool(
            {"AGENDADOR", "ADMIN"}.intersection(roles)
        )

class IsAdminRole(BasePermission):
    message = "Solo un administrador puede realizar esta operación."

    def has_permission(self, request, view):
        roles = getattr(request.user, "roles", [])
        return "ADMIN" in roles    