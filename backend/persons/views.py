from rest_framework import generics
from rest_framework.permissions import IsAuthenticated
from django.db.models import Q

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


class DisponibilidadDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Disponibilidad.objects.all()
    serializer_class = DisponibilidadSerializer
    permission_classes = [IsAuthenticated]


class MedicoDisponibilidadListCreateView(generics.ListCreateAPIView):
    queryset = MedicoDisponibilidad.objects.select_related(
        "medico__persona",
        "disponibilidad",
    )
    serializer_class = MedicoDisponibilidadSerializer
    permission_classes = [IsAuthenticated]


class MedicoDisponibilidadDetailView(
    generics.RetrieveDestroyAPIView,
):
    queryset = MedicoDisponibilidad.objects.all()
    serializer_class = MedicoDisponibilidadSerializer
    permission_classes = [IsAuthenticated]    