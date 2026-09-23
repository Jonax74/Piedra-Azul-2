package com.piedrazul.frontend.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;

public final class PiedrazulDialog {

    private PiedrazulDialog() {
    }

    public static void showInfo(Node owner, String titulo, String mensaje) {
        show(owner, titulo, mensaje, "info");
    }

    public static void showWarning(Node owner, String titulo, String mensaje) {
        show(owner, titulo, mensaje, "warning");
    }

    private static void show(Node owner, String titulo, String mensaje, String tipo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }
        dialog.setTitle("PIEDRAZUL");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("piedrazul-dialog");
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        PiedrazulDialog.class.getResource("/view/css/login.css")
                ).toExternalForm()
        );

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("piedrazul-dialog-title");
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(420);

        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("piedrazul-dialog-message");
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(420);

        Label lblIcono = new Label("warning".equals(tipo) ? "!" : "i");
        lblIcono.getStyleClass().add("warning".equals(tipo)
                ? "piedrazul-dialog-icon-warning"
                : "piedrazul-dialog-icon-info");

        VBox contenido = new VBox(14, lblIcono, lblTitulo, lblMensaje);
        contenido.setAlignment(Pos.CENTER);
        contenido.setPadding(new Insets(8, 12, 4, 12));
        contenido.getStyleClass().add("piedrazul-dialog-content");
        dialog.getDialogPane().setContent(contenido);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
            okButton.setText("Entendido");
        }

        dialog.showAndWait();
    }
}
