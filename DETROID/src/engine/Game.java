package engine;

import java.util.Scanner;

import util.Command;
import util.List;
import util.Queue;
import util.IllegalCommandArgumentException;

/**PRE-MATURE; FOR TESTING ONLY
 * 
 * @author Viktor
 *
 */
public class Game implements Runnable {
	
	
	/**A simple enum for game state types and their assigned value scores.
	 * 
	 * @author Viktor
	 *
	 */
	public enum State {
		
		WIN	(Short.MAX_VALUE),
		TIE	(0),
		LOSS(Short.MIN_VALUE + 1);
		
		public final short score;
		
		private State(int score) {
			this.score = (short)score;
		}
	}
	
	static Scanner in = new Scanner(System.in);
	List<Command<String>> commandList;
	Position pos;
	boolean playersTurn;
	int diff;
	
	{
		commandList = new Queue<>();
		commandList.add(new Command<String>(p -> p.matches("^setboard"),
											p -> { try { pos = new Position(p.replaceFirst("^setboard", "")); }
											catch (IllegalArgumentException e) { throw new IllegalCommandArgumentException(e); }}));
		commandList.add(new Command<String>(p -> p.matches("^([a-hA-H][1-8]){2}[qQrRbBkK]?$"),
											p -> { if (!pos.makeMove(p)) throw new IllegalCommandArgumentException("Illegal move."); }));
	}

	public Game(boolean playerStarts) {
		this.pos = new Position();
		playersTurn = playerStarts;
		diff = 5;
	}
	public Game(boolean playerStarts, int difficulty) {
		this.pos = new Position();
		playersTurn = playerStarts;
		diff = difficulty;
	}
	public Game(Position pos, boolean playerStarts, int difficulty) {
		this.pos = pos;
		playersTurn = playerStarts;
		diff = difficulty;
	}
	public Game(String fen, boolean playerStarts, int difficulty) {
		this.pos = new Position(fen);
		playersTurn = playerStarts;
		diff = difficulty;
	}
	public void run() {
		Position pos = new Position();
		Search s;
		Move move;
		while (true) {
			pos.printStateToConsole();
			if (playersTurn) {
				System.out.print("YOUR MOVE: ");
				if (diff >= 5) {
					s = Search.getInstance(pos);
					s.start();
					while (!pos.makeMove(in.nextLine()))
						System.out.println("ILLEGAL MOVE.");
					s.interrupt();
					try {
						s.join();
					} catch (InterruptedException e) {
						//Not gonna happen
						e.printStackTrace();
					}
				}
				else {
					while (!pos.makeMove(in.nextLine()))
						System.out.println("ILLEGAL MOVE.");
				}
			}
			else {
				s = Search.getInstance(pos, diff*5000);
				s.start();
				try {
					s.join();
				} catch (InterruptedException e) {
					//Not gonna happen
					e.printStackTrace();
				}
				move = s.getBestMove();
				pos.makeMove(move);
				System.out.println("COMPUTER'S MOVE: " + move);
			}
			playersTurn = !playersTurn;
		}
	}
	
	public static void main(String[] args) {
		(new Game(true)).run();
	}
}
