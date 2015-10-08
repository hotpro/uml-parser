package edu.sjsu.cmpe.yutao;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.util.*;

/**
 * Created by yutao on 10/6/15.
 */
public class UmlParser {

    private final String path;
    private final String filename;
    private HashMap<String, ClassOrInterfaceDeclaration> map;
    private ClassOrInterfaceDeclaration currentCID = null;
    private StringBuilder relationSB = new StringBuilder();
    private Map<String, UmlRelationship> relationshipMap = new HashMap<>();
    private StringBuilder sb = new StringBuilder();

    public UmlParser(String path, String filename) {

        this.path = path;
        this.filename = filename;
    }

    public void go() {
        File folder = new File(path);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".java");
            }
        });
        map = new HashMap<>();
        for (File file : files) {
            try {
                CompilationUnit cu = JavaParser.parse(file);
                ClassOrInterfaceDeclaration c = parseClassOrInterfaceDeclaration(cu);
                map.put(c.getName(), c);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        sb.append("@startuml\n");
        for (Map.Entry<String, ClassOrInterfaceDeclaration> entry : map.entrySet()) {
            printClass(entry.getValue());
        }
        printRelationShip();
        sb.append(relationSB.toString());
        sb.append("@enduml\n");
        log(sb.toString());
        draw(sb.toString(), filename);
    }


    private ClassOrInterfaceDeclaration parseClassOrInterfaceDeclaration(CompilationUnit cu) {
        List<Node> nodes = cu.getChildrenNodes();
        if (nodes != null) {
            for (Node n : nodes) {
                if (n instanceof ClassOrInterfaceDeclaration) {
                    return (ClassOrInterfaceDeclaration)n;
                }
            }
        }
        return null;
    }

    public void printClass(ClassOrInterfaceDeclaration cid) {
        this.currentCID = cid;
        sb.append("class ").append(cid.getName()).append(" {\n");
        List<Node> childrenNodes = cid.getChildrenNodes();
        for (Node childNode : childrenNodes) {
            if (childNode instanceof FieldDeclaration) {
                printField((FieldDeclaration) childNode);
            } else if (childNode instanceof MethodDeclaration) {
                printMethod((MethodDeclaration) childNode);
            }
        }
        sb.append("}\n");
    }

    public void printField(FieldDeclaration fd) {
        boolean isRelation = false;
        Type type = fd.getType();
        if (type instanceof ReferenceType) {
            Type subType = ((ReferenceType) type).getType();
            if (((ReferenceType) type).getArrayCount() > 0) {
                // array. int[], B[], String[]

                if (subType instanceof PrimitiveType) {
                    // int[]
                    printPrimitiveType(fd);

                } else if (subType instanceof ClassOrInterfaceType){
                    if (map.containsKey(((ClassOrInterfaceType) subType).getName())) {
                        // B[],
                        createRelationship((ClassOrInterfaceType) subType, "*");
                    } else {
                        // String[]
                        printPrimitiveType(fd);

                    }
                }
            } else {
                // Collection<B>, B b, String s
                if (subType instanceof ClassOrInterfaceType) {
                    if (((ClassOrInterfaceType) subType).getTypeArgs() != null) {
                        // Collection<B>, Collectio<String>
                        Type typeArg = (((ClassOrInterfaceType) subType).getTypeArgs().get(0));
                        if (typeArg instanceof ReferenceType) {
                            Type subsubsubType = ((ReferenceType) typeArg).getType();
                            if (subsubsubType instanceof ClassOrInterfaceType) {
                                if (map.containsKey(((ClassOrInterfaceType) subsubsubType).getName())) {
                                    // Collection<B>
                                    createRelationship((ClassOrInterfaceType) subsubsubType, "*");
                                } else {
                                    // TODO:
                                    // Collection<String>
                                    printPrimitiveType(fd);
                                }
                            }
                        }
                    } else {
                        if (map.containsKey(((ClassOrInterfaceType) subType).getName())) {
                            // B b,
                            createRelationship((ClassOrInterfaceType) subType, "1");
                        } else {
                            // String s
                            printPrimitiveType(fd);
                        }
                    }
                }
            }
        } else {
            // primitive type. int i
            printPrimitiveType(fd);
        }
    }

    private void createRelationship(ClassOrInterfaceType subType, String multiplicity) {
        ClassOrInterfaceDeclaration relatedCID = map.get(subType.getName());
        String relationKey = null;
        if (currentCID.getName().compareTo(relatedCID.getName()) < 0) {
            relationKey = currentCID.getName() + "_" + relatedCID.getName();
        } else {
            relationKey = relatedCID.getName() + "_" + currentCID.getName();
        }
        if (relationshipMap.containsKey(relationKey)) {
            UmlRelationship r = relationshipMap.get(relationKey);
            r.setMultiplicityA(multiplicity);
        } else {
            relationshipMap.put(relationKey, new UmlRelationship(currentCID,
                    "0",
                    relatedCID,
                    multiplicity,
                    UmlRelationShipType.AS));
        }
    }

    private void printRelationShip() {
        for (Map.Entry<String, UmlRelationship> entry : relationshipMap.entrySet()) {
            UmlRelationship r = entry.getValue();
            relationSB.append(r.getA().getName())
                    .append(" \"")
                    .append(r.getMultiplicityA())
                    .append("\" ")
                    .append(r.getType().getS())
                    .append(" \"")
                    .append(r.getMultiplicityB())
                    .append("\" ")
                    .append(r.getB().getName())
                    .append("\n");
        }
    }

    private void printPrimitiveType(FieldDeclaration fd) {
        sb.append(getModifier(fd.getModifiers()));
        sb.append(" ");
        sb.append(fd.getVariables().get(0).getId().getName());
        sb.append(" : ").append(fd.getType());
        sb.append("\n");
    }

    private void parseFiled(FieldDeclaration fd) {
        List<Node> childrenNodes = fd.getChildrenNodes();
        for (Node childNode : childrenNodes) {
            if (childNode instanceof ReferenceType) {
                parseReferenceType((ReferenceType) childNode);
            } else if (childNode instanceof VariableDeclarator) {
                parseVariableDeclarator((VariableDeclarator) childNode);
            } else if (childNode instanceof PrimitiveType) {
                parsePrimitiveType((PrimitiveType) childNode);
            }
        }
    }

    private void parsePrimitiveType(PrimitiveType primitiveType) {
        sb.append(getModifier(((FieldDeclaration)primitiveType.getParentNode()).getModifiers()));
        sb.append(" ");
        sb.append("");
        sb.append(" : ").append("fd.getType()");
        sb.append("\n");
    }

    private void parseReferenceType(ReferenceType referenceType) {

    }

    private void parseVariableDeclarator(VariableDeclarator variableDeclarator) {
        List<Node> childrenNodes = variableDeclarator.getChildrenNodes();
        for (Node childNode : childrenNodes) {
            if (childNode instanceof VariableDeclaratorId) {
                logln(((VariableDeclaratorId) childNode).getName());
            }
        }
    }

    private void log(String s) {
        System.out.print(s);
    }

    private void logln(String s) {
        System.out.println(s);
    }

    private boolean isRelation(FieldDeclaration fd) {
        if (fd.getType() instanceof ReferenceType) {
            ClassOrInterfaceType type =
                    (ClassOrInterfaceType) ((ReferenceType) fd.getType()).getType();
            if (map.containsKey(type)) {

            } else if (type.getName().equals("Collection")) {
                ClassOrInterfaceType subType =
                        (ClassOrInterfaceType) ((ReferenceType) type.getTypeArgs().get(0)).getType();
                if (map.containsKey(subType.getName())) {
                }
            }
        }

        return false;
    }

    public void printMethod(MethodDeclaration md) {
    }

    public String getModifier(int mod) {
        if ((mod & ModifierSet.PUBLIC) != 0)        return "+";
        if ((mod & ModifierSet.PROTECTED) != 0)     return "#";
        if ((mod & ModifierSet.PRIVATE) != 0)       return "-";
        return "~";
    }

    public String draw(String source, String output) {
        OutputStream png = null;
        try {
            png = new FileOutputStream(output);
            SourceStringReader reader = new SourceStringReader(source);

            // Write the first image to "png"
            String desc = reader.generateImage(png);

            // Return a null string if no generation
            return desc;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (png != null) {
                try {
                    png.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
