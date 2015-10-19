package queryExpansion;

/**
 * A class that represents a Query Expansion pipeline. It's used just to generalize
 * how the Language Models use Query Expansion
 * @author Luiz Felix
 */
public interface PipelineInterface {

	/**
	 * Performs the query expansion pipeline
	 * @param query The original query
	 * @param maxExpansions The maximum amount of terms that can be added on the original query
	 * @return The expanded query
	 * @throws Exception an method-dependent exception.
	 */
	public String expandQuery(String query, int maxExpansions) throws Exception;
}
