from datetime import date, timedelta
from types import SimpleNamespace
from unittest.mock import patch

from django.test import TestCase

from appointments.models import Cita
from appointments.serializers import CitaSerializer
from persons.models import Medico, Paciente, Persona
from users.models import Rol, Usuario, UsuarioRol


class CitaPacienteScopeTests(TestCase):
	def setUp(self):
		self.persona_paciente = Persona.objects.create(
			primer_nombre="Ana",
			primer_apellido="Paciente",
			genero="MUJER",
			fecha_nacimiento=date(1992, 1, 1),
			telefono="3000000000",
			dni=10000001,
		)
		self.paciente = Paciente.objects.create(
			persona=self.persona_paciente,
		)
		self.persona_otro_paciente = Persona.objects.create(
			primer_nombre="Laura",
			primer_apellido="Paciente",
			genero="MUJER",
			fecha_nacimiento=date(1993, 1, 1),
			telefono="3000000001",
			dni=10000002,
		)
		self.otro_paciente = Paciente.objects.create(
			persona=self.persona_otro_paciente,
		)
		persona_medico = Persona.objects.create(
			primer_nombre="Carlos",
			primer_apellido="Medico",
			genero="HOMBRE",
			fecha_nacimiento=date(1985, 1, 1),
			telefono="3000000002",
			dni=90000001,
		)
		self.medico = Medico.objects.create(
			persona=persona_medico,
			tipo_profesional="MEDICO",
		)
		self.usuario = Usuario.objects.create(
			username="paciente.test",
			keycloak_user_id="paciente-test",
			persona=self.persona_paciente,
		)
		rol = Rol.objects.create(nombre="PACIENTE")
		UsuarioRol.objects.create(usuario=self.usuario, rol=rol)
		self.request = SimpleNamespace(
			user=SimpleNamespace(
				user_id="paciente-test",
				roles=["PACIENTE"],
			),
		)

	def serializer_for(self, paciente):
		return CitaSerializer(
			data={
				"paciente": paciente.pk,
				"medico": self.medico.pk,
				"fecha_hora": (
					"2099-01-05T10:00:00Z"
				),
			},
			context={"request": self.request},
		)

	@patch("appointments.serializers.validar_cita_programable")
	def test_patient_can_schedule_for_self(self, validar_cita):
		serializer = self.serializer_for(self.paciente)

		self.assertTrue(serializer.is_valid(), serializer.errors)
		self.assertEqual(serializer.validated_data["paciente"], self.paciente)

	@patch("appointments.serializers.validar_cita_programable")
	def test_patient_cannot_schedule_for_another_patient(self, validar_cita):
		serializer = self.serializer_for(self.otro_paciente)

		self.assertFalse(serializer.is_valid())
		self.assertIn("non_field_errors", serializer.errors)
		self.assertIn(
			"Un paciente solo puede agendar citas para sí mismo.",
			serializer.errors["non_field_errors"],
		)
