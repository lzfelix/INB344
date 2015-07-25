package week1;
import java.io.IOException;
import java.util.Scanner;


public class parser {
	public static void main(String args[]) {
		Scanner keyboard = new Scanner(System.in);
		FileReader fr = new FileReader();
		
		String fileName = keyboard.nextLine();
		try {
			fr.computeStatistics(fileName);
			
			System.out.println("Statistics for file " + fileName);
			System.out.println("Vocabulary size: " + fr.getVocabularySize());
			System.out.println("Total amount of elements: " + fr.getVocabularySize());
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
