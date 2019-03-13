package net.viktorc.detroid.framework.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A class representing a chess game. It can parse PGN strings and moves in both FEN and SAN format. It also allows for playing and
 * unplaying moves while keeping track of the state of the game.
 *
 * @author Viktor
 */
public class Game {

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
   * Parses a game in PGN notation and returns a game instance.
   *
   * @param pgn The PGN string.
   * @return The game instance.
   * @throws ChessParseException If the PGN string cannot be parsed.
   */
  public static Game parse(String pgn) throws ChessParseException {
    char tagChar;
    String tagContent, tagType, tagValue,
        event = null, site = null, date = null, round = null,
        whiteName = null, blackName = null, result = null, fen = null;
    Game out = new Game();
    String[] moveDescParts;
    ArrayList<String> sanStrings = new ArrayList<>();
    Move move;
    if (pgn == null) {
      return null;
    }
    try {
      for (int i = 0; i < pgn.length(); i++) {
        if (pgn.charAt(i) == '[') {
          tagContent = "";
          while (++i < pgn.length() && (tagChar = pgn.charAt(i)) != ']') {
            tagContent += tagChar;
          }
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
          whiteName == null || blackName == null || result == null) {
        throw new ChessParseException("Missing tag(s).");
      }
      out.event = event;
      out.site = site;
      out.date = date;
      out.round = "?".equals(round) ? -1 : Short.parseShort(round);
      out.whitePlayerName = whiteName;
      out.blackPlayerName = blackName;
      out.position = fen == null ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
      out.startPosition = new Position(out.position);
      pgn = pgn.substring(pgn.lastIndexOf(']') + 1);
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
          throw new ChessParseException("Invalid result.");
      }
      moveDescParts = pgn.split("[\\s]+");
      for (String s : moveDescParts) {
        s = s.trim();
        if (!s.isEmpty() && !s.matches("^\\$[0-9]+$")) {
          sanStrings.add(s);
        }
      }
      for (String sanString : sanStrings) {
        move = MoveStringUtils.parseSAN(out.position, sanString);
        out.position.makeMove(move);
      }
      out.updateState();
      if (out.state == GameState.IN_PROGRESS && !"*".equals(result)) {
        if ("1-0".equals(result)) {
          out.state = GameState.UNSPECIFIED_WHITE_WIN;
        } else if ("0-1".equals(result)) {
          out.state = GameState.UNSPECIFIED_BLACK_WIN;
        } else {
          out.state = GameState.DRAW_BY_AGREEMENT;
        }
      }
    } catch (Exception e) {
      throw new ChessParseException(e);
    }
    return out;
  }

