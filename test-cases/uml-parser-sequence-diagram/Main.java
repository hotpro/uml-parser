public class Main {

    public static void main(String[] args)
    {
        Alice obj = new Alice( new Bob( new Eve() ) ) ;
        String result = obj.start() ;
        System.out.println(result);
    }
}
