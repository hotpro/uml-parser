public class Main {

    public static void main(String[] args)
    {
        ConcreteDecoratorB obj = new ConcreteDecoratorB( new ConcreteDecoratorA( new ConcreteComponent() ) ) ;
        String result = obj.operation() ;
        System.out.println(result);
    }
}
