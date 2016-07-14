package engine;

import engine.Bitboard.*;
import util.BitOperations;

/**
 * A preinitialized move set database that saves the time costs of calculating move sets on the fly at the price of about 850KBytes. It contains
 * a so called fancy magic move tablebase for sliding piece attack set derivation with all the necessary masks and magic numbers included so the
 * desired move sets can be retrieved by simply invoking one of this database's functions and feeding it an occupancy bitboard or two. Beside the
 * sliding pieces' attack sets, all the other pieces masks have been preinitialized too, for the sake of simplicity.
 * 
 * @author Viktor
 *
 */
enum MoveSetDatabase {
	
	A1 (0x0880011080400121L, 0x0C40100582028020L, 52, 58),
	B1 (0x0840034020031004L, 0x00B0018301120000L, 53, 59),
	C1 (0x4080200050010A80L, 0x20100900D1002020L, 53, 59),
	D1 (0x0080080082100004L, 0x444C404080600004L, 53, 59),
	E1 (0x8200049002002128L, 0x8104042040028000L, 53, 59),
	F1 (0x11000201001C0088L, 0x100A021124808464L, 53, 59),
	G1 (0xC88021800A000900L, 0x00004C02A0100100L, 53, 59),
	H1 (0x1200010140208402L, 0x0409004700A01000L, 52, 58),
	A2 (0x0090800090400C20L, 0x0200400288030908L, 53, 59),
	B2 (0x0800C00520005000L, 0x0202081801A08200L, 54, 59),
	C2 (0x2001001020070040L, 0x2018D00486304000L, 54, 59),
	D2 (0x0202004010A04A00L, 0x0100142418860840L, 54, 59),
	E2 (0x0541001005000800L, 0x2000040421900020L, 54, 59),
	F2 (0x0080800200040081L, 0x4020120802082008L, 54, 59),
	G2 (0x6023003500940A00L, 0x0408040088241001L, 54, 59),
	H2 (0x0200800852800100L, 0x0008428084102208L, 53, 59),
	A3 (0x0104808000204000L, 0x04DC1C4004080240L, 53, 59),
	B3 (0x8000404001201008L, 0xC008002052008220L, 54, 59),
	C3 (0x0112020010208040L, 0x0044000204360200L, 54, 57),
	D3 (0x0002020008104120L, 0x040294080200C002L, 54, 57),
	E3 (0x0080510004380100L, 0x0207006811400015L, 54, 57),
	F3 (0x8080818004001A00L, 0x0041004200825900L, 54, 57),
	G3 (0x150004003009280EL, 0x40C400810C028207L, 54, 59),
	H3 (0x01604200010080C4L, 0x0012800609840100L, 53, 59),
	A4 (0x02C2836080044000L, 0x1050C00004140400L, 53, 59),
	B4 (0x0300200840005000L, 0x0030581205130400L, 54, 59),
	C4 (0x200A022200408310L, 0x30162402A0890400L, 54, 57),
	D4 (0x0100500080280080L, 0x000908021100C100L, 54, 55),
	E4 (0x814C850100080190L, 0x0047001041024002L, 54, 55),
	F4 (0x0001002300040008L, 0x183005010A805104L, 54, 57),
	G4 (0x2002100400224861L, 0x8002020800680220L, 54, 59),
	H4 (0x0105802080005100L, 0x0014006821008200L, 53, 59),
	A5 (0x0002644004800080L, 0x9030103000060482L, 53, 59),
	B5 (0x0000804000802010L, 0x3002180444201500L, 54, 59),
	C5 (0x000040A086001200L, 0x1100404803300020L, 54, 57),
	D5 (0xE000214202001810L, 0x0004140108040100L, 54, 55),
	E5 (0x0000080080802C00L, 0xC5040040100C0100L, 54, 55),
	F5 (0x8C80041008012040L, 0x1021140808610100L, 54, 57),
	G5 (0x0000800200800100L, 0x8810008488020208L, 54, 59),
	H5 (0x2001008122000244L, 0x101A04004104A211L, 53, 59),
	A6 (0x0904400060828000L, 0x4001032020045100L, 53, 59),
	B6 (0x8000500C20004000L, 0x0011052820200201L, 54, 59),
	C6 (0x0010040028002000L, 0x0111301088003010L, 54, 57),
	D6 (0x0018008010018008L, 0x102000C010400600L, 54, 57),
	E6 (0x0080880100050030L, 0x0C22384100C08400L, 54, 57),
	F6 (0x1002001005020008L, 0x0020202204488080L, 54, 57),
	G6 (0x00020011080A0004L, 0x0111860810408900L, 54, 59),
	H6 (0x0400C44400820001L, 0x200148009500008CL, 53, 59),
	A7 (0x0080008020C30100L, 0x2100411808400045L, 53, 59),
	B7 (0x8001002200409200L, 0x0181014104A00220L, 54, 59),
	C7 (0x4000420010802600L, 0x0801820203040000L, 54, 59),
	D7 (0x000121011A100100L, 0x0002000094040082L, 54, 59),
	E7 (0x0080800800140080L, 0x0281824029220108L, 54, 59),
	F7 (0x4008020080240080L, 0x4100202002008420L, 54, 59),
	G7 (0x4401000402000500L, 0x01202C6108011440L, 54, 59),
	H7 (0x1000004409008A00L, 0x0A10250800908004L, 53, 59),
	A8 (0x0280C10021308001L, 0x0069004804014810L, 52, 58),
	B8 (0x0041002040108206L, 0x80000101018220C0L, 53, 59),
	C8 (0x0480120082884022L, 0x0004046282080600L, 53, 59),
	D8 (0x00000824100100A1L, 0xC84404008084140CL, 53, 59),
	E8 (0x0006000891201402L, 0x4014428418106420L, 53, 59),
	F8 (0x1006001008040102L, 0xA000062020020882L, 53, 59),
	G8 (0x0044020810289104L, 0x4D10282284040420L, 53, 59),
	H8 (0x030089040024C082L, 0x0008010802204200L, 52, 58);
	
