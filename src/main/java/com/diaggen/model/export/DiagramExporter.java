package com.diaggen.model.export;

import com.diaggen.model.ClassDiagram;

import java.io.File;
import java.io.IOException;

public interface DiagramExporter {
    void export(ClassDiagram diagram, File file) throws IOException;
}


