package com.diaggen.model.java;

import com.diaggen.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaCodeParser {

    private static final Logger LOGGER = Logger.getLogger(JavaCodeParser.class.getName());
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

        cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {
            if (enumDecl.getParentNode().isPresent() &&
                    enumDecl.getParentNode().get() instanceof TypeDeclaration) {

            } else {
                DiagramClass diagramClass = new DiagramClass(enumDecl.getNameAsString(), packageName, ClassType.ENUM);
                parseFields(enumDecl, diagramClass);
                parseMethods(enumDecl, diagramClass);

                diagram.addClass(diagramClass);
                classMap.put(getFullClassName(packageName, enumDecl.getNameAsString()), diagramClass);
            }
        });

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {

            if (classDecl.getParentNode().isPresent() &&
                    classDecl.getParentNode().get() instanceof TypeDeclaration) {

            } else {
                processClassOrInterface(classDecl, packageName, diagram);
            }
        });
    }

    private void processClassOrInterface(ClassOrInterfaceDeclaration classDecl, String packageName, ClassDiagram diagram) {
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

        processNestedTypes(classDecl, diagramClass, packageName, diagram);

        diagram.addClass(diagramClass);
        classMap.put(getFullClassName(packageName, classDecl.getNameAsString()), diagramClass);
    }

    private void processNestedTypes(TypeDeclaration<?> parentDecl, DiagramClass parentClass, String packageName, ClassDiagram diagram) {

        parentDecl.findAll(ClassOrInterfaceDeclaration.class, classDecl ->
                        classDecl.getParentNode().isPresent() &&
                                classDecl.getParentNode().get() == parentDecl)
                .forEach(nestedClass -> {

                    ClassType nestedClassType;
                    if (nestedClass.isInterface()) {
                        nestedClassType = ClassType.INTERFACE;
                    } else if (nestedClass.isAbstract()) {
                        nestedClassType = ClassType.ABSTRACT_CLASS;
                    } else {
                        nestedClassType = ClassType.CLASS;
                    }

                    String nestedClassName = nestedClass.getNameAsString();
                    String fullNestedClassName = parentClass.getName() + "." + nestedClassName;

                    DiagramClass nestedDiagramClass = new DiagramClass(fullNestedClassName, packageName, nestedClassType);

                    parseFields(nestedClass, nestedDiagramClass);
                    parseMethods(nestedClass, nestedDiagramClass);
                    collectRelationships(nestedClass, nestedDiagramClass, packageName);

                    diagram.addClass(nestedDiagramClass);
                    classMap.put(getFullClassName(packageName, fullNestedClassName), nestedDiagramClass);

                    relationInfos.add(new RelationInfo(
                            parentClass,
                            getFullClassName(packageName, fullNestedClassName),
                            RelationType.COMPOSITION,
                            "1", "1", "inner class"
                    ));

                    processNestedTypes(nestedClass, nestedDiagramClass, packageName, diagram);
                });

        parentDecl.findAll(EnumDeclaration.class, enumDecl ->
                        enumDecl.getParentNode().isPresent() &&
                                enumDecl.getParentNode().get() == parentDecl)
                .forEach(nestedEnum -> {

                    String nestedEnumName = nestedEnum.getNameAsString();
                    String fullNestedEnumName = parentClass.getName() + "." + nestedEnumName;

                    DiagramClass nestedEnumClass = new DiagramClass(fullNestedEnumName, packageName, ClassType.ENUM);

                    parseFields(nestedEnum, nestedEnumClass);
                    parseMethods(nestedEnum, nestedEnumClass);

                    diagram.addClass(nestedEnumClass);
                    classMap.put(getFullClassName(packageName, fullNestedEnumName), nestedEnumClass);

                    relationInfos.add(new RelationInfo(
                            parentClass,
                            getFullClassName(packageName, fullNestedEnumName),
                            RelationType.ASSOCIATION,
                            "1", "1", "enum type"
                    ));
                });
    }

    private void parseFields(TypeDeclaration<?> typeDecl, DiagramClass diagramClass) {
        typeDecl.getFields().forEach(field -> {
            field.getVariables().forEach(variable -> {
                Visibility visibility = determineVisibility(field);
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

            Visibility visibility = determineVisibility(method);
            boolean isAbstract = method.isAbstract();
            boolean isStatic = method.isStatic();


            com.diaggen.model.Method diagramMethod = new com.diaggen.model.Method(
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

    private Visibility determineVisibility(Object declaration) {
        try {
            java.lang.reflect.Method isPublicMethod = declaration.getClass().getMethod("isPublic");
            java.lang.reflect.Method isPrivateMethod = declaration.getClass().getMethod("isPrivate");
            java.lang.reflect.Method isProtectedMethod = declaration.getClass().getMethod("isProtected");

            Boolean isPublic = (Boolean) isPublicMethod.invoke(declaration);
            Boolean isPrivate = (Boolean) isPrivateMethod.invoke(declaration);
            Boolean isProtected = (Boolean) isProtectedMethod.invoke(declaration);

            if (isPublic) {
                return Visibility.PUBLIC;
            } else if (isPrivate) {
                return Visibility.PRIVATE;
            } else if (isProtected) {
                return Visibility.PROTECTED;
            } else {
                return Visibility.PACKAGE;
            }
        } catch (Exception e) {
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
                boolean isFinal = isFieldFinal(field);
                RelationType relationType = isFinal
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

        classDecl.findAll(ObjectCreationExpr.class).forEach(objCreation -> {
            try {
                String typeName = objCreation.getType().getNameAsString();
                if (!isPrimitive(typeName)) {

                    relationInfos.add(new RelationInfo(
                            diagramClass,
                            getFullClassName(packageName, typeName),
                            RelationType.DEPENDENCY,
                            "", "", "uses"
                    ));
                }
            } catch (Exception e) {

                LOGGER.log(Level.FINE, "Couldn't resolve type for object creation", e);
            }
        });
    }

    private boolean isFieldFinal(FieldDeclaration field) {
        try {

            java.lang.reflect.Method isFinalMethod = field.getClass().getMethod("isFinal");
            return (Boolean) isFinalMethod.invoke(field);
        } catch (Exception e) {

            return false;
        }
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