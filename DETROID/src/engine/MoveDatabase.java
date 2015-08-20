package engine;

import engine.Board.*;

/**A preinitialized move database that saves the expenses of calculating move sets on the fly at the cost of about 850KBytes (now even less due to the denser
 * slider tables). It contains a so called fancy magic move tablebase for sliding piece attack set derivation with all the necessary masks and magic numbers
 * included so the desired move sets can be retrieved by simply invoking one of this database's functions and feeding it an occupancy bitmap or two. Beside
 * the sliding pieces' attack sets, all the other pieces masks have been preinitialized too, for the sake of simplicity.
 * 
 * Even though my {@link #engine.MagicNumberGenerator MagicNumberGenerator} should be able to do the job, I did not generate any of the 14 rook and 26 bishop
 * 'enhanced' magic numbers myself, but used the ones distributed on https://chessprogramming.wikispaces.com/Best+Magics+so+far. The rest has been generated
 * by a {@link #engine.MagicNumberGenerator MagicNumberGenerator} instance.
 * 
 * @author Viktor
 *
 */
public enum MoveDatabase {
	
	A1 (0b1001000010000000000000000001001000100000010000000000000010000001L, 0b1111111111101101111110011111110101111100111111001111111111111111L, 52, 59),
	B1 (0b0000000011000000000100000000001001100000000000000100000000000000L, 0b1111110000001001011000101000010101001010011101111111010101110110L, 53, 60),
	C1 (0b0000000010000000000010010010000000000000100000000101000000000100L, 0b0100000000000100000010000000000010000001000000010000000000010000L, 53, 59),
	D1 (0b0000000010000000000100000000000001000100100000000000100000000000L, 0b0000100000100100010000010000000000100000000100110000000001000011L, 53, 59),
	E1 (0b0000000010000000000000100000010000000000000010000000000010000000L, 0b0000000000000100000001010010000000000001001001001000000010000000L, 53, 59),
	F1 (0b0010000010000000000001000000000000100001000001100000000010000000L, 0b0000000000000010000000101000001000100000001000000100000000000000L, 53, 59),
	G1 (0b0110010000000000000011000000000010000001000000100011000000001000L, 0b1111110000001010011001101100011001001010011111101111010101110110L, 53, 60),
	H1 (0b0000001010000000000000100100010000100101000000000000000010000000L, 0b0111111111111101111111011111110010111101011110011111111111111111L, 52, 59),
	A2 (0b0000000001010000100000000000000010000000001000000100000000010100L, 0b1111110000001000010001101010011001001010001101001111111111110110L, 53, 60),
	B2 (0b1000000000010000010000000000000000100000000100000000000001000100L, 0b1111110000001000011110101000011101001010001111001111011111110110L, 54, 60),
	C2 (0b0000000000000110000000000100000001010110000000001000000001100000L, 0b0000000010000001100111100010001000000100000010010000000110000000L, 54, 59),
	D2 (0b0001100000000001000000000011000000000000001000110000001000001000L, 0b0110100000001101100001000100000001000000100010000000000001000000L, 54, 59),
	E2 (0b0000000000000000100000000000100000000011100000000000010000000000L, 0b0001001000100001000001000000010000100000110000100000000000000000L, 54, 59),
	F2 (0b0011000000000010000000000000100000000100000100000000101000000000L, 0b0000000000001010000000010011000000001000001000000000000000001000L, 54, 59),
	G2 (0b0001000100100001000000000000000100000000000001100000000010000100L, 0b1111110000001000011001001010111001011001101101001111111101110110L, 54, 60),
	H2 (0b0001000101000010000000000000010010000000010011000000001100010010L, 0b0011110000001000011000001010111101001011001101011111111101110110L, 53, 60),
	A3 (0b0101000100000011000000010000000000100010100000000000000001000000L, 0b0111001111000000000110101111010101101100111101001100111111111011L, 53, 60),
	B3 (0b0000000100110000000001000100000000000000010010001010000000000000L, 0b0100000110100000000111001111101011010110010010101010111111111100L, 54, 60),
	C3 (0b0000000001001010100000001000000001010000000000000010000000000001L, 0b0010000001001000000110100000010000001000000000001010000010011000L, 54, 57),
	D3 (0b0100000100000011100000011000000000001000000000000011000000000100L, 0b1000000000010100000000100000100000000100100100011001000000000000L, 54, 57),
	E3 (0b0000000100001110000000100000000000001000101000000001000000000100L, 0b0000001000001001000000010100100000100000000010000000010101000100L, 54, 57),
	F3 (0b0010010000100001000000010000000000000100000000001000100001000010L, 0b1011000000000010000000000000110000100000100101000010000000000000L, 54, 57),
	G3 (0b0000000010000000000001000000000000011000000100000000000110001010L, 0b0111110000001100000000101000111101011011001101001111111101110110L, 54, 60),
	H3 (0b0000100100000101000000100000000000010001000000001000000001010100L, 0b1111110000001010000000101000111001011010101101001101111101110110L, 53, 60),
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
	A6 (0b1000001010000000000000000100000100100000000001000100000000000011L, 0b1101110011101111110110011011010101001011111111001100000010011111L, 53, 60),
	B6 (0b0000000000110000000000000100110110100000000000010100000000000000L, 0b1111100101011111111110100111011001011010111111010110000000101011L, 54, 60),
	C6 (0b0000000000010000000000000010000000000001100000001000000000011000L, 0b0000001000000110000000001000001000100000100000000000010000001100L, 54, 57),
	D6 (0b0100010000010101000000001000101000110000000000010000000000100000L, 0b0000000010010000010000000100001000000000100000101000100000000000L, 54, 57),
	E6 (0b0000010000001000000011000000000000001000000000001000000010000000L, 0b0000000000000000010010000000000100000100000000010001000001000010L, 54, 57),
	F6 (0b0000000000000010000000010000010000001000001000100000000000110000L, 0b0000100000000100000100000000000010100010000000000001000100001000L, 54, 57),
	G6 (0b1010000100000100000100100010000100001000000001000000000000010000L, 0b0100001111111111100110100101110011110100110010100000110000000001L, 54, 60),
	H6 (0b0000010000000001000000000011000001000010100010010000000000000010L, 0b0100101111111111110011011000111001111100010110000111011000000001L, 53, 60),
	A7 (0b0100100011111111111111101001100111111110110011111010101000000000L, 0b1111110000001111111100101000011001010011001101001111010101110110L, 54, 60),
	B7 (0b0100100011111111111111101001100111111110110011111010101000000000L, 0b1111110000001011111101101100111001011001001001001111010101110110L, 55, 60),
	C7 (0b0100100101111111111111111010110111111111100111000010111000000000L, 0b0100000001000000001001100000000100010100000001100000100000100000L, 55, 59),
	D7 (0b0110000100111111111111111101110111111111110011101001001000000000L, 0b0000000000000000100000000001101001000110000010000000000001000000L, 55, 59),
	E7 (0b1111111111111111111111111110100111111111111001111100111000000000L, 0b0001011001000000011000000000110000000101000001000000110000100100L, 55, 59),
	F7 (0b1111111111111111111111111111010111111111111100111110011000000000L, 0b0000010000000000011000000000110000010000100000001000001000000000L, 55, 59),
	G7 (0b0000000100000001000000000100001000000000010001000000000100000000L, 0b1100001111111111101101111101110000110110110010101000110010001001L, 55, 60),
	H7 (0b0101000100001111111111111111010111110110001111001001011010100000L, 0b1100001111111111100010100101010011110100110010100010110010001001L, 54, 60),
	A8 (0b1110101111111111111111111011100111111111100111111100010100100110L, 0b1111111111111111111111001111110011111101011110011110110111111111L, 53, 59),
	B8 (0b0110000111111111111111101101110111111110111011011010111010101110L, 0b1111110000001000011000111111110011001011000101000111010101110110L, 54, 60),
	C8 (0b0101001110111111111111111110110111111111110111101011000110100010L, 0b0000100000000000000000001001101000000000100001000000010000000000L, 54, 59),
	D8 (0b0001001001111111111111111011100111111111110111111011010111110110L, 0b0000000100000000000000000000010001000000010000100000001000000100L, 54, 59),
	E8 (0b0100000100011111111111111101110111111111110110111111010011010110L, 0b0010000000000000000000001000000000010000000000100000001000000000L, 54, 59),
	F8 (0b0000000000010001000000000000001010000100000000000000100000000001L, 0b0000000000000000000000010010010010011000100100000000000100000010L, 53, 59),
	G8 (0b0000000000000011111111111110111100100111111011101011111001110100L, 0b1111110000001000011111101000111001001011101100101111011100110110L, 54, 60),
	H8 (0b0111011001000101111111111111111011001011111111101010011110011110L, 0b0100001111111111100111100100111011110100110010100010110010001001L, 53, 59);
	
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
	
