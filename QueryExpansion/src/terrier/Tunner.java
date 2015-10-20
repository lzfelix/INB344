package terrier;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map.Entry;

import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.Pointer;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.bit.InvertedIndex;
import org.terrier.structures.postings.IterablePosting;

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
	 * The g function described on the paper. This implementation differs from what is on the paper in:
	 *  - The loops were interchanged, as not all documents contains all words
	 *  - The fraction powers were manipulated to achieve more efficience using power properties.
	 * @param mu The initial mu value. According to the authors, 1 is a good starting point.
	 * @param samples How much of the corpus is going to be considered while tuning.
	 * @return g(mu)
	 * @throws IOException If there's an I/O fault while reading from the disk.
	 */
	private double g(double mu, double samples) throws IOException {
		long N = (long)Math.ceil(index.getDocumentIndex().getNumberOfDocuments() * samples);
		int vocabularySize = (int)Math.ceil(index.getCollectionStatistics().getNumberOfUniqueTerms() * samples);
		
		CollectionStatistics statistics = index.getCollectionStatistics();
		
		double result = 0;
		
		// loops are the inverse from the paper
		for (int w = 0; w < vocabularySize; w++) {
			Entry<String, LexiconEntry> currentWord = vocabulary.getIthLexiconEntry(w);
			
			double p_w_c = currentWord.getValue().getNumberOfEntries() / (double)statistics.getNumberOfTokens(); 
			double A, B;
			
			// this is the outer loop in the paper
			IterablePosting postingsList = invertedIndex.getPostings(currentWord.getValue());
			while (postingsList.next() != IterablePosting.EOL) {
				
				double c_w_d = postingsList.getFrequency();
				double d_len = postingsList.getDocumentLength() - 1;	//the paper used doc_len - 1
				
				A = c_w_d * (d_len * p_w_c - c_w_d + 1);
				B = (d_len + mu) * (c_w_d - 1 + mu * p_w_c);
				
				result += A / B;
			}
		}
		
		return result;
	}
	
	private double g_prime(double mu, double samples) throws IOException {
		long N = (long)Math.ceil(index.getDocumentIndex().getNumberOfDocuments() * samples);
		int vocabularySize = (int)Math.ceil(index.getCollectionStatistics().getNumberOfUniqueTerms() * samples);
		
		CollectionStatistics statistics = index.getCollectionStatistics();
		
		double result = 0;
		
		// loops are the inverse from the paper
		for (int w = 0; w < vocabularySize; w++) {
			Entry<String, LexiconEntry> currentWord = vocabulary.getIthLexiconEntry(w);
			
			double p_w_c = currentWord.getValue().getNumberOfEntries() / (double)statistics.getNumberOfTokens(); 
			double A, B;
			
			// this is the outer loop in the paper
			IterablePosting postingsList = invertedIndex.getPostings(currentWord.getValue());
			while (postingsList.next() != IterablePosting.EOL) {
				
				double c_w_d = postingsList.getFrequency();
				double d_len = postingsList.getDocumentLength() - 1;	//the paper used doc_len - 1
				
				A = Math.pow(c_w_d * (d_len * p_w_c - c_w_d + 1),2);
				B = Math.pow((d_len + mu),2) * Math.pow(c_w_d - 1 + mu * p_w_c, 2);
				
				result += A / B;
			}
		}
		
		return -result;
	}
	
	// the formula can be resumed in:
	// g  = A/(B*C)
	// g' = -A^2/(B^2*C^2) = -A^2/(B*C)^2
	// B*C = D --> g = A/D ; g' = -A^2/D^2 = -(A/D)^2 -> g' = -g^2
	
	/**
	 * Tunes the mu parameter, used on Dirichlet or DJM Language Model based on the
	 * corpus. This is done through Newton's method. Since the method always converges, according
	 * to the authors (and it probably does as the convergence factor is a 1/p(mu) function, this
	 * method returns mu if either: the modulus of the first derivative is smaller than <code>EPSILON</code>,
	 * or the amount of iterations is bigger than <code>MAX_ITERATIONS</code>. Relative difference on
	 * the update value is ignored.
	 * @param mu The initial value of mu. According to the paper authors 1 is a good initial point.
	 * @param sampling The percentage of the corpus that is going to be taken in account when tunning mu.
	 * This value must be ]0,1[.
	 * @return The tuned value of Mu.
	 * @throws IOException If there's an I/O fault while reading the index data.
	 */
	public double tuneMu(double mu, double sampling) throws IOException {
		double g, g_prime;
		int counter = 0;
		
		do {
			g = g(mu, sampling);
			g_prime = g_prime(mu, sampling);
			
			System.out.println("g_prime(mu) = " + g_prime);
			
			mu = mu - g / g_prime;
			
//			System.out.println("Iteration " + counter + " mu = " + mu);
		} while (++counter < MAX_ITERATIONS && Math.abs(g) > EPSILON);
		
		return mu;
	}	

}
