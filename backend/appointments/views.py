from rest_framework import generics
from rest_framework.permissions import IsAuthenticated

from appointments.models import Cita
from appointments.serializers import CitaSerializer

from datetime import datetime, time

from django.db.models import QuerySet
from django.utils import timezone
from rest_framework.response import Response
from rest_framework.views import APIView

from config.security.permissions import IsAgendadorOrAdmin

from datetime import datetime

from rest_framework import status


from appointments.serializers import FranjaDisponibleSerializer
from appointments.services import obtener_franjas_disponibles
from persons.models import Medico

from appointments.models import ConfiguracionSistema
from appointments.serializers import (
    ConfiguracionSistemaSerializer,
)
from config.security.permissions import IsAdminRole


class ConfiguracionSistemaView(generics.RetrieveUpdateAPIView):
    serializer_class = ConfiguracionSistemaSerializer
    permission_classes = [IsAuthenticated]

    def get_permissions(self):
        permission_class = (
            IsAdminRole
            if self.request.method in {"PUT", "PATCH"}
            else IsAuthenticated
        )
        return [permission_class()]

    def get_object(self):
        configuracion = (
            ConfiguracionSistema.objects
            .filter(activo=True)
            .order_by("-actualizado_en", "-id")
            .first()
        )

        if configuracion is None:
            configuracion = ConfiguracionSistema.objects.create(
                semanas_agendamiento=4,
                activo=True,
            )

        return configuracion

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

    def get_queryset(self):
        queryset = super().get_queryset()
        roles = set(getattr(self.request.user, "roles", []))

        if "PACIENTE" in roles and not roles.intersection(
            {"ADMIN", "AGENDADOR", "MEDICO"},
        ):
            return queryset.filter(
                paciente__persona__usuario__keycloak_user_id=self.request.user.user_id,
            )

        return queryset


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

class AgendaCitasView(APIView):
    permission_classes = [IsAgendadorOrAdmin]

    def get(self, request):
        medico_id = request.query_params.get("medico")
        fecha_texto = request.query_params.get("fecha")

        filtros = {}
        fecha = None

        if medico_id:
            try:
                filtros["medico_id"] = int(medico_id)
            except ValueError:
                return Response(
                    {"detail": "medico debe ser numérico."},
                    status=400,
                )

        if fecha_texto:
            try:
                fecha = datetime.strptime(
                    fecha_texto,
                    "%Y-%m-%d",
                ).date()
            except ValueError:
                return Response(
                    {
                        "detail": (
                            "fecha debe usar el formato YYYY-MM-DD."
                        ),
                    },
                    status=400,
                )

            zona_horaria = timezone.get_current_timezone()
            filtros["fecha_hora__gte"] = timezone.make_aware(
                datetime.combine(fecha, time.min),
                zona_horaria,
            )
            filtros["fecha_hora__lte"] = timezone.make_aware(
                datetime.combine(fecha, time.max),
                zona_horaria,
            )

        citas = (
            Cita.objects
            .select_related(
                "paciente__persona",
                "medico__persona",
                "usuario",
            )
            .filter(**filtros)
            .order_by("fecha_hora")
        )

        return Response(
            {
                "cantidad": citas.count(),
                "resultados": CitaSerializer(
                    citas,
                    many=True,
                ).data,
            },
        )    

class FranjasDisponiblesView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        medico_id = request.query_params.get("medico")
        fecha_texto = request.query_params.get("fecha")

        if not medico_id or not fecha_texto:
            return Response(
                {
                    "detail": (
                        "Los parámetros medico y fecha son obligatorios."
                    ),
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            medico_id = int(medico_id)
            fecha = datetime.strptime(
                fecha_texto,
                "%Y-%m-%d",
            ).date()
        except ValueError:
            return Response(
                {
                    "detail": (
                        "medico debe ser numérico y fecha debe usar "
                        "el formato YYYY-MM-DD."
                    ),
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not Medico.objects.filter(
            pk=medico_id,
            estado="ACTIVO",
        ).exists():
            return Response(
                {
                    "detail": "El médico no existe o está inactivo.",
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        franjas = obtener_franjas_disponibles(
            medico_id=medico_id,
            fecha=fecha,
        )

        serializer = FranjaDisponibleSerializer(
            franjas,
            many=True,
        )

        return Response(
            {
                "medico": medico_id,
                "fecha": fecha.isoformat(),
                "cantidad": len(franjas),
                "franjas": serializer.data,
            },
        )    