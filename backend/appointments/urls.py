from django.urls import path

from appointments.views import (
    AgendaCitasView,
    CitaDetailView,
    CitaListCreateView,
    FranjasDisponiblesView,
    ConfiguracionSistemaView,
)

urlpatterns = [
    path(
        "configuracion/",
        ConfiguracionSistemaView.as_view(),
        name="configuracion-sistema",
    ),
    path(
        "citas/agenda/",
        AgendaCitasView.as_view(),
        name="cita-agenda",
    ),
    path(
        "citas/disponibles/",
        FranjasDisponiblesView.as_view(),
        name="cita-franjas-disponibles",
    ),
    path(
        "citas/",
        CitaListCreateView.as_view(),
        name="cita-list-create",
    ),
    path(
        "citas/<int:pk>/",
        CitaDetailView.as_view(),
        name="cita-detail",
    ),
]