package week2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

public class Tokeniser {
	/* Path to the NER classifier file */
	private static String SERIALIZED_CLASSIFIER = "classifiers/english.all.3class.distsim.crf.ser.gz";
	
	/**
	 * Reads every line of a file, removing HTML tags, multiple spaces and numbers if <code>allowNumbers</code>
	 * is set to <code>true</code>. Returns a String with the contents of the file filtered.
	 * @param textFilePath The path to the file to be purged.
	 * @param allowNumbers If numbers should not be removed from the original file.
	 * @return A String with the file contents filtered.
	 * @throws IOException if there's a fault while manipulating the file.
	 */
	public static String purgeFile(Path textFilePath, boolean allowNumbers) throws IOException {
		String regex = (allowNumbers) ? "[[^\\w ][_]]" : "[[^\\w ][\\d_]]";
		StringBuilder buffer = new StringBuilder();
		
		Files.readAllLines(textFilePath).forEach(line -> {
			//get rid of tabs
			line = line.replaceAll("\t", " ");
			
			//get rid of more than 1 whitespace and HTML tags
			line = line.trim().replaceAll("\\s{2,}", " ").replaceAll("<.*>", "");
			
			if (line.length() > 0) {
				//removing all non-alphabetic characters and multiple spaces
				line = line.replaceAll(regex, "");
				
				String[] words = line.split(" ");
				
				for (int i = 0; i < words.length; i++) {
					if (words[i].length() == 0) continue;
				
					//uncomment this to normalise all the words to lower case (NER may fail) 
//					words[i] = words[i].toLowerCase();
					buffer.append(words[i]);
					
					buffer.append("\n");	
				}
			}
		});
		
		return buffer.toString();
	}
	
	/**
	 * For each file on <code>corpusPath</code>, the following procedure is done:
	 * 1. For each file, a corresponding one, with the same name, is created on a folder on <code>outputFolder</code>. 
	 * If this folder doesn't exist, then it is created;
	 * 2. For each file on <code>corpusPath</code> folder, HTML tags, extra spaces and numbers <code>allowNumbers</code> 
	 * is set to <code>false</code> are removed;
	 * 3. Stanford's NER Classifiers attempts to label each word on one of the categories: Person, Location or Organization. 
	 * If it succeeds, the words is annotated using the format [word]_[class], otherwise it is left as it is;
	 * 4. The word, either classified or not is written on a new line on the file on <code>corpusPath</code>; 
	 * 
	 * <pre>All the files on the source folder MUST be text files.</pre>
	 * <post>If <code>outputFolder</code> doesn't exist, it is created during this function execution.</post> 
	 * @param corpusPath The path to the corpus location.
	 * @param outputFolder The path, including the folder name, to the directory that will hold the annotated files.
	 * @param allowNumbers If <code>true</code> numbers won't be removed from the files.
	 * @throws IOException If there is an IO fault.
	 * @throws ClassCastException NER thrown exception.
	 * @throws ClassNotFoundException NER thrown exception.
	 */
	public static void applyNEDClassifier(String corpusPath, String outputFolder, boolean allowNumbers) 
			throws IOException, ClassCastException, ClassNotFoundException {
		
		//Loading the classifier
		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(SERIALIZED_CLASSIFIER);
		
		//Creating the destination folder, if it doesn't exist
		File of = new File(outputFolder);
		of.mkdir();
		
		// Annotating each file
		Files.list(new File(corpusPath).toPath()).forEach(path -> {
			String purgedFile = "";
			
			try {
				purgedFile = purgeFile(path, allowNumbers);
			}
			catch (IOException e) {
				System.err.println("Error while parsing file " + path);
			}
			
			//apply the classification
			List<List<CoreLabel>> out = classifier.classify(purgedFile);
			
			//I'd rather keep everything in memory to minimise disk access
			StringBuilder buffer = new StringBuilder();
			for (List<CoreLabel> sentence : out)
				for (CoreLabel word : sentence) {
					buffer.append(word.word());
					
					if (word.get(CoreAnnotations.AnswerAnnotation.class) != null)
						buffer.append('_' + word.get(CoreAnnotations.AnswerAnnotation.class));
						
					buffer.append('\n');
				}
			
			
			String newFile = Paths.get(outputFolder, path.getFileName().toString()).toString();
			
			// writing annotated file to disk 
			try {
				PrintWriter output = new PrintWriter(newFile);
				output.write(buffer.toString());
				output.flush();
				output.close();
			} catch (Exception e) { }
		});
		
		
	}
		
	/* Didn't want to write a new class just to tokenise the files */
	public static void main(String args[]) {		
		try {
			applyNEDClassifier("corpus", "korpus", false);
			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
