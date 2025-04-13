package com.diaggen.model.persist;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.Project;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ProjectSerializer {

    private final DiagramSerializer diagramSerializer;

    public ProjectSerializer() {
        this.diagramSerializer = new DiagramSerializer();
    }

    public void serialize(Project project, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new GZIPOutputStream(new FileOutputStream(file)))) {

            ProjectDTO dto = convertToDTO(project);
            oos.writeObject(dto);
        }
    }

    public Project deserialize(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {

            ProjectDTO dto = (ProjectDTO) ois.readObject();
            return convertFromDTO(dto);
        }
    }

    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setCreated(project.getCreated());
        dto.setLastModified(project.getLastModified());

        for (ClassDiagram diagram : project.getDiagrams()) {
            DiagramSerializer.DiagramDTO diagramDTO = diagramSerializer.convertToDTO(diagram);
            dto.getDiagrams().add(diagramDTO);
        }

        return dto;
    }

    private Project convertFromDTO(ProjectDTO dto) {
        Project project = new Project(dto.getName(), dto.getDescription());

        for (DiagramSerializer.DiagramDTO diagramDTO : dto.getDiagrams()) {
            ClassDiagram diagram = diagramSerializer.convertFromDTO(diagramDTO);
            project.addDiagram(diagram);
        }

        return project;
    }
}