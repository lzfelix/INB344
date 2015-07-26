package week1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public class FrequencyTable {
	ArrayList<Element> elements;
	int totalAmount;
	
	public FrequencyTable(Map<String, Integer> table) {
		elements = new ArrayList<Element>();
		
		for (Entry<String, Integer> entry : table.entrySet()) {
			elements.add(new Element(entry.getKey(), entry.getValue()));
			totalAmount += entry.getValue();
		}
		
		Collections.sort(elements);
	}
	
	public void writeToFile(String fileName) throws FileNotFoundException {
		PrintWriter output = new PrintWriter(new File(fileName));
		
		for (Element e : elements)
			output.write(e.entry + " " + e.occurences + " " + ((float)e.occurences) / totalAmount + "\n");
		
		output.flush();
		output.close();
	}
	
	class Element implements Comparable<Element>{
		String entry;
		int occurences;
		
		public Element(String entry, int occurences) {
			this.entry = entry;
			this.occurences = occurences;
		}

		@Override
		public int compareTo(Element other) {
			return other.occurences - this.occurences;
		}
	}
}
