package engine;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicLong;

import engine.Evaluator.Termination;
import engine.KillerTable.KillerTableEntry;
import engine.Move.MoveType;
import util.*;

/**
 * A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It
 * uses principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative
 * deepening framework.
 * 
 * @author Viktor
 *
 */
public class Search implements Runnable {
	
	/**
	 * A simple enum for game tree node types based on their values' relation to alpha and beta.
	 * 
	 * @author Viktor
	 *
	 */
	public enum NodeType {
		
		EXACT,
		FAIL_HIGH,
		FAIL_LOW;
		
		public final byte ind;
		
		private NodeType() {
			this.ind = (byte)ordinal();
		}
	}
	
	/**
	 * An observable class for the principal variation of a search.
	 * 
	 * @author Viktor
	 *
	 */
	public class Results extends Observable {
		
		private Queue<Move> PVline;	// Principal variation.
		private short score;		// The result score of the search.
		private long nodes;			// The number of nodes searched.
		private long time;			// Time spent on the search.
		
		private Results() {
			
		}
		/**
		 * Returns the principal variation of the search as a queue of moves.
		 * 
		 * @return
		 */
		public Queue<Move> getPVline() {
			return PVline;
		}
		/**
		 * Returns the result score of the search for the side to move.
		 * 
		 * @return
		 */
		public short getScore() {
			return score;
		}
		/**
		 * Returns the number of nodes searched to reach this result.
		 * 
		 * @return
		 */
		public long getNodes() {
			return nodes;
		}
		/**
		 * Returns the time spent on the search to reach this result in milliseconds.
		 * 
		 * @return
		 */
		public long getTime() {
			return time;
		}
		private void set(Queue<Move> PVline, short score, long nodes, long time) {
			this.PVline = PVline;
			this.score = score;
			this.nodes = nodes;
			this.time = time;
			setChanged();
			notifyObservers();
		}
	}
	
	public final static int MAX_NOMINAL_SEARCH_DEPTH = 64;
	private final static int MAX_EXPECTED_TOTAL_SEARCH_DEPTH = 8*3*MAX_NOMINAL_SEARCH_DEPTH;	// Including extensions and quiescence search.
	
	private final static int NMR = 2;													// Null move pruning reduction.
	private final static int LMR = 1;													// Late move reduction.
	private final static int FMAR1 = Material.KNIGHT.score;								// Futility margin.
	private final static int FMAR2 = Material.ROOK.score;								// Extended futility margin.
	private final static int FMAR3 = Material.QUEEN.score;								// Razoring margin.
	private final static int A_DELTA = Material.PAWN.score/2;							// The aspiration delta within iterative deepening.
	private final static int Q_DELTA = Material.KNIGHT.score - Material.PAWN.score/2;	// The margin for delta-pruning in the quiescence search.
	
	private final static int FULL_PLY = 4;	// For fractional ply extensions.
	private int ply;
	
	private Position pos;
	private boolean nullMoveObservHolds;	// Whether heuristics based on the null move observation such as stand-pat and NMP are applicable.
	
	private KillerTable kT;				// Killer heuristic table.
	private RelativeHistoryTable hT;	// History heuristic table.
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte hashEntryGen;	// Entry generation.
	
	private Evaluator eval;
	
	private boolean pondering;
	private long searchTime;
	private long deadLine;
	private long maxNodes;
	private AtomicLong nodes;	// Number of searched positions.
	
	private boolean doBreak = false;
	
	private Results results;
	
