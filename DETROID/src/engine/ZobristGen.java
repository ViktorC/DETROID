package engine;

import java.util.Random;

/**A class whose object encodes the most important pieces of information stored in a Board class into a long by XOR-operations.
 * Two Board objects with identical states will always have the same Zobrist keys within one runtime and two Board objects with
 * different values for the concerned instance fields will almost always have different Zobrist keys.
 * The relevant fields are:
 * 		1. color to move
 * 		2. board position
 * 		3. white's castling rights
 * 		4. black's castling rights
 * 		5. en passant rights
 * 
 * The class creates its own random values it then uses for XOR-ing on compile thus Board objects with identical states are 
 * likely to have different keys for each runtime.
 * 
 * @author Viktor
 *
 */
public class ZobristGen {
	
	private static long turn;
	private static long[][] board = new long[13][64];
	private static long[] whiteCastlingRights = new long[4];
	private static long[] blackCastlingRights = new long[4];
	private static long[] enPassantRights = new long[9];
	
	static {
		pseudorandNumGen();
	}
	/**Generates the 'random' values for the instance fields. For the board, there is a value for any piece on any square.*/
	private static void pseudorandNumGen() {
		Random random = new Random();
		turn = random.nextLong();
		for (int i = 0; i < 64; i++)
			board[0][i] = 0;
		for (int i = 1; i < 13; i++) {
			for (int j = 0; j < 64; j++)
				board[i][j] = random.nextLong();
		}
		for (int i = 0; i < 4; i++)
			whiteCastlingRights[i] = random.nextLong();
		for (int i = 0; i < 4; i++)
			blackCastlingRights[i] = random.nextLong();
		for (int i = 0; i < 8; i++)
			enPassantRights[i] = random.nextLong();
	}
	/**Encodes a Board objects and returns the 64-bit key.
	 * 
	 * @param board
	 * @return
	 */
	public long hash(Position p) {
		int[] board64 = p.getOffsetBoard();
		long key = 0;
		if (!p.whitesTurn)
			key ^= turn;
		for (int i = 0; i < 64; i++) {
			key ^= board[board64[i]][i];
		}
		key ^= whiteCastlingRights[p.whiteCastlingRights];
		key ^= blackCastlingRights[p.blackCastlingRights];
		key ^= enPassantRights[p.enPassantRights];
		return key;
	}
	/**Modifies a Board object's Zobrist key by XOR-ing it with the Zobrist object's respective instance fields for the fields of the Board
	 * object changed by the last move made.
	 * 
	 * @param p
	 * @return
	 */
	public long updateKey(Position p) {
		long key	 	 			= p.zobristKey;
		Move move	 	 			= p.getLastMove();
		UnmakeRegister unmakeReg	= p.getUnmakeRegister();
		switch (move.type) {
			case 0: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				key ^= board[move.movedPiece][move.to];
			}
			break;
			case 1: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				key ^= board[move.movedPiece][move.to];
				if (move.movedPiece == 1) {
					key ^= board[3][7];
					key ^= board[3][5];
				}
				else {
					key ^= board[9][63];
					key ^= board[9][61];
				}
			}
			break;
			case 2: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				key ^= board[move.movedPiece][move.to];
				if (move.movedPiece == 1) {
					key ^= board[3][0];
					key ^= board[3][3];
				}
				else {
					key ^= board[9][56];
					key ^= board[9][59];
				}
			}
			break;
			case 3: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.movedPiece][move.to];
				if (move.movedPiece == 6)
					key ^= board[12][move.to - 8];
				else
					key ^= board[6][move.to + 8];
			}
			break;
			case 4: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				if (move.movedPiece == 6)
					key ^= board[2][move.to];
				else
					key ^= board[8][move.to];
			}
			break;
			case 5: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				if (move.movedPiece == 6)
					key ^= board[3][move.to];
				else
					key ^= board[9][move.to];
			}
			break;
			case 6: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				if (move.movedPiece == 6)
					key ^= board[4][move.to];
				else
					key ^= board[10][move.to];
			}
			break;
			case 7: {
				key ^= board[move.movedPiece][move.from];
				key ^= board[move.capturedPiece][move.to];
				if (move.movedPiece == 6)
					key ^= board[5][move.to];
				else
					key ^= board[11][move.to];
			}
		}
		key ^= turn;
		key ^= whiteCastlingRights[unmakeReg.whiteCastlingRights];
		key ^= blackCastlingRights[unmakeReg.blackCastlingRights];
		key ^= enPassantRights[unmakeReg.enPassantRights];
		key ^= whiteCastlingRights[p.whiteCastlingRights];
		key ^= blackCastlingRights[p.blackCastlingRights];
		key ^= enPassantRights[p.enPassantRights];
		return key;
	}
}
