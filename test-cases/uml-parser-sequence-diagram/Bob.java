public class Bob {

	private String addedState;
    private Eve component;

    public Bob(Eve c)
    {
        component = c;
    }

    public String drawAHorse()
    {
        addedState = component.drawHead() ;
        addedState = component.drawBody() ;
        return drawLegs(addedState) ;
    }

	public String drawLegs(String in) {
		return "<em>" + addedState + "</em>" ;
	}

}
