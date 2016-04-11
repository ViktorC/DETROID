package engine;

import java.util.Calendar;
import java.util.Date;

/**
 * A data structure for keeping track of the course of a chess game. It can also parse games in PGN and output its state in PGN.
 * 
 * @author Viktor
 *
 */
public class Game {
	
	
	/**
	 * A simple enum for game outcome types.
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
	}
	
	String startPos;
	Position position;
	String event;
	String site;
	Date date;
	short round;
	String whitePlayerName;
	String blackPlayerName;
	State state;

	Game() {
		
	}
	/**
	 * Returns a game instance according to the parameter values.
	 * 
	 * @param position
	 * @param event
	 * @param site
	 * @param whitePlayerName
	 * @param blackPlayerName
	 */
	public Game(String position, String event, String site, String whitePlayerName,
			String blackPlayerName) {
		startPos = position;
		try {
			this.position = ChessParser.parseFEN(position);
		} catch (ChessParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.event = event;
		this.site = site;
		date = new Date();
		round = 1;
		this.whitePlayerName = whitePlayerName;
		this.blackPlayerName = blackPlayerName;
		state = State.IN_PROGRESS;
	}
	/**
	 * Returns a string of the game in PGN.
	 */
	@Override
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
		pgn += "[FEN \"" + startPos + "\"]\n";
		pgn += "\n";
		pgn += position.moveListToSAN();
		return pgn;
	}
}
