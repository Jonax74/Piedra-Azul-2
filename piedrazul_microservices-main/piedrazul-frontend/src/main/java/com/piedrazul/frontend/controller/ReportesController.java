package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.client.CitaClient;
import com.piedrazul.frontend.dto.response.CitaResponse;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.JavaFxImageUtil;
import com.piedrazul.frontend.util.ReportesExportMessages;
import com.piedrazul.frontend.util.ReportesPdfExporter;
import com.piedrazul.frontend.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.awt.image.BufferedImage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ReportesController {

    private static final String[] MESES_CORTOS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    @FXML private ComboBox<Integer> cmbAnio;
    @FXML private ComboBox<MesFiltro> cmbMes;
    @FXML private StackPane barChartContainer;
    @FXML private StackPane pieChartContainer;
    @FXML private Label lblBarChartTitulo;
    @FXML private Label lblPieChartTitulo;
    @FXML private Label lblResumen;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnVolver;

    private final CitaClient citaClient = new CitaClient();

    private Map<Long, String> nombresMedicosActual = Map.of();
    private Map<Long, int[]> citasPorMedicoMesActual = Map.of();
    private int totalCitasActual = 0;

    @FXML
    public void initialize() {
        configurarEstilosVentana();
        configurarFiltros();
        cargarReportes();
    }

    private void configurarEstilosVentana() {
        barChartContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
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
    }

    private void configurarFiltros() {
        int anioActual = Year.now().getValue();
        List<Integer> anios = IntStream.rangeClosed(anioActual - 4, anioActual + 1)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();
        cmbAnio.setItems(FXCollections.observableArrayList(anios));
        cmbAnio.setValue(anioActual);

        List<MesFiltro> meses = new ArrayList<>();
        meses.add(new MesFiltro(0, "Todos los meses"));
        for (int i = 1; i <= 12; i++) {
            meses.add(new MesFiltro(i, MESES_CORTOS[i - 1]));
        }
        cmbMes.setItems(FXCollections.observableArrayList(meses));
        cmbMes.setValue(meses.get(0));

        cmbAnio.valueProperty().addListener((obs, anterior, nuevo) -> cargarReportes());
        cmbMes.valueProperty().addListener((obs, anterior, nuevo) -> cargarReportes());
    }

    private void cargarReportes() {
        Integer anio = cmbAnio.getValue();
        MesFiltro mesFiltro = cmbMes.getValue();
        if (anio == null || mesFiltro == null) {
            return;
        }

        try {
            LocalDate fechaInicio = LocalDate.of(anio, 1, 1);
            LocalDate fechaFin = LocalDate.of(anio, 12, 31);
            List<CitaResponse> citas = citaClient.listarHistorial(null, fechaInicio, fechaFin);

            Map<Long, String> nombresMedicos = new LinkedHashMap<>();
            Map<Long, int[]> citasPorMedicoMes = new HashMap<>();

            for (CitaResponse cita : citas) {
                LocalDateTime fechaHora = parseFechaHora(cita);
                if (fechaHora == null || fechaHora.getYear() != anio) {
                    continue;
                }

                Long medicoId = cita.getMedicoId();
                if (medicoId == null) {
                    continue;
                }

                nombresMedicos.putIfAbsent(medicoId, resolverNombreMedico(cita));
                int[] conteoMensual = citasPorMedicoMes.computeIfAbsent(medicoId, id -> new int[12]);
                conteoMensual[fechaHora.getMonthValue() - 1]++;
            }

            nombresMedicosActual = nombresMedicos;
            citasPorMedicoMesActual = citasPorMedicoMes;
            totalCitasActual = calcularTotal(nombresMedicos, citasPorMedicoMes, mesFiltro);

            actualizarGraficos(nombresMedicos, citasPorMedicoMes, anio, mesFiltro);
            actualizarResumen(nombresMedicos, citasPorMedicoMes, anio, mesFiltro);

        } catch (Exception e) {
            nombresMedicosActual = Map.of();
            citasPorMedicoMesActual = Map.of();
            totalCitasActual = 0;
            limpiarGraficos();
            lblResumen.setText("No se pudieron cargar los reportes.");
            mostrarError("No se pudieron cargar los reportes: " + e.getMessage());
        }
    }

    private void actualizarGraficos(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            int anio,
            MesFiltro mesFiltro
    ) {
        if (mesFiltro.esTodos()) {
            lblBarChartTitulo.setText("Citas por médico y mes — " + anio);
            lblPieChartTitulo.setText("Distribución anual por médico — " + anio);
            barChartContainer.getChildren().setAll(crearGraficoBarrasAnual(nombresMedicos, citasPorMedicoMes));
            pieChartContainer.getChildren().setAll(crearGraficoCircularAnual(nombresMedicos, citasPorMedicoMes));
        } else {
            int indiceMes = mesFiltro.valor() - 1;
            String nombreMes = MESES_CORTOS[indiceMes];
            lblBarChartTitulo.setText("Citas por médico — " + nombreMes + " " + anio);
            lblPieChartTitulo.setText("Distribución por médico — " + nombreMes + " " + anio);
            barChartContainer.getChildren().setAll(
                    crearGraficoBarrasMensual(nombresMedicos, citasPorMedicoMes, indiceMes)
            );
            pieChartContainer.getChildren().setAll(
                    crearGraficoCircularMensual(nombresMedicos, citasPorMedicoMes, indiceMes)
            );
        }
    }

    private BarChart<String, Number> crearGraficoBarrasAnual(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes
    ) {
        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Mes");
        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Cantidad de citas");
        ejeY.setMinorTickVisible(false);

        BarChart<String, Number> grafico = new BarChart<>(ejeX, ejeY);
        grafico.setTitle(null);
        grafico.setLegendVisible(true);
        grafico.setCategoryGap(12);
        grafico.setBarGap(3);
        grafico.getStyleClass().add("reportes-bar-chart");

        for (Map.Entry<Long, String> entrada : nombresMedicos.entrySet()) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(entrada.getValue());
            int[] conteo = citasPorMedicoMes.getOrDefault(entrada.getKey(), new int[12]);
            for (int i = 0; i < 12; i++) {
                serie.getData().add(new XYChart.Data<>(MESES_CORTOS[i], conteo[i]));
            }
            grafico.getData().add(serie);
        }

        if (grafico.getData().isEmpty()) {
            XYChart.Series<String, Number> serieVacia = new XYChart.Series<>();
            serieVacia.setName("Sin datos");
            for (String mes : MESES_CORTOS) {
                serieVacia.getData().add(new XYChart.Data<>(mes, 0));
            }
            grafico.getData().add(serieVacia);
            grafico.setLegendVisible(false);
        }

        return grafico;
    }

    private BarChart<String, Number> crearGraficoBarrasMensual(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            int indiceMes
    ) {
        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Médico");
        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Cantidad de citas");
        ejeY.setMinorTickVisible(false);

        BarChart<String, Number> grafico = new BarChart<>(ejeX, ejeY);
        grafico.setTitle(null);
        grafico.setLegendVisible(false);
        grafico.setCategoryGap(18);
        grafico.getStyleClass().add("reportes-bar-chart");

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Citas");

        if (nombresMedicos.isEmpty()) {
            serie.getData().add(new XYChart.Data<>("Sin datos", 0));
        } else {
            for (Map.Entry<Long, String> entrada : nombresMedicos.entrySet()) {
                int[] conteo = citasPorMedicoMes.getOrDefault(entrada.getKey(), new int[12]);
                serie.getData().add(new XYChart.Data<>(entrada.getValue(), conteo[indiceMes]));
            }
        }

        grafico.getData().add(serie);
        return grafico;
    }

    private PieChart crearGraficoCircularAnual(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes
    ) {
        PieChart grafico = new PieChart();
        grafico.setTitle(null);
        grafico.setLabelsVisible(true);
        grafico.setLegendVisible(true);
        grafico.getStyleClass().add("reportes-pie-chart");

        if (nombresMedicos.isEmpty()) {
            grafico.getData().add(new PieChart.Data("Sin citas registradas", 1));
            return grafico;
        }

        for (Map.Entry<Long, String> entrada : nombresMedicos.entrySet()) {
            int total = sumarConteo(citasPorMedicoMes.getOrDefault(entrada.getKey(), new int[12]));
            if (total > 0) {
                grafico.getData().add(new PieChart.Data(entrada.getValue() + " (" + total + ")", total));
            }
        }

        if (grafico.getData().isEmpty()) {
            grafico.getData().add(new PieChart.Data("Sin citas registradas", 1));
        }

        return grafico;
    }

    private PieChart crearGraficoCircularMensual(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            int indiceMes
    ) {
        PieChart grafico = new PieChart();
        grafico.setTitle(null);
        grafico.setLabelsVisible(true);
        grafico.setLegendVisible(true);
        grafico.getStyleClass().add("reportes-pie-chart");

        if (nombresMedicos.isEmpty()) {
            grafico.getData().add(new PieChart.Data("Sin citas registradas", 1));
            return grafico;
        }

        for (Map.Entry<Long, String> entrada : nombresMedicos.entrySet()) {
            int total = citasPorMedicoMes.getOrDefault(entrada.getKey(), new int[12])[indiceMes];
            if (total > 0) {
                grafico.getData().add(new PieChart.Data(entrada.getValue() + " (" + total + ")", total));
            }
        }

        if (grafico.getData().isEmpty()) {
            grafico.getData().add(new PieChart.Data("Sin citas en este mes", 1));
        }

        return grafico;
    }

    private void actualizarResumen(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            int anio,
            MesFiltro mesFiltro
    ) {
        int total;
        if (mesFiltro.esTodos()) {
            total = citasPorMedicoMes.values().stream().mapToInt(this::sumarConteo).sum();
            lblResumen.setText(
                    "Total de citas en " + anio + ": " + total
                            + " | Médicos con actividad: " + nombresMedicos.size()
            );
        } else {
            int indiceMes = mesFiltro.valor() - 1;
            total = citasPorMedicoMes.values().stream()
                    .mapToInt(conteo -> conteo[indiceMes])
                    .sum();
            lblResumen.setText(
                    "Total de citas en " + MESES_CORTOS[indiceMes] + " " + anio + ": " + total
                            + " | Médicos con actividad: "
                            + nombresMedicos.entrySet().stream()
                            .filter(e -> citasPorMedicoMes.getOrDefault(e.getKey(), new int[12])[indiceMes] > 0)
                            .count()
            );
        }
    }

    private int sumarConteo(int[] conteo) {
        int total = 0;
        for (int valor : conteo) {
            total += valor;
        }
        return total;
    }

    private int calcularTotal(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            MesFiltro mesFiltro
    ) {
        if (mesFiltro.esTodos()) {
            return citasPorMedicoMes.values().stream().mapToInt(this::sumarConteo).sum();
        }
        int indiceMes = mesFiltro.valor() - 1;
        return citasPorMedicoMes.values().stream()
                .mapToInt(conteo -> conteo[indiceMes])
                .sum();
    }

    @FXML
    private void exportarPdf() {
        Integer anio = cmbAnio.getValue();
        MesFiltro mesFiltro = cmbMes.getValue();
        if (anio == null || mesFiltro == null) {
            return;
        }

        if (totalCitasActual <= 0) {
            mostrarAdvertencia(ReportesExportMessages.SIN_DATOS);
            return;
        }

        try {
            ReportesPdfExporter.ReporteExportData datos = new ReportesPdfExporter.ReporteExportData(
                    anio,
                    mesFiltro.etiqueta(),
                    mesFiltro.esTodos(),
                    mesFiltro.esTodos() ? -1 : mesFiltro.valor() - 1,
                    lblResumen.getText(),
                    lblBarChartTitulo.getText(),
                    lblPieChartTitulo.getText(),
                    nombresMedicosActual,
                    citasPorMedicoMesActual,
                    capturarGrafico(barChartContainer),
                    capturarGrafico(pieChartContainer)
            );

            ReportesPdfExporter.exportar(datos);
            mostrarInformacion(ReportesExportMessages.EXITO);
        } catch (Exception e) {
            mostrarError(ReportesExportMessages.ERROR + " " + e.getMessage());
        }
    }

    private BufferedImage capturarGrafico(StackPane contenedor) {
        if (contenedor.getChildren().isEmpty()) {
            return null;
        }
        Node grafico = contenedor.getChildren().get(0);
        return JavaFxImageUtil.capturarNodo(grafico);
    }

    private void limpiarGraficos() {
        barChartContainer.getChildren().clear();
        pieChartContainer.getChildren().clear();
    }

    private LocalDateTime parseFechaHora(CitaResponse cita) {
        if (cita.getFechaHora() == null || cita.getFechaHora().isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(cita.getFechaHora(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(cita.getFechaHora(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    private String resolverNombreMedico(CitaResponse cita) {
        if (cita.getMedicoNombre() != null && !cita.getMedicoNombre().isBlank()) {
            return cita.getMedicoNombre();
        }
        return "Médico #" + cita.getMedicoId();
    }

    private void mostrarInformacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reportes");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Reportes");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Reportes");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void volver() {
        if (SessionManager.hasRole("ADMINISTRADOR")) {
            SceneManager.showDashboard(
                    "/view/dashboard/administrador-dashboard.fxml",
                    btnVolver,
                    "PIEDRAZUL - Menu principal"
            );
        } else {
            SceneManager.showDashboard(
                    "/view/dashboard/agendador-dashboard.fxml",
                    btnVolver,
                    "PIEDRAZUL - Menu principal"
            );
        }
    }

    public record MesFiltro(int valor, String etiqueta) {
        public boolean esTodos() {
            return valor == 0;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
