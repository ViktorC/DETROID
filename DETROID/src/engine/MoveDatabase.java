package engine;

import engine.board.Square;

public enum MoveDatabase {
	
	A1, B1, C1, D1, E1, F1, G1, H1,
	A2, B2, C2, D2, E2, F2, G2, H2,
	A3, B3, C3, D3, E3, F3, G3, H3,
	A4, B4, C4, D4, E4, F4, G4, H4,
	A5, B5, C5, D5, E5, F5, G5, H5,
	A6, B6, C6, D6, E6, F6, G6, H6,
	A7, B7, C7, D7, E7, F7, G7, H7,
	A8, B8, C8, D8, E8, F8, G8, H8;
	
	final byte sqrInd;
	
	SliderOccupancyMask occupancy;
	Magics magics;
	
	private final long[] rook;
	private final long[] bishop;
	private final long king;
	private final long knight;
	private final long pawnWhiteAdvance;
	private final long pawnWhiteCapture;
	private final long pawnBlackAdvance;
	private final long pawnBlackCapture;
	
	private MoveDatabase() {
		this.sqrInd = (byte)this.ordinal();
		Square sqr = Square.getByIndex(this.sqrInd);
		this.magics = Magics.getByIndex(this.sqrInd);
		this.occupancy = SliderOccupancyMask.getByIndex(this.sqrInd);
		long[] rookOccVar 	= SliderOccupancyVariationGenerator.generateRookOccupancyVariations(this.sqrInd);
		long[] bishopOccVar = SliderOccupancyVariationGenerator.generateBishopOccupancyVariations(this.sqrInd);
		int rookNumOfVar 	= rookOccVar.length;
		int bishopNumOfVar 	= bishopOccVar.length;
		this.rook 	= new long[rookNumOfVar];
		this.bishop = new long[bishopNumOfVar];
		long[] rookAttVar 	= SliderAttackSetCalculator.computeRookAttackSetVariations(sqr, rookOccVar);
		long[] bishopAttVar = SliderAttackSetCalculator.computeBishopAttackSetVariations(sqr, bishopOccVar);
		int index;
		for (int i = 0; i < rookNumOfVar; i++) {
			index = (int)((rookOccVar[i]*this.magics.rookMagicNumber) >>> this.magics.rookShift);
			this.rook[index] = rookAttVar[i];
		}
		for (int i = 0; i < bishopNumOfVar; i++) {
			index = (int)((bishopOccVar[i]*this.magics.bishopMagicNumber) >>> this.magics.bishopShift);
			this.bishop[index] = bishopAttVar[i];
		}
		this.king 				= MoveMaskGenerator.generateKingsMoveMask(sqr);
		this.knight 			= MoveMaskGenerator.generateKnightMasks(sqr);
		this.pawnWhiteAdvance 	= MoveMaskGenerator.generateWhitePawnsAdvanceMasks(sqr);
		this.pawnWhiteCapture 	= MoveMaskGenerator.generateWhitePawnsCaptureMasks(sqr);
		this.pawnBlackAdvance 	= MoveMaskGenerator.generateBlackPawnsAdvanceMasks(sqr);
		this.pawnBlackCapture 	= MoveMaskGenerator.generateBlackPawnsCaptureMasks(sqr);
	}
	public long getCrudeKingMoves() {
		return this.king;
	}
	public long getCrudeKnightMoves() {
		return this.knight;
	}
	public long getCrudeWhitePawnCaptures() {
		return this.pawnWhiteCapture;
	}
	public long getCrudeBlackPawnCaptures() {
		return this.pawnBlackCapture;
	}
	public long getCrudeRookMoves() {
		return this.rook[0];
	}
	public long getCrudeBishopMoves() {
		return this.bishop[0];
	}
	public long getCrudeQueenMoves() {
		return this.rook[0] | this.bishop[0];
	}
	public long getWhiteKingMoves(long allNonWhiteOccupied) {
		return this.king & allNonWhiteOccupied;
	}
	public long getBlackKingMoves(long allNonBlackOccupied) {
		return this.king & allNonBlackOccupied;
	}
	public long getWhiteKnightMoves(long allNonWhiteOccupied) {
		return this.knight & allNonWhiteOccupied;
	}
	public long getBlackKnightMoves(long allNonBlackOccupied) {
		return this.knight & allNonBlackOccupied;
	}
	public long getWhitePawnCaptures(long allBlackPieces) {
		return this.pawnWhiteCapture & allBlackPieces;
	}
	public long getBlackPawnCaptures(long allWhitePieces) {
		return this.pawnBlackCapture & allWhitePieces;
	}
	public long getWhitePawnAdvances(long allEmpty) {
		if (this.sqrInd > 15)
			return this.pawnWhiteAdvance & allEmpty;
		long adv = this.pawnWhiteAdvance & allEmpty;
		return adv | ((adv << 8) & allEmpty);
	}
	public long getBlackPawnAdvances(long allEmpty) {
		if (this.sqrInd < 48)
			return this.pawnBlackAdvance & allEmpty;
		long adv = this.pawnBlackAdvance & allEmpty;
		return adv | ((adv >>> 8) & allEmpty);
	}
	public long getWhitePawnMoves(long allBlackPieces, long allEmpty) {
		return this.getWhitePawnAdvances(allEmpty) | this.getWhitePawnCaptures(allBlackPieces);
	}
	public long getBlackPawnMoves(long allWhitePieces, long allEmpty) {
		return this.getBlackPawnAdvances(allEmpty) | this.getBlackPawnCaptures(allWhitePieces);
	}
	public long getWhiteRookMoves(long allNonWhiteOccupied, long allOccupied) {
		return this.rook[(int)(((this.occupancy.rookOccupancyMask & allOccupied)*magics.rookMagicNumber) >>> magics.rookShift)] & allNonWhiteOccupied;
	}
	public long getBlackRookMoves(long allNonBlackOccupied, long allOccupied) {
		return this.rook[(int)(((this.occupancy.rookOccupancyMask & allOccupied)*magics.rookMagicNumber) >>> magics.rookShift)] & allNonBlackOccupied;
	}
	public long getWhiteBishopMoves(long allNonWhiteOccupied, long allOccupied) {
		return this.bishop[(int)(((this.occupancy.bishopOccupancyMask & allOccupied)*magics.bishopMagicNumber) >>> magics.bishopShift)] & allNonWhiteOccupied;
	}
	public long getBlackBishopMoves(long allNonBlackOccupied, long allOccupied) {
		return this.bishop[(int)(((this.occupancy.bishopOccupancyMask & allOccupied)*magics.bishopMagicNumber) >>> magics.bishopShift)] & allNonBlackOccupied;
	}
	public long getWhiteQueenMoves(long allNonWhiteOccupied, long allOccupied) {
		return (this.rook[(int)(((this.occupancy.rookOccupancyMask & allOccupied)*magics.rookMagicNumber) >>> magics.rookShift)] |
			    this.bishop[(int)(((this.occupancy.bishopOccupancyMask & allOccupied)*magics.bishopMagicNumber) >>> magics.bishopShift)]) & allNonWhiteOccupied;
	}
	public long getBlackQueenMoves(long allNonBlackOccupied, long allOccupied) {
		return (this.rook[(int)(((this.occupancy.rookOccupancyMask & allOccupied)*magics.rookMagicNumber) >>> magics.rookShift)] |
			    this.bishop[(int)(((this.occupancy.bishopOccupancyMask & allOccupied)*magics.bishopMagicNumber) >>> magics.bishopShift)]) & allNonBlackOccupied;
	}
	public static MoveDatabase getByIndex(int sqrInd) {
		switch(sqrInd) {
			case 0:  return A1; case 1:  return B1; case 2:  return C1; case 3:  return D1; case 4:  return E1; case 5:  return F1; case 6:  return G1; case 7:  return H1;
			case 8:  return A2; case 9:  return B2; case 10: return C2; case 11: return D2; case 12: return E2; case 13: return F2; case 14: return G2; case 15: return H2;
			case 16: return A3; case 17: return B3; case 18: return C3; case 19: return D3; case 20: return E3; case 21: return F3; case 22: return G3; case 23: return H3;
			case 24: return A4; case 25: return B4; case 26: return C4; case 27: return D4; case 28: return E4; case 29: return F4; case 30: return G4; case 31: return H4;
			case 32: return A5; case 33: return B5; case 34: return C5; case 35: return D5; case 36: return E5; case 37: return F5; case 38: return G5; case 39: return H5;
			case 40: return A6; case 41: return B6; case 42: return C6; case 43: return D6; case 44: return E6; case 45: return F6; case 46: return G6; case 47: return H6;
			case 48: return A7; case 49: return B7; case 50: return C7; case 51: return D7; case 52: return E7; case 53: return F7; case 54: return G7; case 55: return H7;
			case 56: return A8; case 57: return B8; case 58: return C8; case 59: return D8; case 60: return E8; case 61: return F8; case 62: return G8; case 63: return H8;
			default: throw new IllegalArgumentException("Invalid square index.");
		}
	}
}