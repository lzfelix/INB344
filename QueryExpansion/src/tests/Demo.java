package tests;

import java.util.Map.Entry;
import java.util.TreeMap;

public class Demo {
	public static void main(String args[]) {
		TreeMap<Double, String> t = new TreeMap<>();
		
		t.put((double) 10, "e");
		t.put((double) 5, "e");
		t.put((double) 1.2, "g");
		t.put((double) -100, "g");
		t.put((double) 300, "g");
		
		for (Entry<Double, String> e : t.entrySet())
			System.out.println(e);
	}
}
