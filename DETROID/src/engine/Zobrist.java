package engine;

import java.util.Random;

public class Zobrist {
	
	private long turn;
	private long[][] board = new long[13][64];
	private long[] whiteCastlingRights = new long[4];
	private long[] blackCastlingRights = new long[4];
	private long[] enPassantRights = new long[9];
	
	public Zobrist() {
		this.pseudorandNumGen();
	}
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
	public long hash(Board b) {
		int[] board64 = b.getOffsetBoard();
		long key = 0;
		if (!b.getTurn())
			key ^= turn;
		for (int i = 0; i < 64; i++) {
			key ^= board[board64[i]][i];
		}
		key ^= this.whiteCastlingRights[b.getWhiteCastlingRights()];
		key ^= this.blackCastlingRights[b.getBlackCastlingRights()];
		key ^= this.enPassantRights[b.getEnPassantRights()];
		return key;
	}
	public long updateKey(Board b) {
		long key	 	= b.getZobristKey();
		long move	 	= b.getLastMove();
		int from		= (int)(move >>> Move.FROM.shift  			& Move.FROM.mask);
		int to			= (int)(move >>> Move.TO.shift 				& Move.TO.mask);
		int moved		= (int)(move >>> Move.MOVED_PIECE.shift 	& Move.MOVED_PIECE.mask);
		int captured	= (int)(move >>> Move.CAPTURED_PIECE.shift 	& Move.CAPTURED_PIECE.mask);
		int type		= (int)(move >>> Move.TYPE.shift 			& Move.TYPE.mask);
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
					key ^= this.board[captured][to - 8];
				}
				else {
					key ^= this.board[captured][to + 8];
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
		key ^= this.whiteCastlingRights[(int)(move >>> Move.PREVIOUS_WHITE_CASTLING_RIGHTS.shift & Move.PREVIOUS_WHITE_CASTLING_RIGHTS.mask)];
		key ^= this.blackCastlingRights[(int)(move >>> Move.PREVIOUS_BLACK_CASTLING_RIGHTS.shift & Move.PREVIOUS_BLACK_CASTLING_RIGHTS.mask)];
		key ^= this.enPassantRights[(int)(move >>> Move.PREVIOUS_ENPASSANT_RIGHTS.shift & Move.PREVIOUS_ENPASSANT_RIGHTS.mask)];
		key ^= this.whiteCastlingRights[b.getWhiteCastlingRights()];
		key ^= this.blackCastlingRights[b.getBlackCastlingRights()];
		key ^= this.enPassantRights[b.getEnPassantRights()];
		return key;
	}
}
