package terrier;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.terrier.matching.models.DirichletLM;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.querying.Request;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.outputformat.TRECDocnoOutputFormat;
import org.terrier.structures.postings.IterablePosting;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

public class ModifiedTerrier {
	private static String TERRIER_HOME = "/Users/luiz/Desktop/SET_A/terrier";
	private static String INDEX_PATH = "/Users/luiz/Desktop/SET/terrier-4.0/processing/newIndex";
	private static String QUERIES_PATH = "tools/queries.txt";
	private static String STD_INDEX_ALIAS = "data";
	
	private Index index;
	private EMIMQueryExpansion qe;
	
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
		
		qe = new EMIMQueryExpansion(index);
	}
	
	/**
	 * Reads all TREC-queries into memory. The query file must be in the form:
	 * <query_id> <query_text> \n
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
			String query = purgeQuery(newLine.substring(spaceIndex + 1));
			
			queries.put(qID, query);
		}
		
		queryBuffer.close();
	}
	
	/* Removes all spaces and punctuation characters from input queries */
	private String purgeQuery(String originalQuery) {
		return originalQuery.replaceAll("-", " ").replaceAll("\\p{Punct}", "");
	}
	
	private void expandQuery(String query) {
		
	}
	
	
	public static void main(String args[]) {
		ModifiedTerrier terrier = null;
		
		try {
			terrier = new ModifiedTerrier(TERRIER_HOME, INDEX_PATH);
		} catch (Exception e) {
			System.out.println("Exception caught: " + e.getMessage());
			e.printStackTrace();
		} 
		
		EMIMQueryExpansion qe = new EMIMQueryExpansion(terrier.index);
		
		try {
//			terrier.readQueries(QUERIES_PATH);
			qe.setThresholds(3000, 0);
			qe.setThresholds(-1, 0.7f);
			
			System.out.println(qe.getTranslations("plane", 10));
		}
		catch (IOException e) {
			System.out.println("Error while reading the queries: " + e.getMessage());
			e.printStackTrace();
		}
		
//		DJM languageModel = DJM.getInstance();
//		
//		try {
//			languageModel.performQueries("tools/output.txt", terrier.queries, terrier.index);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		Tunner t = new Tunner(terrier.index);
		
//		try {
//			System.out.println(t.tuneMu(2.1e5, 1));
//		} catch (IOException ioe) {
//			ioe.printStackTrace();
//		}
		
		
		
		System.out.println("Done.");
	}
}
