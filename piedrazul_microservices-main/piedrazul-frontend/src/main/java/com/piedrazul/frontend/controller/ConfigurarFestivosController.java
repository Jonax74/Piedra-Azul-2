package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.ConfiguracionClient;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigurarFestivosController {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private DatePicker dpNuevaFecha;
    @FXML private TableView<FestivoRow> tablaFestivos;
    @FXML private TableColumn<FestivoRow, String> colFecha;
    @FXML private TableColumn<FestivoRow, String> colDiaSemana;
    @FXML private Button btnAgregar;
    @FXML private Button btnEliminar;
    @FXML private Label lblEstado;
    @FXML private Label lblFormError;

    private final ConfiguracionClient configuracionClient = new ConfiguracionClient();
    private final ObservableList<FestivoRow> festivos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !SessionManager.hasRole("ADMINISTRADOR")) {
            SessionManager.clear();
            SceneManager.showLogin("/view/auth_register/loginView.fxml", lblEstado);
            return;
        }

        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fechaFormateada()));
        colDiaSemana.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().diaSemana()));

        tablaFestivos.setItems(festivos);
        tablaFestivos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                btnEliminar.setDisable(newVal == null)
        );

        dpNuevaFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                boolean yaRegistrada = !empty && festivos.stream()
                        .anyMatch(f -> f.getFecha().equals(date));
                setDisable(empty || yaRegistrada);
                if (yaRegistrada) {
                    setStyle("-fx-background-color: #fde8e8;");
                }
            }
        });

        cargarFestivos();
    }

    @FXML
    private void handleAgregarFestivo() {
        ocultarError();

        LocalDate fecha = dpNuevaFecha.getValue();
        if (fecha == null) {
            mostrarError("Seleccione una fecha para marcar como festivo.");
            return;
        }

        if (festivos.stream().anyMatch(f -> f.getFecha().equals(fecha))) {
            mostrarError("Esa fecha ya está registrada como festivo.");
            return;
        }

        try {
            List<LocalDate> actualizados = new ArrayList<>();
            festivos.forEach(f -> actualizados.add(f.getFecha()));
            actualizados.add(fecha);

            List<LocalDate> guardados = configuracionClient.guardarFestivos(actualizados);
            actualizarTabla(guardados);
            dpNuevaFecha.setValue(null);
            mostrarEstado("Festivo agregado correctamente.", "status-success");
        } catch (Exception e) {
            mostrarError("No se pudo agregar el festivo: " + e.getMessage());
        }
    }

    @FXML
    private void handleEliminarFestivo() {
        ocultarError();

        FestivoRow seleccionado = tablaFestivos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarError("Seleccione un festivo de la tabla para eliminarlo.");
            return;
        }

        try {
            List<LocalDate> actualizados = festivos.stream()
                    .map(FestivoRow::getFecha)
                    .filter(f -> !f.equals(seleccionado.getFecha()))
                    .toList();

            List<LocalDate> guardados = configuracionClient.guardarFestivos(actualizados);
            actualizarTabla(guardados);
            mostrarEstado("Festivo eliminado correctamente.", "status-success");
        } catch (Exception e) {
            mostrarError("No se pudo eliminar el festivo: " + e.getMessage());
        }
    }

    @FXML
    private void handleVolver() {
        SceneManager.switchScene(
                "/view/dashboard/administrador-dashboard.fxml",
                lblEstado,
                "PIEDRAZUL - Menu principal"
        );
    }

    private void cargarFestivos() {
        try {
            actualizarTabla(configuracionClient.obtenerFestivos());
            if (festivos.isEmpty()) {
                mostrarEstado("No hay festivos configurados. Agregue fechas cuando lo necesite.", "status-warning");
            } else {
                mostrarEstado("Festivos cargados correctamente.", "status-success");
            }
        } catch (Exception e) {
            mostrarEstado("No se pudieron cargar los festivos: " + e.getMessage(), "status-error");
        }
    }

    private void actualizarTabla(List<LocalDate> fechas) {
        festivos.setAll(fechas.stream().map(FestivoRow::new).toList());
        dpNuevaFecha.setValue(null);
    }

    private void mostrarEstado(String mensaje, String estilo) {
        lblEstado.setText(mensaje);
        lblEstado.getStyleClass().setAll("status-label", estilo);
    }

    private void mostrarError(String mensaje) {
        lblFormError.setText(mensaje);
        lblFormError.setVisible(true);
        lblFormError.setManaged(true);
    }

    private void ocultarError() {
        lblFormError.setVisible(false);
        lblFormError.setManaged(false);
    }

    public static class FestivoRow {
        private final LocalDate fecha;

        public FestivoRow(LocalDate fecha) {
            this.fecha = fecha;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String fechaFormateada() {
            return fecha.format(FORMATO_FECHA);
        }

        public String diaSemana() {
            return fecha.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        }
    }
}
