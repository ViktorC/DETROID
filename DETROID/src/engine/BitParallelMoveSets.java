package engine;

import engine.Board.File;

/**
 * A class for bit parallel move set generation.
 * 
 * @author Viktor
 *
 */
public final class BitParallelMoveSets {
	
	private BitParallelMoveSets() {
		
	}
	/**
	 * Generates a bitboard of the basic king's move set. Does not include target squares of castling; handles the wrap-around effect.
	 * 
	 * @param bitboard
	 * @param allNonSameColorOccupied
	 * @return
	 */
	public final static long getKingMoveSet(long king, long allNonSameColorOccupied) {
		return ((((king << 7) | (king >>> 9) | (king >>> 1)) & ~File.H.bits) |
				(king << 8) | (king >>> 8) |
				(((king << 9) | (king >>> 7) | (king << 1)) & ~File.A.bits)) & allNonSameColorOccupied;
	}
	/**
	 * Generates a bitboard of the basic knight's move set. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param knight
	 * @param allNonSameColorOccupied
	 * @return
	 */
	public final static long getKnightMoveSet(long knight, long allNonSameColorOccupied) {
		return ((((knight << 15) | (knight >>> 17)) & ~File.H.bits) |
				(((knight << 6) | (knight >>> 10)) & ~(File.H.bits | File.G.bits)) |
				(((knight << 10) | (knight >>> 6)) & ~(File.A.bits | File.B.bits)) |
				(((knight << 17) | (knight >>> 15)) & ~File.A.bits)) & allNonSameColorOccupied;
	}
	/**
	 * Generates a bitboard of the basic white pawn's capture set. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param whitePawns
	 * @param allOpponentOccupied
	 * @return
	 */
	public final static long getWhitePawnCaptureSet(long whitePawns, long allOpponentOccupied) {
		return (((whitePawns << 7) & ~File.H.bits) | ((whitePawns << 9) & ~File.A.bits)) & allOpponentOccupied;
	}
	/**
	 * Generates a bitboard of the basic black pawn's capture set. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param blackPawns
	 * @param allOpponentOccupied
	 * @return
	 */
	public final static long getBlackPawnCaptureSet(long blackPawns, long allOpponentOccupied) {
		return (((blackPawns >>> 9) & ~File.H.bits) | ((blackPawns >>> 7) & ~File.A.bits)) & allOpponentOccupied;
	}
	/**
	 * Generates a bitboard of the basic white pawn's advance set. Double advance from initial square is included. Occupancies are disregarded. It
	 * handles the wrap-around effect.
	 * 
	 * @param whitePawns
	 * @param allEmpty
	 * @return
	 */
	public final static long getWhitePawnAdvanceSet(long whitePawns, long allEmpty) {
		return (whitePawns << 8) & allEmpty;
	}
	/**
	 * Generates a bitboard of the basic black pawn's advance set. Double advance from initial square is included. Occupancies are disregarded. It
	 * handles the wrap-around effect.
	 * 
	 * @param blackPawns
	 * @param allEmpty
	 * @return
	 */
	public final static long getBlackPawnAdvanceSet(long blackPawns, long allEmpty) {
		return (blackPawns >>> 8) & allEmpty;
	}
	/**
	 * Generates a move set bitboard for a set of bishops.
	 *
	 * @param bishops
	 * @param allOpponentOccupied
	 * @param allEmpty
	 * @return
	 */
	public final static long getBishopMoveSet(long bishops, long allOpponentOccupied, long allEmpty) {
		long gen, attackSet = 0;
		gen = northWestFill(bishops, allEmpty);
		attackSet |= gen | (((gen << 7) & ~File.H.bits) & allOpponentOccupied);
		gen = southWestFill(bishops, allEmpty);
		attackSet |= gen | (((gen >>> 9) & ~File.H.bits) & allOpponentOccupied);
		gen = northEastFill(bishops, allEmpty);
		attackSet |= gen | (((gen << 9) & ~File.A.bits) & allOpponentOccupied);
		gen = southEastFill(bishops, allEmpty);
		attackSet |= gen | (((gen >>> 7) & ~File.A.bits) & allOpponentOccupied);
		return attackSet^bishops;
	}
	/**
	 * Generates a move set bitboard for a set of rooks.
	 * 
	 * @param rooks
	 * @param allOpponentOccupied
	 * @param allEmpty
	 * @return
	 */
	public final static long getRookMoveSet(long rooks, long allOpponentOccupied, long allEmpty) {
		long gen, attackSet = 0;
		gen = northFill(rooks, allEmpty);
		attackSet |= gen | ((gen << 8) & allOpponentOccupied);
		gen = southFill(rooks, allEmpty);
		attackSet |= gen | ((gen >>> 8) & allOpponentOccupied);
		gen = westFill(rooks, allEmpty);
		attackSet |= gen | (((gen >>> 1) & ~File.H.bits) & allOpponentOccupied);
		gen = eastFill(rooks, allEmpty);
		attackSet |= gen | (((gen << 1) & ~File.A.bits) & allOpponentOccupied);
		return attackSet^rooks;
	}
	/**
	 * Generates a move set bitboard for a set of queens.
	 * 
	 * @param queens
	 * @param allOpponentOccupied
	 * @param allEmpty
	 * @return
	 */
	public final static long getQueenMoveSet(long queens, long allOpponentOccupied, long allEmpty) {
		return getRookMoveSet(queens, allOpponentOccupied, allEmpty) | getBishopMoveSet(queens, allOpponentOccupied, allEmpty);
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long northFill(long generator, long propagator) {
		generator  |= (generator  << 8)  & propagator;
		propagator &= (propagator << 8);
		generator  |= (generator  << 16) & propagator;
		propagator &= (propagator << 16);
		generator  |= (generator  << 32) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long southFill(long generator, long propagator) {
		generator  |= (generator  >>> 8)  & propagator;
		propagator &= (propagator >>> 8);
		generator  |= (generator  >>> 16) & propagator;
		propagator &= (propagator >>> 16);
		generator  |= (generator  >>> 32) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction west of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is
	 * handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long westFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  >>> 1) & propagator;
		propagator &= (propagator >>> 1);
		generator  |= (generator  >>> 2) & propagator;
		propagator &= (propagator >>> 2);
		generator  |= (generator  >>> 4) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction east of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is
	 * handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long eastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  << 1) & propagator;
		propagator &= (propagator << 1);
		generator  |= (generator  << 2) & propagator;
		propagator &= (propagator << 2);
		generator  |= (generator  << 4) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-west of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long northWestFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  << 7)  & propagator;
		propagator &= (propagator << 7);
		generator  |= (generator  << 14) & propagator;
		propagator &= (propagator << 14);
		generator  |= (generator  << 28) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-east of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long northEastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  << 9)  & propagator;
		propagator &= (propagator << 9);
		generator  |= (generator  << 18) & propagator;
		propagator &= (propagator << 18);
		generator  |= (generator  << 36) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-west of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long southWestFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  >>> 9)  & propagator;
		propagator &= (propagator >>> 9);
		generator  |= (generator  >>> 18) & propagator;
		propagator &= (propagator >>> 18);
		generator  |= (generator  >>> 36) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-east of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	final static long southEastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  >>> 7)  & propagator;
		propagator &= (propagator >>> 7);
		generator  |= (generator  >>> 14) & propagator;
		propagator &= (propagator >>> 14);
		generator  |= (generator  >>> 28) & propagator;
		return generator;
	}
}
