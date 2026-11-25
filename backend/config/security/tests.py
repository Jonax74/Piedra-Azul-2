from types import SimpleNamespace
from unittest.mock import patch

import jwt
from django.core.exceptions import ImproperlyConfigured
from django.test import SimpleTestCase
from rest_framework.exceptions import AuthenticationFailed

from config.security.keycloak_authentication import KeycloakJWTAuthentication


class KeycloakJWTAuthenticationTests(SimpleTestCase):
	def setUp(self):
		self.authentication = KeycloakJWTAuthentication()

	def request(self, authorization=None):
		headers = {}
		if authorization is not None:
			headers["Authorization"] = authorization
		return SimpleNamespace(headers=headers)

	def test_ignores_requests_without_bearer_token(self):
		self.assertIsNone(self.authentication.authenticate(self.request()))

	def test_rejects_empty_bearer_token(self):
		with self.assertRaisesMessage(
			AuthenticationFailed,
			"Token Bearer vacío.",
		):
			self.authentication.authenticate(self.request("Bearer   "))

	def test_requires_configured_issuer_for_bearer_token(self):
		with patch(
			"config.security.keycloak_authentication.os.getenv",
			return_value=None,
		):
			with self.assertRaises(ImproperlyConfigured):
				self.authentication.authenticate(self.request("Bearer abc"))

	@patch.dict("os.environ", {"KEYCLOAK_ISSUER": "https://keycloak.test/realm"})
	@patch("config.security.keycloak_authentication.jwt.decode")
	@patch("config.security.keycloak_authentication.jwt.PyJWKClient")
	def test_validates_and_maps_keycloak_claims(self, jwks_client, decode):
		jwks_client.return_value.get_signing_key_from_jwt.return_value = (
			SimpleNamespace(key="public-key")
		)
		decode.return_value = {
			"sub": "keycloak-user-id",
			"preferred_username": "ana",
			"realm_access": {"roles": ["PACIENTE"]},
		}

		user, token = self.authentication.authenticate(
			self.request("Bearer signed-token")
		)

		self.assertEqual(token, "signed-token")
		self.assertEqual(user.user_id, "keycloak-user-id")
		self.assertEqual(user.username, "ana")
		self.assertEqual(user.roles, ["PACIENTE"])
		self.assertTrue(user.is_authenticated)
		self.assertFalse(user.is_anonymous)
		decode.assert_called_once()

	@patch.dict("os.environ", {"KEYCLOAK_ISSUER": "https://keycloak.test/realm"})
	@patch("config.security.keycloak_authentication.jwt.decode")
	@patch("config.security.keycloak_authentication.jwt.PyJWKClient")
	def test_rejects_invalid_jwt(self, jwks_client, decode):
		jwks_client.return_value.get_signing_key_from_jwt.return_value = (
			SimpleNamespace(key="public-key")
		)
		decode.side_effect = jwt.InvalidTokenError("invalid token")

		with self.assertRaisesMessage(
			AuthenticationFailed,
			"Token de Keycloak inválido.",
		):
			self.authentication.authenticate(self.request("Bearer invalid"))
