package engine;

import util.*;
import engine.Bitboard.*;

import java.util.Scanner;

/**A bit board based class whose object holds information amongst others on the current board position, on all the previous moves and positions,
 * on castling and en passant rights, and on the player to move. It uses a pre-calculated 'magic' move database to avoid the cost of computing the
 * possible move sets of sliding pieces on the fly.
 * 
 * The main functions include:
 * {@link #generateMoves() generateMoves}
 * {@link #makeMove(long) makeMove}
 * {@link #unmakeMove() unmakeMove}
 * {@link #perft(int) perft}
 * {@link #divide(int) divide}
 *  
 * @author Viktor
 *
 */
public class Position {
	
	private Bitboard bitboard;															//the main board data-structure; bitmaps representing piece types and union-bitmaps representing all occupied squares etc.
	private int[] offsetBoard;															//a complimentary board data-structure to the bitboards to efficiently detect pieces on specific squares
	
	private boolean whitesTurn = true;
	
	private long checkers = 0;															//a bitboard of all the pieces that attack the color to move's king
	private boolean check = false;
	
	private int halfMoveIndex = 0;														//the count of the current ply/half-move
	private int fiftyMoveRuleClock = 0;													//the number of moves made since the last pawn move or capture; the choice of type fell on long due to data loss when int is shifted beyond the 32nd bit in the move integer
	
	private int enPassantRights = 8;													//denotes the file on which en passant is possible; 8 means no en passant rights
	
	private int whiteCastlingRights = 3;												//denotes to what extent it would still be possible to castle regardless of whether it is actually legally executable in the current position
	private int blackCastlingRights = 3;												//0 - no castling rights, 1 - king-side castling only, 2 - queen-side castling only, 3 - all castling rights
	
	private Stack<Move> moveList = new Stack<Move>();									//a stack of all the moves made so far
	private Stack<UnmakeRegister> unmakeRegisterHistory = new Stack<UnmakeRegister>();	//a stack history of castling rights, en passant rights, fifty-move rule clock, repetitions, and check info.
	
	private ZobristKeyGenerator keyGen = new ZobristKeyGenerator(); 					//a Zobrist key generator for hashing the board
	
	private long zobristKey;															//the Zobrist key that is fairly close to a unique representation of the state of the Board instance in one number
	private long[] zobristKeyHistory;													//all the positions that have occured so far represented in Zobrist keys.
	
	private int repetitions = 0;														//the number of times the current position has occured before; the choice of type fell on long due to data loss when int is shifted beyond the 32nd bit in the move integer
	
