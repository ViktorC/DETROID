package uci;

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
public class Protocol implements Runnable {

	Engine engine;
	Scanner in;
	PrintWriter out;
	
	public Protocol(Engine engine, InputStream in, OutputStream out) {
		this.engine = engine;
		this.in = new Scanner(in);
		this.out = new PrintWriter(out);
	}
	@Override
	public void run() {
		String input = "";
		String[] tokens;
		String header;
		do {
			input = in.nextLine().trim();
			tokens = input.split("\\s+");
			header = tokens[0];
		} while (input.equals("quit"));
	}

}
