package engine;

import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Scanner;

import util.Command;
import util.List;
import util.Queue;
import util.IllegalCommandArgumentException;

/**A UI base and controller class for the engine.
 * 
 * @author Viktor
 *
 */
public class Game implements Runnable {
	
	
	/**A simple enum for game outcome types.
	 * 
	 * @author Viktor
	 *
	 */
	public enum State {
		
		IN_PROGRESS,
		WHITE_WIN,
		BLACK_WIN,
		DRAW;
		
		/**Parses the result tag field's value of a game in PGN and returns the equivalent state.
		 * 
		 * @param tagValue
		 * @return
		 */
		public static State parsePGNResultTag(String tagValue) {
			switch (tagValue) {
				case "1-0":
					return WHITE_WIN;
				case "0-1":
					return BLACK_WIN;
				case "1/2-1/2":
					return DRAW;
				case "*":
					return IN_PROGRESS;
				default:
					return null;
			}
		}
	}
	
	Position pos;
	String event;
	String site;
	Date date;
	short round;
	String whitePlayerName;
	String blackPlayerName;
	State state;
	
	boolean playerIsWhite;
	boolean ponderingOn;
	
	private InputStream inputStream;
	private Scanner in;
	
	List<Command<String>> commandList;
	
	{
		commandList = new Queue<>();
		commandList.add(new Command<String>(p -> p.startsWith("position"),
											p -> { try { pos = new Position(p.replaceFirst("position", "")); }
											catch (IllegalArgumentException e) { throw new IllegalCommandArgumentException(e); }}));
		commandList.add(new Command<String>(p -> p.matches("^([a-hA-H][1-8]){2}[qQrRbBkK]?$"),
											p -> { if (!pos.makeMove(p)) throw new IllegalCommandArgumentException("Illegal move."); }));
	}

	private Game() {
		
	}
	private Game(String event, String site, String whitePlayerName,
			String blackPlayerName) {
		pos = new Position();
		this.event = event;
		this.site = site;
		date = new Date();
		round = 1;
		this.whitePlayerName = whitePlayerName;
		this.blackPlayerName = blackPlayerName;
		state = State.IN_PROGRESS;
		playerIsWhite = true;
		ponderingOn = true;
		inputStream = System.in;
	}
	public static Game getInstance(String event, String site, String whitePlayerName,
			String blackPlayerName) {
		Game out = new Game(event, site, whitePlayerName, blackPlayerName);
		out.in = new Scanner(out.inputStream);
		return out;
	}
	public static Game getInstance(String event, String site, String whitePlayerName,
			String blackPlayerName, InputStream inputStream) {
		Game out = new Game(event, site, whitePlayerName, blackPlayerName);
		out.inputStream = inputStream;
		out.in = new Scanner(out.inputStream);
		return out;
	}
	public static Game getInstance(String fen, String event, String site,
			String whitePlayerName, String blackPlayerName) {
		Game out = getInstance(event, site, whitePlayerName, blackPlayerName);
		out.pos = new Position(fen);
		return out;
	}
	public static Game getInstance(String fen, String event, String site,
			String whitePlayerName, String blackPlayerName, InputStream inputStream) {
		Game out = getInstance(event, site, whitePlayerName, blackPlayerName, inputStream);
		out.pos = new Position(fen);
		return out;
	}
	
	public Game getInstance(String pgn) {
		return parsePGN(pgn);
	}
	public static Game parsePGN(String pgn) {
		Game out = new Game();
		char tagChar;
		String tagContent, tagType, tagValue,
			event = null, site = null, date = null, round = null,
			whiteName = null, blackName = null, result = null, fen = null;
		try {
			for (int i = 0; i < pgn.length(); i++) {
				if (pgn.charAt(i) == '[') {
					tagContent = "";
					while (++i < pgn.length() && (tagChar = pgn.charAt(i)) != ']')
						tagContent += tagChar;
					tagType = tagContent.substring(0, tagContent.indexOf(' '));
					tagValue = tagContent.substring(tagContent.indexOf('"'), tagContent.lastIndexOf('"'));
					switch (tagType.toUpperCase()) {
						case "EVENT":
							event = tagValue;
						break;
						case "SITE":
							site = tagValue;
						break;
						case "DATE":
							date = tagValue;
						break;
						case "ROUND":
							round = tagValue;
						break;
						case "WHITE":
							whiteName = tagValue;
						break;
						case "BLACK":
							blackName = tagValue;
						break;
						case "RESULT":
							result = tagValue;
						break;
						case "FEN":
							fen = tagValue;
					}
				}
			}
			if (event == null || site == null || date == null || round == null ||
				whiteName == null || blackName == null || result == null)
				throw new IllegalArgumentException();
			out.event = event;
			out.site = site;
			out.date = DateFormat.getDateInstance().parse(date);
			out.round = Short.parseShort(round);
			out.whitePlayerName = whiteName;
			out.blackPlayerName = blackName;
			out.state = State.parsePGNResultTag(result);
			out.pos = fen == null ? new Position() : new Position(fen);
		}
		catch (Exception e) {
			
		}
		return out;
	}
	public void run() {
		Position pos = new Position();
		Search s;
		Move move;
		while (true) {
			pos.printStateToConsole();
			if (playerIsWhite) {
				System.out.print("YOUR MOVE: ");
				if (ponderingOn) {
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
				s = Search.getInstance(pos, 20000);
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
			playerIsWhite = !playerIsWhite;
		}
	}
	
	public static void main(String[] args) {
		(new Game()).run();
	}
}
