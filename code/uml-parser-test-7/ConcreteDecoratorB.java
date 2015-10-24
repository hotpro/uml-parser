
public class ConcreteDecoratorB {

    ConcreteDecoratorA component;
    private String addedState;

    public ConcreteDecoratorB( ConcreteDecoratorA c)
    {
        component = c;
    }

    public String operation()
    {
        addedState = component.operation() ;
        return addedBehavior( addedState ) ;
    }

    private String addedBehavior(String in) {
        return "<h1>" + addedState + "</h1>" ;
    }

}
