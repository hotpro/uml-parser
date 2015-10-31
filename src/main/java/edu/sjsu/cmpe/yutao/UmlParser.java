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
import java.util.*;

/**
 * Created by yutao on 10/6/15.
 */
public class UmlParser {

    private final String path;
    private final String filename;
    private Map<String, ClassOrInterfaceDeclaration> classMap;
    private ClassOrInterfaceDeclaration currentCID = null;
    private UmlSDActor currentUmlSDActor = null;
    private StringBuilder relationSB = new StringBuilder();
    private Map<String, UmlRelationship> relationshipMap = new HashMap<>();
    private StringBuilder classDiagramSB = new StringBuilder();
    private StringBuilder sequenceDiagramSB = new StringBuilder();

    // class name -> umlsdactor
    private Map<String, UmlSDActor> sdActorMap = new HashMap<>();

    /** the class which has public static void main method*/
    private String psvmMethodClass;

    private Set<String> getterSetter;

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
                if (c != null) {
                    classMap.put(c.getName(), c);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        classDiagramSB.append("@startuml\n");
        for (Map.Entry<String, ClassOrInterfaceDeclaration> entry : classMap.entrySet()) {
            printClassOrInterface(entry.getValue());
        }
        printRelationShip();
        classDiagramSB.append(relationSB.toString());
        classDiagramSB.append("@enduml\n");
//        log(classDiagramSB.toString());
//        draw(classDiagramSB.toString(), filename);

        logln("\n\n\nSequence:");
//        printReturnMsg();
        sequenceDiagramSB.append("@startuml\n");
        startSequence();
        sequenceDiagramSB.append("@enduml\n");
        log(sequenceDiagramSB.toString());
        draw(sequenceDiagramSB.toString(), "sequence_diagram.png");
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

        // sequence diagram
        UmlSDActor actor = new UmlSDActor();
        this.currentUmlSDActor = actor;
        sdActorMap.put(cid.getName(), actor);
        actor.classOrInterfaceDeclaration = cid;

        getterSetter = new HashSet<>();

        // 1. member and method
        if (cid.isInterface()) {
            classDiagramSB.append("interface ").append(cid.getName()).append(" {\n");
            List<Node> childrenNodes = cid.getChildrenNodes();
            for (Node childNode : childrenNodes) {
                if (childNode instanceof FieldDeclaration) {
                    printField((FieldDeclaration) childNode);
                } else if (childNode instanceof MethodDeclaration) {
                    printMethod((MethodDeclaration) childNode);
                }
            }
            classDiagramSB.append("}\n");
        } else {
            classDiagramSB.append("class ").append(cid.getName()).append(" {\n");
            List<Node> childrenNodes = cid.getChildrenNodes();
            for (Node childNode : childrenNodes) {
                if (childNode instanceof FieldDeclaration) {
                    printField((FieldDeclaration) childNode);
                } else if (childNode instanceof MethodDeclaration) {
                    printMethod((MethodDeclaration) childNode);
                } else if (childNode instanceof ConstructorDeclaration) {
                    printConstructor((ConstructorDeclaration) childNode);
                }
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
                    relationshipMap.put(relationKey,
                            new UmlRelationship(classMap.get(name), "", cid, "", UmlRelationShipType.EX));
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
                    relationshipMap.put(relationKey,
                            new UmlRelationship(classMap.get(name), "", cid, "", UmlRelationShipType.IM));
                }
            }
        }
    }

    public void printField(FieldDeclaration fd) {

        // Private and Public Attributes (ignore package and protected scope)
        if (!isPrivate(fd.getModifiers()) && !isPublic(fd.getModifiers())) {
            return;
        }

        Type type = fd.getType();

        // Relationship
        if (type instanceof ReferenceType) {
            Type subType = ((ReferenceType) type).getType();
            if (((ReferenceType) type).getArrayCount() > 0) {
                // array. int[], B[], String[]

                if (subType instanceof PrimitiveType) {
                    // int[]
                    printPrimitiveType(fd);

                } else if (subType instanceof ClassOrInterfaceType){
                    if (classMap.containsKey(((ClassOrInterfaceType) subType).getName())) {
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
                                if (classMap.containsKey(((ClassOrInterfaceType) subsubsubType).getName())) {
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
                        if (classMap.containsKey(((ClassOrInterfaceType) subType).getName())) {
                            // B b,
                            createRelationship((ClassOrInterfaceType) subType, "1");

                            // sequence diagram
                            ClassOrInterfaceDeclaration relatedCID = classMap.get(((ClassOrInterfaceType) subType).getName());
                            String variableName = null;
                            variableName = fd.getVariables().get(0).getId().getName();
                            this.currentUmlSDActor.attrs.put(variableName, relatedCID);
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

    public void printMethod(MethodDeclaration md) {

        //Public Methods (ignore private, package and protected scope)
        // ignore getter setter
        if (!isPublic(md.getModifiers())
                || getterSetter.contains(md.getName())) {
            return;
        }

        // sequence
        Map<String, ClassOrInterfaceDeclaration> variables = new HashMap<>();
        this.currentUmlSDActor.methodVariables.put(md.getName(), variables);
        this.currentUmlSDActor.methods.put(md.getName(), md);


        if (isPSVM(md)) {
            psvmMethodClass = this.currentCID.getName();
        }

        // TODO: delete it
        // class name -> variable names
        Map<String, List<VariableDeclaratorId>> variableMap = new HashMap<>();

        // variableName -> class name
        Map<String, String> variableNameMap = new HashMap<>();
        classDiagramSB.append(getModifier(md.getModifiers())).append(md.getName()).append("(");
        List<Parameter> parameterList = md.getParameters();
        printParams(variableMap, variableNameMap, parameterList);
        classDiagramSB.append(") : ").append(md.getType()).append("\n");

        BlockStmt body = md.getBody();
        printBody(variableMap, variableNameMap, body);

        // sequence
        // variableNameMap => variables
        for (Map.Entry<String, String> entry : variableNameMap.entrySet()) {
            if (classMap.containsKey(entry.getValue())) {
                variables.put(entry.getKey(), classMap.get(entry.getValue()));
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
        if (!relationshipMap.containsKey(relationKey) && depCID.isInterface()) {
            relationshipMap.put(relationKey,
                    new UmlRelationship(depCID, "", this.currentCID, "", UmlRelationShipType.DEP));
        }
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
//                                    if (sequenceList.peekLast() != this.currentCID) {
//                                        sequenceList.offerLast(this.currentCID);
//                                    }
//                                    sequenceList.offerLast(classMap.get(className));
                                }
                            }
                        }
                    }
                }
            }

        } while (false);
    }

    private void printReturnMsg() {
//        while (sequenceList.size() >= 2) {
//            ClassOrInterfaceDeclaration last = sequenceList.pollLast();
//            sequenceDiagramSB.append(last.getName())
//                    .append(" ")
//                    .append("-->")
//                    .append(" ")
//                    .append(sequenceList.peekLast().getName())
//                    .append("\n");
//        }
    }

    private void createRelationship(ClassOrInterfaceType subType, String multiplicity) {
        ClassOrInterfaceDeclaration relatedCID = classMap.get(subType.getName());

        String relationKey = getASRelationKey(currentCID.getName(), relatedCID.getName());
        if (relationshipMap.containsKey(relationKey)) {
            UmlRelationship r = relationshipMap.get(relationKey);
            r.setMultiplicityA(multiplicity);
        } else {
            relationshipMap.put(relationKey, new UmlRelationship(currentCID,
                    "",
                    relatedCID,
                    multiplicity,
                    UmlRelationShipType.AS));
        }
    }

    private String getASRelationKey(String name1, String name2) {
        if (name1.compareTo(name2) < 0) {
            return name1 + "_" + name2;
        }
        return name2 + "_" + name1;
    }

    private void printRelationShip() {
        for (Map.Entry<String, UmlRelationship> entry : relationshipMap.entrySet()) {
            UmlRelationship r = entry.getValue();
            relationSB.append(r.getA().getName()).append(" ");
            if (r.getType() == UmlRelationShipType.AS && r.getMultiplicityA().length() > 0) {
                relationSB.append("\"")
                        .append(r.getMultiplicityA())
                        .append("\"");

            }
            relationSB.append(" ").append(r.getType().getS()).append(" ");
            if (r.getType() == UmlRelationShipType.AS && r.getMultiplicityB().length() > 0) {

                relationSB.append("\"")
                        .append(r.getMultiplicityB())
                        .append("\"");
            }
            relationSB.append(" ").append(r.getB().getName())
                    .append("\n");
        }
    }

    private void printPrimitiveType(FieldDeclaration fd) {
        // Support also Java Style Public Attributes as "setters and getters"
        String m = isGetSetAttr(fd) ? "+" : getModifier(fd.getModifiers());
        classDiagramSB.append(m);
        classDiagramSB.append(" ");
        classDiagramSB.append(fd.getVariables().get(0).getId().getName());
        classDiagramSB.append(" : ").append(fd.getType());
        classDiagramSB.append("\n");
    }

    private boolean isGetSetAttr(FieldDeclaration fd) {
        List<Node> childrenNodes = currentCID.getChildrenNodes();
        String name = fd.getVariables().get(0).getId().getName();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        boolean hasGet = false, hasSet = false;
        String getter = "", setter = "";
        for (Node node : childrenNodes) {
            if (node instanceof MethodDeclaration) {
                if (((MethodDeclaration) node).getName().equals("get" + name)) {
                    hasGet = true;
                    getter = ((MethodDeclaration) node).getName();
                }
                if (((MethodDeclaration) node).getName().equals("set" + name)) {
                    hasSet = true;
                    setter = ((MethodDeclaration) node).getName();
                }
            }
        }
        if (hasGet && hasSet) {
            getterSetter.add(getter);
            getterSetter.add(setter);
        }
        return hasGet && hasSet;
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

    private boolean isStatic(int mod) {
        return (mod & ModifierSet.STATIC) != 0;
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

    private boolean isPSVM(MethodDeclaration md) {
        boolean isPublic = isPublic(md.getModifiers());
        boolean isStatic = isStatic(md.getModifiers());
        boolean isVoid = md.getType().toString().equals("void");
        boolean isMain = md.getName().toString().equals("main");

        return isPublic && isStatic && isVoid && isMain;
    }

    private void startSequence() {
        if (psvmMethodClass == null) {
            return;
        }
        UmlSDActor actor = sdActorMap.get(psvmMethodClass);
        printCall(actor, actor.methods.get("main"));

    }

    private void printCall(UmlSDActor actor, MethodDeclaration md) {
        if (actor == null || md == null) {
            return;
        }
        BlockStmt body = md.getBody();
        do {
            Map<String, ClassOrInterfaceDeclaration> methodVariables = actor.methodVariables.get(md.getName());
            if (body == null) {
                break;
            }
            List<Statement> expressionStmtList = body.getStmts();
            if (expressionStmtList == null) {
                break;
            }
            for (Statement stmt : expressionStmtList) {
                MethodCallExpr methodCallExpr = findMethodCallExpr(stmt);

                if (methodCallExpr != null) {
                    Expression scopeExp = methodCallExpr.getScope();
                    ClassOrInterfaceDeclaration classOrInterfaceDeclaration = null;
                    String methodName = methodCallExpr.getName();
                    if (scopeExp != null) {
                        String scope = scopeExp.toString();
                        if (actor.attrs.containsKey(scope)) {
                            classOrInterfaceDeclaration = actor.attrs.get(scope);
                        }
                        if (methodVariables.containsKey(scope)) {
                            classOrInterfaceDeclaration = methodVariables.get(scope);
                        }
                    } else {
                        classOrInterfaceDeclaration = actor.classOrInterfaceDeclaration;
                    }
                    if (classOrInterfaceDeclaration != null) {

                        sequenceDiagramSB.append(actor.classOrInterfaceDeclaration.getName())
                                .append(" ")
                                .append("->")
                                .append(" ")
                                .append(classOrInterfaceDeclaration.getName())
                                .append(": ")
                                .append(methodName)
                                .append("\n");
                        sequenceDiagramSB.append("activate " + classOrInterfaceDeclaration.getName());

                        // if internal call, darksalmon color, otherwise FFBBBBB
                        if (actor.classOrInterfaceDeclaration.getName().equals(classOrInterfaceDeclaration.getName())) {
                            sequenceDiagramSB.append(" #DarkSalmon");
                        } else {
                            sequenceDiagramSB.append(" #FFBBBB");
                        }
                        sequenceDiagramSB.append("\n");
                        UmlSDActor nextActor = sdActorMap.get(classOrInterfaceDeclaration.getName());
                        printCall(nextActor, nextActor.methods.get(methodName));

                        // if not internal call, print return message
                        if (!actor.classOrInterfaceDeclaration.getName().equals(classOrInterfaceDeclaration.getName())) {
                            sequenceDiagramSB.append(classOrInterfaceDeclaration.getName())
                                    .append(" ")
                                    .append("-->")
                                    .append(" ")
                                    .append(actor.classOrInterfaceDeclaration.getName())
                                    .append("\n");
                        }
                        sequenceDiagramSB.append("deactivate " + classOrInterfaceDeclaration.getName() + "\n");
                    }
                }
            }

        } while (false);
    }

    private MethodCallExpr findMethodCallExpr(Node node) {
        if (node instanceof MethodCallExpr) {
            return (MethodCallExpr)node;
        }
        List<Node> nodes = node.getChildrenNodes();
        if (nodes != null) {
            for (Node n : nodes) {
                MethodCallExpr ans = findMethodCallExpr(n);
                if (ans != null) {
                    return ans;
                }
            }
        }
        return null;
    }
}
