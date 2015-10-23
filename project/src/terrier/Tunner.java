package terrier;

import java.io.IOException;
import java.util.Map.Entry;

import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;

import dataStructures.Pair;

/**
 * This class determined the best mu for the collection using the formulas described
 * on the reference paper.
 * @author Luiz Felix
 */
public class Tunner {
	// the maximum of iterations performed by Newton's Method
	public final int MAX_ITERATIONS = 40000;
	
	// the minimum precision on the derivative to consider the maxima point.
	public final double EPSILON = 1e-4;
	
	private Index index;
	private PostingIndex<?> invertedIndex;
	private Lexicon<String> vocabulary;
	
	/**
	 * Creates a Tunner object, which is able to fit mu and lambda according to
	 * C. Zhai and J. Lafferty Paper
	 * 
	 * @param index The terrier Index object
	 */
	public Tunner(Index index) {
		this.index = index;
	
		invertedIndex = index.getInvertedIndex();
		vocabulary = index.getLexicon();
	}
	
	/**
	 * This function finds, at the same time both g and g', used on optimisation. I tried to further improve
	 * this function but without much success. This implementation differs from the paper as the summarizations
	 * were interchanged to decrease the amount of iterations and to use Terrier's inverted index.
	 * @param mu The mu parameter. According to the authors, 1 is a good initial point.
	 * @param sampling How much of the corpus vocabulary is going to be considered while tuning.
	 * @return A pair containing g(mu) on the first element and g'(mu) on the second element. 
	 * @throws IOException If there's an I/O fault while reading the index from the disk.
	 */
	private Pair<Double> calculateG_Gprime(double mu, double sampling) throws IOException {
		int vocabularySize = (int)Math.ceil(index.getCollectionStatistics().getNumberOfUniqueTerms() * sampling);
		
		CollectionStatistics statistics = index.getCollectionStatistics();
		
		double g = 0;
		double gPrime = 0;
		
		// loops are the inverse from the paper
		for (int w = 0; w < vocabularySize; w++) {
			Entry<String, LexiconEntry> currentWord = vocabulary.getIthLexiconEntry(w);
			
			double p_w_c = currentWord.getValue().getNumberOfEntries() / (double)statistics.getNumberOfTokens(); 
			double A, B, C, D;
			
			// this is the outer loop in the paper
			IterablePosting postingsList = invertedIndex.getPostings(currentWord.getValue());
			while (postingsList.next() != IterablePosting.EOL) {
				
				double c_w_d = postingsList.getFrequency();
				double d_len = postingsList.getDocumentLength() - 1;	//the paper used doc_len - 1
				
				A = c_w_d * (d_len * p_w_c - c_w_d + 1);
				B = (d_len + mu) * (c_w_d - 1 + mu * p_w_c);
				
				C = A * A;
				D = B * B;
				
				g += A / B;
				gPrime -= C / D;
			}
		}
		
		return new Pair<Double>(g, gPrime);	
	}
	
		
	/**
	 * Tunes the mu parameter, used on Dirichlet or DJM Language Model based on the
	 * corpus. This is done through Newton's method. Since the method always converges, according
	 * to the authors (and it's true, as this is a quadratic function). This method returns mu if 
	 * either: the modulus of the first derivative is smaller than <code>EPSILON</code>,
	 * or the amount of iterations is bigger than <code>MAX_ITERATIONS</code>. Relative difference on
	 * the update value is ignored.
	 * @param mu The initial value of mu. According to the paper authors 1 is a good initial point.
	 * @param sampling The percentage of the corpus vocabulary that is going to be taken in account 
	 * when tunning mu. This value must be ]0,1[.
	 * @return The tuned value of mu.
	 * @throws IOException If there's an I/O fault while reading the index data.
	 */
	public double tuneMu(double mu, double sampling) throws IOException {
		int counter = 0;
		Pair<Double> gAndPrime;
		
		do {
			gAndPrime = calculateG_Gprime(mu, sampling);
			
//			System.out.println("g(mu) = " + gAndPrime.first);
			
			mu = mu - gAndPrime.first / gAndPrime.second;
			
//			System.out.println("Iteration " + counter + " mu = " + mu);
		} while (++counter < MAX_ITERATIONS && Math.abs(gAndPrime.first) > EPSILON);
		
		return mu;
	}	

}
