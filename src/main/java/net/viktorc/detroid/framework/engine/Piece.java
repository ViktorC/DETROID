package net.viktorc.detroid.framework.engine;

/**
 * An enum type for the different chess pieces. Each piece has a FEN notation character.
 *
 * @author Viktor
 */
public enum Piece {

  NULL('\u0000'),
  W_KING('K'),
  W_QUEEN('Q'),
  W_ROOK('R'),
  W_BISHOP('B'),
  W_KNIGHT('N'),
  W_PAWN('P'),
  B_KING('k'),
  B_QUEEN('q'),
  B_ROOK('r'),
  B_BISHOP('b'),
  B_KNIGHT('n'),
  B_PAWN('p');

  private final char letter;

  Piece(char fen) {
    this.letter = fen;
  }

  /**
   * @return A character denoting the piece-type in chess notation.
   */
  public char getLetter() {
    return letter;
  }

}
