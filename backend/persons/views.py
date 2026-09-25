from rest_framework import generics
from rest_framework.permissions import IsAuthenticated

from persons.models import Paciente
from persons.serializers import PacienteSerializer

from persons.models import Especialidad, Medico
from persons.serializers import (
    EspecialidadSerializer,
    MedicoSerializer,
)

from persons.models import Persona
from persons.serializers import PersonaSerializer

from persons.models import Disponibilidad, MedicoDisponibilidad
from persons.serializers import (
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
            return queryset.filter(
                persona__usuario__keycloak_user_id=self.request.user.user_id,
            )

        return queryset


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