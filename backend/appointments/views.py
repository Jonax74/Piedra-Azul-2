from rest_framework import generics
from rest_framework.permissions import IsAuthenticated

from appointments.models import Cita
from appointments.serializers import CitaSerializer


class CitaListCreateView(generics.ListCreateAPIView):
    queryset = (
        Cita.objects
        .select_related(
            "paciente__persona",
            "medico__persona",
            "usuario",
        )
        .all()
    )
    serializer_class = CitaSerializer
    permission_classes = [IsAuthenticated]


class CitaDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = (
        Cita.objects
        .select_related(
            "paciente__persona",
            "medico__persona",
            "usuario",
        )
        .all()
    )
    serializer_class = CitaSerializer
    permission_classes = [IsAuthenticated]