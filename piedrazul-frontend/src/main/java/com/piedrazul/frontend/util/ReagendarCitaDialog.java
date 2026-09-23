package com.piedrazul.frontend.util;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.dto.response.CitaResponse;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public final class ReagendarCitaDialog {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private ReagendarCitaDialog() {
    }

    public static Optional<LocalDateTime> solicitarNuevaFecha(Node owner, CitaResponse cita) {
        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }
        dialog.setTitle("Reagendar cita");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label lblInfo = new Label(mensajeInfo(cita));
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(460);

        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        TableView<LocalDateTime> tablaSlots = new TableView<>();
        TableColumn<LocalDateTime, String> colHora = new TableColumn<>("Horario");
        colHora.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().format(HORA)
                ));
        colHora.setPrefWidth(120);
        tablaSlots.getColumns().add(colHora);
        tablaSlots.setPrefHeight(180);
        tablaSlots.setPlaceholder(new Label("Seleccione una fecha para ver horarios disponibles."));

        Label lblEstado = new Label();
        lblEstado.setWrapText(true);
        lblEstado.setStyle("-fx-text-fill: #c62828;");

        CitaClient citaClient = new CitaClient();

        Runnable cargarSlots = () -> {
            LocalDate fecha = dpFecha.getValue();
            if (fecha == null || cita.getMedicoId() == null) {
                tablaSlots.setItems(FXCollections.observableArrayList());
                return;
            }
            try {
                List<LocalDateTime> slots = citaClient.obtenerSlotsDisponibles(
                        cita.getMedicoId(),
                        cita.getPacienteId()
                );
                List<LocalDateTime> filtrados = slots.stream()
                        .filter(slot -> slot.toLocalDate().equals(fecha))
                        .toList();
                tablaSlots.setItems(FXCollections.observableArrayList(filtrados));
                lblEstado.setText(filtrados.isEmpty()
                        ? "No hay horarios disponibles para la fecha seleccionada."
                        : "");
            } catch (Exception e) {
                tablaSlots.setItems(FXCollections.observableArrayList());
                lblEstado.setText("No se pudieron cargar los horarios: " + e.getMessage());
            }
        };

        dpFecha.valueProperty().addListener((obs, oldVal, newVal) -> cargarSlots.run());
        cargarSlots.run();

        VBox contenido = new VBox(12, lblInfo, dpFecha, tablaSlots, lblEstado);
        contenido.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(contenido);

        Optional<ButtonType> resultado = dialog.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return Optional.empty();
        }

        LocalDateTime slot = tablaSlots.getSelectionModel().getSelectedItem();
        if (slot == null) {
            PiedrazulDialog.showWarning(owner, "Horario requerido",
                    "Debe seleccionar un horario disponible para reagendar la cita.");
            return Optional.empty();
        }
        return Optional.of(slot);
    }

    private static String mensajeInfo(CitaResponse cita) {
        String estado = cita.getEstado() != null ? cita.getEstado() : "";
        if ("Atendida".equalsIgnoreCase(estado)) {
            return "La cita original permanecerá como Atendida. "
                    + "Se creará una nueva cita con la fecha y hora seleccionadas.";
        }
        return "Seleccione la nueva fecha y hora. La franja anterior quedará liberada "
                + "y la cita pasará a estado Reagendada.";
    }
}
