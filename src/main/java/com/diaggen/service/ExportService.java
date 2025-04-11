package com.diaggen.service;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.export.*;
import com.diaggen.view.diagram.DiagramCanvas;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service d'exportation de diagrammes utilisant le pattern Stratégie
 */
public class ExportService {

    private final Map<String, DiagramExporter> exporters = new HashMap<>();
    private final DiagramCanvas diagramCanvas;

    /**
     * Constructeur du service d'exportation
     * @param diagramCanvas Le canvas de diagramme pour l'exportation PNG
     */
    public ExportService(DiagramCanvas diagramCanvas) {
        this.diagramCanvas = diagramCanvas;
        registerExporters();
    }

    /**
     * Enregistre les exportateurs disponibles
     */
    private void registerExporters() {
        exporters.put("png", new PNGExporter(diagramCanvas));
        exporters.put("svg", new SVGExporter());
        exporters.put("puml", new PlantUMLExporter());
        exporters.put("java", new JavaCodeExporter());
    }

    /**
     * Exporte un diagramme dans le format spécifié
     * @param diagram Le diagramme à exporter
     * @param format Le format d'exportation (png, svg, puml, java)
     * @param file Le fichier ou répertoire de destination
     * @throws IOException En cas d'erreur d'exportation
     * @throws IllegalArgumentException Si le format n'est pas pris en charge
     */
    public void exportDiagram(ClassDiagram diagram, String format, File file) throws IOException {
        DiagramExporter exporter = exporters.get(format.toLowerCase());

        if (exporter == null) {
            throw new IllegalArgumentException("Format d'exportation non pris en charge: " + format);
        }

        exporter.export(diagram, file);
    }

    /**
     * Vérifie si un format d'exportation est pris en charge
     * @param format Le format à vérifier
     * @return true si le format est pris en charge, false sinon
     */
    public boolean isFormatSupported(String format) {
        return exporters.containsKey(format.toLowerCase());
    }

    /**
     * Obtient la liste des formats d'exportation pris en charge
     * @return Un tableau des formats pris en charge
     */
    public String[] getSupportedFormats() {
        return exporters.keySet().toArray(new String[0]);
    }
}