package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.MedicoClient;
import com.piedrazul.frontend.client.PacienteClient;
import com.piedrazul.frontend.client.PersonaClient;
import com.piedrazul.frontend.client.UsuarioClient;
import com.piedrazul.frontend.dto.request.CrearPersonaRequest;
import com.piedrazul.frontend.dto.request.CrearUsuarioRequest;
import com.piedrazul.frontend.dto.response.PersonaResponse;
import com.piedrazul.frontend.util.ApiClientException;
import com.piedrazul.frontend.util.ApiErrorParser;
import com.piedrazul.frontend.util.FormFieldHelper;
import com.piedrazul.frontend.util.PersonaFormSupport;
import com.piedrazul.frontend.util.PiedrazulDialog;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[\\w.]{3,50}$");

    private static final String ROL_MEDICO_TERAPISTA = "MEDICO_TERAPISTA";

    private static final List<String> CODIGOS_ESPECIALIDAD = List.of(
            "GENERAL",
            "TERAPEUTA_NEURAL",
            "QUIROPRACTICO",
            "FISIOTERAPEUTA"
    );

    @FXML private TextField txtPrimerNombre;
    @FXML private TextField txtSegundoNombre;
    @FXML private TextField txtPrimerApellido;
    @FXML private TextField txtSegundoApellido;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private DatePicker dateNacimiento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDni;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRol;
    @FXML private Label lblEspecialidadesTitulo;
    @FXML private VBox panelEspecialidades;
    @FXML private VBox boxEspecialidades;
    @FXML private Label errEspecialidades;
    @FXML private VBox panelRegistroCompleto;
    @FXML private VBox panelVincularUsuario;
    @FXML private Label lblVincularTitulo;
    @FXML private Label lblVincularMensaje;
    @FXML private Label lblTituloCuenta;
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblFormError;
    @FXML private Button btnRegistrarse;
    @FXML private Button btnVolver;
    @FXML private Button btnCorregirDni;

    @FXML private Label errPrimerNombre;
    @FXML private Label errSegundoNombre;
    @FXML private Label errPrimerApellido;
    @FXML private Label errSegundoApellido;
    @FXML private Label errGenero;
    @FXML private Label errFechaNacimiento;
    @FXML private Label errTelefono;
    @FXML private Label errDni;
    @FXML private Label errCorreo;
    @FXML private Label errUsername;
    @FXML private Label errPassword;
    @FXML private Label errRol;

    private final PersonaClient personaClient = new PersonaClient();
    private final UsuarioClient usuarioClient = new UsuarioClient();
    private final PacienteClient pacienteClient = new PacienteClient();
    private final MedicoClient medicoClient = new MedicoClient();

    private final Map<String, CheckBox> especialidadChecks = new LinkedHashMap<>();

    private boolean modoVincularUsuario;
    private PersonaResponse personaExistente;
    private String dniVerificado;
    private boolean verificandoDni;
    private boolean registroEnProgreso;
    private boolean bloquearVerificacionDni;
    private int verificacionDniGeneracion;

    @FXML
    public void initialize() {
        if (!configurarContextoNavegacion()) {
            return;
        }

        cmbGenero.getItems().addAll("HOMBRE", "MUJER", "OTRO");
        cmbRol.getItems().addAll("PACIENTE", "MEDICO_TERAPISTA", "ADMINISTRADOR", "AGENDADOR");
        cmbRol.setValue("PACIENTE");

        txtTelefono.setTextFormatter(PersonaFormSupport.digitsOnlyFormatter(10));
        txtDni.setTextFormatter(PersonaFormSupport.digitsOnlyFormatter(12));

        PersonaFormSupport.bindNameNormalization(txtPrimerNombre);
        PersonaFormSupport.bindNameNormalization(txtSegundoNombre);
        PersonaFormSupport.bindNameNormalization(txtPrimerApellido);
        PersonaFormSupport.bindNameNormalization(txtSegundoApellido);

        bindClearOnChange(txtPrimerNombre, errPrimerNombre);
        bindClearOnChange(txtSegundoNombre, errSegundoNombre);
        bindClearOnChange(txtPrimerApellido, errPrimerApellido);
        bindClearOnChange(txtSegundoApellido, errSegundoApellido);
        bindClearOnChange(cmbGenero, errGenero);
        bindClearOnChange(dateNacimiento, errFechaNacimiento);
        bindClearOnChange(txtTelefono, errTelefono);
        bindClearOnChange(txtDni, errDni);
        txtDni.focusedProperty().addListener((obs, estabaEnfocado, estaEnfocado) -> {
            if (estabaEnfocado && !estaEnfocado) {
                verificarDniAlSalirDelCampo();
            }
        });
        txtDni.textProperty().addListener((obs, anterior, nuevo) -> {
            if (modoVincularUsuario && (dniVerificado == null || !dniVerificado.equals(nuevo == null ? "" : nuevo.trim()))) {
                desactivarModoVincularUsuario();
            }
        });
        bindClearOnChange(txtCorreo, errCorreo);
        bindClearOnChange(txtUsername, errUsername);
        bindClearOnChange(txtPassword, errPassword);
        bindClearOnChange(cmbRol, errRol);

        inicializarEspecialidades();
        configurarVisibilidadEspecialidades(cmbRol.getValue());
        cmbRol.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (modoVincularUsuario && !"PACIENTE".equals(nuevo)) {
                desactivarModoVincularUsuario();
            }
            configurarVisibilidadEspecialidades(nuevo);
        });

        if (!SessionManager.isRegisterFromAdminPanel()) {
            javafx.application.Platform.runLater(txtDni::requestFocus);
        }
    }

    private void verificarDniAlSalirDelCampo() {
        if (SessionManager.isRegisterFromAdminPanel()
                || verificandoDni
                || modoVincularUsuario
                || registroEnProgreso
                || bloquearVerificacionDni
                || tieneDatosPersonalesIngresados()) {
            return;
        }
        if (!"PACIENTE".equals(cmbRol.getValue())) {
            return;
        }

        String dni = PersonaFormSupport.trim(txtDni);
        if (dni.length() < 6) {
            return;
        }

        verificandoDni = true;
        final int generacion = ++verificacionDniGeneracion;

        Task<PersonaResponse> task = new Task<>() {
            @Override
            protected PersonaResponse call() {
                return personaClient.buscarPorDni(dni).orElse(null);
            }
        };

        task.setOnSucceeded(e -> {
            verificandoDni = false;
            if (!debeAplicarResultadoVerificacionDni(generacion)) {
                return;
            }
            PersonaResponse persona = task.getValue();
            if (persona != null) {
                mostrarModalPacienteExistente(persona, dni);
            }
        });

        task.setOnFailed(e -> {
            verificandoDni = false;
            Throwable ex = task.getException();
            if (ex != null) {
                System.err.println("Error verificando DNI: " + ex.getMessage());
            }
        });

        Thread worker = new Thread(task, "verificar-dni-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean debeAplicarResultadoVerificacionDni(int generacion) {
        return generacion == verificacionDniGeneracion
                && !bloquearVerificacionDni
                && !registroEnProgreso
                && !modoVincularUsuario
                && !tieneDatosPersonalesIngresados();
    }

    private boolean tieneDatosPersonalesIngresados() {
        return !PersonaFormSupport.normalizedName(txtPrimerNombre).isEmpty()
                || !PersonaFormSupport.normalizedName(txtPrimerApellido).isEmpty()
                || !PersonaFormSupport.trim(txtTelefono).isEmpty()
                || cmbGenero.getValue() != null
                || dateNacimiento.getValue() != null
                || !PersonaFormSupport.trim(txtCorreo).isEmpty();
    }

    private void mostrarModalPacienteExistente(PersonaResponse persona, String dni) {
        String nombre = persona.getNombreCompleto();
        if (nombre == null || nombre.isBlank()) {
            nombre = "registrado en el sistema";
        }

        PiedrazulDialog.showInfo(
                txtDni,
                "Ya está registrado",
                "El DNI ingresado corresponde a " + nombre + ".\n\n"
                        + "Sus datos personales ya están en el sistema. "
                        + "Solo debe crear su usuario con nombre de usuario y contraseña para ingresar."
        );

        activarModoVincularUsuario(persona, dni);
    }

    private void activarModoVincularUsuario(PersonaResponse persona, String dni) {
        modoVincularUsuario = true;
        personaExistente = persona;
        dniVerificado = dni;

        panelRegistroCompleto.setVisible(false);
        panelRegistroCompleto.setManaged(false);

        panelVincularUsuario.setVisible(true);
        panelVincularUsuario.setManaged(true);

        String nombre = persona.getNombreCompleto();
        lblVincularTitulo.setText("Paciente encontrado: " + (nombre == null || nombre.isBlank() ? "DNI " + dni : nombre));
        lblVincularMensaje.setText(
                "Complete únicamente los datos de su cuenta (usuario y contraseña). "
                        + "El rol quedará como Paciente."
        );

        lblTituloCuenta.setText("2. Crear su cuenta de acceso");
        cmbRol.setValue("PACIENTE");
        cmbRol.setDisable(true);
        txtDni.setDisable(true);
        btnRegistrarse.setText("Crear usuario");

        limpiarErroresDatosPersonales();
        clearFormError();
    }

    private void desactivarModoVincularUsuario() {
        if (!modoVincularUsuario) {
            return;
        }

        modoVincularUsuario = false;
        personaExistente = null;
        dniVerificado = null;

        panelRegistroCompleto.setVisible(true);
        panelRegistroCompleto.setManaged(true);

        panelVincularUsuario.setVisible(false);
        panelVincularUsuario.setManaged(false);

        lblTituloCuenta.setText("4. Datos de cuenta");
        cmbRol.setDisable(false);
        txtDni.setDisable(false);
        btnRegistrarse.setText("Registrarse");
    }

    private void limpiarErroresDatosPersonales() {
        FormFieldHelper.clearFieldError(txtPrimerNombre, errPrimerNombre);
        FormFieldHelper.clearFieldError(txtSegundoNombre, errSegundoNombre);
        FormFieldHelper.clearFieldError(txtPrimerApellido, errPrimerApellido);
        FormFieldHelper.clearFieldError(txtSegundoApellido, errSegundoApellido);
        FormFieldHelper.clearFieldError(cmbGenero, errGenero);
        FormFieldHelper.clearFieldError(dateNacimiento, errFechaNacimiento);
        FormFieldHelper.clearFieldError(txtTelefono, errTelefono);
        FormFieldHelper.clearFieldError(txtDni, errDni);
        FormFieldHelper.clearFieldError(txtCorreo, errCorreo);
    }

    private void inicializarEspecialidades() {
        for (String codigo : CODIGOS_ESPECIALIDAD) {
            CheckBox checkBox = new CheckBox(etiquetaEspecialidad(codigo));
            checkBox.setUserData(codigo);
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                    limpiarErrorEspecialidades()
            );
            especialidadChecks.put(codigo, checkBox);
            boxEspecialidades.getChildren().add(checkBox);
        }
    }

    private void configurarVisibilidadEspecialidades(String rol) {
        boolean esMedicoTerapeuta = ROL_MEDICO_TERAPISTA.equals(rol);

        lblEspecialidadesTitulo.setVisible(esMedicoTerapeuta);
        lblEspecialidadesTitulo.setManaged(esMedicoTerapeuta);

        panelEspecialidades.setVisible(esMedicoTerapeuta);
        panelEspecialidades.setManaged(esMedicoTerapeuta);

        especialidadChecks.values().forEach(cb -> cb.setDisable(!esMedicoTerapeuta));

        if (!esMedicoTerapeuta) {
            limpiarEspecialidadesSeleccionadas();
            limpiarErrorEspecialidades();
        }
    }

    private void limpiarErrorEspecialidades() {
        CheckBox referencia = especialidadChecks.isEmpty()
                ? null
                : especialidadChecks.get(CODIGOS_ESPECIALIDAD.get(0));
        if (referencia != null) {
            FormFieldHelper.clearFieldError(referencia, boxEspecialidades, errEspecialidades);
        }
    }

    private void limpiarEspecialidadesSeleccionadas() {
        especialidadChecks.values().forEach(cb -> cb.setSelected(false));
    }

    private List<String> obtenerEspecialidadesSeleccionadas() {
        List<String> seleccionadas = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : especialidadChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                seleccionadas.add(entry.getKey());
            }
        }
        return seleccionadas;
    }

    private static String etiquetaEspecialidad(String codigo) {
        return switch (codigo) {
            case "TERAPEUTA_NEURAL" -> "Terapeuta Neural";
            case "QUIROPRACTICO" -> "Quiropráctico";
            case "FISIOTERAPEUTA" -> "Fisioterapeuta";
            default -> "Medicina General";
        };
    }

    @FXML
    private void handleRegister() {
        clearFormError();

        if (!validateForm()) {
            return;
        }

        cancelarVerificacionDniPendiente();
        bloquearVerificacionDni = true;

        String rol = cmbRol.getValue();
        String username = PersonaFormSupport.trim(txtUsername);
        String password = txtPassword.getText();
        final boolean fueRegistroVinculado = modoVincularUsuario;

        setRegistrationInProgress(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (modoVincularUsuario) {
                    registrarUsuarioParaPersonaExistente(username, password);
                    return null;
                }

                CrearPersonaRequest personaRequest = buildPersonaRequest();
                List<String> especialidades = ROL_MEDICO_TERAPISTA.equals(rol)
                        ? List.copyOf(obtenerEspecialidadesSeleccionadas())
                        : List.of();
                String correo = PersonaFormSupport.trimOrNull(txtCorreo);
                String firstName = PersonaFormSupport.normalizedName(txtPrimerNombre);
                String lastName = PersonaFormSupport.normalizedName(txtPrimerApellido);

                Long personaId = null;
                try {
                    personaId = personaClient.crearPersona(personaRequest);

                    if ("PACIENTE".equals(rol)) {
                        pacienteClient.crearPaciente(personaId);
                    } else if (ROL_MEDICO_TERAPISTA.equals(rol)) {
                        medicoClient.crearMedico(personaId, "MEDICO", especialidades);
                    }

                    crearUsuario(
                            personaId,
                            username,
                            password,
                            correo,
                            firstName,
                            lastName,
                            rol
                    );
                    return null;
                } catch (Exception e) {
                    revertirRegistro(personaId);
                    throw e;
                }
            }
        };

        task.setOnSucceeded(e -> {
            cancelarVerificacionDniPendiente();
            String mensaje = fueRegistroVinculado
                    ? "Cuenta de usuario creada correctamente. Ya puede iniciar sesión."
                    : "Usuario registrado correctamente";
            showAlert("Éxito", mensaje, Alert.AlertType.INFORMATION);
            volverDespuesDeRegistro();
            bloquearVerificacionDni = false;
            setRegistrationInProgress(false);
        });

        task.setOnFailed(e -> {
            bloquearVerificacionDni = false;
            setRegistrationInProgress(false);
            Throwable ex = task.getException();
            if (ex instanceof ApiClientException apiEx) {
                System.err.println("=== ERROR ApiClientException ===");
                System.err.println("Mensaje: " + apiEx.getMessage());
                apiEx.printStackTrace();
                mapServerError(apiEx.getParsedError());
            } else {
                System.err.println("=== ERROR GENERAL ===");
                System.err.println("Mensaje: " + (ex == null ? "desconocido" : ex.getMessage()));
                if (ex != null) {
                    ex.printStackTrace();
                }
                mapServerError(ApiErrorParser.parse(ex == null ? null : ex.getMessage()));
            }
        });

        Thread worker = new Thread(task, "register-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void registrarUsuarioParaPersonaExistente(String username, String password) {
        if (personaExistente == null || personaExistente.getId() == null) {
            throw new IllegalStateException("No se encontró la persona asociada al DNI.");
        }

        Long personaId = personaExistente.getId();
        asegurarPacienteRegistrado(personaId);

        crearUsuario(
                personaId,
                username,
                password,
                personaExistente.getCorreo(),
                personaExistente.getPrimerNombre(),
                personaExistente.getPrimerApellido(),
                "PACIENTE"
        );
    }

    private void asegurarPacienteRegistrado(Long personaId) {
        try {
            pacienteClient.crearPaciente(personaId);
        } catch (Exception e) {
            if (!esPacienteYaRegistrado(e)) {
                throw e;
            }
        }
    }

    private boolean esPacienteYaRegistrado(Throwable error) {
        String mensaje = error.getMessage() == null ? "" : error.getMessage();
        if (error instanceof ApiClientException apiEx && apiEx.getParsedError() != null) {
            mensaje = mensaje + " " + apiEx.getParsedError().message();
        }
        String normalizado = mensaje.toLowerCase();
        return normalizado.contains("ya está registrada como paciente")
                || normalizado.contains("ya registrada como paciente");
    }

    private void crearUsuario(
            Long personaId,
            String username,
            String password,
            String correo,
            String firstName,
            String lastName,
            String rol
    ) {
        CrearUsuarioRequest usuarioRequest = new CrearUsuarioRequest();
        usuarioRequest.setPersonaId(personaId);
        usuarioRequest.setUsername(username);
        usuarioRequest.setPassword(password);
        usuarioRequest.setEmail(correo);
        usuarioRequest.setFirstName(firstName);
        usuarioRequest.setLastName(lastName);
        usuarioRequest.setRoles(List.of(rol));
        usuarioClient.crearUsuario(usuarioRequest);
    }

    private CrearPersonaRequest buildPersonaRequest() {
        CrearPersonaRequest personaRequest = new CrearPersonaRequest();
        personaRequest.setPrimerNombre(PersonaFormSupport.normalizedName(txtPrimerNombre));
        personaRequest.setSegundoNombre(PersonaFormSupport.normalizedNameOrNull(txtSegundoNombre));
        personaRequest.setPrimerApellido(PersonaFormSupport.normalizedName(txtPrimerApellido));
        personaRequest.setSegundoApellido(PersonaFormSupport.normalizedNameOrNull(txtSegundoApellido));
        personaRequest.setGenero(cmbGenero.getValue());
        personaRequest.setFechaNacimiento(dateNacimiento.getValue().toString());
        personaRequest.setTelefono(PersonaFormSupport.trim(txtTelefono));
        personaRequest.setDni(PersonaFormSupport.trim(txtDni));
        personaRequest.setCorreo(PersonaFormSupport.trimOrNull(txtCorreo));
        return personaRequest;
    }

    private void cancelarVerificacionDniPendiente() {
        verificacionDniGeneracion++;
        verificandoDni = false;
    }

    private void setRegistrationInProgress(boolean inProgress) {
        registroEnProgreso = inProgress;
        btnRegistrarse.setDisable(inProgress);
        btnVolver.setDisable(inProgress);
        if (inProgress) {
            btnRegistrarse.setText(modoVincularUsuario ? "Creando usuario..." : "Registrando...");
        } else {
            btnRegistrarse.setText(modoVincularUsuario ? "Crear usuario" : "Registrarse");
        }
    }

    private void revertirRegistro(Long personaId) {
        if (personaId != null) {
            personaClient.compensarRegistroFallido(personaId);
        }
    }

    @FXML
    private void handleCorregirDni() {
        desactivarModoVincularUsuario();
        txtDni.clear();
        txtDni.requestFocus();
    }

    @FXML
    private void handleVolver() {
        volverSinRegistrar();
    }

    private boolean configurarContextoNavegacion() {
        if (SessionManager.isRegisterFromAdminPanel()) {
            if (!SessionManager.isLoggedIn() || !SessionManager.hasRole("ADMINISTRADOR")) {
                SessionManager.endRegisterFromAdminPanel();
                SceneManager.showLogin("/view/auth_register/loginView.fxml", lblTitulo);
                return false;
            }

            lblTitulo.setText("REGISTRO DE USUARIO");
            lblSubtitulo.setText("Panel de administración — complete los datos del nuevo usuario.");
            lblSubtitulo.setVisible(true);
            lblSubtitulo.setManaged(true);
            btnVolver.setText("Volver al menú principal");
            return true;
        }

        lblSubtitulo.setVisible(false);
        lblSubtitulo.setManaged(false);
        btnVolver.setText("Volver al login");
        return true;
    }

    private void volverSinRegistrar() {
        if (SessionManager.isRegisterFromAdminPanel()) {
            SessionManager.endRegisterFromAdminPanel();
            SceneManager.showDashboard(
                    "/view/dashboard/administrador-dashboard.fxml",
                    btnVolver,
                    "PIEDRAZUL - Administrador"
            );
            return;
        }

        SessionManager.endRegisterFromAdminPanel();
        SceneManager.showLogin("/view/auth_register/loginView.fxml", btnVolver);
    }

    private void volverDespuesDeRegistro() {
        if (SessionManager.isRegisterFromAdminPanel()) {
            SessionManager.endRegisterFromAdminPanel();
            SceneManager.showDashboard(
                    "/view/dashboard/administrador-dashboard.fxml",
                    btnVolver,
                    "PIEDRAZUL - Administrador"
            );
            return;
        }

        SessionManager.endRegisterFromAdminPanel();
        SceneManager.showLogin("/view/auth_register/loginView.fxml", btnVolver);
    }

    private boolean validateForm() {
        boolean valid = true;

        valid &= validarDni();

        if (!modoVincularUsuario) {
            valid &= validarDatosPersonales();
        } else if (personaExistente == null) {
            showFormError("No se pudo vincular el DNI. Verifíquelo e intente nuevamente.");
            valid = false;
        }

        String username = PersonaFormSupport.trim(txtUsername);
        if (username.isEmpty()) {
            FormFieldHelper.showFieldError(txtUsername, errUsername, "Ingrese un usuario");
            valid = false;
        } else if (!USERNAME_PATTERN.matcher(username).matches()) {
            FormFieldHelper.showFieldError(txtUsername, errUsername,
                    "Use 3-50 caracteres: letras, números, punto o guion bajo");
            valid = false;
        }

        String password = txtPassword.getText() == null ? "" : txtPassword.getText();
        if (password.isBlank()) {
            FormFieldHelper.showFieldError(txtPassword, errPassword, "Ingrese una contraseña");
            valid = false;
        } else if (password.length() < 6) {
            FormFieldHelper.showFieldError(txtPassword, errPassword,
                    "La contraseña debe tener al menos 6 caracteres");
            valid = false;
        }

        if (cmbRol.getValue() == null) {
            FormFieldHelper.showFieldError(cmbRol, errRol, "Seleccione un rol");
            valid = false;
        }

        if (!modoVincularUsuario
                && ROL_MEDICO_TERAPISTA.equals(cmbRol.getValue())
                && obtenerEspecialidadesSeleccionadas().isEmpty()) {
            CheckBox referencia = especialidadChecks.get(CODIGOS_ESPECIALIDAD.get(0));
            FormFieldHelper.showFieldError(
                    referencia,
                    boxEspecialidades,
                    errEspecialidades,
                    "Seleccione al menos una especialidad"
            );
            valid = false;
        }

        return valid;
    }

    private boolean validarDni() {
        String dni = PersonaFormSupport.trim(txtDni);
        if (dni.isEmpty()) {
            FormFieldHelper.showFieldError(txtDni, errDni, "Ingrese el DNI");
            return false;
        }
        if (dni.length() < 6) {
            FormFieldHelper.showFieldError(txtDni, errDni,
                    "El DNI debe tener al menos 6 dígitos");
            return false;
        }
        return true;
    }

    private boolean validarDatosPersonales() {
        PersonaFormSupport.normalizeNameFields(
                txtPrimerNombre,
                txtSegundoNombre,
                txtPrimerApellido,
                txtSegundoApellido
        );

        boolean valid = true;

        valid &= PersonaFormSupport.requireName(txtPrimerNombre, errPrimerNombre, "Ingrese el primer nombre");
        valid &= PersonaFormSupport.optionalName(txtSegundoNombre, errSegundoNombre);
        valid &= PersonaFormSupport.requireName(txtPrimerApellido, errPrimerApellido, "Ingrese el primer apellido");
        valid &= PersonaFormSupport.optionalName(txtSegundoApellido, errSegundoApellido);

        if (cmbGenero.getValue() == null) {
            FormFieldHelper.showFieldError(cmbGenero, errGenero, "Seleccione un género");
            valid = false;
        }

        if (dateNacimiento.getValue() == null) {
            FormFieldHelper.showFieldError(dateNacimiento, errFechaNacimiento,
                    "Seleccione la fecha de nacimiento");
            valid = false;
        } else if (dateNacimiento.getValue().isAfter(LocalDate.now())) {
            FormFieldHelper.showFieldError(dateNacimiento, errFechaNacimiento,
                    "La fecha no puede ser futura");
            valid = false;
        }

        String telefono = PersonaFormSupport.trim(txtTelefono);
        if (telefono.isEmpty()) {
            FormFieldHelper.showFieldError(txtTelefono, errTelefono, "Ingrese el teléfono");
            valid = false;
        } else if (telefono.length() != 10) {
            FormFieldHelper.showFieldError(txtTelefono, errTelefono,
                    "El teléfono debe tener 10 dígitos");
            valid = false;
        }

        String correo = PersonaFormSupport.trimOrNull(txtCorreo);
        if (correo != null && !EMAIL_PATTERN.matcher(correo).matches()) {
            FormFieldHelper.showFieldError(txtCorreo, errCorreo,
                    "Ingrese un correo válido. Ej: nombre@ejemplo.com");
            valid = false;
        }

        return valid;
    }

    private void mapServerError(ApiErrorParser.ParsedApiError parsed) {
        if (parsed == null) {
            showFormError("No se pudo completar el registro. Intente nuevamente.");
            return;
        }

        boolean mapped = false;
        for (Map.Entry<String, String> entry : parsed.fieldErrors().entrySet()) {
            mapped |= applyFieldError(entry.getKey(), humanizeFieldError(entry.getKey(), entry.getValue()));
        }

        if (mapped) {
            return;
        }

        String message = parsed.message() == null ? "" : parsed.message().toLowerCase();

        if (message.contains("ya existe un usuario") || message.contains("usuario para ese personaid")) {
            showFormError("Ya tiene una cuenta de usuario. Inicie sesión con su usuario y contraseña.");
        } else if (message.contains("dni")) {
            FormFieldHelper.showFieldError(txtDni, errDni, parsed.message());
        } else if (message.contains("username") || message.contains("usuario")) {
            FormFieldHelper.showFieldError(txtUsername, errUsername, humanizeFieldError("username", parsed.message()));
        } else if (message.contains("email") || message.contains("correo")) {
            FormFieldHelper.showFieldError(txtCorreo, errCorreo, humanizeFieldError("email", parsed.message()));
        } else if (message.contains("password") || message.contains("contrase")) {
            FormFieldHelper.showFieldError(txtPassword, errPassword, humanizeFieldError("password", parsed.message()));
        } else {
            showFormError(parsed.message());
        }
    }

    private boolean applyFieldError(String field, String message) {
        return switch (field) {
            case "primerNombre" -> {
                FormFieldHelper.showFieldError(txtPrimerNombre, errPrimerNombre, message);
                yield true;
            }
            case "segundoNombre" -> {
                FormFieldHelper.showFieldError(txtSegundoNombre, errSegundoNombre, message);
                yield true;
            }
            case "primerApellido" -> {
                FormFieldHelper.showFieldError(txtPrimerApellido, errPrimerApellido, message);
                yield true;
            }
            case "segundoApellido" -> {
                FormFieldHelper.showFieldError(txtSegundoApellido, errSegundoApellido, message);
                yield true;
            }
            case "genero" -> {
                FormFieldHelper.showFieldError(cmbGenero, errGenero, message);
                yield true;
            }
            case "fechaNacimiento" -> {
                FormFieldHelper.showFieldError(dateNacimiento, errFechaNacimiento, message);
                yield true;
            }
            case "telefono" -> {
                FormFieldHelper.showFieldError(txtTelefono, errTelefono, message);
                yield true;
            }
            case "dni" -> {
                FormFieldHelper.showFieldError(txtDni, errDni, message);
                yield true;
            }
            case "correo", "email" -> {
                FormFieldHelper.showFieldError(txtCorreo, errCorreo, message);
                yield true;
            }
            case "username" -> {
                FormFieldHelper.showFieldError(txtUsername, errUsername, message);
                yield true;
            }
            case "password" -> {
                FormFieldHelper.showFieldError(txtPassword, errPassword, message);
                yield true;
            }
            case "roles", "rol" -> {
                FormFieldHelper.showFieldError(cmbRol, errRol, message);
                yield true;
            }
            default -> false;
        };
    }

    private String humanizeFieldError(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return "Revise este campo.";
        }
        if ("email".equals(field) && raw.toLowerCase().contains("obligatorio")) {
            return "El correo es opcional. Reinicie usuarios-service si ve este mensaje.";
        }
        if (raw.toLowerCase().contains("email invalido")) {
            return "Ingrese un correo válido. Ej: nombre@ejemplo.com";
        }
        if ("username".equals(field) && raw.contains("3")) {
            return "Use entre 3 y 50 caracteres: letras, números, punto o guion bajo";
        }
        if ("password".equals(field)) {
            return "La contraseña debe tener al menos 6 caracteres";
        }
        return raw;
    }

    private void showFormError(String message) {
        lblFormError.setText(message);
        lblFormError.setVisible(true);
        lblFormError.setManaged(true);
    }

    private void clearFormError() {
        lblFormError.setText("");
        lblFormError.setVisible(false);
        lblFormError.setManaged(false);
    }

    private void bindClearOnChange(Control control, Label errorLabel) {
        FormFieldHelper.bindClearOnChange(control, errorLabel);
        if (control instanceof TextInputControl) {
            ((TextInputControl) control).textProperty().addListener(
                    (obs, oldVal, newVal) -> clearFormError()
            );
        } else if (control instanceof ComboBox<?>) {
            ((ComboBox<?>) control).valueProperty().addListener(
                    (obs, oldVal, newVal) -> clearFormError()
            );
        } else if (control instanceof DatePicker datePicker) {
            datePicker.valueProperty().addListener(
                    (obs, oldVal, newVal) -> clearFormError()
            );
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
