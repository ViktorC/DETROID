package protocols;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * An implementation of the Universal Chess Interface protocol which handles communication between a UCI compatible chess engine and a UCI
 * compatible GUI using the sandard input and standard output streams.
 * 
 * @author Viktor
 *
 */
public class UCI implements Runnable {

	UCIEngine engine;
	Scanner in;
	PrintWriter out;
	
	public UCI(UCIEngine engine, InputStream in, OutputStream out) {
		this.engine = engine;
		this.in = new Scanner(in);
		this.out = new PrintWriter(out);
	}
	@Override
	public void run() {
		String input = "";
		do {
			
		} while (input.equals("quit"));
	}

}
