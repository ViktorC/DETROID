package engine;

import java.util.Scanner;

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
		
		WIN	 (Short.MAX_VALUE),
		TIE	 ((short)0),
		LOSS ((short)(Short.MIN_VALUE + 1));
		
		public final short score;
		
		private State(short score) {
			this.score = score;
		}
	}
	
	Scanner in = new Scanner(System.in);
	Position pos;
	boolean playersTurn;
	int diff;

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
		String userMove;
		while (true) {
			if (playersTurn) {
				System.out.print("YOUR MOVE: ");
				if (diff >= 5) {
					s = new Search(pos.copy());
					s.start();
					userMove = in.nextLine();
					s.interrupt();
					try {
						s.join();
					} catch (InterruptedException e) {
						//Not gonna happen
						e.printStackTrace();
					}
					pos.makeMove(userMove);
				}
				else
					pos.makeMove(in.nextLine());
			}
			else {
				s = new Search(pos.copy(), diff*5000);
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
