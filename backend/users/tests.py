from django.test import TestCase

from rest_framework.test import APIClient
from unittest.mock import patch

from persons.models import Paciente, Persona
from users.models import Usuario
from users.services import sincronizar_usuario_keycloak


class SincronizarUsuarioKeycloakTests(TestCase):
	def test_creates_user_and_ignores_unknown_roles(self):
		usuario = sincronizar_usuario_keycloak(
			keycloak_user_id="keycloak-user-1",
			username="usuario-con-nombre-largo",
			roles=["ADMIN", "ROL_DESCONOCIDO"],
		)

		self.assertEqual(usuario.username, "usuario-con-nombre-l")
		self.assertEqual(usuario.estado, "ACTIVO")
		self.assertEqual(
			list(
				usuario.relaciones_rol
				.order_by("rol__nombre")
				.values_list("rol__nombre", flat=True)
			),
			["ADMIN"],
		)

	def test_updates_existing_user_and_replaces_old_roles(self):
		usuario = sincronizar_usuario_keycloak(
			keycloak_user_id="keycloak-user-2",
			username="paciente",
			roles=["PACIENTE"],
		)
		usuario.estado = "INACTIVO"
		usuario.save(update_fields=["estado"])

		usuario_actualizado = sincronizar_usuario_keycloak(
			keycloak_user_id="keycloak-user-2",
			username="administrador",
			roles=["ADMIN"],
		)

		self.assertEqual(usuario_actualizado.pk, usuario.pk)
		self.assertEqual(usuario_actualizado.username, "administrador")
		self.assertEqual(usuario_actualizado.estado, "ACTIVO")
		self.assertEqual(
			list(
				usuario_actualizado.relaciones_rol
				.values_list("rol__nombre", flat=True)
			),
			["ADMIN"],
		)

	def test_removes_existing_roles_when_keycloak_returns_none(self):
		usuario = sincronizar_usuario_keycloak(
			keycloak_user_id="keycloak-user-3",
			username="usuario",
			roles=["PACIENTE"],
		)

		sincronizar_usuario_keycloak(
			keycloak_user_id="keycloak-user-3",
			username="usuario",
			roles=[],
		)

		self.assertFalse(usuario.relaciones_rol.exists())


class RegistroPacienteApiTests(TestCase):
	def setUp(self):
		self.client = APIClient()
		self.payload = {
			"username": "paciente.nuevo",
			"password": "ClaveSegura123",
			"password_confirmation": "ClaveSegura123",
			"persona": {
				"primer_nombre": "Ana",
				"segundo_nombre": "Maria",
				"primer_apellido": "Torres",
				"segundo_apellido": "Lopez",
				"genero": "MUJER",
				"fecha_nacimiento": "1995-04-20",
				"telefono": "3001234567",
				"dni": 987654321,
				"correo": "ana.torres@example.com",
			},
		}

	@patch(
		"users.registration._create_keycloak_user",
		return_value=("keycloak-paciente-nuevo", "test-token"),
	)
	def test_registration_creates_linked_patient_and_role(self, create_user):
		response = self.client.post(
			"/api/registro/",
			self.payload,
			format="json",
		)

		self.assertEqual(response.status_code, 201)
		usuario = Usuario.objects.get(username="paciente.nuevo")
		paciente = Paciente.objects.get(persona=usuario.persona)
		self.assertEqual(paciente.persona.dni, 987654321)
		self.assertEqual(usuario.keycloak_user_id, "keycloak-paciente-nuevo")
		self.assertTrue(
			usuario.relaciones_rol.filter(rol__nombre="PACIENTE").exists()
		)
		self.assertNotIn("password", response.data)
		create_user.assert_called_once()

	@patch("users.registration._create_keycloak_user")
	def test_registration_rejects_mismatched_passwords_before_keycloak(self, create_user):
		self.payload["password_confirmation"] = "OtraClave123"

		response = self.client.post(
			"/api/registro/",
			self.payload,
			format="json",
		)

		self.assertEqual(response.status_code, 400)
		self.assertIn("password_confirmation", response.data)
		self.assertFalse(Persona.objects.exists())
		create_user.assert_not_called()

	@patch("users.registration._create_keycloak_user")
	def test_registration_explains_short_password_before_keycloak(self, create_user):
		self.payload["password"] = "corta"
		self.payload["password_confirmation"] = "corta"

		response = self.client.post(
			"/api/registro/",
			self.payload,
			format="json",
		)

		self.assertEqual(response.status_code, 400)
		self.assertEqual(
			response.data["password"][0],
			"La contraseña debe tener al menos 8 caracteres.",
		)
		create_user.assert_not_called()

	def test_local_frontend_origins_are_allowed_to_read_api_responses(self):
		for origin in ("http://localhost:4200", "http://127.0.0.1:4200"):
			with self.subTest(origin=origin):
				response = self.client.options(
					"/api/registro/",
					HTTP_ORIGIN=origin,
					HTTP_ACCESS_CONTROL_REQUEST_METHOD="POST",
					HTTP_ACCESS_CONTROL_REQUEST_HEADERS="content-type",
				)

				self.assertEqual(
					response.headers.get("Access-Control-Allow-Origin"),
					origin,
				)
