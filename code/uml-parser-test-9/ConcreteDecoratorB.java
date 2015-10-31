public class ConcreteDecoratorB {

    private ConcreteDecoratorA componentA;
    private ConcreteComponent component;
    private String addedState;

    public ConcreteDecoratorB( ConcreteDecoratorA c)
    {
        componentA = c;
    }

    public String operation()
    {
        addedState = componentA.operation() ;
        return addedBehavior( addedState ) ;
    }

    public String addedBehavior(String in) {
        component.draw2();
        return "<h1>" + addedState + "</h1>" ;
    }

}
