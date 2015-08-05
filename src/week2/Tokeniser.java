package week2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

public class Tokeniser {
	private final static String NED_TOKENS_FOLDER = "NED_tokens";
	private final static String REGULAR_TOKENS_FOLDER = "_tokens";
	
	/* Path to the NER classifier file */
	private static String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";
	
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
	 * Reads all the files from a folder and filter them using <code>purgeFile</code>. Ensure that all the
	 * files on this folder are text. Each word will be replaced by /word[_annotation]/, where [_annontation] can
	 * be PERSON, ORGANIZATION, LOCATION or not exist if the word doesn't fit in any of these classes. 
	 * @param folderPath Path to the folder to be read.
	 * @throws IOException If there's a fault while manipulating the file.
	 */
	public static void TokeniseCollection(String folderPath) throws IOException {
		File tokensFolder = new File(folderPath + REGULAR_TOKENS_FOLDER);
		tokensFolder.mkdir();
		
		Files.list(new File(folderPath).toPath()).forEach(path -> {			
			PrintWriter fileWriter = null;
			String purgedFile = "";
			String tokensFile = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.')) + ".txt";
			
			try {
				fileWriter = new PrintWriter(Paths.get(tokensFolder.getPath(), tokensFile).toString());
				purgedFile = purgeFile(path, false);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			fileWriter.write(purgedFile);
			fileWriter.flush();
			fileWriter.close();
		});
	}
	
	/**
	 * Walks though a folder of files containing tokens words and classify them using NED-Stanford classifier.
	 * @param folderPath Path to the folder with the token files.
	 * @throws ClassCastException Internal NED Classifier thrown exceptions.
	 * @throws ClassNotFoundException Internal NED Classifier thrown exceptions.
	 * @throws IOException If there's a fault while manipulating the file.
	 */
	public static void applyNED(String folderPath) throws ClassCastException, ClassNotFoundException, IOException {
		//Loading the classifier
		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);
		
		//create a folder named NED_tokens on the upper level on the folders hierarchy 
		File currentFolder = new File(folderPath);
		
		String parentFolder = currentFolder.getCanonicalPath().substring(0, currentFolder.getCanonicalPath().lastIndexOf('/'));
		File tokensFolder = new File(Paths.get(parentFolder, NED_TOKENS_FOLDER).toString());
		tokensFolder.mkdirs();
		
		//parse and annotate each file
		Files.list(new File(folderPath).toPath()).forEach(path -> {
			String filePath = Paths.get(tokensFolder.getPath(), path.getFileName().toString()).toString();
			
			try {
				String fileContents = IOUtils.slurpFile(path.toString());
				List<List<CoreLabel>> out = classifier.classify(fileContents);

				PrintWriter pw = new PrintWriter(filePath);
				
				//I'd rather keep everything in memory to minimise the amount of disk accesses
				StringBuilder buffer = new StringBuilder();
				
				for (List<CoreLabel> sentence : out)
					for (CoreLabel word : sentence) {
						buffer.append(word.word());
						
						if (word.get(CoreAnnotations.AnswerAnnotation.class) != null)
							buffer.append('_' + word.get(CoreAnnotations.AnswerAnnotation.class));
							
						buffer.append('\n');
					}
				
				pw.write(buffer.toString());
				pw.flush();
				pw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
	}
	
	/* Didn't want to write a new class just to tokenise the files */
	public static void main(String args[]) {		
		try {
			System.out.println("Tokenising...");
			TokeniseCollection("corpus");
			System.out.println("Labelling...");
			applyNED("corpus" + REGULAR_TOKENS_FOLDER);
			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
