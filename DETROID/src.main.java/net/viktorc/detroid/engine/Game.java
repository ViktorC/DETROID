package net.viktorc.detroid.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.viktorc.detroid.framework.control.GameState;

/**
 * A data structure for keeping track of the course of a chess game. It can also parse games in PGN and output its state in PGN.
 * 
 * @author Viktor
 *
 */
class Game {
	
	private Position startPosition;
	private Position position;
	private String event;
	private String site;
	private String date;
	private int round;
	private String whitePlayerName;
	private String blackPlayerName;
	private GameState state;

	/**
	 * Parses a game in PGN notation and returns a Game instance.
	 * 
	 * @param pgn
	 * @return
	 * @throws ChessParseException
	 */
	static Game parse(String pgn) throws ChessParseException {
		char tagChar;
		String tagContent, tagType, tagValue,
			event = null, site = null, date = null, round = null,
			whiteName = null, blackName = null, result = null, fen = null;
		Game out = new Game();
		String[] moveDescParts;
		ArrayList<String> sanStrings = new ArrayList<>();
		Move move;
		if (pgn == null)
			return null;
		try {
			for (int i = 0; i < pgn.length(); i++) {
				if (pgn.charAt(i) == '[') {
					tagContent = "";
					while (++i < pgn.length() && (tagChar = pgn.charAt(i)) != ']')
						tagContent += tagChar;
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
			out.date = date;
			out.round = "?".equals(round) ? -1 : Short.parseShort(round);
			out.whitePlayerName = whiteName;
			out.blackPlayerName = blackName;
			out.position = fen == null ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			out.startPosition = out.position.deepCopy();
			pgn = pgn.substring(pgn.lastIndexOf(']') + 1, pgn.length());
			pgn = pgn.trim();
			pgn = pgn.replaceAll(";[.]*\\n", "");
			pgn = pgn.replaceAll("\\([^)]*\\)", "");
			pgn = pgn.replaceAll("\\{[^\\}]*\\}", "");
			pgn = pgn.replaceAll("[0-9]+\\.", "");
			switch (result) {
			case "1-0":
				pgn = pgn.replaceAll("1-0", "");
				break;
			case "0-1":
				pgn = pgn.replaceAll("0-1", "");
				break;
			case "1/2-1/2":
				pgn = pgn.replaceAll("1/2-1/2", "");
				break;
			case "*":
				pgn = pgn.replaceAll("\\*", "");
				break;
			default:
				throw new ChessParseException();
			}
			moveDescParts = pgn.split("[\\s]+");
			for (String s : moveDescParts) {
				s = s.trim();
				if (!s.isEmpty() && !s.matches("^\\$[0-9]+$"))
					sanStrings.add(s);
			}
			for (String sanString : sanStrings) {
				move = out.position.parseSAN(sanString);
				out.position.makeMove(move);
			}
			out.setState();
			if (out.state == GameState.IN_PROGRESS && !"*".equals(result)) {
				if ("1-0".equals(result))
					out.state = GameState.UNSPECIFIED_WHITE_WIN;
				else if ("0-1".equals(result))
					out.state = GameState.UNSPECIFIED_BLACK_WIN;
				else
					out.state = GameState.DRAW_BY_AGREEMENT;
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
	 * @param position
	 * @param event
	 * @param site
	 * @param whitePlayerName
	 * @param blackPlayerName
	 * @param round
	 * @throws NullPointerException
	 * @throws ChessParseException 
	 */
	Game(Position position, String event, String site,
			String whitePlayerName, String blackPlayerName, int round) throws NullPointerException, ChessParseException {
		this.position = Position.parse(position.toString());
		startPosition = this.position.deepCopy();
		this.event = event;
		this.site = site;
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
		this.round = round;
		this.whitePlayerName = whitePlayerName;
		this.blackPlayerName = blackPlayerName;
		setState();
	}
	/**
	 * Returns a game instance according to the parameter values with the round number set to 1.
	 * 
	 * @param position
	 * @param event
	 * @param site
	 * @param whitePlayerName
	 * @param blackPlayerName
	 * @throws NullPointerException
	 * @throws ChessParseException 
	 */
	Game(Position position, String event, String site, String whitePlayerName, String blackPlayerName)
			throws NullPointerException, ChessParseException {
		this(position, event, site, whitePlayerName, blackPlayerName, -1);
	}
	/**
	 * Returns a game instance with the site and event values being null and round set to 1.
	 * 
	 * @param position
	 * @param whitePlayerName
	 * @param blackPlayerName
	 * @throws NullPointerException
	 * @throws ChessParseException 
	 */
	Game(Position position, String whitePlayerName, String blackPlayerName) throws NullPointerException, ChessParseException {
		this(position, null, null, whitePlayerName, blackPlayerName, -1);
	}
	/**
	 * Returns a game instance with the site, event, whitePlayerName, and blackPlayerName values being null and round set to 1.
	 * 
	 * @param position
	 * @throws NullPointerException
	 * @throws ChessParseException 
	 */
	Game(Position position) throws NullPointerException, ChessParseException {
		this(position, null, null, null, null, -1);
	}
	/**
	 * Returns a game instance set to the start position with the site, event, whitePlayerName, and blackPlayerName values being null
	 * and round set to 1.
	 */
	Game() {
		try {
			position = Position.parse(Position.START_POSITION_FEN);
		} catch (ChessParseException e) { }
		startPosition = position.deepCopy();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
		round = 1;
		state = GameState.IN_PROGRESS;
	}
	/**
	 * Returns a deep copy of the starting position of the game.
	 * 
	 * @return
	 */
	Position getStartPos() {
		return startPosition;
	}
	/**
	 * Returns a deep copy of the current position. Changes made to the object are not reflected in the Game instance.
	 * 
	 * @return
	 */
	Position getPosition() {
		return position.deepCopy();
	}
	/**
	 * Returns the name of the event the game takes/took place at.
	 * 
	 * @return
	 */
	String getEvent() {
		return event;
	}
	/**
	 * Returns the name of the site where the game takes/took place.
	 * 
	 * @return
	 */
	String getSite() {
		return site;
	}
	/**
	 * Returns the date when the game took place.
	 * 
	 * @return
	 */
	String getDate() {
		return date;
	}
	/**
	 * Returns the round of the game.
	 * 
	 * @return
	 */
	int getRound() {
		return round;
	}
	/**
	 * Returns the white player's name.
	 * 
	 * @return
	 */
	String getWhitePlayerName() {
		return whitePlayerName;
	}
	/**
	 * Returns the black player's name.
	 * 
	 * @return
	 */
	String getBlackPlayerName() {
		return blackPlayerName;
	}
	/**
	 * Returns the side to move according to {@link #Side Side}.
	 * 
	 * @return
	 */
	Side getSideToMove() {
		return position.isWhitesTurn ? Side.WHITE : Side.BLACK;
	}
	/**
	 * Returns the game of the state according to {@link #uibase.GameState GameState}.
	 * 
	 * @return
	 */
	GameState getState() {
		return state;
	}
	/**
	 * Sets the event at which the game takes/took place.
	 * 
	 * @param event
	 */
	void setEvent(String event) {
		this.event = event;
	}
	/**
	 * Sets the site where the game takes/took place.
	 * 
	 * @param site
	 */
	void setSite(String site) {
		this.site = site;
	}
	/**
	 * Sets the name of the white player.
	 * 
	 * @param whitePlayerName
	 */
	void setWhitePlayerName(String whitePlayerName) {
		this.whitePlayerName = whitePlayerName;
	}
	/**
	 * Sets the name of the black player.
	 * 
	 * @param blackPlayerName
	 */
	void setBlackPlayerName(String blackPlayerName) {
		this.blackPlayerName = blackPlayerName;
	}
	/**
	 * Internally updates the state of the game. Should be called after a move has been successfully played.
	 */
	private void setState() {
		if (position.getMoves().size() == 0)
			state = position.isInCheck ? position.isWhitesTurn ? GameState.BLACK_MATES : GameState.WHITE_MATES : GameState.STALE_MATE;
		else {
			if (Evaluator.isMaterialInsufficient(position))
				state = GameState.DRAW_BY_INSUFFICIENT_MATERIAL;
			else if (position.getNumberOfRepetitions(0) >= 2)
				state = GameState.DRAW_BY_3_FOLD_REPETITION;
			else if (position.fiftyMoveRuleClock >= 100)
				state = GameState.DRAW_BY_50_MOVE_RULE;
			else
				state = GameState.IN_PROGRESS;
		}
	}
	/**
	 * Sets the state of the game in case of draw by agreement, resignation, or time out. Otherwise it is ignored.
	 * 
	 * @param state The new state of the game.
	 */
	void setState(GameState state) {
		if (state != null && (state == GameState.DRAW_BY_AGREEMENT || state == GameState.UNSPECIFIED_WHITE_WIN ||
				state == GameState.UNSPECIFIED_BLACK_WIN))
			this.state = state;
	}
	/**
	 * Plays a move defined either in PACN or SAN on the board if legal.
	 * 
	 * @param move The move to make defined either in pure algebraic coordinate notation or standard algebraic notation.
	 * @return Whether the move was legal and of valid format.
	 */
	boolean play(String move) {
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
			return true;
		}
		return false;
	}
	/**
	 * Unmakes the last move and returns it in Pure Algebraic Coordinate Notation. It returns null if no moves have been made yet.
	 * 
	 * @return
	 */
	String unplay() {
		Move m = position.unmakeMove();
		setState();
		return m == null ? null : m.toString();
	}
	/**
	 * Returns a string representation of the position's move list in SAN with six full moves per line.
	 * 
	 * @param p
	 * @return
	 */
	private String moveListToSAN() {
		String moveListSAN = "";
		boolean printRound = true;
		int roundNum = 0;
		ArrayDeque<Move> moves = new ArrayDeque<>();
		for (Move move : position.moveList) {
			moves.addFirst(move);
			position.unmakeMove();
		}
		for (Move move : moves) {
			if (roundNum%6 == 0 && printRound)
				moveListSAN += "\n";
			if (printRound)
				moveListSAN += ++roundNum + ". ";
			printRound = !printRound;
			moveListSAN += position.toSAN(move) + " ";
			position.makeMove(move);
		}
		return moveListSAN;
	}
	/**
	 * Returns a string of the game in PGN.
	 */
	@Override
	public String toString() {
		String pgn = "", result;
		pgn += "[Event \"" + (event == null ? "N/A" : event) + "\"]\n";
		pgn += "[Site \"" + (site == null ? "N/A" : site) + "\"]\n";
		pgn += "[Date \"" + date + "\"]\n";
		pgn += "[Round \"" + (round == -1 ? "?" : round) + "\"]\n";
		pgn += "[White \"" + (whitePlayerName == null ? "N/A" : whitePlayerName) + "\"]\n";
		pgn += "[Black \"" + (blackPlayerName == null ? "N/A" : blackPlayerName) + "\"]\n";
		result = "";
		if (state == GameState.IN_PROGRESS)
			result = "*";
		else if (state == GameState.WHITE_MATES || state == GameState.UNSPECIFIED_WHITE_WIN)
			result = "1-0";
		else if (state == GameState.BLACK_MATES || state == GameState.UNSPECIFIED_BLACK_WIN)
			result = "0-1";
		else
			result = "1/2-1/2";
		pgn += "[Result \"" + result + "\"]\n";
		pgn += "[FEN \"" + startPosition.toString() + "\"]\n";
		pgn += moveListToSAN();
		return pgn;
	}
	
	/**
	 * A simple enum for the two sides/colors in a chess game.
	 * 
	 * @author Viktor
	 *
	 */
	enum Side {
		
		WHITE,
		BLACK;
		
	}
	
}
