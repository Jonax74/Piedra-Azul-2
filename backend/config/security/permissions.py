from rest_framework.permissions import BasePermission


class IsAgendadorOrAdmin(BasePermission):
    message = "No tienes permisos para consultar la agenda."

    def has_permission(self, request, view):
        roles = getattr(request.user, "roles", [])

        return bool(
            {"AGENDADOR", "ADMIN"}.intersection(roles)
        )


class IsAgendadorDoctorOrAdmin(BasePermission):
    message = "No tienes permisos para consultar el historial del paciente."

    def has_permission(self, request, view):
        roles = getattr(request.user, "roles", [])
        return bool({"AGENDADOR", "MEDICO", "ADMIN"}.intersection(roles))


class IsDoctorRole(BasePermission):
    message = "Solo un médico puede consultar su propia agenda."

    def has_permission(self, request, view):
        return "MEDICO" in getattr(request.user, "roles", [])


class IsAdminRole(BasePermission):
    message = "Solo un administrador puede realizar esta operación."

    def has_permission(self, request, view):
        roles = getattr(request.user, "roles", [])
        return "ADMIN" in roles    