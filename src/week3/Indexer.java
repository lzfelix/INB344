package week3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Indexer {
	private Map<String, BitSet> invertedIndex;
	private final int INITIAL_ID_COUNTER = 1;
	
	private int docId;
	
	public Indexer(String path) throws IOException {
		invertedIndex = new HashMap<String, BitSet>();
		docId = INITIAL_ID_COUNTER;
		
		Files.list(new File(path).toPath()).forEach(filePath -> {
			String purgedFile = "";
			
			// skip hidden files
			if (filePath.getFileName().toString().indexOf(".") == 0) return;
			
			// fail silently
			try {
				purgedFile = week2.Tokeniser.purgeFile(filePath, false);
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
	
	public String simpleQuery(String word) throws IndexerException {
		word = word.toLowerCase();
		if (word.length() == 0)
			throw new IndexerException("Invalid empty query.");

		if (invertedIndex.containsKey(word))
			return invertedIndex.get(word).toString();
		return null;
	}
	
	public String andQuery(String[] words) {
		BitSet result = null;
		
		for (int i = 0; i < words.length; i++) {
			words[i] = words[i].toLowerCase();
			
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
		
		if (result == null) return null;
		
		return result.toString();
	}
	
	public String orQuery(String[] words) {
		BitSet result = null;
		
		for (int i = 0; i < words.length; i++) {
			words[i] = words[i].toLowerCase();
			
			// 0 (OR) X = X; X = anything
			if (invertedIndex.containsKey(words[i])) {
				if (result == null)
					result = (BitSet) invertedIndex.get(words[i]).clone();
				else
					result.and(invertedIndex.get(words[i]));
			}
		}
		
		if (result == null) return null;
		
		return result.toString();
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		
		for (Entry<String, BitSet> entry : invertedIndex.entrySet())
			buffer.append(entry.getKey() + " -> " + entry.getValue() + '\n');
		
		return buffer.toString();
	}
}
