package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.ConfiguracionClient;
import com.piedrazul.frontend.client.DisponibilidadClient;
import com.piedrazul.frontend.client.MedicoClient;
import com.piedrazul.frontend.dto.DisponibilidadRow;
import com.piedrazul.frontend.dto.response.ConfiguracionResponse;
import com.piedrazul.frontend.dto.response.DisponibilidadResponse;
import com.piedrazul.frontend.dto.response.MedicoResponse;
import com.piedrazul.frontend.util.FormFieldHelper;
import com.piedrazul.frontend.util.IntegerInputHelper;
import com.piedrazul.frontend.util.SceneManager;
import com.piedrazul.frontend.util.TimeInputHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class ConfiguracionDisponibilidadController {

    private static final Pattern TIME_INPUT = Pattern.compile("[0-9:]*");
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d*");

    @FXML private ComboBox<MedicoResponse> cmbMedicos;
    @FXML private ComboBox<String> cmbDiaSemana;
    @FXML private TextField txtHoraInicio;
    @FXML private TextField txtHoraFin;
    @FXML private TextField txtIntervalo;
    @FXML private TextField txtSemanas;

    @FXML private Button btnGuardarConfiguracion;
    @FXML private Button btnEditarConfiguracion;
    @FXML private Label lblEstadoConfiguracion;
    @FXML private Label lblFormError;
    @FXML private Label lblEstadoEdicionDisponibilidad;
    @FXML private Label lblFiltroTabla;

    @FXML private Label errSemanas;
    @FXML private Label errMedico;
    @FXML private Label errDia;
    @FXML private Label errHoraInicio;
    @FXML private Label errHoraFin;
    @FXML private Label errIntervalo;

    @FXML private VBox contenedorDisponibilidad;

    @FXML private TableView<DisponibilidadRow> tablaDisponibilidad;
    @FXML private TableColumn<DisponibilidadRow, String> colMedico;
    @FXML private TableColumn<DisponibilidadRow, String> colDia;
    @FXML private TableColumn<DisponibilidadRow, String> colHoraInicio;
    @FXML private TableColumn<DisponibilidadRow, String> colHoraFin;
    @FXML private TableColumn<DisponibilidadRow, Integer> colIntervalo;

    @FXML private Button btnAgregarDisponibilidad;
    @FXML private Button btnEditarDisponibilidad;
    @FXML private Button btnGuardarCambiosDisponibilidad;
    @FXML private Button btnEliminarDisponibilidad;
    @FXML private Button btnCancelarEdicionDisponibilidad;

    private final ObservableList<DisponibilidadRow> data = FXCollections.observableArrayList();
    private final ObservableList<DisponibilidadRow> todasLasDisponibilidades = FXCollections.observableArrayList();

    private final MedicoClient medicoClient = new MedicoClient();
    private final DisponibilidadClient disponibilidadClient = new DisponibilidadClient();
    private final ConfiguracionClient configuracionClient = new ConfiguracionClient();

    private Long disponibilidadEnEdicionId;

    @FXML
    public void initialize() {
        colMedico.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMedico()));
        colDia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDia()));
        colHoraInicio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHoraInicio()));
        colHoraFin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHoraFin()));
        colIntervalo.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getIntervalo()));

        tablaDisponibilidad.setItems(data);
        tablaDisponibilidad.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> actualizarBotonesSegunSeleccion());

        cmbDiaSemana.getItems().addAll(
                "LUNES", "MARTES", "MIERCOLES",
                "JUEVES", "VIERNES", "SABADO", "DOMINGO"
        );

        txtSemanas.setTextFormatter(digitsFormatter(2));
        txtIntervalo.setTextFormatter(digitsFormatter(3));
        txtHoraInicio.setTextFormatter(timeFormatter());
        txtHoraFin.setTextFormatter(timeFormatter());

        bindClearOnChange(txtSemanas, errSemanas);
        bindClearOnChange(cmbMedicos, errMedico);
        bindClearOnChange(cmbDiaSemana, errDia);
        bindClearOnChange(txtHoraInicio, errHoraInicio);
        bindClearOnChange(txtHoraFin, errHoraFin);
        bindClearOnChange(txtIntervalo, errIntervalo);

        bindTimeNormalization(txtHoraInicio);
        bindTimeNormalization(txtHoraFin);

        cmbMedicos.getItems().addAll(medicoClient.obtenerMedicos());
        cmbMedicos.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltroTabla());

        actualizarModoFormulario(false);
        cargarConfiguracion();
        cargarDisponibilidades();
    }

    @FXML
    private void handleAgregar() {
        clearFormError();

        if (!validateDisponibilidadForm()) {
            return;
        }

        try {
            Long medicoId = cmbMedicos.getValue().getPersonaId();
            String dia = cmbDiaSemana.getValue();
            String horaInicio = TimeInputHelper.normalize(txtHoraInicio.getText());
            String horaFin = TimeInputHelper.normalize(txtHoraFin.getText());
            Integer intervalo = IntegerInputHelper.parsePositiveInteger(txtIntervalo.getText());

            disponibilidadClient.crearDisponibilidad(
                    medicoId, dia, horaInicio, horaFin, intervalo
            );

            mostrarAlerta("Éxito", "Disponibilidad registrada correctamente", Alert.AlertType.INFORMATION);
            limpiarCamposDisponibilidad();
            cargarDisponibilidades();

        } catch (Exception e) {
            showFormError(extractMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleEditarDisponibilidad() {
        DisponibilidadRow seleccionada = tablaDisponibilidad.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }

        clearFormError();
        clearDisponibilidadErrors();
        disponibilidadEnEdicionId = seleccionada.getId();

        seleccionarMedico(seleccionada.getMedicoId());
        cmbDiaSemana.setValue(seleccionada.getDia());
        txtHoraInicio.setText(seleccionada.getHoraInicio());
        txtHoraFin.setText(seleccionada.getHoraFin());
        txtIntervalo.setText(String.valueOf(seleccionada.getIntervalo()));

        actualizarModoFormulario(true);
    }

    @FXML
    private void handleGuardarCambiosDisponibilidad() {
        if (disponibilidadEnEdicionId == null) {
            return;
        }

        clearFormError();

        if (!validateDisponibilidadForm()) {
            return;
        }

        try {
            Long medicoId = cmbMedicos.getValue().getPersonaId();
            String dia = cmbDiaSemana.getValue();
            String horaInicio = TimeInputHelper.normalize(txtHoraInicio.getText());
            String horaFin = TimeInputHelper.normalize(txtHoraFin.getText());
            Integer intervalo = IntegerInputHelper.parsePositiveInteger(txtIntervalo.getText());

            disponibilidadClient.actualizarDisponibilidad(
                    disponibilidadEnEdicionId,
                    medicoId, dia, horaInicio, horaFin, intervalo
            );

            mostrarAlerta("Éxito", "Disponibilidad actualizada correctamente", Alert.AlertType.INFORMATION);
            salirModoEdicion();
            cargarDisponibilidades();

        } catch (Exception e) {
            showFormError(extractMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleEliminarDisponibilidad() {
        DisponibilidadRow seleccionada = tablaDisponibilidad.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "¿Desea eliminar la disponibilidad seleccionada?\n"
                        + seleccionada.getMedico() + " - " + seleccionada.getDia()
                        + " (" + seleccionada.getHoraInicio() + " a " + seleccionada.getHoraFin() + ")"
        );

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return;
        }

        try {
            disponibilidadClient.eliminarDisponibilidad(seleccionada.getId());
            mostrarAlerta("Éxito", "Disponibilidad eliminada correctamente", Alert.AlertType.INFORMATION);
            salirModoEdicion();
            cargarDisponibilidades();
        } catch (Exception e) {
            showFormError(extractMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleCancelarEdicionDisponibilidad() {
        salirModoEdicion();
    }

    @FXML
    private void handleVolver() {
        SceneManager.showDashboard(
                "/view/dashboard/administrador-dashboard.fxml",
                cmbMedicos,
                "Dashboard Administrador"
        );
    }

    @FXML
    private void handleGuardarConfiguracion() {
        FormFieldHelper.clearFieldError(txtSemanas, errSemanas);

        Integer semanas = validateSemanas();
        if (semanas == null) {
            return;
        }

        try {
            configuracionClient.guardarConfiguracion(semanas);
            mostrarAlerta("Éxito", "Configuración guardada correctamente", Alert.AlertType.INFORMATION);
            cargarConfiguracion();
        } catch (Exception e) {
            FormFieldHelper.showFieldError(txtSemanas, errSemanas, extractMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleEditarConfiguracion() {
        txtSemanas.setEditable(true);
        btnGuardarConfiguracion.setDisable(false);
        lblEstadoConfiguracion.setText("Modo edición habilitado");
        lblEstadoConfiguracion.getStyleClass().setAll("status-label", "status-warning");
    }

    private void actualizarModoFormulario(boolean editando) {
        btnAgregarDisponibilidad.setVisible(!editando);
        btnAgregarDisponibilidad.setManaged(!editando);
        btnGuardarCambiosDisponibilidad.setVisible(editando);
        btnGuardarCambiosDisponibilidad.setManaged(editando);
        btnCancelarEdicionDisponibilidad.setVisible(editando);
        btnCancelarEdicionDisponibilidad.setManaged(editando);

        lblEstadoEdicionDisponibilidad.setVisible(editando);
        lblEstadoEdicionDisponibilidad.setManaged(editando);

        if (!editando) {
            disponibilidadEnEdicionId = null;
        }

        actualizarBotonesSegunSeleccion();
    }

    private void aplicarFiltroTabla() {
        MedicoResponse medicoSeleccionado = cmbMedicos.getValue();

        if (medicoSeleccionado == null) {
            data.setAll(todasLasDisponibilidades);
            lblFiltroTabla.setText("Mostrando todas las disponibilidades registradas.");
            return;
        }

        Long medicoId = medicoSeleccionado.getPersonaId();
        var filtradas = todasLasDisponibilidades.stream()
                .filter(row -> row.getMedicoId().equals(medicoId))
                .toList();

        data.setAll(filtradas);
        lblFiltroTabla.setText("Mostrando disponibilidades de: " + medicoSeleccionado);
    }

    private void actualizarBotonesSegunSeleccion() {
        boolean haySeleccion = tablaDisponibilidad.getSelectionModel().getSelectedItem() != null;
        btnEditarDisponibilidad.setDisable(!haySeleccion || disponibilidadEnEdicionId != null);
        btnEliminarDisponibilidad.setDisable(!haySeleccion || disponibilidadEnEdicionId != null);
    }

    private void salirModoEdicion() {
        limpiarCamposDisponibilidad();
        actualizarModoFormulario(false);
        tablaDisponibilidad.getSelectionModel().clearSelection();
    }

    private void seleccionarMedico(Long medicoId) {
        cmbMedicos.getItems().stream()
                .filter(m -> m.getPersonaId().equals(medicoId))
                .findFirst()
                .ifPresent(m -> cmbMedicos.setValue(m));
    }

    private boolean validateDisponibilidadForm() {
        normalizeTimeFields();

        boolean valid = true;

        if (cmbMedicos.getValue() == null) {
            FormFieldHelper.showFieldError(cmbMedicos, errMedico, "Seleccione un médico");
            valid = false;
        }

        if (cmbDiaSemana.getValue() == null) {
            FormFieldHelper.showFieldError(cmbDiaSemana, errDia, "Seleccione el día de la semana");
            valid = false;
        }

        String horaInicio = TimeInputHelper.normalize(txtHoraInicio.getText());
        if (horaInicio == null) {
            FormFieldHelper.showFieldError(txtHoraInicio, errHoraInicio,
                    "Ingrese una hora válida en formato 24h (HH:mm). Ej: 08:00");
            valid = false;
        }

        String horaFin = TimeInputHelper.normalize(txtHoraFin.getText());
        if (horaFin == null) {
            FormFieldHelper.showFieldError(txtHoraFin, errHoraFin,
                    "Ingrese una hora válida en formato 24h (HH:mm). Ej: 12:00");
            valid = false;
        } else if (horaInicio != null && !TimeInputHelper.isEndAfterStart(horaInicio, horaFin)) {
            FormFieldHelper.showFieldError(txtHoraFin, errHoraFin,
                    "La hora de fin debe ser posterior a la hora de inicio");
            valid = false;
        }

        Integer intervalo = IntegerInputHelper.parsePositiveInteger(txtIntervalo.getText());
        if (intervalo == null) {
            FormFieldHelper.showFieldError(txtIntervalo, errIntervalo,
                    "Ingrese un número entero positivo de minutos. Ej: 30");
            valid = false;
        }

        return valid;
    }

    private Integer validateSemanas() {
        Integer semanas = IntegerInputHelper.parsePositiveInteger(txtSemanas.getText());

        if (semanas == null) {
            FormFieldHelper.showFieldError(txtSemanas, errSemanas,
                    "Ingrese un número entero entre 1 y 52");
            return null;
        }

        if (semanas > 52) {
            FormFieldHelper.showFieldError(txtSemanas, errSemanas,
                    "La cantidad máxima permitida es 52 semanas");
            return null;
        }

        return semanas;
    }

    private void normalizeTimeFields() {
        applyTimeNormalization(txtHoraInicio);
        applyTimeNormalization(txtHoraFin);
    }

    private void bindTimeNormalization(TextField field) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                applyTimeNormalization(field);
            }
        });
    }

    private void applyTimeNormalization(TextField field) {
        String normalized = TimeInputHelper.normalize(field.getText());
        if (normalized != null && !normalized.equals(field.getText())) {
            field.setText(normalized);
        }
    }

    private TextFormatter<String> timeFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (!TIME_INPUT.matcher(next).matches() || next.length() > 5) {
                return null;
            }
            return change;
        };
        return new TextFormatter<>(filter);
    }

    private TextFormatter<String> digitsFormatter(int maxLength) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (!DIGITS_ONLY.matcher(next).matches() || next.length() > maxLength) {
                return null;
            }
            return change;
        };
        return new TextFormatter<>(filter);
    }

    private void bindClearOnChange(Control control, Label errorLabel) {
        FormFieldHelper.bindClearOnChange(control, errorLabel);
        if (control instanceof TextInputControl textInput) {
            textInput.textProperty().addListener((obs, oldVal, newVal) -> clearFormError());
        } else if (control instanceof ComboBox<?>) {
            ((ComboBox<?>) control).valueProperty().addListener((obs, oldVal, newVal) -> clearFormError());
        }
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

    private String extractMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Ocurrió un error inesperado.";
        }

        String body = message;
        int idx = message.indexOf(": ");
        if (idx >= 0 && idx < message.length() - 2) {
            body = message.substring(idx + 2).trim();
        }

        int errorIndex = body.indexOf("\"error\"");
        if (errorIndex >= 0) {
            int start = body.indexOf(':', errorIndex) + 1;
            int firstQuote = body.indexOf('"', start);
            int secondQuote = body.indexOf('"', firstQuote + 1);
            if (firstQuote >= 0 && secondQuote > firstQuote) {
                return body.substring(firstQuote + 1, secondQuote);
            }
        }

        return body.trim();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void limpiarCamposDisponibilidad() {
        cmbDiaSemana.setValue(null);
        txtHoraInicio.clear();
        txtHoraFin.clear();
        txtIntervalo.clear();
        clearDisponibilidadErrors();
    }

    private void clearDisponibilidadErrors() {
        FormFieldHelper.clearFieldError(cmbMedicos, errMedico);
        FormFieldHelper.clearFieldError(cmbDiaSemana, errDia);
        FormFieldHelper.clearFieldError(txtHoraInicio, errHoraInicio);
        FormFieldHelper.clearFieldError(txtHoraFin, errHoraFin);
        FormFieldHelper.clearFieldError(txtIntervalo, errIntervalo);
        clearFormError();
    }

    private void cargarConfiguracion() {
        ConfiguracionResponse config = configuracionClient.obtenerConfiguracion();

        if (config == null) {
            contenedorDisponibilidad.setDisable(true);
            lblEstadoConfiguracion.setText("Debe configurar las semanas disponibles");
            lblEstadoConfiguracion.getStyleClass().setAll("status-label", "status-error");
            txtSemanas.setEditable(true);
            btnEditarConfiguracion.setDisable(true);
            return;
        }

        txtSemanas.setText(String.valueOf(config.getSemanasDisponibles()));
        txtSemanas.setEditable(false);
        lblEstadoConfiguracion.setText("Configuración registrada");
        lblEstadoConfiguracion.getStyleClass().setAll("status-label", "status-success");
        contenedorDisponibilidad.setDisable(false);
        btnGuardarConfiguracion.setDisable(true);
        btnEditarConfiguracion.setDisable(false);
    }

    private void cargarDisponibilidades() {
        try {
            todasLasDisponibilidades.clear();

            var disponibilidades = disponibilidadClient.obtenerDisponibilidades();

            for (DisponibilidadResponse disponibilidad : disponibilidades) {
                String nombreMedico = cmbMedicos.getItems()
                        .stream()
                        .filter(m -> m.getPersonaId().equals(disponibilidad.getMedicoId()))
                        .map(MedicoResponse::toString)
                        .findFirst()
                        .orElse("Médico ID: " + disponibilidad.getMedicoId());

                todasLasDisponibilidades.add(new DisponibilidadRow(
                        disponibilidad.getId(),
                        disponibilidad.getMedicoId(),
                        nombreMedico,
                        disponibilidad.getDiaSemana(),
                        disponibilidad.getHoraInicio(),
                        disponibilidad.getHoraFin(),
                        disponibilidad.getIntervaloMinutos()
                ));
            }

            aplicarFiltroTabla();

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar las disponibilidades", Alert.AlertType.ERROR);
        }
    }
}
