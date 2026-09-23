package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.client.ConfiguracionClient;
import com.piedrazul.frontend.client.MedicoClient;
import com.piedrazul.frontend.dto.request.CrearCitaAutonomaRequest;
import com.piedrazul.frontend.dto.response.CitaResponse;
import com.piedrazul.frontend.dto.response.MedicoResponse;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.CitaProgramadaPolicy;
import com.piedrazul.frontend.util.ConsultaGeneralPolicy;
import com.piedrazul.frontend.util.EspecialidadLabels;
import com.piedrazul.frontend.util.SessionPersonaResolver;
import com.piedrazul.frontend.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AgendarCitaAutonomaController {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_MOTIVO_CARACTERES = 500;

    private static final List<String> ESPECIALIDADES = List.of(
            "GENERAL",
            "TERAPEUTA_NEURAL",
            "QUIROPRACTICO",
            "FISIOTERAPEUTA"
    );

    @FXML private Label lblBienvenida;
    @FXML private Label lblPasoActual;
    @FXML private Label lblSlotsGuia;
    @FXML private Label lblSlotsEstado;
    @FXML private Label lblEstadoGeneral;
    @FXML private Label errEspecialidad;
    @FXML private Label errMedico;
    @FXML private Label errFecha;
    @FXML private Label errSlot;
    @FXML private Label errMotivo;
    @FXML private ComboBox<String> cmbEspecialidad;
    @FXML private ComboBox<MedicoResponse> cmbMedicos;
    @FXML private DatePicker dpFecha;
    @FXML private TableView<LocalDateTime> tablaSlots;
    @FXML private TableColumn<LocalDateTime, String> colSlot;
    @FXML private TextArea txtMotivoAgendamiento;
    @FXML private Button btnAgendar;
    @FXML private Button btnVolver;

    private final MedicoClient medicoClient = new MedicoClient();
    private final CitaClient citaClient = new CitaClient();
    private final ConfiguracionClient configuracionClient = new ConfiguracionClient();

    private List<MedicoResponse> todosLosMedicos = List.of();
    private Set<LocalDate> fechasFestivas = new HashSet<>();
    private String especialidadSeleccionada;
    private MedicoResponse medicoSeleccionado;
    private LocalDateTime slotSeleccionado;
    private Long pacienteId;
    private List<CitaResponse> historialPaciente = List.of();

    @FXML
    public void initialize() {
        colSlot.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().format(FORMATO_HORA)
                )
        );

        tablaSlots.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            slotSeleccionado = newVal;
            actualizarBotonAgendar();
            ocultarError(errSlot);
            if (newVal != null) {
                ocultarEstadoSlots();
            }
            actualizarGuia();
        });

        cmbEspecialidad.valueProperty().addListener((obs, oldVal, newVal) -> {
            especialidadSeleccionada = newVal;
            ocultarError(errEspecialidad);
            medicoSeleccionado = null;
            cmbMedicos.getSelectionModel().clearSelection();
            limpiarEstadoGeneral();
            aplicarFiltroMedicos();
            cargarSlots();
            actualizarGuia();
        });

        cmbMedicos.valueProperty().addListener((obs, oldVal, newVal) -> {
            medicoSeleccionado = newVal;
            ocultarError(errMedico);
            limpiarEstadoGeneral();
            cargarSlots();
            actualizarGuia();
        });

        dpFecha.valueProperty().addListener((obs, oldVal, newVal) -> {
            ocultarError(errFecha);
            limpiarEstadoGeneral();
            cargarSlots();
            actualizarGuia();
        });

        dpFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                boolean esFestivo = !empty && fechasFestivas.contains(date);
                setDisable(empty || date.isBefore(LocalDate.now()) || esFestivo);
                if (esFestivo) {
                    setStyle("-fx-background-color: #fde8e8;");
                }
            }
        });

        cargarFestivos();
        configurarBienvenida();
        cargarMedicos();
        actualizarGuia();
    }

    private void configurarBienvenida() {
        if (!SessionManager.isLoggedIn()) {
            return;
        }

        pacienteId = SessionManager.getPersonaId();
        if (pacienteId == null) {
            pacienteId = SessionPersonaResolver.resolverPersonaId();
        }
        String username = SessionManager.getUsername();

        if (username != null && !username.isBlank()) {
            lblBienvenida.setText("Bienvenido al panel de agendamiento, " + username);
        }

        if (pacienteId == null) {
            mostrarEstadoGeneral(
                    "Su cuenta no tiene un perfil de paciente vinculado. "
                            + "Solicite al administrador que vincule su cuenta antes de agendar.",
                    "agendar-status-warning"
            );
            return;
        }

        cargarHistorialYEspecialidades();
    }

    private void cargarHistorialYEspecialidades() {
        if (pacienteId == null) {
            historialPaciente = List.of();
            cmbEspecialidad.setItems(FXCollections.observableArrayList(ESPECIALIDADES));
            return;
        }

        try {
            historialPaciente = citaClient.listarPorPaciente(pacienteId);
        } catch (Exception e) {
            historialPaciente = List.of();
        }
        aplicarEspecialidadesPermitidas();
    }

    private void aplicarEspecialidadesPermitidas() {
        List<String> permitidas = ConsultaGeneralPolicy.filtrarEspecialidadesDisponibles(
                ESPECIALIDADES,
                historialPaciente
        );

        String seleccionActual = cmbEspecialidad.getValue();
        cmbEspecialidad.setItems(FXCollections.observableArrayList(permitidas));

        if (seleccionActual != null && permitidas.contains(seleccionActual)) {
            cmbEspecialidad.setValue(seleccionActual);
            especialidadSeleccionada = seleccionActual;
        } else {
            cmbEspecialidad.getSelectionModel().clearSelection();
            especialidadSeleccionada = null;
            if (permitidas.size() == 1) {
                cmbEspecialidad.setValue(permitidas.getFirst());
                especialidadSeleccionada = permitidas.getFirst();
            }
        }

        if (CitaProgramadaPolicy.tieneCitaProgramada(historialPaciente)) {
            mostrarEstadoGeneral(CitaProgramadaPolicy.mensajeBloqueo(), "agendar-status-warning");
        } else if (!ConsultaGeneralPolicy.tieneConsultaGeneralAtendida(historialPaciente)) {
            mostrarEstadoGeneral(ConsultaGeneralPolicy.mensajeRestriccion(), "agendar-status-warning");
        }

        actualizarBotonAgendar();
        aplicarFiltroMedicos();
    }

    private void actualizarBotonAgendar() {
        btnAgendar.setDisable(
                slotSeleccionado == null
                        || CitaProgramadaPolicy.tieneCitaProgramada(historialPaciente)
        );
    }

    private void cargarFestivos() {
        try {
            fechasFestivas = new HashSet<>(configuracionClient.obtenerFestivos());
        } catch (Exception e) {
            fechasFestivas = new HashSet<>();
        }
    }

    private void cargarMedicos() {
        try {
            todosLosMedicos = medicoClient.obtenerMedicos();
            aplicarFiltroMedicos();
            if (todosLosMedicos.isEmpty()) {
                mostrarEstadoGeneral(
                        "No hay médicos disponibles en este momento. Intente más tarde.",
                        "agendar-status-warning"
                );
            }
        } catch (Exception e) {
            mostrarEstadoGeneral(
                    "No se pudieron cargar los médicos: " + e.getMessage(),
                    "agendar-status-error"
            );
        }
    }

    private void aplicarFiltroMedicos() {
        if (especialidadSeleccionada == null || especialidadSeleccionada.isBlank()) {
            cmbMedicos.setItems(FXCollections.observableArrayList());
            cmbMedicos.setDisable(true);
            return;
        }

        cmbMedicos.setDisable(false);
        List<MedicoResponse> filtrados = todosLosMedicos.stream()
                .filter(m -> medicoAtiendeEspecialidad(m, especialidadSeleccionada))
                .collect(Collectors.toList());

        cmbMedicos.setItems(FXCollections.observableArrayList(filtrados));

        if (filtrados.isEmpty()) {
            mostrarEstadoGeneral(
                    "No hay médicos disponibles para " + etiquetaEspecialidad(especialidadSeleccionada) + ".",
                    "agendar-status-warning"
            );
        }
    }

    private boolean medicoAtiendeEspecialidad(MedicoResponse medico, String especialidad) {
        if (medico.getEspecialidades() == null || medico.getEspecialidades().isEmpty()) {
            return "GENERAL".equals(especialidad);
        }
        return medico.getEspecialidades().contains(especialidad);
    }

    private void cargarSlots() {
        tablaSlots.getSelectionModel().clearSelection();
        slotSeleccionado = null;
        btnAgendar.setDisable(true);
        ocultarError(errSlot);
        ocultarEstadoSlots();

        if (especialidadSeleccionada == null || medicoSeleccionado == null || dpFecha.getValue() == null) {
            tablaSlots.setItems(FXCollections.observableArrayList());
            actualizarGuia();
            return;
        }

        try {
            List<LocalDateTime> slots = citaClient.obtenerSlotsDisponibles(
                    medicoSeleccionado.getPersonaId(),
                    pacienteId
            );
            LocalDate fechaSeleccionada = dpFecha.getValue();
            List<LocalDateTime> slotsFiltrados = slots.stream()
                    .filter(slot -> slot.toLocalDate().equals(fechaSeleccionada))
                    .toList();

            tablaSlots.setItems(FXCollections.observableArrayList(slotsFiltrados));

            if (slotsFiltrados.isEmpty()) {
                mostrarEstadoSlots(
                        "No hay horarios disponibles para esta fecha. Pruebe con otro día.",
                        "agendar-status-warning"
                );
                lblSlotsGuia.setText("No se encontraron horarios para la fecha seleccionada.");
            } else {
                lblSlotsGuia.setText(
                        "Seleccione la hora de su preferencia (" + slotsFiltrados.size() + " disponible"
                                + (slotsFiltrados.size() == 1 ? "" : "s") + ")."
                );
            }

        } catch (Exception e) {
            tablaSlots.setItems(FXCollections.observableArrayList());
            mostrarEstadoGeneral(
                    mensajeAgendamientoParaPaciente(e.getMessage()),
                    "agendar-status-error"
            );
        }

        actualizarGuia();
    }

    private void actualizarGuia() {
        if (pacienteId == null) {
            lblPasoActual.setText("Complete su perfil de paciente para continuar.");
            lblPasoActual.getStyleClass().setAll("agendar-step-indicator", "agendar-status-warning");
            return;
        }

        lblPasoActual.getStyleClass().setAll("agendar-step-indicator");

        if (especialidadSeleccionada == null) {
            lblPasoActual.setText("Paso 1 de 5: Seleccione el tipo de consulta");
            lblSlotsGuia.setText("Primero elija la especialidad que necesita.");
        } else if (medicoSeleccionado == null) {
            lblPasoActual.setText("Paso 2 de 5: Seleccione su médico");
            lblSlotsGuia.setText("Elija un médico que atienda "
                    + etiquetaEspecialidad(especialidadSeleccionada) + ".");
        } else if (dpFecha.getValue() == null) {
            lblPasoActual.setText("Paso 3 de 5: Seleccione la fecha de su consulta");
            lblSlotsGuia.setText("Elija una fecha para ver los horarios de "
                    + nombreMedico(medicoSeleccionado) + ".");
        } else if (slotSeleccionado == null) {
            lblPasoActual.setText("Paso 4 de 5: Seleccione la hora de su cita");
        } else {
            lblPasoActual.setText(
                    "Paso 5 de 5: Puede indicar el motivo de su consulta (opcional) y confirmar con «Agendar cita»"
            );
        }
    }

    @FXML
    private void handleAgendar() {
        limpiarValidaciones();

        boolean valido = true;

        if (especialidadSeleccionada == null || especialidadSeleccionada.isBlank()) {
            mostrarError(errEspecialidad, "Debe seleccionar el tipo de consulta.");
            marcarInputInvalido(cmbEspecialidad, true);
            valido = false;
        } else if (!ConsultaGeneralPolicy.especialidadPermitida(especialidadSeleccionada, historialPaciente)) {
            mostrarError(errEspecialidad, ConsultaGeneralPolicy.mensajeRestriccion());
            marcarInputInvalido(cmbEspecialidad, true);
            valido = false;
        }

        if (medicoSeleccionado == null) {
            mostrarError(errMedico, "Debe seleccionar un médico.");
            marcarInputInvalido(cmbMedicos, true);
            valido = false;
        }

        if (dpFecha.getValue() == null) {
            mostrarError(errFecha, "Debe seleccionar una fecha.");
            marcarInputInvalido(dpFecha, true);
            valido = false;
        }

        if (slotSeleccionado == null) {
            mostrarError(errSlot, "Debe seleccionar un horario de la tabla.");
            valido = false;
        }

        String motivo = motivoAgendamientoNormalizado();
        if (motivo != null && motivo.length() > MAX_MOTIVO_CARACTERES) {
            mostrarError(errMotivo, "El motivo no puede superar " + MAX_MOTIVO_CARACTERES + " caracteres.");
            marcarTextAreaInvalida(true);
            valido = false;
        }

        if (pacienteId == null) {
            mostrarEstadoGeneral(
                    "No se pudo identificar su perfil de paciente.",
                    "agendar-status-error"
            );
            valido = false;
        } else if (CitaProgramadaPolicy.tieneCitaProgramada(historialPaciente)) {
            mostrarEstadoGeneral(CitaProgramadaPolicy.mensajeBloqueo(), "agendar-status-error");
            valido = false;
        }

        if (!valido) {
            actualizarGuia();
            return;
        }

        try {
            CrearCitaAutonomaRequest request = new CrearCitaAutonomaRequest(
                    pacienteId,
                    medicoSeleccionado.getPersonaId(),
                    pacienteId,
                    slotSeleccionado,
                    especialidadSeleccionada,
                    motivo
            );

            citaClient.crearCitaAutonoma(request);

            mostrarEstadoGeneral(
                    "¡Cita agendada correctamente! Será redirigido al menú principal.",
                    "agendar-status-success"
            );
            btnAgendar.setDisable(true);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.8)
            );
            pause.setOnFinished(e -> volverAlDashboard());
            pause.play();

        } catch (Exception e) {
            mostrarEstadoGeneral(
                    mensajeAgendamientoParaPaciente(e.getMessage()),
                    "agendar-status-error"
            );
            cargarSlots();
        }
    }

    @FXML
    private void handleVolver() {
        volverAlDashboard();
    }

    private void volverAlDashboard() {
        SceneManager.switchScene(
                "/view/dashboard/paciente-dashboard.fxml",
                btnVolver,
                "PIEDRAZUL - Menú principal"
        );
    }

    private void limpiarValidaciones() {
        ocultarError(errEspecialidad);
        ocultarError(errMedico);
        ocultarError(errFecha);
        ocultarError(errSlot);
        ocultarError(errMotivo);
        marcarTextAreaInvalida(false);
        marcarInputInvalido(cmbEspecialidad, false);
        marcarInputInvalido(cmbMedicos, false);
        marcarInputInvalido(dpFecha, false);
        limpiarEstadoGeneral();
    }

    private void mostrarError(Label label, String mensaje) {
        label.setText(mensaje);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void ocultarError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private String motivoAgendamientoNormalizado() {
        if (txtMotivoAgendamiento == null) {
            return null;
        }
        String texto = txtMotivoAgendamiento.getText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private void marcarTextAreaInvalida(boolean invalida) {
        if (invalida) {
            if (!txtMotivoAgendamiento.getStyleClass().contains("agendar-textarea-error")) {
                txtMotivoAgendamiento.getStyleClass().add("agendar-textarea-error");
            }
        } else {
            txtMotivoAgendamiento.getStyleClass().remove("agendar-textarea-error");
        }
    }

    private void marcarInputInvalido(Control control, boolean invalido) {
        if (invalido) {
            if (!control.getStyleClass().contains("agendar-input-error")) {
                control.getStyleClass().add("agendar-input-error");
            }
        } else {
            control.getStyleClass().remove("agendar-input-error");
        }
    }

    private void mostrarEstadoSlots(String mensaje, String estilo) {
        lblSlotsEstado.setText(mensaje);
        lblSlotsEstado.getStyleClass().setAll("agendar-status-banner", estilo);
        lblSlotsEstado.setVisible(true);
        lblSlotsEstado.setManaged(true);
    }

    private void ocultarEstadoSlots() {
        lblSlotsEstado.setText("");
        lblSlotsEstado.setVisible(false);
        lblSlotsEstado.setManaged(false);
    }

    private void mostrarEstadoGeneral(String mensaje, String estilo) {
        lblEstadoGeneral.setText(mensaje);
        lblEstadoGeneral.getStyleClass().setAll("agendar-status-banner", estilo);
        lblEstadoGeneral.setVisible(true);
        lblEstadoGeneral.setManaged(true);
    }

    private void limpiarEstadoGeneral() {
        lblEstadoGeneral.setText("");
        lblEstadoGeneral.setVisible(false);
        lblEstadoGeneral.setManaged(false);
    }

    private static String nombreMedico(MedicoResponse medico) {
        return medico.getPrimerNombre() + " " + medico.getPrimerApellido();
    }

    private static String etiquetaEspecialidad(String codigo) {
        return EspecialidadLabels.etiqueta(codigo);
    }

    private static String mensajeAgendamientoParaPaciente(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "No se pudo completar el agendamiento. Intente nuevamente.";
        }

        if (mensaje.contains("El paciente ya tiene una cita agendada")) {
            return "Ya tiene una cita agendada en el horario seleccionado. Elija otra hora.";
        }
        if (mensaje.contains("El médico ya tiene una cita agendada")) {
            return "Ese horario acaba de ser ocupado. Elija otra hora disponible.";
        }
        if (mensaje.contains("ya no está disponible")) {
            return "El horario seleccionado ya no está disponible. Elija otra hora.";
        }
        if (mensaje.contains("no atiende la especialidad")) {
            return "El médico seleccionado no atiende ese tipo de consulta. Elija otro médico o especialidad.";
        }
        if (mensaje.contains("Consulta General") || mensaje.contains("Medicina General")) {
            return ConsultaGeneralPolicy.mensajeRestriccion();
        }
        if (mensaje.contains("cita programada") || mensaje.contains("reagendada pendiente")) {
            return CitaProgramadaPolicy.mensajeBloqueo();
        }

        return mensaje;
    }
}
