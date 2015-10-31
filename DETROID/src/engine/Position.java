package engine;

import util.*;
import engine.Board.*;
import engine.Move.*;

import java.util.Scanner;

/**A bit board based class whose object holds information amongst others on the current board position, on all the previous moves and positions,
 * on castling and en passant rights, and on the player to move. It uses a pre-calculated 'magic' move database to avoid the cost of computing the
 * possible move sets of sliding pieces on the fly.
 * 
 * The main functions include:
 * {@link #generateAllMoves() generateMoves}
 * {@link #makeMove(Move) makeMove}
 * {@link #unmakeMove() unmakeMove}
 * {@link #perft(int) perft}
 * {@link #divide(int) divide}
 *  
 * @author Viktor
 * 
 */
public class Position implements Hashable {
	
	/**A FEN string for the starting chess position.*/
	public final static String INITIAL_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	
	/**A simple enum type for the representation of a side's castling rights in a position.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum CastlingRights {
		
		NONE,
		SHORT,
		LONG,
		ALL;
		
		public final byte ind;	//numeric representation of the the castling rights
		
		private CastlingRights() {
			ind = (byte)ordinal();
		}
		/**Returns a CastlingRights type based on the argument numeral.
		 * 
		 * @param num
		 * @return
		 */
		public static CastlingRights getByIndex(int num) {
			switch (num) {
			case 0: return NONE; case 1: return SHORT; case 2: return LONG; case 3: return ALL;
			default: throw new IllegalArgumentException();
		}
		}
		/**Parses a string in FEN notation and returns an array of two containing white's and black's castling rights respectively.
		 * 
		 * @param fen
		 * @return
		 */
		public static CastlingRights[] getInstancesFromFen(String fen) {
			if (fen == null)
				return new CastlingRights[] { null, null };
			if (fen.equals("-"))
				return new CastlingRights[] { CastlingRights.NONE, CastlingRights.NONE };
			CastlingRights whiteCastlingRights, blackCastlingRights;
			if (fen.contains("K")) {
				if (fen.contains("Q"))
					whiteCastlingRights = CastlingRights.ALL;
				else
					whiteCastlingRights = CastlingRights.SHORT;
			}
			else if (fen.contains("Q"))
				whiteCastlingRights = CastlingRights.LONG;
			else
				whiteCastlingRights = CastlingRights.NONE;
			if (fen.contains("k")) {
				if (fen.contains("q"))
					blackCastlingRights = CastlingRights.ALL;
				else
					blackCastlingRights = CastlingRights.SHORT;
			}
			else if (fen.contains("q"))
				blackCastlingRights = CastlingRights.LONG;
			else
				blackCastlingRights = CastlingRights.NONE;
			return new CastlingRights[] { whiteCastlingRights, blackCastlingRights };
		}
		/**Returns a string representation in FEN notation of two castling right enum types for white and black respectively.
		 * 
		 * @param white
		 * @param black
		 * @return
		 */
		public static String toFen(CastlingRights white, CastlingRights black) {
			if (white == null || black == null)
				return null;
			String out = "";
			switch (white) {
				case NONE:
				break;
				case SHORT:
					out += "K";
				break;
				case LONG:
					out += "Q";
				break;
				case ALL:
					out += "KQ";
			}
			switch (white) {
				case NONE:
				break;
				case SHORT:
					out += "k";
				break;
				case LONG:
					out += "q";
				break;
				case ALL:
					out += "kq";
			}
			if (out.equals(""))
				return "-";
			return out;
		}
	}
	
	/**A simple enum type for the representation of a side's en passant rights in a position.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum EnPassantRights {
		
		A, B, C, D, E, F, G, H,
		NONE;
		
		public final byte ind;										//numeric representation of the the en passant rights
		public final static byte TO_W_DEST_SQR_IND = Square.A6.ind;	//the difference between the en passant right index and the square index of the destination of en passant for white
		public final static byte TO_W_VICT_SQR_IND = Square.A5.ind;	//the difference between the en passant right index and the square index of the possible vicim of en passant for white
		public final static byte TO_B_DEST_SQR_IND = Square.A3.ind;	//the difference between the en passant right index and the square index of the destination of en passant for black
		public final static byte TO_B_VICT_SQR_IND = Square.A4.ind;	//the difference between the en passant right index and the square index of the possible vicim of en passant for black
		
		private EnPassantRights() {
			ind = (byte)ordinal();
		}
		/**Returns a EnPassantRights type based on the argument numeral.
		 * 
		 * @param num
		 * @return
		 */
		public static EnPassantRights getByIndex(int num) {
			switch (num) {
				case 0: return A; case 1: return B; case 2: return C; case 3: return D; case 4: return E;
				case 5: return F; case 6: return G; case 7: return H; case 8: return NONE;
				default: throw new IllegalArgumentException();
			}
		}
		/**Parses a string in FEN notation and returns an EnPassantRights type.
		 * 
		 * @param fen
		 * @return
		 */
		public static EnPassantRights getByFen(String fen) {
			if (fen == null || fen.length() < 2)
				return null;
			if (fen.equals("-"))
				return NONE;
			return values()[fen.toLowerCase().charAt(0) - 'a'];
		}
		/**Returns a string representation.*/
		public String toString() {
			if (this == NONE)
				return "-";
			else
				return super.toString().toLowerCase();
		}
	}
	
	//bitboards for each piece type
	long whiteKing;
	long whiteQueens;
	long whiteRooks;
	long whiteBishops;
	long whiteKnights;
	long whitePawns;
	
	long blackKing;
	long blackQueens;
	long blackRooks;
	long blackBishops;
	long blackKnights;
	long blackPawns;
	
	//bitboard unions maintained for faster processing of positions
	private long allWhiteOccupied;
	private long allBlackOccupied;
	
	private long allNonWhiteOccupied;
	private long allNonBlackOccupied;
	
	private long allOccupied;
	private long allEmpty;
	
	private int[] offsetBoard;															//a complimentary board data-structure to the bitboards to efficiently detect pieces on specific squares
	
	boolean whitesTurn = true;															//denotes whether it is white's turn to make a move, or not, i.e. it is black's
	
	private long checkers = 0;															//a bitmap of all the pieces that attack the color to move's king
	private boolean check = false;														//denotes whether the color to move's king is in check or not
	
	private int halfMoveIndex = 0;														//the count of the current ply/half-move
	private int fiftyMoveRuleClock = 0;													//the number of moves made since the last pawn move or capture; the choice of type fell on long due to data loss when int is shifted beyond the 32nd bit in the move integer
	
	int enPassantRights = EnPassantRights.NONE.ind;									//denotes the file on which en passant is possible; 8 means no en passant rights
	
	int whiteCastlingRights = CastlingRights.ALL.ind;								//denotes to what extent it would still be possible to castle regardless of whether it is actually legally executable in the current position
	int blackCastlingRights = CastlingRights.ALL.ind;								//0 - no castling rights, 1 - king-side castling only, 2 - queen-side castling only, 3 - all castling rights
	
	private Stack<Move> moveList = new Stack<Move>();									//a stack of all the moves made so far
	private Stack<UnmakeRegister> unmakeRegisterHistory = new Stack<UnmakeRegister>();	//a stack history of castling rights, en passant rights, fifty-move rule clock, repetitions, and check info.
	
	long key;																			//the Zobrist key that is fairly close to a unique representation of the state of the Board instance in one number
	private long[] keyHistory;															//all the positions that have occured so far represented in Zobrist keys.
	
	private int repetitions = 0;														//the number of times the current position has occured before; the choice of type fell on long due to data loss when int is shifted beyond the 32nd bit in the move integer
	
	/**Initializes an instance of Board and sets up the pieces in their initial position.*/
	public Position() {
		this(INITIAL_POSITION_FEN);
	}
	/**It parses a FEN-String and sets the instance fields accordingly.
	 * Beside standard six-field FEN-Strings, it also accepts four-field Strings without the fifty-move rule clock and the move index.
	 * 
	 * @param fen
	 */
	public Position(String fen) {
		String[] fenFields = fen.split(" "), ranks;
		String board, turn, castling, enPassant, rank;
		char piece;
		int pieceNum, index = 0, fiftyMoveRuleClock, moveIndex;
		CastlingRights[] castlingRights;
		EnPassantRights enPassantRights;
		if (fenFields.length == 6) {
			try {
				fiftyMoveRuleClock = Integer.parseInt(fenFields[4]);
				if (fiftyMoveRuleClock >= 0)
					this.fiftyMoveRuleClock = fiftyMoveRuleClock;
				else
					this.fiftyMoveRuleClock = 0;
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("The fifty-move rule clock field of the FEN-string does not conform to the standards. Parsing not possible.");
			}
			try {
				moveIndex = (Integer.parseInt(fenFields[5]) - 1)*2;
				if (!this.whitesTurn)
					moveIndex++;
				if (moveIndex >= 0)
					halfMoveIndex = moveIndex;
				else
					halfMoveIndex = 0;
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("The move index field does not conform to the standards. Parsing not possible.");
			}
		}
		else if (fenFields.length != 4)
			throw new IllegalArgumentException("The FEN-String has an unallowed number of fields.");
		board = fenFields[0];
		turn = fenFields[1];
		castling = fenFields[2];
		enPassant = fenFields[3];
		ranks = board.split("/");
		if (ranks.length != 8)
			throw new IllegalArgumentException("The board position representation does not have eight ranks.");
		offsetBoard = new int[64];
		for (int i = 0; i < 64; i++)
			offsetBoard[i] = Piece.NULL.ind;
		for (int i = 7; i >= 0; i--) {
			rank = ranks[i];
			for (int j = 0; j < rank.length(); j++) {
				piece = rank.charAt(j);
				pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					offsetBoard[index] = Piece.getByFenNotation(piece).ind;
					switch (piece) {
						case 'K': {
							whiteKing = Square.getByIndex(index).bitmap;
						}
						break;
						case 'Q': {
							whiteQueens	|= Square.getByIndex(index).bitmap;
						}
						break;
						case 'R': {
							whiteRooks |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'B': {
							whiteBishops |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'N': {
							whiteKnights |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'P': {
							whitePawns |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'k': {
							blackKing = Square.getByIndex(index).bitmap;
						}
						break;
						case 'q': {
							blackQueens |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'r': {
							blackRooks |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'b': {
							blackBishops |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'n': {
							blackKnights |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'p': {
							blackPawns |= Square.getByIndex(index).bitmap;
						}
					}
					index++;
				}
			}
		}
		allWhiteOccupied = whiteKing | whiteQueens | whiteRooks | whiteBishops | whiteKnights | whitePawns;
		allBlackOccupied = blackKing | blackQueens | blackRooks | blackBishops | blackKnights | blackPawns;
		allNonWhiteOccupied = ~allWhiteOccupied;
		allNonBlackOccupied = ~allBlackOccupied;
		allOccupied	=  allWhiteOccupied | allBlackOccupied;
		allEmpty = ~allOccupied;
		if (turn.toLowerCase().compareTo("w") == 0)
			whitesTurn = true;
		else
			whitesTurn = false;
		castlingRights = CastlingRights.getInstancesFromFen(castling);
		whiteCastlingRights = castlingRights[0] != null ? castlingRights[0].ind : whiteCastlingRights;
		blackCastlingRights = castlingRights[1] != null ? castlingRights[1].ind : blackCastlingRights;
		enPassantRights = EnPassantRights.getByFen(enPassant);
		this.enPassantRights = enPassantRights != null ? enPassantRights.ind : this.enPassantRights;
		setCheck();
		//"The longest decisive tournament game is Fressinet-Kosteniuk, Villandry 2007, which Kosteniuk won in 237 moves." - half of that is used as the initial length of the history array
		keyHistory = new long[237];
		key = Zobrist.hash(this);
		keyHistory[0] = this.key;
	}
	/**Returns a copy of the position.
	 * 
	 * @return
	 */
	public Position copy() {
		Position copy = new Position();
		copy.keyHistory = new long[keyHistory.length];
		Stack<Move> reverse = new Stack<>();
		while (moveList.hasNext())
			reverse.add(moveList.next());
		while (reverse.hasNext())
			copy.makeMove(reverse.next());
		return copy;
	}
	/**Returns an array of longs representing the current position with each array element denoting a square and the value in the element denoting the piece on the square.*/
	public int[] getOffsetBoard() {
		return offsetBoard;
	}
	/**Returns whether it is white's turn or not.*/
	public boolean getTurn() {
		return whitesTurn;
	}
	/**Returns whether the color to move's king is in check.*/
	public boolean getCheck() {
		return check;
	}
	/**Returns the current ply/half-move index.*/
	public int getHalfMoveIndex() {
		return halfMoveIndex;
	}
	/**Returns the number of half-moves made since the last pawn-move or capture.*/
	public long getFiftyMoveRuleClock() {
		return fiftyMoveRuleClock;
	}
	/**Returns a number denoting white's castling rights according to the {@link #engine.Position.CastlingRights CastlingRights} definition.*/
	public int getWhiteCastlingRights() {
		return whiteCastlingRights;
	}
	/**Returns a number denoting black's castling rights according to the {@link #engine.Position.CastlingRights CastlingRights} definition.*/
	public int getBlackCastlingRights() {
		return blackCastlingRights;
	}
	/**Returns a number denoting  the en passant rights of the current position according to the {@link #engine.Position.EnPassantRights EnPassantRights} definition.*/
	public int getEnPassantRights() {
		return enPassantRights;
	}
	/**Returns the number of times the current position has previously occured since the initialization of the object.*/
	public long getRepetitions() {
		return repetitions;
	}
	/**Returns the 64-bit Zobrist key of the current position. A Zobrist key is used to almost uniquely hash a chess position to an integer.*/
	public long key() {
		return key;
	}
	/**Returns an object containing all relevant information about the last move made. If the move history list is empty, it returns null.*/
	public Move getLastMove() {
		return moveList.getHead();
	}
	/**Returns an object containing some information about the previous position.*/
	public UnmakeRegister getUnmakeRegister() {
		return unmakeRegisterHistory.getHead();
	}
	private void setBitboards(int moved, int captured, long fromBit, long toBit) {
		if (whitesTurn) {
			PseudoSwitch1: {
				if (moved == Piece.W_KING.ind) {
					whiteKing ^= fromBit;
					whiteKing ^= toBit;
				}
				else if (moved == Piece.W_QUEEN.ind) {
					whiteQueens ^= fromBit;
					whiteQueens ^= toBit;
				}
				else if (moved == Piece.W_ROOK.ind) {
					whiteRooks ^= fromBit;
					whiteRooks ^= toBit;
				}
				else if (moved == Piece.W_BISHOP.ind) {
					whiteBishops ^= fromBit;
					whiteBishops ^= toBit;
				}
				else if (moved == Piece.W_KNIGHT.ind) {
					whiteKnights ^= fromBit;
					whiteKnights ^= toBit;
				}
				else if (moved == Piece.W_PAWN.ind) {
					whitePawns ^= fromBit;
					whitePawns ^= toBit;
				}
				else break PseudoSwitch1;
				allWhiteOccupied ^= fromBit;
				allWhiteOccupied ^= toBit;
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			PseudoSwitch2: {
				if (captured == Piece.NULL.ind) break PseudoSwitch2;
				else if (captured == Piece.B_QUEEN.ind)
					blackQueens ^= toBit;
				else if (captured == Piece.B_ROOK.ind)
					blackRooks ^= toBit;
				else if (captured == Piece.B_BISHOP.ind)
					blackBishops ^= toBit;
				else if (captured == Piece.B_KNIGHT.ind)
					blackKnights ^= toBit;
				else if (captured == Piece.B_PAWN.ind)
					blackPawns ^= toBit;
				allBlackOccupied ^= toBit;
				allNonBlackOccupied = ~allBlackOccupied;
			}
			allOccupied = allWhiteOccupied | allBlackOccupied;
			allEmpty = ~allOccupied;
		}
		else {
			PseudoSwitch1: {
				if (moved == Piece.B_KING.ind) {
					blackKing ^= fromBit;
					blackKing ^= toBit;
				}
				else if (moved == Piece.B_QUEEN.ind) {
					blackQueens ^= fromBit;
					blackQueens ^= toBit;
				}
				else if (moved == Piece.B_ROOK.ind) {
					blackRooks ^= fromBit;
					blackRooks ^= toBit;
				}
				else if (moved == Piece.B_BISHOP.ind) {
					blackBishops ^= fromBit;
					blackBishops ^= toBit;
				}
				else if (moved == Piece.B_KNIGHT.ind) {
					blackKnights ^= fromBit;
					blackKnights ^= toBit;
				}
				else if (moved == Piece.B_PAWN.ind) {
					blackPawns ^= fromBit;
					blackPawns ^= toBit;
				}
				else break PseudoSwitch1;
				allBlackOccupied ^= fromBit;
				allBlackOccupied ^= toBit;
				allNonBlackOccupied = ~allBlackOccupied;
			}
			PseudoSwitch2: {
				if (captured == Piece.NULL.ind) break PseudoSwitch2;
				else if (captured == Piece.W_QUEEN.ind)
					whiteQueens ^= toBit;
				else if (captured == Piece.W_ROOK.ind)
					whiteRooks ^= toBit;
				else if (captured == Piece.W_BISHOP.ind)
					whiteBishops ^= toBit;
				else if (captured == Piece.W_KNIGHT.ind)
					whiteKnights ^= toBit;
				else if (captured == Piece.W_PAWN.ind)
					whitePawns ^= toBit;
				allWhiteOccupied ^= toBit;
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			allOccupied = allWhiteOccupied | allBlackOccupied;
			allEmpty = ~allOccupied;
		}
	}
	private void setTurn() {
		whitesTurn = !whitesTurn;
	}
	private void setMoveIndices(int moved, int captured) {
		halfMoveIndex++;
		if (captured != Piece.NULL.ind || moved == Piece.W_PAWN.ind || moved == Piece.B_PAWN.ind)
			fiftyMoveRuleClock = 0;
		else
			fiftyMoveRuleClock++;
	}
	private void setEnPassantRights(int from, int to, int movedPiece) {
		if (movedPiece == Piece.W_PAWN.ind) {
			if (to - from == 16) {
				enPassantRights = to%8;
				return;
			}
		}
		else if (movedPiece == Piece.B_PAWN.ind) {
			if (from - to == 16) {
				enPassantRights = to%8;
				return;
			}
		}
		enPassantRights = 8;
	}
	private void setCastlingRights() {
		if (whitesTurn) {
			if (whiteCastlingRights == CastlingRights.NONE.ind) return;
			if (whiteCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			else if (whiteCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			else {
				if (offsetBoard[Square.E1.ind] == Piece.W_KING.ind) {
					if (offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind) {
						if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
							whiteCastlingRights = CastlingRights.NONE.ind;
						else
							whiteCastlingRights = CastlingRights.LONG.ind;
					}
					else if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
						whiteCastlingRights = CastlingRights.SHORT.ind;
				}
				else
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
		}
		else {
			if (blackCastlingRights == CastlingRights.NONE.ind) return;
			if (blackCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			else if (blackCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			else {
				if (offsetBoard[Square.E8.ind] == Piece.B_KING.ind) {
					if (offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind) {
						if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
							blackCastlingRights = CastlingRights.NONE.ind;
						else
							blackCastlingRights = CastlingRights.LONG.ind;
					}
					else if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
						blackCastlingRights = CastlingRights.SHORT.ind;
				}
				else
					blackCastlingRights = CastlingRights.NONE.ind;
			}
		}
	}
	private void setCheck() {
		checkers = getCheckers();
		check = (checkers != 0) ? true : false;
	}
	private void setKeys() {
		key = Zobrist.updateKey(this);
		keyHistory[halfMoveIndex] = key;
	}
	private void extendKeyHistory() {
		long[] temp;
		if (keyHistory.length - halfMoveIndex <= 75) {
			temp = keyHistory;
			keyHistory = new long[keyHistory.length + 25];
			for (int i = 0; i < temp.length; i++)
				keyHistory[i] = temp[i];
		}
	}
	/**Should be used before resetMoveIndices().*/
	private void setRepetitions() {
		if (fiftyMoveRuleClock >= 4) {
			for (int i = halfMoveIndex; i >= (halfMoveIndex - fiftyMoveRuleClock); i -= 2) {
				if (keyHistory[i] == key)
					repetitions++;
			}
		}
		else
			repetitions = 0;
	}
	/**Returns whether there are any pieces of the color defined by byWhite that could be, in the current position, legally moved to the supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public boolean isAttacked(int sqrInd, boolean byWhite) {
		MoveTable dB = MoveTable.getByIndex(sqrInd);
		if (byWhite) {
			if ((whiteKing & dB.getCrudeKingMoves()) != 0)
				return true;
			if ((whiteKnights & dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((whitePawns & dB.getCrudeBlackPawnCaptures()) != 0)
				return true;
			if (((whiteQueens | whiteRooks) & dB.getBlackRookMoves(allNonBlackOccupied, allOccupied)) != 0)
				return true;
			if (((whiteQueens | whiteBishops) & dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied)) != 0)
				return true;
			if (offsetBoard[sqrInd] == Piece.B_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights) {
				if ((whitePawns & dB.getCrudeKingMoves() & Rank.R5.bitmap) != 0)
					return true;
			}
		}
		else {
			if ((blackKing & dB.getCrudeKingMoves()) != 0)
				return true;
			if ((blackKnights & dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((blackPawns & dB.getCrudeWhitePawnCaptures()) != 0)
				return true;
			if (((blackQueens | blackRooks) & dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied)) != 0)
				return true;
			if (((blackQueens | blackBishops) & dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied)) != 0)
				return true;
			if (offsetBoard[sqrInd] == Piece.W_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights) {
				if ((blackPawns & dB.getCrudeKingMoves() & Rank.R4.bitmap) != 0)
					return true;
			}
		}
		return false;
	}
	/**Returns whether there are any sliding pieces of the color defined by byWhite that could be, in the current position, legally moved to the supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public boolean isAttackedBySliders(int sqrInd, boolean byWhite) {
		MoveTable dB = MoveTable.getByIndex(sqrInd);
		if (byWhite) {
			if (((whiteQueens | whiteRooks) & dB.getBlackRookMoves(allNonBlackOccupied, allOccupied)) != 0)
				return true;
			if (((whiteQueens | whiteBishops) & dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied)) != 0)
				return true;
		}
		else {
			if (((blackQueens | blackRooks) & dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied)) != 0)
				return true;
			if (((blackQueens | blackBishops) & dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied)) != 0)
				return true;
		}
		return false;
	}
	/**Returns a bitmap representing all the squares on which the pieces are of the color defined by byWhite and in the current position could legally be moved to the supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public long getAttackers(int sqrInd, boolean byWhite) {
		long attackers = 0;
		MoveTable dB = MoveTable.getByIndex(sqrInd);
		if (byWhite) {
			attackers = whiteKing & dB.getCrudeKingMoves();
			attackers |= whiteKnights & dB.getCrudeKnightMoves();
			attackers |= whitePawns & dB.getCrudeBlackPawnCaptures();
			attackers |= (whiteQueens | whiteRooks) & dB.getBlackRookMoves(allNonBlackOccupied, allOccupied);
			attackers |= (whiteQueens | whiteBishops) & dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied);
			if (enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
				attackers |=  whitePawns & dB.getCrudeKingMoves() & Rank.R5.bitmap;
		}
		else {
			attackers = blackKing & dB.getCrudeKingMoves();
			attackers |= blackKnights & dB.getCrudeKnightMoves();
			attackers |= blackPawns & dB.getCrudeWhitePawnCaptures();
			attackers |= (blackQueens | blackRooks) & dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied);
			attackers |= (blackQueens | blackBishops) & dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied);
			if (enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
				attackers |=  blackPawns & dB.getCrudeKingMoves() & Rank.R4.bitmap;
		}
		return attackers;
	}
	/**Returns a long representing all the squares on which the pieces are of the color defined by byWhite and in the current position could legally be moved to the supposedly empty square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public long getBlockerCandidates(int sqrInd, boolean byWhite) {
		long blockerCandidates = 0;
		long sqrBit = 1L << sqrInd;
		long blackPawnAdvance = (sqrBit >>> 8), whitePawnAdvance = (sqrBit << 8);
		if (byWhite) {
			MoveTable dB = MoveTable.getByIndex(sqrInd);
			blockerCandidates |= whiteKnights & dB.getCrudeKnightMoves();
			blockerCandidates |= whitePawns & blackPawnAdvance;
			if ((sqrBit & Rank.R4.bitmap) != 0 && (allEmpty & blackPawnAdvance) != 0)
				blockerCandidates |= whitePawns & (blackPawnAdvance >>> 8);
			blockerCandidates |= (whiteQueens | whiteRooks) & dB.getBlackRookMoves(allNonBlackOccupied, allOccupied);
			blockerCandidates |= (whiteQueens | whiteBishops) & dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R6.bitmap) != 0)
				blockerCandidates |=  whitePawns & dB.getCrudeBlackPawnCaptures();
		}
		else {
			MoveTable dB = MoveTable.getByIndex(sqrInd);
			blockerCandidates |= blackKnights & dB.getCrudeKnightMoves();
			blockerCandidates |= blackPawns & whitePawnAdvance;
			if ((sqrBit & Rank.R5.bitmap) != 0 && (allEmpty & whitePawnAdvance) != 0)
				blockerCandidates |= blackPawns & (whitePawnAdvance << 8);
			blockerCandidates |= (blackQueens | blackRooks) & dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied);
			blockerCandidates |= (blackQueens | blackBishops) & dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R3.bitmap) != 0)
				blockerCandidates |=  blackPawns & dB.getCrudeWhitePawnCaptures();
		}
		return blockerCandidates;
	}
	/**Returns a bitmap representing the attackers of the color to move's king.
	 * 
	 * @return
	 */
	public long getCheckers() {
		long attackers = 0;
		int sqrInd;
		MoveTable dB;
		if (this.whitesTurn) {
			sqrInd = BitOperations.indexOfBit(whiteKing);
			dB = MoveTable.getByIndex(sqrInd);
			attackers = blackKnights & dB.getCrudeKnightMoves();
			attackers |= blackPawns & dB.getCrudeWhitePawnCaptures();
			attackers |= (blackQueens | blackRooks) & dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied);
			attackers |= (blackQueens | blackBishops) & dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied);
		}
		else {
			sqrInd = BitOperations.indexOfBit(blackKing);
			dB = MoveTable.getByIndex(sqrInd);
			attackers = whiteKnights & dB.getCrudeKnightMoves();
			attackers |= whitePawns & dB.getCrudeBlackPawnCaptures();
			attackers |= (whiteQueens | whiteRooks) & dB.getBlackRookMoves(allNonBlackOccupied, allOccupied);
			attackers |= (whiteQueens | whiteBishops) & dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied);
		}
		return attackers;
	}
	/**Returns a long representing all the squares on which there are pinned pieces of the color defined by forWhite in the current position. A pinned piece is one that when moved would expose its king to a check.
	 * 
	 * @param forWhite
	 * @return
	 */
	public long getPinnedPieces(boolean forWhite) {
		long rankPos, rankNeg, filePos, fileNeg, diagonalPos, diagonalNeg, antiDiagonalPos, antiDiagonalNeg;
		long straightSliders, diagonalSliders, pinnedPiece, pinnedPieces = 0;
		RayMask attRayMask;
		if (forWhite) {
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(this.whiteKing));
			rankPos = attRayMask.rankPos & allOccupied;
			rankNeg = attRayMask.rankNeg & allOccupied;
			filePos = attRayMask.filePos & allOccupied;
			fileNeg = attRayMask.fileNeg & allOccupied;
			diagonalPos = attRayMask.diagonalPos & allOccupied;
			diagonalNeg = attRayMask.diagonalNeg & allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos & allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg & allOccupied;
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		else {
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			rankPos = attRayMask.rankPos & allOccupied;
			rankNeg = attRayMask.rankNeg & allOccupied;
			filePos = attRayMask.filePos & allOccupied;
			fileNeg = attRayMask.fileNeg & allOccupied;
			diagonalPos = attRayMask.diagonalPos & allOccupied;
			diagonalNeg = attRayMask.diagonalNeg & allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos & allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg & allOccupied;
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		return pinnedPieces;
	}
	/**Returns whether a move would give check or not in this position.
	 * 
	 * @param move
	 * @return Whether the move would give check or not in this position.
	 */
	public boolean givesCheck(Move move) {
		MoveTable db;
		long toBit = 1L << move.to;
		boolean givesCheck = false;
		if (whitesTurn) {
			db = MoveTable.getByIndex(BitOperations.indexOfBit(blackKing));
			switch (move.movedPiece) {
				case 2: {
					if ((db.getWhiteQueenMoves(allNonWhiteOccupied, allOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 3: {
					if ((db.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 4: {
					if ((db.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 5: {
					if ((db.getWhiteKnightMoves(allNonWhiteOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 6: {
					if ((db.getCrudeBlackPawnCaptures() & toBit) != 0)
						givesCheck = true;
				}
			}
		}
		else {
			db = MoveTable.getByIndex(BitOperations.indexOfBit(whiteKing));
			switch (move.movedPiece) {
				case 8: {
					if ((db.getBlackQueenMoves(allNonBlackOccupied, allOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 9: {
					if ((db.getBlackRookMoves(allNonBlackOccupied, allOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 10: {
					if ((db.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 11: {
					if ((db.getBlackKnightMoves(allNonBlackOccupied) & toBit) != 0)
						givesCheck = true;
				}
				break;
				case 12: {
					if ((db.getCrudeWhitePawnCaptures() & toBit) != 0)
						givesCheck = true;
				}
			}
		}
		return givesCheck;
	}
	/**A method that returns an array of bitmaps of the squares from which the opponent's king can be checked for each piece type for
	 * the side to move (king obviously excluded).
	 * 
	 * @return An array of bitboards of squares from where the king can be checked  for each piece type for the side to move.
	 */
	public long[] squaresToCheckFrom() {
		MoveTable kingDB;
		long[] threatSquares = new long[5];
		if (whitesTurn) {
			kingDB = MoveTable.getByIndex(BitOperations.indexOfBit(blackKing));
			threatSquares[0] = kingDB.getWhiteQueenMoves(allNonWhiteOccupied, allOccupied);
			threatSquares[1] = kingDB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied);
			threatSquares[2] = kingDB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied);
			threatSquares[3] = kingDB.getWhiteKnightMoves(allNonWhiteOccupied);
			threatSquares[4] = kingDB.getCrudeBlackPawnCaptures();
		}
		else {
			kingDB = MoveTable.getByIndex(BitOperations.indexOfBit(whiteKing));
			threatSquares[0] = kingDB.getBlackQueenMoves(allNonBlackOccupied, allOccupied);
			threatSquares[1] = kingDB.getBlackRookMoves(allNonBlackOccupied, allOccupied);
			threatSquares[2] = kingDB.getBlackBishopMoves(allNonBlackOccupied, allOccupied);
			threatSquares[3] = kingDB.getBlackKnightMoves(allNonBlackOccupied);
			threatSquares[4] = kingDB.getCrudeWhitePawnCaptures();
		}
		return threatSquares;
	}
	/**Generates and adds all pinned-piece-moves to the input parameter 'moves' and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addAllPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0;
		int pinnedPieceInd, pinnedPiece, to;
		IntStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - whiteKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd + 7)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd + 7) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd + 9)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd + 9) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((whiteKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - blackKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], 0));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], 0));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((blackKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (this.enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd - 7)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd - 7) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd - 9)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd - 9) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**Generates and adds the tactical (captures/promotions) pinned-piece-moves to the input parameter 'moves' and returns the set of
	 * pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @param rookCheckSquares
	 * @param bishopCheckSquares
	 * @param pawnCheckSquares
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addTacticalPinnedPieceMoves(List<Move> moves, long rookCheckSquares, long bishopCheckSquares, long pawnCheckSquares) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0, moveSet;
		int pinnedPieceInd, pinnedPiece, to;
		IntStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - whiteKing) << 1) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.filePos) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						moveSet = MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty) & pawnCheckSquares;
						if (moveSet != 0) {
							to = BitOperations.indexOfBit(moveSet);
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.diagonalPos) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd + 7)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd + 7) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.antiDiagonalPos) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd + 9)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd + 9) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((whiteKing - pinnerBit) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.fileNeg) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						moveSet = MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty) & pawnCheckSquares;
						if (moveSet != 0) {
							to = BitOperations.indexOfBit(moveSet);
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.diagonalNeg) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.antiDiagonalNeg) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - blackKing) << 1) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.filePos) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						moveSet = MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty) & pawnCheckSquares;
						if (moveSet != 0) {
							to = BitOperations.indexOfBit(moveSet);
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.diagonalPos) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.antiDiagonalPos) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((blackKing - pinnerBit) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.fileNeg) & (pinnerBit | rookCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						moveSet = MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty) & pawnCheckSquares;
						if (moveSet != 0) {
							to = BitOperations.indexOfBit(moveSet);
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.diagonalNeg) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (this.enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd - 7)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd - 7) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.antiDiagonalNeg) & (pinnerBit | bishopCheckSquares));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd - 9)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd - 9) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**Generates and adds the quiet pinned-piece-moves to the input parameter 'moves' and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @param rookCheckSquares
	 * @param bishopCheckSquares
	 * @param pawnCheckSquares
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addQuietPinnedPieceMoves(List<Move> moves, long rookCheckSquares, long bishopCheckSquares, long pawnCheckSquares) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0;
		long rookNonCheckSquares = ~rookCheckSquares,
		bishopNonCheckSquares = ~bishopCheckSquares,
		pawnNonCheckSquares = ~pawnCheckSquares;
		int pinnedPieceInd, pinnedPiece;
		IntStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1))^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - (whiteKing << 1)) & attRayMask.filePos)^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty) & pawnNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - (whiteKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - (whiteKing << 1)) & attRayMask.antiDiagonalPos)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1))^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((whiteKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty) & pawnNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((whiteKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((whiteKing - (pinnerBit << 1)) & attRayMask.antiDiagonalNeg)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1))^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - (blackKing << 1)) & attRayMask.filePos)^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty) & pawnNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - (blackKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - (blackKing << 1)) & attRayMask.antiDiagonalPos)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1))^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((blackKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit) & rookNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty) & pawnNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((blackKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((blackKing - (pinnerBit << 1)) & attRayMask.antiDiagonalNeg)^pinnedPieceBit) & bishopNonCheckSquares);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**Generates and adds only the pinned-piece-moves that change the material balance on the board (captures/promotions) to the input
	 * parameter 'moves' and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addMaterialPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0;
		int pinnedPieceInd, pinnedPiece, to;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd + 7)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd + 7) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd + 9)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd + 9) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (this.enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd - 7)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd - 7) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = pinnedPieceInd - 9)) & (pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = pinnedPieceInd - 9) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**Generates and adds only the pinned-piece-moves that do not change the material balance on the board to the input parameter 'moves'
	 * and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addNonMaterialPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0;
		int pinnedPieceInd, pinnedPiece;
		IntStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((pinnerBit - (whiteKing << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1)) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1)) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((whiteKing - (pinnerBit << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getWhitePawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, 0));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1)) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((pinnerBit - (blackKing << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1)) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1)) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((blackKing - (pinnerBit << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveTable.getByIndex(pinnedPieceInd).getBlackPawnAdvances(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1)) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**A method that returns a list (queue) of all the legal moves from a non-check position.
	 * 
	 * @return A queue of legal moves.
	 */
	private Queue<Move> generateAllNormalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits;
		int king, piece, to, victim;
		IntStack pieces, moveList;
		Move move;
		Queue<Move> moves = new Queue<Move>();
		if (whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveTable.getByIndex(king).getWhiteKingMoves(allNonWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			if (whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0) {
					if ((move = moves.getTail()) != null && move.to == Square.D1.ind && !isAttacked(Square.C1.ind, false))
						moves.add(new Move(king, Square.C1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			if (whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
						moves.add(new Move(king, Square.G1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			movablePieces = ~addAllPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhiteQueenMoves(allNonWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteRookMoves(allNonWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteBishopMoves(allNonWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteKnightMoves(allNonWhiteOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhitePawnMoves(allBlackOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to >= Square.A8.ind) {
						victim = offsetBoard[to];
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveTable.getByIndex(to = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights).getBlackPawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, false))
							moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveTable.getByIndex(king).getBlackKingMoves(allNonBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			if (blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0) {
					if ((move = moves.getHead()) != null && move.to == Square.F8.ind && !isAttacked(Square.G8.ind, true))
						moves.add(new Move(king, Square.G8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			if (blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
						moves.add(new Move(king, Square.C8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			movablePieces = ~addAllPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackQueenMoves(allNonBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackRookMoves(allNonBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackBishopMoves(allNonBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackKnightMoves(allNonBlackOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getBlackPawnMoves(allWhiteOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to < Square.A2.ind) {
						victim = offsetBoard[to];
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveTable.getByIndex(to = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights).getWhitePawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, true))
							moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	/**A method that returns a list (queue) of the tactical legal moves (promotions/captures) from a non-check position.
	 * 
	 * @param checkSquares An array of bitmaps representing the squares from which the given pieces the opponent can be checked.
	 * @return A queue of tactical legal moves and checks.
	 */
	private Queue<Move> generateTacticalNormalMoves(long[] checkSquares) {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits;
		long queenCheckSquares, rookCheckSquares, bishopCheckSquares, knightCheckSquares, pawnCheckSquares;
		int king, piece, to, victim;
		IntStack pieces, moveList;
		Queue<Move> moves = new Queue<Move>();
		queenCheckSquares = checkSquares[0];
		rookCheckSquares = checkSquares[1];
		bishopCheckSquares = checkSquares[2];
		knightCheckSquares = checkSquares[3];
		pawnCheckSquares = checkSquares[4];
		if (whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveTable.getByIndex(king).getWhiteKingMoves(allBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			movablePieces = ~addTacticalPinnedPieceMoves(moves, rookCheckSquares, bishopCheckSquares, pawnCheckSquares);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhiteQueenMoves(allBlackOccupied | queenCheckSquares, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteRookMoves(allBlackOccupied | rookCheckSquares, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteBishopMoves(allBlackOccupied | bishopCheckSquares, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteKnightMoves(allBlackOccupied | knightCheckSquares);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhitePawnMoves(allBlackOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					victim = offsetBoard[to];
					if (to >= Square.A8.ind) {
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (victim != 0 || ((1L << to) & pawnCheckSquares) != 0)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveTable.getByIndex(to = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights).getBlackPawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, false))
							moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveTable.getByIndex(king).getBlackKingMoves(allWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			movablePieces = ~addTacticalPinnedPieceMoves(moves, rookCheckSquares, bishopCheckSquares, pawnCheckSquares);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackQueenMoves(allWhiteOccupied | queenCheckSquares, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackRookMoves(allWhiteOccupied | rookCheckSquares, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackBishopMoves(allWhiteOccupied | bishopCheckSquares, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackKnightMoves(allWhiteOccupied | knightCheckSquares);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getBlackPawnMoves(allWhiteOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					victim = offsetBoard[to];
					if (to < Square.A2.ind) {
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (victim != 0 || ((1L << to) & pawnCheckSquares) != 0)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveTable.getByIndex(to = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights).getWhitePawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, true))
							moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	/**A method that returns a list (queue) of the quiet legal moves from a non-check position.
	 * 
	 * @param checkSquares An array of bitmaps representing the squares from which the given pieces the opponent can be checked.
	 * @return A queue of quiet legal moves.
	 */
	private Queue<Move> generateQuietNormalMoves(long[] checkSquares) {
		long movablePieces, pieceSet, moveSet;
		long queenNonCheckSquares, rookNonCheckSquares, bishopNonCheckSquares, knightNonCheckSquares, pawnNonCheckSquares;
		int king, piece, to;
		IntStack pieces, moveList;
		Move move;
		Queue<Move> moves = new Queue<Move>();
		queenNonCheckSquares = ~checkSquares[0];
		rookNonCheckSquares = ~checkSquares[1];
		bishopNonCheckSquares = ~checkSquares[2];
		knightNonCheckSquares = ~checkSquares[3];
		pawnNonCheckSquares = ~checkSquares[4];
		if (whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveTable.getByIndex(king).getWhiteKingMoves(allEmpty);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
			}
			if (whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0) {
					if ((move = moves.getTail()) != null && move.to == Square.D1.ind && !isAttacked(Square.C1.ind, false))
						moves.add(new Move(king, Square.C1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			if (whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
						moves.add(new Move(king, Square.G1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			movablePieces = ~addQuietPinnedPieceMoves(moves, ~rookNonCheckSquares, ~bishopNonCheckSquares, ~pawnNonCheckSquares);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhiteQueenMoves(allEmpty, allOccupied) & queenNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteRookMoves(allEmpty, allOccupied) & rookNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteBishopMoves(allEmpty, allOccupied) & bishopNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteKnightMoves(allEmpty) & knightNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhitePawnAdvances(allEmpty) & pawnNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to <= 55)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveTable.getByIndex(king).getBlackKingMoves(allEmpty);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
			}
			if (blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0) {
					if ((move = moves.getHead()) != null && move.to == Square.F8.ind && !isAttacked(Square.G8.ind, true))
						moves.add(new Move(king, Square.G8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			if (blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
						moves.add(new Move(king, Square.C8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			movablePieces = ~addQuietPinnedPieceMoves(moves, ~rookNonCheckSquares, ~bishopNonCheckSquares, ~pawnNonCheckSquares);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackQueenMoves(allEmpty, allOccupied) & queenNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackRookMoves(allEmpty, allOccupied) & rookNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackBishopMoves(allEmpty, allOccupied) & bishopNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackKnightMoves(allEmpty) & knightNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getBlackPawnAdvances(allEmpty) & pawnNonCheckSquares;
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to >= 8)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**A method that returns a list (queue) of the legal moves that change the material balance on the board (captures/promotions) from a
	 * non-check position.
	 * 
	 * @return A queue of material-tactical legal moves.
	 */
	private Queue<Move> generateMaterialNormalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits;
		int king, piece, to, victim;
		IntStack pieces, moveList;
		Queue<Move> moves = new Queue<Move>();
		if (whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveTable.getByIndex(king).getWhiteKingMoves(allBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			movablePieces = ~addMaterialPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhiteQueenMoves(allBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteRookMoves(allBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteBishopMoves(allBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteKnightMoves(allBlackOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhitePawnMoves(allBlackOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					victim = offsetBoard[to];
					if (to >= Square.A8.ind) {
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (victim != 0)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveTable.getByIndex(to = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights).getBlackPawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, false))
							moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveTable.getByIndex(king).getBlackKingMoves(allWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			movablePieces = ~addMaterialPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackQueenMoves(allWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackRookMoves(allWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackBishopMoves(allWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackKnightMoves(allWhiteOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getBlackPawnMoves(allWhiteOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					victim = offsetBoard[to];
					if (to < Square.A2.ind) {
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (victim != 0)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveTable.getByIndex(to = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights).getWhitePawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, true))
							moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	/**A method that returns a list (queue) of the legal moves that do not affect the material balance of the position (no captures or
	 * promotions) from a non-check position.
	 * 
	 * @return A queue of non-material legal moves.
	 */
	private Queue<Move> generateNonMaterialNormalMoves() {
		long movablePieces, pieceSet, moveSet;
		int king, piece, to;
		IntStack pieces, moveList;
		Move move;
		Queue<Move> moves = new Queue<Move>();
		if (whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveTable.getByIndex(king).getWhiteKingMoves(allEmpty);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
			}
			if (whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0) {
					if ((move = moves.getTail()) != null && move.to == Square.D1.ind && !isAttacked(Square.C1.ind, false))
						moves.add(new Move(king, Square.C1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			if (whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
						moves.add(new Move(king, Square.G1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			movablePieces = ~addNonMaterialPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhiteQueenMoves(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteRookMoves(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteBishopMoves(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getWhiteKnightMoves(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getWhitePawnAdvances(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to <= 55)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveTable.getByIndex(king).getBlackKingMoves(allEmpty);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
			}
			if (blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0) {
					if ((move = moves.getHead()) != null && move.to == Square.F8.ind && !isAttacked(Square.G8.ind, true))
						moves.add(new Move(king, Square.G8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			if (blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
						moves.add(new Move(king, Square.C8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			movablePieces = ~addNonMaterialPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackQueenMoves(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackRookMoves(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackBishopMoves(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveTable.getByIndex(piece).getBlackKnightMoves(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveTable.getByIndex(piece).getBlackPawnAdvances(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to >= 8)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**This method returns a list (queue) of all the legal moves from a position in which the side to move is in check.
	 * 
	 * @return A queue of all legal moves from a check position.
	 */
	private Queue<Move> generateAllCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare,
			checkerBlockerSquare, king, to, movedPiece;
		IntStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveTable dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getWhiteKingMoves(allNonWhiteOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.W_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 + 8, movedPiece, checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
							if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(false);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getBlackKingMoves(allNonBlackOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~blackKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.B_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 - 8, movedPiece, checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
							if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, 0));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, 0));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**This method returns a list (queue) of the tactical legal moves (captures/promotions) and checks from a position in which the side
	 * to move is in check.
	 * 
	 * @param checkSquares An array of bitmaps representing the squares from which the given pieces the opponent can be checked.
	 * @return A queue of the tactical legal moves and checks from a check position.
	 */
	private Queue<Move> generateTacticalCheckEvasionMoves(long[] checkSquares) {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, squareOfInterventionBit, checkerAttackerSet, checkerBlockerSet, pawnCheckSquares;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare,
			checkerBlockerSquare, king, to, movedPiece;
		IntStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveTable dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		pawnCheckSquares = checkSquares[4];
		if (this.whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getWhiteKingMoves(allBlackOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.W_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 + 8, movedPiece, checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
							if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else if ((squareOfInterventionBit & pawnCheckSquares) != 0)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else if ((checkSquares[movedPiece - Piece.W_QUEEN.ind] & squareOfInterventionBit) != 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else if ((squareOfInterventionBit & pawnCheckSquares) != 0)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else if ((checkSquares[movedPiece - Piece.W_QUEEN.ind] & squareOfInterventionBit) != 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else if ((squareOfInterventionBit & pawnCheckSquares) != 0)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else if ((checkSquares[movedPiece - Piece.W_QUEEN.ind] & squareOfInterventionBit) != 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(false);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getBlackKingMoves(allWhiteOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~blackKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.B_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 - 8, movedPiece, checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
							if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else if ((squareOfInterventionBit & pawnCheckSquares) != 0)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else if ((checkSquares[movedPiece - Piece.B_QUEEN.ind] & squareOfInterventionBit) != 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else if ((squareOfInterventionBit & pawnCheckSquares) != 0)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else if ((checkSquares[movedPiece - Piece.B_QUEEN.ind] & squareOfInterventionBit) != 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else if ((squareOfInterventionBit & pawnCheckSquares) != 0)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else if ((checkSquares[movedPiece - Piece.B_QUEEN.ind] & squareOfInterventionBit) != 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**This method returns a list (queue) of the quiet legal moves from a position in which the side to move is in check.
	 * 
	 * @param checkSquares An array of bitmaps representing the squares from which the given pieces the opponent can be checked.
	 * @return A queue of the quiet legal moves from a check position.
	 */
	private Queue<Move> generateQuietCheckEvasionMoves(long[] checkSquares) {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerBlockerSet, squareOfInterventionBit;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention,
			checkerBlockerSquare, king, to, movedPiece;
		IntStack kingMoves, squaresOfIntervention, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveTable dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getWhiteKingMoves(allEmpty);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
							if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if ((!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind) && (squareOfInterventionBit & checkSquares[movedPiece - Piece.W_QUEEN.ind]) == 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if ((!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind) && (squareOfInterventionBit & checkSquares[movedPiece - Piece.W_QUEEN.ind]) == 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if ((squareOfInterventionBit & checkSquares[movedPiece - Piece.W_QUEEN.ind]) == 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(false);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getBlackKingMoves(allEmpty);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
							if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if ((!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind) && (squareOfInterventionBit & checkSquares[movedPiece - Piece.B_QUEEN.ind]) == 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if ((!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind) && (squareOfInterventionBit & checkSquares[movedPiece - Piece.B_QUEEN.ind]) == 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							squareOfInterventionBit = 1L << squareOfIntervention;
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if ((squareOfInterventionBit & checkSquares[movedPiece - Piece.B_QUEEN.ind]) == 0)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**This method returns a list (queue) of the legal moves that change the material balance on the board (captures/promotions) from a
	 * position in which the side to move is in check.
	 * 
	 * @param checkSquares An array of bitmaps representing the squares from which the given pieces the opponent can be checked.
	 * @return A queue of legal material moves from a check position.
	 */
	private Queue<Move> generateMaterialCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare,
			checkerBlockerSquare, king, to, movedPiece;
		IntStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveTable dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getWhiteKingMoves(allBlackOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.W_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 + 8, movedPiece, checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
				case 8: {
					if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
						squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
					}
					else
						squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.W_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
				}
				break;
				case 9: {
					if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.W_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getCrudeRookMoves();
				}
				break;
				case 10: {
					squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.W_PAWN.ind) {
								if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getCrudeBishopMoves();
				}
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
		else {
			checker1 = BitOperations.indexOfLSBit(checkers);
			checkerPiece1 = offsetBoard[checker1];
			checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
			checkerPiece2 = offsetBoard[checker2];
			dB = MoveTable.getByIndex(checker1);
			switch (checkerPiece1) {
				case 8:
					kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
				break;
				case 9:
					kingMoveSet &= ~dB.getCrudeRookMoves();
				break;
				case 10:
					kingMoveSet &= ~dB.getCrudeBishopMoves();
				break;
				case 11:
					kingMoveSet &= ~dB.getCrudeKnightMoves();
			}
			dB = MoveTable.getByIndex(checker2);
			switch (checkerPiece2) {
				case 8:
					kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
				break;
				case 9:
					kingMoveSet &= ~dB.getCrudeRookMoves();
				break;
				case 10:
					kingMoveSet &= ~dB.getCrudeBishopMoves();
				break;
				case 11:
					kingMoveSet &= ~dB.getCrudeKnightMoves();
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
	}
	else {
		king = BitOperations.indexOfBit(blackKing);
		movablePieces = ~getPinnedPieces(false);
		kingDb = MoveTable.getByIndex(king);
		kingMoveSet = kingDb.getBlackKingMoves(allWhiteOccupied);
		if (BitOperations.resetLSBit(checkers) == 0) {
			checker1 = BitOperations.indexOfBit(checkers);
			checkerPiece1 = offsetBoard[checker1];
			dB = MoveTable.getByIndex(checker1);
			if ((checkers & Rank.R1.bitmap) != 0)
				promotionOnAttackPossible = true;
			checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~blackKing;
			checkerAttackers = BitOperations.serialize(checkerAttackerSet);
			while (checkerAttackers.hasNext()) {
				checkerAttackerSquare = checkerAttackers.next();
				movedPiece = offsetBoard[checkerAttackerSquare];
				if (movedPiece == Piece.B_PAWN.ind) {
					if (promotionOnAttackPossible) {
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
						moves.add(new Move(checkerAttackerSquare, checker1 - 8, movedPiece, checkerPiece1, MoveType.EN_PASSANT.ind));
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				else
					moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
			}
			switch (checkerPiece1) {
				case 2: {
					if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
						squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
					}
					else
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.B_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, 6, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
				}
				break;
				case 3: {
					if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.B_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, 6, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getCrudeRookMoves();
				}
				break;
				case 4: {
					squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.B_PAWN.ind) {
								if (enPassantRights != EnPassantRights.NONE.ind && squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getCrudeBishopMoves();
				}
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
		else {
			checker1 = BitOperations.indexOfLSBit(checkers);
			checkerPiece1 = offsetBoard[checker1];
			checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
			checkerPiece2 = this.offsetBoard[checker2];
			dB = MoveTable.getByIndex(checker1);
			switch (checkerPiece1) {
				case 2:
					kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
				break;
				case 3:
					kingMoveSet &= ~dB.getCrudeRookMoves();
				break;
				case 4:
					kingMoveSet &= ~dB.getCrudeBishopMoves();
				break;
				case 5:
					kingMoveSet &= ~dB.getCrudeKnightMoves();
			}
			dB = MoveTable.getByIndex(checker2);
			switch (checkerPiece2) {
				case 2:
					kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
				break;
				case 3:
					kingMoveSet &= ~dB.getCrudeRookMoves();
				break;
				case 4:
					kingMoveSet &= ~dB.getCrudeBishopMoves();
				break;
				case 5:
					kingMoveSet &= ~dB.getCrudeKnightMoves();
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
	}
	return moves;
}
	/**This method returns a list (queue) of the legal moves that do not affect the material balance (no captures or promotions) from a position in which the side to move
	 * is in check.
	 * 
	 * @return A queue of non-material legal moves from a check position.
	 */
	private Queue<Move> generateNonMaterialCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerBlockerSet;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention,
			checkerBlockerSquare, king, to, movedPiece;
		IntStack kingMoves, squaresOfIntervention, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveTable dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.whitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getWhiteKingMoves(allEmpty);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
							if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteRookMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied) & kingDb.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, offsetBoard[checkerBlockerSquare], Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(false);
			kingDb = MoveTable.getByIndex(king);
			kingMoveSet = kingDb.getBlackKingMoves(allEmpty);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveTable.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
							if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackRookMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied) & kingDb.getBlackBishopMoves(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, offsetBoard[checkerBlockerSquare], Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveTable.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveTable.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**Generates a queue of Move objects that represents all the legal moves from the current position.
	 * 
	 * @return A queue of all the legal moves from this position.
	 */
	public Queue<Move> generateAllMoves() {
		if (check)
			return generateAllCheckEvasionMoves();
		else
			return generateAllNormalMoves();
	}
	/**Generates a queue of Move objects that represents the tactical legal moves such as promotions, captures, and checks from the
	 * current position.
	 * 
	 * @return A queue of the tactical legal moves from this position.
	 */
	public Queue<Move> generateTacticalMoves(long[] checkSquares) {
		if (check)
			return generateTacticalCheckEvasionMoves(checkSquares);
		else
			return generateTacticalNormalMoves(checkSquares);
	}
	/**Generates a queue of Move objects that represents the quiet legal moves (meaning no promotions, captures, or checks) from the
	 * current position.
	 * 
	 * @return A queue of the quiet legal moves from this position.
	 */
	public Queue<Move> generateQuietMoves(long[] checkSquares) {
		if (check)
			return generateQuietCheckEvasionMoves(checkSquares);
		else
			return generateQuietNormalMoves(checkSquares);
	}
	/**Generates a queue of Move objects that represents the material legal moves (i.e. the ones that change the material balance of the
	 * position such as captures and promotions) from the current position.
	 * 
	 * @return A queue of the material legal moves from this position.
	 */
	public Queue<Move> generateMaterialMoves() {
		if (check)
			return generateMaterialCheckEvasionMoves();
		else
			return generateMaterialNormalMoves();
	}
	/**Generates a queue of Move objects that represents the non-material legal moves (i.e. the ones that do not affect the material
	 * balance of the position such as non-promotion and non-capture moves) from the current position.
	 * 
	 * @return A queue of the non-material legal moves from this position.
	 */
	public Queue<Move> generateNonMaterialMoves() {
		if (check)
			return generateNonMaterialCheckEvasionMoves();
		else
			return generateNonMaterialNormalMoves();
	}
	/**Returns whether a move is legal or not in the current position. It expects the move to be possibly legal in some position at least, and it
	 * checks if it still is in this one. That means, it does not check for inconsistency, such as a castling move with a capture, or an en passant
	 * with a moved piece other than pawn, etc.
	 * 
	 * @param move
	 * @return
	 */
	public boolean isLegal(Move move) {
		MoveTable dB;
		long checkers;
		long moveSet = 0;
		long toBit = (1L << move.to);
		if (offsetBoard[move.from] == move.movedPiece) {
			dB = MoveTable.getByIndex(move.to);
			PseudoSwitch: {
				if (whitesTurn) {
					if (move.movedPiece == Piece.W_KING.ind) {
						if (move.type == MoveType.SHORT_CASTLING.ind) {
							if (!check && whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0) {
									if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
										return true;
								}
							}
						}
						else if (move.type == MoveType.LONG_CASTLING.ind) {
							if (!check && whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0) {
									if (!isAttacked(Square.D1.ind, false) && !isAttacked(Square.C1.ind, false))
										return true;
								}
							}
						}
						else {
							moveSet = dB.getWhiteKingMoves(allNonWhiteOccupied);
							if ((moveSet & toBit) != 0 && !isAttacked(move.to, false))
								return true;
						}
						return false;
					}
					else if (move.movedPiece == Piece.W_QUEEN.ind)
						moveSet = dB.getWhiteQueenMoves(allNonWhiteOccupied, allOccupied);
					else if (move.movedPiece == Piece.W_ROOK.ind)
						moveSet = dB.getWhiteRookMoves(allNonWhiteOccupied, allOccupied);
					else if (move.movedPiece == Piece.W_BISHOP.ind)
						moveSet = dB.getWhiteBishopMoves(allNonWhiteOccupied, allOccupied);
					else if (move.movedPiece == Piece.W_KNIGHT.ind)
						moveSet = dB.getWhiteKnightMoves(allNonWhiteOccupied);
					else if (move.movedPiece == Piece.W_PAWN.ind) {
						moveSet = dB.getWhitePawnMoves(allBlackOccupied, allEmpty);
						if (move.type == MoveType.EN_PASSANT.ind && enPassantRights != EnPassantRights.NONE.ind && move.to == EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights) {
							moveSet |= toBit;
							break PseudoSwitch;
						}
					}
					else return false;
				}
				else {
					if (move.movedPiece == Piece.B_KING.ind) {
						if (move.type == MoveType.SHORT_CASTLING.ind) {
							if (!check && blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0) {
									if (!isAttacked(Square.F8.ind, true) && !isAttacked(Square.G8.ind, true))
										return true;
								}
							}
						}
						else if (move.type == MoveType.LONG_CASTLING.ind) {
							if (!check && blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0) {
									if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
										return true;
								}
							}
						}
						else {
							moveSet = dB.getBlackKingMoves(allNonBlackOccupied);
							if ((moveSet & toBit) != 0 && !isAttacked(move.to, false))
								return true;
						}
						return false;
					}
					else if (move.movedPiece == Piece.B_QUEEN.ind)
						moveSet = dB.getBlackQueenMoves(allNonBlackOccupied, allOccupied);
					else if (move.movedPiece == Piece.B_ROOK.ind)
						moveSet = dB.getBlackRookMoves(allNonBlackOccupied, allOccupied);
					else if (move.movedPiece == Piece.B_BISHOP.ind)
						moveSet = dB.getBlackBishopMoves(allNonBlackOccupied, allOccupied);
					else if (move.movedPiece == Piece.B_KNIGHT.ind)
						moveSet = dB.getBlackKnightMoves(allNonBlackOccupied);
					else if (move.movedPiece == Piece.B_PAWN.ind) {
						moveSet = dB.getBlackPawnMoves(allWhiteOccupied, allEmpty);
						if (move.type == MoveType.EN_PASSANT.ind && enPassantRights != EnPassantRights.NONE.ind && move.to == EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights) {
							moveSet |= toBit;
							break PseudoSwitch;
						}
					}
					else return false;
				}
				if (offsetBoard[move.to] != move.capturedPiece)
					return false;
			}
			if ((moveSet & toBit) != 0) {
				makeMoveOnBoard(move);
				checkers = getCheckers();
				unmakeMoveOnBoard(move);
				if (checkers == 0) return true;
			}
		}
		return false;
	}
	/**Makes a null move that can be taken back without breaking the game.
	 * 
	 */
	public void makeNullMove() {
		moveList.add(null);
		unmakeRegisterHistory.add(new UnmakeRegister(whiteCastlingRights, blackCastlingRights, enPassantRights, fiftyMoveRuleClock, repetitions, checkers));
		setTurn();
		setMoveIndices(Piece.NULL.ind, Piece.NULL.ind);
		setKeys();
	}
	/**Makes a move only on the chess board representations of this Position object.
	 * 
	 * @param move
	 */
	private void makeMoveOnBoard(Move move) {
		int enPassantVictimSquare;
		long enPassantVictimSquareBit;
		long fromBit = Square.getByIndex(move.from).bitmap;
		long toBit = Square.getByIndex(move.to).bitmap;
		if (move.type == MoveType.NORMAL.ind) {
			offsetBoard[move.from] = Piece.NULL.ind;
			offsetBoard[move.to] = move.movedPiece;
			setBitboards(move.movedPiece, move.capturedPiece, fromBit, toBit);
		}
		else if (move.type == MoveType.SHORT_CASTLING.ind) {
			if (whitesTurn) {
				move.movedPiece = Piece.W_KING.ind;
				offsetBoard[Square.H1.ind] = Piece.NULL.ind;
				offsetBoard[Square.F1.ind] = Piece.W_ROOK.ind;
				setBitboards(Piece.W_ROOK.ind, Piece.NULL.ind, Square.H1.bitmap, Square.F1.bitmap);
			}
			else {
				move.movedPiece = Piece.B_KING.ind;
				offsetBoard[Square.H8.ind] = Piece.NULL.ind;
				offsetBoard[Square.F8.ind] = Piece.B_ROOK.ind;
				setBitboards(Piece.B_ROOK.ind, Piece.NULL.ind, Square.H8.bitmap, Square.F8.bitmap);
			}
			offsetBoard[move.from] = Piece.NULL.ind;
			offsetBoard[move.to] = move.movedPiece;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, toBit);
		}
		else if (move.type == MoveType.LONG_CASTLING.ind) {
			if (whitesTurn) {
				move.movedPiece = Piece.W_KING.ind;
				offsetBoard[Square.A1.ind] = Piece.NULL.ind;
				offsetBoard[Square.D1.ind] = Piece.W_ROOK.ind;
				setBitboards(Piece.W_ROOK.ind, Piece.NULL.ind, Square.A1.bitmap, Square.D1.bitmap);
			}
			else {
				move.movedPiece = Piece.B_KING.ind;
				offsetBoard[Square.A8.ind] = Piece.NULL.ind;
				offsetBoard[Square.D8.ind] = Piece.B_ROOK.ind;
				setBitboards(Piece.B_ROOK.ind, Piece.NULL.ind, Square.A8.bitmap, Square.D8.bitmap);
			}
			offsetBoard[move.from] = Piece.NULL.ind;
			offsetBoard[move.to] = move.movedPiece;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, toBit);
		}
		else if (move.type == MoveType.EN_PASSANT.ind) {
			if (whitesTurn)
				enPassantVictimSquare = move.to - 8;
			else
				enPassantVictimSquare = move.to + 8;
			offsetBoard[move.from] = Piece.NULL.ind;
			offsetBoard[move.to] = move.movedPiece;
			offsetBoard[enPassantVictimSquare] = Piece.NULL.ind;
			enPassantVictimSquareBit = Square.getByIndex(enPassantVictimSquare).bitmap;
			setBitboards(move.movedPiece, move.capturedPiece, fromBit, enPassantVictimSquareBit);
			setBitboards(move.movedPiece, Piece.NULL.ind, enPassantVictimSquareBit, toBit);
		}
		else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
			if (whitesTurn) {
				offsetBoard[move.to] = Piece.W_QUEEN.ind;
				setBitboards(Piece.W_QUEEN.ind, move.capturedPiece, 0, toBit);
			}
			else {
				offsetBoard[move.to] = Piece.B_QUEEN.ind;
				setBitboards(Piece.B_QUEEN.ind, move.capturedPiece, 0, toBit);
			}
			offsetBoard[move.from] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
		}
		else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
			if (whitesTurn) {
				offsetBoard[move.to] = Piece.W_ROOK.ind;
				setBitboards(Piece.W_ROOK.ind, move.capturedPiece, 0, toBit);
			}
			else {
				offsetBoard[move.to] = Piece.B_ROOK.ind;
				setBitboards(Piece.B_ROOK.ind, move.capturedPiece, 0, toBit);
			}
			offsetBoard[move.from] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
		}
		else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
			if (whitesTurn) {
				offsetBoard[move.to] = Piece.W_BISHOP.ind;
				setBitboards(Piece.W_BISHOP.ind, move.capturedPiece, 0, toBit);
			}
			else {
				offsetBoard[move.to] = Piece.B_BISHOP.ind;
				setBitboards(Piece.B_BISHOP.ind, move.capturedPiece, 0, toBit);
			}
			offsetBoard[move.from] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
		}
		else if (move.type == MoveType.PROMOTION_TO_KNIGHT.ind) {
			if (whitesTurn) {
				move.movedPiece = Piece.W_PAWN.ind;
				offsetBoard[move.to] = Piece.W_KNIGHT.ind;
				setBitboards(Piece.W_KNIGHT.ind, move.capturedPiece, 0, toBit);
			}
			else {
				offsetBoard[move.to] = Piece.B_KNIGHT.ind;
				setBitboards(Piece.B_KNIGHT.ind, move.capturedPiece, 0, toBit);
			}
			offsetBoard[move.from] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
		}
	}
	/**Unmakes a move only on the chess board representations of this Position object.
	 * 
	 * @param move
	 */
	private void unmakeMoveOnBoard(Move move) {
		int enPassantVictimSquare;
		long fromBit = Square.getByIndex(move.from).bitmap;
		long toBit = Square.getByIndex(move.to).bitmap;
		if (move.type == MoveType.NORMAL.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = move.capturedPiece;
			setBitboards(move.movedPiece, move.capturedPiece, fromBit, toBit);
		}
		else if (move.type == MoveType.SHORT_CASTLING.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, toBit);
			if (whitesTurn) {
				offsetBoard[Square.H1.ind] = Piece.W_ROOK.ind;
				offsetBoard[Square.F1.ind] = Piece.NULL.ind;
				setBitboards(Piece.W_ROOK.ind, Piece.NULL.ind, Square.F1.bitmap, Square.H1.bitmap);
			}
			else {
				offsetBoard[Square.H8.ind] = Piece.B_ROOK.ind;
				offsetBoard[Square.F8.ind] = Piece.NULL.ind;
				setBitboards(Piece.B_ROOK.ind, Piece.NULL.ind, Square.F8.bitmap, Square.H8.bitmap);
			}
		}
		else if (move.type == MoveType.LONG_CASTLING.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, toBit);
			if (whitesTurn) {
				offsetBoard[Square.A1.ind] = Piece.W_ROOK.ind;
				offsetBoard[Square.D1.ind] = Piece.NULL.ind;
				setBitboards(Piece.W_ROOK.ind, Piece.NULL.ind, Square.D1.bitmap, Square.A1.bitmap);
			}
			else {
				offsetBoard[Square.A8.ind] = Piece.B_ROOK.ind;
				offsetBoard[Square.D8.ind] = Piece.NULL.ind;
				setBitboards(Piece.B_ROOK.ind, Piece.NULL.ind, Square.D8.bitmap, Square.A8.bitmap);
			}
		}
		else if (move.type == MoveType.EN_PASSANT.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = Piece.NULL.ind;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, toBit);
			if (whitesTurn)
				enPassantVictimSquare = move.to - 8;
			else
				enPassantVictimSquare = move.to + 8;
			offsetBoard[enPassantVictimSquare] = move.capturedPiece;
			setBitboards(Piece.NULL.ind, move.capturedPiece, 0, Square.getByIndex(enPassantVictimSquare).bitmap);
		}
		else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = move.capturedPiece;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
			if (whitesTurn)
				setBitboards(Piece.W_QUEEN.ind, Piece.NULL.ind, toBit, 0);
			else
				setBitboards(Piece.B_QUEEN.ind, Piece.NULL.ind, toBit, 0);
			setBitboards(Piece.NULL.ind, move.capturedPiece, 0, toBit);
		}
		else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = move.capturedPiece;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
			if (whitesTurn)
				setBitboards(Piece.W_ROOK.ind, Piece.NULL.ind, toBit, 0);
			else
				setBitboards(Piece.B_ROOK.ind, Piece.NULL.ind, toBit, 0);
			setBitboards(Piece.NULL.ind, move.capturedPiece, 0, toBit);
		}
		else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = move.capturedPiece;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
			if (whitesTurn)
				setBitboards(Piece.W_BISHOP.ind, Piece.NULL.ind, toBit, 0);
			else
				setBitboards(Piece.B_BISHOP.ind, Piece.NULL.ind, toBit, 0);
			setBitboards(Piece.NULL.ind, move.capturedPiece, 0, toBit);
		}
		else if (move.type == MoveType.PROMOTION_TO_KNIGHT.ind) {
			offsetBoard[move.from] = move.movedPiece;
			offsetBoard[move.to] = move.capturedPiece;
			setBitboards(move.movedPiece, Piece.NULL.ind, fromBit, 0);
			if (whitesTurn)
				setBitboards(Piece.W_KNIGHT.ind, Piece.NULL.ind, toBit, 0);
			else
				setBitboards(Piece.B_KNIGHT.ind, Piece.NULL.ind, toBit, 0);
			setBitboards(Piece.NULL.ind, move.capturedPiece, 0, toBit);
		}
	}
	/**Makes a single move from a LongQueue generated by generateMoves.
	 * 
	 * @param move A Move object that is going to be made in the position.
	 */
	public void makeMove(Move move) {
		makeMoveOnBoard(move);
		moveList.add(move);
		unmakeRegisterHistory.add(new UnmakeRegister(whiteCastlingRights, blackCastlingRights, enPassantRights, fiftyMoveRuleClock, repetitions, checkers));
		setTurn();
		setCastlingRights();
		setEnPassantRights(move.from, move.to, move.movedPiece);
		setCheck();
		setMoveIndices(move.movedPiece, move.capturedPiece);
		setKeys();
		setRepetitions();
	}
	/**Reverts the state of the instance to that before the last move made in every aspect necessary for the traversal of the game tree. Used within the engine.
	 * 
	 */
	public void unmakeMove() {
		Move move = moveList.pop();
		setTurn();
		if (move != null) unmakeMoveOnBoard(move);
		UnmakeRegister positionInfo = unmakeRegisterHistory.pop();
		whiteCastlingRights = positionInfo.whiteCastlingRights;
		blackCastlingRights = positionInfo.blackCastlingRights;
		enPassantRights = positionInfo.enPassantRights;
		fiftyMoveRuleClock = positionInfo.fiftyMoveRuleClock;
		repetitions = positionInfo.repetitions;
		checkers = positionInfo.checkers;
		if (checkers != 0)
			check = true;
		else
			check = false;
		keyHistory[halfMoveIndex] = 0;
		key = keyHistory[--halfMoveIndex];
	}
	/**Makes a move specified by user input. If it is legal and the command is valid ([origin square + destination square] as e.g.: b1a3 without any spaces; in case of promotion,
	 * the FEN notation of the piece the pawn is wished to be promoted to should be appended to the command as in c7c8q; the parser is not case sensitive), it returns true, else
	 * false.
	 * 
	 * @param input A string representation of the move to make [origin square|destination square|(piece to promote to)]
	 * 				e.g.: b1c3 or e7e8q
	 * @return Whether the move was legal and could successfully be made or not.
	 */
	public boolean makeMove(String input) {
		char zero, one, two, three, four;
		int from, to, type = 0;
		Move move;
		Queue<Move> moves;
		String command = "";
		for (int i = 0; i < input.length(); i++) {
			if (Character.toString(input.charAt(i)).matches("\\p{Graph}"))
				command += input.charAt(i);
		}
		if (command.length() >= 4 && command.length() <= 5) {
			command = command.toLowerCase();
			if (Character.toString(zero = command.charAt(0)).matches("[a-h]") && Character.toString(two = command.charAt(2)).matches("[a-h]") && Character.toString(one = command.charAt(1)).matches("[1-8]") && Character.toString(three = command.charAt(3)).matches("[1-8]")) {
				from = (zero - 'a') + (one - '1')*8;
				to 	 = (two - 'a') + (three - '1')*8;
				if (command.length() == 5) {
					four = command.charAt(4);
					if (three == 1 || three == 8) {
						switch (four) {
							case 'q':
								type = 4;
							break;
							case 'r':
								type = 5;
							break;
							case 'b':
								type = 6;
							break;
							case 'n':
								type = 7;
							break;
							default:
								return false;
						}
					}
					else
						return false;
				}
				moves = generateAllMoves();
				while (moves.hasNext()) {
					move = moves.next();
					if (move.from == from && move.to == to) {
						if (type != 0) {
							if (move.type == type) {
								makeMove(move);
								extendKeyHistory();
								return true;
							}
						}
						else {
							makeMove(move);
							extendKeyHistory();
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	/**Returns the current state of a Board object as a one-line String in FEN-notation. The FEN-notation consists of six fields separated by spaces.
	 * The six fields are as follows:
	 * 		1. board position
	 * 		2. color to move
	 * 		3. castling rights
	 * 		4. en passant rights
	 * 		5. fifty-move rule clock
	 * 		6. fullmove number
	 */
	public String toString() {
		String fen = "";
		int piece, emptyCount;
		for (int i = 7; i >= 0; i--) {
			emptyCount = 0;
			for (int j = 0; j < 8; j++) {
				piece = offsetBoard[i*8 + j];
				if (piece == 0)
					emptyCount++;
				else {
					if (emptyCount != 0)
						fen += emptyCount;
					emptyCount = 0;
					fen += Piece.getByNumericNotation(piece).fen;
				}
			}
			if (emptyCount != 0)
				fen += emptyCount;
			if (i != 0)
				fen += '/';
		}
		fen += ' ';
		if (whitesTurn)
			fen += 'w';
		else
			fen += 'b';
		fen += ' ';
		fen += CastlingRights.toFen(CastlingRights.getByIndex(whiteCastlingRights), CastlingRights.getByIndex(blackCastlingRights));
		fen += ' ';
		fen += EnPassantRights.getByIndex(enPassantRights).toString();
		if (enPassantRights != EnPassantRights.NONE.ind) {
			if (whitesTurn)
				fen += 6;
			else
				fen += 3;
		}
		fen += ' ';
		fen += fiftyMoveRuleClock;
		fen += ' ';
		fen += 1 + halfMoveIndex/2;
		return fen;
	}
	/**Prints the object's move history to the console in chronological order in pseudo-algebraic chess notation.*/
	public void printMoveHistoryToConsole() {
		List<Move> chronMoves = new Stack<Move>();
		int i = 0;
		while (moveList.hasNext())
			chronMoves.add(moveList.next());
		while (chronMoves.hasNext()) {
			i++;
			System.out.printf(i + ". %-8s ", chronMoves.next());
		}
		System.out.println();
	}
	/**Prints a bitboard representing all the occupied squares of the Position object's board position to the console in a human-readable form,
	 * aligned like a chess board.*/
	public void printBitboardToConsole() {
		Board.printBitboardToConsole(allOccupied);
	}
	/**Prints the array representing the Position object's board position to the console in a human-readable form, aligned like a chess board with 
	 * integers denoting the pieces. 0 means an empty square, 1 is the white king, 2 is the white queen, ..., 7 is the black king, etc.*/
	public void printOffsetBoardToConsole() {
		for (int i = 7; i >= 0; i--) {
			for (int j = 0; j < 8; j++)
				System.out.format("%3d", offsetBoard[i*8 + j]);
			System.out.println();
		}
		System.out.println();
	}
	/**Prints the chess board to the console. Pieces are represented according to the FEN notation.*/
	public void printFancyBoardToConsole() {
		for (int i = 16; i >= 0; i--) {
			if (i%2 == 0) {
				System.out.print("  ");
				for (int j = 0; j < 17; j++) {
					if (j%2 == 0)
						System.out.print("+");
					else
						System.out.print("---");
				}
			}
			else {
				System.out.print((i + 1)/2 + " ");
				for (int j = 0; j < 17; j++) {
					if (j%2 == 0)
						System.out.print("|");
					else
						System.out.print(" " + Piece.getByNumericNotation(offsetBoard[(i - 1)*4 + j/2]).fen + " ");
				}
			}
			System.out.println();
		}
		System.out.print("  ");
		for (int i = 0; i < 8; i++) {
			System.out.print("  " + (char)('A' + i) + " ");
		}
		System.out.println();
	}
	/**Prints information that constitutes the Board instance's state to the console.*/
	public void printStateToConsole() {
		IntStack checkers;
		this.printFancyBoardToConsole();
		System.out.println();
		System.out.printf("%-23s ", "To move:");
		if (this.whitesTurn)
			System.out.println("white");
		else
			System.out.println("black");
		if (this.check) {
			System.out.printf("%-23s ", "Checker(s):");
			checkers = BitOperations.serialize(this.checkers);
			while (checkers.hasNext())
				System.out.print(Square.toString(checkers.next()) + " ");
			System.out.println();
		}
		System.out.printf("%-23s ", "Castling rights:");
		System.out.print(CastlingRights.toFen(CastlingRights.getByIndex(whiteCastlingRights), CastlingRights.getByIndex(blackCastlingRights)));
		System.out.println();
		System.out.printf("%-23s ", "En passant rights:");
		System.out.println(EnPassantRights.getByIndex(enPassantRights).toString());
		System.out.printf("%-23s " + halfMoveIndex + "\n", "Half-move index:");
		System.out.printf("%-23s " + fiftyMoveRuleClock + "\n", "Fifty-move rule clock:");
		System.out.printf("%-23s " + Long.toHexString(key) + "\n", "Hash key:");
		System.out.printf("%-23s ", "Move history:");
		printMoveHistoryToConsole();
		System.out.println();
	}
	/**Runs a perft test to the given depth and returns the number of leaf nodes the traversed game tree had. It is used mainly for move generation and move
	 * making speed benchmarking; and bug detection by comparing the returned values to validated results.
	 * 
	 * @param depth
	 * @return
	 */
	public long perft(int depth) {
		Queue<Move> moves;
		Move move;
		long leafNodes = 0;
		if (depth == 0)
			return 1;
		moves = generateAllMoves();
		while (moves.hasNext()) {
			move = moves.next();
			makeMove(move);
			leafNodes += perft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**Runs a perft test with staged move generation, first generating only the material moves such as captures and promotions and then
	 * proceeding to generate the non-material moves i.e. the rest.
	 * 
	 * @param depth
	 * @return
	 */
	public long materialStagedPerft(int depth) {
		Queue<Move> materialMoves, nonMaterialMoves;
		Move move;
		long leafNodes = 0;
		if (depth == 0)
			return 1;
		materialMoves = generateMaterialMoves();
		while (materialMoves.hasNext()) {
			move = materialMoves.next();
			makeMove(move);
			leafNodes += materialStagedPerft(depth - 1);
			unmakeMove();
		}
		nonMaterialMoves = generateNonMaterialMoves();
		while (nonMaterialMoves.hasNext()) {
			move = nonMaterialMoves.next();
			makeMove(move);
			leafNodes += materialStagedPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**Runs a perft test with staged move generation, first generating only the tactical moves such as captures, promotions, and checks
	 * and then proceeding to generate the quiet moves, i.e. the rest.
	 * 
	 * @param depth
	 * @return
	 */
	public long tacticalStagedPerft(int depth) {
		long[] checkSquares;
		Queue<Move> tacticalMoves, quietMoves;
		Move move;
		long leafNodes = 0;
		if (depth == 0)
			return 1;
		checkSquares = squaresToCheckFrom();
		tacticalMoves = generateTacticalMoves(checkSquares);
		while (tacticalMoves.hasNext()) {
			move = tacticalMoves.next();
			makeMove(move);
			leafNodes += tacticalStagedPerft(depth - 1);
			unmakeMove();
		}
		quietMoves = generateQuietMoves(checkSquares);
		while (quietMoves.hasNext()) {
			move = quietMoves.next();
			makeMove(move);
			leafNodes += tacticalStagedPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**Runs a perft test faster than the standard method. Instead of making and unmaking the moves leading to the leafnodes, it simply
	 * returns the number of generated moves from the nodes at depth 1. More suitable for benchmarking move generation.
	 * 
	 * @param depth
	 * @return
	 */
	public long quickPerft(int depth) {
		Queue<Move> moves;
		Move move;
		long leafNodes = 0;
		moves = generateAllMoves();
		if (depth == 1)
			return moves.length();
		while (moves.hasNext()) {
			move = moves.next();
			makeMove(move);
			leafNodes += quickPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**Runs a quick perft test (not making and unmaking leaf moves, just returning their count) with staged move generation, first
	 * generating only the material moves such as captures and promotions and then proceeding to generate the non-material moves i.e.
	 * the rest.
	 * 
	 * @param depth
	 * @return
	 */
	public long materialStagedQuickPerft(int depth) {
		Queue<Move> materialMoves, nonMaterialMoves;
		Move move;
		long leafNodes = 0;
		if (depth == 1)
			return generateMaterialMoves().length() + generateNonMaterialMoves().length();
		materialMoves = generateMaterialMoves();
		while (materialMoves.hasNext()) {
			move = materialMoves.next();
			makeMove(move);
			leafNodes += materialStagedQuickPerft(depth - 1);
			unmakeMove();
		}
		nonMaterialMoves = generateNonMaterialMoves();
		while (nonMaterialMoves.hasNext()) {
			move = nonMaterialMoves.next();
			makeMove(move);
			leafNodes += materialStagedQuickPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**Runs a quick perft test (not making and unmaking leaf moves, just returning their count) with staged move generation, first
	 * generating only the tactical moves such as captures, promotions, and checks and then proceeding to generate the quiet moves i.e.
	 * the rest.
	 * 
	 * @param depth
	 * @return
	 */
	public long tacticalStagedQuickPerft(int depth) {
		long[] checkSquares = squaresToCheckFrom();
		Queue<Move> tacticalMoves, quietMoves;
		Move move;
		long leafNodes = 0;
		if (depth == 1)
			return generateTacticalMoves(checkSquares).length() + generateQuietMoves(checkSquares).length();
		tacticalMoves = generateTacticalMoves(checkSquares);
		while (tacticalMoves.hasNext()) {
			move = tacticalMoves.next();
			makeMove(move);
			leafNodes += tacticalStagedQuickPerft(depth - 1);
			unmakeMove();
		}
		quietMoves = generateQuietMoves(checkSquares);
		while (quietMoves.hasNext()) {
			move = quietMoves.next();
			makeMove(move);
			leafNodes += tacticalStagedQuickPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**Runs a perft test to the given depth and returns the number of leafnodes resulting from moves of the type specified by moveType.
	 * Expected moveType values:
	 * default:	all kinds of moves;
	 * 1:		ordinary moves;
	 * 2:		castling;
	 * 3:		en passant;
	 * 4:		promotion.
	 * It can also print to the console either the move list or one of two kinds of representations of the board position in the leafnodes according to consoleOutputType.
	 * Expected consoleOutputType values:
	 * default:	no output;
	 * 1:		the whole move list in chronological order;
	 * 2:		a bitboard representing all the occupied squares using {@link #printBitboardToConsole() printBitboardToConsole};
	 * 3:		a matrix of integers denoting chess pieces according to {@link #Board.Piece Piece} using {@link #printOffsetBoardToConsole() printOffsetBoardToConsole}.
	 * 
	 * @param depth
	 * @param moveType
	 * @param consoleOutputType
	 * @return
	 */
	public long detailedPerft(int depth, int moveType, int consoleOutputType) {
		Queue<Move> moves;
		Move move, moveListHead;
		long leafNodes = 0;
		if (depth == 0 && (moveListHead = moveList.getHead()) != null) {
			switch (moveType) {
				case 1: {
					if (moveListHead.type == 1) {
						switch (consoleOutputType) {
							case 1: 
								this.printMoveHistoryToConsole();
							break;
							case 2:
								this.printBitboardToConsole();
							break;
							case 3:
								this.printOffsetBoardToConsole();
						}
						return 1;
					}
				}
				break;
				case 2: {
					if (moveListHead.type == 2) {
						switch (consoleOutputType) {
							case 1: 
								this.printMoveHistoryToConsole();
							break;
							case 2:
								this.printBitboardToConsole();
							break;
							case 3:
								this.printOffsetBoardToConsole();
						}
						return 1;
					}
				}
				break;
				case 3: {
					if (moveListHead.type == 3) {
						switch (consoleOutputType) {
							case 1: 
								this.printMoveHistoryToConsole();
							break;
							case 2:
								this.printBitboardToConsole();
							break;
							case 3:
								this.printOffsetBoardToConsole();
						}
						return 1;
					}
				}
				break;
				case 4: {
					if (moveListHead.type > 3) {
						switch (consoleOutputType) {
							case 1: 
								this.printMoveHistoryToConsole();
							break;
							case 2:
								this.printBitboardToConsole();
							break;
							case 3:
								this.printOffsetBoardToConsole();
						}
						return 1;
					}
				}
				break;
				default: {
					switch (consoleOutputType) {
						case 1: 
							this.printMoveHistoryToConsole();
						break;
						case 2:
							this.printBitboardToConsole();
						break;
						case 3:
							this.printOffsetBoardToConsole();
					}
					return 1;
				}
			}
			return 0;
		}
		moves = generateAllMoves();
		while (moves.hasNext()) {
			move = moves.next();
			makeMove(move);
			leafNodes += detailedPerft(depth - 1, moveType, consoleOutputType);
			unmakeMove();
		}
		return leafNodes;
	}
	/**An interactive perft/divide function that runs a perft test and prints out all the legal moves at the root including the number of leafnodes in the subtrees.
	 * Selecting the index of a move results in the move made and the divide function run again for that subtree. If an invalid index is entered or depth 0 is reached, the method exits.
	 * A debugging tool based on comparison to correct move generators' results.
	 * 
	 * @param depth
	 */
	public void dividePerft(int depth) {
		System.out.println("DIVIDE_START");
		Scanner in = new Scanner(System.in);
		int moveIndex, i;
		IntQueue moveIndices = new IntQueue();
		long nodes, total;
		Queue<Move> moves, quietMoves;
		Stack<Move> chronoHistory;
		Move move;
		long[] checkSquares;
		boolean found = true;
		while (depth > 0 && found) {
			depth--;
			printStateToConsole();
			total = 0;
			found = false;
			i = 0;
			chronoHistory = new Stack<Move>();
			while (moveList.hasNext())
				chronoHistory.add(moveList.next());
			checkSquares = squaresToCheckFrom();
			moves = generateTacticalMoves(checkSquares);
			System.out.println("TACTICAL MOVES");
			while (moves.hasNext()) {
				while (chronoHistory.hasNext()) {
					move = chronoHistory.next();
					System.out.printf("%3d. %-8s ", moveIndices.next(), move);
				}
				moveIndices.reset();
				i++;
				move = moves.next();
				makeMove(move);
				if (depth > 0)
					nodes = tacticalStagedQuickPerft(depth);
				else
					nodes = 1;
				System.out.printf("%3d. %-8s nodes: %d\n", i, move, nodes);
				total += nodes;
				unmakeMove();
			}
			quietMoves = generateQuietMoves(checkSquares);
			System.out.println("QUIET MOVES");
			while (quietMoves.hasNext()) {
				while (chronoHistory.hasNext()) {
					move = chronoHistory.next();
					System.out.printf("%3d. %-8s ", moveIndices.next(), move);
				}
				moveIndices.reset();
				i++;
				move = quietMoves.next();
				makeMove(move);
				if (depth > 0)
					nodes = tacticalStagedQuickPerft(depth);
				else
					nodes = 1;
				System.out.printf("%3d. %-8s nodes: %d\n", i, move, nodes);
				total += nodes;
				unmakeMove();
			}
			while (quietMoves.hasNext())
				moves.add(quietMoves.next());
			System.out.println("\nMoves: " + moves.length());
			System.out.println("Total nodes: " + total);
			System.out.print("Enter the index of the move to divide: ");
			moveIndex = in.nextInt();
			if (moveIndex >= 0 && moveIndex <= i) {
				i = 0;
				while (moves.hasNext()) {
					i++;
					move = moves.next();
					if (i == moveIndex) {
						makeMove(move);
						moveIndices.add(moveIndex);
						found = true;
					}
				}
			}
		}
		in.close();
		System.out.println("DIVIDE_END");
	}
	/**Breaks up the number perft would return into the root's subtrees. It prints to the console all the legal moves from the root position and the number of leafnodes in
	 * the subtrees to which the moves lead.
	 * 
	 * @param depth
	 */
	public void divide(int depth) {
		long total = 0;
		Move move;
		Queue<Move> moves = generateAllMoves();
		while (moves.hasNext()) {
			move = moves.next();
			System.out.printf("%-10s ", move + ":");
			makeMove(move);
			System.out.println(total += quickPerft(depth - 1));
			unmakeMove();
		}
		System.out.println("Moves: " + moves.length());
		System.out.println("Total nodes: " + total);
	}
}
