package com.diaggen.service;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.export.*;
import com.diaggen.view.diagram.DiagramCanvas;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExportService {

    private final Map<String, DiagramExporter> exporters = new HashMap<>();
    private final DiagramCanvas diagramCanvas;

        public ExportService(DiagramCanvas diagramCanvas) {
        this.diagramCanvas = diagramCanvas;
        registerExporters();
    }

        private void registerExporters() {
        exporters.put("png", new PNGExporter(diagramCanvas));
        exporters.put("svg", new SVGExporter());
        exporters.put("puml", new PlantUMLExporter());
        exporters.put("java", new JavaCodeExporter());
    }

        public void exportDiagram(ClassDiagram diagram, String format, File file) throws IOException {
        DiagramExporter exporter = exporters.get(format.toLowerCase());

        if (exporter == null) {
            throw new IllegalArgumentException("Format d'exportation non pris en charge: " + format);
        }

        exporter.export(diagram, file);
    }

        public boolean isFormatSupported(String format) {
        return exporters.containsKey(format.toLowerCase());
    }

        public String[] getSupportedFormats() {
        return exporters.keySet().toArray(new String[0]);
    }
}