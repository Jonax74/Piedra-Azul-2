from datetime import date

from django.test import TestCase

from persons.models import Paciente, Persona
from persons.serializers import PacienteSerializer
from users.models import Rol, Usuario, UsuarioRol


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
