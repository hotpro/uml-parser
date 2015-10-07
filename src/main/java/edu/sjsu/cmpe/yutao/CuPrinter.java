package edu.sjsu.cmpe.yutao;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by yutao on 9/15/15.
 */
public class CuPrinter {
    public static void main(String[] args) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("code/TestChild.java");
            CompilationUnit cu = JavaParser.parse(fis);
//            cu = createCU();
//            new MethodChangerVisitor().visit(cu, null);
//            changeMethods(cu);
            System.out.println(cu.toString());
            printInheritance(cu);
//            new MethodVisitor().visit(cu, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void printInheritance(CompilationUnit cu) {

        Node parentNode = cu.getParentNode();
        List<Node> childNodes = cu.getChildrenNodes();

        List<TypeDeclaration> types = cu.getTypes();
        for (TypeDeclaration type : types) {
            System.out.println(type);
            if (type instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration)type;
                List<ClassOrInterfaceType> classOrInterfaceTypes = classOrInterfaceDeclaration.getExtends();
                for (ClassOrInterfaceType classOrInterfaceType : classOrInterfaceTypes) {
                    System.out.println(classOrInterfaceType);
                }
            }
        }
    }

    private static class MethodVisitor extends VoidVisitorAdapter {
        @Override
        public void visit(MethodDeclaration n, Object arg) {
            // here you can access the attributes of the method.
            // this method will be called for all methods in this
            // CompilationUnit, including inner class methods
            System.out.println(n.getName());
        }
    }

    private static class MethodChangerVisitor extends VoidVisitorAdapter {
        @Override
        public void visit(MethodDeclaration n, Object arg) {
            n.setName(n.getName().toUpperCase());
            Parameter newArg = ASTHelper.createParameter(ASTHelper.INT_TYPE, "value");
            ASTHelper.addParameter(n, newArg);
        }
    }

    private static void changeMethods(CompilationUnit cu) {
        List<TypeDeclaration> types = cu.getTypes();
        for (TypeDeclaration type : types) {
            List<BodyDeclaration> members = type.getMembers();
            for (BodyDeclaration member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    changeMethod(method);
                }
            }
        }
    }

    private static void changeMethod(MethodDeclaration n) {
        // change the name of the method to upper case
        n.setName(n.getName().toUpperCase());

        // create the new parameter
        Parameter newArg = ASTHelper.createParameter(ASTHelper.INT_TYPE, "value");

        // add the parameter to the method
        ASTHelper.addParameter(n, newArg);
    }

    /**
     * creates the compilation unit
     */
    private static CompilationUnit createCU() {
        CompilationUnit cu = new CompilationUnit();
        // set the package
        cu.setPackage(new PackageDeclaration(ASTHelper.createNameExpr("java.parser.test")));

        // create the type declaration
        ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ModifierSet.PUBLIC, false, "GeneratedClass");
        ASTHelper.addTypeDeclaration(cu, type);

        // create a method
        MethodDeclaration method = new MethodDeclaration(ModifierSet.PUBLIC, ASTHelper.VOID_TYPE, "main");
        method.setModifiers(ModifierSet.addModifier(method.getModifiers(), ModifierSet.STATIC));
        ASTHelper.addMember(type, method);

        // add a parameter to the method
        Parameter param = ASTHelper.createParameter(ASTHelper.createReferenceType("String", 0), "args");
        param.setVarArgs(true);
        ASTHelper.addParameter(method, param);

        // add a body to the method
        BlockStmt block = new BlockStmt();
        method.setBody(block);

        // add a statement do the method body
        NameExpr clazz = new NameExpr("System");
        FieldAccessExpr field = new FieldAccessExpr(clazz, "out");
        MethodCallExpr call = new MethodCallExpr(field, "println");
        ASTHelper.addArgument(call, new StringLiteralExpr("Hello World!"));
        ASTHelper.addStmt(block, call);

        return cu;
    }
}
