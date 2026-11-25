from django.db import connection
from django.http import JsonResponse


def health(request):
    """Liveness: el proceso responde. No toca la base de datos ni pide token."""
    return JsonResponse({"status": "ok"})


def ready(request):
    """Readiness: además verifica que PostgreSQL responde."""
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
    except Exception:
        # No se devuelve el detalle del error para no filtrar información interna.
        return JsonResponse({"status": "error"}, status=503)
    return JsonResponse({"status": "ok"})