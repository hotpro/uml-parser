package edu.sjsu.cmpe.yutao;

/**
 * Created by yutao on 10/6/15.
 */
public class Main {
    public static void main(String[] args) {
        int[] cases = {1, 2, 3, 4, 5};
//        int[] cases = {5};
        for (int suffix : cases) {
            args = new String[2];
            args[0] = "test-cases/uml-parser-test-" + suffix;
            args[1] = "output" + suffix + ".png";
            if (args == null || args.length < 2) {
                System.out.println("Please input umlparser <classpath> <output file name>");
            }
            System.out.println("===========test case " + suffix + " start ========");
            new UmlParser(args[0], args[1]).go();
            System.out.println("===========test case " + suffix + " done ========");
        }
    }
}
