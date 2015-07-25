package week1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;



public class FileReader {
	private static final String folderPath = "/Users/luiz/Desktop/SET/week1/wk1/cacm/CACM-";
	private Map<String, Integer> table = new HashMap<>();
	
	public void computeStatistics(String fileName) throws IOException {
		
		for (String line : Files.readAllLines(Paths.get(folderPath + fileName + ".html"))) {
			//get rid of tabs
			line = line.replaceAll("\t", " ");
			
			//get rid of more than 1 whitespace
			line = line.trim().replaceAll(" +", " ");
			    
			if (line.length() > 0 && line.charAt(0) == '<') continue;
			
			for (String word : line.split(" ")) {
				if (table.containsKey(word)) {
					int previousAmount = table.get(word);
					
					table.remove(word);
					table.put(word, previousAmount + 1);
				}
				else
					table.put(word, 1);
			}
		}
		// comment
	}
	
	public int getVocabularySize() {
		return table.size();
	}
	
	public int getNumberOfElements() {
		int amount = 0;
		
		for (Entry<String, Integer> entry : table.entrySet())
			amount += entry.getValue();
		
		return amount;
	}
	
	public Map<String, Integer> getTable() {
		return this.table;
	}
}
