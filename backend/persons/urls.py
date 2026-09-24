from django.urls import path

from persons.views import (
    PacienteDetailView,
    PacienteListCreateView,
)

from persons.views import (
    EspecialidadListCreateView,
    MedicoDetailView,
    MedicoListCreateView,
)

from persons.views import (
    PersonaDetailView,
    PersonaListCreateView,
)

from persons.views import (
    DisponibilidadDetailView,
    DisponibilidadListCreateView,
    MedicoDisponibilidadDetailView,
    MedicoDisponibilidadListCreateView,
)

urlpatterns = [

    path(
        "personas/",
        PersonaListCreateView.as_view(),
        name="persona-list-create",
    ),
    path(
        "personas/<int:pk>/",
        PersonaDetailView.as_view(),
        name="persona-detail",
    ),
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

    path(
        "especialidades/",
        EspecialidadListCreateView.as_view(),
        name="especialidad-list-create",
    ),
    path(
        "medicos/",
        MedicoListCreateView.as_view(),
        name="medico-list-create",
    ),
    path(
        "medicos/<int:pk>/",
        MedicoDetailView.as_view(),
        name="medico-detail",
    ), 
    path(
    "disponibilidades/",
    DisponibilidadListCreateView.as_view(),
    name="disponibilidad-list-create",
    ),
    path(
    "disponibilidades/<int:pk>/",
    DisponibilidadDetailView.as_view(),
    name="disponibilidad-detail",
    ),
    path(
    "medico-disponibilidades/",
    MedicoDisponibilidadListCreateView.as_view(),
    name="medico-disponibilidad-list-create",
    ),
    path(
    "medico-disponibilidades/<int:pk>/",
    MedicoDisponibilidadDetailView.as_view(),
    name="medico-disponibilidad-detail",
    ),   
]