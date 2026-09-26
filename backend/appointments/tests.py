from datetime import date, datetime, time, timedelta
from types import SimpleNamespace
from unittest.mock import patch

from django.core.exceptions import ValidationError
from django.test import TestCase
from django.utils import timezone
from rest_framework.test import APIRequestFactory, force_authenticate

from appointments.models import (
	Cita,
	ConfiguracionSistema,
	Festivo,
)
from appointments.serializers import CitaSerializer
from appointments.services import (
	esta_en_intervalo_disponible,
	fecha_dentro_de_ventana,
	obtener_franjas_disponibles,
	validar_cita_programable,
)
from appointments.views import (
	AgendaCitasView,
	CitaListCreateView,
	ConfiguracionSistemaView,
)
from persons.models import (
	Disponibilidad,
	Medico,
	MedicoDisponibilidad,
	Paciente,
	Persona,
)
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


class ConfiguracionSistemaPermissionTests(TestCase):
	def setUp(self):
		self.configuracion = ConfiguracionSistema.objects.create(
			semanas_agendamiento=8,
			activo=True,
		)

	def authenticated_user(self, roles):
		return SimpleNamespace(
			roles=roles,
			is_authenticated=True,
		)

	def test_authenticated_non_admin_can_read_configuration(self):
		request = APIRequestFactory().get("/api/configuracion/")
		force_authenticate(
			request,
			user=self.authenticated_user(["PACIENTE"]),
		)

		response = ConfiguracionSistemaView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(
			response.data["semanas_agendamiento"],
			self.configuracion.semanas_agendamiento,
		)

	def test_only_admin_can_update_configuration(self):
		factory = APIRequestFactory()
		request = factory.patch(
			"/api/configuracion/",
			{"semanas_agendamiento": 12},
			format="json",
		)
		force_authenticate(
			request,
			user=self.authenticated_user(["AGENDADOR"]),
		)

		response = ConfiguracionSistemaView.as_view()(request)

		self.assertEqual(response.status_code, 403)

		request = factory.patch(
			"/api/configuracion/",
			{"semanas_agendamiento": 12},
			format="json",
		)
		force_authenticate(
			request,
			user=self.authenticated_user(["ADMIN"]),
		)

		response = ConfiguracionSistemaView.as_view()(request)
		self.configuracion.refresh_from_db()

		self.assertEqual(response.status_code, 200)
		self.assertEqual(self.configuracion.semanas_agendamiento, 12)


class AgendaServiceTests(TestCase):
	def setUp(self):
		self.fecha = date(2099, 1, 5)
		self.persona_paciente = Persona.objects.create(
			primer_nombre="Ana",
			primer_apellido="Paciente",
			genero="MUJER",
			fecha_nacimiento=date(1992, 1, 1),
			telefono="3000000000",
			dni=10000011,
		)
		self.paciente = Paciente.objects.create(
			persona=self.persona_paciente,
		)
		self.persona_medico = Persona.objects.create(
			primer_nombre="Carlos",
			primer_apellido="Medico",
			genero="HOMBRE",
			fecha_nacimiento=date(1985, 1, 1),
			telefono="3000000002",
			dni=90000011,
		)
		self.medico = Medico.objects.create(
			persona=self.persona_medico,
			tipo_profesional="MEDICO",
		)
		self.usuario = Usuario.objects.create(
			username="agenda.test",
			keycloak_user_id="agenda-test",
		)
		self.disponibilidad = Disponibilidad.objects.create(
			dia_semana="LUNES",
			hora_inicio=time(9, 0),
			hora_fin=time(10, 0),
			intervalo=30,
		)
		MedicoDisponibilidad.objects.create(
			medico=self.medico,
			disponibilidad=self.disponibilidad,
		)

	def fecha_hora(self, hora):
		return timezone.make_aware(
			datetime.combine(self.fecha, hora),
			timezone.get_current_timezone(),
		)

	def crear_cita(self, fecha_hora, estado="PROGRAMADA"):
		return Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora=fecha_hora,
			estado=estado,
		)

	def test_generates_only_free_future_slots_in_schedule(self):
		franjas = obtener_franjas_disponibles(self.medico.pk, self.fecha)

		self.assertEqual(
			[franja["hora"] for franja in franjas],
			["09:00", "09:30"],
		)

	def test_holiday_has_no_available_slots(self):
		Festivo.objects.create(fecha=self.fecha)

		self.assertEqual(
			obtener_franjas_disponibles(self.medico.pk, self.fecha),
			[],
		)

	def test_occupied_slot_is_not_returned(self):
		self.crear_cita(self.fecha_hora(time(9, 0)))

		franjas = obtener_franjas_disponibles(self.medico.pk, self.fecha)

		self.assertEqual([franja["hora"] for franja in franjas], ["09:30"])

	def test_slot_must_match_availability_interval(self):
		self.assertTrue(
			esta_en_intervalo_disponible(
				self.medico.pk,
				self.fecha_hora(time(9, 30)),
			)
		)
		self.assertFalse(
			esta_en_intervalo_disponible(
				self.medico.pk,
				self.fecha_hora(time(9, 15)),
			)
		)

	def test_booking_validation_rejects_time_outside_schedule(self):
		with self.assertRaisesMessage(
			ValidationError,
			"El médico no tiene disponibilidad en ese horario.",
		):
			validar_cita_programable(
				medico_id=self.medico.pk,
				fecha_hora=self.fecha_hora(time(10, 0)),
			)

	def test_booking_window_includes_boundary_and_rejects_later_time(self):
		ahora = timezone.make_aware(
			datetime(2026, 1, 1, 12, 0),
			timezone.get_current_timezone(),
		)
		ConfiguracionSistema.objects.create(semanas_agendamiento=2)
		limite = ahora + timedelta(weeks=2)

		with patch("appointments.services.timezone.now", return_value=ahora):
			self.assertTrue(fecha_dentro_de_ventana(limite))
			self.assertFalse(
				fecha_dentro_de_ventana(limite + timedelta(seconds=1))
			)
