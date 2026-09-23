package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.client.MedicoClient;
import com.piedrazul.frontend.dto.response.CitaResponse;
import com.piedrazul.frontend.dto.response.MedicoResponse;
import com.piedrazul.frontend.util.CitaEstadoAcciones;
import com.piedrazul.frontend.util.CitaGestionHelper;
import com.piedrazul.frontend.util.CitaMotivoUtil;
import com.piedrazul.frontend.util.CitasCsvExporter;
import com.piedrazul.frontend.util.CitasExcelExporter;
import com.piedrazul.frontend.util.CitasExportMessages;
import com.piedrazul.frontend.util.EspecialidadLabels;
import com.piedrazul.frontend.util.RangoFechasUtil;
import com.piedrazul.frontend.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class HistorialCitasController {

    private static final DateTimeFormatter FECHA_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ComboBox<MedicoResponse> cmbMedicos;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private Label lblGestionHint;
    @FXML private Button btnMarcarAtendida;
    @FXML private Button btnMarcarNoAsistida;
    @FXML private Button btnReagendar;
    @FXML private Button btnCancelar;
    @FXML private Button btnVolver;
    @FXML private TableView<CitaResponse> tablaCitas;

    @FXML private TableColumn<CitaResponse, String> colId;
    @FXML private TableColumn<CitaResponse, String> colFecha;
    @FXML private TableColumn<CitaResponse, String> colPaciente;
    @FXML private TableColumn<CitaResponse, String> colMedico;
    @FXML private TableColumn<CitaResponse, String> colTipo;
    @FXML private TableColumn<CitaResponse, String> colEstado;
    @FXML private TableColumn<CitaResponse, String> colMotivo;

    private final CitaClient citaClient = new CitaClient();
    private final MedicoClient medicoClient = new MedicoClient();
    private CitaGestionHelper gestionHelper;

    @FXML
    public void initialize() {
        gestionHelper = new CitaGestionHelper(tablaCitas, this::buscarCitas);

        configurarEstilosVentana();
        configurarColumnas();
        configurarGestionCitas();
        configurarFiltros();
        cargarMedicos();
        buscarCitas();
    }

    private void configurarGestionCitas() {
        tablaCitas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) ->
                gestionHelper.configurarSeleccion(
                        seleccionada,
                        btnCancelar,
                        btnReagendar,
                        btnMarcarAtendida,
                        btnMarcarNoAsistida,
                        lblGestionHint
                )
        );
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
        colId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getId()));

        colFecha.setCellValueFactory(data ->
                new SimpleStringProperty(formatearFecha(data.getValue())));

        colPaciente.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getPacienteNombre() != null
                                ? data.getValue().getPacienteNombre()
                                : String.valueOf(data.getValue().getPacienteId())
                ));

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

        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEstado()));
        CitaEstadoAcciones.configurarColumnaEstado(colEstado);

        colMotivo.setCellValueFactory(data ->
                new SimpleStringProperty(CitaMotivoUtil.resolverMotivo(data.getValue())));
    }

    private void configurarFiltros() {
        cmbMedicos.setPromptText("Todos los médicos");
        dpFechaInicio.setPromptText("Fecha inicial");
        dpFechaFin.setPromptText("Fecha final");

        cmbMedicos.valueProperty().addListener((obs, anterior, nuevo) -> buscarCitas());
        dpFechaInicio.valueProperty().addListener((obs, anterior, nuevo) -> buscarCitas());
        dpFechaFin.valueProperty().addListener((obs, anterior, nuevo) -> buscarCitas());
    }

    private void cargarMedicos() {
        try {
            List<MedicoResponse> medicos = medicoClient.obtenerMedicos();
            cmbMedicos.setItems(FXCollections.observableArrayList(medicos));
        } catch (Exception e) {
            mostrarError("No se pudo cargar el listado de médicos: " + e.getMessage());
        }
    }

    @FXML
    private void buscarCitas() {
        LocalDate fechaInicio = dpFechaInicio.getValue();
        LocalDate fechaFin = dpFechaFin.getValue();

        if (RangoFechasUtil.esRangoInvalido(fechaInicio, fechaFin)) {
            mostrarAdvertencia(RangoFechasUtil.MSG_RANGO_INVALIDO);
            return;
        }

        try {
            MedicoResponse medicoSeleccionado = cmbMedicos.getValue();
            Long medicoId = medicoSeleccionado != null ? medicoSeleccionado.getPersonaId() : null;

            List<CitaResponse> citas = citaClient.listarHistorial(medicoId, fechaInicio, fechaFin);
            citas.sort(Comparator.comparing(this::parseFechaHora).reversed());
            tablaCitas.setItems(FXCollections.observableArrayList(citas));

        } catch (Exception e) {
            tablaCitas.setItems(FXCollections.observableArrayList());
            mostrarError("No se pudieron cargar las citas: " + e.getMessage());
        }
    }

    @FXML
    private void exportarCitasExcel() {
        if (!validarDatosExportacion()) {
            return;
        }
        try {
            CitasExcelExporter.exportar(tablaCitas.getItems(), true, "historial_citas_agendador");
            mostrarInformacion(CitasExportMessages.EXITO);
        } catch (Exception e) {
            mostrarError(CitasExportMessages.ERROR);
        }
    }

    @FXML
    private void exportarCitasCsv() {
        if (!validarDatosExportacion()) {
            return;
        }
        try {
            CitasCsvExporter.exportar(tablaCitas.getItems(), true, "historial_citas_agendador");
            mostrarInformacion(CitasExportMessages.EXITO);
        } catch (Exception e) {
            mostrarError(CitasExportMessages.ERROR);
        }
    }

    private boolean validarDatosExportacion() {
        LocalDate fechaInicio = dpFechaInicio.getValue();
        LocalDate fechaFin = dpFechaFin.getValue();

        if (RangoFechasUtil.esRangoInvalido(fechaInicio, fechaFin)) {
            mostrarAdvertencia(RangoFechasUtil.MSG_RANGO_INVALIDO);
            return false;
        }

        List<CitaResponse> citas = tablaCitas.getItems();
        if (citas == null || citas.isEmpty()) {
            mostrarAdvertencia(CitasExportMessages.SIN_DATOS);
            return false;
        }
        return true;
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
        if (cita.getFechaHora() == null || cita.getFechaHora().isBlank()) {
            return "-";
        }
        try {
            return parseFechaHora(cita).format(FECHA_DISPLAY);
        } catch (Exception e) {
            return cita.getFechaHora();
        }
    }

    private void mostrarInformacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Historial de citas");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Historial de citas");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Historial de citas");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void marcarAtendida() {
        gestionHelper.marcarAtendida(tablaCitas.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void marcarNoAsistida() {
        gestionHelper.marcarNoAsistida(tablaCitas.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void reagendarCita() {
        gestionHelper.reagendar(tablaCitas.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void cancelarCita() {
        gestionHelper.cancelar(tablaCitas.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void volver() {
        SceneManager.switchScene(
                "/view/dashboard/agendador-dashboard.fxml",
                btnVolver,
                "Dashboard"
        );
    }
}
