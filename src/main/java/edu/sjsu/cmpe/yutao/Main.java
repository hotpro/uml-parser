package edu.sjsu.cmpe.yutao;

/**
 * Created by yutao on 10/6/15.
 */
public class Main {
    public static void main(String[] args) {
        args = new String[2];
        int suffix = 9;
        args[0] = "code/uml-parser-test-" + suffix;
        args[1] = "output" + suffix + ".png";
        if (args == null || args.length < 2) {
            System.out.println("Please input umlparser <classpath> <output file name>");
        }
        new UmlParser(args[0], args[1]).go();

        System.out.println("\n\n\n");
        System.out.println("done");
    }
}
