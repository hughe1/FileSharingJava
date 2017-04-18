package Test;

import java.io.File;

public class TestURI {

	public static void main(String[] args) {
		File file = new File("D:/test.txt");
		System.out.println(file.toURI());

	}

}
