package engine;

import engine.Board.*;

/**A preinitialized move database that saves the time costs of calculating move sets on the fly at the cost of about 850KBytes. It contains a so called
 * fancy magic move tablebase for sliding piece attack set derivation with all the necessary masks and magic numbers included so the desired move sets
 * can be retrieved by simply invoking one of this database's functions and feeding it an occupancy bitmap or two. Beside the sliding pieces' attack sets,
 * all the other pieces masks have been preinitialized too, for the sake of simplicity.
 * 
 * @author Viktor
 *
 */
public enum MoveDatabase {
	
	A1 (0b1001000010000000000000000001001000100000010000000000000010000001L, 0b0000000000000010000100000000000100001000000000001000000010000000L, 52, 58),
	B1 (0b0000000011000000000100000000001001100000000000000100000000000000L, 0b1010000000010001000100000000000110000010100100001000000001000100L, 53, 59),
	C1 (0b0000000010000000000010010010000000000000100000000101000000000100L, 0b0100000000000100000010000000000010000001000000010000000000010000L, 53, 59),
	D1 (0b0000000010000000000100000000000001000100100000000000100000000000L, 0b0000100000100100010000010000000000100000000100110000000001000011L, 53, 59),
	E1 (0b0000000010000000000000100000010000000000000010000000000010000000L, 0b0000000000000100000001010010000000000001001001001000000010000000L, 53, 59),
	F1 (0b0010000010000000000001000000000000100001000001100000000010000000L, 0b0000000000000010000000101000001000100000001000000100000000000000L, 53, 59),
	G1 (0b0110010000000000000011000000000010000001000000100011000000001000L, 0b1001000000000100000000010000010000000110101000000010000000000000L, 53, 59),
	H1 (0b0000001010000000000000100100010000100101000000000000000010000000L, 0b0000100000000001000001001000001000000000101000000000010001000000L, 52, 58),
	A2 (0b0000000001010000100000000000000010000000001000000100000000010100L, 0b1000011010000000000010000101100100001000000010000000000011001000L, 53, 59),
	B2 (0b1000000000010000010000000000000000100000000100000000000001000100L, 0b0000000000000010001000000000000100000010001000001000001100000000L, 54, 59),
	C2 (0b0000000000000110000000000100000001010110000000001000000001100000L, 0b0000000010000001100111100010001000000100000010010000000110000000L, 54, 59),
	D2 (0b0001100000000001000000000011000000000000001000110000001000001000L, 0b0110100000001101100001000100000001000000100010000000000001000000L, 54, 59),
	E2 (0b0000000000000000100000000000100000000011100000000000010000000000L, 0b0001001000100001000001000000010000100000110000100000000000000000L, 54, 59),
	F2 (0b0011000000000010000000000000100000000100000100000000101000000000L, 0b0000000000001010000000010011000000001000001000000000000000001000L, 54, 59),
	G2 (0b0001000100100001000000000000000100000000000001100000000010000100L, 0b0000010100100000001000100000100100000001001000000001001000100000L, 54, 59),
	H2 (0b0001000101000010000000000000010010000000010011000000001100010010L, 0b0010000000010000100000100000000100100000100010000000010000000000L, 53, 59),
	A3 (0b0101000100000011000000010000000000100010100000000000000001000000L, 0b0000000010000100000100000000100000010000000001000000100000000000L, 53, 59),
	B3 (0b0000000100110000000001000100000000000000010010001010000000000000L, 0b0100000010100110000010000010000001010100000000110000001000000000L, 54, 59),
	C3 (0b0000000001001010100000001000000001010000000000000010000000000001L, 0b0010000001001000000110100000010000001000000000001010000010011000L, 54, 57),
	D3 (0b0100000100000011100000011000000000001000000000000011000000000100L, 0b1000000000010100000000100000100000000100100100011001000000000000L, 54, 57),
	E3 (0b0000000100001110000000100000000000001000101000000001000000000100L, 0b0000001000001001000000010100100000100000000010000000010101000100L, 54, 57),
	F3 (0b0010010000100001000000010000000000000100000000001000100001000010L, 0b1011000000000010000000000000110000100000100101000010000000000000L, 54, 57),
	G3 (0b0000000010000000000001000000000000011000000100000000000110001010L, 0b0000000000000001010000000000001100011000000000100001000000000000L, 54, 59),
	H3 (0b0000100100000101000000100000000000010001000000001000000001010100L, 0b0001100001000001010000000100000100000001010010000000010000000000L, 53, 59),
	A4 (0b0000000001000000100000000010000010000000000000000100000000000000L, 0b1000010000010000010000000100000001000100000001000000010000000101L, 53, 59),
	B4 (0b1000000001000010100000010000000100000000001000000100000000001001L, 0b0011100000000100000011000001001000100000101010000000010010000000L, 54, 59),
	C4 (0b0000010000000100010000000011000100000000001000000000000100000001L, 0b0000000000010000111010000000100000100100000100000100010000000000L, 54, 57),
	D4 (0b0000000000000100011000010000000100000000000100000000000000001000L, 0b0100100000001100000000010000000010000000001000000000100010000000L, 54, 55),
	E4 (0b0001000001000000000110000000000010000000000001000000000010000000L, 0b0000001000001101000000000100000000101100000001000100000000000000L, 54, 55),
	F4 (0b1001000000001010000000000000101000000000010010001001000000000100L, 0b0100011010001000000010100000010000000000110000000100001000000010L, 54, 57),
	G4 (0b0000000000010000000000110000010000000000010100100100100000010000L, 0b0100000000001000000000100000000000000100100100100000000100000000L, 54, 59),
	H4 (0b1100010110000001100000000000110010000000000010000100100100000000L, 0b0001000000000000110000010000000000101000100001000001000100010100L, 53, 59),
	A5 (0b0000010000000000100000000100000000001100100000000000100000100000L, 0b0100000000010001001000000010000000100000000110000000100001001000L, 53, 59),
	B5 (0b0010000000010000000000000010000000010100110000000000000001000110L, 0b0000000000000000100100000001000000000000000101000000010000110000L, 54, 59),
	C5 (0b0000000000001100100000100001000000000000100000000010000000000000L, 0b0100000000000010000000100000101000000001000000010000100000000000L, 54, 57),
	D5 (0b0000000000001010000000000100000000010010000000000010000000101000L, 0b0000000100000001001000000010000000100000000010000000000010000000L, 54, 55),
	E5 (0b0000010000001000000000011000000000001000100000000000010000000000L, 0b0100000000010000000000100000000010000000000000000101000000000101L, 54, 55),
	F5 (0b1000000000001000000010100000000010000000100000000000010000000000L, 0b0000000000100010000000001000001000000100000000010000000001000000L, 54, 57),
	G5 (0b0000010100100001110000010000100000110100000000000001000000100010L, 0b0000000000001000000000001010010000001110010100010000000100000000L, 54, 59),
	H5 (0b0000000001001010011000000100010000001010000000000000001110100001L, 0b0000000000000100000000001000000000111000000001100000000100000000L, 53, 59),
	A6 (0b1000001010000000000000000100000100100000000001000100000000000011L, 0b0000010000000100000001000000010001000000000000000000010100000000L, 53, 59),
	B6 (0b0000000000110000000000000100110110100000000000010100000000000000L, 0b0000000000000010000000001001000000000100000001000000100010000000L, 54, 59),
	C6 (0b0000000000010000000000000010000000000001100000001000000000011000L, 0b0000001000000110000000001000001000100000100000000000010000001100L, 54, 57),
	D6 (0b0100010000010101000000001000101000110000000000010000000000100000L, 0b0000000010010000010000000100001000000000100000101000100000000000L, 54, 57),
	E6 (0b0000010000001000000011000000000000001000000000001000000010000000L, 0b0000000000000000010010000000000100000100000000010001000001000010L, 54, 57),
	F6 (0b0000000000000010000000010000010000001000001000100000000000110000L, 0b0000100000000100000100000000000010100010000000000001000100001000L, 54, 57),
	G6 (0b1010000100000100000100100010000100001000000001000000000000010000L, 0b1000000000100100010010000001111010000101000010000001010000000000L, 54, 59),
	H6 (0b0000010000000001000000000011000001000010100010010000000000000010L, 0b0000001000001001000010000000001000010000010101000010001010000000L, 53, 59),
	A7 (0b1000000000000000100010100000000100000100001000000100001000000000L, 0b0000000000010010000000010000000110001000010000000000100000001000L, 53, 59),
	B7 (0b0100000000000010010000000000000010000000001000000000010010000000L, 0b0100000001000010000000110000001100001001001100000000000010000000L, 54, 59),
	C7 (0b0100001000000001010000000011001000000101101000001000001000000000L, 0b0100000001000000001001100000000100010100000001100000100000100000L, 54, 59),
	D7 (0b1000000000010000000000010000000000010000011001001010100100000000L, 0b0000000000000000100000000001101001000110000010000000000001000000L, 54, 59),
	E7 (0b0000000000010100000010000000000010000000100001000000000110000000L, 0b0001011001000000011000000000110000000101000001000000110000100100L, 54, 59),
	F7 (0b1000000001101000000000100000000010000000100001000000000010000000L, 0b0000010000000000011000000000110000010000100000001000001000000000L, 54, 59),
	G7 (0b0000000100000001000000000100001000000000010001000000000100000000L, 0b0010000001100010101001000001000000000110100000010000000010010000L, 54, 59),
	H7 (0b0000010000010011000000000000011000000000010100001000000100000000L, 0b0001010001100000010010000000001000000000110000000100100000001000L, 53, 59),
	A8 (0b0100000000001110000000000010000010000000010000010101010100000010L, 0b0000000000000000010000100000000001000100001000000010010000000000L, 52, 58),
	B8 (0b0010000100010000100001000100000100000010000000000010010000010010L, 0b1000000000001001000000001100000001000110000100000001000000000010L, 53, 59),
	C8 (0b0000001010000010100000000100000100001010010100100000000000100010L, 0b0000100000000000000000001001101000000000100001000000010000000000L, 53, 59),
	D8 (0b0000010000000000000010000101000000000001000000000010000000110101L, 0b0000000100000000000000000000010001000000010000100000001000000100L, 53, 59),
	E8 (0b1000000000000010000000000010010100001000101000000001000000010010L, 0b0010000000000000000000001000000000010000000000100000001000000000L, 53, 59),
	F8 (0b0000000000010001000000000000001010000100000000000000100000000001L, 0b0000000000000000000000010010010010011000100100000000000100000010L, 53, 59),
	G8 (0b1000010000000000001000001000000100010000000010000000001000000100L, 0b0010001000000100000010010010000001011000001000001000100100000000L, 53, 59),
	H8 (0b1000001000000000000000010000010000001110001000001000010101000010L, 0b0100000000000100110010000000000010001000000100100000000001000100L, 52, 58);
	
