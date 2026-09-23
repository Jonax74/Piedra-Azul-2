package com.piedrazul.frontend.util;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;

public final class JavaFxImageUtil {

    private JavaFxImageUtil() {
    }

    public static BufferedImage capturarNodo(Node node) {
        if (node == null) {
            return null;
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);

        WritableImage image = node.snapshot(params, null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        return bufferedImage;
    }
}
