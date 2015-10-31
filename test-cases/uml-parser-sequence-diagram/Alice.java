public class Alice {

    private Bob bob;
    private Eve component;
    private String addedState;

    public Alice(Bob c)
    {
        bob = c;
    }

    public String start()
    {
        addedState = bob.drawAHorse() ;
        return drawTail(addedState) ;
    }

    public String drawTail(String in) {
        return "<h1>" + addedState + "</h1>" ;
    }

}
