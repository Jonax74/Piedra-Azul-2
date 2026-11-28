from datetime import date
from types import SimpleNamespace
from unittest.mock import patch

from django.test import TestCase
from rest_framework.test import APIRequestFactory, force_authenticate

from persons.models import (
	Disponibilidad,
	Especialidad,
	Medico,
	MedicoDisponibilidad,
	Paciente,
	Persona,
)
from persons.serializers import MedicoSerializer, PacienteSerializer
from persons.views import (
	DisponibilidadDetailView,
	DisponibilidadListCreateView,
	MedicoDisponibilidadDetailView,
	MedicoDisponibilidadListCreateView,
	MedicoListCreateView,
	PacienteListCreateView,
)
from users.models import Rol, Usuario, UsuarioRol


class PersonaSearchTests(TestCase):
	def setUp(self):
		self.factory = APIRequestFactory()
		self.patient_persona = Persona.objects.create(
			primer_nombre="Ana",
			segundo_nombre="Maria",
			primer_apellido="Torres",
			segundo_apellido="Lopez",
			genero="MUJER",
			fecha_nacimiento=date(1995, 4, 20),
			telefono="3001234567",
			dni=987654321,
			correo="ana@example.com",
		)
		Paciente.objects.create(persona=self.patient_persona)
		self.medico_persona = Persona.objects.create(
			primer_nombre="Carlos",
			primer_apellido="Gomez",
			genero="HOMBRE",
			fecha_nacimiento=date(1985, 1, 1),
			telefono="3007654321",
			dni=123456789,
			correo="carlos@example.com",
		)
		Medico.objects.create(
			persona=self.medico_persona,
			tipo_profesional="MEDICO",
			estado="ACTIVO",
		)
		self.user = SimpleNamespace(
			roles=["AGENDADOR"],
			user_id="agendador-test",
			is_authenticated=True,
		)

	def test_patient_can_be_filtered_by_exact_dni(self):
		request = self.factory.get("/api/pacientes/", {"dni": "987654321"})
		force_authenticate(request, user=self.user)
		response = PacienteListCreateView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(len(response.data), 1)
		self.assertEqual(response.data[0]["persona"]["dni"], 987654321)

	def test_patient_can_be_searched_by_name_or_partial_dni(self):
		request = self.factory.get("/api/pacientes/", {"q": "Ana"})
		force_authenticate(request, user=self.user)
		response = PacienteListCreateView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(len(response.data), 1)
		self.assertEqual(response.data[0]["persona"]["primer_nombre"], "Ana")

	def test_medico_can_be_filtered_by_exact_dni(self):
		request = self.factory.get("/api/medicos/", {"dni": "123456789"})
		force_authenticate(request, user=self.user)
		response = MedicoListCreateView.as_view()(request)

		self.assertEqual(response.status_code, 200)
		self.assertEqual(len(response.data), 1)
		self.assertEqual(response.data[0]["persona"], self.medico_persona.id)


