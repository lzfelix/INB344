package week3;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class QueryConsole {
	private final static String PATH = "corpus";
	
	public static void main(String args[]) {
		Indexer index = null;
		
		try {
			index = new Indexer(PATH);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		System.out.println(index);
		
		System.out.println("Index created.");
		
		Scanner keyboard = new Scanner(System.in);
		
		do {
			String[] input = keyboard.nextLine().toLowerCase().split(" ");
			String result = "";
			
			try {
				if (input.length == 1 && input[0].equals("exit"))
					break;
				else if (input.length == 1)
					result = index.simpleQuery(input[0]);
				else if (input[0].equals("and"))
					result = index.andQuery(Arrays.copyOfRange(input, 1, input.length));
				else if (input[0].equals("or"))
					result = index.orQuery(Arrays.copyOfRange(input, 1, input.length));
				else
					System.out.println("Invalid query.");
				
				if (result != null)
					System.out.println(result);
				else
					System.out.println("No results match this query.");
				
			}
			catch (IndexerException ex) {
				System.out.println("Invalid query.");
			}
			
		} while(true);
	}
}
