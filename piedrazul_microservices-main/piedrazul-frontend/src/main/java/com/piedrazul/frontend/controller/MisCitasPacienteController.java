package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.dto.response.CitaResponse;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.CitaMotivoUtil;
import com.piedrazul.frontend.util.EspecialidadLabels;
import com.piedrazul.frontend.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class MisCitasPacienteController {

    private static final DateTimeFormatter FECHA_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblProximaCita;
    @FXML private Label lblLeyendaProxima;
    @FXML private Button btnVolver;
    @FXML private TableView<CitaResponse> tablaCitas;

    @FXML private TableColumn<CitaResponse, String> colId;
    @FXML private TableColumn<CitaResponse, String> colFecha;
    @FXML private TableColumn<CitaResponse, String> colMedico;
    @FXML private TableColumn<CitaResponse, String> colTipo;
    @FXML private TableColumn<CitaResponse, String> colEstado;
    @FXML private TableColumn<CitaResponse, String> colMotivo;

    private String proximaCitaId;
    private final CitaClient citaClient = new CitaClient();

    @FXML
    public void initialize() {
        configurarEstilosVentana();
        configurarColumnas();
        configurarResaltadoFilas();
        cargarCitas();
    }

    private void configurarEstilosVentana() {
        tablaCitas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String css = getClass()
                        .getResource("/view/css/dashboard.css")
                        .toExternalForm();
                if (!newScene.getStylesheets().contains(css)) {
                    newScene.getStylesheets().add(css);
                }
                newScene.windowProperty().addListener((o, oldWindow, newWindow) -> {
                    if (newWindow instanceof javafx.stage.Stage stage) {
                        stage.setMaximized(true);
                    }
                });
            }
        });

        tablaCitas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colFecha.setCellValueFactory(data ->
                new SimpleStringProperty(formatearFecha(data.getValue())));

        colMedico.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getMedicoNombre() != null
                                ? data.getValue().getMedicoNombre()
                                : String.valueOf(data.getValue().getMedicoId())
                ));

        colTipo.setCellValueFactory(data ->
                new SimpleStringProperty(
                        EspecialidadLabels.etiqueta(data.getValue().getEspecialidad())
                ));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colMotivo.setCellValueFactory(data ->
                new SimpleStringProperty(CitaMotivoUtil.resolverMotivo(data.getValue())));
    }

    private void configurarResaltadoFilas() {
        tablaCitas.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(CitaResponse item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("cita-proxima-row");
                if (!empty && item != null && item.getId() != null && item.getId().equals(proximaCitaId)) {
                    getStyleClass().add("cita-proxima-row");
                }
            }
        });
    }

    private void cargarCitas() {
        try {
            Long pacienteId = SessionManager.getPersonaId();
            if (pacienteId == null) {
                mostrarError("No se pudo identificar tu perfil de paciente. Vuelve a iniciar sesión.");
                return;
            }

            List<CitaResponse> citas = citaClient.listarPorPaciente(pacienteId);
            citas.sort(Comparator.comparing(this::parseFechaHora).reversed());

            proximaCitaId = citas.stream()
                    .filter(c -> !esCancelada(c))
                    .filter(c -> !parseFechaHora(c).isBefore(LocalDateTime.now()))
                    .min(Comparator.comparing(this::parseFechaHora))
                    .map(CitaResponse::getId)
                    .orElse(null);

            actualizarEtiquetaProximaCita(citas);
            tablaCitas.setItems(FXCollections.observableArrayList(citas));
            tablaCitas.refresh();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudieron cargar tus citas: " + e.getMessage());
        }
    }

    private void actualizarEtiquetaProximaCita(List<CitaResponse> citas) {
        boolean hayProxima = proximaCitaId != null;
        lblLeyendaProxima.setVisible(hayProxima);
        lblLeyendaProxima.setManaged(hayProxima);

        if (!hayProxima) {
            lblProximaCita.setText("No tienes citas próximas programadas.");
            lblProximaCita.getStyleClass().remove("proxima-cita-label-activa");
            return;
        }

        citas.stream()
                .filter(c -> proximaCitaId.equals(c.getId()))
                .findFirst()
                .ifPresent(cita -> {
                    String medico = cita.getMedicoNombre() != null
                            ? cita.getMedicoNombre()
                            : "médico #" + cita.getMedicoId();
                    lblProximaCita.setText(
                            "Próxima cita: " + formatearFecha(cita) + " con " + medico
                    );
                    if (!lblProximaCita.getStyleClass().contains("proxima-cita-label-activa")) {
                        lblProximaCita.getStyleClass().add("proxima-cita-label-activa");
                    }
                });
    }

    private boolean esCancelada(CitaResponse cita) {
        return cita.getEstado() != null && cita.getEstado().equalsIgnoreCase("Cancelada");
    }

    private LocalDateTime parseFechaHora(CitaResponse cita) {
        if (cita.getFechaHora() == null || cita.getFechaHora().isBlank()) {
            return LocalDateTime.MIN;
        }
        try {
            return LocalDateTime.parse(cita.getFechaHora(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(cita.getFechaHora(), DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    private String formatearFecha(CitaResponse cita) {
        try {
            return parseFechaHora(cita).format(FECHA_DISPLAY);
        } catch (Exception e) {
            return cita.getFechaHora();
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void volver() {
        SceneManager.switchScene(
                "/view/dashboard/paciente-dashboard.fxml",
                btnVolver,
                "Dashboard"
        );
    }
}
