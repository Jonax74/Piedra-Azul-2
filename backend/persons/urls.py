from django.urls import path

from persons.views import (
    PacienteDetailView,
    PacienteListCreateView,
)

urlpatterns = [
    path(
        "pacientes/",
        PacienteListCreateView.as_view(),
        name="paciente-list-create",
    ),
    path(
        "pacientes/<int:pk>/",
        PacienteDetailView.as_view(),
        name="paciente-detail",
    ),
]