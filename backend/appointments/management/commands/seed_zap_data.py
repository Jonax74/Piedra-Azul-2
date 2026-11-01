from datetime import date, datetime, time, timedelta

from django.core.management.base import BaseCommand
from django.db import transaction
from django.utils import timezone

from appointments.models import Cita, ConfiguracionSistema
from persons.models import (
    Disponibilidad,
    Medico,
    MedicoDisponibilidad,
    Paciente,
    Persona,
)
from users.models import Rol, Usuario, UsuarioRol


class Command(BaseCommand):
    help = "Create synthetic demo records in the isolated ZAP test database."

    @transaction.atomic
    def handle(self, *args, **options):
        test_people = {
            "paciente1": {
                "names": ("Ana", "Paciente"),
                "dni": 91000001,
                "gender": "MUJER",
                "phone": "3000000001",
                "email": "paciente1@piedrazul.test",
            },
            "paciente2": {
                "names": ("Luis", "Paciente"),
                "dni": 91000002,
                "gender": "HOMBRE",
                "phone": "3000000002",
                "email": "paciente2@piedrazul.test",
            },
            "medico1": {
                "names": ("Carla", "Medica"),
                "dni": 91000003,
                "gender": "MUJER",
                "phone": "3000000003",
                "email": "medico1@piedrazul.test",
            },
        }

        patient_profiles = {}
        for username in ("paciente1", "paciente2"):
            patient_profiles[username] = self._person(
                test_people[username],
            )
            Paciente.objects.get_or_create(
                persona=patient_profiles[username],
            )

        doctor_person = self._person(test_people["medico1"])
        doctor, _ = Medico.objects.update_or_create(
            persona=doctor_person,
            defaults={"tipo_profesional": "MEDICO", "estado": "ACTIVO"},
        )

        users = {}
        user_roles = {
            "paciente1": "PACIENTE",
            "paciente2": "PACIENTE",
            "agendador1": "AGENDADOR",
            "admin1": "ADMIN",
            "medico1": "MEDICO",
        }
        for username, role_name in user_roles.items():
            person = patient_profiles.get(username)
            if username == "medico1":
                person = doctor_person
            user, _ = Usuario.objects.update_or_create(
                username=username,
                defaults={
                    "persona": person,
                    "keycloak_user_id": None,
                    "estado": "ACTIVO",
                },
            )
            role, _ = Rol.objects.get_or_create(nombre=role_name)
            UsuarioRol.objects.get_or_create(usuario=user, rol=role)
            users[username] = user

        schedule_defaults = {
            "hora_inicio": time(8, 0),
            "hora_fin": time(17, 0),
            "intervalo": 30,
        }
        for weekday in (
            "LUNES",
            "MARTES",
            "MIERCOLES",
            "JUEVES",
            "VIERNES",
        ):
            schedule, _ = Disponibilidad.objects.get_or_create(
                dia_semana=weekday,
                **schedule_defaults,
            )
            MedicoDisponibilidad.objects.get_or_create(
                medico=doctor,
                disponibilidad=schedule,
            )

        next_weekday = timezone.localdate() + timedelta(days=1)
        while next_weekday.weekday() > 4:
            next_weekday += timedelta(days=1)
        appointment_time = timezone.make_aware(
            datetime.combine(next_weekday, time(10, 0)),
            timezone.get_current_timezone(),
        )
        Cita.objects.get_or_create(
            paciente_id=patient_profiles["paciente1"].pk,
            medico=doctor,
            fecha_hora=appointment_time,
            defaults={
                "usuario": users["agendador1"],
                "estado": "CONFIRMADA",
                "observacion": "Cita sintética para pruebas locales.",
            },
        )
        ConfiguracionSistema.objects.update_or_create(
            activo=True,
            defaults={"semanas_agendamiento": 8},
        )

        self.stdout.write(
            self.style.SUCCESS(
                "Datos sintéticos listos: 2 pacientes, 1 médico, "
                "1 agendador, 1 administrador y horarios entre semana."
            ),
        )

    @staticmethod
    def _person(details):
        first_name, surname = details["names"]
        person, _ = Persona.objects.update_or_create(
            dni=details["dni"],
            defaults={
                "primer_nombre": first_name,
                "primer_apellido": surname,
                "genero": details["gender"],
                "fecha_nacimiento": date(1985, 1, 1),
                "telefono": details["phone"],
                "correo": details["email"],
            },
        )
        return person
