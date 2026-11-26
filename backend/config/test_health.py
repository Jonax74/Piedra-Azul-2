from unittest.mock import patch

from django.db import OperationalError
from django.test import TestCase


class HealthEndpointsTests(TestCase):
    def test_health_responde_sin_token(self):
        response = self.client.get("/api/health/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_ready_responde_ok_con_base_de_datos(self):
        response = self.client.get("/api/health/ready/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_ready_devuelve_503_si_falla_la_base_de_datos(self):
        with patch("config.health.connection") as mock_connection:
            mock_connection.cursor.side_effect = OperationalError("falla simulada")
            response = self.client.get("/api/health/ready/")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json(), {"status": "error"})