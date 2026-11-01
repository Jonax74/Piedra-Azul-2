from datetime import date, datetime, timedelta

from django.core.exceptions import ValidationError
from django.utils import timezone

from appointments.models import (
    ESTADOS_OCUPADOS,
    Cita,
    ConfiguracionSistema,
    Festivo,
)
from persons.models import MedicoDisponibilidad


DIAS_SEMANA = [
    "LUNES",
    "MARTES",
    "MIERCOLES",
    "JUEVES",
    "VIERNES",
    "SABADO",
    "DOMINGO",
]


def obtener_franjas_disponibles(medico_id, fecha):
    if Festivo.objects.filter(fecha=fecha).exists():
        return []

    dia_semana = DIAS_SEMANA[fecha.weekday()]

    relaciones = (
        MedicoDisponibilidad.objects
        .select_related("disponibilidad")
        .filter(
            medico_id=medico_id,
            disponibilidad__dia_semana=dia_semana,
        )
        .order_by("disponibilidad__hora_inicio")
    )

    zona_horaria = timezone.get_current_timezone()
    franjas = []

    for relacion in relaciones:
        disponibilidad = relacion.disponibilidad
        hora_actual = disponibilidad.hora_inicio

        while hora_actual < disponibilidad.hora_fin:
            fecha_hora = timezone.make_aware(
                datetime.combine(fecha, hora_actual),
                zona_horaria,
            )

            ocupado = Cita.objects.filter(
                medico_id=medico_id,
                fecha_hora=fecha_hora,
                estado__in=ESTADOS_OCUPADOS,
            ).exists()

            if (
                not ocupado
                and fecha_hora > timezone.now()
                and fecha_dentro_de_ventana(fecha_hora)
            ):
                franjas.append(
                    {
                        "fecha": fecha.isoformat(),
                        "hora": hora_actual.strftime("%H:%M"),
                        "fecha_hora": fecha_hora.isoformat(),
                    }
                )

            siguiente = (
                datetime.combine(fecha, hora_actual)
                + timedelta(minutes=disponibilidad.intervalo)
            )

            hora_actual = siguiente.time()

    return franjas


def obtener_fechas_disponibles(medico_id: int, mes: date) -> list[str]:
    inicio = mes.replace(day=1)
    if mes.month == 12:
        fin = mes.replace(year=mes.year + 1, month=1, day=1)
    else:
        fin = mes.replace(month=mes.month + 1, day=1)

    ahora = timezone.now()
    ahora_local = timezone.localtime(ahora)
    configuracion = (
        ConfiguracionSistema.objects
        .filter(activo=True)
        .order_by("-actualizado_en")
        .first()
    )
    limite = (
        ahora + timedelta(weeks=configuracion.semanas_agendamiento)
        if configuracion is not None
        else None
    )

    disponibilidades_por_dia = {}
    relaciones = (
        MedicoDisponibilidad.objects
        .select_related("disponibilidad")
        .filter(medico_id=medico_id)
        .order_by("disponibilidad__hora_inicio")
    )
    for relacion in relaciones:
        dia = relacion.disponibilidad.dia_semana
        disponibilidades_por_dia.setdefault(dia, []).append(
            relacion.disponibilidad,
        )

    festivos = set(
        Festivo.objects
        .filter(fecha__gte=inicio, fecha__lt=fin)
        .values_list("fecha", flat=True)
    )
    fecha_inicio_local = timezone.make_aware(
        datetime.combine(inicio, datetime.min.time()),
        timezone.get_current_timezone(),
    )
    fecha_fin_local = timezone.make_aware(
        datetime.combine(fin, datetime.min.time()),
        timezone.get_current_timezone(),
    )
    ocupadas = {
        timezone.localtime(fecha_hora)
        for fecha_hora in Cita.objects.filter(
            medico_id=medico_id,
            fecha_hora__gte=fecha_inicio_local,
            fecha_hora__lt=fecha_fin_local,
            estado__in=ESTADOS_OCUPADOS,
        ).values_list("fecha_hora", flat=True)
    }

    fechas_disponibles = []
    fecha_actual = inicio
    while fecha_actual < fin:
        dia_semana = DIAS_SEMANA[fecha_actual.weekday()]
        horarios = disponibilidades_por_dia.get(dia_semana, [])
        if fecha_actual not in festivos and horarios:
            hay_fecha_disponible = False
            for disponibilidad in horarios:
                hora_actual = disponibilidad.hora_inicio
                while hora_actual < disponibilidad.hora_fin:
                    fecha_hora = timezone.make_aware(
                        datetime.combine(fecha_actual, hora_actual),
                        timezone.get_current_timezone(),
                    )
                    if (
                        fecha_hora > ahora_local
                        and (limite is None or fecha_hora <= limite)
                        and fecha_hora not in ocupadas
                    ):
                        hay_fecha_disponible = True
                        break
                    hora_actual = (
                        datetime.combine(fecha_actual, hora_actual)
                        + timedelta(minutes=disponibilidad.intervalo)
                    ).time()
                if hay_fecha_disponible:
                    break
            if hay_fecha_disponible:
                fechas_disponibles.append(fecha_actual.isoformat())
        fecha_actual += timedelta(days=1)

    return fechas_disponibles


