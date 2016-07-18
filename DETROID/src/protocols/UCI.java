package protocols;

import java.util.Scanner;

/**
 * An implementation of the Universal Chess Interface protocol which handles communication between a UCI compatible chess engine and a UCI
 * compatible GUI using the sandard input and standard output streams.
 * 
 * @author Viktor
 *
 */
public class UCI implements Runnable {

	Scanner in;
	UCIEngine engine;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
