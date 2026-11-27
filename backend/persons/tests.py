from datetime import date
from types import SimpleNamespace
from unittest.mock import patch

from django.test import TestCase
from rest_framework.test import APIRequestFactory, force_authenticate

from persons.models import Medico, Paciente, Persona
from persons.serializers import MedicoSerializer, PacienteSerializer
from persons.views import MedicoListCreateView, PacienteListCreateView
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
