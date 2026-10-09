import csv
from datetime import datetime, time
from io import StringIO

from django.db.models.functions import TruncDate
from django.http import HttpResponse
from django.utils import timezone
from django.utils.text import slugify
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from appointments.models import Cita, ConfiguracionSistema
from appointments.serializers import (
    CitaSerializer,
    ConfiguracionSistemaSerializer,
    FranjaDisponibleSerializer,
)
from appointments.services import obtener_franjas_disponibles
from config.security.permissions import (
    IsAdminRole,
    IsAgendadorOrAdmin,
    IsDoctorRole,
)
from persons.models import Medico


def _safe_spreadsheet_text(value):
    if value and value.lstrip(" \t\r\n")[:1] in {"=", "+", "-", "@"}:
        return f"'{value}"
    return value


def _appointment_csv_response(citas, medico, fecha):
    zona_horaria = timezone.get_current_timezone()
    persona_medico = medico.persona
    nombre_medico = " ".join(
        filter(
            None,
            [
                persona_medico.primer_nombre,
                persona_medico.segundo_nombre,
                persona_medico.primer_apellido,
                persona_medico.segundo_apellido,
            ],
        ),
    )

    output = StringIO()
    output.write("\ufeff")
    writer = csv.writer(output, delimiter=";")
    writer.writerow(
        [f"Citas médico {nombre_medico} - fecha {fecha.isoformat()}"],
    )
    writer.writerow([])
    writer.writerow(
        ["Fecha", "Hora", "Paciente", "Documento", "Teléfono", "Estado"],
    )
    for cita in citas:
        paciente = cita.paciente.persona
        fecha_hora = timezone.localtime(cita.fecha_hora, zona_horaria)
        writer.writerow(
            [
                fecha_hora.strftime("%Y-%m-%d"),
                fecha_hora.strftime("%H:%M"),
                _safe_spreadsheet_text(
                    " ".join(
                        filter(
                            None,
                            [
                                paciente.primer_nombre,
                                paciente.segundo_nombre,
                                paciente.primer_apellido,
                                paciente.segundo_apellido,
                            ],
                        ),
                    ),
                ),
                paciente.dni,
                _safe_spreadsheet_text(paciente.telefono),
                cita.get_estado_display(),
            ],
        )

    response = HttpResponse(
        output.getvalue(),
        content_type="text/csv; charset=utf-8",
    )
    filename_medico = slugify(nombre_medico) or f"medico-{medico.pk}"
    response["Content-Disposition"] = (
        f'attachment; filename="citas-{filename_medico}-{fecha.isoformat()}.csv"'
    )
    return response


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


