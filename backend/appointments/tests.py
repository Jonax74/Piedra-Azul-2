from datetime import date
from types import SimpleNamespace
from unittest.mock import patch

from django.test import TestCase
from rest_framework.test import APIRequestFactory, force_authenticate

from appointments.models import Cita
from appointments.serializers import CitaSerializer
from appointments.views import AgendaCitasView, CitaListCreateView
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
				is_authenticated=True,
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

	def test_cita_includes_patient_and_medico_names(self):
		cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)

		data = CitaSerializer(cita).data

		self.assertEqual(
			data["paciente_detalle"]["persona"]["primer_nombre"],
			"Ana",
		)
		self.assertEqual(
			data["medico_detalle"]["persona"]["primer_nombre"],
			"Carlos",
		)

	def test_agenda_accepts_no_filters(self):
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)
		request = APIRequestFactory().get("/api/citas/agenda/")
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["AGENDADOR"],
				user_id="agendador-test",
			),
		)

		response = AgendaCitasView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(response.data["cantidad"], 1)

	def test_patient_sees_appointment_created_by_scheduler(self):
		scheduler = Usuario.objects.create(
			username="agendador.test",
			keycloak_user_id="agendador-test",
		)
		Cita.objects.create(
			usuario=scheduler,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)
		request = APIRequestFactory().get("/api/citas/")
		force_authenticate(request, user=self.request.user)

		response = CitaListCreateView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(len(response.data), 1)
