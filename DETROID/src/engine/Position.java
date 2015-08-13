package engine;

import util.*;
import engine.Board.*;

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
	
	//bitboards for each piece type
	private long whiteKing;
	private long whiteQueens;
	private long whiteRooks;
	private long whiteBishops;
	private long whiteKnights;
	private long whitePawns;
	
	private long blackKing;
	private long blackQueens;
	private long blackRooks;
	private long blackBishops;
	private long blackKnights;
	private long blackPawns;
	
	//bitboard unions maintained for faster processing of positions
	private long allWhitePieces;
	private long allBlackPieces;
	
	private long allNonWhiteOccupied;
	private long allNonBlackOccupied;
	
	private long allOccupied;
	private long allEmpty;
	
	private int[] offsetBoard;										//a complimentary board data-structure to the bitboards to efficiently detect pieces on specific squares
	
	private boolean whitesTurn = true;
	
	private long checkers = 0;										//a bitboard of all the pieces that attack the color to move's king
	private boolean check = false;
	
	private int plyIndex = 0;										//the count of the current ply/half-move
	private long fiftyMoveRuleClock = 0;							//the number of moves made since the last pawn move or capture; the choice of type fell on long due to data loss when int is shifted beyond the 32nd bit in the move integer
	
	private int enPassantRights = 8;								//denotes the file on which en passant is possible; 8 means no en passant rights
	
	private int whiteCastlingRights = 3;							//denotes to what extent it would still be possible to castle regardless of whether it is actually legally executable in the current position
	private int blackCastlingRights = 3;							//0 - no castling rights, 1 - king-side castling only, 2 - queen-side castling only, 3 - all castling rights
	
	private LongStack moveList = new LongStack();					//a stack of all the moves made so far
	private LongStack positionInfoHistory = new LongStack(); 		//a stack history of castling rights, en passant rights, fifty-move rule clock, repetitions, and check info.
	
	private ZobristKeyGenerator keyGen = new ZobristKeyGenerator(); //a Zobrist key generator for hashing the board
	
	private long zobristKey;										//the Zobrist key that is fairly close to a unique representation of the state of the Board instance in one number
	private long[] zobristKeyHistory;								//all the positions that have occured so far represented in Zobrist keys.
	
	private long repetitions = 0;									//the number of times the current position has occured before; the choice of type fell on long due to data loss when int is shifted beyond the 32nd bit in the move integer
	
	/**Initializes an instance of Board and sets up the pieces in their initial position.*/
	public Position() {
		this.initializeBitBoards();
		this.initializeOffsetBoard();
		this.initializeZobristKeys();
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
					this.plyIndex = moveIndex;
				else
					this.plyIndex = 0;
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
		ranks = board.split("/");
		if (ranks.length != 8)
			throw new IllegalArgumentException("The board position representation does not have eight ranks.");
		this.offsetBoard = new int[64];
		for (int i = 0; i < 64; i++)
			this.offsetBoard[i] = 0;
		for (int i = 7; i >= 0; i--) {
			rank = ranks[i];
			for (int j = 0; j < rank.length(); j++) {
				piece = rank.charAt(j);
				pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					switch (piece) {
						case 'K': {
							this.offsetBoard[index] = 1;
							this.whiteKing		 = Square.getBitmapByIndex(index);
						}
						break;
						case 'Q': {
							this.offsetBoard[index] = 2;
							this.whiteQueens	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'R': {
							this.offsetBoard[index] = 3;
							this.whiteRooks		|= Square.getBitmapByIndex(index);
						}
						break;
						case 'B': {
							this.offsetBoard[index] = 4;
							this.whiteBishops	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'N': {
							this.offsetBoard[index] = 5;
							this.whiteKnights	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'P': {
							this.offsetBoard[index] = 6;
							this.whitePawns		|= Square.getBitmapByIndex(index);
						}
						break;
						case 'k': {
							this.offsetBoard[index] = 7;
							this.blackKing		 = Square.getBitmapByIndex(index);
						}
						break;
						case 'q': {
							this.offsetBoard[index] = 8;
							this.blackQueens	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'r': {
							this.offsetBoard[index] = 9;
							this.blackRooks		|= Square.getBitmapByIndex(index);
						}
						break;
						case 'b': {
							this.offsetBoard[index] = 10;
							this.blackBishops	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'n': {
							this.offsetBoard[index] = 11;
							this.blackKnights	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'p': {
							this.offsetBoard[index] = 12;
							this.blackPawns		|= Square.getBitmapByIndex(index);
						}
					}
					index++;
				}
			}
		}
		this.initializeCollections();
		if (turn.toLowerCase().compareTo("w") == 0)
			this.whitesTurn = true;
		else
			this.whitesTurn = false;
		this.whiteCastlingRights = 0;
		if (castling.contains("K"))
			this.whiteCastlingRights += 1;
		if (castling.contains("Q"))
			this.whiteCastlingRights += 2;
		this.blackCastlingRights = 0;
		if (castling.contains("k"))
			this.blackCastlingRights += 1;
		if (castling.contains("q"))
			this.blackCastlingRights += 2;
		if (enPassant.compareTo("-") == 0)
			this.enPassantRights = 8;
		else
			this.enPassantRights = enPassant.toLowerCase().charAt(0) - 'a';
		this.setCheck();
		this.initializeZobristKeys();
	}
	private void initializeCollections() {
		this.allWhitePieces		 =  this.whiteKing | this.whiteQueens | this.whiteRooks | this.whiteBishops | this.whiteKnights | this.whitePawns;
		this.allBlackPieces		 =  this.blackKing | this.blackQueens | this.blackRooks | this.blackBishops | this.blackKnights | this.blackPawns;
		this.allNonWhiteOccupied = ~this.allWhitePieces;
		this.allNonBlackOccupied = ~this.allBlackPieces;
		this.allOccupied		 =  this.allWhitePieces | this.allBlackPieces;
		this.allEmpty			 = ~this.allOccupied;
	}
	private void initializeBitBoards() {
		this.whiteKing		=  Piece.WHITE_KING.initPosBitmap;
		this.whiteQueens	=  Piece.WHITE_QUEEN.initPosBitmap;
		this.whiteRooks		=  Piece.WHITE_ROOK.initPosBitmap;
		this.whiteBishops	=  Piece.WHITE_BISHOP.initPosBitmap;
		this.whiteKnights	=  Piece.WHITE_KNIGHT.initPosBitmap;
		this.whitePawns		=  Piece.WHITE_PAWN.initPosBitmap;
		
		this.blackKing		=  Piece.BLACK_KING.initPosBitmap;
		this.blackQueens	=  Piece.BLACK_QUEEN.initPosBitmap;
		this.blackRooks		=  Piece.BLACK_ROOK.initPosBitmap;
		this.blackBishops	=  Piece.BLACK_BISHOP.initPosBitmap;
		this.blackKnights	=  Piece.BLACK_KNIGHT.initPosBitmap;
		this.blackPawns		=  Piece.BLACK_PAWN.initPosBitmap;
		this.initializeCollections();
	}
	private void initializeOffsetBoard() {
		this.offsetBoard = new int[64];
		this.offsetBoard[0] =  Piece.WHITE_ROOK.numericNotation;
		this.offsetBoard[1] =  Piece.WHITE_KNIGHT.numericNotation;
		this.offsetBoard[2] =  Piece.WHITE_BISHOP.numericNotation;
		this.offsetBoard[3] =  Piece.WHITE_QUEEN.numericNotation;
		this.offsetBoard[4] =  Piece.WHITE_KING.numericNotation;
		this.offsetBoard[5] =  Piece.WHITE_BISHOP.numericNotation;
		this.offsetBoard[6] =  Piece.WHITE_KNIGHT.numericNotation;
		this.offsetBoard[7] =  Piece.WHITE_ROOK.numericNotation;
		for (int i = 8; i < 16; i++)
			this.offsetBoard[i] = Piece.WHITE_PAWN.numericNotation;
		
		for (int i = 48; i < 56; i++)
			this.offsetBoard[i] = Piece.BLACK_PAWN.numericNotation;
		this.offsetBoard[56] = Piece.BLACK_ROOK.numericNotation;
		this.offsetBoard[57] = Piece.BLACK_KNIGHT.numericNotation;
		this.offsetBoard[58] = Piece.BLACK_BISHOP.numericNotation;
		this.offsetBoard[59] = Piece.BLACK_QUEEN.numericNotation;
		this.offsetBoard[60] = Piece.BLACK_KING.numericNotation;
		this.offsetBoard[61] = Piece.BLACK_BISHOP.numericNotation;
		this.offsetBoard[62] = Piece.BLACK_KNIGHT.numericNotation;
		this.offsetBoard[63] = Piece.BLACK_ROOK.numericNotation;
	}
	private void initializeZobristKeys() {
		//"The longest decisive tournament game is Fressinet-Kosteniuk, Villandry 2007, which Kosteniuk won in 237 moves." - half of that is used as the initial length of the history array
		zobristKeyHistory = new long[237];
		this.zobristKey = keyGen.hash(this);
		this.zobristKeyHistory[0] = this.zobristKey;
	}
	/**Returns a bitmap representing the white king's position.*/
	public long getWhiteKing() {
		return this.whiteKing;
	}
	/**Returns a bitmap representing the white queens' position.*/
	public long getWhiteQueens() {
		return this.whiteQueens;
	}
	/**Returns a bitmap representing the white rooks' position.*/
	public long getWhiteRooks() {
		return this.whiteRooks;
	}
	/**Returns a bitmap representing the white bishops' position.*/
	public long getWhiteBishops() {
		return this.whiteBishops;
	}
	/**Returns a bitmap representing the white knights' position.*/
	public long getWhiteKnights() {
		return this.whiteKnights;
	}
	/**Returns a bitmap representing the white pawns' position.*/
	public long getWhitePawns() {
		return this.whitePawns;
	}
	/**Returns a bitmap representing the black king's position.*/
	public long getBlackKing() {
		return this.blackKing;
	}
	/**Returns a bitmap representing the black queens' position.*/
	public long getBlackQueens() {
		return this.blackQueens;
	}
	/**Returns a bitmap representing the black rooks' position.*/
	public long getBlackRooks() {
		return this.blackRooks;
	}
	/**Returns a bitmap representing the black bishops' position.*/
	public long getBlackBishops() {
		return this.blackBishops;
	}
	/**Returns a bitmap representing the black knights' position.*/
	public long getBlackKnights() {
		return this.blackKnights;
	}
	/**Returns a bitmap representing the black pawns' position.*/
	public long getBlackPawns() {
		return this.blackPawns;
	}
	/**Returns an array of longs representing the current position with each array element denoting a square and the value in the element denoting the piece on the square.*/
	public int[] getOffsetBoard() {
		return this.offsetBoard;
	}
	/**Returns whether it is white's turn or not.*/
	public boolean getTurn() {
		return this.whitesTurn;
	}
	/**Returns whether the color to move's king is in check.*/
	public boolean getCheck() {
		return this.check;
	}
	/**Returns the current ply/half-move index.*/
	public int getPlyIndex() {
		return this.plyIndex;
	}
	/**Returns the number of half-moves made since the last pawn-move or capture.*/
	public long getFiftyMoveRuleClock() {
		return this.fiftyMoveRuleClock;
	}
	/**Returns a number denoting white's castling rights.
	 * 0 - no castling rights
	 * 1 - king-side castling only
	 * 2 - queen-side castling only
	 * 3 - all castling rights
	 */
	public int getWhiteCastlingRights() {
		return this.whiteCastlingRights;
	}
	/**Returns a number denoting black's castling rights.
	 * 0 - no castling rights
	 * 1 - king-side castling only
	 * 2 - queen-side castling only
	 * 3 - all castling rights
	 */
	public int getBlackCastlingRights() {
		return this.blackCastlingRights;
	}
	/**Returns a number denoting the file on which in the current position en passant is possible.
	 * 0 - a; 1 - b; ...; 7 - h; 8 - no en passant rights
	 */
	public int getEnPassantRights() {
		return this.enPassantRights;
	}
	/**Returns the number of times the current position has previously occured since the initialization of the object.*/
	public long getRepetitions() {
		return this.repetitions;
	}
	/**Returns the 64-bit Zobrist key of the current position. A Zobrist key is used to almost uniquely hash a chess position to an integer.*/
	public long getZobristKey() {
		return this.zobristKey;
	}
	/**Returns a long containing all relevant information about the last move made according to the Move enum. if the move history list is empty, it returns 0.*/
	public long getLastMove() {
		return this.moveList.getHead();
	}
	/**Returns a long containing some information about the previous position according to the PositionInfo enum.*/
	public long getPreviousPositionInfo() {
		return this.positionInfoHistory.getHead();
	}
	private void setBitboards(int moved, int captured, long fromBit, long toBit) {
		if (this.whitesTurn) {
			switch (moved) {
				case 1: {
					this.whiteKing 			 ^=  fromBit;
					this.whiteKing 			 ^=  toBit;
					this.allWhitePieces 	 ^=  fromBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 2: {
					this.whiteQueens		 ^=  fromBit;
					this.whiteQueens 		 ^=  toBit;
					this.allWhitePieces 	 ^=  fromBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 3: {
					this.whiteRooks 		 ^=  fromBit;
					this.whiteRooks 		 ^=  toBit;
					this.allWhitePieces 	 ^=  fromBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 4: {
					this.whiteBishops 		 ^=  fromBit;
					this.whiteBishops 		 ^=  toBit;
					this.allWhitePieces 	 ^=  fromBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 5: {
					this.whiteKnights 		 ^=  fromBit;
					this.whiteKnights 		 ^=  toBit;
					this.allWhitePieces 	 ^=  fromBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 6: {
					this.whitePawns 		 ^=  fromBit;
					this.whitePawns 		 ^=  toBit;
					this.allWhitePieces 	 ^=  fromBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
			}
			switch (captured) {
				case 0:
				break;
				case 8: {
					this.blackQueens 		 ^=  toBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 9: {
					this.blackRooks 		 ^=  toBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 10: {
					this.blackBishops 		 ^=  toBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 11: {
					this.blackKnights 		 ^=  toBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 12: {
					this.blackPawns 		 ^=  toBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
			}
			this.allOccupied 		  =  this.allWhitePieces | this.allBlackPieces;
			this.allEmpty			  = ~this.allOccupied;
		}
		else {
			switch (moved) {
				case 7: {
					this.blackKing 			 ^=  fromBit;
					this.blackKing 			 ^=  toBit;
					this.allBlackPieces 	 ^=  fromBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 8: {
					this.blackQueens		 ^=  fromBit;
					this.blackQueens 		 ^=  toBit;
					this.allBlackPieces 	 ^=  fromBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 9: {
					this.blackRooks 		 ^=  fromBit;
					this.blackRooks 		 ^=  toBit;
					this.allBlackPieces 	 ^=  fromBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 10: {
					this.blackBishops 		 ^=  fromBit;
					this.blackBishops 		 ^=  toBit;
					this.allBlackPieces 	 ^=  fromBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 11: {
					this.blackKnights 		 ^=  fromBit;
					this.blackKnights 		 ^=  toBit;
					this.allBlackPieces 	 ^=  fromBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
				break;
				case 12: {
					this.blackPawns 		 ^=  fromBit;
					this.blackPawns 		 ^=  toBit;
					this.allBlackPieces 	 ^=  fromBit;
					this.allBlackPieces 	 ^=  toBit;
					this.allNonBlackOccupied  = ~this.allBlackPieces;
				}
			}
			switch (captured) {
				case 0:
				break;
				case 2: {
					this.whiteQueens 		 ^=  toBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 3: {
					this.whiteRooks 		 ^=  toBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 4: {
					this.whiteBishops 		 ^=  toBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 5: {
					this.whiteKnights 		 ^=  toBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
				break;
				case 6: {
					this.whitePawns 		 ^=  toBit;
					this.allWhitePieces 	 ^=  toBit;
					this.allNonWhiteOccupied  = ~this.allWhitePieces;
				}
			}
			this.allOccupied 		  =  this.allWhitePieces | this.allBlackPieces;
			this.allEmpty			  = ~this.allOccupied;
		}
	}
	private void setTurn() {
		this.whitesTurn = !this.whitesTurn;
	}
	private void setMoveIndices(int moved, int captured) {
		this.plyIndex++;
		if (captured != 0 || moved == 6 || moved == 12)
			this.fiftyMoveRuleClock = 0;
		else
			this.fiftyMoveRuleClock++;
	}
	private void setEnPassantRights(int from, int to, int movedPiece) {
		if (movedPiece == 6) {
			if (to - from == 16) {
				this.enPassantRights = to%8;
				return;
			}
		}
		else if (movedPiece == 12) {
			if (from - to == 16) {
				this.enPassantRights = to%8;
				return;
			}
		}
		this.enPassantRights = 8;
	}
	private void setCastlingRights() {
		if (this.whitesTurn) {
			switch (this.whiteCastlingRights) {
				case 0: return;
				case 1: {
					if (this.offsetBoard[4] != 1 || this.offsetBoard[7] != 3)
						this.whiteCastlingRights = 0;
				}
				break;
				case 2: {
					if (this.offsetBoard[4] != 1 || this.offsetBoard[0] != 3)
						this.whiteCastlingRights = 0;
				}
				break;
				case 3: {
					if (this.offsetBoard[4] == 1) {
						if (this.offsetBoard[7] != 3)
							this.whiteCastlingRights -= 1;
						if (this.offsetBoard[0] != 3)
							this.whiteCastlingRights -= 2;
					}
					else
						this.whiteCastlingRights = 0;
				}
			}
		}
		else {
			switch (this.blackCastlingRights) {
				case 0:
					return;
				case 1: {
					if (this.offsetBoard[60] != 7 || this.offsetBoard[63] != 9)
						this.blackCastlingRights = 0;
				}
				break;
				case 2: {
					if (this.offsetBoard[60] != 7 || this.offsetBoard[56] != 9)
						this.blackCastlingRights = 0;
				}
				break;
				case 3: {
					if (this.offsetBoard[60] == 7) {
						if (this.offsetBoard[63] != 9)
							this.blackCastlingRights -= 1;
						if (this.offsetBoard[56] != 9)
							this.blackCastlingRights -= 2;
					}
					else
						this.blackCastlingRights = 0;
				}
			}
		}
	}
	private void setCheck() {
		if (this.whitesTurn) {
			this.checkers = getAttackers(BitOperations.indexOfBit(this.whiteKing), false);
			if (this.checkers == 0)
				this.check = false;
			else
				this.check = true;
		}
		else {
			this.checkers = getAttackers(BitOperations.indexOfBit(this.blackKing), true);
			if (this.checkers == 0)
				this.check = false;
			else
				this.check = true;
		}
	}
	private void setKeys() {
		this.zobristKey = keyGen.updateKey(this);
		this.zobristKeyHistory[this.plyIndex] = this.zobristKey;
	}
	private void extendKeyHistory() {
		long[] temp;
		if (this.zobristKeyHistory.length - this.plyIndex <= 75) {
			temp = this.zobristKeyHistory;
			this.zobristKeyHistory = new long[this.zobristKeyHistory.length + 25];
			for (int i = 0; i < temp.length; i++)
				this.zobristKeyHistory[i] = temp[i];
		}
	}
	private void setPositionInfo() {
		long positionInfo, checker2;
		positionInfo = this.whiteCastlingRights |
		 			   (this.blackCastlingRights << PositionRegister.BLACK_CASTLING_RIGHTS.shift) |
		 			   (this.enPassantRights << PositionRegister.EN_PASSANT_RIGHTS.shift) |
		 			   (this.fiftyMoveRuleClock << PositionRegister.FIFTY_MOVE_RULE_CLOCK.shift) |
		 			   (this.repetitions << PositionRegister.REPETITIONS.shift);
		if (this.check) {
			positionInfo |= (((long)BitOperations.indexOfLSBit(this.checkers)) << PositionRegister.CHECKER1.shift);
			if ((checker2 = (long)BitOperations.indexOfBit(BitOperations.resetLSBit(this.checkers))) != 0) {
				positionInfo |= (2L << PositionRegister.CHECK.shift) |
								(checker2 << PositionRegister.CHECKER2.shift);
			}
			else
				positionInfo |= (1L << PositionRegister.CHECK.shift);
		}
		this.positionInfoHistory.add(positionInfo);
	}
	/**Should be used before resetMoveIndices().*/
	private void setRepetitions() {
		if (this.fiftyMoveRuleClock >= 4) {
			for (int i = this.plyIndex; i >= (this.plyIndex - this.fiftyMoveRuleClock); i -= 2) {
				if (this.zobristKeyHistory[i] == this.zobristKey)
					this.repetitions++;
			}
		}
		else
			this.repetitions = 0;
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
			if ((this.whiteKing		& dB.getCrudeKingMoves()) != 0)
				return true;
			if ((this.whiteKnights 	& dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((this.whitePawns 	& dB.getCrudeBlackPawnCaptures()) != 0)
				return true;
			if (((this.whiteQueens | this.whiteRooks) 	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied)) != 0)
				return true;
			if (((this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied)) != 0)
				return true;
			if (this.offsetBoard[sqrInd] == 12 && this.enPassantRights != 8 && sqrInd == 32 + this.enPassantRights) {
				if ((this.whitePawns & dB.getCrudeKingMoves() & Rank.getByIndex(4)) != 0)
					return true;
			}
		}
		else {
			if ((this.blackKing		& dB.getCrudeKingMoves()) != 0)
				return true;
			if ((this.blackKnights 	& dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((this.blackPawns 	& dB.getCrudeWhitePawnCaptures()) != 0)
				return true;
			if (((this.blackQueens | this.blackRooks) 	& dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied)) != 0)
				return true;
			if (((this.blackQueens | this.blackBishops) & dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied)) != 0)
				return true;
			if (this.offsetBoard[sqrInd] == 6 && this.enPassantRights != 8 && sqrInd == 24 + this.enPassantRights) {
				if ((this.blackPawns & dB.getCrudeKingMoves() & Rank.getByIndex(3)) != 0)
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
			if (((this.whiteQueens | this.whiteRooks) 	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied)) != 0)
				return true;
			if (((this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied)) != 0)
				return true;
		}
		else {
			if (((this.blackQueens | this.blackRooks) 	& dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied)) != 0)
				return true;
			if (((this.blackQueens | this.blackBishops) & dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied)) != 0)
				return true;
		}
		return false;
	}
	/**Returns a long representing all the squares on which the pieces are of the color defined by byWhite and in the current position could legally be moved to the supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public long getAttackers(int sqrInd, boolean byWhite) {
		long attackers = 0;
		MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
		if (byWhite) {
			attackers  =  this.whiteKing						& dB.getCrudeKingMoves();
			attackers |=  this.whiteKnights						& dB.getCrudeKnightMoves();
			attackers |=  this.whitePawns 						& dB.getCrudeBlackPawnCaptures();
			attackers |= (this.whiteQueens | this.whiteRooks)	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied);
			attackers |= (this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied);
			if (this.offsetBoard[sqrInd] == 12 && this.enPassantRights != 8 && sqrInd == 32 + this.enPassantRights)
				attackers |=  this.whitePawns & dB.getCrudeKingMoves() & Rank.getByIndex(4);
		}
		else {
			attackers  =  this.blackKing						& dB.getCrudeKingMoves();
			attackers |=  this.blackKnights						& dB.getCrudeKnightMoves();
			attackers |=  this.blackPawns 						& dB.getCrudeWhitePawnCaptures();
			attackers |= (this.blackQueens | this.blackRooks)	& dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied);
			attackers |= (this.blackQueens | this.blackBishops) & dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied);
			if (this.offsetBoard[sqrInd] == 6 && this.enPassantRights != 8 && sqrInd == 24 + this.enPassantRights)
				attackers |=  this.blackPawns & dB.getCrudeKingMoves() & Rank.getByIndex(3);
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
		long sqrBit = Square.getBitmapByIndex(sqrInd);
		long blackPawnAdvance = (sqrBit >>> 8), whitePawnAdvance = (sqrBit << 8);
		if (byWhite) {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			blockerCandidates |=  this.whiteKnights						& dB.getCrudeKnightMoves();
			blockerCandidates |=  this.whitePawns 						& blackPawnAdvance;
			if ((sqrBit & Rank.getByIndex(3)) != 0 && (this.allEmpty & blackPawnAdvance) != 0)
				blockerCandidates |=  this.whitePawns 					& (blackPawnAdvance >>> 8);
			blockerCandidates |= (this.whiteQueens | this.whiteRooks)	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied);
			blockerCandidates |= (this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied);
			if (this.enPassantRights == sqrInd%8 && (sqrBit & Rank.getByIndex(5)) != 0)
				blockerCandidates |=  this.whitePawns & dB.getCrudeBlackPawnCaptures();
		}
		else {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			blockerCandidates |=  this.blackKnights						& dB.getCrudeKnightMoves();
			blockerCandidates |=  this.blackPawns 						& whitePawnAdvance;
			if ((sqrBit & Rank.getByIndex(4)) != 0 && (this.allEmpty & whitePawnAdvance) != 0)
				blockerCandidates |=  this.blackPawns 					& (whitePawnAdvance << 8);
			blockerCandidates |= (this.blackQueens | this.blackRooks)	& dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied);
			blockerCandidates |= (this.blackQueens | this.blackBishops) & dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied);
			if (this.enPassantRights == sqrInd%8 && (sqrBit & Rank.getByIndex(2)) != 0)
				blockerCandidates |=  this.blackPawns & dB.getCrudeWhitePawnCaptures();
		}
		return blockerCandidates;
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
			rankPos 		= attRayMask.rankPos 			& this.allOccupied;
			rankNeg 		= attRayMask.rankNeg 			& this.allOccupied;
			filePos 		= attRayMask.filePos 			& this.allOccupied;
			fileNeg 		= attRayMask.fileNeg 			& this.allOccupied;
			diagonalPos 	= attRayMask.diagonalPos 		& this.allOccupied;
			diagonalNeg 	= attRayMask.diagonalNeg 		& this.allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos 	& this.allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg 	& this.allOccupied;
			straightSliders = this.blackQueens | this.blackRooks;
			diagonalSliders = this.blackQueens | this.blackBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos)			 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos)			 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos)	 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg)		 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg)			 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg)	 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		else {
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(this.blackKing));
			rankPos 		= attRayMask.rankPos 			& this.allOccupied;
			rankNeg 		= attRayMask.rankNeg 			& this.allOccupied;
			filePos 		= attRayMask.filePos 			& this.allOccupied;
			fileNeg 		= attRayMask.fileNeg 			& this.allOccupied;
			diagonalPos 	= attRayMask.diagonalPos 		& this.allOccupied;
			diagonalNeg 	= attRayMask.diagonalNeg 		& this.allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos 	& this.allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg 	& this.allOccupied;
			straightSliders = this.whiteQueens | this.whiteRooks;
			diagonalSliders = this.whiteQueens | this.whiteBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos)			 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos)			 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos)	 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg)		 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg)			 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg)	 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) 	 & this.allBlackPieces) != 0) {
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
	private long addPinnedPieceMoves(LongList moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieceMove, pinnedPieces = 0, promotion = 0, enPassantDestination = 0;
		int pinnedPieceInd, pinnedPiece, to;
		IntStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.whitesTurn) {
			straightSliders = this.blackQueens | this.blackRooks;
			diagonalSliders = this.blackQueens | this.blackBishops;
			attRayMask 		= RayMask.getByIndex(BitOperations.indexOfBit(this.whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & this.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - this.whiteKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & this.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - this.whiteKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 6) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvances(this.allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & this.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - this.whiteKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 6) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						if (this.enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures((this.allBlackPieces | (1L << (enPassantDestination = 40 + this.enPassantRights)) & attRayMask.diagonalPos)))) != 0) {
								if (to == enPassantDestination)
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (12L << Move.CAPTURED_PIECE.shift) | (3L << Move.TYPE.shift));
								else if (to >= 56) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures(this.allBlackPieces & attRayMask.diagonalPos))) != 0) {
								if (to >= 56) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & this.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - this.whiteKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 6) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						if (this.enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures((this.allBlackPieces | (1L << (enPassantDestination = 40 + this.enPassantRights)) & attRayMask.antiDiagonalPos)))) != 0) {
								if (to == enPassantDestination)
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (12L << Move.CAPTURED_PIECE.shift) | (3L << Move.TYPE.shift));
								else if (to >= 56) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnCaptures(this.allBlackPieces & attRayMask.antiDiagonalPos))) != 0) {
								if (to >= 56) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & this.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((this.whiteKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & this.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 3) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((this.whiteKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 6) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvances(this.allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & this.allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((this.whiteKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & this.allOccupied) & this.allWhitePieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & this.allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 2 || pinnedPiece == 4) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((this.whiteKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
		}
		else {
			straightSliders = this.whiteQueens | this.whiteRooks;
			diagonalSliders = this.whiteQueens | this.whiteBishops;
			attRayMask 		= RayMask.getByIndex(BitOperations.indexOfBit(this.blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & this.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - this.blackKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & this.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - this.blackKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 12) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvances(this.allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & this.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - this.blackKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & this.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - this.blackKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & this.allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize((this.blackKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & this.allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 9) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((this.blackKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 12) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvances(this.allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & this.allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((this.blackKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 12) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						if (this.enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures((this.allWhitePieces | (1L << (enPassantDestination = 16 + this.enPassantRights)) & attRayMask.diagonalNeg)))) != 0) {
								if (to == enPassantDestination)
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (6L << Move.CAPTURED_PIECE.shift) | (3L << Move.TYPE.shift));
								else if (to < 8) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures(this.allWhitePieces & attRayMask.diagonalNeg))) != 0) {
								if (to < 8) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & this.allOccupied) & this.allBlackPieces) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & this.allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces	|= pinnedPieceBit;
					pinnedPieceInd	 = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece		 = this.offsetBoard[pinnedPieceInd];
					if (pinnedPiece == 8 || pinnedPiece == 10) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						pinnedPieceMoves = BitOperations.serialize(((this.blackKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
					else if (pinnedPiece == 12) {
						pinnedPieceMove  =  pinnedPieceInd;
						pinnedPieceMove |= (pinnedPiece << Move.MOVED_PIECE.shift);
						if (this.enPassantRights != 8) {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures((this.allWhitePieces | (1L << (enPassantDestination = 16 + this.enPassantRights)) & attRayMask.antiDiagonalNeg)))) != 0) {
								if (to == enPassantDestination)
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (6L << Move.CAPTURED_PIECE.shift) | (3L << Move.TYPE.shift));
								else if (to < 8) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
						else {
							if ((to = BitOperations.indexOfBit(MoveDatabase.getByIndex(pinnedPieceInd).getBlackPawnCaptures(this.allWhitePieces & attRayMask.antiDiagonalNeg))) != 0) {
								if (to < 8) {
									promotion = pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
									moves.add(promotion | (4L << Move.TYPE.shift));
									moves.add(promotion | (5L << Move.TYPE.shift));
									moves.add(promotion | (6L << Move.TYPE.shift));
									moves.add(promotion | (7L << Move.TYPE.shift));
								}
								else
									moves.add(pinnedPieceMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	private LongQueue generateNormalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits, promotion = 0, move = 0;
		int king, piece, to;
		IntStack pieces, moveList;
		LongQueue moves = new LongQueue();
		if (this.whitesTurn) {
			king  = BitOperations.indexOfBit(this.whiteKing);
			move  = king;
			move |= (1L << Move.MOVED_PIECE.shift);
			moveSet	  = MoveDatabase.getByIndex(king).getWhiteKingMoves(this.allNonWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
			}
			if ((this.whiteCastlingRights & 2) != 0) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & this.allOccupied) == 0) {
					if (((moves.getTail() >>> Move.TO.shift) & Move.TO.mask) == 3 && !isAttacked(2, false))
						moves.add(move | (2L << Move.TO.shift) | (2L << Move.TYPE.shift));
				}
			}
			if ((this.whiteCastlingRights & 1) != 0) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & this.allOccupied) == 0) {
					if (!isAttacked(5, false) && !isAttacked(6, false))
						moves.add(move | (6L << Move.TO.shift) | (1L << Move.TYPE.shift));
				}
			}
			movablePieces = ~this.addPinnedPieceMoves(moves);
			pieceSet = this.whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (2L << Move.MOVED_PIECE.shift);
				moveSet = MoveDatabase.getByIndex(piece).getWhiteQueenMoves(this.allNonWhiteOccupied, this.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (3L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (4L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (5L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getWhiteKnightMoves(this.allNonWhiteOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (6L << Move.MOVED_PIECE.shift);
				moveSet = MoveDatabase.getByIndex(piece).getWhitePawnMoves(this.allBlackPieces, this.allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to > 55) {
						promotion = move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
						moves.add(promotion | (4L << Move.TYPE.shift));
						moves.add(promotion | (5L << Move.TYPE.shift));
						moves.add(promotion | (6L << Move.TYPE.shift));
						moves.add(promotion | (7L << Move.TYPE.shift));
					}
					else
						moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			if (this.enPassantRights != 8) {
				if ((pieceSet = MoveDatabase.getByIndex(to = 40 + this.enPassantRights).getBlackPawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					move = (to << Move.TO.shift) | (6L << Move.MOVED_PIECE.shift) | (12L << Move.CAPTURED_PIECE.shift) | (3L << Move.TYPE.shift);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						this.allNonWhiteOccupied ^= enPassAttBits;
						this.allOccupied ^= enPassBits;
						if (!this.isAttackedBySliders(king, false))
							moves.add(move | piece);
						this.allNonWhiteOccupied ^= enPassAttBits;
						this.allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(this.blackKing);
			move  = king;
			move |= (7L << Move.MOVED_PIECE.shift);
			moveSet	= MoveDatabase.getByIndex(king).getBlackKingMoves(this.allNonBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
			}
			if ((this.blackCastlingRights & 1) != 0) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & this.allOccupied) == 0) {
					if (((moves.getHead() >>> Move.TO.shift) & Move.TO.mask) == 61 && !isAttacked(62, true))
						moves.add(move | (62L << Move.TO.shift) | (1L << Move.TYPE.shift));
				}
			}
			if ((this.blackCastlingRights & 2) != 0) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & this.allOccupied) == 0) {
					if (!isAttacked(58, true) && !isAttacked(59, true))
						moves.add(move | (58L << Move.TO.shift) | (2L << Move.TYPE.shift));
				}
			}
			movablePieces = ~this.addPinnedPieceMoves(moves);
			pieceSet = this.blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (8L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getBlackQueenMoves(this.allNonBlackOccupied, this.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (9L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (10L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (11L << Move.MOVED_PIECE.shift);
				moveSet	= MoveDatabase.getByIndex(piece).getBlackKnightMoves(this.allNonBlackOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			pieceSet = this.blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				move  = piece;
				move |= (12L << Move.MOVED_PIECE.shift);
				moveSet = MoveDatabase.getByIndex(piece).getBlackPawnMoves(this.allWhitePieces, this.allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to < 8) {
						promotion = move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift);
						moves.add(promotion | (4L << Move.TYPE.shift));
						moves.add(promotion | (5L << Move.TYPE.shift));
						moves.add(promotion | (6L << Move.TYPE.shift));
						moves.add(promotion | (7L << Move.TYPE.shift));
					}
					else
						moves.add(move | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			if (this.enPassantRights != 8) {
				if ((pieceSet = MoveDatabase.getByIndex(to = 16 + this.enPassantRights).getWhitePawnCaptures(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					move = (to << Move.TO.shift) | (12L << Move.MOVED_PIECE.shift) | (6L << Move.CAPTURED_PIECE.shift) | (3L << Move.TYPE.shift);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						this.allNonBlackOccupied ^= enPassAttBits;
						this.allOccupied ^= enPassBits;
						if (!this.isAttackedBySliders(king, true))
							moves.add(move | piece);
						this.allNonBlackOccupied ^= enPassAttBits;
						this.allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	private LongQueue generateCheckEvasionMoves() {
		long kingMove = 0, move = 0, promotion, kingMoveSet, pinnedPieces, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare, checkerBlockerSquare, king, to, piece;
		IntStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		LongQueue moves = new LongQueue();
		MoveDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.whitesTurn) {
			king 			 = BitOperations.indexOfBit(this.whiteKing);
			kingMove 		 = king | (1L << Move.MOVED_PIECE.shift);
			pinnedPieces 	 = this.getPinnedPieces(true);
			movablePieces	 = ~pinnedPieces;
			kingDb			 = MoveDatabase.getByIndex(king);
			kingMoveSet		 = kingDb.getWhiteKingMoves(this.allNonWhiteOccupied);
			if (BitOperations.resetLSBit(this.checkers) == 0) {
				checker1  		 = BitOperations.indexOfBit(this.checkers);
				checkerPiece1 	 = this.offsetBoard[checker1];
				dB				 = MoveDatabase.getByIndex(checker1);
				if ((this.checkers & Rank.getByIndex(7)) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~this.whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					move = checkerAttackerSquare | ((piece = this.offsetBoard[checkerAttackerSquare]) << Move.MOVED_PIECE.shift) | (checkerPiece1 << Move.CAPTURED_PIECE.shift);
					if (piece == 6) {
						if (promotionOnAttackPossible) {
							promotion = move | (checker1 << Move.TO.shift);
							moves.add(promotion | (4L << Move.TYPE.shift));
							moves.add(promotion | (5L << Move.TYPE.shift));
							moves.add(promotion | (6L << Move.TYPE.shift));
							moves.add(promotion | (7L << Move.TYPE.shift));
						}
						else if (this.enPassantRights != 8 && checker1 == 32 + this.enPassantRights) {
							moves.add(move | ((checker1 + 8) << Move.TO.shift) | (3L << Move.TYPE.shift));
						}
						else
							moves.add(move | (checker1 << Move.TO.shift));
					}
					else
						moves.add(move | (checker1 << Move.TO.shift));
				}
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king) & this.checkers) != 0 || (Rank.getBySquareIndex(king) & this.checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied));
							if (promotionOnAttackPossible && (this.whiteKing & Rank.getByIndex(7)) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								move = checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
								if (promotionOnBlockPossible && (move >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
									moves.add(move | (4L << Move.TYPE.shift));
									moves.add(move | (5L << Move.TYPE.shift));
									moves.add(move | (6L << Move.TYPE.shift));
									moves.add(move | (7L << Move.TYPE.shift));
								}
								else
									moves.add(move);
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(this.allNonWhiteOccupied, (this.allOccupied^this.whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (this.whiteKing & Rank.getByIndex(7)) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								move = checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
								if (promotionOnBlockPossible && (move >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
									moves.add(move | (4L << Move.TYPE.shift));
									moves.add(move | (5L << Move.TYPE.shift));
									moves.add(move | (6L << Move.TYPE.shift));
									moves.add(move | (7L << Move.TYPE.shift));
								}
								else
									moves.add(move);
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								move = checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
								if (promotionOnBlockPossible && (move >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
									moves.add(move | (4L << Move.TYPE.shift));
									moves.add(move | (5L << Move.TYPE.shift));
									moves.add(move | (6L << Move.TYPE.shift));
									moves.add(move | (7L << Move.TYPE.shift));
								}
								else
									moves.add(move);
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			else {
				checker1 		= BitOperations.indexOfLSBit(this.checkers);
				checkerPiece1 	= this.offsetBoard[checker1];
				checker2		= BitOperations.indexOfBit(BitOperations.resetLSBit(this.checkers));
				checkerPiece2 	= this.offsetBoard[checker2];
				dB = MoveDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getWhiteQueenMoves(this.allNonWhiteOccupied, (this.allOccupied^this.whiteKing));
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
						kingMoveSet &= ~dB.getWhiteQueenMoves(this.allNonWhiteOccupied, (this.allOccupied^this.whiteKing));
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
						moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
		}
		else {
			king 	  		= BitOperations.indexOfBit(this.blackKing);
			kingMove	    = king | (7L << Move.MOVED_PIECE.shift);
			pinnedPieces  	= this.getPinnedPieces(false);
			movablePieces 	= ~pinnedPieces;
			kingDb	 		= MoveDatabase.getByIndex(king);
			kingMoveSet		= kingDb.getBlackKingMoves(this.allNonBlackOccupied);
			if (BitOperations.resetLSBit(this.checkers) == 0) {
				checker1  		= BitOperations.indexOfBit(this.checkers);
				checkerPiece1	= this.offsetBoard[checker1];
				dB				= MoveDatabase.getByIndex(checker1);
				if ((this.checkers & Rank.getByIndex(0)) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~this.blackKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					move = checkerAttackerSquare | ((piece = this.offsetBoard[checkerAttackerSquare]) << Move.MOVED_PIECE.shift) | (checkerPiece1 << Move.CAPTURED_PIECE.shift);
					if (piece == 12) {
						if (promotionOnAttackPossible) {
							promotion = move | (checker1 << Move.TO.shift);
							moves.add(promotion | (4L << Move.TYPE.shift));
							moves.add(promotion | (5L << Move.TYPE.shift));
							moves.add(promotion | (6L << Move.TYPE.shift));
							moves.add(promotion | (7L << Move.TYPE.shift));
						}
						else if (this.enPassantRights != 8 && checker1 == 24 + this.enPassantRights)
							moves.add(move | ((checker1 - 8) << Move.TO.shift) | (3L << Move.TYPE.shift));
						else
							moves.add(move | (checker1 << Move.TO.shift));
					}
					else
						moves.add(move | (checker1 << Move.TO.shift));
				}
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king) & this.checkers) != 0 || (Rank.getBySquareIndex(king) & this.checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied));
							if (promotionOnAttackPossible && (this.blackKing & Rank.getByIndex(0)) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								move = checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
								if (promotionOnBlockPossible && (move >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
									moves.add(move | (4L << Move.TYPE.shift));
									moves.add(move | (5L << Move.TYPE.shift));
									moves.add(move | (6L << Move.TYPE.shift));
									moves.add(move | (7L << Move.TYPE.shift));
								}
								else
									moves.add(move);
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(this.allNonBlackOccupied, (this.allOccupied^this.blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (this.blackKing & Rank.getByIndex(0)) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								move = checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
								if (promotionOnBlockPossible && (move >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
									moves.add(move | (4L << Move.TYPE.shift));
									moves.add(move | (5L << Move.TYPE.shift));
									moves.add(move | (6L << Move.TYPE.shift));
									moves.add(move | (7L << Move.TYPE.shift));
								}
								else
									moves.add(move);
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								move = checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
								if (promotionOnBlockPossible && (move >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
									moves.add(move | (4L << Move.TYPE.shift));
									moves.add(move | (5L << Move.TYPE.shift));
									moves.add(move | (6L << Move.TYPE.shift));
									moves.add(move | (7L << Move.TYPE.shift));
								}
								else
									moves.add(move);
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
			else {
				checker1 		= BitOperations.indexOfLSBit(this.checkers);
				checkerPiece1 	= this.offsetBoard[checker1];
				checker2		= BitOperations.indexOfBit(BitOperations.resetLSBit(this.checkers));
				checkerPiece2 	= this.offsetBoard[checker2];
				dB = MoveDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getBlackQueenMoves(this.allNonBlackOccupied, (this.allOccupied^this.blackKing));
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
						kingMoveSet &= ~dB.getBlackQueenMoves(this.allNonBlackOccupied, (this.allOccupied^this.blackKing));
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
						moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
				}
			}
		}
		return moves;
	}
	/**Generates a queue of longs that represent all the legal moves from the current position.
	 * 
	 * @return
	 */
	public LongQueue generateMoves() {
		if (this.check)
			return this.generateCheckEvasionMoves();
		else
			return this.generateNormalMoves();
	}
	/**Makes a single move from a LongQueue generated by generateMoves.
	 * 
	 * @param move
	 */
	public void makeMove(long move) {
		int from 			= (int)((move >>> Move.FROM.shift)		 	  & Move.FROM.mask);
		int to	 			= (int)((move >>> Move.TO.shift) 			  & Move.TO.mask);
		int moved			= (int)((move >>> Move.MOVED_PIECE.shift) 	  & Move.MOVED_PIECE.mask);
		int captured	 	= (int)((move >>> Move.CAPTURED_PIECE.shift)  & Move.CAPTURED_PIECE.mask);
		int type			= (int)((move >>> Move.TYPE.shift)  		  & Move.TYPE.mask);
		long fromBit = Square.getBitmapByIndex(from);
		long toBit	 = Square.getBitmapByIndex(to);
		int enPassantVictimSquare;
		long enPassantVictimSquareBit;
		this.setPositionInfo();
		switch (type) {
			case 0: {
				this.offsetBoard[from]  = 0;
				this.offsetBoard[to]	= moved;
				this.setBitboards(moved, captured, fromBit, toBit);
			}
			break;
			case 1: {
				this.offsetBoard[from]   = 0;
				this.offsetBoard[to]	 = moved;
				this.setBitboards(moved, 0, fromBit, toBit);
				if (this.whitesTurn) {
					this.offsetBoard[7]	 = 0;
					this.offsetBoard[5]	 = 3;
					this.setBitboards(3, 0, Square.getBitmapByIndex(7), Square.getBitmapByIndex(5));
				}
				else {
					this.offsetBoard[63] = 0;
					this.offsetBoard[61] = 9;
					this.setBitboards(9, 0, Square.getBitmapByIndex(63), Square.getBitmapByIndex(61));
				}
			}
			break;
			case 2: {
				this.offsetBoard[from]   = 0;
				this.offsetBoard[to]	 = moved;
				this.setBitboards(moved, 0, fromBit, toBit);
				if (this.whitesTurn) {
					this.offsetBoard[0]	 = 0;
					this.offsetBoard[3]	 = 3;
					this.setBitboards(3, 0, Square.getBitmapByIndex(0), Square.getBitmapByIndex(3));
				}
				else {
					this.offsetBoard[56] = 0;
					this.offsetBoard[59] = 9;
					this.setBitboards(9, 0, Square.getBitmapByIndex(56), Square.getBitmapByIndex(59));
				}
			}
			break;
			case 3: {
				this.offsetBoard[from]   = 0;
				this.offsetBoard[to]	 = moved;
				if (this.whitesTurn)
					enPassantVictimSquare = to - 8;
				else
					enPassantVictimSquare = to + 8;
				this.offsetBoard[enPassantVictimSquare] = 0;
				enPassantVictimSquareBit = Square.getBitmapByIndex(enPassantVictimSquare);
				this.setBitboards(moved, captured, fromBit, enPassantVictimSquareBit);
				this.setBitboards(moved, 0, enPassantVictimSquareBit, toBit);
			}
			break;
			case 4: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 2;
					this.setBitboards(2, captured, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 8;
					this.setBitboards(8, captured, 0, toBit);
				}
			}
			break;
			case 5: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 3;
					this.setBitboards(3, captured, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 9;
					this.setBitboards(9, captured, 0, toBit);
				}
			}
			break;
			case 6: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 4;
					this.setBitboards(4, captured, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 10;
					this.setBitboards(10, captured, 0, toBit);
				}
			}
			break;
			case 7: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 5;
					this.setBitboards(5, captured, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 11;
					this.setBitboards(11, captured, 0, toBit);
				}
			}
		}
		this.moveList.add(move);
		this.setTurn();
		this.setCastlingRights();
		this.setEnPassantRights(from, to, moved);
		this.setCheck();
		this.setMoveIndices(moved, captured);
		this.setKeys();
		this.setRepetitions();
	}
	/**Reverts the state of the instance to that before the last move made in every aspect necessary for the traversal of the game tree. Used within the engine.*/
	public void unMakeMove() {
		long positionInfo = this.positionInfoHistory.pop(), move = this.moveList.pop();
		int from					= (int)((move >>> Move.FROM.shift)		 	  					& Move.FROM.mask);
		int to						= (int)((move >>> Move.TO.shift) 			  					& Move.TO.mask);
		int moved					= (int)((move >>> Move.MOVED_PIECE.shift) 	 					& Move.MOVED_PIECE.mask);
		int captured				= (int)((move >>> Move.CAPTURED_PIECE.shift)  					& Move.CAPTURED_PIECE.mask);
		int type					= (int)((move >>> Move.TYPE.shift)  							& Move.TYPE.mask);
		int enPassantVictimSquare;
		long fromBit = Square.getBitmapByIndex(from);
		long toBit	 = Square.getBitmapByIndex(to);
		this.setTurn();
		switch (type) {
			case 0: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to]	= captured;
				this.setBitboards(moved, captured, fromBit, toBit);
			}
			break;
			case 1: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to]	= 0;
				this.setBitboards(moved, 0, fromBit, toBit);
				if (this.whitesTurn) {
					this.offsetBoard[7]	 = 3;
					this.offsetBoard[5]	 = 0;
					this.setBitboards(3, 0, Square.getBitmapByIndex(5), Square.getBitmapByIndex(7));
				}
				else {
					this.offsetBoard[63] = 9;
					this.offsetBoard[61] = 0;
					this.setBitboards(9, 0, Square.getBitmapByIndex(61), Square.getBitmapByIndex(63));
				}
			}
			break;
			case 2: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to]	= 0;
				this.setBitboards(moved, 0, fromBit, toBit);
				if (this.whitesTurn) {
					this.offsetBoard[0]	 = 3;
					this.offsetBoard[3]	 = 0;
					this.setBitboards(3, 0, Square.getBitmapByIndex(3), Square.getBitmapByIndex(0));
				}
				else {
					this.offsetBoard[56] = 9;
					this.offsetBoard[59] = 0;
					this.setBitboards(9, 0, Square.getBitmapByIndex(59), Square.getBitmapByIndex(56));
				}
			}
			break;
			case 3: {
				this.offsetBoard[from]   = moved;
				this.offsetBoard[to]	 = 0;
				this.setBitboards(moved, 0, fromBit, toBit);
				if (this.whitesTurn)
					enPassantVictimSquare = to - 8;
				else
					enPassantVictimSquare = to + 8;
				this.offsetBoard[enPassantVictimSquare] = captured;
				this.setBitboards(0, captured, 0, Square.getBitmapByIndex(enPassantVictimSquare));
			}
			break;
			case 4: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn)
					this.setBitboards(2, 0, toBit, 0);
				else
					this.setBitboards(8, 0, toBit, 0);
				this.setBitboards(0, captured, 0, toBit);
			}
			break;
			case 5: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn)
					this.setBitboards(3, 0, toBit, 0);
				else
					this.setBitboards(9, 0, toBit, 0);
				this.setBitboards(0, captured, 0, toBit);
			}
			break;
			case 6: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn)
					this.setBitboards(4, 0, toBit, 0);
				else
					this.setBitboards(10, 0, toBit, 0);
				this.setBitboards(0, captured, 0, toBit);
			}
			break;
			case 7: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, fromBit, 0);
				if (this.whitesTurn)
					this.setBitboards(5, 0, toBit, 0);
				else
					this.setBitboards(11, 0, toBit, 0);
				this.setBitboards(0, captured, 0, toBit);
			}
		}
		this.whiteCastlingRights 	= (int)((positionInfo >>> PositionRegister.WHITE_CASTLING_RIGHTS.shift)	& PositionRegister.WHITE_CASTLING_RIGHTS.mask);
		this.blackCastlingRights 	= (int)((positionInfo >>> PositionRegister.BLACK_CASTLING_RIGHTS.shift)	& PositionRegister.BLACK_CASTLING_RIGHTS.mask);
		this.enPassantRights 		= (int)((positionInfo >>> PositionRegister.EN_PASSANT_RIGHTS.shift)		& PositionRegister.EN_PASSANT_RIGHTS.mask);
		this.fiftyMoveRuleClock		= 		(positionInfo >>> PositionRegister.FIFTY_MOVE_RULE_CLOCK.shift)	& PositionRegister.FIFTY_MOVE_RULE_CLOCK.mask;
		this.repetitions			=		(positionInfo >>> PositionRegister.REPETITIONS.shift)			& PositionRegister.REPETITIONS.mask;
		switch ((int)((positionInfo >>> PositionRegister.CHECK.shift) & PositionRegister.CHECK.mask)) {
			case 0: {
				this.check = false;
				this.checkers = 0;
			}
			break;
			case 1: {
				this.check = true;
				this.checkers = (1L << ((positionInfo >>> PositionRegister.CHECKER1.shift) & PositionRegister.CHECKER1.mask));
			}
			break;
			case 2: {
				this.check = true;
				this.checkers = (1L << ((positionInfo >>> PositionRegister.CHECKER1.shift) & PositionRegister.CHECKER1.mask)) |
								(1L << (positionInfo >>> PositionRegister.CHECKER2.shift));
			}
		}
		this.zobristKeyHistory[this.plyIndex] = 0;
		this.zobristKey = this.zobristKeyHistory[--this.plyIndex];
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
		long move;
		LongQueue moves;
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
				moves = this.generateMoves();
				while (moves.hasNext()) {
					move = moves.next();
					if (((move >>> Move.FROM.shift) & Move.FROM.mask) == from && ((move >>> Move.TO.shift) & Move.TO.mask) == to) {
						if (type != 0) {
							if (((move >>> Move.TYPE.shift) & Move.TYPE.mask) == type) {
								this.makeMove(move);
								this.extendKeyHistory();
								return true;
							}
						}
						else {
							this.makeMove(move);
							this.extendKeyHistory();
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
				piece = this.offsetBoard[i*8 + j];
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
		if (this.whitesTurn)
			fen += 'w';
		else
			fen += 'b';
		fen += ' ';
		if (this.whiteCastlingRights != 0) {
			if ((this.whiteCastlingRights & 1) != 0)
				fen += 'K';
			if ((this.whiteCastlingRights & 2) != 0)
				fen += 'Q';
		}
		if (this.blackCastlingRights != 0) {
			if ((this.blackCastlingRights & 1) != 0)
				fen += 'k';
			if ((this.blackCastlingRights & 2) != 0)
				fen += 'q';
		}
		if (this.whiteCastlingRights == 0 && this.blackCastlingRights == 0)
			fen += '-';
		fen += ' ';
		if (this.enPassantRights == 8)
			fen += '-';
		else {
			fen += (char)(this.enPassantRights + 'a');
			if (this.whitesTurn)
				fen += 6;
			else
				fen += 3;
		}
		fen += ' ';
		fen += this.fiftyMoveRuleClock;
		fen += ' ';
		fen += 1 + this.plyIndex/2;
		return fen;
	}
	/**Prints the object's move history to the console in chronological order in pseudo-algebraic chess notation.*/
	public void printMoveHistoryToConsole() {
		LongList chronMoves = new LongStack();
		int i = 0;
		while (this.moveList.hasNext())
			chronMoves.add(this.moveList.next());
		while (chronMoves.hasNext()) {
			i++;
			System.out.printf(i + ". %-8s ", Move.pseudoAlgebraicNotation(chronMoves.next()));
		}
		System.out.println();
	}
	/**Prints a bitboard representing all the occupied squares of the Position object's board position to the console in a human-readable form,
	 * aligned like a chess board.*/
	public void printBitboardToConsole() {
		Board.printBitboardToConsole(this.allOccupied);
	}
	/**Prints the array representing the Position object's board position to the console in a human-readable form, aligned like a chess board with 
	 * integers denoting the pieces. 0 means an empty square, 1 is the white king, 2 is the white queen, ..., 7 is the black king, etc.*/
	public void printOffsetBoardToConsole() {
		Board.printOffsetBoardToConsole(this.offsetBoard);
	}
	/**Prints the chess board to the console. Pieces are represented according to the FEN notation.*/
	public void printFancyBoardToConsole() {
		Board.printFancyBoardToConsole(this.offsetBoard);
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
		if ((this.whiteCastlingRights & 1) != 0)
			System.out.print("K");
		if ((this.whiteCastlingRights & 2) != 0)
			System.out.print("Q");
		if ((this.blackCastlingRights & 1) != 0)
			System.out.print("k");
		if ((this.blackCastlingRights & 2) != 0)
			System.out.print("q");
		if (this.whiteCastlingRights == 0 && this.blackCastlingRights == 0)
			System.out.print("-");
		System.out.println();
		System.out.printf("%-23s ", "En passant rights:");
		if (this.enPassantRights == 8)
			System.out.println("-");
		else
			System.out.println((char)('a' + this.enPassantRights));
		System.out.printf("%-23s " + this.plyIndex + "\n", "Half-move index:");
		System.out.printf("%-23s " + this.fiftyMoveRuleClock + "\n", "Fifty-move rule clock:");
		System.out.printf("%-23s " + Long.toHexString(this.zobristKey) + "\n", "Hash key:");
		System.out.printf("%-23s ", "Move history:");
		this.printMoveHistoryToConsole();
		System.out.println();
	}
	/**Runs a perft test to the given depth and returns the number of leaf nodes the traversed game tree had. It is used mainly for move generation and move
	 * making speed benchmarking; and bug detection by comparing the returned values to validated results.
	 * 
	 * @param depth
	 * @return
	 */
	public long perft(int depth) {
		LongQueue moves;
		long move, leafNodes = 0;
		if (depth == 0)
			return 1;
		moves = this.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			this.makeMove(move);
			leafNodes += this.perft(depth - 1);
			this.unMakeMove();
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
		LongQueue moves;
		long move, leafNodes = 0;
		moves = this.generateMoves();
		if (depth == 1)
			return moves.length();
		while (moves.hasNext()) {
			move = moves.next();
			this.makeMove(move);
			leafNodes += this.quickPerft(depth - 1);
			this.unMakeMove();
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
		LongList moves;
		long move, type, leafNodes = 0;
		if (depth == 0) {
			switch (moveType) {
				case 1: {
					if (((this.moveList.getHead() >>> Move.TYPE.shift) & Move.TYPE.mask) == 0) {
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
					if ((type = (this.moveList.getHead() >>> Move.TYPE.shift) & Move.TYPE.mask) == 1 || type == 2) {
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
					if (((this.moveList.getHead() >>> Move.TYPE.shift) & Move.TYPE.mask) == 3) {
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
					if (((this.moveList.getHead() >>> Move.TYPE.shift) & Move.TYPE.mask) > 3) {
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
		moves = this.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			this.makeMove(move);
			leafNodes += this.detailedPerft(depth - 1, moveType, consoleOutputType);
			this.unMakeMove();
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
		long move, nodes, total;
		LongQueue moves;
		LongStack chronoHistory;
		boolean found = true;
		while (depth > 0 && found) {
			depth--;
			this.printStateToConsole();
			total = 0;
			found = false;
			i = 0;
			chronoHistory = new LongStack();
			while (this.moveList.hasNext())
				chronoHistory.add(this.moveList.next());
			moves = this.generateMoves();
			while (moves.hasNext()) {
				while (chronoHistory.hasNext()) {
					move = chronoHistory.next();
					System.out.printf("%3d. %-8s ", moveIndices.next(), Move.pseudoAlgebraicNotation(move));
				}
				moveIndices.reset();
				i++;
				move = moves.next();
				this.makeMove(move);
				if (depth > 0)
					nodes = this.quickPerft(depth);
				else
					nodes = 1;
				System.out.printf("%3d. %-8s nodes: %d\n", i, Move.pseudoAlgebraicNotation(move), nodes);
				total += nodes;
				this.unMakeMove();
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
						this.makeMove(move);
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
		long move, total = 0;
		LongQueue moves = this.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			System.out.printf("%-10s ", Move.pseudoAlgebraicNotation(move) + ":");
			this.makeMove(move);
			System.out.println(total += this.quickPerft(depth - 1));
			this.unMakeMove();
		}
		System.out.println("Moves: " + moves.length());
		System.out.println("Total nodes: " + total);
	}
}
