from django.urls import path

from appointments.views import (
    CitaDetailView,
    CitaListCreateView,
)

urlpatterns = [
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