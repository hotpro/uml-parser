package edu.sjsu.cmpe.yutao;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yutao on 10/6/15.
 */
public class UmlParser {

    private final String path;
    private final String filename;
    private HashMap<String, ClassOrInterfaceDeclaration> map;
    private StringBuilder relationSB = new StringBuilder();
    private Map<String, String> relationMap = new HashMap<>();

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

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        for (Map.Entry<String, ClassOrInterfaceDeclaration> entry : map.entrySet()) {
            String source = printClass(entry.getValue());
            sb.append(source);
        }
        sb.append(relationSB.toString());
        sb.append("@enduml\n");
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

    public String printClass(ClassOrInterfaceDeclaration cid) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(cid.getName()).append(" {\n");
        List<Node> nodes = cid.getChildrenNodes();
        for (Node n : nodes) {
            if (n instanceof FieldDeclaration) {
                sb.append(printField(cid, (FieldDeclaration) n));
            } else if (n instanceof MethodDeclaration) {
                sb.append(printMethod((MethodDeclaration) n));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String printField(ClassOrInterfaceDeclaration cid, FieldDeclaration fd) {
        StringBuilder sb = new StringBuilder();
        boolean isRelation = false;
        for (VariableDeclarator vdor : fd.getVariables()) {
            if (fd.getType() instanceof ReferenceType
                    && ((ReferenceType) fd.getType()).getType() instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType type =
                        (ClassOrInterfaceType)((ReferenceType)fd.getType()).getType();
                if (map.containsKey(type.getName())) {
                    isRelation = true;

                } else if (type.getName().equals("Collection")) {
                    ClassOrInterfaceType subType =
                             (ClassOrInterfaceType)((ReferenceType)type.getTypeArgs().get(0)).getType();
                    if (map.containsKey(subType.getName())) {
                        isRelation = true;
                        relationSB.append(cid.getName())
                                .append(" \"1\" -- \"*\" ")
                                .append(subType.getName())
                                .append("\n");
                    }
                }
            }
            if (!isRelation) {
                sb.append(getModifier(fd.getModifiers()));
                sb.append(" ");
                sb.append(vdor.getId().getName());
                sb.append(" : ").append(fd.getType());
                sb.append("\n");
            }
        }
        return sb.toString();
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

    public String t() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    public String printMethod(MethodDeclaration md) {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
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
