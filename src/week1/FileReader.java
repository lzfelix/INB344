package week1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class that scans all the files in a folder to calculate the frequency of words
 * within the files. The user can specify the files extensions to be parsed. Note 
 * that this support is limited to names that don't contain a . as part of it, for 
 * example ".tar.gz". This normally would not be a problem, since the files must be
 * plain text, such as .txt and .html.
 * 
 * @author Luiz Felix
 */
public class FileReader {
	private Map<String, Integer> table = new HashMap<>();
	private int amountOfFiles;
	
	/**
	 * Tokenises and compute the statistics for all the files within this folder that have
	 * their extension on <code>extensions</code>. The obtained data can be accessed though
	 * the methods <code>getVocabularySize()</code>, <code>getNumberOfElements()</code>,
	 * <code>getTable()</code> and <code>getAmountOfFiles()</code>.
	 *  
	 * @param folderPath the path to the folder to be scanned.
	 * @param allowNumbers if <code>false</code>, then number will not be considered on the
	 * parsing statistics.
	 * @param extensions the supported extensions, for example <code>{html, txt}</code>.
	 * @throws IOException if there is a problem reading any file.
	 */
	public void computeFolderStatistics(String folderPath, boolean allowNumbers, String extensions[]) throws IOException {
		File folder = new File(folderPath);
		amountOfFiles = 0;
		
		for (File file : folder.listFiles()) {
			if (file.isFile()) {				
				String completePath = file.getAbsolutePath();
				
				int nameEnd = completePath.lastIndexOf(".");
				if (nameEnd > 0) {
					//it has an extension
					String fileExtension = completePath.substring(nameEnd + 1);
					
					for (String ex : extensions)
						if (ex.equals(fileExtension)) {
							computeFileStatistics(completePath, allowNumbers);
							amountOfFiles++;
						}
				}
			}
		}
	}
	
	/**
	 * Tokenizes a single file. The obtained data can be accessed though
	 * the methods <code>getVocabularySize()</code>, <code>getNumberOfElements()</code>,
	 * <code>getTable()</code> and <code>getAmountOfFiles()</code>.
	 * 
	 * @param fileName The name of the file to be parsed.
	 * @param allowNumbers if <code>false</code>, then number will not be considered on the
	 * parsing statistics.
	 * @throws IOException if there is any problem while reading the file.
	 */
	public void computeFileStatistics(String fileName, boolean allowNumbers) throws IOException {
		String regex = (allowNumbers) ? "[[^\\w ][_]]" : "[[^\\w ][\\d_]]";
		
		for (String line : Files.readAllLines(Paths.get(fileName))) {
			//get rid of tabs
			line = line.replaceAll("\t", " ");
			
			//get rid of more than 1 whitespace and HTML tags
			line = line.trim().replaceAll("\\s{2,}", " ").replaceAll("<.*>", "");
			
			if (line.length() == 0) continue;
			
			//removing all non-alphabetic characters and multiple spaces
			line = line.replaceAll(regex, "");
			
			for (String word : line.split(" ")) {
				if (word.length() == 0) continue;
				
				//normalising data
				word = word.toLowerCase();
				
				if (table.containsKey(word)) {
					int previousAmount = table.get(word);
					
					table.remove(word);
					table.put(word, previousAmount + 1);
				}
				else
					table.put(word, 1);
			}
		}
	}
	
	/**
	 * @return The amount of different words (and number if this is the case)
	 * found on the last parsing.
	 */
	public int getVocabularySize() {
		return table.size();
	}
	
	/**
	 * @return The total amount of words (and number if this is the case) 
	 * found on the last parsing.
	 */
	public int getNumberOfElements() {
		int amount = 0;
		
		for (Entry<String, Integer> entry : table.entrySet())
			amount += entry.getValue();
		
		return amount;
	}
	
	/**
	 * @return Returns a dictionary holding all the found words and how many times it occurs.
	 */
	public Map<String, Integer> getTable() {
		return this.table;
	}
	
	/**
	 * Call this function only iff <code>computeFolderStatistics</code> was called previously.
	 * @return Returns the amount of files that were read on the last parsing.
	 */
	public int getAmountOfFiles() {
		return amountOfFiles;
	}
}
