package chv_expansion;
import java.io.IOException;
import java.util.Scanner;


public class Main {
	public static void main(String args[]) {
		FileParser p = null;
		
		try {
			p = new FileParser("./CHV/CHV_modified.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Scanner s = new Scanner(System.in);
		while(s.hasNextLine()) {
			String input = s.nextLine();
			
			String[] newTerms = p.expandQuery(input,2);
			for (String term : newTerms)
				System.out.print(term + " ");
			System.out.println();
		}
	}
}
