package edu.sjsu.cmpe.yutao;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by yutao on 10/6/15.
 */
public class UmlParser {

    private final String path;
    private final String filename;
    private HashMap<String, ClassOrInterfaceDeclaration> classMap;
    private ClassOrInterfaceDeclaration currentCID = null;
    private StringBuilder relationSB = new StringBuilder();
    private StringBuilder classDiagramSB = new StringBuilder();
    private StringBuilder sequenceDiagramSB = new StringBuilder();

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
        classMap = new HashMap<>();
        for (File file : files) {
            try {
                CompilationUnit cu = JavaParser.parse(file);
                ClassOrInterfaceDeclaration c = parseClassOrInterfaceDeclaration(cu);
                classMap.put(c.getName(), c);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        classDiagramSB.append("@startuml\n");
        sequenceDiagramSB.append("@startuml\n");
        for (Map.Entry<String, ClassOrInterfaceDeclaration> entry : classMap.entrySet()) {
            printClassOrInterface(entry.getValue());
        }
        printRelationShip();
        classDiagramSB.append(relationSB.toString());
        classDiagramSB.append("@enduml\n");
        sequenceDiagramSB.append("@enduml\n");
        log(classDiagramSB.toString());
        draw(classDiagramSB.toString(), filename);
        logln("\n\n\nSequence:");
        log(sequenceDiagramSB.toString());
//        draw(sequenceDiagramSB.toString(), "s.png");

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

    public void printClassOrInterface(ClassOrInterfaceDeclaration cid) {
        this.currentCID = cid;

        // 1. member and method
        if (cid.isInterface()) {
            classDiagramSB.append("interface ").append(cid.getName()).append(" {\n");
            List<Node> childrenNodes = cid.getChildrenNodes();
            for (Node childNode : childrenNodes) {
            }
            classDiagramSB.append("}\n");
        } else {
            classDiagramSB.append("class ").append(cid.getName()).append(" {\n");
            List<Node> childrenNodes = cid.getChildrenNodes();
            for (Node childNode : childrenNodes) {
            }
            classDiagramSB.append("}\n");
        }

        // 2. inheritance
        List<ClassOrInterfaceType> extendsList = cid.getExtends();
        if (extendsList != null) {
            for (ClassOrInterfaceType classType : extendsList) {
                String name = classType.getName();
                if (classMap.containsKey(name)) {
                    String relationKey = name + "_" + cid.getName();
                }
            }
        }

        // 3. implementation
        List<ClassOrInterfaceType> implementList = cid.getImplements();
        if (implementList != null) {
            for (ClassOrInterfaceType interfaceType : implementList) {
                String name = interfaceType.getName();
                if (classMap.containsKey(name)) {
                    String relationKey = name + "_" + cid.getName();
                }
            }
        }
    }

    private void printConstructor(ConstructorDeclaration md) {
        //Public Methods (ignore private, package and protected scope)
        if (!isPublic(md.getModifiers())) {
            return;
        }
        Map<String, List<VariableDeclaratorId>> variableMap = new HashMap<>();
        Map<String, String> variableNameMap = new HashMap<>();
        classDiagramSB.append(getModifier(md.getModifiers())).append(md.getName()).append("(");
        List<Parameter> parameterList = md.getParameters();
        printParams(variableMap, variableNameMap, parameterList);
        classDiagramSB.append(")\n");

        BlockStmt body = md.getBlock();
        printBody(variableMap, variableNameMap, body);
    }

    private void printParams(Map<String, List<VariableDeclaratorId>> variableMap, Map<String, String> variableNameMap, List<Parameter> parameterList) {
        if (parameterList != null && parameterList.size() > 0) {
            int i = 0;
            for (Parameter p : parameterList) {
                Type type = p.getType();
                if (i > 0) {
                    classDiagramSB.append(", ");
                }
                classDiagramSB.append(p.getId().getName()).append(":").append(type);
                if (type instanceof ReferenceType) {
                    // A a,
                    Type subType = ((ReferenceType) type).getType();
                    if (subType instanceof ClassOrInterfaceType) {
                        String depName = ((ClassOrInterfaceType) subType).getName();

                        if (this.classMap.containsKey(depName) ) {

                            // Dependency
                            printDependency(depName);

                            // cache variable for sequence
                            List<VariableDeclaratorId> ids = new LinkedList<>();
                            ids.add(p.getId());
                            variableMap.put(depName, ids);
                            variableNameMap.put(p.getId().getName(), depName);
                        }
                    }
                }
                i++;
            }
        }
    }

    private void printDependency(String depName) {
        ClassOrInterfaceDeclaration depCID = this.classMap.get(depName);
        String relationKey = getASRelationKey(depName, currentCID.getName());
        // if they have stronger relationship, ignore dependency
    }

    private void printBody(Map<String, List<VariableDeclaratorId>> variableMap, Map<String, String> variableNameMap, BlockStmt body) {
        // body, sequence diagram
        do {
            if (body == null) {
                break;
            }
            List<Statement> expressionStmtList = body.getStmts();
            if (expressionStmtList == null) {
                break;
            }
            for (Statement stmt : expressionStmtList) {
                if (stmt instanceof ExpressionStmt) {
                    Expression expression = ((ExpressionStmt) stmt).getExpression();
                    if (expression instanceof VariableDeclarationExpr) {
                        String depName = ((VariableDeclarationExpr) expression).getType().toString();
                        if (classMap.containsKey(depName)) {
                            // dependency
                            printDependency(depName);

                            List<VariableDeclaratorId> list = null;
                            if (variableMap.containsKey(depName)) {
                                list = variableMap.get(depName);
                            } else {
                                list = new LinkedList<>();
                                variableMap.put(depName,
                                        list);
                            }
                            list.add(((VariableDeclarationExpr) expression).getVars().get(0).getId());
                            variableNameMap.put(((VariableDeclarationExpr) expression).getVars().get(0).getId().getName(),
                                    depName);
                        }
                    } else if (expression instanceof MethodCallExpr) {
                        Expression scopeExp = ((MethodCallExpr) expression).getScope();
                        String methodName = ((MethodCallExpr) expression).getName();
                        if (scopeExp != null) {
                            String scope = scopeExp.toString();
                            if (variableNameMap.containsKey(scope)) {
                                String className = variableNameMap.get(scope);
                                if (classMap.containsKey(className)) {
                                    sequenceDiagramSB.append(this.currentCID.getName())
                                            .append(" ")
                                            .append("->")
                                            .append(" ")
                                            .append(className)
                                            .append(": ")
                                            .append(methodName)
                                            .append("\n");
                                }
                            }
                        }
                    }
                }
            }

        } while (false);
    }

    private void createRelationship(ClassOrInterfaceType subType, String multiplicity) {
        ClassOrInterfaceDeclaration relatedCID = classMap.get(subType.getName());
        String relationKey = getASRelationKey(currentCID.getName(), relatedCID.getName());
    }

    private String getASRelationKey(String name1, String name2) {
        if (name1.compareTo(name2) < 0) {
            return name1 + "_" + name2;
        }
        return name2 + "_" + name1;
    }

    private void printRelationShip() {
    }

    private void printPrimitiveType(FieldDeclaration fd) {
        classDiagramSB.append(getModifier(fd.getModifiers()));
        classDiagramSB.append(" ");
        classDiagramSB.append(fd.getVariables().get(0).getId().getName());
        classDiagramSB.append(" : ").append(fd.getType());
        classDiagramSB.append("\n");
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
        classDiagramSB.append(getModifier(((FieldDeclaration) primitiveType.getParentNode()).getModifiers()));
        classDiagramSB.append(" ");
        classDiagramSB.append("");
        classDiagramSB.append(" : ").append("fd.getType()");
        classDiagramSB.append("\n");
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
            if (classMap.containsKey(type)) {

            } else if (type.getName().equals("Collection")) {
                ClassOrInterfaceType subType =
                        (ClassOrInterfaceType) ((ReferenceType) type.getTypeArgs().get(0)).getType();
                if (classMap.containsKey(subType.getName())) {
                }
            }
        }

        return false;
    }

    public String getModifier(int mod) {
        if ((mod & ModifierSet.PUBLIC) != 0)        return "+";
        if ((mod & ModifierSet.PROTECTED) != 0)     return "#";
        if ((mod & ModifierSet.PRIVATE) != 0)       return "-";
        return "~";
    }

    private boolean isPublic(int mod) {
        return (mod & ModifierSet.PUBLIC) != 0;
    }

    private boolean isPrivate(int mod) {
        return (mod & ModifierSet.PRIVATE) != 0;
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
