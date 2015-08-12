package week3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class indexes all files in a given folder under the assumption that they are all text files.
 * To avoid the indexing of a file, its name has to begin with '.'. The class doesn't store a secondary
 * index to keep track of the names of the files, but this can be easily implemented. (It is not done
 * because the corpus's files are all numerically identified).
 *
 * 
 * @author Luiz Felix
 */
public class Indexer {
	
	/* To match the corpus's file names */
	private final int INITIAL_ID_COUNTER = 1;
	private int docId;
	
	private Map<String, BitSet> invertedIndex;
	
	private static String readFile(Path textFilePath, boolean allowNumbers) throws IOException {
		String regex = (allowNumbers) ? "[[^\\w ][_]]" : "[[^\\w ][\\d_]]";
		StringBuilder buffer = new StringBuilder();
		
		Files.readAllLines(textFilePath).forEach(line -> {
			//get rid of tabs
			line = line.replaceAll("\t", " ");
			
			//get rid of more than 1 whitespace and HTML tags
			line = line.trim().replaceAll("\\s{2,}", " ").replaceAll("<.*>", "");
			
			if (line.length() > 0) {
				//removing all non-alphabetic characters and multiple spaces
				line = line.replaceAll(regex, "");
				
				String[] words = line.split(" ");
				
				for (int i = 0; i < words.length; i++) {
					if (words[i].length() == 0) continue;
				
					//uncomment this to normalise all the words to lower case (NER may fail) 
					words[i] = words[i].toLowerCase();
					buffer.append(words[i]);
					
					buffer.append("\n");	
				}
			}
		});
		
		return buffer.toString();
	}
	
	/**
	 * Indexes all files from a given folder that are not hidden. 
	 * <pre>All the files on this folder must be text otherwise must be hidden.</pre>
	 * 
	 * @param path The path to the folder to be indexed.
	 * @throws IOException If an IO fault occurs.
	 */
	public Indexer(String path) throws IOException {
		invertedIndex = new HashMap<String, BitSet>();
		docId = INITIAL_ID_COUNTER;
		
		Files.list(new File(path).toPath()).forEach(filePath -> {
			String purgedFile = "";
			
			// skip hidden files
			if (filePath.getFileName().toString().indexOf(".") == 0) return;
			
			// fail silently
			try {
				purgedFile = readFile(filePath, false);
			} catch (Exception e) {
				System.err.println("Error while indexing file " + filePath);
			}
			
			// add word to the inverted index and update it
			for (String word : purgedFile.split("\\n")) {
				if (invertedIndex.containsKey(word))
					invertedIndex.get(word).set(docId, true);
				else {
					BitSet documents = new BitSet(docId + 1);
					documents.set(docId);
					
					invertedIndex.put(word, documents);
				}
			}
			
			docId++;
		});
	}
	
	/**
	 * Returns a list with the ID of the documents that contains the term <code>word</code>.
	 * This method is case insensitive.
	 * @param word The word to be searched on all files.
	 * @return A String with the ID of each matched document. If none is found, <code>null</code>
	 * is returned.
	 * @throws IndexerException if the query is an empty string or <code>null</code>.
	 */
	public String simpleQuery(String word) throws IndexerException {
		word = isInvalidString(word);
		if (word == null)
			throw new IndexerException("Invalid empty query.");

		if (invertedIndex.containsKey(word))
			return invertedIndex.get(word).toString();
		return null;
	}
	
	/**
	 * Returns a list with the ID of all the documents that contains all the terms on <code>words</code>.
	 * @param words The words to be queried.
	 * @return A String with the ID of each matched document. If none is found, <code>null</code>
	 * is returned.
	 * @throws IndexerException if any element of the query is an empty string or <code>null</code>. 
	 */
	public String andQuery(String[] words) throws IndexerException {
		BitSet result = null;
		
		for (int i = 0; i < words.length; i++) {
			
			words[i] = isInvalidString(words[i]);
			if (words[i] == null)
				throw new IndexerException("The term number " + i + " is invalid.");
			
			// 0 (AND) X = 0; X = anything
			if (!invertedIndex.containsKey(words[i])) {
				if (result != null) 
					result.clear();
			}
			else {
				if (result == null)
					result = (BitSet) invertedIndex.get(words[i]).clone();
				else
					result.and(invertedIndex.get(words[i]));
			}
		}
		
		if (result == null || result.cardinality() == 0) return null;
		
		return result.toString();
	}
	
	/**
	 * Returns a list with the ID of all the documents that contains at least one term on <code>words</code>.
	 * @param words The words to be queried.
	 * @return A String with the ID of each matched document. If none is found, <code>null</code>
	 * is returned.
	 * @throws IndexerException if any element of the query is an empty string or <code>null</code>. 
	 */
	public String orQuery(String[] words) throws IndexerException {
		BitSet result = null;
		
		for (int i = 0; i < words.length; i++) {
			words[i] = isInvalidString(words[i]);
			if (words[i] == null)
				throw new IndexerException("The term number " + i + " is invalid.");
			
			// 0 (OR) X = X; X = anything
			if (invertedIndex.containsKey(words[i])) {
				if (result == null)
					result = (BitSet) invertedIndex.get(words[i]).clone();
				else
					result.or(invertedIndex.get(words[i]));
			}
		}
		
		if (result == null || result.cardinality() == 0) return null;
		
		return result.toString();
	}
	
	/**
	 * Returns a list with the ID of all the documents that match ONLY the first element on <code>words</code>.
	 * @param words The words to be queried.
	 * @return A String with the ID of each matched document. If none is found, <code>null</code>
	 * is returned.
	 * @throws IndexerException if any element of the query is an empty string or <code>null</code>. 
	 */
	public String notQuery(String[] words) throws IndexerException {
		if (words.length < 2)
			throw new IndexerException("At least two terms must be queried");
		
		words[0] = isInvalidString(words[0]);
		if (words[0] == null)
			throw new IndexerException("The first term of the query is invalid.");
			
		BitSet toReturn = (BitSet) invertedIndex.get(words[0]).clone();
		if (toReturn == null) return null;
		
		for (int i = 1; i < words.length; i++) {
			words[i] = isInvalidString(words[i]);
			if (words[i] == null)
				throw new IndexerException("The term number " + i + " is invalid.");
			
			if (!invertedIndex.containsKey(words[i]))
				continue;
			
			//finds ~B
			BitSet currentSearch = (BitSet) invertedIndex.get(words[i]).clone();
			currentSearch.flip(0, currentSearch.size());
			
			//computes A & ~B
			toReturn.and(currentSearch);
		}
		
		// complying with Javadoc
		if (toReturn.cardinality() == 0) return null;
		return toReturn.toString();
	}
	
	/*
	 * Internal method that lowecases a word and removes all its spaces (?). If the string is invalid, returns null
	 */
	private String isInvalidString(String s) {
		if (s == null) return s;
		
		s = (s.replace("\\s", "")).toLowerCase();
		if (s.isEmpty()) return null;
		
		return s;
	}
	
	/*
	 * Overwriting toString for debug purposes 
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		
		for (Entry<String, BitSet> entry : invertedIndex.entrySet())
			buffer.append(entry.getKey() + " -> " + entry.getValue() + '\n');
		
		return buffer.toString();
	}
}