	/**Requires magic numbers which can be generated and printed to the console externally, using {@link #engine.MagicNumberGenerator MagicNumberGenerator}.
	 * 
	 * @param rookMagicNumber
	 * @param bishopMagicNumber
	 * @param rookMagicShift
	 * @param bishopMagicShift
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
		rookOccupancyMask = MaskGenerator.rookOccupancyMask(sqr);
		bishopOccupancyMask = MaskGenerator.bishopOccupancyMask(sqr);
		long[] rookOccVar = SliderOccupancyVariationGenerator.occupancyVariations(rookOccupancyMask);
		long[] bishopOccVar = SliderOccupancyVariationGenerator.occupancyVariations(bishopOccupancyMask);
		long[] rookAttVar = SliderAttackSetCalculator.rookAttackSetVariations(sqr, rookOccVar);
		long[] bishopAttVar = SliderAttackSetCalculator.bishopAttackSetVariations(sqr, bishopOccVar);
		for (int i = 0; i < rookOccVar.length; i++) {
			index = (int)((rookOccVar[i]*rookMagicNumber) >>> rookMagicShift);
			rook[index] = rookAttVar[i];
		}
		for (int i = 0; i < bishopOccVar.length; i++) {
			index = (int)((bishopOccVar[i]*bishopMagicNumber) >>> bishopMagicShift);
			bishop[index] = bishopAttVar[i];
		}
		king 			 = MaskGenerator.kingMoveMask(sqr);
		knight 			 = MaskGenerator.knightMoveMasks(sqr);
		pawnWhiteAdvance = MaskGenerator.whitePawnAdvanceMasks(sqr);
		pawnWhiteCapture = MaskGenerator.whitePawnCaptureMasks(sqr);
		pawnBlackAdvance = MaskGenerator.blackPawnAdvanceMasks(sqr);
		pawnBlackCapture = MaskGenerator.blackPawnCaptureMasks(sqr);
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
