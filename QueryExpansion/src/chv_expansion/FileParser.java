package chv_expansion;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Parses the CHV file into a (lay -> technical) words thesaurus.
 * @author Luiz Ribeiro
 */
public class FileParser {
	
	/**
	 * Workout class, as Java doesn't have Pairs<> by default. This is used to
	 * store related words frequency in a unique way and order them.
	 * @author Luiz Ribeiro
	 */
	private class WordStatistics implements Comparable<WordStatistics>{
		public int frequency;
		public String word;
		
		public WordStatistics(String word, int frequency) {
			this.word = word;
			this.frequency = frequency;
		}
		
		@Override
		public int compareTo(WordStatistics other) {
			return other.frequency - this.frequency;
		}
		
		public String toString() {
			return word + "x " + frequency;
		}
	}
	
	private TwoSidedHash technicalDictionary;
	private HashMap<String, Pair> layDictionary;
	
	private final String PARAMS_SEPARATOR = "\t";
	private final String REMOVE_PARENTHESIS_REGEX = "\\s*\\(.*\\)";
	private final int AMOUNT_OF_PARAMETERS = 4;
	
	/**
	 * Opens the modified CHV file to create the lay to technical terms dictionary
	 * @param path The path to the CHV modified file containning {id, lay-term, technical-term1, technical-term2}
	 * tab-separated 
	 * @throws FileNotFoundException If there's an IO issue
	 */
	public FileParser(String path) throws FileNotFoundException {
		technicalDictionary = new TwoSidedHash();
		layDictionary = new HashMap<>();
		
		Scanner inputFile = new Scanner(new File(path));
		
		while (inputFile.hasNextLine()) {
			String currentLine = inputFile.nextLine();
			String[] parameters = parseParameters(currentLine);
			
			//inserting technical words
			int technical1 = technicalDictionary.forceDirectGet(parameters[2]);
			int technical2 = technicalDictionary.forceDirectGet(parameters[3]);
			
			Pair layVocabulary = new Pair(technical1, technical2);
			layDictionary.put(parameters[1], layVocabulary);
		}
		
		inputFile.close();
		technicalDictionary.invertDictionary();
	}
	
	/**
	 * Splits the input string on tabs and removes possible text between parenthesis.
	 * @param a read line with at least <code>AMOUNT_OF_PARAMETERS</code> parameters.
	 * @return an array with <code>AMOUNT_OF_PARAMETERS</code> elements.
	 */
	private String[] parseParameters(String inputLine) {
		String params[] = inputLine.split(PARAMS_SEPARATOR);
		
		// {i = 1 -> lay-word, i = 2, 3 -> technical words 2 and 3}
		for (int i = 1; i < AMOUNT_OF_PARAMETERS; i++)
			params[i] = params[i].replaceFirst(REMOVE_PARENTHESIS_REGEX, "");
		
		for (int i = 1; i < AMOUNT_OF_PARAMETERS; i++)
			params[i] = params[i].toLowerCase();
		
		return params;
	}
	
	/**
	 * Given a lay-people word, tries to find technical terms.
	 * @param term The lay-people vocabulary
	 * @return An 2-position array containing at least 1 word, if the lay-term was found. Otherwise the positions
	 * contain <code>null<code>.
	 */
	public String[] expand(String term) {
		String technicalTerms[] = new String[2];
		String technicalTerm;
		
		if (layDictionary.containsKey(term)) {
			Pair technicalIDs = layDictionary.get(term);
			
			technicalTerm = technicalDictionary.getInvertedKey(technicalIDs.getX());
			if (!technicalTerm.equals(term))
				technicalTerms[0] = technicalTerm;
				
			if (technicalIDs.getY() != null) {
				technicalTerm = technicalDictionary.getInvertedKey(technicalIDs.getY());
				if (!technicalTerm.equals(term))
					technicalTerms[1] = technicalTerm;
			}
		}
		
		return technicalTerms;
	}
	
	/**
	 * Tries to find new technical terms to a informed (possibly multi-worded) query. The thesaurus is
	 * searched word by word and current with next word (except for the last one). After finding all the
	 * technical words, they are sorted in decreasing way and just the <code>maxWords</code> more frequent
	 * words are returned.
	 * @param query The initial query. It may contain more than 1 word. Actually this increases precision.
	 * @param maxWords The maximum amount of words to be returned (the most frequent have preference)
	 * @return An array containing the <code>maxWords</code> related technical words. Bear in mind that
	 * the array may be empty.
	 */
	public String[] expandQuery(String query, int maxWords) {
		HashMap<String, Integer> expansionTable = new HashMap<>();
		String[] queryWords = query.split(" ");
		
		for (int i = 0; i < queryWords.length; i++) {
			//trying word alone
			String[] expandedSingle = expand(queryWords[i]);
			insertIntoExpansionTable(expansionTable, expandedSingle);
			
			//trying word + next
			if (i + 1 < queryWords.length) {
				String[] expandedDouble = expand(queryWords[i] + " " + queryWords[i + 1]);
				insertIntoExpansionTable(expansionTable, expandedDouble);
			}
		}
		
		WordStatistics[] newWords = new WordStatistics[expansionTable.size()];
		int count = 0;
		
		for (Entry<String, Integer> e : expansionTable.entrySet())
			newWords[count++] = new WordStatistics(e.getKey(), e.getValue());
		
		Arrays.sort(newWords);
		
		if (maxWords > newWords.length)
			maxWords = newWords.length;
		
		String[] mostFrequentNewWords = new String[maxWords];
		for (int i = 0; i < maxWords; i++)
			mostFrequentNewWords[i] = newWords[i].word;		
		
		return mostFrequentNewWords;
	}
	
	/* Extracted helper method to update the expansion table */
	private void insertIntoExpansionTable(HashMap<String, Integer> table, String[] words) {
		for (String word : words)
			if (word != null) {
				if (table.containsKey(word))
					table.put(word, table.get(word) + 1);
				else
					table.put(word, 1);
			}
	}
}