  /**
   * @param position The start position of the game.
   * @param event The name of the event.
   * @param site The site of the event.
   * @param whitePlayerName The white player's name.
   * @param blackPlayerName The black player's name.
   * @param round The round number in case the game is one of a series of games.
   */
  public Game(Position position, String event, String site, String whitePlayerName, String blackPlayerName,
      int round) {
    try {
      this.position = Position.parse(position.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    startPosition = new Position(this.position);
    this.event = event;
    this.site = site;
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
    this.round = round;
    this.whitePlayerName = whitePlayerName;
    this.blackPlayerName = blackPlayerName;
    updateState();
  }

  /**
   * @param position The start position of the game.
   * @param event The name of the event.
   * @param site The site of the event.
   * @param whitePlayerName The white player's name.
   * @param blackPlayerName The black player's name.
   */
  Game(Position position, String event, String site, String whitePlayerName, String blackPlayerName) {
    this(position, event, site, whitePlayerName, blackPlayerName, -1);
  }

  /**
   * @param position The start position of the game.
   */
  Game(Position position) {
    this(position, null, null, null, null, -1);
  }

  /**
   * Default constructor.
   */
  Game() {
    try {
      position = Position.parse(Position.START_POSITION_FEN);
    } catch (ChessParseException e) {
      // Can't really happen.
      return;
    }
    startPosition = new Position(position);
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
    round = 1;
    state = GameState.IN_PROGRESS;
  }

  /**
   * @return A deep copy of the starting position of the game.
   */
  public Position getStartPos() {
    return new Position(startPosition);
  }

  /**
   * @return A deep copy of the current position
   */
  public Position getPosition() {
    return new Position(position);
  }

  /**
   * @return The name of the event the game took place at.
   */
  public String getEvent() {
    return event;
  }

  /**
   * @return The site of the event where the game took place.
   */
  public String getSite() {
    return site;
  }

  /**
   * @return The date the game took place.
   */
  public String getDate() {
    return date;
  }

  /**
   * @return The round of the game.
   */
  public int getRound() {
    return round;
  }

  /**
   * @return The white player's name.
   */
  public String getWhitePlayerName() {
    return whitePlayerName;
  }

  /**
   * @return The black player's name.
   */
  public String getBlackPlayerName() {
    return blackPlayerName;
  }

  /**
   * @return The state of the game.
   */
  public GameState getState() {
    return state;
  }

  /**
   * @return Whether it is white's turn to make a move.
   */
  public boolean isWhitesTurn() {
    return position.isWhitesTurn();
  }

  /**
   * @param event The name of the event at which the game took place.
   */
  public void setEvent(String event) {
    this.event = event;
  }

  /**
   * @param site The site of the event where the game took place.
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * @param whitePlayerName The name of the white player.
   */
  public void setWhitePlayerName(String whitePlayerName) {
    this.whitePlayerName = whitePlayerName;
  }

  /**
   * @param blackPlayerName The name of the black player.
   */
  public void setBlackPlayerName(String blackPlayerName) {
    this.blackPlayerName = blackPlayerName;
  }

  /**
   * Sets the state of the game in case of draw by agreement, resignation, or time out. Otherwise it is a no-op.
   *
   * @param state The new state of the game.
   */
  public void setState(GameState state) {
    if (state != null && (state == GameState.DRAW_BY_AGREEMENT || state == GameState.UNSPECIFIED_WHITE_WIN ||
        state == GameState.UNSPECIFIED_BLACK_WIN)) {
      this.state = state;
    }
  }

  private void updateState() {
    if (position.getMoves().size() == 0) {
      state = position.isInCheck() ?
          (position.isWhitesTurn() ? GameState.BLACK_MATES : GameState.WHITE_MATES) :
          GameState.STALE_MATE;
    } else {
      if (Evaluator.isMaterialInsufficient(position)) {
        state = GameState.DRAW_BY_INSUFFICIENT_MATERIAL;
      } else if (position.hasRepeated(2)) {
        state = GameState.DRAW_BY_3_FOLD_REPETITION;
      } else if (position.getFiftyMoveRuleClock() >= 100) {
        state = GameState.DRAW_BY_50_MOVE_RULE;
      } else {
        state = GameState.IN_PROGRESS;
      }
    }
  }

  /**
   * Plays a move defined either in PACN or SAN format if legal.
   *
   * @param move The move to make defined either in Pure Algebraic Coordinate Notation or Standard Algebraic Notation.
   * @return Whether the move was legal and of valid format.
   */
  public boolean play(String move) {
    Move m;
    try {
      m = MoveStringUtils.parsePACN(position, move);
    } catch (ChessParseException | NullPointerException e) {
      try {
        m = MoveStringUtils.parseSAN(position, move);
      } catch (ChessParseException | NullPointerException e1) {
        return false;
      }
    }
    if (position.getMoves().contains(m)) {
      position.makeMove(m);
      updateState();
      return true;
    }
    return false;
  }

  /**
   * Unmakes the last move and returns it in Pure Algebraic Coordinate Notation. It returns null if no moves have been made yet.
   *
   * @return The last move PACN format.
   */
  public String unplay() {
    Move m = position.unmakeMove();
    updateState();
    return m == null ? null : m.toString();
  }

  private String moveListToSAN() {
    String moveListSAN = "";
    boolean printRound = true;
    int roundNum = 0;
    Position posCopy = new Position(position);
    ArrayDeque<Move> moves = new ArrayDeque<>();
    for (Move move : posCopy.getMoveHistory()) {
      moves.addFirst(move);
      posCopy.unmakeMove();
    }
    for (Move move : moves) {
      if (roundNum % 6 == 0 && printRound) {
        moveListSAN += "\n";
      }
      if (printRound) {
        moveListSAN += ++roundNum + ". ";
      }
      printRound = !printRound;
      moveListSAN += MoveStringUtils.toSAN(posCopy, move) + " ";
      posCopy.makeMove(move);
    }
    return moveListSAN;
  }

  @Override
  public String toString() {
    String pgn = "", result;
    pgn += "[Event \"" + (event == null ? "N/A" : event) + "\"]\n";
    pgn += "[Site \"" + (site == null ? "N/A" : site) + "\"]\n";
    pgn += "[Date \"" + date + "\"]\n";
    pgn += "[Round \"" + (round == -1 ? "?" : round) + "\"]\n";
    pgn += "[White \"" + (whitePlayerName == null ? "N/A" : whitePlayerName) + "\"]\n";
    pgn += "[Black \"" + (blackPlayerName == null ? "N/A" : blackPlayerName) + "\"]\n";
    if (state == GameState.IN_PROGRESS) {
      result = "*";
    } else if (state == GameState.WHITE_MATES || state == GameState.UNSPECIFIED_WHITE_WIN) {
      result = "1-0";
    } else if (state == GameState.BLACK_MATES || state == GameState.UNSPECIFIED_BLACK_WIN) {
      result = "0-1";
    } else {
      result = "1/2-1/2";
    }
    pgn += "[Result \"" + result + "\"]\n";
    pgn += "[FEN \"" + startPosition.toString() + "\"]\n";
    pgn += moveListToSAN();
    return pgn;
  }

}