class AvailabilityWritePermissionTests(TestCase):
	def setUp(self):
		self.factory = APIRequestFactory()
		self.persona = Persona.objects.create(
			primer_nombre="Carlos",
			primer_apellido="Gomez",
			genero="HOMBRE",
			fecha_nacimiento=date(1985, 1, 1),
			telefono="3007654321",
			dni=123456789,
		)
		self.medico = Medico.objects.create(
			persona=self.persona,
			tipo_profesional="MEDICO",
			estado="ACTIVO",
		)
		self.disponibilidad = Disponibilidad.objects.create(
			dia_semana="LUNES",
			hora_inicio="09:00",
			hora_fin="12:00",
			intervalo=30,
		)
		self.relacion = MedicoDisponibilidad.objects.create(
			medico=self.medico,
			disponibilidad=self.disponibilidad,
		)
		self.admin = SimpleNamespace(
			roles=["ADMIN"],
			is_authenticated=True,
		)
		self.doctor = SimpleNamespace(
			roles=["MEDICO"],
			is_authenticated=True,
		)

	def request(self, method, path, view, user, data=None, pk=None):
		request = getattr(self.factory, method.lower())(
			path,
			data=data,
			format="json",
		)
		force_authenticate(request, user=user)
		if pk is None:
			return view.as_view()(request)
		return view.as_view()(request, pk=pk)

	def test_doctor_can_read_but_cannot_create_or_modify_availability(self):
		response = self.request(
			"get",
			"/api/disponibilidades/",
			DisponibilidadListCreateView,
			self.doctor,
		)
		self.assertEqual(response.status_code, 200)

		for method, view, path, data, pk in (
			(
				"post",
				DisponibilidadListCreateView,
				"/api/disponibilidades/",
				{
					"dia_semana": "MARTES",
					"hora_inicio": "09:00",
					"hora_fin": "12:00",
					"intervalo": 30,
				},
				None,
			),
			(
				"patch",
				DisponibilidadDetailView,
				f"/api/disponibilidades/{self.disponibilidad.pk}/",
				{"hora_fin": "13:00"},
				self.disponibilidad.pk,
			),
			(
				"delete",
				DisponibilidadDetailView,
				f"/api/disponibilidades/{self.disponibilidad.pk}/",
				None,
				self.disponibilidad.pk,
			),
			(
				"post",
				MedicoDisponibilidadListCreateView,
				"/api/medico-disponibilidades/",
				{
					"medico": self.medico.pk,
					"disponibilidad": self.disponibilidad.pk,
				},
				None,
			),
			(
				"delete",
				MedicoDisponibilidadDetailView,
				f"/api/medico-disponibilidades/{self.relacion.pk}/",
				None,
				self.relacion.pk,
			),
		):
			with self.subTest(method=method, path=path):
				response = self.request(
					method,
					path,
					view,
					self.doctor,
					data=data,
					pk=pk,
				)
				self.assertEqual(response.status_code, 403)

	def test_admin_can_create_availability_and_associate_it(self):
		response = self.request(
			"post",
			"/api/disponibilidades/",
			DisponibilidadListCreateView,
			self.admin,
			data={
				"dia_semana": "MARTES",
				"hora_inicio": "09:00",
				"hora_fin": "12:00",
				"intervalo": 30,
			},
		)
		self.assertEqual(response.status_code, 201)
		disponibilidad_id = response.data["id"]

		response = self.request(
			"post",
			"/api/medico-disponibilidades/",
			MedicoDisponibilidadListCreateView,
			self.admin,
			data={
				"medico": self.medico.pk,
				"disponibilidad": disponibilidad_id,
			},
		)
		self.assertEqual(response.status_code, 201)
		nueva_relacion = MedicoDisponibilidad.objects.get(
			disponibilidad_id=disponibilidad_id,
		)

		response = self.request(
			"put",
			f"/api/disponibilidades/{disponibilidad_id}/",
			DisponibilidadDetailView,
			self.admin,
			data={
				"dia_semana": "MARTES",
				"hora_inicio": "09:00",
				"hora_fin": "13:00",
				"intervalo": 30,
			},
			pk=disponibilidad_id,
		)
		self.assertEqual(response.status_code, 200)

		response = self.request(
			"delete",
			f"/api/medico-disponibilidades/{nueva_relacion.pk}/",
			MedicoDisponibilidadDetailView,
			self.admin,
			pk=nueva_relacion.pk,
		)
		self.assertEqual(response.status_code, 204)

		response = self.request(
			"delete",
			f"/api/disponibilidades/{disponibilidad_id}/",
			DisponibilidadDetailView,
			self.admin,
			pk=disponibilidad_id,
		)
		self.assertEqual(response.status_code, 204)


