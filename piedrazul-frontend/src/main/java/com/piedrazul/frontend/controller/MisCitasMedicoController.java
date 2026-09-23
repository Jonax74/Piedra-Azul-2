package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.client.PacienteClient;
import com.piedrazul.frontend.client.PersonaClient;
import com.piedrazul.frontend.dto.response.CitaResponse;
import com.piedrazul.frontend.dto.response.PacienteListItem;
import com.piedrazul.frontend.dto.response.PacienteResponse;
import com.piedrazul.frontend.dto.response.PersonaResponse;
import com.piedrazul.frontend.session.SessionManager;
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
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MisCitasMedicoController {

    private static final DateTimeFormatter FECHA_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ComboBox<PacienteListItem> cmbPacientes;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private Label lblProximaCita;
    @FXML private Label lblLeyendaProxima;
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
    @FXML private TableColumn<CitaResponse, String> colTipo;
    @FXML private TableColumn<CitaResponse, String> colEstado;
    @FXML private TableColumn<CitaResponse, String> colMotivo;

    private Long medicoId;
    private String proximaCitaId;
    private CitaGestionHelper gestionHelper;

    private final CitaClient citaClient = new CitaClient();
    private final PacienteClient pacienteClient = new PacienteClient();
    private final PersonaClient personaClient = new PersonaClient();

    @FXML
    public void initialize() {
        medicoId = SessionManager.getPersonaId();
        if (medicoId == null) {
            mostrarError("No se pudo identificar tu perfil de médico. Vuelve a iniciar sesión.");
            return;
        }

        gestionHelper = new CitaGestionHelper(tablaCitas, this::buscarCitas);

        configurarEstilosVentana();
        configurarColumnas();
        configurarResaltadoFilas();
        configurarGestionCitas();
        cargarPacientes();
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
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colFecha.setCellValueFactory(data ->
                new SimpleStringProperty(formatearFecha(data.getValue())));

        colPaciente.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getPacienteNombre() != null
                                ? data.getValue().getPacienteNombre()
                                : String.valueOf(data.getValue().getPacienteId())
                ));

        colTipo.setCellValueFactory(data ->
                new SimpleStringProperty(
                        EspecialidadLabels.etiqueta(data.getValue().getEspecialidad())
                ));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        CitaEstadoAcciones.configurarColumnaEstado(colEstado);

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

    private void cargarPacientes() {
        try {
            List<PacienteResponse> pacientes = pacienteClient.obtenerPacientes();
            Map<Long, PersonaResponse> personasPorId = personaClient.listarPersonas().stream()
                    .collect(Collectors.toMap(PersonaResponse::getId, p -> p, (a, b) -> a));

            List<PacienteListItem> items = pacientes.stream()
                    .map(p -> {
                        PersonaResponse persona = personasPorId.get(p.getPersonaId());
                        String nombre = persona != null
                                ? persona.getPrimerNombre() + " " + persona.getPrimerApellido()
                                : "Paciente #" + p.getPersonaId();
                        return new PacienteListItem(p.getPersonaId(), nombre.trim());
                    })
                    .sorted(Comparator.comparing(PacienteListItem::getNombreCompleto))
                    .toList();

            cmbPacientes.setItems(FXCollections.observableArrayList(items));
            cmbPacientes.setPromptText("Todos los pacientes");

        } catch (Exception e) {
            mostrarError("No se pudo cargar el listado de pacientes: " + e.getMessage());
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
            PacienteListItem pacienteSeleccionado = cmbPacientes.getValue();
            Long pacienteId = pacienteSeleccionado != null ? pacienteSeleccionado.getPersonaId() : null;

            List<CitaResponse> citas = citaClient.listarPorMedico(medicoId, pacienteId, fechaInicio, fechaFin);
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
            mostrarError("No se pudieron cargar tus citas: " + e.getMessage());
        }
    }

    @FXML
    private void exportarCitasExcel() {
        if (!validarDatosExportacion()) {
            return;
        }
        try {
            CitasExcelExporter.exportar(tablaCitas.getItems(), false, "informe_citas_medico");
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
            CitasCsvExporter.exportar(tablaCitas.getItems(), false, "informe_citas_medico");
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
                    String paciente = cita.getPacienteNombre() != null
                            ? cita.getPacienteNombre()
                            : "paciente #" + cita.getPacienteId();
                    lblProximaCita.setText(
                            "Próxima cita: " + formatearFecha(cita) + " con " + paciente
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

    private void mostrarInformacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mis citas");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Mis citas");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
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
                "/view/dashboard/medico-dashboard.fxml",
                btnVolver,
                "Dashboard"
        );
    }
}
