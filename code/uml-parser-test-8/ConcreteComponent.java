import java.lang.System;

public class ConcreteComponent {

	public String operation() {
		doSomething();
		return "Hello World!";
	}

	public String operation2() {
		doSomething();
		return "Hello World!";
	}

	private void doSomething() {
		System.out.println("do something");
	}

}
