
public class ConcreteDecoratorA {

	private String addedState;
    private ConcreteComponent component;

    public ConcreteDecoratorA( ConcreteComponent c)
    {
        component = c;
    }

    public String operation()
    {
        addedState = component.operation() ;
        return addedBehavior( addedState ) ;
    }

	private String addedBehavior(String in) {
		return "<em>" + addedState + "</em>" ;
	}

}
