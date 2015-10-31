package edu.sjsu.cmpe.yutao;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yutao on 10/24/15.
 */
public class UmlSDActor {
    ClassOrInterfaceDeclaration classOrInterfaceDeclaration;
    Map<String, ClassOrInterfaceDeclaration> attrs = new HashMap<>();
    Map<String, MethodDeclaration> methods = new HashMap<>();
    Map<String, Map<String, ClassOrInterfaceDeclaration>> methodVariables = new HashMap<>();
}
