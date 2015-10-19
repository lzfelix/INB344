package queryExpansion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.terrier.structures.Index;
import org.terrier.terms.PorterStemmer;
import org.terrier.terms.Stopwords;

/**
 * This class performs the two-pahse query expansion. Initially it expands the query based on the CHV 
 * collection and if there are more expansion slots available, it uses the EMIM query expansion.
 * 
 * @author Luiz Felix
 */
public class StagedQueryExpansion implements PipelineInterface{
	// used to prepare the queries
	private PorterStemmer porterStemmer;
	private Stopwords stopwords;
	
	private EMIMQueryExpansion qe;
	private CHVQueryExpansion chv;
	
	/**
	 * Creates an object that expands a query by incrementally using the Consumer Health Vocabulary
	 * (thesaurus expansion) and then EMIM query expansion.
	 * @param index The collection's Terrier Index.
	 * @param CHVPath The path to the (modified) Consumer Health Vocabulary File
	 * @throws FileNotFoundException If the CHV file isn't fount at <code>CHVPath</code>
	 */
	public StagedQueryExpansion(Index index, String CHVPath) throws FileNotFoundException {
		porterStemmer = new PorterStemmer();
		stopwords = new Stopwords(null);
		
		qe = new EMIMQueryExpansion(index);
		chv = new CHVQueryExpansion(CHVPath);
	}
	
	/**
	 * Given a input query or single word:
	 *  - removes all punctuation characters
	 *  - replaces - by spaces
	 *  - skips stop and repeated words
	 *  - stores lower case words stem 
	 * @param originalQuery The original query from the input file.
	 * @return A query consisting on non-stopped, unique, lower cased stemmed words.
	 */
	private String prepareQuery(String originalQuery) {
		Set<String> uniqueTerms = new HashSet<>();
		StringBuilder preparedString = new StringBuilder();
		
		// replacing - by spaces and removing punctuation characters
		String query = originalQuery.replaceAll("-", " ").replaceAll("\\p{Punct}", "");
		
		// keeping only the lower case steam of non-stop-words  
		for (String term : query.split(" ")) {
			if (this.stopwords.isStopword(term)) continue;
			
			String treatedTerm = porterStemmer.stem(term.toLowerCase());
			
			if (!uniqueTerms.contains(treatedTerm)) {
				uniqueTerms.add(treatedTerm);
				preparedString.append(treatedTerm + " ");
			}
		}
		
		return preparedString.substring(0, preparedString.length() - 1);
	}
	
	/**
	 * Performs the query expansion in phases that are responsible for the following steps:
	 * - Performs thesaurus query expansion using CHV.
	 * - Stops, stems, lower cases and remove repeated terms from the original query
	 * - Performs EMIM query expansion based on this transformed query (it doesn't use the CHV terms found on
	 * step 1)
	 * - Adds all the CHV expansion terms to the treated query (usually a small set) and then adds the EMIM expansion
	 * terms on a Round-Robin fashion, but giving priority for the most likely terms 
	 * @param query The original input query
	 * @param maxExpansions How many words are allowed to be added to the query. If this value is 0 then only
	 * CHV expansion is performed
	 * @return an stopped, stemmed, lower cased, unique-terms query.
	 * @throws IOException If there's an IO fault while performing EMIM query expansion.
	 */
	public String expandQuery(String query, int maxExpansions) throws IOException {
		StringBuilder expansionQueryBuffer = new StringBuilder();

		/* Phrase 1 - CHV expansion (simple as that) */
		String CHVWords[] = chv.expandQuery(query, maxExpansions);
		
		/* Phase 2 - Stop, steam and lower case the CHV expansions. Don't add them to the original query yet */
		int maxCHVExpansions;
		if (maxExpansions > 0 && CHVWords.length > maxExpansions / 2)
			maxCHVExpansions = maxExpansions / 2;
		else
			maxCHVExpansions = CHVWords.length;
		
		for (int i = 0; i < maxCHVExpansions; i++) {
			expansionQueryBuffer.append(prepareQuery(CHVWords[i]));
			expansionQueryBuffer.append(" ");
		}
		
		/* Phase 3 - Stop, steam and lower case the original query */
		query = prepareQuery(query);
		
		/* Phase 4 - EMIM expansion (this one is tricky) */
		
		// updates the amount of expansion slots. Since the CHV expansion usually returns
		// few terms, all of them are considered.
		maxExpansions -= maxCHVExpansions;
		
		if (maxExpansions > 0) {
			int amountOfTranslations = maxExpansions;
			
			// stores the expansion for each term
			List<List<String>> expansions = new ArrayList<>();
			
			/* So, it tries to expand each word with [amountOfTranslatiosn] translations. Since at least one 
			 * translation for each word is employed (if amountOfTranslatiosn is big enough), after adding 
			 * one new term, the amount of free expansion slots is decreased by 1.
			 */
			for (String word : query.split(" ")) {
				if (amountOfTranslations == 0) break;
				
				// converting the linked list into array list for efficiency
				ArrayList<String> translationArray = new ArrayList<>(qe.getTranslations(word, amountOfTranslations));
				expansions.add(translationArray);
				
				amountOfTranslations--;
			}
			
			/* Phase 4.1 - Adding just EMIM translations enough to the query using Round-robin*/
			int marker = 0;
			int modulatedIndex = -1;
			
			//finding the maximum amount of possible expansions
			int totalListLength = 0;
			int examinedExpansions = -1;
			for (List<String> ls : expansions) 
				totalListLength += ls.size();
			
			do {
				// grants that the amount of expansions isn't bigger than the amount of available words
				if (examinedExpansions++ > totalListLength) break;
				
				modulatedIndex++;
				if (modulatedIndex == expansions.size()) {
					modulatedIndex = 0;
					marker++;
				}
				
				// a little bit radical
				if (expansions.get(modulatedIndex).size() <= marker) continue;
				
				// add the translation space-separated on the buffer
				expansionQueryBuffer.append(expansions.get(modulatedIndex).get(marker));
				if (--maxExpansions > 0)
					expansionQueryBuffer.append(" ");
				
				
			} while (maxExpansions > 0 );			
		}
		
		/* Phase 5 - Joining the both expansions and the treated query and return it */
		String expandedQuery = query + " " + expansionQueryBuffer.toString();
		
		/* Phase 6 - Removing eventual repeated terms -- This method can be extracted, but it'll screw the performance*/
		Set<String> uniqueTerms = new HashSet<String>();
		StringBuilder uniqueTermsBuffer = new StringBuilder();
		
		for (String term : expandedQuery.split(" ")) {
			if (!uniqueTerms.contains(term)) {
				uniqueTerms.add(term);
				
				// adding new terms separated is faster =)
				uniqueTermsBuffer.append(term);
				uniqueTermsBuffer.append(" ");
			}
		}
		
		// getting rid of the trailling space
		return uniqueTermsBuffer.substring(0, uniqueTermsBuffer.length() - 1);
	}
}
