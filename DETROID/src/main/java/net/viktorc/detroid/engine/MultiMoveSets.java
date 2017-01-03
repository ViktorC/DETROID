package main.java.net.viktorc.detroid.engine;

import main.java.net.viktorc.detroid.engine.Bitboard.File;

/**
 * A class for bit parallel move set generation.
 * 
 * @author Viktor
 *
 */
final class MultiMoveSets {
	
	private MultiMoveSets() {
		
	}
	/**
	 * Generates a bitboard of the basic king's move set. Does not include target squares of castling; handles the wrap-around effect.
	 * 
	 * @param bitboard
	 * @param allNonSameColorOccupied
	 * @return
	 */
	final static long kingMoveSets(long king, long allNonSameColorOccupied) {
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
	final static long knightMoveSets(long knight, long allNonSameColorOccupied) {
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
	final static long whitePawnCaptureSets(long whitePawns, long allOpponentOccupied) {
		return (((whitePawns << 7) & ~File.H.bits) | ((whitePawns << 9) & ~File.A.bits)) & allOpponentOccupied;
	}
	/**
	 * Generates a bitboard of the basic black pawn's capture set. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param blackPawns
	 * @param allOpponentOccupied
	 * @return
	 */
	final static long blackPawnCaptureSets(long blackPawns, long allOpponentOccupied) {
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
	final static long whitePawnAdvanceSets(long whitePawns, long allEmpty) {
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
	final static long blackPawnAdvanceSets(long blackPawns, long allEmpty) {
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
	final static long bishopMoveSets(long bishops, long allOpponentOccupied, long allEmpty) {
		long gen, attackSet = 0;
		gen = Bitboard.northWestFill(bishops, allEmpty);
		attackSet |= gen | (((gen << 7) & ~File.H.bits) & allOpponentOccupied);
		gen = Bitboard.southWestFill(bishops, allEmpty);
		attackSet |= gen | (((gen >>> 9) & ~File.H.bits) & allOpponentOccupied);
		gen = Bitboard.northEastFill(bishops, allEmpty);
		attackSet |= gen | (((gen << 9) & ~File.A.bits) & allOpponentOccupied);
		gen = Bitboard.southEastFill(bishops, allEmpty);
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
	final static long rookMoveSets(long rooks, long allOpponentOccupied, long allEmpty) {
		long gen, attackSet = 0;
		gen = Bitboard.northFill(rooks, allEmpty);
		attackSet |= gen | ((gen << 8) & allOpponentOccupied);
		gen = Bitboard.southFill(rooks, allEmpty);
		attackSet |= gen | ((gen >>> 8) & allOpponentOccupied);
		gen = Bitboard.westFill(rooks, allEmpty);
		attackSet |= gen | (((gen >>> 1) & ~File.H.bits) & allOpponentOccupied);
		gen = Bitboard.eastFill(rooks, allEmpty);
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
	final static long queenMoveSets(long queens, long allOpponentOccupied, long allEmpty) {
		return rookMoveSets(queens, allOpponentOccupied, allEmpty) | bishopMoveSets(queens, allOpponentOccupied, allEmpty);
	}
	
}
