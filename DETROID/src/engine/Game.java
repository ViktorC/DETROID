package engine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import util.List;
import util.Queue;

/**
 * A data structure for keeping track of the course of a chess game. It can also parse games in PGN and output its state in PGN.
 * 
 * @author Viktor
 *
 */
class Game {
	
	
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
	/**
	 * A simple enum for the two sides/colors in a chess game.
	 * 
	 * @author Viktor
	 *
	 */
	public enum Side {
		
		WHITE,
		BLACK;
		
	}
	
	private String startPos;
	private Position position;
	private String event;
	private String site;
	private Date date;
	private float round;
	private String whitePlayerName;
	private String blackPlayerName;
	private State state;

	/**
	 * Parses a game in PGN notation and returns a Game instance.
	 * 
	 * @param pgn
	 * @return
	 * @throws ChessParseException
	 */
	public static Game parse(String pgn) throws ChessParseException {
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
				throw new ChessParseException("Missing tag(s).");
			out.event = event;
			out.site = site;
			dF = new SimpleDateFormat("yyyy.MM.dd");
			dF.setLenient(false);
			out.date = dF.parse(date);
			out.round = Short.parseShort(round);
			out.whitePlayerName = whiteName;
			out.blackPlayerName = blackName;
			switch (result) {
				case "1-0":
					out.state = State.WHITE_WIN;
					break;
				case "0-1":
					out.state = State.BLACK_WIN;
					break;
				case "1/2-1/2":
					out.state = State.DRAW;
					break;
				case "*":
					out.state = State.IN_PROGRESS;
					break;
				default:
					throw new ChessParseException();
			}
			out.position = fen == null ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			out.startPos = out.position.toString();
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
				move = out.position.parseSAN(sanStrings.next());
				out.position.makeMove(move);
			}
		}
		catch (Exception e) {
			throw new ChessParseException(e);
		}
		return out;
	}
	/**
	 * Returns a game instance according to the parameter values.
	 * 
	 * @param position The position in FEN.
	 * @param event
	 * @param site
	 * @param whitePlayerName
	 * @param blackPlayerName
	 * @throws ChessParseException 
	 */
	public Game(String position, String event, String site, String whitePlayerName, String blackPlayerName) throws ChessParseException {
		this.position = Position.parse(position);
		startPos = this.position.toString();
		this.event = event;
		this.site = site;
		date = new Date();
		round = 1;
		this.whitePlayerName = whitePlayerName;
		this.blackPlayerName = blackPlayerName;
		setState();
	}
	/**
	 * Returns a game instance with the site and event values being null.
	 * 
	 * @param position The position in FEN.
	 * @param whitePlayerName
	 * @param blackPlayerName
	 * @throws ChessParseException 
	 */
	public Game(String position, String whitePlayerName, String blackPlayerName) throws ChessParseException {
		this(position, null, null, whitePlayerName, blackPlayerName);
	}
	/**
	 * Returns a game instance with the site, event, whitePlayerName, and blackPlayerName values being null.
	 * 
	 * @param position The position in FEN.
	 * @throws ChessParseException 
	 */
	public Game(String position) throws ChessParseException {
		this(position, null, null, null, null);
	}
	/**
	 * Returns a game instance set to the start position with the site, event, whitePlayerName, and blackPlayerName values being null.
	 */
	public Game() {
		try {
			position = Position.parse(Position.START_POSITION_FEN);
		} catch (ChessParseException e) { }
		startPos = Position.START_POSITION_FEN;
		date = new Date();
		round = 1;
		state = State.IN_PROGRESS;
	}
	public String getStartPos() {
		return startPos;
	}
	/**
	 * Returns a deep copy of the current position. Changes made to the object are not reflected in the Game instance.
	 * 
	 * @return
	 */
	public Position getPosition() {
		return position.deepCopy();
	}
	public String getEvent() {
		return event;
	}
	public String getSite() {
		return site;
	}
	public Date getDate() {
		return date;
	}
	public short getRound() {
		return (short)round;
	}
	public String getWhitePlayerName() {
		return whitePlayerName;
	}
	public String getBlackPlayerName() {
		return blackPlayerName;
	}
	public Side getSideToMove() {
		return position.isWhitesTurn ? Side.WHITE : Side.BLACK;
	}
	public State getState() {
		return state;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public void setSite(String site) {
		this.site = site;
	}
	public void setWhitePlayerName(String whitePlayerName) {
		this.whitePlayerName = whitePlayerName;
	}
	public void setBlackPlayerName(String blackPlayerName) {
		this.blackPlayerName = blackPlayerName;
	}
	private void setState() {
		state = position.getMoves().size() == 0 || position.fiftyMoveRuleClock >= 100 || position.repetitions >= 3 ?
			position.isInCheck ? position.isWhitesTurn ? State.BLACK_WIN : State.WHITE_WIN : State.DRAW : State.IN_PROGRESS;
	}
	/**
	 * Plays a move defined either in PACN or SAN on the board if legal.
	 * 
	 * @param move The move to make defined either in pure algebraic coordinate notation or standard algebraic notation.
	 * @return Whether the move was legal and of valid format.
	 */
	public boolean play(String move) {
		Move m;
		try {
			m = position.parsePACN(move);
		} catch (ChessParseException | NullPointerException e) {
			try {
				m = position.parseSAN(move);
			} catch (ChessParseException | NullPointerException e1) { return false; }
		}
		if (position.isLegal(m)) {
			position.makeMove(m);
			setState();
			round += 0.5f;
			return true;
		}
		return false;
	}
	/**
	 * Returns a string of the game in PGN.
	 */
	@Override
	public String toString() {
		String pgn = "", date;
		Calendar cal = Calendar.getInstance();
		pgn += "[Event \"" + event == null ? "N/A" : event + "\"]\n";
		pgn += "[Site \"" + site == null ? "N/A" : site + "\"]\n";
		if (this.date == null)
			date = "??";
		else {
			cal.setTime(this.date);
			date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
		}
		pgn += "[Date \"" + date + "\"]\n";
		pgn += "[Round \"" + round + "\"]\n";
		pgn += "[White \"" + whitePlayerName == null ? "N/A" : whitePlayerName + "\"]\n";
		pgn += "[Black \"" + blackPlayerName == null ? "N/A" : blackPlayerName + "\"]\n";
		pgn += "[Result \"" + state.pgnNotation + "\"]\n";
		pgn += "[FEN \"" + startPos + "\"]\n";
		pgn += "\n";
		pgn += position.getMoveListStringInSAN();
		return pgn;
	}
}
