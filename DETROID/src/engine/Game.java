package engine;

import java.util.Scanner;

/**PRE-MATURE; FOR TESTING ONLY
 * 
 * @author Viktor
 *
 */
public class Game implements Runnable {
	
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
		Search s = new Search(pos, diff*5000);
		Move move;
		String userMove;
		Thread t;
		while (true) {
			if (playersTurn) {
				pos.printStateToConsole();
				System.out.print("MOVE: ");
				if (diff >= 5) {
					s.setPondering(true);
					t = new Thread(s);
					t.start();
					userMove = in.nextLine();
					t.interrupt();
					try {
						t.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					pos.makeMove(userMove);
				}
				else
					pos.makeMove(in.nextLine());
			}
			else {
				t = new Thread(s);
				t.start();
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				move = s.getBestMove();
				pos.makeMove(move);
				System.out.println(move);
			}
			playersTurn = !playersTurn;
		}
	}
	
	public static void main(String[] args) {
		(new Game(true)).run();
	}
}
