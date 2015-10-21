package dataStructures;

/**
 * Workout class, as Java doesn't have Pair by default. This is used to
 * store related words frequency in a unique way and order them.
 * @author Luiz Ribeiro
 */
public class WordStatistics implements Comparable<WordStatistics>{
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