	/**Initializes an instance of Board and sets up the pieces in their initial position.*/
	public Position() {
		bitboard = new Bitboard();
		initializeOffsetBoard();
		initializeZobristKeys();
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
		board 			= fenFields[0];
		turn 			= fenFields[1];
		castling 		= fenFields[2];
		enPassant 		= fenFields[3];
		bitboard = new Bitboard(board);
		ranks = board.split("/");
		offsetBoard = new int[64];
		for (int i = 0; i < 64; i++)
			offsetBoard[i] = 0;
		for (int i = 7; i >= 0; i--) {
			rank = ranks[i];
			for (int j = 0; j < rank.length(); j++) {
				piece = rank.charAt(j);
				pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					switch (piece) {
						case 'K':
							offsetBoard[index] = 1;
						break;
						case 'Q':
							offsetBoard[index] = 2;
						break;
						case 'R':
							offsetBoard[index] = 3;
						break;
						case 'B':
							offsetBoard[index] = 4;
						break;
						case 'N':
							offsetBoard[index] = 5;
						break;
						case 'P':
							offsetBoard[index] = 6;
						break;
						case 'k':
							offsetBoard[index] = 7;
						break;
						case 'q':
							offsetBoard[index] = 8;
						break;
						case 'r':
							offsetBoard[index] = 9;
						break;
						case 'b':
							offsetBoard[index] = 10;
						break;
						case 'n':
							offsetBoard[index] = 11;
						break;
						case 'p':
							offsetBoard[index] = 12;
					}
					index++;
				}
			}
		}
		if (turn.toLowerCase().compareTo("w") == 0)
			whitesTurn = true;
		else
			whitesTurn = false;
		whiteCastlingRights = 0;
		if (castling.contains("K"))
			whiteCastlingRights += 1;
		if (castling.contains("Q"))
			whiteCastlingRights += 2;
		blackCastlingRights = 0;
		if (castling.contains("k"))
			blackCastlingRights += 1;
		if (castling.contains("q"))
			blackCastlingRights += 2;
		if (enPassant.compareTo("-") == 0)
			enPassantRights = 8;
		else
			enPassantRights = enPassant.toLowerCase().charAt(0) - 'a';
		setCheck();
		initializeZobristKeys();
	}
	private void initializeOffsetBoard() {
		offsetBoard = new int[64];
		offsetBoard[0] =  Piece.WHITE_ROOK.numericNotation;
		offsetBoard[1] =  Piece.WHITE_KNIGHT.numericNotation;
		offsetBoard[2] =  Piece.WHITE_BISHOP.numericNotation;
		offsetBoard[3] =  Piece.WHITE_QUEEN.numericNotation;
		offsetBoard[4] =  Piece.WHITE_KING.numericNotation;
		offsetBoard[5] =  Piece.WHITE_BISHOP.numericNotation;
		offsetBoard[6] =  Piece.WHITE_KNIGHT.numericNotation;
		offsetBoard[7] =  Piece.WHITE_ROOK.numericNotation;
		for (int i = 8; i < 16; i++)
			offsetBoard[i] = Piece.WHITE_PAWN.numericNotation;
		
		for (int i = 48; i < 56; i++)
			offsetBoard[i] = Piece.BLACK_PAWN.numericNotation;
		offsetBoard[56] = Piece.BLACK_ROOK.numericNotation;
		offsetBoard[57] = Piece.BLACK_KNIGHT.numericNotation;
		offsetBoard[58] = Piece.BLACK_BISHOP.numericNotation;
		offsetBoard[59] = Piece.BLACK_QUEEN.numericNotation;
		offsetBoard[60] = Piece.BLACK_KING.numericNotation;
		offsetBoard[61] = Piece.BLACK_BISHOP.numericNotation;
		offsetBoard[62] = Piece.BLACK_KNIGHT.numericNotation;
		offsetBoard[63] = Piece.BLACK_ROOK.numericNotation;
	}
	private void initializeZobristKeys() {
		//"The longest decisive tournament game is Fressinet-Kosteniuk, Villandry 2007, which Kosteniuk won in 237 moves." - half of that is used as the initial length of the history array
		zobristKeyHistory = new long[237];
		zobristKey = keyGen.hash(this);
		zobristKeyHistory[0] = this.zobristKey;
	}
	/**Returns a {@link #engine.Bitboard Bitboard} object holding all the piece bitmaps and a few unions.*/
	public Bitboard getBitboard() {
		return bitboard;
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
	/**Returns a number denoting white's castling rights.
	 * 0 - no castling rights
	 * 1 - king-side castling only
	 * 2 - queen-side castling only
	 * 3 - all castling rights
	 */
	public int getWhiteCastlingRights() {
		return whiteCastlingRights;
	}
	/**Returns a number denoting black's castling rights.
	 * 0 - no castling rights
	 * 1 - king-side castling only
	 * 2 - queen-side castling only
	 * 3 - all castling rights
	 */
	public int getBlackCastlingRights() {
		return blackCastlingRights;
	}
	/**Returns a number denoting the file on which in the current position en passant is possible.
	 * 0 - a; 1 - b; ...; 7 - h; 8 - no en passant rights
	 */
	public int getEnPassantRights() {
		return enPassantRights;
	}
	/**Returns the number of times the current position has previously occured since the initialization of the object.*/
	public long getRepetitions() {
		return repetitions;
	}
	/**Returns the 64-bit Zobrist key of the current position. A Zobrist key is used to almost uniquely hash a chess position to an integer.*/
	public long getZobristKey() {
		return zobristKey;
	}
	/**Returns an object containing all relevant information about the last move made. If the move history list is empty, it returns null.*/
	public Move getLastMove() {
		return moveList.getHead();
	}
	/**Returns an object containing some information about the previous position.*/
	public UnmakeRegister getUnmakeRegister() {
		return unmakeRegisterHistory.getHead();
	}
	private void setTurn() {
		whitesTurn = !whitesTurn;
	}
	private void setMoveIndices(int moved, int captured) {
		halfMoveIndex++;
		if (captured != 0 || moved == 6 || moved == 12)
			fiftyMoveRuleClock = 0;
		else
			fiftyMoveRuleClock++;
	}
	private void setEnPassantRights(int from, int to, int movedPiece) {
		if (movedPiece == 6) {
			if (to - from == 16) {
				enPassantRights = to%8;
				return;
			}
		}
		else if (movedPiece == 12) {
			if (from - to == 16) {
				enPassantRights = to%8;
				return;
			}
		}
		enPassantRights = 8;
	}
	private void setCastlingRights() {
		if (whitesTurn) {
			switch (whiteCastlingRights) {
				case 0: return;
				case 1: {
					if (offsetBoard[4] != 1 || offsetBoard[7] != 3)
						whiteCastlingRights = 0;
				}
				break;
				case 2: {
					if (offsetBoard[4] != 1 || offsetBoard[0] != 3)
						whiteCastlingRights = 0;
				}
				break;
				case 3: {
					if (offsetBoard[4] == 1) {
						if (offsetBoard[7] != 3)
							whiteCastlingRights -= 1;
						if (offsetBoard[0] != 3)
							whiteCastlingRights -= 2;
					}
					else
						whiteCastlingRights = 0;
				}
			}
		}
		else {
			switch (blackCastlingRights) {
				case 0:
					return;
				case 1: {
					if (offsetBoard[60] != 7 || offsetBoard[63] != 9)
						blackCastlingRights = 0;
				}
				break;
				case 2: {
					if (offsetBoard[60] != 7 || offsetBoard[56] != 9)
						blackCastlingRights = 0;
				}
				break;
				case 3: {
					if (offsetBoard[60] == 7) {
						if (offsetBoard[63] != 9)
							blackCastlingRights -= 1;
						if (offsetBoard[56] != 9)
							blackCastlingRights -= 2;
					}
					else
						blackCastlingRights = 0;
				}
			}
		}
	}
	private void setCheck() {
		checkers = getCheckers();
		check = (checkers != 0) ? true : false;
	}
	private void setKeys() {
		zobristKey = keyGen.updateKey(this);
		zobristKeyHistory[halfMoveIndex] = zobristKey;
	}
	private void extendKeyHistory() {
		long[] temp;
		if (zobristKeyHistory.length - halfMoveIndex <= 75) {
			temp = zobristKeyHistory;
			zobristKeyHistory = new long[zobristKeyHistory.length + 25];
			for (int i = 0; i < temp.length; i++)
				zobristKeyHistory[i] = temp[i];
		}
	}
	/**Should be used before resetMoveIndices().*/
	private void setRepetitions() {
		if (fiftyMoveRuleClock >= 4) {
			for (int i = halfMoveIndex; i >= (halfMoveIndex - fiftyMoveRuleClock); i -= 2) {
				if (zobristKeyHistory[i] == zobristKey)
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
		MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
		if (byWhite) {
			if ((bitboard.whiteKing								& dB.getCrudeKingMoves()) != 0)
				return true;
			if ((bitboard.whiteKnights 							& dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((bitboard.whitePawns 							& dB.getCrudeBlackPawnCaptures()) != 0)
				return true;
			if (((bitboard.whiteQueens | bitboard.whiteRooks) 	& dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied)) != 0)
				return true;
			if (((bitboard.whiteQueens | bitboard.whiteBishops) & dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied)) != 0)
				return true;
			if (offsetBoard[sqrInd] == 12 && enPassantRights != 8 && sqrInd == 32 + enPassantRights) {
				if ((bitboard.whitePawns & dB.getCrudeKingMoves() & Rank.R5.bitmap) != 0)
					return true;
			}
		}
		else {
			if ((bitboard.blackKing								& dB.getCrudeKingMoves()) != 0)
				return true;
			if ((bitboard.blackKnights 							& dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((bitboard.blackPawns 							& dB.getCrudeWhitePawnCaptures()) != 0)
				return true;
			if (((bitboard.blackQueens | bitboard.blackRooks) 	& dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied)) != 0)
				return true;
			if (((bitboard.blackQueens | bitboard.blackBishops) & dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied)) != 0)
				return true;
			if (offsetBoard[sqrInd] == 6 && enPassantRights != 8 && sqrInd == 24 + enPassantRights) {
				if ((bitboard.blackPawns & dB.getCrudeKingMoves() & Rank.R4.bitmap) != 0)
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
		MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
		if (byWhite) {
			if (((bitboard.whiteQueens | bitboard.whiteRooks) 	& dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied)) != 0)
				return true;
			if (((bitboard.whiteQueens | bitboard.whiteBishops) & dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied)) != 0)
				return true;
		}
		else {
			if (((bitboard.blackQueens | bitboard.blackRooks) 	& dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied)) != 0)
				return true;
			if (((bitboard.blackQueens | bitboard.blackBishops) & dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied)) != 0)
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
		MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
		if (byWhite) {
			attackers  =  bitboard.whiteKing							& dB.getCrudeKingMoves();
			attackers |=  bitboard.whiteKnights							& dB.getCrudeKnightMoves();
			attackers |=  bitboard.whitePawns 							& dB.getCrudeBlackPawnCaptures();
			attackers |= (bitboard.whiteQueens | bitboard.whiteRooks)	& dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
			attackers |= (bitboard.whiteQueens | bitboard.whiteBishops) & dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
			if (offsetBoard[sqrInd] == 12 && enPassantRights != 8 && sqrInd == 32 + enPassantRights)
				attackers |=  bitboard.whitePawns 						& dB.getCrudeKingMoves() & Rank.R5.bitmap;
		}
		else {
			attackers  =  bitboard.blackKing							& dB.getCrudeKingMoves();
			attackers |=  bitboard.blackKnights							& dB.getCrudeKnightMoves();
			attackers |=  bitboard.blackPawns 							& dB.getCrudeWhitePawnCaptures();
			attackers |= (bitboard.blackQueens | bitboard.blackRooks)	& dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
			attackers |= (bitboard.blackQueens | bitboard.blackBishops) & dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
			if (offsetBoard[sqrInd] == 6 && enPassantRights != 8 && sqrInd == 24 + enPassantRights)
				attackers |=  bitboard.blackPawns 						& dB.getCrudeKingMoves() & Rank.R4.bitmap;
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
		long sqrBit = Square.getByIndex(sqrInd).bitmap;
		long blackPawnAdvance = (sqrBit >>> 8), whitePawnAdvance = (sqrBit << 8);
		if (byWhite) {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			blockerCandidates |=  bitboard.whiteKnights							& dB.getCrudeKnightMoves();
			blockerCandidates |=  bitboard.whitePawns 							& blackPawnAdvance;
			if ((sqrBit & Rank.R4.bitmap) != 0 && (bitboard.allEmpty & blackPawnAdvance) != 0)
				blockerCandidates |= bitboard.whitePawns 						& (blackPawnAdvance >>> 8);
			blockerCandidates |= (bitboard.whiteQueens | bitboard.whiteRooks)	& dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
			blockerCandidates |= (bitboard.whiteQueens | bitboard.whiteBishops) & dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R6.bitmap) != 0)
				blockerCandidates |=  bitboard.whitePawns 						& dB.getCrudeBlackPawnCaptures();
		}
		else {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			blockerCandidates |=  bitboard.blackKnights							& dB.getCrudeKnightMoves();
			blockerCandidates |=  bitboard.blackPawns 							& whitePawnAdvance;
			if ((sqrBit & Rank.R5.bitmap) != 0 && (bitboard.allEmpty & whitePawnAdvance) != 0)
				blockerCandidates |= bitboard.blackPawns 						& (whitePawnAdvance << 8);
			blockerCandidates |= (bitboard.blackQueens | bitboard.blackRooks)	& dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
			blockerCandidates |= (bitboard.blackQueens | bitboard.blackBishops) & dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R3.bitmap) != 0)
				blockerCandidates |=  bitboard.blackPawns 						& dB.getCrudeWhitePawnCaptures();
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
		MoveDatabase dB;
		if (this.whitesTurn) {
			sqrInd = BitOperations.indexOfBit(bitboard.whiteKing);
			dB = MoveDatabase.getByIndex(sqrInd);
			attackers  =  bitboard.blackKnights							& dB.getCrudeKnightMoves();
			attackers |=  bitboard.blackPawns 							& dB.getCrudeWhitePawnCaptures();
			attackers |= (bitboard.blackQueens | bitboard.blackRooks)	& dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
			attackers |= (bitboard.blackQueens | bitboard.blackBishops) & dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
		}
		else {
			sqrInd = BitOperations.indexOfBit(bitboard.blackKing);
			dB = MoveDatabase.getByIndex(sqrInd);
			attackers  =  bitboard.whiteKnights							& dB.getCrudeKnightMoves();
			attackers |=  bitboard.whitePawns 							& dB.getCrudeBlackPawnCaptures();
			attackers |= (bitboard.whiteQueens | bitboard.whiteRooks)	& dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
			attackers |= (bitboard.whiteQueens | bitboard.whiteBishops) & dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
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
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(bitboard.whiteKing));
			rankPos 		= attRayMask.rankPos 			& bitboard.allOccupied;
			rankNeg 		= attRayMask.rankNeg 			& bitboard.allOccupied;
			filePos 		= attRayMask.filePos 			& bitboard.allOccupied;
			fileNeg 		= attRayMask.fileNeg 			& bitboard.allOccupied;
			diagonalPos 	= attRayMask.diagonalPos 		& bitboard.allOccupied;
			diagonalNeg 	= attRayMask.diagonalNeg 		& bitboard.allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos 	& bitboard.allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg 	& bitboard.allOccupied;
			straightSliders = bitboard.blackQueens | bitboard.blackRooks;
			diagonalSliders = bitboard.blackQueens | bitboard.blackBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos)			 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos)			 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos)	 	 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) 	 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg)		 	 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg)			 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg)	 	 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) 	 & bitboard.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		else {
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(bitboard.blackKing));
			rankPos 		= attRayMask.rankPos 			& bitboard.allOccupied;
			rankNeg 		= attRayMask.rankNeg 			& bitboard.allOccupied;
			filePos 		= attRayMask.filePos 			& bitboard.allOccupied;
			fileNeg 		= attRayMask.fileNeg 			& bitboard.allOccupied;
			diagonalPos 	= attRayMask.diagonalPos 		& bitboard.allOccupied;
			diagonalNeg 	= attRayMask.diagonalNeg 		& bitboard.allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos 	& bitboard.allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg 	& bitboard.allOccupied;
			straightSliders = bitboard.whiteQueens | bitboard.whiteRooks;
			diagonalSliders = bitboard.whiteQueens | bitboard.whiteBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos)			 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos)			 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos)	 	 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) 	 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg)		 	 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg)			 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg)	 	 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) 	 & bitboard.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		return pinnedPieces;
	}
	/**Generates and adds all pinned-piece-moves to the input parameter 'moves' and returns the set of pinned pieces as a long.
	 * 
	 * @param moves
	 * @return
	 */
	private long addPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0;
		int pinnedPieceInd, pinnedPiece, to;
		IntStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = bitboard.blackQueens | bitboard.blackRooks;
			diagonalSliders = bitboard.blackQueens | bitboard.blackBishops;
			attRayMask 		= RayMask.getByIndex(BitOperations.indexOfBit(bitboard.whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & bitboard.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - bitboard.whiteKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & bitboard.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - bitboard.whiteKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 6) {
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvances(bitboard.allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & bitboard.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - bitboard.whiteKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 6) {
						if (enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures((bitboard.allBlackPieces | (1L << (enPassantDestination = 40 + enPassantRights)) & attRayMask.diagonalPos)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, 3));
								else if (to >= 56) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures(bitboard.allBlackPieces & attRayMask.diagonalPos))) != 0) {
								if (to >= 56) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & bitboard.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - bitboard.whiteKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 6) {
						if (enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures((bitboard.allBlackPieces | (1L << (enPassantDestination = 40 + enPassantRights)) & attRayMask.antiDiagonalPos)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, 3));
								else if (to >= 56) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures(bitboard.allBlackPieces & attRayMask.antiDiagonalPos))) != 0) {
								if (to >= 56) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & bitboard.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMoves = BitOperations.serialize((bitboard.whiteKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & bitboard.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMoves = BitOperations.serialize(((bitboard.whiteKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 6) {
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvances(bitboard.allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & bitboard.allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMoves = BitOperations.serialize(((bitboard.whiteKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & bitboard.allOccupied) & bitboard.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & bitboard.allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMoves = BitOperations.serialize(((bitboard.whiteKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
		}
		else {
			straightSliders = bitboard.whiteQueens | bitboard.whiteRooks;
			diagonalSliders = bitboard.whiteQueens | bitboard.whiteBishops;
			attRayMask 		= RayMask.getByIndex(BitOperations.indexOfBit(bitboard.blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & bitboard.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - bitboard.blackKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & bitboard.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - bitboard.blackKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 12) {
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvances(bitboard.allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & bitboard.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - bitboard.blackKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & bitboard.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - bitboard.blackKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & bitboard.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMoves = BitOperations.serialize((bitboard.blackKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & bitboard.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMoves = BitOperations.serialize(((bitboard.blackKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 12) {
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvances(bitboard.allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & bitboard.allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMoves = BitOperations.serialize(((bitboard.blackKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 12) {
						if (this.enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures((bitboard.allWhitePieces | (1L << (enPassantDestination = 16 + enPassantRights)) & attRayMask.diagonalNeg)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, 3));
								else if (to < 8) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures(bitboard.allWhitePieces & attRayMask.diagonalNeg))) != 0) {
								if (to < 8) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & bitboard.allOccupied) & bitboard.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & bitboard.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMoves = BitOperations.serialize(((bitboard.blackKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next()));
					}
					else if (pinnedPiece == 12) {
						if (enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures((bitboard.allWhitePieces | (1L << (enPassantDestination = 16 + enPassantRights)) & attRayMask.antiDiagonalNeg)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, 3));
								else if (to < 8) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures(bitboard.allWhitePieces & attRayMask.antiDiagonalNeg))) != 0) {
								if (to < 8) {
									moves.add(new Move(pinnedPieceInd, to, 4));
									moves.add(new Move(pinnedPieceInd, to, 5));
									moves.add(new Move(pinnedPieceInd, to, 6));
									moves.add(new Move(pinnedPieceInd, to, 7));
								}
								else
									moves.add(new Move(pinnedPieceInd, to));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	private Queue<Move> generateNormalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits;
		int king, piece, to;
		IntStack pieces, moveList;
		Move move;
		Queue<Move> moves = new Queue<Move>();
		if (whitesTurn) {
			king = BitOperations.indexOfBit(bitboard.whiteKing);
			moveSet  = MoveDatabase.getByIndex(king).getWhiteKingMoves(bitboard.allNonWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to));
			}
			if ((whiteCastlingRights & 2) != 0) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & bitboard.allOccupied) == 0) {
					if ((move = moves.getTail()) != null && move.to == 3 && !isAttacked(2, false))
						moves.add(new Move(king, 2, 2));
				}
			}
			if ((whiteCastlingRights & 1) != 0) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & bitboard.allOccupied) == 0) {
					if (!isAttacked(5, false) && !isAttacked(6, false))
						moves.add(new Move(king, 6, 1));
				}
			}
			movablePieces = ~addPinnedPieceMoves(moves);
			pieceSet = bitboard.whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveDatabase.getByIndex(piece).getWhiteQueenMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getWhiteKnightMoves(bitboard.allNonWhiteOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveDatabase.getByIndex(piece).getWhitePawnMoves(bitboard.allBlackPieces, bitboard.allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to > 55) {
						moves.add(new Move(piece, to, 4));
						moves.add(new Move(piece, to, 5));
						moves.add(new Move(piece, to, 6));
						moves.add(new Move(piece, to, 7));
					}
					else
						moves.add(new Move(piece, to));
				}
			}
			if (this.enPassantRights != 8) {
				if ((pieceSet = MoveDatabase.getByIndex(to = 40 + enPassantRights).getBlackPawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						bitboard.allNonWhiteOccupied ^= enPassAttBits;
						bitboard.allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, false))
							moves.add(new Move(piece, to, 3));
						bitboard.allNonWhiteOccupied ^= enPassAttBits;
						bitboard.allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(bitboard.blackKing);
			moveSet	= MoveDatabase.getByIndex(king).getBlackKingMoves(bitboard.allNonBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to));
			}
			if ((blackCastlingRights & 1) != 0) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & bitboard.allOccupied) == 0) {
					if ((move = moves.getHead()) != null && move.to == 61 && !isAttacked(62, true))
						moves.add(new Move(king, 62, 1));
				}
			}
			if ((blackCastlingRights & 2) != 0) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & bitboard.allOccupied) == 0) {
					if (!isAttacked(58, true) && !isAttacked(59, true))
						moves.add(new Move(king, 58, 2));
				}
			}
			movablePieces = ~addPinnedPieceMoves(moves);
			pieceSet = bitboard.blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getBlackQueenMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveDatabase.getByIndex(piece).getBlackKnightMoves(bitboard.allNonBlackOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext())
					moves.add(new Move(piece, moveList.next()));
			}
			pieceSet = bitboard.blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveDatabase.getByIndex(piece).getBlackPawnMoves(bitboard.allWhitePieces, bitboard.allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to < 8) {
						moves.add(new Move(piece, to, 4));
						moves.add(new Move(piece, to, 5));
						moves.add(new Move(piece, to, 6));
						moves.add(new Move(piece, to, 7));
					}
					else
						moves.add(new Move(piece, to));
				}
			}
			if (this.enPassantRights != 8) {
				if ((pieceSet = MoveDatabase.getByIndex(to = 16 + enPassantRights).getWhitePawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						this.bitboard.allNonBlackOccupied ^= enPassAttBits;
						this.bitboard.allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, true))
							moves.add(new Move(piece, to, 3));
						this.bitboard.allNonBlackOccupied ^= enPassAttBits;
						this.bitboard.allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	private Queue<Move> generateCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare, checkerBlockerSquare, king, to;
		IntStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.whitesTurn) {
			king 			 = BitOperations.indexOfBit(bitboard.whiteKing);
			movablePieces	 = ~getPinnedPieces(true);
			kingDb			 = MoveDatabase.getByIndex(king);
			kingMoveSet		 = kingDb.getWhiteKingMoves(bitboard.allNonWhiteOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1  		 = BitOperations.indexOfBit(checkers);
				checkerPiece1 	 = this.offsetBoard[checker1];
				dB				 = MoveDatabase.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~bitboard.whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					if (offsetBoard[checkerAttackerSquare] == 6) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, 4));
							moves.add(new Move(checkerAttackerSquare, checker1, 5));
							moves.add(new Move(checkerAttackerSquare, checker1, 6));
							moves.add(new Move(checkerAttackerSquare, checker1, 7));
						}
						else if (enPassantRights != 8 && checker1 == 32 + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 + 8, 3));
						else
							moves.add(new Move(checkerAttackerSquare, checker1));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1));
				}
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied) & kingDb.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied));
							if (promotionOnAttackPossible && (bitboard.whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied) & kingDb.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								if (promotionOnBlockPossible && offsetBoard[checkerBlockerSquare] == 6) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 4));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 5));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 6));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 7));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention));
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(bitboard.allNonWhiteOccupied, (bitboard.allOccupied^bitboard.whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (bitboard.whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied) & kingDb.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								if (promotionOnBlockPossible && offsetBoard[checkerBlockerSquare] == 6) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 4));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 5));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 6));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 7));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied) & kingDb.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								if (promotionOnBlockPossible && offsetBoard[checkerBlockerSquare] == 6) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 4));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 5));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 6));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 7));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to));
				}
			}
			else {
				checker1 		= BitOperations.indexOfLSBit(checkers);
				checkerPiece1 	= offsetBoard[checker1];
				checker2		= BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 	= offsetBoard[checker2];
				dB = MoveDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(bitboard.allNonWhiteOccupied, (bitboard.allOccupied^bitboard.whiteKing));
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
				dB = MoveDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(bitboard.allNonWhiteOccupied, (bitboard.allOccupied^bitboard.whiteKing));
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
						moves.add(new Move(king, to));
				}
			}
		}
		else {
			king 	  		= BitOperations.indexOfBit(bitboard.blackKing);
			movablePieces 	= ~getPinnedPieces(false);
			kingDb	 		= MoveDatabase.getByIndex(king);
			kingMoveSet		= kingDb.getBlackKingMoves(bitboard.allNonBlackOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1  		= BitOperations.indexOfBit(checkers);
				checkerPiece1	= offsetBoard[checker1];
				dB				= MoveDatabase.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~bitboard.blackKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					if (this.offsetBoard[checkerAttackerSquare] == 12) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, 4));
							moves.add(new Move(checkerAttackerSquare, checker1, 5));
							moves.add(new Move(checkerAttackerSquare, checker1, 6));
							moves.add(new Move(checkerAttackerSquare, checker1, 7));
						}
						else if (enPassantRights != 8 && checker1 == 24 + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, checker1 - 8, 3));
						else
							moves.add(new Move(checkerAttackerSquare, checker1));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1));
				}
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied) & kingDb.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied));
							if (promotionOnAttackPossible && (bitboard.blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied) & kingDb.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								if (promotionOnBlockPossible && offsetBoard[checkerBlockerSquare] == 12) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 4));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 5));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 6));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 7));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention));
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(bitboard.allNonBlackOccupied, (bitboard.allOccupied^bitboard.blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (bitboard.blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied) & kingDb.getBlackRookMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								if (promotionOnBlockPossible && offsetBoard[checkerBlockerSquare] == 12) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 4));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 5));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 6));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 7));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention));
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(bitboard.allNonWhiteOccupied, bitboard.allOccupied) & kingDb.getBlackBishopMoves(bitboard.allNonBlackOccupied, bitboard.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								if (promotionOnBlockPossible && offsetBoard[checkerBlockerSquare] == 12) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 4));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 5));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 6));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, 7));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention));
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to));
				}
			}
			else {
				checker1 		= BitOperations.indexOfLSBit(checkers);
				checkerPiece1 	= offsetBoard[checker1];
				checker2		= BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 	= this.offsetBoard[checker2];
				dB = MoveDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(bitboard.allNonBlackOccupied, (bitboard.allOccupied^bitboard.blackKing));
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
				dB = MoveDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(bitboard.allNonBlackOccupied, (bitboard.allOccupied^bitboard.blackKing));
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
						moves.add(new Move(king, to));
				}
			}
		}
		return moves;
	}
	/**Generates a queue of Move objects that represents all the legal moves from the current position.
	 * 
	 * @return
	 */
	public Queue<Move> generateMoves() {
		if (check)
			return this.generateCheckEvasionMoves();
		else
			return this.generateNormalMoves();
	}
	/**Makes a single move from a LongQueue generated by generateMoves.
	 * 
	 * @param move
	 */
	public void makeMove(Move move) {
		int moved, captured, enPassantVictimSqr;
		Square enPassantVictimSquare;
		Square from = Square.getByIndex(move.from);
		Square to   = Square.getByIndex(move.to);
		switch (move.type) {
			case 0: {
				moved = offsetBoard[move.from];
				offsetBoard[move.from] = 0;
				captured = offsetBoard[move.to];
				offsetBoard[move.to] = moved;
				bitboard.makeMove(moved, captured, from, to);
			}
			break;
			case 1: {
				if (whitesTurn) {
					moved = 1;
					offsetBoard[7]	 = 0;
					offsetBoard[5]	 = 3;
					bitboard.makeMove(3, 0, Square.H1, Square.F1);
				}
				else {
					moved = 7;
					offsetBoard[63] = 0;
					offsetBoard[61] = 9;
					bitboard.makeMove(9, 0, Square.H8, Square.F8);
				}
				captured = 0;
				offsetBoard[move.from] = 0;
				offsetBoard[move.to] = moved;
				bitboard.makeMove(moved, 0, from, to);
			}
			break;
			case 2: {
				if (whitesTurn) {
					moved = 1;
					offsetBoard[0]	 = 0;
					offsetBoard[3]	 = 3;
					bitboard.makeMove(3, 0, Square.A1, Square.D1);
				}
				else {
					moved = 7;
					offsetBoard[56] = 0;
					offsetBoard[59] = 9;
					bitboard.makeMove(9, 0, Square.A8, Square.D8);
				}
				captured = 0;
				offsetBoard[move.from] = 0;
				offsetBoard[move.to] = moved;
				bitboard.makeMove(moved, 0, from, to);
			}
			break;
			case 3: {
				if (whitesTurn) {
					moved = 6;
					captured = 12;
					enPassantVictimSqr = move.to - 8;
				}
				else {
					moved = 12;
					captured = 6;
					enPassantVictimSqr = move.to + 8;
				}
				offsetBoard[move.from] = 0;
				offsetBoard[move.to] = moved;
				offsetBoard[enPassantVictimSqr] = 0;
				enPassantVictimSquare = Square.getByIndex(enPassantVictimSqr);
				bitboard.makeMove(moved, captured, from, enPassantVictimSquare);
				bitboard.makeMove(moved, 0, enPassantVictimSquare, to);
			}
			break;
			case 4: {
				captured = offsetBoard[move.to];
				if (whitesTurn) {
					moved = 6;
					offsetBoard[move.to] = 2;
					bitboard.makeMove(2, captured, Square.NULL, to);
				}
				else {
					moved = 12;
					offsetBoard[move.to] = 8;
					bitboard.makeMove(8, captured, Square.NULL, to);
				}
				offsetBoard[move.from] = 0;
				bitboard.makeMove(moved, 0, from, Square.NULL);
			}
			break;
			case 5: {
				captured = offsetBoard[move.to];
				if (whitesTurn) {
					moved = 6;
					offsetBoard[move.to] = 3;
					bitboard.makeMove(3, captured, Square.NULL, to);
				}
				else {
					moved = 12;
					offsetBoard[move.to] = 9;
					bitboard.makeMove(9, captured, Square.NULL, to);
				}
				offsetBoard[move.from] = 0;
				bitboard.makeMove(moved, 0, from, Square.NULL);
			}
			break;
			case 6: {
				captured = offsetBoard[move.to];
				if (whitesTurn) {
					moved = 6;
					offsetBoard[move.to] = 4;
					bitboard.makeMove(4, captured, Square.NULL, to);
				}
				else {
					moved = 12;
					offsetBoard[move.to] = 10;
					bitboard.makeMove(10, captured, Square.NULL, to);
				}
				offsetBoard[move.from] = 0;
				bitboard.makeMove(moved, 0, from, Square.NULL);
			}
			break;
			case 7: {
				captured = offsetBoard[move.to];
				if (whitesTurn) {
					moved = 6;
					offsetBoard[move.to] = 5;
					bitboard.makeMove(5, captured, Square.NULL, to);
				}
				else {
					moved = 12;
					offsetBoard[move.to] = 11;
					bitboard.makeMove(11, captured, Square.NULL, to);
				}
				offsetBoard[move.from] = 0;
				bitboard.makeMove(moved, 0, from, Square.NULL);
			}
			break;
			default: {
				moved = -1;
				captured = -1;
			}
		}
		moveList.add(move);
		unmakeRegisterHistory.add(new UnmakeRegister(moved, captured, whiteCastlingRights, blackCastlingRights, enPassantRights, fiftyMoveRuleClock, repetitions, checkers));
		setTurn();
		setCastlingRights();
		setEnPassantRights(move.from, move.to, moved);
		setCheck();
		setMoveIndices(moved, captured);
		setKeys();
		setRepetitions();
	}
	/**Reverts the state of the instance to that before the last move made in every aspect necessary for the traversal of the game tree. Used within the engine.*/
	public void unmakeMove() {
		UnmakeRegister positionInfo = unmakeRegisterHistory.pop();
		Move move 					= moveList.pop();
		int moved 	 = positionInfo.movedPiece;
		int captured = positionInfo.capturedPiece;
		int enPassantVictimSquare;
		Square fromBit = Square.getByIndex(move.from);
		Square toBit   = Square.getByIndex(move.to);
		setTurn();
		switch (move.type) {
			case 0: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = captured;
				bitboard.makeMove(moved, captured, fromBit, toBit);
			}
			break;
			case 1: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = 0;
				bitboard.makeMove(moved, 0, fromBit, toBit);
				if (whitesTurn) {
					offsetBoard[7] = 3;
					offsetBoard[5] = 0;
					bitboard.makeMove(3, 0, Square.F1, Square.H1);
				}
				else {
					offsetBoard[63] = 9;
					offsetBoard[61] = 0;
					bitboard.makeMove(3, 0, Square.F8, Square.H8);
				}
			}
			break;
			case 2: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = 0;
				bitboard.makeMove(moved, 0, fromBit, toBit);
				if (whitesTurn) {
					offsetBoard[0] = 3;
					offsetBoard[3] = 0;
					bitboard.makeMove(3, 0, Square.D1, Square.A1);
				}
				else {
					offsetBoard[56] = 9;
					offsetBoard[59] = 0;
					bitboard.makeMove(3, 0, Square.D8, Square.A8);
				}
			}
			break;
			case 3: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = 0;
				bitboard.makeMove(moved, 0, fromBit, toBit);
				if (whitesTurn)
					enPassantVictimSquare = move.to - 8;
				else
					enPassantVictimSquare = move.to + 8;
				offsetBoard[enPassantVictimSquare] = captured;
				bitboard.makeMove(0, captured, Square.NULL, Square.getByIndex(enPassantVictimSquare));
			}
			break;
			case 4: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = captured;
				bitboard.makeMove(moved, 0, fromBit, Square.NULL);
				if (whitesTurn)
					bitboard.makeMove(2, 0, toBit, Square.NULL);
				else
					bitboard.makeMove(8, 0, toBit, Square.NULL);
				bitboard.makeMove(0, captured, Square.NULL, toBit);
			}
			break;
			case 5: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = captured;
				bitboard.makeMove(moved, 0, fromBit, Square.NULL);
				if (whitesTurn)
					bitboard.makeMove(3, 0, toBit, Square.NULL);
				else
					bitboard.makeMove(9, 0, toBit, Square.NULL);
				bitboard.makeMove(0, captured, Square.NULL, toBit);
			}
			break;
			case 6: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = captured;
				bitboard.makeMove(moved, 0, fromBit, Square.NULL);
				if (whitesTurn)
					bitboard.makeMove(4, 0, toBit, Square.NULL);
				else
					bitboard.makeMove(10, 0, toBit, Square.NULL);
				bitboard.makeMove(0, captured, Square.NULL, toBit);
			}
			break;
			case 7: {
				offsetBoard[move.from] = moved;
				offsetBoard[move.to] = captured;
				bitboard.makeMove(moved, 0, fromBit, Square.NULL);
				if (whitesTurn)
					bitboard.makeMove(5, 0, toBit, Square.NULL);
				else
					bitboard.makeMove(11, 0, toBit, Square.NULL);
				bitboard.makeMove(0, captured, Square.NULL, toBit);
			}
		}
		whiteCastlingRights 	= positionInfo.whiteCastlingRights;
		blackCastlingRights 	= positionInfo.blackCastlingRights;
		enPassantRights 		= positionInfo.enPassantRights;
		fiftyMoveRuleClock		= positionInfo.fiftyMoveRuleClock;
		repetitions				= positionInfo.repetitions;
		checkers 				= positionInfo.checkers;
		if (checkers != 0)
			check = true;
		else
			check = false;
		zobristKeyHistory[halfMoveIndex] = 0;
		zobristKey = zobristKeyHistory[--halfMoveIndex];
	}
	/**Makes a move specified by user input. If it is legal and the command is valid ([origin square + destination square] as e.g.: b1a3 without any spaces; in case of promotion,
	 * the FEN notation of the piece the pawn is wished to be promoted to should be appended to the command as in c7c8q; the parser is not case sensitive), it returns true, else
	 * false.
	 * 
	 * @return
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
				moves = generateMoves();
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
					fen += Piece.fenNotation(piece);
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
		if (whiteCastlingRights != 0) {
			if ((whiteCastlingRights & 1) != 0)
				fen += 'K';
			if ((whiteCastlingRights & 2) != 0)
				fen += 'Q';
		}
		if (blackCastlingRights != 0) {
			if ((blackCastlingRights & 1) != 0)
				fen += 'k';
			if ((blackCastlingRights & 2) != 0)
				fen += 'q';
		}
		if (whiteCastlingRights == 0 && blackCastlingRights == 0)
			fen += '-';
		fen += ' ';
		if (enPassantRights == 8)
			fen += '-';
		else {
			fen += (char)(enPassantRights + 'a');
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
		Bitboard.printBitmapToConsole(bitboard.allOccupied);
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
						System.out.print(" " + Piece.fenNotation(offsetBoard[(i - 1)*4 + j/2]) + " ");
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
		if ((whiteCastlingRights & 1) != 0)
			System.out.print("K");
		if ((whiteCastlingRights & 2) != 0)
			System.out.print("Q");
		if ((blackCastlingRights & 1) != 0)
			System.out.print("k");
		if ((blackCastlingRights & 2) != 0)
			System.out.print("q");
		if (whiteCastlingRights == 0 && blackCastlingRights == 0)
			System.out.print("-");
		System.out.println();
		System.out.printf("%-23s ", "En passant rights:");
		if (enPassantRights == 8)
			System.out.println("-");
		else
			System.out.println((char)('a' + enPassantRights));
		System.out.printf("%-23s " + halfMoveIndex + "\n", "Half-move index:");
		System.out.printf("%-23s " + fiftyMoveRuleClock + "\n", "Fifty-move rule clock:");
		System.out.printf("%-23s " + Long.toHexString(zobristKey) + "\n", "Hash key:");
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
		moves = generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			makeMove(move);
			leafNodes += perft(depth - 1);
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
		moves = generateMoves();
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
		moves = generateMoves();
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
		Queue<Move> moves;
		Stack<Move> chronoHistory;
		Move move;
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
			moves = generateMoves();
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
					nodes = quickPerft(depth);
				else
					nodes = 1;
				System.out.printf("%3d. %-8s nodes: %d\n", i, move, nodes);
				total += nodes;
				unmakeMove();
			}
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
		Queue<Move> moves = generateMoves();
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
