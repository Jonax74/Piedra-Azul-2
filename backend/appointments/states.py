from django.core.exceptions import ValidationError


class EstadoCita:
    nombre = ""
    transiciones_permitidas = frozenset()

    def validar_transicion(
        self,
        cita,
        nuevo_estado,
        fecha_hora=None,
    ):
        if nuevo_estado not in ESTADOS_CITA:
            raise ValidationError(
                {"estado": "El estado indicado no es válido."},
            )

        if nuevo_estado != self.nombre and (
            nuevo_estado not in self.transiciones_permitidas
        ):
            raise ValidationError(
                {
                    "estado": (
                        f"No se puede cambiar una cita de {self.nombre} "
                        f"a {nuevo_estado}."
                    ),
                },
            )

        if nuevo_estado == "REAGENDADA":
            if fecha_hora is None:
                raise ValidationError(
                    {
                        "fecha_hora": (
                            "Debe indicar la nueva fecha y hora para "
                            "reagendar la cita."
                        ),
                    },
                )
            if fecha_hora == cita.fecha_hora:
                raise ValidationError(
                    {
                        "fecha_hora": (
                            "La nueva fecha y hora debe ser diferente "
                            "a la actual."
                        ),
                    },
                )
        elif fecha_hora is not None and fecha_hora != cita.fecha_hora:
            raise ValidationError(
                {
                    "fecha_hora": (
                        "La fecha solo puede cambiar al reagendar la cita."
                    ),
                },
            )

    def transicionar(self, cita, nuevo_estado, fecha_hora=None):
        self.validar_transicion(cita, nuevo_estado, fecha_hora)
        if nuevo_estado == "REAGENDADA":
            cita.fecha_hora = fecha_hora
        cita.estado = nuevo_estado


class Programada(EstadoCita):
    nombre = "PROGRAMADA"
    transiciones_permitidas = frozenset(
        {"CONFIRMADA", "CANCELADA", "REAGENDADA", "ATENDIDA", "NO_ASISTIDA"},
    )


class Confirmada(EstadoCita):
    nombre = "CONFIRMADA"
    transiciones_permitidas = frozenset(
        {"CANCELADA", "REAGENDADA", "ATENDIDA", "NO_ASISTIDA"},
    )


class Cancelada(EstadoCita):
    nombre = "CANCELADA"
    transiciones_permitidas = frozenset({"CONFIRMADA", "REAGENDADA"})


class Reagendada(EstadoCita):
    nombre = "REAGENDADA"
    transiciones_permitidas = frozenset(
        {"CONFIRMADA", "CANCELADA", "ATENDIDA", "NO_ASISTIDA"},
    )


class Atendida(EstadoCita):
    nombre = "ATENDIDA"


class NoAsistida(EstadoCita):
    nombre = "NO_ASISTIDA"


ESTADOS_CITA = {
    estado.nombre: estado
    for estado in (
        Programada(),
        Confirmada(),
        Cancelada(),
        Reagendada(),
        Atendida(),
        NoAsistida(),
    )
}


def obtener_estado_cita(nombre):
    try:
        return ESTADOS_CITA[nombre]
    except KeyError as error:
        raise ValueError(f"Estado de cita desconocido: {nombre}") from error
