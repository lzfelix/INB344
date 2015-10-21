package tests;

import terrier.ModifiedTerrier;

/**
 * This class allows you to use the functionalities developed on top of Terrier.
 * Use the private constants and terrier methods to achieve retrieval.
 * @author Luiz Felix
 *
 */
public class UseTerrier {
	/* path to Terrier */
	private static String TERRIER_HOME = "/Users/luiz/Desktop/SET_A/terrier";
	
	/* path to the already processed index folder */
	private static String INDEX_PATH = "/Users/luiz/Desktop/SET/terrier-4.0/corpus/clef";
	
	/* where to save the querying results -- if the case */
	private static String RESULTS_FILE_PATH = "tools/output.txt";
	
	/* where to save the expanded queries -- if the case */
	private static String EXPANDED_QUERIES_PATH = "tools/clef_lnqueries.txt";
	
	private static String QUERIES_PATH = "tools/clef_queries.txt";
	
	public static void main(String args[]) {
		ModifiedTerrier terrier = null;
		
		try {
			terrier = new ModifiedTerrier(TERRIER_HOME, INDEX_PATH);
		} catch (Exception e) {
			System.out.println("Exception caught: " + e.getMessage());
			e.printStackTrace();
		} 
		
		try {
			/* Tunning mu. For clef the best mu is 303 */
//			terrier.tuneMu(1f);
//			System.out.println(terrier.getInternalMu());
			
			/* Reding queries from disk */
			terrier.readQueries(QUERIES_PATH);
			
			/* Now you can perform retrieval with the original queries, expand them and the perform retrieval 
			 * or just save the expanded queries */
//			terrier.writeExpandedQueries(EXPANDED_QUERIES_PATH, false);
//			terrier.performQueriesWithStagedExpansion(RESULTS_FILE_PATH);
			terrier.performQueriesWithoutExpansion(RESULTS_FILE_PATH, true, true);
		}
		catch (Exception e) {
			System.out.println("Error while reading the queries: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
