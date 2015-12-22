package engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

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
	
	private static Game INSTANCE;
	
	private Position pos;
	private String event;
	private String site;
	private Date date;
	private short round;
	private String whitePlayerName;
	private String blackPlayerName;
	private State state;
	
	private boolean playerIsWhite;

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
}