class PersonaUsuarioLinkTests(TestCase):
	def setUp(self):
		self.usuario = Usuario.objects.create(
			username="paciente.test",
			keycloak_user_id="test-paciente",
		)
		rol = Rol.objects.create(nombre="PACIENTE")
		UsuarioRol.objects.create(usuario=self.usuario, rol=rol)

	def persona_data(self, dni):
		return {
			"primer_nombre": "Paciente",
			"primer_apellido": "Prueba",
			"genero": "OTRO",
			"fecha_nacimiento": date(1990, 1, 1),
			"telefono": "3000000000",
			"dni": dni,
		}

	def test_creates_patient_with_optional_user_link(self):
		serializer = PacienteSerializer(
			data={
				"persona": self.persona_data(12345678),
				"usuario_id": self.usuario.id,
			},
		)

		self.assertTrue(serializer.is_valid(), serializer.errors)
		paciente = serializer.save()

		self.assertEqual(paciente.persona.usuario, self.usuario)

	def test_rejects_user_with_wrong_role(self):
		medico_user = Usuario.objects.create(
			username="medico.test",
			keycloak_user_id="test-medico",
		)
		rol = Rol.objects.create(nombre="MEDICO")
		UsuarioRol.objects.create(usuario=medico_user, rol=rol)

		serializer = PacienteSerializer(
			data={
				"persona": self.persona_data(87654321),
				"usuario_id": medico_user.id,
			},
		)

		self.assertFalse(serializer.is_valid())
		self.assertIn("usuario_id", serializer.errors)

	def test_rejects_medico_link_with_wrong_role(self):
		persona = Persona.objects.create(**self.persona_data(23456789))
		serializer = MedicoSerializer(
			data={
				"persona": persona.pk,
				"tipo_profesional": "MEDICO",
				"usuario_id": self.usuario.pk,
			},
		)

		self.assertFalse(serializer.is_valid())
		self.assertIn("usuario_id", serializer.errors)

	def test_creates_medico_and_persona_atomically_from_persona_data(self):
		serializer = MedicoSerializer(
			data={
				"persona_data": {
					"primer_nombre": "Medico",
					"primer_apellido": "Prueba",
					"genero": "OTRO",
					"fecha_nacimiento": date(1985, 1, 1),
					"telefono": "3000000001",
					"dni": 98765432,
				},
				"tipo_profesional": "MEDICO",
				"estado": "ACTIVO",
			},
		)

		self.assertTrue(serializer.is_valid(), serializer.errors)
		medico = serializer.save()

		self.assertEqual(medico.persona.primer_nombre, "Medico")
		self.assertEqual(Medico.objects.count(), 1)

	@patch(
		"users.registration._create_keycloak_user",
		return_value=("keycloak-medico-nuevo", "test-token"),
	)
	def test_creates_medico_with_new_user_account(self, create_user):
		fisioterapia = Especialidad.objects.get(nombre="Fisioterapia")
		persona_data = {
			"primer_nombre": "Medico",
			"primer_apellido": "Prueba",
			"genero": "OTRO",
			"fecha_nacimiento": date(1985, 1, 1),
			"telefono": "3000000001",
			"dni": 98765432,
			"correo": "medico@example.com",
		}
		serializer = MedicoSerializer(
			data={
				"persona_data": persona_data,
				"tipo_profesional": "MEDICO",
				"estado": "ACTIVO",
				"especialidad_ids": [fisioterapia.pk],
				"username": "medico.prueba",
				"password": "ClaveSegura123",
				"password_confirmation": "ClaveSegura123",
			},
		)

		self.assertTrue(serializer.is_valid(), serializer.errors)
		medico = serializer.save()
		usuario = Usuario.objects.get(username="medico.prueba")

		self.assertEqual(usuario.persona_id, medico.persona_id)
		self.assertTrue(
			usuario.relaciones_rol.filter(rol__nombre="MEDICO").exists()
		)
		self.assertEqual(
			list(medico.especialidades.values_list("especialidad__nombre", flat=True)),
			["Fisioterapia"],
		)
		create_user.assert_called_once()

	@patch("users.registration._create_keycloak_user")
	def test_rejects_mismatched_medico_account_passwords(self, create_user):
		serializer = MedicoSerializer(
			data={
				"persona_data": {
					**self.persona_data(98765432),
					"correo": "medico@example.com",
				},
				"tipo_profesional": "MEDICO",
				"username": "medico.prueba",
				"password": "ClaveSegura123",
				"password_confirmation": "OtraClave123",
			},
		)

		self.assertFalse(serializer.is_valid())
		self.assertIn("password_confirmation", serializer.errors)
		create_user.assert_not_called()

	def test_rejects_medico_account_with_short_or_invalid_username(self):
		for username in ("ab", "medico prueba"):
			with self.subTest(username=username):
				serializer = MedicoSerializer(
					data={
						"persona_data": {
							**self.persona_data(98765432),
							"correo": "medico@example.com",
						},
						"tipo_profesional": "MEDICO",
						"username": username,
						"password": "ClaveSegura123",
						"password_confirmation": "ClaveSegura123",
					},
				)

				self.assertFalse(serializer.is_valid())
				self.assertIn("username", serializer.errors)

	def test_rejects_medico_account_with_invalid_phone_format(self):
		serializer = MedicoSerializer(
			data={
				"persona_data": {
					**self.persona_data(98765432),
					"telefono": "abcdefgh",
					"correo": "medico@example.com",
				},
				"tipo_profesional": "MEDICO",
				"username": "medico.prueba",
				"password": "ClaveSegura123",
				"password_confirmation": "ClaveSegura123",
			},
		)

		self.assertFalse(serializer.is_valid())
		self.assertIn("telefono", serializer.errors["persona_data"])

	def test_rejects_medico_account_password_over_maximum_length(self):
		password = "x" * 129
		serializer = MedicoSerializer(
			data={
				"persona_data": {
					**self.persona_data(98765432),
					"correo": "medico@example.com",
				},
				"tipo_profesional": "MEDICO",
				"username": "medico.prueba",
				"password": password,
				"password_confirmation": password,
			},
		)

		self.assertFalse(serializer.is_valid())
		self.assertIn("password", serializer.errors)
		self.assertIn("password_confirmation", serializer.errors)

	def test_medico_requires_existing_person_or_persona_data(self):
		serializer = MedicoSerializer(
			data={
				"tipo_profesional": "MEDICO",
				"estado": "ACTIVO",
			},
		)

		self.assertFalse(serializer.is_valid())
		self.assertIn("persona_data", serializer.errors)

	def test_updating_patient_user_link_moves_the_relationship(self):
		persona = Persona.objects.create(**self.persona_data(34567890))
		paciente = Paciente.objects.create(persona=persona)
		self.usuario.persona = persona
		self.usuario.save(update_fields=["persona"])
		nuevo_usuario = Usuario.objects.create(
			username="paciente.nuevo",
			keycloak_user_id="test-paciente-nuevo",
		)
		rol = Rol.objects.get(nombre="PACIENTE")
		UsuarioRol.objects.create(usuario=nuevo_usuario, rol=rol)

		serializer = PacienteSerializer(
			instance=paciente,
			data={"usuario_id": nuevo_usuario.pk},
			partial=True,
		)

		self.assertTrue(serializer.is_valid(), serializer.errors)
		serializer.save()
		self.usuario.refresh_from_db()
		nuevo_usuario.refresh_from_db()

		self.assertIsNone(self.usuario.persona_id)
		self.assertEqual(nuevo_usuario.persona_id, persona.pk)
