package terrier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.terrier.matching.ResultSet;
import org.terrier.matching.CollectionResultSet;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.querying.Request;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.outputformat.TRECDocnoOutputFormat;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.terms.PorterStemmer;
import org.terrier.terms.Stopwords;

import queryExpansion.PipelineInterface;

/**
 * A class for Dirichlet Jelinek-Mercer two-phase language model smoothing
 * @author Luiz Felix
 */
public class DJM {
	private static DJM instance = null;
	
	/* Used when outputing the file */
	public final String METHOD_NAME = "DJM";
	public final int MIN_EXPANSIONS = 5;
	public final int MAX_EXPANSIONS = 10;
	
	private PorterStemmer porterStemmer;
	private Stopwords stopwords;
	
	private double mu = 2500;
	private double lambda = 0.5;
	private int amountOfRetrievedDocuments = 1000;
	
	/* Private constructor for a singleton class */
	private DJM() {
		this.porterStemmer = new PorterStemmer();
		this.stopwords = new Stopwords(null);
	}

	/**
	 * Singleton class retrieval method
	 * @return the DJM instance.
	 */
	public static DJM getInstance() {
		if (instance == null)
			instance = new DJM();
		
		return instance;
	}
	
	
	/**
	 * Perform a query on the corpus using the DJM method.
	 * @param query The already prepared query (expanded, stopped, stemmed, lower cased containning only unique terms)
	 * @param index The Terrier index.
	 * @return A request containing the query results.
	 * @throws IOException If there's any I/O fault while reading the index.
	 */
	public Request queryCorpus(String query, Index index) throws IOException {
		CollectionStatistics statistics = index.getCollectionStatistics();
		
		int  D = statistics.getNumberOfDocuments();			//corpus size
		
		Lexicon<String> lexiconCollection = index.getLexicon();
		PostingIndex<?> invertedIndex = index.getInvertedIndex();
		
		// the scoring array
		double[] logP_d_q = new double[D];
		
		// preparing the query by removing repeated terms
		Set<String> preparedQuery = new HashSet<>();
		
		// add stemmed words only
		for (String q : query.split(" ")) {
			if (this.stopwords.isStopword(q)) continue;
			
			preparedQuery.add(porterStemmer.stem(q.toLowerCase()));
		}
		
		// applying the formula over every query term
		for (String queryTerm : preparedQuery) {			
			// this is the word entry on the collection lexicon
			LexiconEntry lexicon = lexiconCollection.getLexiconEntry(queryTerm);
			
			if (lexicon == null) continue;
			
			// this is the list of documents that contain this word
			IterablePosting postingsList = invertedIndex.getPostings(lexicon);
			
			// the background probability, never changes across documents
			double p_w_c = lexicon.getFrequency() / (double)statistics.getNumberOfTokens();
			
			// iterate over all of these documents to score them
			while (postingsList.next() != IterablePosting.EOL) {
				int docId = postingsList.getId();
				int docLen = postingsList.getDocumentLength();
				
				double c_w_d = postingsList.getFrequency();
				// double c_w_d = postingsList.getFrequency() / (double)postingsList.getDocumentLength();
				
//				double dirichlet = (c_w_d + this.mu * p_w_c) / (double)(docLen + this.mu);
				
				double  x = this.mu + lexicon.getFrequency() / (double)statistics.getNumberOfTokens();
				
				
				double dirichlet = WeightingModelLibrary.log(1 + lexicon.getFrequency() / x) + WeightingModelLibrary.log(this.mu / (double)(postingsList.getDocumentLength() + this.mu));
				
				// the later part is JM
				double formulaResult = (1 - this.lambda) * dirichlet + (this.lambda * p_w_c);
//				double formulaResult = dirichlet;
				
				// ask Guido about adding the LOG. Using - to boost the score up
				logP_d_q[docId] += formulaResult;
			}
		}	
		
//		double max = 0;
//		for (int i = 0; i < logP_d_q.length; i++) {
//			if (logP_d_q[i] > max)
//				max = logP_d_q[i];
//		}
//		
//		for (int i = 0; i < logP_d_q.length; i++) {
//			if (logP_d_q[i] > 1e-5)
//				logP_d_q[i] = (max - logP_d_q[i]) * 10;
//			else
//				logP_d_q[i] = 0;
//		}
		
		// storing the results on Terrier format and ordering document scoring
		ResultSet resultSet = new CollectionResultSet(statistics.getNumberOfDocuments());
		resultSet.initialise(logP_d_q);
		resultSet.sort();
		
		// storing data to generate the output file on the trec_eval format
		Request request = new Request();
		request.setResultSet(resultSet);
		
		return request;
	}