def fecha_dentro_de_ventana(fecha_hora):
    configuracion = (
        ConfiguracionSistema.objects
        .filter(activo=True)
        .order_by("-actualizado_en")
        .first()
    )

    if configuracion is None:
        return True

    ahora = timezone.localtime(timezone.now())
    limite = ahora + timedelta(
        weeks=configuracion.semanas_agendamiento,
    )

    return fecha_hora <= limite


def esta_dentro_de_disponibilidad(
    medico_id: int,
    fecha_hora: datetime,
) -> bool:
    fecha_hora_local = timezone.localtime(fecha_hora)
    dia_semana = DIAS_SEMANA[fecha_hora_local.weekday()]

    return MedicoDisponibilidad.objects.filter(
        medico_id=medico_id,
        disponibilidad__dia_semana=dia_semana,
        disponibilidad__hora_inicio__lte=fecha_hora_local.time(),
        disponibilidad__hora_fin__gt=fecha_hora_local.time(),
    ).exists()


def esta_en_intervalo_disponible(
    medico_id: int,
    fecha_hora: datetime,
) -> bool:
    fecha_hora_local = timezone.localtime(fecha_hora)
    dia_semana = DIAS_SEMANA[fecha_hora_local.weekday()]

    disponibilidades = (
        MedicoDisponibilidad.objects
        .filter(
            medico_id=medico_id,
            disponibilidad__dia_semana=dia_semana,
        )
        .select_related("disponibilidad")
    )

    minutos_cita = (
        fecha_hora_local.hour * 60
        + fecha_hora_local.minute
    )

    for relacion in disponibilidades:
        disponibilidad = relacion.disponibilidad

        inicio = (
            disponibilidad.hora_inicio.hour * 60
            + disponibilidad.hora_inicio.minute
        )
        fin = (
            disponibilidad.hora_fin.hour * 60
            + disponibilidad.hora_fin.minute
        )

        if (
            inicio <= minutos_cita < fin
            and (
                minutos_cita - inicio
            ) % disponibilidad.intervalo == 0
        ):
            return True

    return False


def validar_cita_programable(
    medico_id: int,
    fecha_hora: datetime,
    cita_id: int | None = None,
) -> None:
    fecha_hora_local = timezone.localtime(fecha_hora)
    ahora = timezone.localtime(timezone.now())

    if fecha_hora_local <= ahora:
        raise ValidationError(
            "La cita debe programarse en una fecha futura."
        )

    if not fecha_dentro_de_ventana(fecha_hora_local):
        raise ValidationError(
            "La fecha supera la ventana permitida "
            "para agendar citas."
        )

    if Festivo.objects.filter(
        fecha=fecha_hora_local.date(),
    ).exists():
        raise ValidationError(
            "No se pueden agendar citas en días festivos."
        )

    if not esta_dentro_de_disponibilidad(
        medico_id=medico_id,
        fecha_hora=fecha_hora,
    ):
        raise ValidationError(
            "El médico no tiene disponibilidad en ese horario."
        )

    if not esta_en_intervalo_disponible(
        medico_id=medico_id,
        fecha_hora=fecha_hora,
    ):
        raise ValidationError(
            "La hora no coincide con un intervalo disponible."
        )

    citas_ocupadas = Cita.objects.filter(
        medico_id=medico_id,
        fecha_hora=fecha_hora,
        estado__in=ESTADOS_OCUPADOS,
    )

    if cita_id is not None:
        citas_ocupadas = citas_ocupadas.exclude(id=cita_id)

    if citas_ocupadas.exists():
        raise ValidationError(
            "El médico ya tiene una cita en ese horario."
        )