import csv
from datetime import date, time
from io import StringIO
from types import SimpleNamespace
from unittest.mock import patch

from django.core.exceptions import ValidationError as DjangoValidationError
from django.test import TestCase
from rest_framework.test import APIRequestFactory, force_authenticate

from appointments.models import Cita, ConfiguracionSistema
from appointments.serializers import CitaSerializer
from appointments.views import (
	AgendaCitasView,
	AgendaMedicoView,
	CitaDetailView,
	CitaListCreateView,
	ConfiguracionSistemaView,
	ExportarAgendaCitasView,
	ExportarAgendaMedicoView,
	FechasDisponiblesView,
	FechasAgendaCitasView,
	HistorialPacienteView,
)
from persons.models import (
	Disponibilidad,
	Medico,
	MedicoDisponibilidad,
	Paciente,
	Persona,
)
from users.models import Rol, Usuario, UsuarioRol
from appointments.services import obtener_fechas_disponibles


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

	def test_patient_can_change_appointment_to_confirmed_or_cancelled(self):
		for hora, estado in (
			("10:00:00", "CONFIRMADA"),
			("11:00:00", "CANCELADA"),
		):
			cita = Cita.objects.create(
				usuario=self.usuario,
				paciente=self.paciente,
				medico=self.medico,
				fecha_hora=f"2099-01-05T{hora}Z",
			)
			request = APIRequestFactory().patch(
				f"/api/citas/{cita.pk}/",
				{"estado": estado},
				format="json",
			)
			force_authenticate(request, user=self.request.user)

			response = CitaDetailView.as_view()(request, pk=cita.pk)

			self.assertEqual(response.status_code, 200, response.data)
			self.assertEqual(response.data["estado"], estado)

	def test_patient_can_reschedule_own_appointment(self):
		cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)
		nueva_fecha_hora = "2099-01-06T10:00:00Z"
		request = APIRequestFactory().patch(
			f"/api/citas/{cita.pk}/",
			{
				"estado": "REAGENDADA",
				"fecha_hora": nueva_fecha_hora,
			},
			format="json",
		)
		force_authenticate(request, user=self.request.user)

		with patch("appointments.serializers.validar_cita_programable") as validar:
			response = CitaDetailView.as_view()(request, pk=cita.pk)

		self.assertEqual(response.status_code, 200, response.data)
		self.assertEqual(response.data["estado"], "REAGENDADA")
		self.assertEqual(
			response.data["fecha_hora"],
			"2099-01-06T05:00:00-05:00",
		)
		validar.assert_called_once()
		self.assertEqual(validar.call_args.kwargs["cita_id"], cita.pk)

	def test_reactivating_cancelled_appointment_checks_availability(self):
		cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
			estado="CANCELADA",
		)
		request = APIRequestFactory().patch(
			f"/api/citas/{cita.pk}/",
			{"estado": "CONFIRMADA"},
			format="json",
		)
		force_authenticate(request, user=self.request.user)

		with patch(
			"appointments.serializers.validar_cita_programable",
			side_effect=DjangoValidationError(
				"El médico ya tiene una cita en ese horario.",
			),
		) as validar:
			response = CitaDetailView.as_view()(request, pk=cita.pk)

		self.assertEqual(response.status_code, 400)
		validar.assert_called_once()
		self.assertEqual(validar.call_args.kwargs["cita_id"], cita.pk)
		self.assertIn("non_field_errors", response.data)

	def test_cancelled_appointment_releases_its_time_slot(self):
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
			estado="CANCELADA",
		)

		nueva_cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)

		self.assertEqual(nueva_cita.estado, "PROGRAMADA")

	def test_rescheduling_requires_a_new_future_date(self):
		cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)
		request = APIRequestFactory().patch(
			f"/api/citas/{cita.pk}/",
			{"estado": "REAGENDADA"},
			format="json",
		)
		force_authenticate(request, user=self.request.user)

		response = CitaDetailView.as_view()(request, pk=cita.pk)

		self.assertEqual(response.status_code, 400)
		self.assertIn("fecha_hora", response.data)

		request = APIRequestFactory().patch(
			f"/api/citas/{cita.pk}/",
			{
				"estado": "REAGENDADA",
				"fecha_hora": "2020-01-05T10:00:00Z",
			},
			format="json",
		)
		force_authenticate(request, user=self.request.user)

		response = CitaDetailView.as_view()(request, pk=cita.pk)

		self.assertEqual(response.status_code, 400)
		self.assertIn("fecha_hora", response.data)

	def test_patient_cannot_reschedule_to_the_current_date_or_another_patient(self):
		cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)
		request = APIRequestFactory().patch(
			f"/api/citas/{cita.pk}/",
			{
				"estado": "REAGENDADA",
				"fecha_hora": "2099-01-05T10:00:00Z",
			},
			format="json",
		)
		force_authenticate(request, user=self.request.user)

		response = CitaDetailView.as_view()(request, pk=cita.pk)

		self.assertEqual(response.status_code, 400)
		self.assertIn("fecha_hora", response.data)

		otra_cita = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T11:00:00Z",
		)
		request = APIRequestFactory().patch(
			f"/api/citas/{otra_cita.pk}/",
			{"estado": "CANCELADA"},
			format="json",
		)
		force_authenticate(request, user=self.request.user)

		response = CitaDetailView.as_view()(request, pk=otra_cita.pk)

		self.assertEqual(response.status_code, 404)

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

	def test_scheduler_can_search_patient_by_document_and_get_all_appointments(self):
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
			estado="ATENDIDA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-07T11:00:00Z",
			estado="CANCELADA",
		)
		request = APIRequestFactory().get(
			"/api/citas/agenda/paciente/",
			{"cedula": "10000001"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["AGENDADOR"], user_id="agendador-test"),
		)

		response = HistorialPacienteView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(response.data["paciente"]["nombre"], "Ana Paciente")
		self.assertEqual(response.data["paciente"]["cedula"], 10000001)
		self.assertEqual(response.data["cantidad"], 2)
		self.assertEqual(
			[appointment["estado"] for appointment in response.data["citas"]],
			["Cancelada", "Atendida"],
		)
		self.assertEqual(response.data["citas"][0]["medico"], "Carlos Medico")
		self.assertNotIn("observacion", response.data["citas"][0])

	def test_patient_history_rejects_invalid_or_unknown_documents(self):
		user = SimpleNamespace(roles=["AGENDADOR"], user_id="agendador-test")
		factory = APIRequestFactory()

		request = factory.get(
			"/api/citas/agenda/paciente/",
			{"cedula": "abc"},
		)
		force_authenticate(request, user=user)
		response = HistorialPacienteView.as_view()(request)
		self.assertEqual(response.status_code, 400)
		self.assertIn("debe contener", response.data["detail"])

		request = factory.get(
			"/api/citas/agenda/paciente/",
			{"cedula": "10999999"},
		)
		force_authenticate(request, user=user)
		response = HistorialPacienteView.as_view()(request)
		self.assertEqual(response.status_code, 404)
		self.assertIn("No se encontró", response.data["detail"])

	def test_patient_cannot_search_another_patients_history(self):
		request = APIRequestFactory().get(
			"/api/citas/agenda/paciente/",
			{"cedula": "10000001"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["PACIENTE"],
				user_id="paciente-test",
				is_authenticated=True,
			),
		)

		response = HistorialPacienteView.as_view()(request)

		self.assertEqual(response.status_code, 403)

	def test_doctor_can_search_patient_history_by_document(self):
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
			estado="ATENDIDA",
		)
		request = APIRequestFactory().get(
			"/api/citas/agenda/paciente/",
			{"cedula": "10000001"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["MEDICO"], user_id="medico-test"),
		)

		response = HistorialPacienteView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(response.data["paciente"]["cedula"], 10000001)
		self.assertEqual(response.data["cantidad"], 1)
		self.assertEqual(response.data["citas"][0]["estado"], "Atendida")

	def test_scheduler_calendar_dates_filter_by_optional_doctor_and_ignore_cancelled(self):
		other_persona = Persona.objects.create(
			primer_nombre="Mario",
			primer_apellido="Terapista",
			genero="HOMBRE",
			fecha_nacimiento=date(1988, 2, 1),
			telefono="3000000004",
			dni=90000003,
		)
		other_medico = Medico.objects.create(
			persona=other_persona,
			tipo_profesional="TERAPISTA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T10:00:00Z",
			estado="PROGRAMADA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=self.medico,
			fecha_hora="2099-01-07T10:00:00Z",
			estado="CANCELADA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=other_medico,
			fecha_hora="2099-01-07T11:00:00Z",
			estado="CONFIRMADA",
		)
		user = SimpleNamespace(roles=["AGENDADOR"], user_id="agendador-test")
		factory = APIRequestFactory()

		request = factory.get(
			"/api/citas/agenda/fechas/",
			{"mes": "2099-01", "medico": self.medico.pk},
		)
		force_authenticate(request, user=user)
		filtered_response = FechasAgendaCitasView.as_view()(request)

		request = factory.get(
			"/api/citas/agenda/fechas/",
			{"mes": "2099-01"},
		)
		force_authenticate(request, user=user)
		all_response = FechasAgendaCitasView.as_view()(request)

		self.assertEqual(filtered_response.status_code, 200)
		self.assertEqual(filtered_response.data["fechas"], ["2099-01-05"])
		self.assertEqual(all_response.data["fechas"], ["2099-01-05", "2099-01-07"])

	def test_scheduler_can_export_selected_doctor_and_date_as_csv(self):
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T15:00:00Z",
			estado="CONFIRMADA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T16:00:00Z",
			estado="CANCELADA",
		)
		request = APIRequestFactory().get(
			"/api/citas/agenda/exportar/",
			{"medico": self.medico.pk, "fecha": "2099-01-05"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["AGENDADOR"], user_id="agendador-test"),
		)

		response = ExportarAgendaCitasView.as_view()(request)
		rows = list(csv.reader(
			StringIO(response.content.decode("utf-8-sig")),
			delimiter=";",
		))

		self.assertEqual(response.status_code, 200)
		self.assertEqual(response["Content-Type"], "text/csv; charset=utf-8")
		self.assertIn("attachment;", response["Content-Disposition"])
		self.assertIn("citas-carlos-medico-2099-01-05.csv", response["Content-Disposition"])
		self.assertEqual(rows[0], ["Citas médico Carlos Medico - fecha 2099-01-05"])
		self.assertEqual(rows[1], [])
		self.assertEqual(rows[2], ["Fecha", "Hora", "Paciente", "Documento", "Teléfono", "Estado"])
		self.assertEqual(len(rows[3]), 6)
		self.assertIn("Ana Paciente", rows[3])
		self.assertIn("Confirmada", rows[3])

	def test_scheduler_csv_export_requires_doctor_and_valid_date(self):
		request = APIRequestFactory().get(
			"/api/citas/agenda/exportar/",
			{"medico": self.medico.pk, "fecha": "no-es-fecha"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["AGENDADOR"], user_id="agendador-test"),
		)

		response = ExportarAgendaCitasView.as_view()(request)

		self.assertEqual(response.status_code, 400)
		self.assertIn("YYYY-MM-DD", response.data["detail"])

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

	def test_doctor_sees_only_own_appointments_and_can_filter_by_date(self):
		other_persona_medico = Persona.objects.create(
			primer_nombre="Valentina",
			primer_apellido="Pabon",
			genero="MUJER",
			fecha_nacimiento=date(1990, 1, 1),
			telefono="3000000003",
			dni=90000002,
		)
		other_medico = Medico.objects.create(
			persona=other_persona_medico,
			tipo_profesional="MEDICO",
		)
		doctor_user = Usuario.objects.create(
			username="medico.valentina",
			keycloak_user_id="medico-valentina",
			persona=other_persona_medico,
		)
		doctor_role = Rol.objects.create(nombre="MEDICO")
		UsuarioRol.objects.create(usuario=doctor_user, rol=doctor_role)
		own_appointment = Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=other_medico,
			fecha_hora="2099-01-05T10:00:00Z",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T11:00:00Z",
		)
		request = APIRequestFactory().get(
			"/api/citas/medico/agenda/",
			{"fecha": "2099-01-05"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["MEDICO"],
				user_id="medico-valentina",
			),
		)

		response = AgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(response.data["cantidad"], 1)
		self.assertEqual(response.data["resultados"][0]["id"], own_appointment.id)

	def test_doctor_agenda_rejects_invalid_date(self):
		request = APIRequestFactory().get(
			"/api/citas/medico/agenda/",
			{"fecha": "2026-31-12"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["MEDICO"], user_id="medico-valentina"),
		)

		response = AgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 400)
		self.assertIn("YYYY-MM-DD", response.data["detail"])

	def test_non_doctor_cannot_access_doctor_agenda(self):
		request = APIRequestFactory().get("/api/citas/medico/agenda/")
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["PACIENTE"],
				user_id="paciente-test",
				is_authenticated=True,
			),
		)

		response = AgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 403)

	def test_doctor_without_linked_profile_gets_clear_not_found(self):
		request = APIRequestFactory().get("/api/citas/medico/agenda/")
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["MEDICO"], user_id="medico-sin-perfil"),
		)

		response = AgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 404)
		self.assertIn("vinculada a un perfil médico", response.data["detail"])

	@patch("appointments.views.obtener_fechas_disponibles")
	def test_available_dates_endpoint_returns_doctor_dates_for_month(
		self,
		obtener_fechas,
	):
		obtener_fechas.return_value = ["2099-01-05", "2099-01-12"]
		request = APIRequestFactory().get(
			"/api/citas/disponibles/fechas/",
			{"medico": self.medico.pk, "mes": "2099-01"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["PACIENTE"],
				user_id="paciente-test",
				is_authenticated=True,
			),
		)

		response = FechasDisponiblesView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(response.data["medico"], self.medico.pk)
		self.assertEqual(response.data["mes"], "2099-01")
		self.assertEqual(response.data["fechas"], obtener_fechas.return_value)
		obtener_fechas.assert_called_once_with(
			self.medico.pk,
			date(2099, 1, 1),
		)

	def test_available_dates_endpoint_rejects_invalid_month(self):
		request = APIRequestFactory().get(
			"/api/citas/disponibles/fechas/",
			{"medico": self.medico.pk, "mes": "2099-13"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["PACIENTE"],
				user_id="paciente-test",
				is_authenticated=True,
			),
		)

		response = FechasDisponiblesView.as_view()(request)

		self.assertEqual(response.status_code, 400)

	def test_available_dates_include_only_days_with_unoccupied_future_slots(self):
		disponibilidad = Disponibilidad.objects.create(
			dia_semana="MARTES",
			hora_inicio=time(9, 0),
			hora_fin=time(10, 0),
			intervalo=30,
		)
		MedicoDisponibilidad.objects.create(
			medico=self.medico,
			disponibilidad=disponibilidad,
		)
		for hora in ("14:00:00", "14:30:00"):
			Cita.objects.create(
				usuario=self.usuario,
				paciente=self.paciente,
				medico=self.medico,
				fecha_hora=f"2099-01-06T{hora}Z",
			)

		fechas = obtener_fechas_disponibles(
			self.medico.pk,
			date(2099, 1, 1),
		)

		self.assertNotIn("2099-01-06", fechas)
		self.assertEqual(fechas, ["2099-01-13", "2099-01-20", "2099-01-27"])

	def test_doctor_can_export_only_own_appointments_in_csv(self):
		doctor_persona = Persona.objects.create(
			primer_nombre="Valentina",
			primer_apellido="Pabon",
			genero="MUJER",
			fecha_nacimiento=date(1990, 1, 1),
			telefono="3000000003",
			dni=90000002,
		)
		doctor = Medico.objects.create(
			persona=doctor_persona,
			tipo_profesional="MEDICO",
		)
		Usuario.objects.create(
			username="medico.valentina",
			keycloak_user_id="medico-valentina-export",
			persona=doctor_persona,
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.paciente,
			medico=doctor,
			fecha_hora="2099-01-05T10:00:00Z",
			estado="PROGRAMADA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=doctor,
			fecha_hora="2099-01-05T11:00:00Z",
			estado="CANCELADA",
		)
		Cita.objects.create(
			usuario=self.usuario,
			paciente=self.otro_paciente,
			medico=self.medico,
			fecha_hora="2099-01-05T12:00:00Z",
			estado="CONFIRMADA",
		)
		request = APIRequestFactory().get(
			"/api/citas/medico/agenda/exportar/",
			{"fecha": "2099-01-05"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(
				roles=["MEDICO"],
				user_id="medico-valentina-export",
			),
		)

		response = ExportarAgendaMedicoView.as_view()(request)
		rows = list(csv.reader(
			StringIO(response.content.decode("utf-8-sig")),
			delimiter=";",
		))

		self.assertEqual(response.status_code, 200)
		self.assertEqual(
			response["Content-Disposition"],
			'attachment; filename="citas-valentina-pabon-2099-01-05.csv"',
		)
		self.assertEqual(rows[0], ["Citas médico Valentina Pabon - fecha 2099-01-05"])
		self.assertEqual(rows[2], ["Fecha", "Hora", "Paciente", "Documento", "Teléfono", "Estado"])
		self.assertEqual(len(rows), 4)
		self.assertIn("Ana Paciente", rows[3])
		self.assertNotIn("Laura Paciente", rows[3])

	def test_doctor_export_requires_linked_profile_and_valid_date(self):
		request = APIRequestFactory().get(
			"/api/citas/medico/agenda/exportar/",
			{"fecha": "2099-13-05"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["MEDICO"], user_id="doctor-sin-perfil"),
		)

		response = ExportarAgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 400)
		self.assertIn("YYYY-MM-DD", response.data["detail"])

		request = APIRequestFactory().get(
			"/api/citas/medico/agenda/exportar/",
			{"fecha": "2099-01-05"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["MEDICO"], user_id="doctor-sin-perfil"),
		)
		response = ExportarAgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 404)
		self.assertIn("vinculada a un perfil médico", response.data["detail"])

	def test_non_doctor_cannot_export_doctor_agenda(self):
		request = APIRequestFactory().get(
			"/api/citas/medico/agenda/exportar/",
			{"fecha": "2099-01-05"},
		)
		force_authenticate(
			request,
			user=SimpleNamespace(roles=["PACIENTE"], user_id="paciente-test"),
		)

		response = ExportarAgendaMedicoView.as_view()(request)

		self.assertEqual(response.status_code, 403)

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