	/**
	 * The policy to expand a query. If it has more TheoryAnchor <code>MAX_EXPANSION</code> words, it allows
	 * <code>MIN_EXPANSION</code>, otherwise <code>MAX_EXPANSION</code> - (amount of words on the current query)
	 * expansions are allows.
	 * @param query the input query 
	 * @return the amount of words allows to expand <code>query</code>
	 */
	private int expandingFactor(String query) {
		int amountOfWords = query.split(" ").length;
		int toExpand = MAX_EXPANSIONS - amountOfWords;

		return Math.max(toExpand, MIN_EXPANSIONS);
	}
	
	/**
	 * Performs a set of queries, stored in a HashMap as <Key, Query> pair on the corpus. After each query is performed,
	 * its result is stored on <code>outputFile</code> path.
	 * @param outputFile The complete file path to where to write the file with the queries result.
	 * @param queries Contains the queries on pairs <Key, Query>. The queries must be space-separated and Stopwords and Porter-Stemmer
	 * is performed over them before issuing them to Terrier.
	 * @param index The Terrier index file
	 * @param PipelineInterface An object to the class used to perform query expansion. If this object is <code>null</code>, then no
	 * query expansion is performed
	 * @throws Exception if an IO fault happens
	 */
	public void performQueries(String outputFile, Map<String, String> queries, Index index, PipelineInterface queryExpansionPipeline) throws Exception {
		// this is responsible for organizing the ResultSets on the correct output format
		TRECDocnoOutputFormat outputFormatter = new TRECDocnoOutputFormat(index);
		
		PrintWriter pw = new PrintWriter(new File(outputFile));
		
		for (Entry<String, String> query : queries.entrySet()) {
			System.out.println("Processing query " + query.getKey());
			
			int amountOfExpansions = expandingFactor(query.getValue());
			
			String expandedQuery;
			if (queryExpansionPipeline != null)
				expandedQuery = queryExpansionPipeline.expandQuery(query.getValue(), amountOfExpansions);
			else
				expandedQuery = query.getValue();
			
			// query with the expanded query, but store the original query
			Request queryRequest = queryCorpus(expandedQuery, index);
			
			queryRequest.setOriginalQuery(query.getValue());
			queryRequest.setQueryID(query.getKey());
			
			outputFormatter.printResults(pw, queryRequest, METHOD_NAME, "Q0", amountOfRetrievedDocuments);
		}
		
		// releasing resources
		pw.flush();
		pw.close();
	}
	
	/**
	 * Just expands the query terms and writes into a new SingleLineTRECQUery formated file.
	 * @param outputFile The path to the new file to be created.
	 * @param queries A Map containing queryID -> queryText. The IDs are preserved on the new file.
	 * @param index The Terrier index object.
	 * @param queryExpansionPipeline The query expansion strategy, that must correspond to a class implementing PipelineInterfae
	 * @throws Exception If there's an I/O fault either while expanding the query or writing the new file to disk.
	 */
	public void writeExpandedQueries(String outputFile, Map<String, String> queries, Index index, PipelineInterface queryExpansionPipeline) throws Exception {
		FileWriter fw = new FileWriter(new File(outputFile));
		
		for (Entry<String, String> query : queries.entrySet()) {
			System.out.println("Expanding query " + query.getKey() + ": " + query.getValue());
			
			int amountOfExpansions = expandingFactor(query.getValue());
			String expandedQuery = queryExpansionPipeline.expandQuery(query.getValue(), amountOfExpansions);
			
			System.out.println("Got: " + expandedQuery);
			
			fw.write(query.getKey() + " " + expandedQuery + "\n");
		}
		
		fw.flush();
		fw.close();
	}

	/**
	 * @return Returns the current value of mu.
	 */
	public double getMu() { return this.mu; }
	
	/**
	 * Update the value of mu used on DJM method. If <code>mu</code> < 0, the previous value is kept.
	 * @param mu The new value of mu. It must be greater or equals than 0.
	 */
	public void setMu(double mu) { 
		if (mu < 0) return;
		this.mu = mu;
	}
	
	/**
	 * @return Returns the current value of lambda.
	 */
	public double getLambda() { return this.lambda; }
	
	/**
	 * Update the value of lambda used on DJM method. If <code>lambda</code> < 0, the previous value is kept.
	 * @param lambda The new value of lambda. It must be greater or equals than 0.
	 */
	public void setLambda(double lambda) {
		if (lambda < 0) return;
		this.lambda = lambda;
	}

	/**
	 * @return Retuns the amount of retrieved documents when a query is performed
	 */
	public int getAmountOfRetrievedDocuments() { return this.amountOfRetrievedDocuments; }
	
	/**
	 * @param newAmount The new amount of retrieved documents when a query is performed.
	 * The default value is 1000. If this value is lesser than 0, this method has no effect.
	 */
	public void setAmountOfRetrievedDocuments(int newAmount) {
		if (newAmount > 0)
			this.amountOfRetrievedDocuments = newAmount;
	}
}
