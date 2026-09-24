from rest_framework import generics
from rest_framework.permissions import IsAuthenticated

from persons.models import Paciente
from persons.serializers import PacienteSerializer


class PacienteListCreateView(generics.ListCreateAPIView):
    queryset = Paciente.objects.select_related("persona").all()
    serializer_class = PacienteSerializer
    permission_classes = [IsAuthenticated]


class PacienteDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Paciente.objects.select_related("persona").all()
    serializer_class = PacienteSerializer
    permission_classes = [IsAuthenticated]