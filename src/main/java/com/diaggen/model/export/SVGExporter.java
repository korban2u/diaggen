package com.diaggen.model.export;

import com.diaggen.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class SVGExporter implements DiagramExporter {

    @Override
    public void export(ClassDiagram diagram, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {

            int width = 1200;
            int height = 800;

            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            writer.println("<svg width=\"" + width + "\" height=\"" + height + "\" xmlns=\"http://www.w3.org/2000/svg\">");

            writer.println("<defs>");
            writer.println("  <marker id=\"arrow\" markerWidth=\"10\" markerHeight=\"10\" refX=\"9\" refY=\"3\" orient=\"auto\" markerUnits=\"strokeWidth\">");
            writer.println("    <path d=\"M0,0 L0,6 L9,3 z\" fill=\"#000\"/>");
            writer.println("  </marker>");
            writer.println("  <marker id=\"diamond\" markerWidth=\"12\" markerHeight=\"12\" refX=\"6\" refY=\"6\" orient=\"auto\">");
            writer.println("    <path d=\"M0,6 L6,0 L12,6 L6,12 z\" fill=\"white\" stroke=\"black\"/>");
            writer.println("  </marker>");
            writer.println("  <marker id=\"filledDiamond\" markerWidth=\"12\" markerHeight=\"12\" refX=\"6\" refY=\"6\" orient=\"auto\">");
            writer.println("    <path d=\"M0,6 L6,0 L12,6 L6,12 z\" fill=\"black\" stroke=\"black\"/>");
            writer.println("  </marker>");
            writer.println("</defs>");

            writer.println("<style>");
            writer.println("  .class-box { fill: white; stroke: black; stroke-width: 2; }");
            writer.println("  .class-name { font-weight: bold; font-family: Arial; font-size: 14px; }");
            writer.println("  .class-type { font-style: italic; font-family: Arial; font-size: 12px; }");
            writer.println("  .attribute { font-family: Arial; font-size: 12px; }");
            writer.println("  .method { font-family: Arial; font-size: 12px; }");
            writer.println("  .relation { stroke: black; stroke-width: 1.5; fill: none; }");
            writer.println("  .relation-label { font-family: Arial; font-size: 11px; }");
            writer.println("  .multiplicity { font-family: Arial; font-size: 11px; }");
            writer.println("</style>");

            Map<String, double[]> classPositions = new HashMap<>();

            int x = 50;
            int y = 50;
            int maxHeight = 0;

            for (DiagramClass diagramClass : diagram.getClasses()) {
                int classX = (int) diagramClass.getX();
                int classY = (int) diagramClass.getY();

                if (classX == 0 && classY == 0) {
                    classX = x;
                    classY = y;
                    x += 300;
                    if (x > width - 250) {
                        x = 50;
                        y += maxHeight + 100;
                        maxHeight = 0;
                    }
                }

                int classWidth = 200;
                int nameHeight = 30;
                int separatorHeight = 2;
                int attributeHeight = 20 * diagramClass.getAttributes().size();
                if (attributeHeight == 0) attributeHeight = separatorHeight;
                int methodHeight = 20 * diagramClass.getMethods().size();
                if (methodHeight == 0) methodHeight = separatorHeight;
                int classHeight = nameHeight + separatorHeight + attributeHeight + separatorHeight + methodHeight;

                maxHeight = Math.max(maxHeight, classHeight);

                writer.println("<rect x=\"" + classX + "\" y=\"" + classY + "\" width=\"" + classWidth + "\" height=\"" + classHeight + "\" class=\"class-box\"/>");

                String classTypeText = "";
                if (diagramClass.getClassType() == ClassType.INTERFACE) {
                    classTypeText = "«interface»";
                } else if (diagramClass.getClassType() == ClassType.ABSTRACT_CLASS) {
                    classTypeText = "«abstract»";
                } else if (diagramClass.getClassType() == ClassType.ENUM) {
                    classTypeText = "«enumeration»";
                }

                if (!classTypeText.isEmpty()) {
                    writer.println("<text x=\"" + (classX + classWidth/2) + "\" y=\"" + (classY + 15) + "\" text-anchor=\"middle\" class=\"class-type\">" + classTypeText + "</text>");
                    writer.println("<text x=\"" + (classX + classWidth/2) + "\" y=\"" + (classY + 35) + "\" text-anchor=\"middle\" class=\"class-name\">" + diagramClass.getName() + "</text>");
                } else {
                    writer.println("<text x=\"" + (classX + classWidth/2) + "\" y=\"" + (classY + 20) + "\" text-anchor=\"middle\" class=\"class-name\">" + diagramClass.getName() + "</text>");
                }

                writer.println("<line x1=\"" + classX + "\" y1=\"" + (classY + nameHeight) + "\" x2=\"" + (classX + classWidth) + "\" y2=\"" + (classY + nameHeight) + "\" stroke=\"black\" stroke-width=\"1\"/>");

                int attributeY = classY + nameHeight + 5;
                for (Member attribute : diagramClass.getAttributes()) {
                    writer.println("<text x=\"" + (classX + 10) + "\" y=\"" + (attributeY + 15) + "\" class=\"attribute\">" +
                            attribute.getVisibility().getSymbol() + " " + attribute.getName() + " : " + attribute.getType() + "</text>");
                    attributeY += 20;
                }

                writer.println("<line x1=\"" + classX + "\" y1=\"" + (classY + nameHeight + attributeHeight + separatorHeight) +
                        "\" x2=\"" + (classX + classWidth) + "\" y2=\"" + (classY + nameHeight + attributeHeight + separatorHeight) +
                        "\" stroke=\"black\" stroke-width=\"1\"/>");

                int methodY = classY + nameHeight + attributeHeight + separatorHeight + 5;
                for (Method method : diagramClass.getMethods()) {
                    StringBuilder methodText = new StringBuilder();
                    methodText.append(method.getVisibility().getSymbol()).append(" ");
                    if (method.isStatic()) {
                        methodText.append("static ");
                    }
                    if (method.isAbstract()) {
                        methodText.append("abstract ");
                    }
                    methodText.append(method.getName()).append("(");

                    boolean first = true;
                    for (Parameter param : method.getParameters()) {
                        if (!first) {
                            methodText.append(", ");
                        }
                        methodText.append(param.getName()).append(" : ").append(param.getType());
                        first = false;
                    }

                    methodText.append(") : ").append(method.getReturnType());

                    writer.println("<text x=\"" + (classX + 10) + "\" y=\"" + (methodY + 15) + "\" class=\"method\">" +
                            methodText + "</text>");
                    methodY += 20;
                }

                double[] position = {
                    classX + classWidth / 2,
                    classY + classHeight / 2,
                    classX, classY,
                    classX + classWidth, classY + classHeight
                };
                classPositions.put(diagramClass.getId(), position);
            }

            for (DiagramRelation relation : diagram.getRelations()) {
                DiagramClass source = relation.getSourceClass();
                DiagramClass target = relation.getTargetClass();

                if (!classPositions.containsKey(source.getId()) || !classPositions.containsKey(target.getId())) {
                    continue;
                }

                double[] sourcePos = classPositions.get(source.getId());
                double[] targetPos = classPositions.get(target.getId());

                double srcX = sourcePos[0];
                double srcY = sourcePos[1];
                double tgtX = targetPos[0];
                double tgtY = targetPos[1];

                double srcBoxLeft = sourcePos[2];
                double srcBoxTop = sourcePos[3];
                double srcBoxRight = sourcePos[4];
                double srcBoxBottom = sourcePos[5];

                double tgtBoxLeft = targetPos[2];
                double tgtBoxTop = targetPos[3];
                double tgtBoxRight = targetPos[4];
                double tgtBoxBottom = targetPos[5];

                int startX, startY, endX, endY;

                if (srcX < tgtX && Math.abs(srcY - tgtY) < Math.abs(srcX - tgtX)) {
                    startX = (int) srcBoxRight;
                    startY = (int) srcY;
                    endX = (int) tgtBoxLeft;
                    endY = (int) tgtY;
                } else if (srcX > tgtX && Math.abs(srcY - tgtY) < Math.abs(srcX - tgtX)) {
                    startX = (int) srcBoxLeft;
                    startY = (int) srcY;
                    endX = (int) tgtBoxRight;
                    endY = (int) tgtY;
                } else if (srcY < tgtY) {
                    startX = (int) srcX;
                    startY = (int) srcBoxBottom;
                    endX = (int) tgtX;
                    endY = (int) tgtBoxTop;
                } else {
                    startX = (int) srcX;
                    startY = (int) srcBoxTop;
                    endX = (int) tgtX;
                    endY = (int) tgtBoxBottom;
                }

                String markerEnd = "";
                if (relation.getRelationType() == RelationType.INHERITANCE ||
                    relation.getRelationType() == RelationType.IMPLEMENTATION) {
                    markerEnd = " marker-end=\"url(#arrow)\"";
                } else if (relation.getRelationType() == RelationType.AGGREGATION) {
                    markerEnd = " marker-start=\"url(#diamond)\"";
                } else if (relation.getRelationType() == RelationType.COMPOSITION) {
                    markerEnd = " marker-start=\"url(#filledDiamond)\"";
                } else if (relation.getRelationType() == RelationType.DEPENDENCY ||
                           relation.getRelationType() == RelationType.ASSOCIATION) {
                    markerEnd = " marker-end=\"url(#arrow)\"";
                }

                String strokeDashArray = "";
                if (relation.getRelationType() == RelationType.IMPLEMENTATION ||
                    relation.getRelationType() == RelationType.DEPENDENCY) {
                    strokeDashArray = " stroke-dasharray=\"5,5\"";
                }

                writer.println("<line x1=\"" + startX + "\" y1=\"" + startY + "\" x2=\"" + endX + "\" y2=\"" + endY +
                        "\" class=\"relation\"" + markerEnd + strokeDashArray + "/>");

                double midX = (startX + endX) / 2;
                double midY = (startY + endY) / 2;

                if (!relation.getLabel().isEmpty()) {
                    writer.println("<text x=\"" + midX + "\" y=\"" + (midY - 10) + "\" text-anchor=\"middle\" class=\"relation-label\">" +
                            relation.getLabel() + "</text>");
                }

                if (!relation.getSourceMultiplicity().isEmpty()) {
                    writer.println("<text x=\"" + (startX + 15) + "\" y=\"" + (startY - 5) + "\" text-anchor=\"middle\" class=\"multiplicity\">" +
                            relation.getSourceMultiplicity() + "</text>");
                }

                if (!relation.getTargetMultiplicity().isEmpty()) {
                    writer.println("<text x=\"" + (endX - 15) + "\" y=\"" + (endY - 5) + "\" text-anchor=\"middle\" class=\"multiplicity\">" +
                            relation.getTargetMultiplicity() + "</text>");
                }
            }

            writer.println("</svg>");
        }
    }
}


