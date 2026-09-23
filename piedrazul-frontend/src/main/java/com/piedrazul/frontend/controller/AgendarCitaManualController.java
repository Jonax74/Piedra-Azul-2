package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.client.ConfiguracionClient;
import com.piedrazul.frontend.client.MedicoClient;
import com.piedrazul.frontend.client.PacienteClient;
import com.piedrazul.frontend.client.PersonaClient;
import com.piedrazul.frontend.dto.request.CrearCitaAutonomaRequest;
import com.piedrazul.frontend.dto.request.CrearPersonaRequest;
import com.piedrazul.frontend.dto.response.CitaResponse;
import com.piedrazul.frontend.dto.response.MedicoResponse;
import com.piedrazul.frontend.dto.response.PacienteListItem;
import com.piedrazul.frontend.dto.response.PacienteResponse;
import com.piedrazul.frontend.dto.response.PersonaResponse;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.ApiClientException;
import com.piedrazul.frontend.util.CitaProgramadaPolicy;
import com.piedrazul.frontend.util.ConsultaGeneralPolicy;
import com.piedrazul.frontend.util.EspecialidadLabels;
import com.piedrazul.frontend.util.FormFieldHelper;
import com.piedrazul.frontend.util.PersonaFormSupport;
import com.piedrazul.frontend.util.SceneManager;
import com.piedrazul.frontend.util.SessionPersonaResolver;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgendarCitaManualController {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_MOTIVO_CARACTERES = 500;
    private static final int MAX_REINTENTOS_CITA = 8;
    private static final long ESPERA_INICIAL_MS = 250L;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private static final List<String> ESPECIALIDADES = List.of(
            "GENERAL", "TERAPEUTA_NEURAL", "QUIROPRACTICO", "FISIOTERAPEUTA"
    );

    @FXML private Label lblBienvenida;
    @FXML private Label lblGuiaAgendamiento;
    @FXML private Label lblPasoActual;
    @FXML private Label lblSlotsGuia;
    @FXML private Label lblSlotsEstado;
    @FXML private Label lblEstadoGeneral;
    @FXML private Label errPaciente;
    @FXML private Label errEspecialidad;
    @FXML private Label errMedico;
    @FXML private Label errFecha;
    @FXML private Label errSlot;
    @FXML private Label errMotivo;
    @FXML private ComboBox<PacienteListItem> cmbPacientes;
    @FXML private VBox panelNuevoPaciente;
    @FXML private TextField txtPrimerNombre;
    @FXML private TextField txtSegundoNombre;
    @FXML private TextField txtPrimerApellido;
    @FXML private TextField txtSegundoApellido;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private DatePicker dpNacimiento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDni;
    @FXML private TextField txtCorreo;
    @FXML private Label errPrimerNombre;
    @FXML private Label errSegundoNombre;
    @FXML private Label errPrimerApellido;
    @FXML private Label errSegundoApellido;
    @FXML private Label errGenero;
    @FXML private Label errFechaNacimiento;
    @FXML private Label errTelefono;
    @FXML private Label errDni;
    @FXML private Label errCorreo;
    @FXML private Label lblMedicoTitulo;
    @FXML private VBox panelSelectorMedico;
    @FXML private VBox panelMedicoFijo;
    @FXML private Label lblMedicoFijo;
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
    private final PacienteClient pacienteClient = new PacienteClient();
    private final PersonaClient personaClient = new PersonaClient();
    private final ConfiguracionClient configuracionClient = new ConfiguracionClient();

    private List<MedicoResponse> todosLosMedicos = List.of();
    private Set<LocalDate> fechasFestivas = new HashSet<>();
    private PacienteListItem pacienteSeleccionado;
    private boolean registrandoNuevoPaciente;
    private String especialidadSeleccionada;
    private MedicoResponse medicoSeleccionado;
    private LocalDateTime slotSeleccionado;
    private boolean modoMedico;
    private Long creadorId;
    private List<CitaResponse> historialPaciente = List.of();

    @FXML
    public void initialize() {
        modoMedico = SessionManager.isAgendarManualComoMedico();

        boolean esAgendador = SessionManager.hasRole("AGENDADOR");
        boolean esMedico = SessionManager.hasRole("MEDICO_TERAPISTA");
        boolean rolPermitido = modoMedico ? esMedico : esAgendador;

        if (!SessionManager.isLoggedIn() || !rolPermitido) {
            SessionManager.endAgendarManualComoMedico();
            SessionManager.clear();
            SceneManager.showLogin("/view/auth_register/loginView.fxml", btnAgendar);
            return;
        }

        creadorId = SessionPersonaResolver.resolverPersonaId();
        String username = SessionManager.getUsername();
        if (username != null && !username.isBlank()) {
            String rolEtiqueta = modoMedico ? "Médico" : "Agendador";
            lblBienvenida.setText("Agendamiento Manual - " + rolEtiqueta + " " + username);
        }
        if (modoMedico) {
            lblGuiaAgendamiento.setText(
                    "Registre una cita para su paciente. Usted será el médico que atiende la consulta."
            );
            lblMedicoTitulo.setText("Médico");
            configurarModoMedico();
        } else if (creadorId == null) {
            mostrarEstadoGeneral(
                    "Su cuenta de agendador no tiene una persona vinculada. "
                            + "Solicite al administrador que revise su usuario antes de agendar citas.",
                    "agendar-status-warning"
            );
        }

        colSlot.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().format(FORMATO_HORA)
                ));

        configurarFormularioNuevoPaciente();

        tablaSlots.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            slotSeleccionado = newVal;
            btnAgendar.setDisable(newVal == null);
            ocultarError(errSlot);
            if (newVal != null) {
                ocultarEstadoSlots();
            }
            actualizarGuia();
        });

        cmbPacientes.valueProperty().addListener((obs, oldVal, newVal) -> {
            pacienteSeleccionado = newVal;
            registrandoNuevoPaciente = newVal != null && newVal.esNuevoPaciente();
            actualizarPanelNuevoPaciente();
            actualizarHistorialYEspecialidades();
            ocultarError(errPaciente);
            limpiarEstadoGeneral();
            cargarSlots();
            actualizarGuia();
        });

        cmbEspecialidad.valueProperty().addListener((obs, oldVal, newVal) -> {
            especialidadSeleccionada = newVal;
            ocultarError(errEspecialidad);
            if (!modoMedico) {
                medicoSeleccionado = null;
                cmbMedicos.getSelectionModel().clearSelection();
                aplicarFiltroMedicos();
            }
            limpiarEstadoGeneral();
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

        if (!modoMedico) {
            cmbEspecialidad.setItems(FXCollections.observableArrayList(ESPECIALIDADES));
        }
        cargarFestivos();
        cargarPacientes();
        if (!modoMedico) {
            cargarMedicos();
        }
        actualizarGuia();
    }

    private void configurarModoMedico() {
        panelSelectorMedico.setVisible(false);
        panelSelectorMedico.setManaged(false);
        panelMedicoFijo.setVisible(true);
        panelMedicoFijo.setManaged(true);

        if (creadorId == null) {
            mostrarEstadoGeneral(
                    "Su cuenta de médico no tiene una persona vinculada. "
                            + "Solicite al administrador que revise su usuario antes de agendar citas.",
                    "agendar-status-warning"
            );
            return;
        }

        try {
            todosLosMedicos = medicoClient.obtenerMedicos();
            medicoSeleccionado = todosLosMedicos.stream()
                    .filter(m -> creadorId.equals(m.getPersonaId()))
                    .findFirst()
                    .orElse(null);

            if (medicoSeleccionado == null) {
                mostrarEstadoGeneral(
                        "No se encontró su perfil de médico en el sistema.",
                        "agendar-status-error"
                );
                return;
            }

            lblMedicoFijo.setText("Dr(a). " + medicoSeleccionado.getPrimerNombre() + " "
                    + medicoSeleccionado.getPrimerApellido() + " — Médico #" + medicoSeleccionado.getPersonaId());

            aplicarEspecialidadesPermitidas();
        } catch (Exception e) {
            mostrarEstadoGeneral(
                    "No se pudo cargar su perfil de médico: " + e.getMessage(),
                    "agendar-status-error"
            );
        }
    }

    private void actualizarHistorialYEspecialidades() {
        if (pacienteSeleccionado == null) {
            historialPaciente = List.of();
            aplicarEspecialidadesPermitidas();
            return;
        }

        if (registrandoNuevoPaciente) {
            historialPaciente = List.of();
            aplicarEspecialidadesPermitidas();
            return;
        }

        try {
            historialPaciente = citaClient.listarPorPaciente(pacienteSeleccionado.getPersonaId());
        } catch (Exception e) {
            historialPaciente = List.of();
        }
        aplicarEspecialidadesPermitidas();
    }

    private List<String> obtenerEspecialidadesBase() {
        if (modoMedico && medicoSeleccionado != null) {
            List<String> especialidadesMedico = medicoSeleccionado.getEspecialidades();
            if (especialidadesMedico == null || especialidadesMedico.isEmpty()) {
                return List.of("GENERAL");
            }
            return especialidadesMedico;
        }
        return ESPECIALIDADES;
    }

    private void aplicarEspecialidadesPermitidas() {
        List<String> permitidas = ConsultaGeneralPolicy.filtrarEspecialidadesDisponibles(
                obtenerEspecialidadesBase(),
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

        if (pacienteSeleccionado != null && !registrandoNuevoPaciente
                && CitaProgramadaPolicy.tieneCitaProgramada(historialPaciente)) {
            mostrarEstadoGeneral(CitaProgramadaPolicy.mensajeBloqueo(), "agendar-status-warning");
        } else if (pacienteSeleccionado != null
                && !ConsultaGeneralPolicy.tieneConsultaGeneralAtendida(historialPaciente)) {
            if (permitidas.isEmpty()) {
                mostrarEstadoGeneral(
                        "Este paciente debe atender primero una Consulta General, "
                                + "pero el médico seleccionado no atiende Medicina General.",
                        "agendar-status-warning"
                );
            } else if (permitidas.size() == 1 && "GENERAL".equals(permitidas.getFirst())) {
                mostrarEstadoGeneral(ConsultaGeneralPolicy.mensajeRestriccion(), "agendar-status-warning");
            }
        }

        if (!modoMedico) {
            aplicarFiltroMedicos();
        }
    }

    private void configurarFormularioNuevoPaciente() {
        cmbGenero.setItems(FXCollections.observableArrayList("HOMBRE", "MUJER", "OTRO"));
        txtTelefono.setTextFormatter(PersonaFormSupport.digitsOnlyFormatter(10));
        txtDni.setTextFormatter(PersonaFormSupport.digitsOnlyFormatter(12));

        PersonaFormSupport.bindNameNormalization(txtPrimerNombre);
        PersonaFormSupport.bindNameNormalization(txtSegundoNombre);
        PersonaFormSupport.bindNameNormalization(txtPrimerApellido);
        PersonaFormSupport.bindNameNormalization(txtSegundoApellido);

        FormFieldHelper.bindClearOnChange(txtPrimerNombre, errPrimerNombre);
        FormFieldHelper.bindClearOnChange(txtSegundoNombre, errSegundoNombre);
        FormFieldHelper.bindClearOnChange(txtPrimerApellido, errPrimerApellido);
        FormFieldHelper.bindClearOnChange(txtSegundoApellido, errSegundoApellido);
        FormFieldHelper.bindClearOnChange(cmbGenero, errGenero);
        FormFieldHelper.bindClearOnChange(dpNacimiento, errFechaNacimiento);
        FormFieldHelper.bindClearOnChange(txtTelefono, errTelefono);
        FormFieldHelper.bindClearOnChange(txtDni, errDni);
        FormFieldHelper.bindClearOnChange(txtCorreo, errCorreo);
    }

    private void actualizarPanelNuevoPaciente() {
        panelNuevoPaciente.setVisible(registrandoNuevoPaciente);
        panelNuevoPaciente.setManaged(registrandoNuevoPaciente);
        if (!registrandoNuevoPaciente) {
            limpiarErroresNuevoPaciente();
        }
    }

    private void cargarFestivos() {
        try {
            fechasFestivas = new HashSet<>(configuracionClient.obtenerFestivos());
        } catch (Exception e) {
            fechasFestivas = new HashSet<>();
        }
    }

    private void cargarPacientes() {
        try {
            List<PacienteResponse> pacientes = pacienteClient.obtenerPacientes();
            Map<Long, PersonaResponse> personasPorId = personaClient.listarPersonas().stream()
                    .collect(Collectors.toMap(PersonaResponse::getId, p -> p, (a, b) -> a));

            List<PacienteListItem> items = new ArrayList<>();
            items.add(PacienteListItem.nuevoPaciente());
            pacientes.stream()
                    .map(p -> {
                        PersonaResponse persona = personasPorId.get(p.getPersonaId());
                        String nombre = persona != null
                                ? persona.getPrimerNombre() + " " + persona.getPrimerApellido()
                                : "Paciente #" + p.getPersonaId();
                        return new PacienteListItem(p.getPersonaId(), nombre.trim());
                    })
                    .sorted(Comparator.comparing(PacienteListItem::getNombreCompleto))
                    .forEach(items::add);

            cmbPacientes.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            mostrarEstadoGeneral(
                    "No se pudieron cargar los pacientes: " + e.getMessage(),
                    "agendar-status-error"
            );
        }
    }

    private void cargarMedicos() {
        try {
            todosLosMedicos = medicoClient.obtenerMedicos();
            aplicarFiltroMedicos();
            if (todosLosMedicos.isEmpty()) {
                mostrarEstadoGeneral(
                        "No hay médicos disponibles en este momento.",
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
        return medico.getEspecialidades().stream()
                .anyMatch(e -> e.equalsIgnoreCase(especialidad));
    }

    private void cargarSlots() {
        tablaSlots.getSelectionModel().clearSelection();
        slotSeleccionado = null;
        btnAgendar.setDisable(true);
        ocultarError(errSlot);
        ocultarEstadoSlots();

        boolean pacienteListo = pacienteSeleccionado != null
                && (!registrandoNuevoPaciente || pacienteSeleccionado.esNuevoPaciente());

        if (!pacienteListo || especialidadSeleccionada == null
                || medicoSeleccionado == null || dpFecha.getValue() == null) {
            tablaSlots.setItems(FXCollections.observableArrayList());
            actualizarGuia();
            return;
        }

        try {
            Long pacienteId = registrandoNuevoPaciente ? null : pacienteSeleccionado.getPersonaId();
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
                        "No hay horarios disponibles para esta fecha.",
                        "agendar-status-warning"
                );
                lblSlotsGuia.setText("No se encontraron horarios para la fecha seleccionada.");
            } else {
                lblSlotsGuia.setText(
                        "Seleccione la hora (" + slotsFiltrados.size() + " disponible"
                                + (slotsFiltrados.size() == 1 ? "" : "s") + ")."
                );
            }

        } catch (Exception e) {
            tablaSlots.setItems(FXCollections.observableArrayList());
            mostrarEstadoGeneral(
                    "Error al consultar horarios: " + e.getMessage(),
                    "agendar-status-error"
            );
        }

        actualizarGuia();
    }

    private void actualizarGuia() {
        lblPasoActual.getStyleClass().setAll("agendar-step-indicator");

        if (pacienteSeleccionado == null) {
            lblPasoActual.setText("Paso 1: Seleccione o registre el paciente");
        } else if (registrandoNuevoPaciente) {
            lblPasoActual.setText("Paso 1: Complete los datos del nuevo paciente");
        } else if (especialidadSeleccionada == null) {
            lblPasoActual.setText("Paso 2: Seleccione la especialidad");
        } else if (!modoMedico && medicoSeleccionado == null) {
            lblPasoActual.setText("Paso 3: Seleccione el médico");
        } else if (dpFecha.getValue() == null) {
            lblPasoActual.setText(modoMedico ? "Paso 3: Seleccione la fecha" : "Paso 4: Seleccione la fecha");
        } else if (slotSeleccionado == null) {
            lblPasoActual.setText(modoMedico ? "Paso 4: Seleccione la hora" : "Paso 5: Seleccione la hora");
        } else {
            lblPasoActual.setText(modoMedico ? "Paso 5: Confirme la cita" : "Paso 6: Confirme la cita");
        }
    }

    @FXML
    private void handleAgendar() {
        limpiarValidaciones();

        boolean valido = true;

        if (pacienteSeleccionado == null) {
            mostrarError(errPaciente, "Debe seleccionar un paciente o registrar uno nuevo.");
            marcarInputInvalido(cmbPacientes, true);
            valido = false;
        } else if (registrandoNuevoPaciente) {
            valido &= validarFormularioNuevoPaciente();
        } else if (CitaProgramadaPolicy.tieneCitaProgramada(historialPaciente)) {
            mostrarError(errPaciente, CitaProgramadaPolicy.mensajeBloqueo());
            marcarInputInvalido(cmbPacientes, true);
            valido = false;
        }

        if (especialidadSeleccionada == null || especialidadSeleccionada.isBlank()) {
            mostrarError(errEspecialidad, "Debe seleccionar una especialidad.");
            marcarInputInvalido(cmbEspecialidad, true);
            valido = false;
        } else if (!ConsultaGeneralPolicy.especialidadPermitida(especialidadSeleccionada, historialPaciente)) {
            mostrarError(errEspecialidad, ConsultaGeneralPolicy.mensajeRestriccion());
            marcarInputInvalido(cmbEspecialidad, true);
            valido = false;
        }

        if (medicoSeleccionado == null) {
            if (modoMedico) {
                mostrarEstadoGeneral(
                        "No se pudo identificar su perfil de médico.",
                        "agendar-status-error"
                );
            } else {
                mostrarError(errMedico, "Debe seleccionar un médico.");
                marcarInputInvalido(cmbMedicos, true);
            }
            valido = false;
        }

        if (dpFecha.getValue() == null) {
            mostrarError(errFecha, "Debe seleccionar una fecha.");
            marcarInputInvalido(dpFecha, true);
            valido = false;
        }

        if (slotSeleccionado == null) {
            mostrarError(errSlot, "Debe seleccionar un horario.");
            valido = false;
        }

        String motivo = motivoAgendamientoNormalizado();
        if (motivo != null && motivo.length() > MAX_MOTIVO_CARACTERES) {
            mostrarError(errMotivo, "El motivo no puede superar " + MAX_MOTIVO_CARACTERES + " caracteres.");
            marcarTextAreaInvalida(true);
            valido = false;
        }

        if (!valido) {
            actualizarGuia();
            return;
        }

        Long idCreador = creadorId != null ? creadorId : SessionPersonaResolver.resolverPersonaId();
        if (idCreador == null) {
            String perfil = modoMedico ? "médico" : "agendador";
            mostrarEstadoGeneral(
                    "No se pudo identificar su perfil de " + perfil + ". "
                            + "Cierre sesión e ingrese nuevamente, o contacte al administrador.",
                    "agendar-status-error"
            );
            return;
        }
        creadorId = idCreador;

        setAgendamientoEnProgreso(true);

        final Long creadorIdFinal = idCreador;
        CrearCitaAutonomaRequest requestBase = new CrearCitaAutonomaRequest(
                null,
                medicoSeleccionado.getPersonaId(),
                creadorIdFinal,
                slotSeleccionado,
                especialidadSeleccionada,
                motivo
        );

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Long pacienteId;

                if (registrandoNuevoPaciente) {
                    pacienteId = registrarNuevoPaciente();
                } else {
                    pacienteId = pacienteSeleccionado.getPersonaId();
                }

                requestBase.setPacienteId(pacienteId);
                crearCitaConReintento(requestBase);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setAgendamientoEnProgreso(false);
            mostrarEstadoGeneral(
                    registrandoNuevoPaciente
                            ? "¡Paciente registrado y cita agendada correctamente!"
                            : "¡Cita agendada correctamente!",
                    "agendar-status-success"
            );
            btnAgendar.setDisable(true);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5)
            );
            pause.setOnFinished(ev -> volverAlDashboard());
            pause.play();
        });

        task.setOnFailed(e -> {
            setAgendamientoEnProgreso(false);
            Throwable ex = task.getException();
            String mensaje = ex instanceof ApiClientException apiEx
                    ? apiEx.getMessage()
                    : (ex == null ? "No se pudo completar el agendamiento." : ex.getMessage());
            mostrarEstadoGeneral(mensajeAmigable(mensaje), "agendar-status-error");
            cargarSlots();
        });

        Thread worker = new Thread(task, "agendar-manual-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private Long registrarNuevoPaciente() throws Exception {
        CrearPersonaRequest personaRequest = buildPersonaRequest();
        Long personaId = null;

        try {
            personaId = personaClient.crearPersona(personaRequest);
            pacienteClient.crearPaciente(personaId);
            return personaId;
        } catch (Exception e) {
            if (personaId != null) {
                personaClient.compensarRegistroFallido(personaId);
            }
            throw e;
        }
    }

    private CrearPersonaRequest buildPersonaRequest() {
        CrearPersonaRequest request = new CrearPersonaRequest();
        request.setPrimerNombre(PersonaFormSupport.normalizedName(txtPrimerNombre));
        request.setSegundoNombre(PersonaFormSupport.normalizedNameOrNull(txtSegundoNombre));
        request.setPrimerApellido(PersonaFormSupport.normalizedName(txtPrimerApellido));
        request.setSegundoApellido(PersonaFormSupport.normalizedNameOrNull(txtSegundoApellido));
        request.setGenero(cmbGenero.getValue());
        if (dpNacimiento.getValue() != null) {
            request.setFechaNacimiento(dpNacimiento.getValue().toString());
        }
        request.setTelefono(PersonaFormSupport.trim(txtTelefono));
        request.setDni(PersonaFormSupport.trim(txtDni));
        request.setCorreo(PersonaFormSupport.trimOrNull(txtCorreo));
        return request;
    }

    private void crearCitaConReintento(CrearCitaAutonomaRequest request) throws Exception {
        long espera = ESPERA_INICIAL_MS;

        for (int intento = 1; intento <= MAX_REINTENTOS_CITA; intento++) {
            try {
                citaClient.crearCitaManual(request);
                return;
            } catch (Exception e) {
                if (!esErrorPacienteNoEncontrado(e) || intento == MAX_REINTENTOS_CITA) {
                    throw e;
                }
                Thread.sleep(espera);
                espera = Math.min(espera * 2, 2000L);
            }
        }
    }

    private boolean esErrorPacienteNoEncontrado(Exception e) {
        String mensaje = e.getMessage();
        if (mensaje == null) {
            return false;
        }
        String lower = mensaje.toLowerCase();
        return lower.contains("paciente no encontrado")
                || lower.contains("patient not found");
    }

    private boolean validarFormularioNuevoPaciente() {
        PersonaFormSupport.normalizeNameFields(
                txtPrimerNombre,
                txtSegundoNombre,
                txtPrimerApellido,
                txtSegundoApellido
        );

        boolean valido = true;
        valido &= PersonaFormSupport.requireName(txtPrimerNombre, errPrimerNombre, "Ingrese el primer nombre");
        valido &= PersonaFormSupport.optionalName(txtSegundoNombre, errSegundoNombre);
        valido &= PersonaFormSupport.requireName(txtPrimerApellido, errPrimerApellido, "Ingrese el primer apellido");
        valido &= PersonaFormSupport.optionalName(txtSegundoApellido, errSegundoApellido);

        if (cmbGenero.getValue() == null) {
            FormFieldHelper.showFieldError(cmbGenero, errGenero, "Seleccione un género");
            valido = false;
        }

        if (dpNacimiento.getValue() != null && dpNacimiento.getValue().isAfter(LocalDate.now())) {
            FormFieldHelper.showFieldError(dpNacimiento, errFechaNacimiento,
                    "La fecha no puede ser futura");
            valido = false;
        }

        String telefono = PersonaFormSupport.trim(txtTelefono);
        if (telefono.isEmpty()) {
            FormFieldHelper.showFieldError(txtTelefono, errTelefono, "Ingrese el teléfono");
            valido = false;
        } else if (telefono.length() != 10) {
            FormFieldHelper.showFieldError(txtTelefono, errTelefono,
                    "El teléfono debe tener 10 dígitos");
            valido = false;
        }

        String dni = PersonaFormSupport.trim(txtDni);
        if (dni.isEmpty()) {
            FormFieldHelper.showFieldError(txtDni, errDni, "Ingrese el DNI");
            valido = false;
        } else if (dni.length() < 6) {
            FormFieldHelper.showFieldError(txtDni, errDni,
                    "El DNI debe tener al menos 6 dígitos");
            valido = false;
        }

        String correo = PersonaFormSupport.trimOrNull(txtCorreo);
        if (correo != null && !EMAIL_PATTERN.matcher(correo).matches()) {
            FormFieldHelper.showFieldError(txtCorreo, errCorreo,
                    "Ingrese un correo válido. Ej: nombre@ejemplo.com");
            valido = false;
        }

        return valido;
    }

    private void limpiarErroresNuevoPaciente() {
        FormFieldHelper.clearFieldError(txtPrimerNombre, errPrimerNombre);
        FormFieldHelper.clearFieldError(txtSegundoNombre, errSegundoNombre);
        FormFieldHelper.clearFieldError(txtPrimerApellido, errPrimerApellido);
        FormFieldHelper.clearFieldError(txtSegundoApellido, errSegundoApellido);
        FormFieldHelper.clearFieldError(cmbGenero, errGenero);
        FormFieldHelper.clearFieldError(dpNacimiento, errFechaNacimiento);
        FormFieldHelper.clearFieldError(txtTelefono, errTelefono);
        FormFieldHelper.clearFieldError(txtDni, errDni);
        FormFieldHelper.clearFieldError(txtCorreo, errCorreo);
    }

    private void setAgendamientoEnProgreso(boolean enProgreso) {
        btnAgendar.setDisable(enProgreso || slotSeleccionado == null);
        btnVolver.setDisable(enProgreso);
        btnAgendar.setText(enProgreso ? "Agendando..." : "Agendar cita");
    }

    @FXML
    private void handleVolver() {
        volverAlDashboard();
    }

    private void volverAlDashboard() {
        if (modoMedico) {
            SessionManager.endAgendarManualComoMedico();
            SceneManager.switchScene(
                    "/view/dashboard/medico-dashboard.fxml",
                    btnVolver,
                    "PIEDRAZUL - Dashboard Médico"
            );
        } else {
            SceneManager.switchScene(
                    "/view/dashboard/agendador-dashboard.fxml",
                    btnVolver,
                    "PIEDRAZUL - Dashboard Agendador"
            );
        }
    }

    private void limpiarValidaciones() {
        ocultarError(errPaciente);
        ocultarError(errEspecialidad);
        ocultarError(errMedico);
        ocultarError(errFecha);
        ocultarError(errSlot);
        ocultarError(errMotivo);
        limpiarErroresNuevoPaciente();
        marcarTextAreaInvalida(false);
        marcarInputInvalido(cmbPacientes, false);
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
        if (txtMotivoAgendamiento == null) return null;
        String texto = txtMotivoAgendamiento.getText();
        return (texto == null || texto.isBlank()) ? null : texto.trim();
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

    private static String etiquetaEspecialidad(String codigo) {
        return EspecialidadLabels.etiqueta(codigo);
    }

    private static String mensajeAmigable(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "No se pudo completar el agendamiento.";
        }
        if (mensaje.contains("ya tiene una cita agendada")) {
            return "El paciente o médico ya tiene una cita en ese horario. Elija otro.";
        }
        if (mensaje.contains("ya no está disponible")) {
            return "El horario ya no está disponible. Seleccione otro.";
        }
        if (mensaje.contains("no atiende la especialidad")) {
            return "El médico no atiende esa especialidad.";
        }
        if (mensaje.contains("Ya existe una persona registrada con el DNI")) {
            return "Ya existe una persona con ese DNI. Búsquela en la lista de pacientes.";
        }
        if (mensaje.contains("ya está registrada como paciente")) {
            return "Esa persona ya es paciente. Búsquela en la lista.";
        }
        if (mensaje.contains("Paciente no encontrado")) {
            return "El paciente aún no está disponible en el sistema. Intente de nuevo en unos segundos.";
        }
        if (mensaje.contains("usuario creador") || mensaje.contains("usuarioCreadorId")) {
            return "No se pudo identificar al agendador. Cierre sesión e ingrese nuevamente.";
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
