package tests;
import java.io.IOException;
import java.util.Scanner;

import queryExpansion.CHVQueryExpansion;

/**
 * A simple class to test CHV expansion. Just type a query to expand.
 * If expansions are found, they are printed, otherwise a newline is
 * displayed.
 * @author Luiz Felix
 */
public class TestCHVExpansion {
	public static void main(String args[]) {
		CHVQueryExpansion p = null;
		
		try {
			p = new CHVQueryExpansion("./CHV/CHV_modified.txt");
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
		s.close();
	}
}
