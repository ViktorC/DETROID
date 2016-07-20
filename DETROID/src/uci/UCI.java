package uci;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Observer;
import java.util.Scanner;
import java.util.*;
import java.util.concurrent.*;

/**
 * An implementation of the Universal Chess Interface protocol which serves as a controller for a UCI compatible chess engine
 * when communicating with a UCI compatible GUI.
 * 
 * @author Viktor
 *
 */
public final class UCI implements Observer {
	
	private static UCI INSTANCE = new UCI(System.in, System.out);
	
	private Scanner in;
	private PrintWriter out;
	
	public UCI getInstance() {
		return INSTANCE;
	}
	private UCI(InputStream in, OutputStream out) {
		this.in = new Scanner(in);
		this.out = new PrintWriter(out);
	}
	@Override
	public void update(Observable p1, Object p2)
	{
		// TODO: Implement this method
	}
	
	public synchronized void run(Engine engine) {
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		boolean over =false;
		String input = "";
		String[] tokens;
		String header;
		while (!in.nextLine().trim().equals("uci"));
		while (!over) {
			input = in.nextLine().trim();
			tokens = input.split("\\s+");
			header = tokens[0];
			switch (header) {
				case "debug": {
					if (tokens[1].equals("on"))
						engine.getInfo().addObserver(this);
					else
						engine.getInfo().deleteObserver(this);
				} break;
				case "isready": {
					out.println("readyok");
				} break;
				case "setoption": {
					
				} break;
				case "register": {
					
				} break;
				case "ucinewgame": {
					
				} break;
				case "position": {
					
				} break;
				case "go": {
					
				} break;
				case "stop": {
					
				} break;
				case "ponderhit": {
					
				} break;
				case "quit": {
					over = true;
				}
			}
		}
		engine.getInfo().deleteObserver(this);
		exec.shutdown();
	}

}
