import logging

from django.db import IntegrityError
from django.db.utils import OperationalError
from rest_framework import generics
from rest_framework.permissions import IsAuthenticated
from rest_framework import status
from rest_framework.response import Response
from django.db.models import Q
from config.security.permissions import IsAdminRole

from persons.models import (
    Disponibilidad,
    Especialidad,
    Medico,
    MedicoDisponibilidad,
    Paciente,
    Persona,
)
from persons.serializers import (
    EspecialidadSerializer,
    MedicoSerializer,
    PacienteSerializer,
    PersonaSerializer,
    DisponibilidadSerializer,
    MedicoDisponibilidadSerializer,
)
from users.registration import KeycloakAdminError, RegistrationConflict

logger = logging.getLogger(__name__)


class PacienteListCreateView(generics.ListCreateAPIView):
    queryset = Paciente.objects.select_related("persona").all()
    serializer_class = PacienteSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = super().get_queryset()
        roles = set(getattr(self.request.user, "roles", []))

        if "PACIENTE" in roles and not roles.intersection(
            {"ADMIN", "AGENDADOR", "MEDICO"},
        ):
            queryset = queryset.filter(
                persona__usuario__keycloak_user_id=self.request.user.user_id,
            )

        dni = (self.request.query_params.get("dni") or "").strip()
        search = (self.request.query_params.get("q") or "").strip()

        if dni:
            if dni.isdigit():
                queryset = queryset.filter(persona__dni=int(dni))
            else:
                return queryset.none()

        if search:
            queryset = queryset.filter(
                Q(persona__dni__icontains=search)
                | Q(persona__primer_nombre__icontains=search)
                | Q(persona__segundo_nombre__icontains=search)
                | Q(persona__primer_apellido__icontains=search)
                | Q(persona__segundo_apellido__icontains=search)
            )

        return queryset.order_by(
            "persona__primer_nombre",
            "persona__primer_apellido",
            "persona__dni",
        )


class PacienteDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Paciente.objects.select_related("persona").all()
    serializer_class = PacienteSerializer
    permission_classes = [IsAuthenticated]

class EspecialidadListCreateView(generics.ListCreateAPIView):
    queryset = Especialidad.objects.all()
    serializer_class = EspecialidadSerializer
    permission_classes = [IsAuthenticated]


class MedicoListCreateView(generics.ListCreateAPIView):
    queryset = (
        Medico.objects
        .select_related("persona")
        .prefetch_related("especialidades__especialidad")
        .all()
    )
    serializer_class = MedicoSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        try:
            return super().create(request, *args, **kwargs)
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
            logger.exception(
                "La base de datos no pudo guardar el registro del médico.",
            )
            return Response(
                {
                    "detail": (
                        "No fue posible guardar el registro en la base de datos. "
                        "Verifica que PostgreSQL esté activo e inténtalo nuevamente."
                    )
                },
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

    def get_queryset(self):
        queryset = super().get_queryset()
        dni = (self.request.query_params.get("dni") or "").strip()
        search = (self.request.query_params.get("q") or "").strip()

        if dni:
            if dni.isdigit():
                queryset = queryset.filter(persona__dni=int(dni))
            else:
                return queryset.none()

        if search:
            queryset = queryset.filter(
                Q(persona__dni__icontains=search)
                | Q(persona__primer_nombre__icontains=search)
                | Q(persona__segundo_nombre__icontains=search)
                | Q(persona__primer_apellido__icontains=search)
                | Q(persona__segundo_apellido__icontains=search)
            )

        return queryset.order_by(
            "persona__primer_nombre",
            "persona__primer_apellido",
            "persona__dni",
        )


class MedicoDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = (
        Medico.objects
        .select_related("persona")
        .prefetch_related("especialidades__especialidad")
        .all()
    )
    serializer_class = MedicoSerializer
    permission_classes = [IsAuthenticated] 

class PersonaListCreateView(generics.ListCreateAPIView):
    queryset = Persona.objects.all()
    serializer_class = PersonaSerializer
    permission_classes = [IsAuthenticated]


class PersonaDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Persona.objects.all()
    serializer_class = PersonaSerializer
    permission_classes = [IsAuthenticated]

class DisponibilidadListCreateView(generics.ListCreateAPIView):
    queryset = Disponibilidad.objects.all()
    serializer_class = DisponibilidadSerializer
    permission_classes = [IsAuthenticated]

    def get_permissions(self):
        permission_class = (
            IsAdminRole
            if self.request.method == "POST"
            else IsAuthenticated
        )
        return [permission_class()]


class DisponibilidadDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Disponibilidad.objects.all()
    serializer_class = DisponibilidadSerializer
    permission_classes = [IsAuthenticated]

    def get_permissions(self):
        permission_class = (
            IsAdminRole
            if self.request.method in {"PUT", "PATCH", "DELETE"}
            else IsAuthenticated
        )
        return [permission_class()]


class MedicoDisponibilidadListCreateView(generics.ListCreateAPIView):
    queryset = MedicoDisponibilidad.objects.select_related(
        "medico__persona",
        "disponibilidad",
    )
    serializer_class = MedicoDisponibilidadSerializer
    permission_classes = [IsAuthenticated]

    def get_permissions(self):
        permission_class = (
            IsAdminRole
            if self.request.method == "POST"
            else IsAuthenticated
        )
        return [permission_class()]


class MedicoDisponibilidadDetailView(
    generics.RetrieveDestroyAPIView,
):
    queryset = MedicoDisponibilidad.objects.all()
    serializer_class = MedicoDisponibilidadSerializer
    permission_classes = [IsAuthenticated]

    def get_permissions(self):
        permission_class = (
            IsAdminRole
            if self.request.method == "DELETE"
            else IsAuthenticated
        )
        return [permission_class()]