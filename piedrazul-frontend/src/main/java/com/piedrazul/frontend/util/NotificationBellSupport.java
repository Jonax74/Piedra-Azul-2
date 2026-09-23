package com.piedrazul.frontend.util;

import com.piedrazul.frontend.client.NotificationClient;
import com.piedrazul.frontend.dto.response.NotificacionResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Bounds;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Conecta la campanita del dashboard con notifications-service:
 * badge de no leídas, panel desplegable y polling periódico.
 */
public final class NotificationBellSupport {

    private static final Duration POLL_INTERVAL = Duration.seconds(45);
    private static final double PANEL_WIDTH = 320;
    private static final double PANEL_PADDING = 24;
    private static final double CELL_TEXT_INSET = 16;
    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "notification-bell-worker");
                t.setDaemon(true);
                return t;
            });

    private final NotificationClient notificationClient = new NotificationClient();
    private final Long personaId;
    private final StackPane bellContainer;
    private final Label badgeLabel;
    private final Popup popup = new Popup();
    private final ListView<NotificacionResponse> listView = new ListView<>();
    private final Label emptyLabel = new Label("No tienes notificaciones.");
    private Timeline pollTimeline;

    private NotificationBellSupport(Long personaId, StackPane bellContainer, Label badgeLabel) {
        this.personaId = personaId;
        this.bellContainer = bellContainer;
        this.badgeLabel = badgeLabel;
        buildPopup();
        wireBellClick();
        refreshBadgeAsync();
        startPolling();
    }

    public static NotificationBellSupport attach(StackPane bellContainer, Label badgeLabel) {
        Long personaId = SessionPersonaResolver.resolverPersonaId();
        if (personaId == null) {
            bellContainer.setDisable(true);
            return null;
        }
        return new NotificationBellSupport(personaId, bellContainer, badgeLabel);
    }

    public void dispose() {
        if (pollTimeline != null) {
            pollTimeline.stop();
        }
        popup.hide();
    }

    private void wireBellClick() {
        bellContainer.setOnMouseClicked(this::handleBellClick);
        bellContainer.setStyle("-fx-cursor: hand;");
    }

    private void handleBellClick(MouseEvent event) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        if (!popup.getContent().isEmpty() && popup.getContent().get(0) instanceof BorderPane panel) {
            if (bellContainer.getScene() != null) {
                panel.getStylesheets().setAll(bellContainer.getScene().getStylesheets());
            }
        }
        loadNotificationsAsync(true);
        Bounds bounds = bellContainer.localToScreen(bellContainer.getBoundsInLocal());
        double x = Math.max(8, bounds.getMaxX() - PANEL_WIDTH);
        popup.show(bellContainer.getScene().getWindow(), x, bounds.getMaxY() + 4);
    }

    private void buildPopup() {
        Label title = new Label("Notificaciones");
        title.getStyleClass().add("notification-panel-title");

        Button btnLeerTodas = new Button("Marcar todas como leídas");
        btnLeerTodas.getStyleClass().add("notification-panel-action");
        btnLeerTodas.setOnAction(e -> marcarTodasLeidasAsync());

        HBox header = new HBox(12, title);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);

        listView.setPrefWidth(PANEL_WIDTH);
        listView.setMaxWidth(PANEL_WIDTH);
        listView.setPrefHeight(280);
        listView.getStyleClass().add("notification-list");
        listView.widthProperty().addListener((obs, oldWidth, newWidth) -> listView.refresh());
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NotificacionResponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("notification-item-unread");
                    return;
                }
                double textWidth = resolveTextWidth();
                VBox content = new VBox(4);
                content.setMaxWidth(textWidth);
                content.setPrefWidth(textWidth);
                content.setFillWidth(true);

                Label lblTitle = createWrappingLabel(item.getTitulo(), "notification-item-title", textWidth);
                Label lblMessage = createWrappingLabel(item.getMensaje(), "notification-item-message", textWidth);
                String fecha = item.getFechaCreacion() == null
                        ? ""
                        : item.getFechaCreacion().format(FECHA_FORMATO);
                Label lblDate = createWrappingLabel(fecha, "notification-item-date", textWidth);

                content.getChildren().addAll(lblTitle, lblMessage, lblDate);
                setGraphic(content);
                setText(null);
                setPrefWidth(PANEL_WIDTH);
                setMaxWidth(PANEL_WIDTH);
                if (!item.isLeida()) {
                    if (!getStyleClass().contains("notification-item-unread")) {
                        getStyleClass().add("notification-item-unread");
                    }
                } else {
                    getStyleClass().remove("notification-item-unread");
                }
            }

            private double resolveTextWidth() {
                double listWidth = listView.getWidth() > 0 ? listView.getWidth() : PANEL_WIDTH;
                return Math.max(180, listWidth - CELL_TEXT_INSET);
            }
        });
        listView.setOnMouseClicked(e -> {
            NotificacionResponse selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isLeida()) {
                marcarLeidaAsync(selected);
            }
        });

        emptyLabel.getStyleClass().add("notification-empty-label");
        emptyLabel.setWrapText(true);
        emptyLabel.setMaxWidth(PANEL_WIDTH);
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        title.setWrapText(true);
        title.setMaxWidth(PANEL_WIDTH);

        VBox body = new VBox(8, listView, emptyLabel);
        body.setMaxWidth(PANEL_WIDTH);
        VBox footer = new VBox(btnLeerTodas);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setMaxWidth(PANEL_WIDTH);

        BorderPane panel = new BorderPane();
        panel.getStyleClass().add("notification-panel");
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(PANEL_WIDTH + PANEL_PADDING);
        panel.setMaxWidth(PANEL_WIDTH + PANEL_PADDING);
        panel.setTop(header);
        panel.setCenter(body);
        panel.setBottom(footer);

        popup.getContent().add(panel);
        popup.setAutoHide(true);
    }

    private void startPolling() {
        pollTimeline = new Timeline(new KeyFrame(POLL_INTERVAL, e -> refreshBadgeAsync()));
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();
    }

    private void refreshBadgeAsync() {
        EXECUTOR.submit(() -> {
            try {
                long count = notificationClient.contarNoLeidas(personaId);
                Platform.runLater(() -> updateBadge(count));
            } catch (Exception ignored) {
                // Silencioso en polling para no interrumpir el dashboard.
            }
        });
    }

    private void loadNotificationsAsync(boolean panelVisible) {
        EXECUTOR.submit(() -> {
            try {
                List<NotificacionResponse> items = notificationClient.listarMisNotificaciones(personaId, null);
                Platform.runLater(() -> renderList(items, panelVisible));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    emptyLabel.setText("No se pudieron cargar las notificaciones.");
                    emptyLabel.setVisible(true);
                    emptyLabel.setManaged(true);
                    listView.setVisible(false);
                    listView.setManaged(false);
                });
            }
        });
    }

    private void marcarLeidaAsync(NotificacionResponse item) {
        EXECUTOR.submit(() -> {
            try {
                notificationClient.marcarLeida(item.getId(), personaId);
                Platform.runLater(() -> {
                    item.setLeida(true);
                    listView.refresh();
                    refreshBadgeAsync();
                });
            } catch (Exception ignored) {
                // Sin bloquear la UI si falla el marcado.
            }
        });
    }

    private void marcarTodasLeidasAsync() {
        EXECUTOR.submit(() -> {
            try {
                notificationClient.marcarTodasLeidas(personaId);
                Platform.runLater(() -> {
                    listView.getItems().forEach(n -> n.setLeida(true));
                    listView.refresh();
                    updateBadge(0);
                });
            } catch (Exception ignored) {
                // Sin bloquear la UI si falla el marcado masivo.
            }
        });
    }

    private void renderList(List<NotificacionResponse> items, boolean panelVisible) {
        listView.getItems().setAll(items);
        boolean empty = items.isEmpty();
        emptyLabel.setVisible(empty && panelVisible);
        emptyLabel.setManaged(empty && panelVisible);
        listView.setVisible(!empty || !panelVisible);
        listView.setManaged(!empty || !panelVisible);
        if (empty && panelVisible) {
            emptyLabel.setText("No tienes notificaciones.");
        }
    }

    private static Label createWrappingLabel(String text, String styleClass, double maxWidth) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMaxWidth(maxWidth);
        label.setPrefWidth(maxWidth);
        label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return label;
    }

    private void updateBadge(long count) {
        if (count <= 0) {
            badgeLabel.setText("");
            badgeLabel.setVisible(false);
            badgeLabel.setManaged(false);
            return;
        }
        badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
        badgeLabel.setVisible(true);
        badgeLabel.setManaged(true);
    }
}