class FechasAgendaCitasView(APIView):
    permission_classes = [IsAgendadorOrAdmin]

    def get(self, request):
        month_text = request.query_params.get("mes", "")
        medico_id = request.query_params.get("medico")

        try:
            month = datetime.strptime(month_text, "%Y-%m").date().replace(day=1)
        except ValueError:
            return Response(
                {"detail": "mes es obligatorio y debe usar el formato YYYY-MM."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        filtros = {}
        if medico_id:
            try:
                filtros["medico_id"] = int(medico_id)
            except ValueError:
                return Response(
                    {"detail": "medico debe ser numérico."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        next_month = (
            month.replace(year=month.year + 1, month=1)
            if month.month == 12
            else month.replace(month=month.month + 1)
        )
        zona_horaria = timezone.get_current_timezone()
        fechas = (
            Cita.objects
            .filter(
                **filtros,
                fecha_hora__gte=timezone.make_aware(
                    datetime.combine(month, time.min),
                    zona_horaria,
                ),
                fecha_hora__lt=timezone.make_aware(
                    datetime.combine(next_month, time.min),
                    zona_horaria,
                ),
            )
            .exclude(estado="CANCELADA")
            .annotate(fecha_local=TruncDate("fecha_hora", tzinfo=zona_horaria))
            .values_list("fecha_local", flat=True)
            .distinct()
            .order_by("fecha_local")
        )

        return Response(
            {
                "mes": month.strftime("%Y-%m"),
                "fechas": [fecha.isoformat() for fecha in fechas],
            },
        )


class ExportarAgendaCitasView(APIView):
    permission_classes = [IsAgendadorOrAdmin]

    def get(self, request):
        medico_id = request.query_params.get("medico", "")
        fecha_texto = request.query_params.get("fecha", "")
        try:
            medico_id = int(medico_id)
        except ValueError:
            return Response(
                {"detail": "Debes seleccionar un médico o terapista válido."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            fecha = datetime.strptime(fecha_texto, "%Y-%m-%d").date()
        except ValueError:
            return Response(
                {"detail": "fecha es obligatoria y debe usar el formato YYYY-MM-DD."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not Medico.objects.filter(pk=medico_id, estado="ACTIVO").exists():
            return Response(
                {"detail": "El médico o terapista seleccionado no está activo."},
                status=status.HTTP_404_NOT_FOUND,
            )

        zona_horaria = timezone.get_current_timezone()
        inicio = timezone.make_aware(datetime.combine(fecha, time.min), zona_horaria)
        fin = timezone.make_aware(datetime.combine(fecha, time.max), zona_horaria)
        citas = (
            Cita.objects
            .select_related("paciente__persona", "medico__persona")
            .filter(
                medico_id=medico_id,
                fecha_hora__gte=inicio,
                fecha_hora__lte=fin,
            )
            .exclude(estado="CANCELADA")
            .order_by("fecha_hora")
        )
        medico = Medico.objects.select_related("persona").get(pk=medico_id)
        return _appointment_csv_response(citas, medico, fecha)


class ExportarAgendaMedicoView(APIView):
    permission_classes = [IsDoctorRole]

    def get(self, request):
        fecha_texto = request.query_params.get("fecha", "")
        try:
            fecha = datetime.strptime(fecha_texto, "%Y-%m-%d").date()
        except ValueError:
            return Response(
                {"detail": "fecha es obligatoria y debe usar el formato YYYY-MM-DD."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        medico = (
            Medico.objects
            .select_related("persona")
            .filter(
                persona__usuario__keycloak_user_id=request.user.user_id,
            )
            .first()
        )
        if medico is None:
            return Response(
                {"detail": "Tu cuenta no está vinculada a un perfil médico."},
                status=status.HTTP_404_NOT_FOUND,
            )

        zona_horaria = timezone.get_current_timezone()
        inicio = timezone.make_aware(datetime.combine(fecha, time.min), zona_horaria)
        fin = timezone.make_aware(datetime.combine(fecha, time.max), zona_horaria)
        citas = (
            Cita.objects
            .select_related("paciente__persona", "medico__persona")
            .filter(
                medico_id=medico.pk,
                fecha_hora__gte=inicio,
                fecha_hora__lte=fin,
            )
            .exclude(estado="CANCELADA")
            .order_by("fecha_hora")
        )
        return _appointment_csv_response(citas, medico, fecha)


class AgendaMedicoView(APIView):
    permission_classes = [IsDoctorRole]

    def get(self, request):
        keycloak_user_id = request.user.user_id
        fecha_texto = request.query_params.get("fecha")
        filtros = {
            "medico__persona__usuario__keycloak_user_id": keycloak_user_id,
        }

        if fecha_texto:
            try:
                fecha = datetime.strptime(fecha_texto, "%Y-%m-%d").date()
            except ValueError:
                return Response(
                    {"detail": "fecha debe usar el formato YYYY-MM-DD."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        if not Medico.objects.filter(
            persona__usuario__keycloak_user_id=keycloak_user_id,
        ).exists():
            return Response(
                {"detail": "Tu cuenta no está vinculada a un perfil médico."},
                status=status.HTTP_404_NOT_FOUND,
            )

        if fecha_texto:
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
                "resultados": CitaSerializer(citas, many=True).data,
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