	private final byte sqrInd;
	
	private final long rookOccupancyMask;
	private final long bishopOccupancyMask;
	
	private final byte rookMagicShift;
	private final byte bishopMagicShift;
	
	private final long rookMagicNumber;
	private final long bishopMagicNumber;
	
	private final long[] rook;
	private final long[] bishop;
	private final long king;
	private final long knight;
	private final long pawnWhiteAdvance;
	private final long pawnWhiteCapture;
	private final long pawnBlackAdvance;
	private final long pawnBlackCapture;
	
	/**Requires magic numbers which can be generated and printed to the console externally, using the {@link #engine.MagicNumberGenerator MagicNumberGenerator}.
	 * 
	 * @param rookMagicNumber
	 * @param bishopMagicNumber
	 */
	private MoveDatabase(long rookMagicNumber, long bishopMagicNumber, int rookMagicShift, int bishopMagicShift) {
		int index;
		this.rookMagicNumber = rookMagicNumber;
		this.bishopMagicNumber = bishopMagicNumber;
		this.rookMagicShift = (byte)rookMagicShift;
		this.bishopMagicShift = (byte)bishopMagicShift;
		sqrInd = (byte)ordinal();
		rook = new long[1 << (64 - rookMagicShift)];
		bishop = new long[1 << (64 - bishopMagicShift)];
		Square sqr = Square.getByIndex(sqrInd);
		rookOccupancyMask = MoveMask.rookOccupancyMask(sqr);
		bishopOccupancyMask = MoveMask.bishopOccupancyMask(sqr);
		long[] rookOccVar = SliderAttack.occupancyVariations(rookOccupancyMask);
		long[] bishopOccVar = SliderAttack.occupancyVariations(bishopOccupancyMask);
		long[] rookAttVar = SliderAttack.rookAttackSetVariations(sqr, rookOccVar);
		long[] bishopAttVar = SliderAttack.bishopAttackSetVariations(sqr, bishopOccVar);
		for (int i = 0; i < rookOccVar.length; i++) {
			index = (int)((rookOccVar[i]*rookMagicNumber) >>> rookMagicShift);
			rook[index] = rookAttVar[i];
		}
		for (int i = 0; i < bishopOccVar.length; i++) {
			index = (int)((bishopOccVar[i]*bishopMagicNumber) >>> bishopMagicShift);
			bishop[index] = bishopAttVar[i];
		}
		king = MoveMask.kingMoveMask(sqr);
		knight = MoveMask.knightMoveMask(sqr);
		pawnWhiteAdvance = MoveMask.whitePawnAdvanceMask(sqr);
		pawnWhiteCapture = MoveMask.whitePawnCaptureMask(sqr);
		pawnBlackAdvance = MoveMask.blackPawnAdvanceMask(sqr);
		pawnBlackCapture = MoveMask.blackPawnCaptureMask(sqr);
	}
	/**Returns a simple king move mask bitmap.
	 * 
	 * @return
	 */
	public long getCrudeKingMoves() {
		return king;
	}
	/**Returns a simple knight move mask bitmap.
	 * 
	 * @return
	 */
	public long getCrudeKnightMoves() {
		return knight;
	}
	/**Returns a white pawn's capture-only mask bitmap.
	 * 
	 * @return
	 */
	public long getCrudeWhitePawnCaptures() {
		return pawnWhiteCapture;
	}
	/**Returns a black pawn's capture-only mask bitmap.
	 * 
	 * @return
	 */
	public long getCrudeBlackPawnCaptures() {
		return pawnBlackCapture;
	}
	/**Returns a simple rook move mask, i.e. the file and rank that cross each other on the square indexed by this enum instance.
	 * 
	 * @return
	 */
	public long getCrudeRookMoves() {
		return rook[0];
	}
	/**Returns a simple bishop move mask, i.e. the diagonal and anti-diagonal that cross each other on the square indexed by this enum instance.
	 * 
	 * @return
	 */
	public long getCrudeBishopMoves() {
		return bishop[0];
	}
	/**Returns a simple queen move mask, i.e. the file, rank, diagonal, and anti-diagonal that cross each other on the square indexed by this enum instance.
	 * 
	 * @return
	 */
	public long getCrudeQueenMoves() {
		return rook[0] | bishop[0];
	}
	/**Returns a white king's pseudo-legal move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhiteKingMoves(long allNonWhiteOccupied) {
		return king & allNonWhiteOccupied;
	}
	/**Returns a black king's pseudo-legal move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackKingMoves(long allNonBlackOccupied) {
		return king & allNonBlackOccupied;
	}
	/**Returns a white knight's pseudo-legal move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhiteKnightMoves(long allNonWhiteOccupied) {
		return knight & allNonWhiteOccupied;
	}
	/**Returns a black knight's pseudo-legal move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackKnightMoves(long allNonBlackOccupied) {
		return knight & allNonBlackOccupied;
	}
	/**Returns a white pawn's pseudo-legal attack set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhitePawnCaptures(long allBlackPieces) {
		return pawnWhiteCapture & allBlackPieces;
	}
	/**Returns a black pawn's pseudo-legal attack set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackPawnCaptures(long allWhitePieces) {
		return pawnBlackCapture & allWhitePieces;
	}
	/**Returns a white pawn's pseudo-legal quiet move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhitePawnAdvances(long allEmpty) {
		if (sqrInd > 15)
			return pawnWhiteAdvance & allEmpty;
		long adv = pawnWhiteAdvance & allEmpty;
		return adv | ((adv << 8) & allEmpty);
	}
	/**Returns a black pawn's pseudo-legal quiet move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackPawnAdvances(long allEmpty) {
		if (sqrInd < 48)
			return pawnBlackAdvance & allEmpty;
		long adv = pawnBlackAdvance & allEmpty;
		return adv | ((adv >>> 8) & allEmpty);
	}
	/**Returns a white pawn's pseudo-legal complete move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhitePawnMoves(long allBlackPieces, long allEmpty) {
		return getWhitePawnAdvances(allEmpty) | getWhitePawnCaptures(allBlackPieces);
	}
	/**Returns a black pawn's pseudo-legal complete move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackPawnMoves(long allWhitePieces, long allEmpty) {
		return getBlackPawnAdvances(allEmpty) | getBlackPawnCaptures(allWhitePieces);
	}
	/**Returns a white rook's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonWhiteOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getWhiteRookMoves(long allNonWhiteOccupied, long allOccupied) {
		return rook[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] & allNonWhiteOccupied;
	}
	/**Returns a black rook's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonWhiteOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getBlackRookMoves(long allNonBlackOccupied, long allOccupied) {
		return rook[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] & allNonBlackOccupied;
	}
	/**Returns a white bishop's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonWhiteOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getWhiteBishopMoves(long allNonWhiteOccupied, long allOccupied) {
		return bishop[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)] & allNonWhiteOccupied;
	}
	/**Returns a black bishop's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonWhiteOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getBlackBishopMoves(long allNonBlackOccupied, long allOccupied) {
		return bishop[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)] & allNonBlackOccupied;
	}
	/**Returns a white queen's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonWhiteOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getWhiteQueenMoves(long allNonWhiteOccupied, long allOccupied) {
		return (rook[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] |
			    bishop[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)]) & allNonWhiteOccupied;
	}
	/**Returns a black queen's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonWhiteOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getBlackQueenMoves(long allNonBlackOccupied, long allOccupied) {
		return (rook[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] |
			    bishop[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)]) & allNonBlackOccupied;
	}
	/**Returns a MoveDatabase enum instance that holds the preinitialized move sets for the square specified by the given square index, sqrInd.
	 * 
	 * @param sqrInd
	 * @return
	 */
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
