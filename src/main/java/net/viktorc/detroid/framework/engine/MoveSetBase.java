package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.engine.Bitboard.*;
import net.viktorc.detroid.framework.util.BitOperations;

import java.io.IOException;
import java.util.Map;

/**
 * An enum of pre-calculated move set databases for each square of a chess board for saving the time costs of
 * calculating move sets on the fly at the price of about 850KB. Each instance contains a so called 'fancy magic move
 * tablebase' for sliding pieces and pre-calculated move sets for all other pieces as well.
 * 
 * @author Viktor
 *
 */
public enum MoveSetBase {

	A1, B1, C1, D1, E1, F1, G1, H1,
	A2, B2, C2, D2, E2, F2, G2, H2,
	A3, B3, C3, D3, E3, F3, G3, H3,
	A4, B4, C4, D4, E4, F4, G4, H4,
	A5, B5, C5, D5, E5, F5, G5, H5,
	A6, B6, C6, D6, E6, F6, G6, H6,
	A7, B7, C7, D7, E7, F7, G7, H7,
	A8, B8, C8, D8, E8, F8, G8, H8;

	private final long kingMoveMask;
	private final long knightMoveMask;
	private final long pawnWhiteAdvanceMoveMask;
	private final long pawnBlackAdvanceMoveMask;
	private final long pawnWhiteCaptureMoveMask;
	private final long pawnBlackCaptureMoveMask;
	private final long rookOccupancyMask;
	private final long bishopOccupancyMask;
	private final byte rookMagicShift;
	private final byte bishopMagicShift;
	private final long rookMagicNumber;
	private final long bishopMagicNumber;
	private final long[] rookMoveSets;
	private final long[] bishopMoveSets;
	
