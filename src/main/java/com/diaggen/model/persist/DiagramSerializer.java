package com.diaggen.model.persist;

import com.diaggen.model.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DiagramSerializer {

    public void serialize(ClassDiagram diagram, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new GZIPOutputStream(new FileOutputStream(file)))) {

            DiagramDTO dto = convertToDTO(diagram);
            oos.writeObject(dto);
        }
    }

    public ClassDiagram deserialize(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {

            DiagramDTO dto = (DiagramDTO) ois.readObject();
            return convertFromDTO(dto);
        }
    }

    public DiagramDTO convertToDTO(ClassDiagram diagram) {
        DiagramDTO dto = new DiagramDTO();
        dto.setId(diagram.getId());
        dto.setName(diagram.getName());

        Map<String, DiagramClassDTO> classDTOs = new HashMap<>();

        for (DiagramClass diagramClass : diagram.getClasses()) {
            DiagramClassDTO classDTO = new DiagramClassDTO();
            classDTO.setId(diagramClass.getId());
            classDTO.setName(diagramClass.getName());
            classDTO.setPackageName(diagramClass.getPackageName());
            classDTO.setX(diagramClass.getX());
            classDTO.setY(diagramClass.getY());
            classDTO.setClassType(diagramClass.getClassType().name());

            for (Member attribute : diagramClass.getAttributes()) {
                MemberDTO memberDTO = new MemberDTO();
                memberDTO.setId(attribute.getId());
                memberDTO.setName(attribute.getName());
                memberDTO.setType(attribute.getType());
                memberDTO.setVisibility(attribute.getVisibility().name());
                classDTO.getAttributes().add(memberDTO);
            }

            for (Method method : diagramClass.getMethods()) {
                MethodDTO methodDTO = new MethodDTO();
                methodDTO.setId(method.getId());
                methodDTO.setName(method.getName());
                methodDTO.setReturnType(method.getReturnType());
                methodDTO.setVisibility(method.getVisibility().name());
                methodDTO.setAbstract(method.isAbstract());
                methodDTO.setStatic(method.isStatic());

                for (Parameter parameter : method.getParameters()) {
                    ParameterDTO paramDTO = new ParameterDTO();
                    paramDTO.setId(parameter.getId());
                    paramDTO.setName(parameter.getName());
                    paramDTO.setType(parameter.getType());
                    methodDTO.getParameters().add(paramDTO);
                }

                classDTO.getMethods().add(methodDTO);
            }

            classDTOs.put(diagramClass.getId(), classDTO);
            dto.getClasses().add(classDTO);
        }

        for (DiagramRelation relation : diagram.getRelations()) {
            DiagramRelationDTO relationDTO = new DiagramRelationDTO();
            relationDTO.setId(relation.getId());
            relationDTO.setSourceClassId(relation.getSourceClass().getId());
            relationDTO.setTargetClassId(relation.getTargetClass().getId());
            relationDTO.setRelationType(relation.getRelationType().name());
            relationDTO.setSourceMultiplicity(relation.getSourceMultiplicity());
            relationDTO.setTargetMultiplicity(relation.getTargetMultiplicity());
            relationDTO.setLabel(relation.getLabel());
            dto.getRelations().add(relationDTO);
        }

        return dto;
    }

    public ClassDiagram convertFromDTO(DiagramDTO dto) {

        ClassDiagram diagram = new ClassDiagram(dto.getName());


        Map<String, DiagramClass> classesMap = new HashMap<>();

        Map<String, String> oldToNewClassIds = new HashMap<>();

        for (DiagramClassDTO classDTO : dto.getClasses()) {
            ClassType classType = ClassType.valueOf(classDTO.getClassType());

            DiagramClass diagramClass = new DiagramClass(classDTO.getName(), classDTO.getPackageName(), classType);

            oldToNewClassIds.put(classDTO.getId(), diagramClass.getId());

            diagramClass.setX(classDTO.getX());
            diagramClass.setY(classDTO.getY());

            for (MemberDTO attributeDTO : classDTO.getAttributes()) {
                Visibility visibility = Visibility.valueOf(attributeDTO.getVisibility());

                Member attribute = new Member(attributeDTO.getName(), attributeDTO.getType(), visibility);
                diagramClass.addAttribute(attribute);
            }

            for (MethodDTO methodDTO : classDTO.getMethods()) {
                Visibility visibility = Visibility.valueOf(methodDTO.getVisibility());

                java.util.List<Parameter> parameters = new java.util.ArrayList<>();
                for (ParameterDTO paramDTO : methodDTO.getParameters()) {
                    Parameter param = new Parameter(paramDTO.getName(), paramDTO.getType());
                    parameters.add(param);
                }

                Method method = new Method(
                        methodDTO.getName(),
                        methodDTO.getReturnType(),
                        parameters,
                        visibility,
                        methodDTO.isAbstract(),
                        methodDTO.isStatic()
                );

                diagramClass.addMethod(method);
            }

            classesMap.put(diagramClass.getId(), diagramClass);
            diagram.addClass(diagramClass);
        }

        for (DiagramRelationDTO relationDTO : dto.getRelations()) {

            String newSourceClassId = oldToNewClassIds.get(relationDTO.getSourceClassId());
            String newTargetClassId = oldToNewClassIds.get(relationDTO.getTargetClassId());

            if (newSourceClassId != null && newTargetClassId != null) {
                DiagramClass sourceClass = classesMap.get(newSourceClassId);
                DiagramClass targetClass = classesMap.get(newTargetClassId);

                RelationType relationType = RelationType.valueOf(relationDTO.getRelationType());

                DiagramRelation relation = new DiagramRelation(
                        sourceClass,
                        targetClass,
                        relationType,
                        relationDTO.getSourceMultiplicity(),
                        relationDTO.getTargetMultiplicity(),
                        relationDTO.getLabel()
                );

                diagram.addRelation(relation);
            }
        }

        return diagram;
    }

    static class DiagramDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private java.util.List<DiagramClassDTO> classes = new java.util.ArrayList<>();
        private java.util.List<DiagramRelationDTO> relations = new java.util.ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public java.util.List<DiagramClassDTO> getClasses() { return classes; }
        public void setClasses(java.util.List<DiagramClassDTO> classes) { this.classes = classes; }

        public java.util.List<DiagramRelationDTO> getRelations() { return relations; }
        public void setRelations(java.util.List<DiagramRelationDTO> relations) { this.relations = relations; }
    }

    static class DiagramClassDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String packageName;
        private double x;
        private double y;
        private String classType;
        private java.util.List<MemberDTO> attributes = new java.util.ArrayList<>();
        private java.util.List<MethodDTO> methods = new java.util.ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public String getClassType() { return classType; }
        public void setClassType(String classType) { this.classType = classType; }

        public java.util.List<MemberDTO> getAttributes() { return attributes; }
        public void setAttributes(java.util.List<MemberDTO> attributes) { this.attributes = attributes; }

        public java.util.List<MethodDTO> getMethods() { return methods; }
        public void setMethods(java.util.List<MethodDTO> methods) { this.methods = methods; }
    }

    static class MemberDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String type;
        private String visibility;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
    }

    static class MethodDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String returnType;
        private String visibility;
        private boolean isAbstract;
        private boolean isStatic;
        private java.util.List<ParameterDTO> parameters = new java.util.ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }

        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }

        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public java.util.List<ParameterDTO> getParameters() { return parameters; }
        public void setParameters(java.util.List<ParameterDTO> parameters) { this.parameters = parameters; }
    }

    static class ParameterDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String type;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    static class DiagramRelationDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String sourceClassId;
        private String targetClassId;
        private String relationType;
        private String sourceMultiplicity;
        private String targetMultiplicity;
        private String label;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getSourceClassId() { return sourceClassId; }
        public void setSourceClassId(String sourceClassId) { this.sourceClassId = sourceClassId; }

        public String getTargetClassId() { return targetClassId; }
        public void setTargetClassId(String targetClassId) { this.targetClassId = targetClassId; }

        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }

        public String getSourceMultiplicity() { return sourceMultiplicity; }
        public void setSourceMultiplicity(String sourceMultiplicity) { this.sourceMultiplicity = sourceMultiplicity; }

        public String getTargetMultiplicity() { return targetMultiplicity; }
        public void setTargetMultiplicity(String targetMultiplicity) { this.targetMultiplicity = targetMultiplicity; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}