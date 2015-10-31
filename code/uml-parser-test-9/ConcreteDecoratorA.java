public class ConcreteDecoratorA {

	private String addedState;
    private ConcreteComponent component;

    public ConcreteDecoratorA( ConcreteComponent c)
    {
        component = c;
    }

    public String operation()
    {
        addedState = component.draw1() ;
        addedState = component.draw2() ;
        return addedBehavior( addedState ) ;
    }

	public String addedBehavior(String in) {
		return "<em>" + addedState + "</em>" ;
	}

}
