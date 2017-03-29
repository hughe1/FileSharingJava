package Test;

public class LambdaTest {

	public static void main(String[] args) {
//		Method m1 = new Method();
//		
//		new Thread(m1).start();
//		System.out.println("HELLO");

		new Thread(()->doSomething(100)).start();
	}
	
	private static int doSomething(int limit) {
		int sum = 0;
		for(int i = 1; i<=limit; i++) {
			sum += i;
		}
		return sum;
	}
	
}
