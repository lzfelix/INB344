package tests;

import terrier.ModifiedTerrier;

/**
 * !!! Add this class' description. !!!
 * @author Luiz Felix
 *
 */
public class UseTerrier {
	private static String TERRIER_HOME = "/Users/luiz/Desktop/SET_A/terrier";
	private static String INDEX_PATH = "/Users/luiz/Desktop/SET/terrier-4.0/processing/newIndex";
//	private static String INDEX_PATH = "/Users/luiz/Desktop/SET/terrier-4.0/corpus/clef";
	private static String QUERIES_PATH = "tools/expanded_queries.txt";
//	private static String QUERIES_PATH = "tools/clef_queries.txt";
	private static String EXPANDED_QUERIES_PATH = "tools/clef_expanded_queries2.txt";
	private static String RESULTS_FILE_PATH = "tools/output.txt";
	
	public static void main(String args[]) {
		ModifiedTerrier terrier = null;
		
		try {
			terrier = new ModifiedTerrier(TERRIER_HOME, INDEX_PATH);
		} catch (Exception e) {
			System.out.println("Exception caught: " + e.getMessage());
			e.printStackTrace();
		} 
		
		try {			
			terrier.readQueries(QUERIES_PATH);
//			terrier.writeExpandedQueries(EXPANDED_QUERIES_PATH);
//			terrier.performQueriesWithStagedExpansion("tools/wololo.txt");
//			terrier.performQueriesWithoutExpansion(RESULTS_FILE_PATH);
			
//			terrier.performQueriesWithStagedExpansion(RESULTS_FILE_PATH);
		}
		catch (Exception e) {
			System.out.println("Error while reading the queries: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
