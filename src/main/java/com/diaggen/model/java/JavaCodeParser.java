package com.diaggen.model.java;

import com.diaggen.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;

public class JavaCodeParser {

    private final Map<String, DiagramClass> classMap = new HashMap<>();
    private final List<RelationInfo> relationInfos = new ArrayList<>();

    public ClassDiagram parseProject(Path projectPath) {
        ClassDiagram diagram = new ClassDiagram(projectPath.getFileName().toString());
        SourceRoot sourceRoot = new SourceRoot(projectPath);

        try {
            sourceRoot.tryToParse();
            for (CompilationUnit cu : sourceRoot.getCompilationUnits()) {
                parseCompilationUnit(cu, diagram);
            }

            createRelations(diagram);

            return diagram;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Java project", e);
        }
    }

    public ClassDiagram parseFile(File file) {
        ClassDiagram diagram = new ClassDiagram(file.getName());

        try (FileInputStream in = new FileInputStream(file)) {
            CompilationUnit cu = new JavaParser().parse(in).getResult().orElseThrow();
            parseCompilationUnit(cu, diagram);
            createRelations(diagram);
            return diagram;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Java file", e);
        }
    }

    private void parseCompilationUnit(CompilationUnit cu, ClassDiagram diagram) {
        String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            ClassType classType;
            if (classDecl.isInterface()) {
                classType = ClassType.INTERFACE;
            } else if (classDecl.isAbstract()) {
                classType = ClassType.ABSTRACT_CLASS;
            } else {
                classType = ClassType.CLASS;
            }

            DiagramClass diagramClass = new DiagramClass(classDecl.getNameAsString(), packageName, classType);

            parseFields(classDecl, diagramClass);
            parseMethods(classDecl, diagramClass);
            collectRelationships(classDecl, diagramClass, packageName);

            diagram.addClass(diagramClass);
            classMap.put(getFullClassName(packageName, classDecl.getNameAsString()), diagramClass);
        });

        cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {
            DiagramClass diagramClass = new DiagramClass(enumDecl.getNameAsString(), packageName, ClassType.ENUM);

            parseFields(enumDecl, diagramClass);
            parseMethods(enumDecl, diagramClass);

            diagram.addClass(diagramClass);
            classMap.put(getFullClassName(packageName, enumDecl.getNameAsString()), diagramClass);
        });
    }

    private void parseFields(TypeDeclaration<?> typeDecl, DiagramClass diagramClass) {
        typeDecl.getFields().forEach(field -> {
            field.getVariables().forEach(variable -> {
                Visibility visibility = getVisibility(field);
                Member member = new Member(
                    variable.getNameAsString(),
                    field.getElementType().asString(),
                    visibility
                );
                diagramClass.addAttribute(member);
            });
        });
    }

    private void parseMethods(TypeDeclaration<?> typeDecl, DiagramClass diagramClass) {
        typeDecl.getMethods().forEach(method -> {
            List<Parameter> parameters = new ArrayList<>();
            method.getParameters().forEach(param -> {
                parameters.add(new Parameter(
                    param.getNameAsString(),
                    param.getType().asString()
                ));
            });

            Visibility visibility = getVisibility(method);
            boolean isAbstract = method.isAbstract();
            boolean isStatic = method.isStatic();

            Method diagramMethod = new Method(
                method.getNameAsString(),
                method.getType().asString(),
                parameters,
                visibility,
                isAbstract,
                isStatic
            );

            diagramClass.addMethod(diagramMethod);
        });
    }

    private Visibility getVisibility(BodyDeclaration<?> declaration) {
        if (declaration.isPublic()) {
            return Visibility.PUBLIC;
        } else if (declaration.isPrivate()) {
            return Visibility.PRIVATE;
        } else if (declaration.isProtected()) {
            return Visibility.PROTECTED;
        } else {
            return Visibility.PACKAGE;
        }
    }

    private void collectRelationships(ClassOrInterfaceDeclaration classDecl, DiagramClass diagramClass, String packageName) {
        classDecl.getExtendedTypes().forEach(extendedType -> {
            relationInfos.add(new RelationInfo(
                diagramClass,
                getFullClassName(packageName, extendedType.getNameAsString()),
                RelationType.INHERITANCE,
                "", "", ""
            ));
        });

        classDecl.getImplementedTypes().forEach(implementedType -> {
            relationInfos.add(new RelationInfo(
                diagramClass,
                getFullClassName(packageName, implementedType.getNameAsString()),
                RelationType.IMPLEMENTATION,
                "", "", ""
            ));
        });

        classDecl.getFields().forEach(field -> {
            String fieldType = field.getElementType().asString();
            if (!isPrimitive(fieldType)) {
                RelationType relationType = field.getModifiers().contains(Modifier.nodeFromModifier(Modifier.Keyword.FINAL))
                    ? RelationType.COMPOSITION
                    : RelationType.AGGREGATION;

                String multiplicity = isCollection(fieldType) ? "0..*" : "0..1";

                relationInfos.add(new RelationInfo(
                    diagramClass,
                    getFullClassName(packageName, stripGenerics(fieldType)),
                    relationType,
                    "1", multiplicity, field.getVariable(0).getNameAsString()
                ));
            }
        });
    }

    private void createRelations(ClassDiagram diagram) {
        relationInfos.forEach(info -> {
            DiagramClass targetClass = classMap.get(info.targetClassName);
            if (targetClass != null) {
                DiagramRelation relation = new DiagramRelation(
                    info.sourceClass,
                    targetClass,
                    info.relationType,
                    info.sourceMultiplicity,
                    info.targetMultiplicity,
                    info.label
                );
                diagram.addRelation(relation);
            }
        });
    }

    private String getFullClassName(String packageName, String className) {
        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    private boolean isPrimitive(String typeName) {
        return typeName.equals("int") ||
               typeName.equals("long") ||
               typeName.equals("double") ||
               typeName.equals("float") ||
               typeName.equals("boolean") ||
               typeName.equals("char") ||
               typeName.equals("byte") ||
               typeName.equals("short") ||
               typeName.equals("String") ||
               typeName.startsWith("java.lang.");
    }

    private boolean isCollection(String typeName) {
        return typeName.contains("List") ||
               typeName.contains("Set") ||
               typeName.contains("Map") ||
               typeName.contains("Collection") ||
               typeName.contains("[]");
    }

    private String stripGenerics(String typeName) {
        int idx = typeName.indexOf('<');
        return idx > 0 ? typeName.substring(0, idx) : typeName;
    }

    private static class RelationInfo {
        DiagramClass sourceClass;
        String targetClassName;
        RelationType relationType;
        String sourceMultiplicity;
        String targetMultiplicity;
        String label;

        public RelationInfo(DiagramClass sourceClass, String targetClassName,
                            RelationType relationType, String sourceMultiplicity,
                            String targetMultiplicity, String label) {
            this.sourceClass = sourceClass;
            this.targetClassName = targetClassName;
            this.relationType = relationType;
            this.sourceMultiplicity = sourceMultiplicity;
            this.targetMultiplicity = targetMultiplicity;
            this.label = label;
        }
    }
}


