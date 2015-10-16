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
	private static String STD_INDEX_ALIAS = "data";
	
	private static double INITIAL_SCORE = -1000;
	private static double MU = 3000;	//a random guess
	
	// used to decide if a word can be expanded via translation or not
	private static int LOWER_BOUND = 10;
	private static int UPPER_BOUND = 5000;
	
	private static double EPSILON = 1e-10;
	
	private static boolean __DEBUG_TRANSLATION_CATALOG = true;
	
	private static String QUERIES_PATH = "queries.txt";
	
	private Index index;
	
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
	
	/* Removes all spaecs and punctuation characters from input queries */
	private String purgeQuery(String originalQuery) {
		return originalQuery.replaceAll("-", " ").replaceAll("\\p{Punct}", "");
	}
	
	/**
	 * Returns a set of the documents on the index which contain <code>word</code>.
	 * If a stopped word is used, an empty set is returned. 
	 * @param word The word to be queried for.
	 * @return A hashed set with the ID of the documents which contain <code>word</code>.
	 * @throws IOException If there's an IO fault while reading the index 
	 */
	public Set<Integer> getDocumentWith(String word) throws IOException {
		Set<Integer> relevantDocuments = new HashSet<>();
		
		LexiconEntry entry = index.getLexicon().getLexiconEntry(word);
		if (entry == null)
			return relevantDocuments;
		
		//get a list with all documents that contain word
		IterablePosting iterator = index.getInvertedIndex().getPostings(entry);
		while (iterator.next() != IterablePosting.EOL)
			relevantDocuments.add(iterator.getId());
		
		return relevantDocuments;
	}
	
	/* Returns false either if the amount of word occurrences exceeds the bounds or if it contains a number */
	private boolean isValidToExpand(String word, LexiconEntry entry) {
		int frequency = entry.getFrequency();
		int corpusFrequency = entry.getDocumentFrequency();
		
		return (frequency < LOWER_BOUND || corpusFrequency < LOWER_BOUND || corpusFrequency > UPPER_BOUND || word.matches(".*\\d+.*"));
	}
	
	private double mutualInformation(Set documentsWithW, int amountOfDocsWithW, double collectionSize, 
									 double p_w_1, double p_w_0, String wordU, LexiconEntry lexU) throws IOException {
		
		if (isValidToExpand(wordU, lexU)) return 0;
		
		// finding the intersection
		Set<Integer> documentsWithU = getDocumentWith(wordU);
		Set<Integer> intersectedDoc = new HashSet<>(documentsWithU);
		intersectedDoc.retainAll(documentsWithW);
		
		double amountOfCoocurrences = intersectedDoc.size();
		
		if (amountOfCoocurrences == 0)
			return 0;
		
		LexiconEntry lexiconsWithU = lexU;
		
		double p_u_1 = lexiconsWithU.getDocumentFrequency()/collectionSize;
		double p_u_0 = 1 - p_u_1;
		
		//calculating the arguments of information gain
		double p_w1_u1 = amountOfCoocurrences / collectionSize;
		
		double p_w1_u0 = (amountOfDocsWithW - amountOfCoocurrences) / collectionSize;
		double p_w0_u1 = (lexiconsWithU.getDocumentFrequency() - amountOfCoocurrences) / collectionSize;
		double p_w0_u0 = 1 - p_w1_u1 - p_w1_u0 - p_w0_u1;
		
		//Swear that I didn't understand this part =(((((
		// \/ -> 4.2499047338214075E-6
		double I_w_u = p_w0_u0 * WeightingModelLibrary.log( p_w0_u0 / (p_w_0 * p_u_0) ) 
				+ p_w1_u0 * WeightingModelLibrary.log( p_w1_u0 / (p_w_1 * p_u_0) )
				+ p_w0_u1 * WeightingModelLibrary.log( p_w0_u1 / (p_w_0 * p_u_1) )
				+ p_w1_u1 * WeightingModelLibrary.log( p_w1_u1 / (p_w_1 * p_u_1) );
		
		return I_w_u;
	}
	
	private boolean isBiggerThanZero(double value) {
		return Math.abs(value) < EPSILON;
	}
	
	private HashMap<String, Double> getTranslations(String word, int maxTranslations) throws IOException {
		Lexicon<String> dictionary = index.getLexicon();
		double collectionSize = index.getCollectionStatistics().getNumberOfDocuments();
		
		//stores translating terms alphabetically in descending order according to its relative mutual information
		TreeMultimap invertedTranslations = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		
		//set with all documents that contain word
		Set<Integer> documentsWithW = getDocumentWith(word);
		
		int documentFrequencyW = dictionary.getLexiconEntry(word).getDocumentFrequency();

		double p_w_1 = ((double)documentFrequencyW) / collectionSize;
		double p_w_0 = 1 - p_w_1;
		
		//finding the mutual information between current word (w) and all others)
		for (Entry<String, LexiconEntry> entry : dictionary) {
			String wordU = entry.getKey();
			
			if (isValidToExpand(wordU, entry.getValue())) continue;
			
//			int k = 0;
//			if (wordU.equals("isaak"))
//				k = 10;
//			
//			
//			// finding the intersection
//			Set<Integer> documentsWithU = getDocumentWith(wordU);
//			Set<Integer> intersectedDoc = new HashSet<>(documentsWithU);
//			intersectedDoc.retainAll(documentsWithW);
//			
//			double amountOfCoocurrences = intersectedDoc.size();
//			
//			//ask Guido about empty intersection
//			if (amountOfCoocurrences == 0) continue;
//			
//			LexiconEntry lexiconsWithU = entry.getValue();
//			
//			double p_u_1 = lexiconsWithU.getDocumentFrequency()/collectionSize;
//			double p_u_0 = 1 - p_u_1;
//			
//			//calculating the arguments of information gain
//			double p_w1_u1 = amountOfCoocurrences / collectionSize;
//			
//			double p_w1_u0 = (lexiconsWithW.getDocumentFrequency() - amountOfCoocurrences) / collectionSize;
//			double p_w0_u1 = (lexiconsWithU.getDocumentFrequency() - amountOfCoocurrences) / collectionSize;
//			double p_w0_u0 = 1 - p_w1_u1 - p_w1_u0 - p_w0_u1;
//			
//			//Swear that I didn't understand this part =(((((
//			// \/ -> 4.2499047338214075E-6
//			double I_w_u = p_w0_u0 * WeightingModelLibrary.log( p_w0_u0 / (p_w_0 * p_u_0) ) 
//					+ p_w1_u0 * WeightingModelLibrary.log( p_w1_u0 / (p_w_1 * p_u_0) )
//					+ p_w0_u1 * WeightingModelLibrary.log( p_w0_u1 / (p_w_0 * p_u_1) )
//					+ p_w1_u1 * WeightingModelLibrary.log( p_w1_u1 / (p_w_1 * p_u_1) );
//			
//			//well... this is the absolute information gain, let's find the relative one
//			double totalInformationGain = 0;
			
			double I_w_u = mutualInformation(documentsWithW, documentFrequencyW, collectionSize, p_w_1, p_w_0, wordU, entry.getValue());
			
			if (__DEBUG_TRANSLATION_CATALOG)
				System.out.println("Translating " + word + " into " + wordU + " the MI is: " + I_w_u);
			
			if (I_w_u == 0) continue;
			
			Set<Integer> documentsWithU = getDocumentWith(wordU);
			int documentFrequencyU = dictionary.getLexiconEntry(word).getDocumentFrequency();
			double p_u_1 = ((double)documentFrequencyU) / collectionSize;
			double p_u_0 = 1 - p_u_1;
			
			double totalMutualInformation = 0;
			
			for (Entry<String, LexiconEntry> entry2 : dictionary) {
				double newInfo = mutualInformation(documentsWithU, documentFrequencyU, 
															collectionSize, p_w_1, p_w_0, wordU, entry2.getValue());
				
				totalMutualInformation += newInfo;
				if (Double.isNaN(totalMutualInformation))
					newInfo = 0;
				
				System.out.println(">>>" + totalMutualInformation);
			}
			
			double relativeMI = I_w_u / totalMutualInformation;
			
			if (__DEBUG_TRANSLATION_CATALOG) {
				System.out.println("------------------\nMutual information between "+word+" and "+wordU+" = " + relativeMI);
				System.out.println("------------------\n");
			}
		}
		
		
		return null;
	}
	
	
	public void performQueries() {
//		trecoutput = new TRECDocnoOutputFormat(index);
		DirichletLM dir_lm = new DirichletLM();
		
		// pair <query_id, query_text>
		for (Entry<String, String> e : queries.entrySet()) {
			Request req = new Request();
			
			// storing original data
			req.setOriginalQuery(e.getValue());
			req.setQueryID(e.getKey());
			
			
		}
	}
	
	private void somethingMagical(String[] queryTerms) {
		// this array stores the score of each document on the collection
		double[] log_probs = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		
		//does it really have to be a negative number??
		Arrays.fill(log_probs, INITIAL_SCORE);
		
		// store some index pointers
		PostingIndex<?> di = index.getDirectIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		
		// the collection dictionary, along with some numeric data
		Lexicon<String> dictionary = index.getLexicon();
		
		double amountOfTokens = index.getCollectionStatistics().getNumberOfTokens();
		
		for (String term : queryTerms) {			
			LexiconEntry termEntry = dictionary.getLexiconEntry(term);
			if (termEntry == null) {
				System.err.println("Term " + term + " can't be expanded, skipping.");
				continue;
			}
			
			// talk with Guido about translation thresholds
			
			//put translation invokation here
				
		}
	}
	
	
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
//			System.out.println(terrier.getTranslations("knife", 100));
		}
		catch (IOException e) {
			System.out.println("Error while reading the queries: " + e.getMessage());
			e.printStackTrace();
		}
		
		DJM languageModel = DJM.getInstance();
		
		try {
			languageModel.performQueries("output.txt", terrier.queries, terrier.index);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("x");
	}
}
