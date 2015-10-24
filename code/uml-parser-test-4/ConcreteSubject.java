                    
import java.util.*;

public class ConcreteSubject implements Subject {
 
	private String state;
	 
	private Collection<Observer> observers = new ArrayList<Observer>() ;
	 
	public String getState() {
		return state ;
	}
	 
	public void setState(String status) {
		state = status ;
        notifyObservers();
	}

	public void attach(Observer obj) {
	    observers.add(obj) ;
	}

	public void detach(Observer obj) {
        observers.remove(obj) ;
	}

	public void notifyObservers() {
        for (Observer obj  : observers)
        {
            obj.update();
        }
	}

    public void showState()
    {
        System.out.println( "Subject: " + this.getClass().getName() + " = " + subjectState );
    }
	 
}
 
