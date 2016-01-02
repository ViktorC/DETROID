package engine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

import engine.Book.SelectionModel;
import util.List;
import util.Queue;

/**A UI base and controller class for the engine.
 * 
 * @author Viktor
 *
 */
public class Game {
	
	
	/**A simple enum for game outcome types.
	 * 
	 * @author Viktor
	 *
	 */
	public enum State {
		
		IN_PROGRESS ("*"),
		WHITE_WIN	("1-0"),
		BLACK_WIN	("0-1"),
		DRAW		("1/2-1/2");
		
		public final String pgnNotation;
		
		private State(String pgnNotation) {
			this.pgnNotation = pgnNotation;
		}
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
	
	private static Game INSTANCE = new Game();
	
	private Position pos;
	private String event;
	private String site;
	private Date date;
	private short round;
	private String whitePlayerName;
	private String blackPlayerName;
	private State state;
	
	private boolean playerIsWhite;
	
	private Book book;
	private long whiteTimeLeft;
	private long blackTimeLeft;

	private Game() {
		book = Book.getInstance();
		whiteTimeLeft = blackTimeLeft = 60*60*1000;
	}
	private Game(String event, String site, String whitePlayerName,
			String blackPlayerName) {
		this();
		pos = new Position();
		this.event = event;
		this.site = site;
		date = new Date();
		round = 1;
		this.whitePlayerName = whitePlayerName;
		this.blackPlayerName = blackPlayerName;
		state = State.IN_PROGRESS;
		playerIsWhite = true;
	}
	private Game(String event, String site, String whitePlayerName,
			String blackPlayerName, long whiteTimeLeft, long blackTimeLeft) {
		this(event, site, whitePlayerName, blackPlayerName);
		this.whiteTimeLeft = whiteTimeLeft;
		this.blackTimeLeft = blackTimeLeft;
	}
	private static Game parsePGN(String pgn) throws IllegalArgumentException {
		char tagChar;
		String tagContent, tagType, tagValue,
			event = null, site = null, date = null, round = null,
			whiteName = null, blackName = null, result = null, fen = null;
		int moveDescStartInd = 0;
		Game out = new Game();
		SimpleDateFormat dF;
		String[] moveDescParts;
		List<String> sanStrings = new Queue<>();
		Move move;
		if (pgn == null)
			return null;
		try {
			for (int i = 0; i < pgn.length(); i++) {
				if (pgn.charAt(i) == '[') {
					tagContent = "";
					while (++i < pgn.length() && (tagChar = pgn.charAt(i)) != ']')
						tagContent += tagChar;
					moveDescStartInd = i + 1;
					tagType = tagContent.substring(0, tagContent.indexOf(' '));
					tagValue = tagContent.substring(tagContent.indexOf('"') + 1, tagContent.lastIndexOf('"'));
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
			dF = new SimpleDateFormat("yyyy.MM.dd");
			dF.setLenient(false);
			out.date = dF.parse(date);
			out.round = Short.parseShort(round);
			out.whitePlayerName = whiteName;
			out.blackPlayerName = blackName;
			out.state = State.parsePGNResultTag(result);
			out.pos = fen == null ? new Position() : new Position(fen);
			if (moveDescStartInd < pgn.length())
				pgn = pgn.substring(moveDescStartInd);
			pgn = pgn.trim();
			pgn = pgn.replaceAll(";[.]*\\n", "");
			pgn = pgn.replaceAll("\\([^)]*\\)", "");
			pgn = pgn.replaceAll("\\{[^\\}]*\\}", "");
			if (out.state != State.IN_PROGRESS) {
				pgn = pgn.replaceAll("(1/2-1/2)|(1-0)|(0-1)", "");
			}
			moveDescParts = pgn.split("[\\s]+");
			for (String s : moveDescParts) {
				if (!s.matches("^[0-9]+.$") && !s.matches("^\\$[0-9]+$"))
					sanStrings.add(s);
			}
			while (sanStrings.hasNext()) {
				move = out.pos.parseSAN(sanStrings.next());
				out.pos.makeMove(move);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}
	private static void setInstance(Game game) {
		INSTANCE.pos = game.pos;
		INSTANCE.event = game.event;
		INSTANCE.site = game.site;
		INSTANCE.date = game.date;
		INSTANCE.round = game.round;
		INSTANCE.whitePlayerName = game.whitePlayerName;
		INSTANCE.blackPlayerName = game.blackPlayerName;
		INSTANCE.state = game.state;
		INSTANCE.playerIsWhite = game.playerIsWhite;
		INSTANCE.book = game.book;
		INSTANCE.whiteTimeLeft = game.whiteTimeLeft;
		INSTANCE.blackTimeLeft = game.blackTimeLeft;
	}
	public static Game getInstance(String event, String site, String whitePlayerName,
			String blackPlayerName) {
		setInstance(new Game(event, site, whitePlayerName, blackPlayerName));
		return INSTANCE;
	}
	public static Game getInstance(String event, String site, String whitePlayerName,
			String blackPlayerName, long whiteTimeLeft, long blackTimeLeft) {
		setInstance(new Game(event, site, whitePlayerName, blackPlayerName, whiteTimeLeft, blackTimeLeft));
		return INSTANCE;
	}
	public static Game getInstance(String pgn) {
		setInstance(parsePGN(pgn));
		return INSTANCE;
	}
	public long calculateTimeForNextMove(boolean forWhite) {
		return 30*1000;
	}
	public String toString() {
		String pgn = "", date;
		Calendar cal = Calendar.getInstance();
		pgn += "[Event \"" + event + "\"]\n";
		pgn += "[Site \"" + site + "\"]\n";
		if (this.date == null)
			date = "??";
		else {
			cal.setTime(this.date);
			date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
		}
		pgn += "[Date \"" + date + "\"]\n";
		pgn += "[Round \"" + round + "\"]\n";
		pgn += "[White \"" + whitePlayerName + "\"]\n";
		pgn += "[Black \"" + blackPlayerName + "\"]\n";
		pgn += "[Result \"" + state.pgnNotation + "\"]\n";
		pgn += "\n";
		pgn += pos.moveListInSAN();
		return pgn;
	}
	public static void main(String[] args) {
		boolean outOfBook = false;
		long start, end;
		Move bookMove;
		Scanner in = new Scanner(System.in);
		Game game = getInstance("test", "at home", "human opponent", "computer");
		while (game.state == State.IN_PROGRESS) {
			game.pos.printStateToConsole();
			if (game.pos.getTurn() == game.playerIsWhite) {
				System.out.print("Please make your move: ");
				start = System.currentTimeMillis();
				Search s = new Search(game.pos);
				s.start();
				while (!game.pos.makeMove(in.nextLine()))
					System.out.print("Illegal move.\nPlease try again: ");
				end = System.currentTimeMillis();
				s.interrupt();
				if (game.playerIsWhite) {
					game.whiteTimeLeft -= (end - start);
					if (game.whiteTimeLeft <= 0) {
						System.out.println("Out of time. Black wins.");
						break;
					}
				}
				else {
					game.blackTimeLeft -= (end - start);
					if (game.blackTimeLeft <= 0) {
						System.out.println("Out of time. White wins.");
						break;
					}
				}
			}
			else {
				System.out.println("Searching...");
				start = System.currentTimeMillis();
				if (!outOfBook) {
					bookMove = game.book.getMove(game.pos, SelectionModel.STOCHASTIC);
					if (bookMove != null) {
						end = System.currentTimeMillis();
						System.out.println("Book move: " + bookMove + "\n");
						game.pos.makeMove(bookMove);
						if (!game.playerIsWhite) {
							game.whiteTimeLeft -= (end - start);
							if (game.whiteTimeLeft <= 0) {
								System.out.println("Out of time. Black wins.");
								break;
							}
						}
						else {
							game.blackTimeLeft -= (end - start);
							if (game.blackTimeLeft <= 0) {
								System.out.println("Out of time. White wins.");
								break;
							}
						}
						continue;
					}
					outOfBook = true;
				}
				Search s = new Search(game.pos, game.calculateTimeForNextMove(!game.playerIsWhite));
				s.start();
				try {
					s.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				end = System.currentTimeMillis();
				System.out.println("Best move: " + s.getBestMove() + "\n");
				game.pos.makeMove(s.getBestMove());
				if (!game.playerIsWhite) {
					game.whiteTimeLeft -= (end - start);
					if (game.whiteTimeLeft <= 0) {
						System.out.println("Out of time. Black wins.");
						break;
					}
				}
				else {
					game.blackTimeLeft -= (end - start);
					if (game.blackTimeLeft <= 0) {
						System.out.println("Out of time. White wins.");
						break;
					}
				}
			}
			System.out.println();
			// Very rudimental for now.
			if (game.pos.generateAllMoves().length() == 0) {
				game.state = game.pos.getTurn() ? State.BLACK_WIN : State.WHITE_WIN;
			}
		}
		in.close();
		System.out.print("Game over: " + game.state);
	}
}
