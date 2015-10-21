package terrier;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.terrier.structures.Index;

import queryExpansion.StagedQueryExpansion;

/**
 * This class uses Terrier's functionalities to perform IR over a corpus.
 * @author Luiz Felix
 */
public class ModifiedTerrier {
	/* Path to the modified Consumer Health Vocabulary file */
	private static String CHV_PATH = "./CHV/CHV_modified.txt";
	
	/* Internal terrier parameter used as an alias for the index property */
	private static String STD_INDEX_ALIAS = "data";
	
	private Index index;
	private StagedQueryExpansion queryExpansion;
	
	// can be improved
	private LinkedHashMap<String, String> queries;
	
	/**
	 * Installs terrier.home variable on the environment and load index file. 
	 * @param terrierHome The terrier home path
	 * @param indexPath Path to the previously created index
	 * @throws Exception If the indexPath points to an invalid index.
	 */
	public ModifiedTerrier(String terrierHome, String indexPath) throws Exception {
		System.setProperty("terrier.home", terrierHome);
		index = Index.createIndex(indexPath, STD_INDEX_ALIAS);
		
		if (index == null)
			throw new Exception("Index is null, probably the path is invalid.");
		
		queryExpansion = new StagedQueryExpansion(index, CHV_PATH);
	}
	
	/**
	 * Reads all TREC-queries into memory. The queries are lower-cased, stop words are
	 * removed, only stemmed unique terms are kept. The query file must be in the form:
	 * <query_id> <query_text>\n
	 * @param queryPath The path to the query file one by line, with ID
	 * @throws IOException if either the file doesn't exist or there's an IO problem
	 */
	public void readQueries(String queryPath) throws IOException {
		BufferedReader queryBuffer = new BufferedReader(new FileReader(queryPath));
		String newLine;
		
		queries = new LinkedHashMap<String, String>();
		
		while ((newLine = queryBuffer.readLine()) != null) {
			//file ending line problem bypassing
			if (newLine.length() == 0) continue;
			
			int spaceIndex = newLine.indexOf(' ');
			
			//terrier stores qID as a String
			String qID = newLine.substring(0, spaceIndex);
			String query = newLine.substring(spaceIndex + 1);
			
			queries.put(qID, query);
		}
		
		queryBuffer.close();
	}
	
	/**
	 * Runs the read queries on the index using CHV-EMIM query expansion.
	 * @param outputFile Path to the output results file.
	 * @param useDocnoAsMeta if the document name should be used as metakey on the output file.
	 * @param doCHV if <code>true</code> CHV query expansion is performed.
	 * @throws Exception If the CHV file isn't found of if there's a fault while reading the index.
	 */
	public void performQueriesWithStagedExpansion(String outputFile, boolean useDocnoAsMeta, boolean doCHV) throws Exception {
		DJM.getInstance().performQueries(outputFile, queries, index, this.queryExpansion, useDocnoAsMeta, doCHV);
	}
	
	/**
	 * Runs the read queries on the index without expanding them.
	 * @param outputFile Path to the output results file.
	 * @param useDocnoAsMeta if the document name should be used as metakey on the output file.
	 * @param doCHV if <code>true</code> CHV query expansion is performed.
	 * @throws Exception If there's a fault while reading the index.
	 */
	public void performQueriesWithoutExpansion(String outputFile, boolean useDocnoAsMeta, boolean doCHV) throws Exception {
		DJM.getInstance().performQueries(outputFile, queries, index, null, useDocnoAsMeta, doCHV);
	}
	
	/**
	 * Expands the read queries using the staged expansion and creates a new file to be used with Terrier.
	 * @param outputFile The path to the new file to be created.
	 * @param CHVOnly if <code>true</code> only CHV query expansion is performed.
	 * @throws Exception If there's an I/O fault either while expanding the query or writing the new file to disk.
	 */
	public void writeExpandedQueries(String outputFile, boolean CHVOnly) throws Exception {
		DJM.getInstance().writeExpandedQueries(outputFile, queries, index, this.queryExpansion, CHVOnly);
	}
	
	/**
	 * Automatically set DJM's mu parameter using the Tunner class.
	 * @param sampling The percentage of the corpus that is going to be taken in account while tunning.
	 * More is slower, but gives better precision.
	 * @throws IOException If there's an I/O fault while reading the index file.
	 */
	public void tuneMu(double sampling) throws IOException {
		Tunner t = new Tunner(index);
		double mu = t.tuneMu(1.0f, sampling);
		
		DJM.getInstance().setMu(mu);
	}
	
	/**
	 * Returns the current value of mu used by DJM in order to score the documents.
	 * @return Returns the current value of mu used by DJM in order to score the documents.
	 */
	public double getInternalMu() {
		return DJM.getInstance().getMu();
	}	
}
