package com.diaggen.model.export;

import com.diaggen.model.ClassDiagram;

import com.diaggen.view.diagram.DiagramCanvas;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class PNGExporter implements DiagramExporter {
    private final DiagramCanvas diagramCanvas;

    public PNGExporter(DiagramCanvas diagramCanvas) {
        this.diagramCanvas = diagramCanvas;
    }

    @Override
    public void export(ClassDiagram diagram, File file) throws IOException {
        WritableImage image = diagramCanvas.snapshot(new SnapshotParameters(), null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
    }
}


