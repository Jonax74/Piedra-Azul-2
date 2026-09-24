from datetime import datetime, timedelta

from django.utils import timezone

from appointments.models import Cita, Festivo
from persons.models import MedicoDisponibilidad

from datetime import date, datetime

from django.core.exceptions import ValidationError



ESTADOS_OCUPADOS = ["PROGRAMADA", "CONFIRMADA"]


def obtener_franjas_disponibles(medico_id, fecha):
    if Festivo.objects.filter(fecha=fecha).exists():
        return []

    dia_semana = [
        "LUNES",
        "MARTES",
        "MIERCOLES",
        "JUEVES",
        "VIERNES",
        "SABADO",
        "DOMINGO",
    ][fecha.weekday()]

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

            if not ocupado and fecha_hora > timezone.now():
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

def esta_dentro_de_disponibilidad(
    medico_id: int,
    fecha_hora: datetime,
) -> bool:
    fecha_hora_local = timezone.localtime(fecha_hora)

    dia_semana = [
        "LUNES",
        "MARTES",
        "MIERCOLES",
        "JUEVES",
        "VIERNES",
        "SABADO",
        "DOMINGO",
    ][fecha_hora_local.weekday()]

    return MedicoDisponibilidad.objects.filter(
        medico_id=medico_id,
        disponibilidad__dia_semana=dia_semana,
        disponibilidad__hora_inicio__lte=fecha_hora_local.time(),
        disponibilidad__hora_fin__gt=fecha_hora_local.time(),
    ).exists()


def validar_cita_programable(
    medico_id: int,
    fecha_hora: datetime,
    cita_id: int | None = None,
) -> None:
    fecha_hora_local = timezone.localtime(fecha_hora)

    if fecha_hora_local <= timezone.localtime(timezone.now()):
        raise ValidationError(
            "La cita debe programarse en una fecha futura."
        )

    if Festivo.objects.filter(fecha=fecha_hora_local.date()).exists():
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

def esta_en_intervalo_disponible(
    medico_id: int,
    fecha_hora: datetime,
) -> bool:
    fecha_hora_local = timezone.localtime(fecha_hora)

    dia_semana = [
        "LUNES",
        "MARTES",
        "MIERCOLES",
        "JUEVES",
        "VIERNES",
        "SABADO",
        "DOMINGO",
    ][fecha_hora_local.weekday()]

    disponibilidades = MedicoDisponibilidad.objects.filter(
        medico_id=medico_id,
        disponibilidad__dia_semana=dia_semana,
    ).select_related("disponibilidad")

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
            and (minutos_cita - inicio) % disponibilidad.intervalo == 0
        ):
            return True

    return False    