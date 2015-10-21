package dataStructures;

/**
 * A general pair class.
 * @author Luiz Felix
 *
 * @param <T> The type holded by the pair
 */
public class Pair<T> {
	public T first, second;
	
	/**
	 * Convenience constructor for already initializing the pair.
	 * @param first The first value held by the pair.
	 * @param second The second value held by the pair.
	 */
	public Pair (T first, T second) {
		this.first = first;
		this.second = second;
	}
	
	/**
	 * Constructs a pair, but leaves the fields uninitialized.
	 */
	public Pair() { }
}
