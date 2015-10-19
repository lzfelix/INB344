package queryExpansion;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

/**
 * Performs Query Expansion based on corpus's documents
 * @author Luiz Felix
 */
public class EMIMQueryExpansion {
	private Index index;
	private CollectionStatistics statistics;
	private Lexicon<String> lexicon;
	
	public final int RARE_TRESHOLD = 100;
	public final int POPULAR_THRESHOLD = 50000;
	private final double EPSILON = 1e-10;
	
	private int rareThreshold;
	private int popularThreshold;
	
	/**
	 * Creates a new Query Expansion object
	 */
	public EMIMQueryExpansion(Index index) {
		this.index = index;
		this.statistics = index.getCollectionStatistics();
		this.lexicon = index.getLexicon();
		
		this.rareThreshold = this.RARE_TRESHOLD;
		this.popularThreshold = this.POPULAR_THRESHOLD;
	}

	/**
	 * Expands a query term that has frequency bigger than <code>RARE_THRESHOLD</code> and appears on at
	 * least <code>RARE_THRESHOLD</code> documents but also has corpus frequency < <code>POPULAR_THRESHOLD</code>.
	 * Words with numbers aren't expanded either.
	 * @param word The query term to be expanded
	 * @return A list with the top <code>maxTranslations</code> likely translations for <code>word<code>. If it's not
	 * possible to translate this word, then this list is empty.
	 * @throws IOException if there's an IO fault while reading the index files.
	 */
	public List<String> getTranslations(String word, int maxTranslations) throws IOException {		
		LexiconEntry qLexiconEntry = lexicon.getLexiconEntry(word);
		if (qLexiconEntry == null) 
			return new LinkedList<String>();
		
		// it's double to avoid integer division
		double collectionSize = statistics.getNumberOfDocuments();
		
		// won't expand neither rare, too popular or words with digits
		if (!isValidToExpand(word, qLexiconEntry))
			return new LinkedList<String>();
		
		HashMap<String, Double> tempMutualInfoTable = new HashMap<>();
		
		//stores translating terms alphabetically in descending order according to its relative mutual information
		TreeMultimap<Double, String> normalizedTranslations = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		
		//set with all documents that contain word
		Set<Integer> docsWithW = getDocumentWith(word);
		
		int amountOfDocsWithW = qLexiconEntry.getDocumentFrequency();
		
		// probability of the word occurring or not on the corpus
		double p_w_1 = (amountOfDocsWithW) / collectionSize;
		double p_w_0 = 1 - p_w_1;
		
		// used to normalise
		double totalMutualInformation = 0;
		
		//finding the mutual information between current word (w) and all others on documents that contains w.
		for (Entry<String, LexiconEntry> entry : lexicon) {
			String wordU = entry.getKey();
			
			if (!isValidToExpand(wordU, entry.getValue())) continue;
			
			Set<Integer> docsWithU = getDocumentWith(wordU);
			int amountOfDocsWithU = docsWithU.size();
			
			// Performing intersection in place. It's modifying the docsWithU set. This saves memory and time
			docsWithU.retainAll(docsWithW);
			
			double coocurrenceFreq = docsWithU.size();
			
			// probabilities of U occuring (and not) over the collection)
			double p_u_1 = amountOfDocsWithU / collectionSize;
			double p_u_0 = 1 - p_u_1;
			
			// calculating componentes of mutual information -- If you aren't sure about this, just draw a Venn Diagram
			double p_w1_u1 = coocurrenceFreq / collectionSize;
			double p_w1_u0 = (amountOfDocsWithW - coocurrenceFreq) / collectionSize;
			double p_w0_u1 = (amountOfDocsWithU - coocurrenceFreq) / collectionSize;
			double p_w0_u0 = 1 - p_w1_u1 - p_w1_u0 - p_w0_u1; 
		
			// now, if any of these guys is 0, the MI formula is going to break, as Lg(0) = NaN. So I take some extra care here
			// Could also do some Laplace (NOT Laplacian) Smoothing
			double I_w_u = p_w0_u0 * doSafeLog(p_w0_u0, p_w_0, p_u_0) +
							p_w1_u0 * doSafeLog(p_w1_u0, p_w_1, p_u_0) +
							p_w0_u1 * doSafeLog(p_w0_u1, p_w_0, p_u_1) +
							p_w1_u1 * doSafeLog(p_w1_u1, p_w_1, p_u_1);
			
			totalMutualInformation += I_w_u;
			tempMutualInfoTable.put(wordU, I_w_u);			
		}
		
		// update the translation table and add to the output set
		for (String w : tempMutualInfoTable.keySet()) {
			double I_w_u = tempMutualInfoTable.get(w);
			normalizedTranslations.put(I_w_u / totalMutualInformation, w);
		}
		
		// getting top terms to return
		List<String> translations = new LinkedList<>();
		
		int amountOfTranslations = 0;
		for (Double I_w_u : normalizedTranslations.keySet()) {
			
			for (String translation : normalizedTranslations.get(I_w_u)) {
				// avoid self-translation
				if (translation.equals(word)) continue;
				
				translations.add(translation);
				if (++amountOfTranslations >= maxTranslations) break;
			}
			
			if (amountOfTranslations > maxTranslations) break;
		}
		
		return translations;
	}
	
