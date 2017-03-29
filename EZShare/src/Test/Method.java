package Test;

import java.util.AbstractMap;
import java.util.ConcurrentModificationException;

public class Method implements Runnable {
	
	AbstractMap<String,Integer> map;
	
//	public Method(AbstractMap map) {
//		this.map = map; 
//	}
	
	@Override
	public void run() {
		for (int i=0; i<100; i++) {
			// map.put(Integer.toString(i), i);
			// System.out.println("{" + Thread.currentThread().getName() + ", " + i + "}");
			System.out.println(Thread.currentThread().getName() + " " + i);
		}			
	}
	
}
