package engine;

import java.util.Random;

/**A class whose object encodes the most important pieces of information stored in a Board class into a long by XOR-operations.
 * Two Board objects with identical states will always have the same Zobrist keys within one runtime and two Board objects with
 * different values for the concerned instance fields will almost always have different Zobrist keys. The concerned fields are:
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
public class ZobristKeyGenerator {
	
	private long turn;
	private long[][] board = new long[13][64];
	private long[] whiteCastlingRights = new long[4];
	private long[] blackCastlingRights = new long[4];
	private long[] enPassantRights = new long[9];
	
	public ZobristKeyGenerator() {
		this.pseudorandNumGen();
	}
	/**Generates the 'random' values for the instance fields. For the board, there is a value for any piece on any square.*/
	private void pseudorandNumGen() {
		Random random = new Random();
		this.turn = random.nextLong();
		for (int i = 0; i < 64; i++)
			this.board[0][i] = 0;
		for (int i = 1; i < 13; i++) {
			for (int j = 0; j < 64; j++)
				this.board[i][j] = random.nextLong();
		}
		for (int i = 0; i < 4; i++)
			this.whiteCastlingRights[i] = random.nextLong();
		for (int i = 0; i < 4; i++)
			this.blackCastlingRights[i] = random.nextLong();
		for (int i = 0; i < 8; i++)
			this.enPassantRights[i] = random.nextLong();
	}
	/**Encodes a Board objects and returns the 64-bit key.
	 * 
	 * @param board
	 * @return
	 */
	public long hash(Position board) {
		int[] board64 = board.getOffsetBoard();
		long key = 0;
		if (!board.getTurn())
			key ^= this.turn;
		for (int i = 0; i < 64; i++) {
			key ^= this.board[board64[i]][i];
		}
		key ^= this.whiteCastlingRights[board.getWhiteCastlingRights()];
		key ^= this.blackCastlingRights[board.getBlackCastlingRights()];
		key ^= this.enPassantRights[board.getEnPassantRights()];
		return key;
	}
	/**Modifies a Board object's Zobrist key by XOR-ing it with the Zobrist object's respective instance fields for the fields of the Board
	 * object changed by the last move made.
	 * 
	 * @param board
	 * @return
	 */
	public long updateKey(Position board) {
		long key	 	 = board.getZobristKey();
		long move	 	 = board.getLastMove();
		long unmakeReg 	 = board.getUnmakeRegister();
		int from		 = (int)((move 		>>> Move.FROM.shift)  						& Move.FROM.mask);
		int to			 = (int)((move 		>>> Move.TO.shift) 							& Move.TO.mask);
		int moved 	 	 = (int) (unmakeReg 											& UnmakeRegister.MOVED_PIECE.mask);
		int captured 	 = (int)((unmakeReg >>> UnmakeRegister.CAPTURED_PIECE.shift) 	& UnmakeRegister.CAPTURED_PIECE.mask);
		int type		 = (int)((move 		>>> Move.TYPE.shift) 						& Move.TYPE.mask);
		switch (type) {
			case 0: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				key ^= this.board[moved][to];
			}
			break;
			case 1: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				key ^= this.board[moved][to];
				if (moved == 1) {
					key ^= this.board[3][7];
					key ^= this.board[3][5];
				}
				else {
					key ^= this.board[9][63];
					key ^= this.board[9][61];
				}
			}
			break;
			case 2: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				key ^= this.board[moved][to];
				if (moved == 1) {
					key ^= this.board[3][0];
					key ^= this.board[3][3];
				}
				else {
					key ^= this.board[9][56];
					key ^= this.board[9][59];
				}
			}
			break;
			case 3: {
				key ^= this.board[moved][from];
				key ^= this.board[moved][to];
				if (moved == 6) {
					key ^= this.board[12][to - 8];
				}
				else {
					key ^= this.board[6][to + 8];
				}
			}
			break;
			case 4: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				if (moved == 6) {
					key ^= this.board[2][to];
				}
				else {
					key ^= this.board[8][to];
				}
			}
			break;
			case 5: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				if (moved == 6) {
					key ^= this.board[3][to];
				}
				else {
					key ^= this.board[9][to];
				}
			}
			break;
			case 6: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				if (moved == 6) {
					key ^= this.board[4][to];
				}
				else {
					key ^= this.board[10][to];
				}
			}
			break;
			case 7: {
				key ^= this.board[moved][from];
				key ^= this.board[captured][to];
				if (moved == 6) {
					key ^= this.board[5][to];
				}
				else {
					key ^= this.board[11][to];
				}
			}
		}
		key ^= this.turn;
		key ^= this.whiteCastlingRights[(int)((unmakeReg >>> UnmakeRegister.WHITE_CASTLING_RIGHTS.shift) & UnmakeRegister.WHITE_CASTLING_RIGHTS.mask)];
		key ^= this.blackCastlingRights[(int)((unmakeReg >>> UnmakeRegister.BLACK_CASTLING_RIGHTS.shift) & UnmakeRegister.BLACK_CASTLING_RIGHTS.mask)];
		key ^= this.enPassantRights[(int)((unmakeReg >>> UnmakeRegister.EN_PASSANT_RIGHTS.shift) & UnmakeRegister.EN_PASSANT_RIGHTS.mask)];
		key ^= this.whiteCastlingRights[board.getWhiteCastlingRights()];
		key ^= this.blackCastlingRights[board.getBlackCastlingRights()];
		key ^= this.enPassantRights[board.getEnPassantRights()];
		return key;
	}
}
