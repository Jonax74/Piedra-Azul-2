package com.piedrazul.frontend.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class ReportesPdfExporter {

    private static final DateTimeFormatter NOMBRE_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String[] MESES_CORTOS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font FONT_TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font FONT_TABLA_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_TABLA = FontFactory.getFont(FontFactory.HELVETICA, 9);

    private ReportesPdfExporter() {
    }

    public record ReporteExportData(
            int anio,
            String mesEtiqueta,
            boolean todosMeses,
            int indiceMes,
            String resumen,
            String tituloBarras,
            String tituloCircular,
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            BufferedImage imagenBarras,
            BufferedImage imagenCircular
    ) {
    }

    public static Path exportar(ReporteExportData data) throws IOException, DocumentException {
        Path destino = resolverRutaDescarga(data.anio(), data.mesEtiqueta());

        try (OutputStream out = Files.newOutputStream(destino)) {
            Document document = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("PIEDRAZUL - Reportes de Citas", FONT_TITULO));
            document.add(espacio(8));
            document.add(new Paragraph("Año: " + data.anio(), FONT_TEXTO));
            document.add(new Paragraph("Mes: " + data.mesEtiqueta(), FONT_TEXTO));
            document.add(new Paragraph(data.resumen(), FONT_TEXTO));
            document.add(new Paragraph(
                    "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    FONT_TEXTO
            ));
            document.add(espacio(12));

            agregarImagen(document, data.tituloBarras(), data.imagenBarras());
            document.add(espacio(10));
            agregarImagen(document, data.tituloCircular(), data.imagenCircular());
            document.add(espacio(12));

            document.add(new Paragraph("Detalle de citas", FONT_SUBTITULO));
            document.add(espacio(6));
            if (data.todosMeses()) {
                document.add(crearTablaAnual(data.nombresMedicos(), data.citasPorMedicoMes()));
            } else {
                document.add(crearTablaMensual(data.nombresMedicos(), data.citasPorMedicoMes(), data.indiceMes()));
            }

            document.close();
        }

        return destino;
    }

    private static void agregarImagen(Document document, String titulo, BufferedImage imagen)
            throws DocumentException, IOException {
        document.add(new Paragraph(titulo, FONT_SUBTITULO));
        document.add(espacio(4));

        if (imagen == null) {
            document.add(new Paragraph("Gráfico no disponible.", FONT_TEXTO));
            return;
        }

        Image pdfImage = Image.getInstance(imagen, null);
        float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
        pdfImage.scaleToFit(maxWidth, 260);
        pdfImage.setAlignment(Element.ALIGN_CENTER);
        document.add(pdfImage);
    }

    private static PdfPTable crearTablaAnual(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes
    ) throws DocumentException {
        PdfPTable tabla = new PdfPTable(14);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(4f);
        tabla.setWidths(new float[]{3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f});

        agregarCeldaEncabezado(tabla, "Médico");
        for (String mes : MESES_CORTOS) {
            agregarCeldaEncabezado(tabla, mes);
        }
        agregarCeldaEncabezado(tabla, "Total");

        for (Map.Entry<Long, String> entrada : nombresMedicos.entrySet()) {
            int[] conteo = citasPorMedicoMes.getOrDefault(entrada.getKey(), new int[12]);
            agregarCelda(tabla, entrada.getValue());
            int totalMedico = 0;
            for (int valor : conteo) {
                agregarCelda(tabla, String.valueOf(valor));
                totalMedico += valor;
            }
            agregarCelda(tabla, String.valueOf(totalMedico));
        }

        if (nombresMedicos.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase("Sin datos", FONT_TABLA));
            vacia.setColspan(14);
            vacia.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(vacia);
        }

        return tabla;
    }

    private static PdfPTable crearTablaMensual(
            Map<Long, String> nombresMedicos,
            Map<Long, int[]> citasPorMedicoMes,
            int indiceMes
    ) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(60);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.setSpacingBefore(4f);

        agregarCeldaEncabezado(tabla, "Médico");
        agregarCeldaEncabezado(tabla, "Citas");

        for (Map.Entry<Long, String> entrada : nombresMedicos.entrySet()) {
            int total = citasPorMedicoMes.getOrDefault(entrada.getKey(), new int[12])[indiceMes];
            agregarCelda(tabla, entrada.getValue());
            agregarCelda(tabla, String.valueOf(total));
        }

        if (nombresMedicos.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase("Sin datos", FONT_TABLA));
            vacia.setColspan(2);
            vacia.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(vacia);
        }

        return tabla;
    }

    private static void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FONT_TABLA_HEADER));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setBackgroundColor(new java.awt.Color(36, 192, 235));
        celda.setPadding(4f);
        tabla.addCell(celda);
    }

    private static void agregarCelda(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FONT_TABLA));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(3f);
        tabla.addCell(celda);
    }

    private static Paragraph espacio(float altura) {
        Paragraph parrafo = new Paragraph(" ");
        parrafo.setSpacingAfter(altura);
        return parrafo;
    }

    private static Path resolverRutaDescarga(int anio, String mesEtiqueta) throws IOException {
        Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloads);
        String mesSeguro = mesEtiqueta
                .toLowerCase()
                .replace(" ", "_")
                .replace("ó", "o");
        String nombre = "reportes_citas_" + anio + "_" + mesSeguro + "_"
                + LocalDateTime.now().format(NOMBRE_ARCHIVO) + ".pdf";
        return downloads.resolve(nombre);
    }
}
