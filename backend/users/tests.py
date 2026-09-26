from django.test import TestCase

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