	MoveSetBase() {
		int sqrInd = ordinal();
		long bit = Square.values()[sqrInd].getBitboard();
		kingMoveMask = Bitboard.computeKingMoveSets(bit, Bitboard.FULL_BOARD);
		knightMoveMask = Bitboard.computeKnightMoveSets(bit, Bitboard.FULL_BOARD);
		pawnWhiteAdvanceMoveMask = Bitboard.computeWhitePawnAdvanceSets(bit, Bitboard.FULL_BOARD);
		pawnBlackAdvanceMoveMask = Bitboard.computeBlackPawnAdvanceSets(bit, Bitboard.FULL_BOARD);
		pawnWhiteCaptureMoveMask = Bitboard.computeWhitePawnCaptureSets(bit, Bitboard.FULL_BOARD);
		pawnBlackCaptureMoveMask = Bitboard.computeBlackPawnCaptureSets(bit, Bitboard.FULL_BOARD);
		rookOccupancyMask = Bitboard.computeRookOccupancyMasks(bit);
		bishopOccupancyMask = Bitboard.computeBishopOccupancyMasks(bit);
		MagicsConfig magicsConfig = MagicsConfig.getInstance();
		try {
			magicsConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Map.Entry<Long,Byte> rookMagics = magicsConfig.getRookMagics(sqrInd);
		Map.Entry<Long,Byte> bishopMagics = magicsConfig.getBishopMagics(sqrInd);
		rookMagicNumber = rookMagics.getKey();
		rookMagicShift = rookMagics.getValue();
		bishopMagicNumber = bishopMagics.getKey();
		bishopMagicShift = bishopMagics.getValue();
		rookMoveSets = new long[1 << (64 - rookMagicShift)];
		bishopMoveSets = new long[1 << (64 - bishopMagicShift)];
		long[] rookOccupancyVariations = BitOperations.getAllSubsets(rookOccupancyMask);
		long[] bishopOccupancyVariations = BitOperations.getAllSubsets(bishopOccupancyMask);
		long[] rookMoveSetVariations = Bitboard.computeRookMoveSetVariations(bit, rookOccupancyVariations);
		long[] bishopMoveSetVariations = Bitboard.computeBishopMoveSetVariations(bit, bishopOccupancyVariations);
		for (int i = 0; i < rookOccupancyVariations.length; i++) {
			int index = (int) ((rookOccupancyVariations[i]*rookMagicNumber) >>> rookMagicShift);
			rookMoveSets[index] = rookMoveSetVariations[i];
		}
		for (int i = 0; i < bishopOccupancyVariations.length; i++) {
			int index = (int) ((bishopOccupancyVariations[i]*bishopMagicNumber) >>> bishopMagicShift);
			bishopMoveSets[index] = bishopMoveSetVariations[i];
		}
	}
	/**
	 * @return A simple king move mask from the square indexed by this enum instance.
	 */
	public long getKingMoveMask() {
		return kingMoveMask;
	}
	/**
	 * @return A simple queen move mask, i.e. the file, rank, diagonal, and anti-diagonal that cross each other on
	 * the square indexed by this enum instance.
	 */
	public long getQueenMoveMask() {
		return rookMoveSets[0] | bishopMoveSets[0];
	}
	/**
	 * @return A simple rook move mask, i.e. the file and rank that cross each other on the square indexed by this
	 * enum instance.
	 */
	public long getRookMoveMask() {
		return rookMoveSets[0];
	}
	/**
	 * @return A simple bishop move mask, i.e. the diagonal and anti-diagonal that cross each other on the square
	 * indexed by this enum instance.
	 */
	public long getBishopMoveMask() {
		return bishopMoveSets[0];
	}
	/**
	 * @return A simple knight move mask from the square indexed by this enum instance.
	 */
	public long getKnightMoveMask() {
		return knightMoveMask;
	}
	/**
	 * @return A simple white pawn advance mask from the square indexed by this enum instance.
	 */
	public long getPawnWhiteAdvanceMoveMask() {
		return pawnWhiteAdvanceMoveMask;
	}
	/**
	 * @return A simple black pawn advance mask from the square indexed by this enum instance.
	 */
	public long getPawnBlackAdvanceMoveMask() {
		return pawnBlackAdvanceMoveMask;
	}
	/**
	 * @return A simple white pawn capture mask from the square indexed by this enum instance.
	 */
	public long getPawnWhiteCaptureMoveMask() {
		return pawnWhiteCaptureMoveMask;
	}
	/**
	 * @return A simple black pawn capture mask from the square indexed by this enum instance.
	 */
	public long getPawnBlackCaptureMoveMask() {
		return pawnBlackCaptureMoveMask;
	}
	/**
	 * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the king.
	 * @return A king's pseudo-legal move set.
	 */
	public long getKingMoveSet(long allNonSameColorOccupied) {
		return kingMoveMask & allNonSameColorOccupied;
	}
	/**
	 * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the queen.
	 * @param allOccupied All squared occupied by any piece.
	 * @return A queen's pseudo-legal move set given the occupancies fed to the method.
	 */
	public long getQueenMoveSet(long allNonSameColorOccupied, long allOccupied) {
		return (rookMoveSets[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] |
				bishopMoveSets[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)]) &
				allNonSameColorOccupied;
	}
	/**
	 * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the rook.
	 * @param allOccupied All squared occupied by any piece.
	 * @return A rook's pseudo-legal move set given the occupancies fed to the method.
	 */
	public long getRookMoveSet(long allNonSameColorOccupied, long allOccupied) {
		return rookMoveSets[(int)(((rookOccupancyMask & allOccupied)*rookMagicNumber) >>> rookMagicShift)] &
				allNonSameColorOccupied;
	}
	/**
	 * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the bishop.
	 * @param allOccupied All squared occupied by any piece.
	 * @return A bishop's pseudo-legal move set given the occupancies fed to the method.
	 */
	public long getBishopMoveSet(long allNonSameColorOccupied, long allOccupied) {
		return bishopMoveSets[(int)(((bishopOccupancyMask & allOccupied)*bishopMagicNumber) >>> bishopMagicShift)] &
				allNonSameColorOccupied;
	}
	/**
	 * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the knight.
	 * @return A knight's pseudo-legal move set.
	 */
	public long getKnightMoveSet(long allNonSameColorOccupied) {
		return knightMoveMask & allNonSameColorOccupied;
	}
	/**
	 * @param allBlackOccupied All squares occupied by black pieces.
	 * @return A white pawn's pseudo-legal attack set.
	 */
	public long getWhitePawnCaptureSet(long allBlackOccupied) {
		return pawnWhiteCaptureMoveMask & allBlackOccupied;
	}
	/**
	 * @param allWhiteOccupied All squares occupied by white pieces.
	 * @return A black pawn's pseudo-legal attack set.
	 */
	public long getBlackPawnCaptureSet(long allWhiteOccupied) {
		return pawnBlackCaptureMoveMask & allWhiteOccupied;
	}
	/**
	 * @param allEmpty All empty squares.
	 * @return A white pawn's pseudo-legal quiet move set.
	 */
	public long getWhitePawnAdvanceSet(long allEmpty) {
		long adv = pawnWhiteAdvanceMoveMask & allEmpty;
		if (ordinal() < 16)
			adv |= Bitboard.computeWhitePawnAdvanceSets(adv, allEmpty);
		return adv;
	}
	/**
	 * @param allEmpty All empty squares.
	 * @return A black pawn's pseudo-legal quiet move set.
	 */
	public long getBlackPawnAdvanceSet(long allEmpty) {
		long adv = pawnBlackAdvanceMoveMask & allEmpty;
		if (ordinal() > 47)
			adv |= Bitboard.computeBlackPawnAdvanceSets(adv, allEmpty);
		return adv;
	}
	/**
	 * @param allBlackOccupied All squares occupied by black pieces.
	 * @param allEmpty All empty squares.
	 * @return A white pawn's pseudo-legal complete move set.
	 */
	public long getWhitePawnMoveSet(long allBlackOccupied, long allEmpty) {
		return getWhitePawnAdvanceSet(allEmpty) | getWhitePawnCaptureSet(allBlackOccupied);
	}
	/**
	 * @param allWhiteOccupied All squares occupied by white pieces.
	 * @param allEmpty All empty squares.
	 * @return A black pawn's pseudo-legal complete move set.
	 */
	public long getBlackPawnMoveSet(long allWhiteOccupied, long allEmpty) {
		return getBlackPawnAdvanceSet(allEmpty) | getBlackPawnCaptureSet(allWhiteOccupied);
	}
	
}