	/**
	 * Creates a new Search thread instance for searching a position for the specified amount of time, maximum number of searched nodes, and
	 * moves to search; if maxNodes is <= 0, the engine will ponder on the position once the search thread is started, and will not stop until
	 * it is interrupted.
	 * 
	 * @param pos The position to search.
	 * @param timeLeft Time left until the end of the time control.
	 * @param maxNodes Max number of searched nodes.
	 * @param moves Moves to search.
	 * @param gamePhase The game phase in which the position is.
	 * @param hT History table.
	 * @param hashEntryGen Hash entry generation.
	 * @param tT Transposition table.
	 * @param eT Evaluation hash table.
	 * @param pT Pawn hash table.
	 */
	public Search(Position pos, long timeLeft, long maxNodes, Move[] moves, GamePhase gamePhase,
			RelativeHistoryTable hT, byte hashEntryGen, HashTable<TTEntry> tT, HashTable<ETEntry> eT, HashTable<PTEntry> pT) {
		this.pos = pos.deepCopy();
		if (maxNodes <= 0)
			pondering = true;
		else {
			pondering = false;
			searchTime = timeLeft <= 0 ? 0 : allocateSearchTime(pos, gamePhase, timeLeft);
		}
		nullMoveObservHolds = gamePhase != GamePhase.END_GAME;
		kT = new KillerTable(3*MAX_NOMINAL_SEARCH_DEPTH + 1);
		this.hT = hT;
		this.hashEntryGen = hashEntryGen;
		this.tT = tT;
		this.eT = eT;
		this.pT = pT;
		results = new Results();
	}
	/**
	 * Returns an observable container class for the results of the search.
	 * 
	 * @return
	 */
	public Results getResults() {
		return results;
	}
	/**
	 * Returns a queue of Move objects according to the best line of play extracted form the transposition table.
	 * 
	 * @return A queue of Move objects according to the best line of play.
	 */
	private Queue<Move> extractPv() {
		// In case every ply within the search triggers the maximum number of fractional extensions.
		Move[] pVarr = new Move[3*MAX_NOMINAL_SEARCH_DEPTH + 1];
		Queue<Move> pV = new Queue<>();
		TTEntry e;
		Move bestMove;
		int i, j;
		i = j = 0;
		while ((e = tT.lookUp(pos.key)) != null && e.bestMove != 0 && i < MAX_NOMINAL_SEARCH_DEPTH) {
			bestMove = Move.toMove(e.bestMove);
			pos.makeMove(bestMove);
			pVarr[i++] = bestMove;
		}
		for (int k = 0; k < i; k++)
			pos.unmakeMove();
		while (j < pVarr.length && pVarr[j] != null)
			pV.add(pVarr[j++]);
		return pV;
	}
	@Override
	public void run() {
		eval = new Evaluator(eT, pT, hashEntryGen);
		deadLine = System.currentTimeMillis() + searchTime;
		iterativeDeepening();
	}
	private long allocateSearchTime(Position pos, GamePhase gamePhase, long timeLeft) {
		// !FIXME
		return 30*1000;
	}
	/**
	 * Starts searching the current position until the allocated search time has passed, or the thread is interrupted, or the maximum search
	 * depth has been reached.
	 */
	private void iterativeDeepening() {
		int alpha, beta, score, failHigh, failLow;
		nodes = new AtomicLong(0);
		alpha = Termination.CHECK_MATE.score;
		beta = -alpha;
		failHigh = failLow = 0; // The number of consecutive fail highs/fail lows.
		for (int i = 1; i <= MAX_NOMINAL_SEARCH_DEPTH; i++) {
			ply = i;
			score = pVsearch(pos, ply*FULL_PLY, alpha, beta, true, 0);
			results.set(extractPv(), (short)score, nodes.get(), System.currentTimeMillis() - (deadLine - searchTime));
			if (doBreak || (!pondering && (Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)))
				break;
			// Aspiration windows with gradual widening.
			if (score <= alpha) {
				alpha = failLow == 0 ? Math.max(alpha - 2*A_DELTA, Termination.CHECK_MATE.score) :
					failLow == 1 ? Math.max(alpha - 5*A_DELTA, Termination.CHECK_MATE.score) :Termination.CHECK_MATE.score;
				failLow++;
				if (i == MAX_NOMINAL_SEARCH_DEPTH)
					i--;
				continue;
			}
			if (score >= beta) {
				beta = failHigh == 0 ? Math.min(beta + 2*A_DELTA, -Termination.CHECK_MATE.score) :
					failHigh == 1 ? Math.min(beta + 5*A_DELTA, -Termination.CHECK_MATE.score) : -Termination.CHECK_MATE.score;
				failHigh++;
				if (i == MAX_NOMINAL_SEARCH_DEPTH)
					i--;
				continue;
			}
			failHigh = failLow = 0;
			alpha = score - A_DELTA;
			beta = score + A_DELTA;
		}
	}
	/**
	 * A principal variation search algorithm utilizing a transposition table. It returns only the score for the searched position, but the
	 * principal variation can be extracted from the transposition table after a search has been run.
	 * 
	 * @param depth Depth of the main search in plies.
	 * @param alpha
	 * @param beta
	 * @param nullMoveAllowed
	 * @param qDepth The depth with which quiescence search should be called.
	 * @return The score of the position searched.
	 */
	private int pVsearch(Position pos, int depth, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
		final int checkMateLim = Termination.CHECK_MATE.score + MAX_EXPECTED_TOTAL_SEARCH_DEPTH;
		final int distFromRoot = ply - depth/FULL_PLY;
		final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
		final int origAlpha = alpha;
		final boolean inCheck = pos.inCheck();
		int bestScore, score, searchedMoves, matMoveBreakInd, extPly, kMove, bestMoveInt, evalScore, razRed, extension;
		Move hashMove, bestMove, killerMove1, killerMove2, move, lastMove;
		boolean isThereHashMove, isThereKM1, isThereKM2, lastMoveIsMaterial;
		Queue<Move> matMoves, nonMatMoves;
		Move[] matMovesArr, nonMatMovesArr;
		TTEntry e;
		KillerTableEntry kE;
		bestScore = Termination.CHECK_MATE.score;
		bestMove = null;
		searchedMoves = 0;
		hashMove = killerMove1 = killerMove2 = null;
		isThereHashMove = isThereKM1 = isThereKM2 = false;
		matMoves = nonMatMoves = null;
		nodes.incrementAndGet();
		if (!pondering && (Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine || nodes.get() >= maxNodes))
			doBreak = true;
		Search: {
			// Check for the repetition rule; return a draw score if it applies.
			if (pos.getRepetitions() >= 3)
				return Termination.DRAW_CLAIMED.score;
			// Mate distance pruning.
			if (-mateScore < beta) {
				if (alpha >= -mateScore)
					return -mateScore;
			}
			if (mateScore > alpha) {
				  if (beta <= mateScore)
					  return mateScore;
			}
			// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
			e = tT.lookUp(pos.key);
			if (e != null) {
				e.generation = hashEntryGen;
				// If the hashed entry's depth is greater than or equal to the current search depth, adjust alpha and beta accordingly or return
				// the score if the entry stored a PV node. 
				if (e.depth >= depth/FULL_PLY) {
					// Mate score adjustment to root distance.
					if (e.score <= checkMateLim)
						score = e.score + distFromRoot;
					else if (e.score >= -checkMateLim)
						score = e.score - distFromRoot;
					else
						score = e.score;
					/* If the score was exact, or it was the score of an all node and is smaller than or equal to alpha, or it is that of a cut
					 * node and is greater than or equal to beta, return the score. */
					if (e.type == NodeType.EXACT.ind ||
							/* To make sure that a score that might not have been the exact score for the subtree below the node regardless of the
							 * alpha-beta boundaries is not treated as an exact score in the current context, we can not allow it to fall between
							 * the current alpha and beta. If it was a fail high node, the score is a lower boundary of the exact score of the
							 * node due to there possibly being siblings to the right of the child node that raised alpha higher than beta and
							 * caused a cut-off that could raise alpha even higher. If it was a fail low node, the score is a higher boundary for
							 * the exact score of the node, because all children of a fail low node are fail high nodes (-score <= alpha ->
							 * score >= -alpha [-alpha = beta in the child node]). To keep the interval of values the exact score could take on
							 * out of (alpha, beta), the score has to be lower than or equal to alpha if it is a higher boundary, i.e. fail low
							 * score, and it has to be greater than or equal to beta if it is a lower boundary i.e. fail high score.
							 */
							(e.type == NodeType.FAIL_HIGH.ind && score >= beta) || (e.type == NodeType.FAIL_LOW.ind && score <= alpha)) {
						if (score >= beta && e.bestMove != 0 && !(bestMove = Move.toMove(e.bestMove)).isMaterial()) {
							// Only if not in the quiescence search's check extension.
							if (qDepth == 0)
								kT.add(distFromRoot, bestMove);
						}
						return score;
					}
					
				}
				// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
				if (e.bestMove != 0) {
					hashMove = Move.toMove(e.bestMove);
					isThereHashMove = true;
				}
			}
			// Return the score from the quiescence search in case a leaf node has been reached.
			if (depth/FULL_PLY <= 0) {
				score = quiescence(pos, qDepth, alpha, beta);
				if (score > bestScore) {
					bestMove = null;
					bestScore = score;
					if (score > alpha)
						alpha = score;
				}
				break Search;
			}
			// Check extension (less than a whole ply because the quiescence search handles checks).
			depth = inCheck && qDepth == 0 ? depth + FULL_PLY/4 : depth;
			// If there is no hash entry in a PV node that is to be searched deep, try IID.
			if (!isThereHashMove && depth/FULL_PLY >= 5 && beta > origAlpha + 1) {
				extPly = ply;
				for (int i = 1; i < depth/FULL_PLY*3/5; i++) {
					ply = i;
					pVsearch(pos, ply*FULL_PLY, alpha, beta, true, qDepth);
				}
				ply = extPly;
				e = tT.lookUp(pos.key);
				if (e != null && e.bestMove != 0) {
					hashMove = Move.toMove(e.bestMove);
					isThereHashMove = true;
				}
			}
			lastMove = pos.getLastMove();
			lastMoveIsMaterial = lastMove != null && lastMove.isMaterial();
			// If there is a hash move, search that first.
			if (isThereHashMove) {
				// Recapture extension (includes capturing newly promoted pieces).
				extension = lastMoveIsMaterial && hashMove.capturedPiece != Piece.NULL.ind && hashMove.to == lastMove.to ? FULL_PLY/2 : 0;
				pos.makeMove(hashMove);
				score = -pVsearch(pos, depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = hashMove;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta) {
							if (!hashMove.isMaterial()) {
								if (qDepth == 0)
									kT.add(distFromRoot, hashMove);	// Add to killer moves.
								hT.recordSuccessfulMove(hashMove);	// Record success in the relative history table.
							}
							break Search;
						}
						else if (!hashMove.isMaterial())
							hT.recordUnsuccessfulMove(hashMove);	// Record failure in the relative history table.
					}
				}
			}
			// Generate material moves.
			matMoves = pos.generateMaterialMoves();
			// If there was no hash move or material moves, perform a mate check.
			if (!isThereHashMove && matMoves.length() == 0) {
				nonMatMoves = pos.generateNonMaterialMoves();
				if (nonMatMoves.length() == 0) {
					score = inCheck ? mateScore : Termination.STALE_MATE.score;
					if (score > bestScore) {
						bestMove = null;
						bestScore = score;
						if (score > alpha)
							alpha = score;
					}
					break Search;
				}
			}
			// Check for the fifty-move rule; return a draw score if it applies.
			if (pos.getFiftyMoveRuleClock() >= 100)
				return Termination.DRAW_CLAIMED.score;
			// If it is not a terminal node, try null move pruning if it is allowed and the side to move is not in check.
			if (nullMoveAllowed && nullMoveObservHolds && depth/FULL_PLY >= NMR && !inCheck) {
				pos.makeNullMove();
				// Do not allow consecutive null moves.
				if (depth/FULL_PLY == NMR) {
					score = -pVsearch(pos, depth - NMR*FULL_PLY, -beta, -beta + 1, false, qDepth);
					// Mate threat extension.
					if (score <= checkMateLim)
						depth += FULL_PLY;
				}
				else
					score = -pVsearch(pos, depth - (NMR + 1)*FULL_PLY, -beta, -beta + 1, false, qDepth);
				pos.unmakeMove();
				if (score >= beta) {
					return score;
				}
			}
			// Order the material moves.
			matMovesArr = orderMaterialMovesMVVLVA(matMoves);
			matMoveBreakInd = 0;
			// Search winning and equal captures.
			for (int i = 0; i < matMovesArr.length; i++) {
				move = matMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				// If the current move's order-value indicates a losing capture, break the search to check the killer moves.
				if (move.value < 0) {
					matMoveBreakInd = i;
					break;
				}
				// Recapture extension (includes capturing newly promoted pieces).
				extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? FULL_PLY/2 : 0;
				pos.makeMove(move);
				// PVS.
				if (i == 0)
					score = -pVsearch(pos, depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
				else {
					score = -pVsearch(pos, depth + extension - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
					if (score > alpha && score < beta)
						score = -pVsearch(pos, depth + extension - FULL_PLY, -beta, -score, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta)
							break Search;
					}
				}
				if (doBreak)
					break Search;
			}
			// If there are no more winning or equal captures, check and search the killer moves if legal from this position.
			kE = kT.retrieve(distFromRoot);
			if ((kMove = kE.getMove1()) != 0) {	// Killer move no. 1.
				killerMove1 = Move.toMove(kMove);
				if (pos.isLegalSoft(killerMove1) && (!isThereHashMove || !killerMove1.equals(hashMove))) {
					isThereKM1 = true;
					pos.makeMove(killerMove1);
					if (!isThereHashMove && matMoveBreakInd == 0)
						score = -pVsearch(pos, depth - FULL_PLY, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(pos, depth - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(pos, depth - FULL_PLY, -beta, -score, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = killerMove1;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								hT.recordSuccessfulMove(killerMove1);	// Record success in the relative history table.
								break Search;
							}
							else
								hT.recordUnsuccessfulMove(killerMove1);	// Record failure in the relative history table.
						}
					}
					if (doBreak)
						break Search;
				}
			}
			if ((kMove = kE.getMove2()) != 0) {	// Killer move no. 2.
				killerMove2 = Move.toMove(kMove);
				if (pos.isLegalSoft(killerMove2) && (!isThereHashMove || !killerMove2.equals(hashMove))) {
					isThereKM2 = true;
					pos.makeMove(killerMove2);
					if (!isThereHashMove && !isThereKM1 && matMoveBreakInd == 0)
						score = -pVsearch(pos, depth - FULL_PLY, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(pos, depth - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(pos, depth - FULL_PLY, -beta, -score, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = killerMove2;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								hT.recordSuccessfulMove(killerMove2);	// Record success in the relative history table.
								break Search;
							}
							else
								hT.recordUnsuccessfulMove(killerMove2);	// Record failure in the relative history table.
						}
					}
					if (doBreak)
						break Search;
				}
			}	// Killer move check ending.
			// Search losing captures if there are any.
			for (int i = matMoveBreakInd; i < matMovesArr.length; i++) {
				move = matMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				// Recapture extension.
				extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? FULL_PLY/2 : 0;
				pos.makeMove(move);
				// PVS.
				if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2)
					score = -pVsearch(pos, depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
				else {
					score = -pVsearch(pos, depth + extension - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
					if (score > alpha && score < beta)
						score = -pVsearch(pos, depth + extension - FULL_PLY, -beta, -score, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta)
							break Search;
					}
				}
				if (doBreak)
					break Search;
			}
			// Generate the non-material legal moves if they are not generated yet.
			if (nonMatMoves == null)
				nonMatMoves = pos.generateNonMaterialMoves();
			// One reply extension.
			if (matMoves.length() == 0 && nonMatMoves.length() == 1)
				depth += FULL_PLY/2;
			evalScore = score = eval.score(pos, alpha, beta);
			// Order and search the non-material moves.
			nonMatMovesArr = orderNonMaterialMoves(nonMatMoves);
			for (int i = 0; i < nonMatMovesArr.length; i++) {
				move = nonMatMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				// If this move was the first killer move, skip it.
				if (isThereKM1 && move.equals(killerMove1)) {
					isThereKM1 = false;
					continue;
				}
				// If this move was the second killer move, skip it.
				if (isThereKM2 && move.equals(killerMove2)) {
					isThereKM2 = false;
					continue;
				}
				razRed = 0;
				// Futility pruning, extended futility pruning, and razoring.
				if (depth/FULL_PLY <= 3) {
					if (alpha > checkMateLim && beta < -checkMateLim && !inCheck && lastMoveIsMaterial && !pos.givesCheck(move)) {
						if (depth/FULL_PLY == 1) {
							if (evalScore + FMAR1 <= alpha)
								continue;
						}
						else if (depth/FULL_PLY == 2) {
							if (evalScore + FMAR2 <= alpha)
								continue;
						}
						else {
							if (evalScore + FMAR3 <= alpha)
								razRed = 1;
						}
					}
				}
				pos.makeMove(move);
				// Try late move reduction if not within the PV.
				if (depth/FULL_PLY > 2 && beta == origAlpha + 1 && !inCheck && pos.getUnmakeRegister().checkers == 0 &&
						alpha < checkMateLim && searchedMoves > 4) {
					score = -pVsearch(pos, depth - (LMR + 1)*FULL_PLY, -alpha - 1, -alpha, true, qDepth);
					// If it does not fail low, research with "full" window.
					if (score > alpha)
						score = -pVsearch(pos, depth - FULL_PLY, -beta, -score, true, qDepth);
				}
				// Else PVS.
				else if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2 && matMovesArr.length == 0)
					score = -pVsearch(pos, depth - (razRed + 1)*FULL_PLY, -beta, -alpha, true, qDepth);
				else {
					score = -pVsearch(pos, depth - (razRed + 1)*FULL_PLY, -alpha - 1, -alpha, true, qDepth);
					if (score > alpha && score < beta)
						score = -pVsearch(pos, depth - (razRed + 1)*FULL_PLY, -beta, -score, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta) {	// Cutoff from a non-material move.
							if (qDepth == 0)
								kT.add(distFromRoot, move);	// Add to killer moves.
							hT.recordSuccessfulMove(move);	// Record success in the relative history table.
							break Search;
						}
						else
							hT.recordUnsuccessfulMove(move);	// Record failure in the relative history table.
					}
				}
				if (doBreak)
					break Search;
			}
		}
		// If the search has been invoked from quiescence search, do not store entries in the TT.
		if (qDepth < 0)
			return bestScore;
		// Adjustment of the best score for TT insertion according to the distance from the mate position in case it's a check mate score.
		if (bestScore <= checkMateLim)
			score = bestScore - distFromRoot;
		else if (bestScore >= -checkMateLim)
			score = bestScore + distFromRoot;
		else
			score = bestScore;
		bestMoveInt = bestMove == null ? 0 : bestMove.toInt();
		//	Add new entry to the transposition table.
		if (bestScore <= origAlpha)
			tT.insert(new TTEntry(pos.key, (short)(depth/FULL_PLY), NodeType.FAIL_LOW.ind, (short)score, bestMoveInt, hashEntryGen));
		else if (bestScore >= beta)
			tT.insert(new TTEntry(pos.key, (short)(depth/FULL_PLY), NodeType.FAIL_HIGH.ind, (short)score, bestMoveInt, hashEntryGen));
		else
			tT.insert(new TTEntry(pos.key, (short)(depth/FULL_PLY), NodeType.EXACT.ind, (short)score, bestMoveInt, hashEntryGen));
		// Return the unadjusted best score.
		return bestScore;
	}
	/**
	 * A search algorithm for diminishing the horizon effect once the main search algorithm has reached a leaf node. It keep searching until
	 * the side to move is not in check and does not have any legal winning captures according to SEE.
	 * 
	 * In the first two plies (unless it has been extended due to the side to move being in chess), it also searches moves that give check.
	 * 
	 * @param depth
	 * @param alpha
	 * @param beta
	 * @return
	 */
	public int quiescence(Position pos, int depth, int alpha, int beta) {
		final int distFromRoot = ply - depth;
		final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
		final boolean inCheck = pos.inCheck();
		List<Move> materialMoves, allMoves;
		Move[] moves;
		Move move;
		int bestScore, searchScore;
		if (depth != 0)
			nodes.incrementAndGet();
		if (!pondering && (Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine || nodes.get() >= maxNodes))
			doBreak = true;
		// Just for my peace of mind.
		if (distFromRoot >= MAX_EXPECTED_TOTAL_SEARCH_DEPTH)
			return eval.score(pos, alpha, beta);
		// If the side to move is in check, stand-pat does not hold and the main search will be called later on so no moves need to be generated.
		if (inCheck) {
			materialMoves = null;
			bestScore = mateScore;
		}
		/* Generate tactical and quiet moves separately then combine them in the quiet move list for evaluation of the position for stand-pat
		 * this way the ordering if the interesting moves can be restricted to only the tactical moves. */
		else {
			// Generate only material moves.
			materialMoves = pos.generateMaterialMoves();
			// Stand-pat, evaluate position.
			if (nullMoveObservHolds) {
				if (materialMoves.length() == 0) {
					allMoves = pos.generateNonMaterialMoves();
					allMoves.addAll(materialMoves);
				}
				else
					allMoves = null;
				if (allMoves != null && allMoves.length() == 0)
					bestScore = inCheck ? mateScore : Termination.STALE_MATE.score;
				// Use evaluation hash table.
				else
					bestScore = eval.score(pos, alpha, beta);
			}
			// No bound.
			else
				bestScore = mateScore;
		}
		// Fail soft.
		if (bestScore > alpha) {
			alpha = bestScore;
			if (bestScore >= beta)
				return bestScore;
		}
		// If check, call the main search for one ply (while keeping the track of the quiescence search depth to avoid resetting it).
		if (inCheck) {
			nodes.decrementAndGet();
			searchScore = pVsearch(pos, FULL_PLY, alpha, beta, false, depth - 2);
			if (searchScore > bestScore) {
				bestScore = searchScore;
				if (bestScore > alpha)
					alpha = bestScore;
			}
		}
		// Quiescence search.
		else {
			moves = orderMaterialMovesSEE(pos, materialMoves);
			for (int i = 0; i < moves.length; i++) {
				move = moves[i];
				// If the SEE value is below 0 or the delta pruning limit, break the search because the rest of the moves are even worse.
				if (move.value < 0 || (nullMoveObservHolds && move.value < alpha - Q_DELTA))
					break;
				pos.makeMove(move);
				searchScore = -quiescence(pos, depth - 1, -beta, -alpha);
				pos.unmakeMove();
				if (searchScore > bestScore) {
					bestScore = searchScore;
					if (bestScore > alpha) {
						alpha = bestScore;
						if (alpha >= beta)
							break;
					}
				}
				if (doBreak)
					break;
			}
		}
		return bestScore;
	}
	/**
	 * Orders material moves and checks, the former of which according to the SEE swap algorithm.
	 * 
	 * @param pos
	 * @param moves
	 * @return
	 */
	private Move[] orderMaterialMovesSEE(Position pos, List<Move> moves) {
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			move.value = Evaluator.SEE(pos, move);	// Static exchange evaluation.
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
	}
	/**
	 * Orders captures and promotions according to the MVV-LVA principle; in case of a promotion, add the standard value of a queen to the score.
	 * 
	 * @param moves
	 * @return
	 */
	private Move[] orderMaterialMovesMVVLVA(List<Move> moves) {
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			if (move.type >= MoveType.PROMOTION_TO_QUEEN.ind) {
				move.value = Material.QUEEN.score;
				if (move.capturedPiece != Piece.NULL.ind)
					move.value += Material.getByPieceInd(move.capturedPiece).score - Material.getByPieceInd(move.movedPiece).score;
			}
			else
				move.value = (short)(Material.getByPieceInd(move.capturedPiece).score - Material.getByPieceInd(move.movedPiece).score);
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
	}
	/**
	 * Orders non-material moves according to the relative history heuristic.
	 * 
	 * @param moves
	 * @return
	 */
	private Move[] orderNonMaterialMoves(List<Move> moves) {
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			move.value = hT.score(move);
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
	}
}
