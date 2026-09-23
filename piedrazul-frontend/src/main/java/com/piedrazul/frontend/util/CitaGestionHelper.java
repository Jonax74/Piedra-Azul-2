package com.piedrazul.frontend.util;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.dto.request.MarcarAsistenciaRequest;
import com.piedrazul.frontend.dto.request.ReagendarCitaRequest;
import com.piedrazul.frontend.dto.response.CitaResponse;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.time.LocalDateTime;
import java.util.Optional;

public class CitaGestionHelper {

    private final CitaClient citaClient = new CitaClient();
    private final Node owner;
    private final Runnable refrescarTabla;

    public CitaGestionHelper(Node owner, Runnable refrescarTabla) {
        this.owner = owner;
        this.refrescarTabla = refrescarTabla;
    }

    public void configurarSeleccion(
            CitaResponse cita,
            Button btnCancelar,
            Button btnReagendar,
            Button btnMarcarAtendida,
            Button btnMarcarNoAsistida,
            Label lblHint
    ) {
        boolean haySeleccion = cita != null;
        btnCancelar.setDisable(!haySeleccion || !CitaEstadoAcciones.puedeEjecutar(
                CitaEstadoAcciones.Accion.CANCELAR, cita));
        btnReagendar.setDisable(!haySeleccion || !CitaEstadoAcciones.puedeEjecutar(
                CitaEstadoAcciones.Accion.REAGENDAR, cita));
        btnMarcarAtendida.setDisable(!haySeleccion || !CitaEstadoAcciones.puedeEjecutar(
                CitaEstadoAcciones.Accion.MARCAR_ATENDIDA, cita));
        btnMarcarNoAsistida.setDisable(!haySeleccion || !CitaEstadoAcciones.puedeEjecutar(
                CitaEstadoAcciones.Accion.MARCAR_NO_ASISTIDA, cita));

        if (lblHint != null) {
            lblHint.setText(CitaEstadoAcciones.hintAcciones(cita));
        }
    }

    public void cancelar(CitaResponse cita) {
        if (!validarAccion(CitaEstadoAcciones.Accion.CANCELAR, cita)) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cancelar cita");
        dialog.setHeaderText("Indique el motivo de la cancelación");
        dialog.setContentText("Motivo:");
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }

        Optional<String> motivo = dialog.showAndWait();
        if (motivo.isEmpty() || motivo.get().isBlank()) {
            PiedrazulDialog.showWarning(owner, "Motivo requerido",
                    "Debe indicar un motivo para cancelar la cita.");
            return;
        }

        try {
            citaClient.cancelarCita(cita.getId(), motivo.get().trim());
            PiedrazulDialog.showInfo(owner, "Cita cancelada",
                    "La cita fue cancelada correctamente. La franja horaria quedó disponible nuevamente.");
            refrescarTabla.run();
        } catch (Exception e) {
            PiedrazulDialog.showWarning(owner, "No se pudo cancelar", e.getMessage());
        }
    }

    public void reagendar(CitaResponse cita) {
        if (!validarAccion(CitaEstadoAcciones.Accion.REAGENDAR, cita)) {
            return;
        }

        Optional<LocalDateTime> nuevaFecha = ReagendarCitaDialog.solicitarNuevaFecha(owner, cita);
        if (nuevaFecha.isEmpty()) {
            return;
        }

        try {
            citaClient.reagendarCita(new ReagendarCitaRequest(cita.getId(), nuevaFecha.get()));
            String mensaje = "Atendida".equalsIgnoreCase(cita.getEstado())
                    ? "Se creó una nueva cita de seguimiento correctamente."
                    : "La cita fue reagendada correctamente y la franja anterior quedó liberada.";
            PiedrazulDialog.showInfo(owner, "Cita reagendada", mensaje);
            refrescarTabla.run();
        } catch (Exception e) {
            PiedrazulDialog.showWarning(owner, "No se pudo reagendar", e.getMessage());
        }
    }

    public void marcarAtendida(CitaResponse cita) {
        if (!validarAccion(CitaEstadoAcciones.Accion.MARCAR_ATENDIDA, cita)) {
            return;
        }

        try {
            citaClient.marcarAsistencia(new MarcarAsistenciaRequest(cita.getId(), true));
            PiedrazulDialog.showInfo(owner, "Asistencia registrada",
                    "La cita fue marcada como Atendida correctamente.");
            refrescarTabla.run();
        } catch (Exception e) {
            PiedrazulDialog.showWarning(owner, "No se pudo registrar", e.getMessage());
        }
    }

    public void marcarNoAsistida(CitaResponse cita) {
        if (!validarAccion(CitaEstadoAcciones.Accion.MARCAR_NO_ASISTIDA, cita)) {
            return;
        }

        try {
            citaClient.marcarAsistencia(new MarcarAsistenciaRequest(cita.getId(), false));
            PiedrazulDialog.showInfo(owner, "Inasistencia registrada",
                    "La cita fue marcada como No Asistida. Este es un estado final.");
            refrescarTabla.run();
        } catch (Exception e) {
            PiedrazulDialog.showWarning(owner, "No se pudo registrar", e.getMessage());
        }
    }

    private boolean validarAccion(CitaEstadoAcciones.Accion accion, CitaResponse cita) {
        if (CitaEstadoAcciones.puedeEjecutar(accion, cita)) {
            return true;
        }
        PiedrazulDialog.showWarning(owner, "Acción no permitida",
                CitaEstadoAcciones.mensajeNoPermitido(accion, cita));
        return false;
    }
}