	private final byte sqrInd;
	
	private final long rookOccupancyMask;
	private final long bishopOccupancyMask;
	
	private final byte rookMagicShift;
	private final byte bishopMagicShift;
	
	private final long rookMagicNumber;
	private final long bishopMagicNumber;
	
	private final long[] rookMoveSets;
	private final long[] bishopMoveSets;
	public final long kingMoveMask;
	public final long knightMoveMask;
	private final long pawnWhiteAdvanceMoveMask;
	private final long pawnBlackAdvanceMoveMask;
	public final long pawnWhiteCaptureMoveMask;
	public final long pawnBlackCaptureMoveMask;
	
	/**
	 * Requires magic numbers which can be generated and printed to the console externally, using the
	 * {@link #engine.MagicNumberGenerator MagicNumberGenerator}.
	 * 
	 * @param rookMagicNumber
	 * @param bishopMagicNumber
	 */
	private MoveSetDatabase(long rookMagicNumber, long bishopMagicNumber, int rookMagicShift, int bishopMagicShift) {
		int index;
		long bit;
		long[] bishopOccupancyVariations, rookOccupancyVariations;
		long[] bishopAttackSetVariations, rookAttackSetVariations;
		this.rookMagicNumber = rookMagicNumber;
		this.bishopMagicNumber = bishopMagicNumber;
		this.rookMagicShift = (byte)rookMagicShift;
		this.bishopMagicShift = (byte)bishopMagicShift;
		sqrInd = (byte)ordinal();
		rookMoveSets = new long[1 << (64 - rookMagicShift)];
		bishopMoveSets = new long[1 << (64 - bishopMagicShift)];
		bit = 1L << sqrInd;
		bishopOccupancyMask = MultiMoveSets.bishopMoveSets(bit, 0, -1) & ~(File.A.bits | File.H.bits | Rank.R1.bits | Rank.R8.bits);
		rookOccupancyMask = (Bitboard.northFill(bit, ~Rank.R8.bits) | Bitboard.southFill(bit, ~Rank.R1.bits) |
				Bitboard.westFill(bit, ~File.A.bits) | Bitboard.eastFill(bit, ~File.H.bits))^bit;
		bishopOccupancyVariations = BitOperations.getAllSubsets(bishopOccupancyMask);
		rookOccupancyVariations = BitOperations.getAllSubsets(rookOccupancyMask);
		bishopAttackSetVariations = new long[bishopOccupancyVariations.length];
		rookAttackSetVariations = new long[rookOccupancyVariations.length];
		for (int i = 0; i < bishopOccupancyVariations.length; i++)
			bishopAttackSetVariations[i] = MultiMoveSets.bishopMoveSets(bit, -1, ~bishopOccupancyVariations[i]);
		for (int i = 0; i < rookOccupancyVariations.length; i++)
			rookAttackSetVariations[i] = MultiMoveSets.rookMoveSets(bit, -1, ~rookOccupancyVariations[i]);
		for (int i = 0; i < rookOccupancyVariations.length; i++) {
			index = (int)((rookOccupancyVariations[i]*rookMagicNumber) >>> rookMagicShift);
			rookMoveSets[index] = rookAttackSetVariations[i];
		}
		for (int i = 0; i < bishopOccupancyVariations.length; i++) {
			index = (int)((bishopOccupancyVariations[i]*bishopMagicNumber) >>> bishopMagicShift);
			bishopMoveSets[index] = bishopAttackSetVariations[i];
		}
		kingMoveMask = MultiMoveSets.kingMoveSets(bit, -1);
		knightMoveMask = MultiMoveSets.knightMoveSets(bit, -1);
		pawnWhiteAdvanceMoveMask = MultiMoveSets.whitePawnAdvanceSets(bit, -1);
		pawnWhiteCaptureMoveMask = MultiMoveSets.whitePawnCaptureSets(bit, -1);
		pawnBlackAdvanceMoveMask = MultiMoveSets.blackPawnAdvanceSets(bit, -1);
		pawnBlackCaptureMoveMask = MultiMoveSets.blackPawnCaptureSets(bit, -1);
	}
	/**
	 * Returns a simple rook move mask, i.e. the file and rank that cross each other on the square indexed by this enum instance.
	 * 
	 * @return
	 */
	public long getRookMoveMask() {
		return rookMoveSets[0];
	}
	/**
	 * Returns a simple bishop move mask, i.e. the diagonal and anti-diagonal that cross each other on the square indexed by this enum instance.
	 * 
	 * @return
	 */
	public long getBishopMoveMask() {
		return bishopMoveSets[0];
	}
	/**
	 * Returns a simple queen move mask, i.e. the file, rank, diagonal, and anti-diagonal that cross each other on the square indexed by this enum
	 * instance.
	 * 
	 * @return
	 */
	public long getQueenMoveMask() {
		return rookMoveSets[0] | bishopMoveSets[0];
	}
	/**
	 * Returns a king's pseudo-legal move set.
	 * 
	 * @param allOpponentOccupied
	 * @return
	 */
	public long getKingMoveSet(long allNonSameColorOccupied) {
		return kingMoveMask & allNonSameColorOccupied;
	}
	/**
	 * Returns a knight's pseudo-legal move set.
	 * 
	 * @param allNonSameColorOccupied
	 * @return
	 */
	public long getKnightMoveSet(long allNonSameColorOccupied) {
		return knightMoveMask & allNonSameColorOccupied;
	}
	/**
	 * Returns a white pawn's pseudo-legal attack set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhitePawnCaptureSet(long allBlackOccupied) {
		return pawnWhiteCaptureMoveMask & allBlackOccupied;
	}
	/**
	 * Returns a black pawn's pseudo-legal attack set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackPawnCaptureSet(long allWhiteOccupied) {
		return pawnBlackCaptureMoveMask & allWhiteOccupied;
	}
	/**
	 * Returns a white pawn's pseudo-legal quiet move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhitePawnAdvanceSet(long allEmpty) {
		if (sqrInd > 15)
			return pawnWhiteAdvanceMoveMask & allEmpty;
		long adv = pawnWhiteAdvanceMoveMask & allEmpty;
		return adv | ((adv << 8) & allEmpty);
	}
	/**
	 * Returns a black pawn's pseudo-legal quiet move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackPawnAdvanceSet(long allEmpty) {
		if (sqrInd < 48)
			return pawnBlackAdvanceMoveMask & allEmpty;
		long adv = pawnBlackAdvanceMoveMask & allEmpty;
		return adv | ((adv >>> 8) & allEmpty);
	}
	/**
	 * Returns a white pawn's pseudo-legal complete move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getWhitePawnMoveSet(long allBlackOccupied, long allEmpty) {
		long advSet, captSet = pawnWhiteCaptureMoveMask & allBlackOccupied;
		advSet = pawnWhiteAdvanceMoveMask & allEmpty;
		if (sqrInd < 16)
			advSet |= ((advSet << 8) & allEmpty);
		return advSet | captSet;
	}
	/**
	 * Returns a black pawn's pseudo-legal complete move set.
	 * 
	 * @param allNonWhiteOccupied
	 * @return
	 */
	public long getBlackPawnMoveSet(long allWhiteOccupied, long allEmpty) {
		long advSet, captSet = pawnBlackCaptureMoveMask & allWhiteOccupied;
		advSet = pawnBlackAdvanceMoveMask & allEmpty;
		if (sqrInd > 47)
			advSet |= ((advSet >>> 8) & allEmpty);
		return advSet | captSet;
	}
	/**
	 * Returns a rook's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonSameColorOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getRookMoveSet(long allNonSameColorOccupied, long allOccupied) {
		return rookMoveSets[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] & allNonSameColorOccupied;
	}
	
	/**
	 * Returns a bishop's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonSameColorOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getBishopMoveSet(long allNonSameColorOccupied, long allOccupied) {
		return bishopMoveSets[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)] & allNonSameColorOccupied;
	}
	/**
	 * Returns a queen's pseudo-legal move set given the occupancies fed to the method.
	 * 
	 * @param allNonSameColorOccupied
	 * @param allOccupied
	 * @return
	 */
	public long getQueenMoveSet(long allNonSameColorOccupied, long allOccupied) {
		return (rookMoveSets[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] |
			    bishopMoveSets[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)]) & allNonSameColorOccupied;
	}
	/**
	 * Returns a MoveDatabase enum instance that holds the preinitialized move sets for the square specified by the given square index, sqrInd.
	 * 
	 * @param sqrInd
	 * @return
	 */
	public static MoveSetDatabase getByIndex(int sqrInd) {
		switch(sqrInd) {
			case 0:  return A1; case 1:  return B1; case 2:  return C1; case 3:  return D1; case 4:  return E1; case 5:  return F1;
			case 6:  return G1; case 7:  return H1; case 8:  return A2; case 9:  return B2; case 10: return C2; case 11: return D2;
			case 12: return E2; case 13: return F2; case 14: return G2; case 15: return H2; case 16: return A3; case 17: return B3;
			case 18: return C3; case 19: return D3; case 20: return E3; case 21: return F3; case 22: return G3; case 23: return H3;
			case 24: return A4; case 25: return B4; case 26: return C4; case 27: return D4; case 28: return E4; case 29: return F4;
			case 30: return G4; case 31: return H4; case 32: return A5; case 33: return B5; case 34: return C5; case 35: return D5;
			case 36: return E5; case 37: return F5; case 38: return G5; case 39: return H5; case 40: return A6; case 41: return B6;
			case 42: return C6; case 43: return D6; case 44: return E6; case 45: return F6; case 46: return G6; case 47: return H6;
			case 48: return A7; case 49: return B7; case 50: return C7; case 51: return D7; case 52: return E7; case 53: return F7;
			case 54: return G7; case 55: return H7; case 56: return A8; case 57: return B8; case 58: return C8; case 59: return D8;
			case 60: return E8; case 61: return F8; case 62: return G8; case 63: return H8;
			default: throw new IllegalArgumentException("Invalid square index.");
		}
	}
}
