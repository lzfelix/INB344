package week1;
import java.io.IOException;

public class Parser {
	private static final String folderPath = "corpus";
	
	public static void main(String args[]) {
		FileReader fr = new FileReader();
		
		try {
			fr.computeFolderStatistics(folderPath, false, new String[]{"html"});

			System.out.println("Vocabulary size: " + fr.getVocabularySize());
			System.out.println("Total amount of elements: " + fr.getVocabularySize());
			System.out.println("In " + fr.getAmountOfFiles() + " files.");
			
//			for (Entry<String, Integer> entry : fr.getTable().entrySet()) 
//				System.out.println(entry.getKey() + " x" + entry.getValue());
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
