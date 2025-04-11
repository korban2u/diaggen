package com.diaggen.model.export;

import com.diaggen.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class JavaCodeExporter implements DiagramExporter {

    @Override
    public void export(ClassDiagram diagram, File directory) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        for (DiagramClass diagramClass : diagram.getClasses()) {
            exportClass(diagramClass, directory.toPath());
        }
    }

    private void exportClass(DiagramClass diagramClass, Path baseDir) throws IOException {
        String packagePath = diagramClass.getPackageName().replace('.', '/');
        Path packageDir = baseDir.resolve(packagePath);
        Files.createDirectories(packageDir);

        Path classFile = packageDir.resolve(diagramClass.getName() + ".java");

        try (PrintWriter writer = new PrintWriter(new FileWriter(classFile.toFile()))) {
            if (!diagramClass.getPackageName().isEmpty()) {
                writer.println("package " + diagramClass.getPackageName() + ";");
                writer.println();
            }

            writer.println("public " + getClassTypeKeyword(diagramClass) + " " + diagramClass.getName() + " {");

            for (Member attribute : diagramClass.getAttributes()) {
                writer.println("    " + getVisibilityKeyword(attribute.getVisibility()) + " " +
                        attribute.getType() + " " + attribute.getName() + ";");
            }

            if (!diagramClass.getAttributes().isEmpty()) {
                writer.println();
            }

            for (Method method : diagramClass.getMethods()) {
                StringBuilder methodSig = new StringBuilder();
                methodSig.append("    ");
                methodSig.append(getVisibilityKeyword(method.getVisibility())).append(" ");

                if (method.isStatic()) {
                    methodSig.append("static ");
                }

                if (method.isAbstract()) {
                    methodSig.append("abstract ");
                }

                methodSig.append(method.getReturnType()).append(" ");
                methodSig.append(method.getName()).append("(");

                methodSig.append(method.getParameters().stream()
                        .map(param -> param.getType() + " " + param.getName())
                        .collect(Collectors.joining(", ")));

                methodSig.append(")");

                if (method.isAbstract()) {
                    methodSig.append(";");
                } else {
                    methodSig.append(" {");
                    writer.println(methodSig.toString());
                    writer.println("        // TODO: Implement method");
                    writer.println("    }");
                }

                if (method.isAbstract()) {
                    writer.println(methodSig.toString());
                }

                writer.println();
            }

            writer.println("}");
        }
    }

    private String getClassTypeKeyword(DiagramClass diagramClass) {
        switch (diagramClass.getClassType()) {
            case INTERFACE:
                return "interface";
            case ABSTRACT_CLASS:
                return "abstract class";
            case ENUM:
                return "enum";
            default:
                return "class";
        }
    }

    private String getVisibilityKeyword(Visibility visibility) {
        switch (visibility) {
            case PUBLIC:
                return "public";
            case PRIVATE:
                return "private";
            case PROTECTED:
                return "protected";
            default:
                return "";
        }
    }
}


