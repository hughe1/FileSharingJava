package Test;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import EZShare.Resource;

public class LambdaTest {

	public static void main(String[] args) {
		Consumer<Resource> c = System.out::println;
		c.accept(new Resource());
		BiFunction<Integer,Integer, Resource> c1 = LambdaTest::getInt;
		System.out.println(c1.apply(5, 5));
	}
	
	public static Resource getInt(int a, int b) {
		System.out.println(a + b);
		return new Resource();
	}
	
}
