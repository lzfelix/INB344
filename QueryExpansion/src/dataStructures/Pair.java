package dataStructures;
/**
 * A immutable pair of integers, where y must be different of x, or null 
 * @author Luiz Ribeiro
 */
public class Pair {
	private Integer x;
	private Integer y;
	
	/**
	 * Creates a new pair. If <code>x</code> == <code>y</code>, then
	 * <code>y</code> = <code>null</code>.
	 * @param x The first number to be stored on the pair
	 * @param y The second number to be stored on the pair
	 */
	public Pair(int x, int y) {
		this.x = x;
		this.y = (x == y) ? null : y; 
	}
	
	/** @return The stored X value */
	public int getX() { return this.x; }
	
	/** @return <code>null</code> if the y parameter was equals to x on the constructor */
	public Integer getY() { return this.y; }
}
