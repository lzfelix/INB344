package dataStructures;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * A class that wraps a standard HashMap<String, Integer>, but after <code>invertDictionary</code> is 
 * invoked, the table is updated on the form HashMap<Integer, String>. Both String and Integer must be
 * unique.
 * 
 * This class can be improved
 * 
 * @author Luiz Ribeiro
 */
public class TwoSidedHash {
	private HashMap<String, Integer> dictionary;
	private HashMap<Integer, String> invertedDictionary;
	
	private int currentID;
	
	public TwoSidedHash() {
		dictionary = new HashMap<>();
		currentID = 0;
	}
	
	/**
	 * If the initial dictionary already contains this key, return its identifier, otherwise append it and
	 * then return its ID. If this fuction is called after <code>invertDictionary()</code> it simply returns
	 * -1 with no side-effects.
	 * @param The new key to insert into the dictionary
	 * @return The unique ID of such key
	 */
	public int forceDirectGet(String key) {
		if (dictionary == null) return -1;
		
		//dict already contains the key
		if (dictionary.containsKey(key))
			return dictionary.get(key);
		
		dictionary.put(key, currentID);
		return currentID++;
	}
	
	/**
	 * Inverses the mapping String -> Integer to Integer -> String and DESTROYS the
	 * initial dictionary.
	 */
	public void invertDictionary() {
		invertedDictionary = new HashMap<Integer, String>();
		
		for (Entry<String, Integer> e : dictionary.entrySet()) {
			invertedDictionary.put(e.getValue(), e.getKey());
		}
		
		//forcing the JVM to free the initial dictionary
		dictionary = null;
	}
	
	/**
	 * Queries the inverted index based on the ID.
	 * @param ID the ID which maps to a String
	 * @return the String that corresponds to ID, or <code>null<code> if either the dictionary wasn't
	 * reversed or if the element doesn't exists.
	 */
	public String getInvertedKey(int ID) {
		if (invertedDictionary == null)
			return null;
		
		return invertedDictionary.get(ID);
	}
}