	/**
	 * This function is the same as Log(a/(b*c)), used on the Mutual Information calculation.
	 * Because of the log and division, some ensurances must be made:
	 * - b*c != 0
	 * - a/(b*c) > 0
	 * So this function first multiplies b and c, if this is lesser than 1e-10, then this function
	 * returns 0. Otherwise it'll try to perform the following division, if the result is lesser than 
	 * 1e-10, then it returns 0.
	 * @param a 
	 * @param b
	 * @param c
	 * @return 0 if b*c < 1e-10 or if a/(b*c) < 1e-10, otherwise the standard result Lg2(a/(b*c)).
	 */
	private double doSafeLog(double a, double b, double c) {
		double divisor = b * c;
		if (Math.abs(divisor) < EPSILON) return 0;
		
		divisor = a / divisor;
		if (divisor < EPSILON) return 0;
		
		return WeightingModelLibrary.log(divisor);
	}
	
	/**
	 * Returns a set of the documents on the index which contain <code>word</code>.
	 * If a stopped word is used, an empty set is returned. 
	 * @param word The word to be queried for.
	 * @return A hashed set with the ID of the documents which contain <code>word</code>.
	 * @throws IOException If there's an IO fault while reading the index 
	 */
	private Set<Integer> getDocumentWith(String word) throws IOException {
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
	
	/**
	 * This function is used to decide if it's worth to expand a certain word. It'll return false if either:
	 *  - the amount of occurrences of this word on the corpus is smaller than <code>RARE_TRESHOLD</code> or bigger than <code>POPULAR_TRESHOLD</code> 
	 *  - the amount of documents containing this word is lesser than <code>RARE_TRESHOLD</code>
	 *  - the word doesn't exist on the lexicon (<code>entry</code> is null)
	 * @param word The word to be verified if it's worth expanding
	 * @param entry This word's lexicon entry on Terrier's Index.
	 * @return <code>true</code> if expanding the word seems worth, <code>false</code> otherwise
	 */
	private boolean isValidToExpand(String word, LexiconEntry entry) {
		if (entry == null) return false;
		
		int frequency = entry.getFrequency();
		int corpusFrequency = entry.getDocumentFrequency();
		
		return !(frequency < rareThreshold || corpusFrequency < rareThreshold || corpusFrequency > popularThreshold || word.matches(".*\\d+.*"));
	}

	/* Getters and setters boring part */
	
	/**
	 * Sets both threshold according to the parameters. If either of these are smaller than 0, or if
	 * <code>rare</code> > <code>popular</code>, then nothing happens.
	 * @param rare the new rare threshold
	 * @param popular the new popular threshold
	 */
	public void setThresholds(int rare, int popular) {
		if (rare > popular) return;
		
		if (rare >= 0)
			this.rareThreshold = rare;
		
		if (popular >= 0)
			this.popularThreshold = popular;
	}

	/**
	 * Sets the thresholds based on a percentage of the amount of tokens on the collection.
	 * If a parameter is smaller than 1 or bigger than its internal parameter isn't updated.  
	 * @param rare the new rare threshold will be set as <code>rare</code> * amount of words on the collection
	 * @param popular the new popular threshold will be set as <code>popular</code> * amount of words on the collection
	 */
	public void setThresholds(float rare, float popular) {
		if (rare > popular) return;
		
		if (rare <= 1 && rare >= 0) 
			this.rareThreshold = (int)Math.floor(statistics.getNumberOfTokens() * rare);
		
		if (popular <= 1 && popular >= 0) 
			this.popularThreshold = (int)Math.floor(statistics.getNumberOfTokens() * popular);
	}
	
	/**
	 * @return Returns the current rareThreshold
	 */
	public int getRareThreshold() { return this.rareThreshold; }
	
	/**
	 * @return Returns the current popularThreshold
	 */
	public int getPopularThreshold() { return this.popularThreshold; }
}
