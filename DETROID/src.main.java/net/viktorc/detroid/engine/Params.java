package net.viktorc.detroid.engine;

import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.Parameter;
import net.viktorc.detroid.framework.tuning.ParameterException;
import net.viktorc.detroid.framework.tuning.ParameterType;

/**
 * The definitions of the evaluation, search, and hash and time management parameters used in the chess engine.
 * 
 * The suffix 'Hth' marks that the number denotes a factor of one given in hundreths and thus should be divided by 100 when used.
 * 
 * @author Viktor
 *
 */
final class Params extends EngineParameters {

	// Piece values.
	@Parameter (binaryLengthLimit = 0)
	short kingValue;
	@Parameter (binaryLengthLimit = 11)
	short queenValue;
	@Parameter (binaryLengthLimit = 10)
	short rookValue;
	@Parameter (binaryLengthLimit = 9)
	short bishopValue;
	@Parameter (binaryLengthLimit = 9)
	short knightValue;
	@Parameter (binaryLengthLimit = 0)
	short pawnValue;

	// Piece phase weights
	@Parameter (binaryLengthLimit = 0)
	byte kingPhaseWeight;
	@Parameter (binaryLengthLimit = 0)
	byte queenPhaseWeight;
	@Parameter (binaryLengthLimit = 0)
	byte rookPhaseWeight;
	@Parameter (binaryLengthLimit = 0)
	byte bishopPhaseWeight;
	@Parameter (binaryLengthLimit = 0)
	byte knightPhaseWeight;
	@Parameter (binaryLengthLimit = 0)
	byte pawnPhaseWeight;

	// Evaluation weights.
	@Parameter
	byte blockedPawnWeight1;
	@Parameter
	byte blockedPawnWeight2;
	@Parameter
	byte blockedPawnWeight3;
	@Parameter
	byte kingFriendlyNormalPawnTropismWeight;
	@Parameter
	byte kingOpponentNormalPawnTropismWeight;
	@Parameter
	byte kingFriendlyOpenBackwardPawnTropismWeight;
	@Parameter
	byte kingOpponentOpenBackwardPawnTropismWeight;
	@Parameter
	byte kingFriendlyPassedPawnTropismWeight;
	@Parameter
	byte kingOpponentPassedPawnTropismWeight;
	@Parameter
	byte passedPawnWeight;
	@Parameter
	byte openBackwardPawnWeight;
	@Parameter
	byte stoppedPawnWeight;
	@Parameter
	byte pinnedQueenWeight;
	@Parameter
	byte pinnedRookWeight;
	@Parameter
	byte pinnedBishopWeight;
	@Parameter
	byte pinnedKnightWeight;
	@Parameter
	byte pinnedPawnWeight;
	@Parameter
	byte rookMobilityWeight;
	@Parameter
	byte bishopMobilityWeight;
	@Parameter
	byte knightMobilityWeight;
	@Parameter
	byte queenKingTropismWeight;
	@Parameter
	byte bishopPairAdvantage;
	@Parameter
	byte tempoAdvantage;

	// Game phase intervals.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	short gamePhaseOpeningLower;
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	short gamePhaseOpeningUpper;
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 8)
	short gamePhaseEndgameLower;
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	short gamePhaseEndgameUpper;

	// The margin for lazy evaluation. The extended score should be very unlikely to differ by more than this amount from the core score.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 11)
	short lazyEvalMar;
	// Search parameters.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
	byte nullMoveReduction; // Null move pruning reduction.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	byte extraNullMoveReduction; // Additional null move pruning reduction.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
	byte extraNullMoveReductionDepthLimit; // The depth limit at which the extra null move reduction is applied.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
	byte lateMoveReduction; // Late move reduction.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 3)
	byte lateMoveReductionMinActivationDepth; // Min. depth for late move reduction.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
	byte minMovesSearchedForLmr; // Min. number of searched moves for late move reduction.
	@Parameter (type = ParameterType.SEARCH_CONTROL)
	boolean doRazor; // Whether razoring should be enabled.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
	short razoringMargin1; // Razoring margin for pre-frontier nodes.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
	short razoringMargin2; // Limited razoring.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
	short futilityMargin1; // Futility margin.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
	short futilityMargin2; // Extended futility margin.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
	short futilityMargin3; // Deep futility margin.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
	short deltaPruningMargin; // The margin for delta-pruning in the quiescence search.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
	short aspirationDelta; // The aspiration delta within iterative deepening.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	byte maxNominalSearchDepth; // The maximum nominal search depth.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	byte fullPly; // For fractional ply extensions.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
	byte checkExt; // Fractional check extension.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
	byte recapExt; // Fractional recapture extension.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
	byte singleReplyExt; // Fractional single reply extension.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	byte iidMinActivationDepth; // The minimum depth at which IID is activated.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	byte iidRelDepthHth; // The portion of the total depth to which the position will be searched with IID.

	// The amount of time the engine waits for the updated result after a search has been cancelled.
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	byte minSearchMilliSecondsForWaiting;
	@Parameter (type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
	int nanoSecondsToWaitForResult;
	
	// The relative history table's value depreciation factor.
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte rhtDecrementFactor;

	// The default hash size in megabytes.
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	short defaultHashSize;
	// The shares of the different hash tables of the total hash size.
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte tTshare;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte eTshare;

	// The number of turns for which the different hash table's entries are retained by default.
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte tTentryLifeCycle;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte eTentryLifeCycle;

	// The values considered when calculating search time extensions.
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	short scoreFluctuationLimit;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte fractionOfOrigSearchTimeSinceLastResultChangeLimitHth;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte resultChangesPerDepthLimit;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
	byte avgMovesPerGame;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 6)
	byte movesToGoSafetyMargin;
	@Parameter (type = ParameterType.ENGINE_MANAGEMENT)
	byte fractionOfTotalTimeToUseHth;

	// Piece-square tables for openings and end games.
	@Parameter
	byte pstPawnOpening8;
	@Parameter
	byte pstPawnOpening9;
	@Parameter
	byte pstPawnOpening10;
	@Parameter
	byte pstPawnOpening11;
	@Parameter
	byte pstPawnOpening12;
	@Parameter
	byte pstPawnOpening13;
	@Parameter
	byte pstPawnOpening14;
	@Parameter
	byte pstPawnOpening15;
	@Parameter
	byte pstPawnOpening16;
	@Parameter
	byte pstPawnOpening17;
	@Parameter
	byte pstPawnOpening18;
	@Parameter
	byte pstPawnOpening19;
	@Parameter
	byte pstPawnOpening20;
	@Parameter
	byte pstPawnOpening21;
	@Parameter
	byte pstPawnOpening22;
	@Parameter
	byte pstPawnOpening23;
	@Parameter
	byte pstPawnOpening24;
	@Parameter
	byte pstPawnOpening25;
	@Parameter
	byte pstPawnOpening26;
	@Parameter
	byte pstPawnOpening27;
	@Parameter
	byte pstPawnOpening28;
	@Parameter
	byte pstPawnOpening29;
	@Parameter
	byte pstPawnOpening30;
	@Parameter
	byte pstPawnOpening31;
	@Parameter
	byte pstPawnOpening32;
	@Parameter
	byte pstPawnOpening33;
	@Parameter
	byte pstPawnOpening34;
	@Parameter
	byte pstPawnOpening35;
	@Parameter
	byte pstPawnOpening36;
	@Parameter
	byte pstPawnOpening37;
	@Parameter
	byte pstPawnOpening38;
	@Parameter
	byte pstPawnOpening39;
	@Parameter
	byte pstPawnOpening40;
	@Parameter
	byte pstPawnOpening41;
	@Parameter
	byte pstPawnOpening42;
	@Parameter
	byte pstPawnOpening43;
	@Parameter
	byte pstPawnOpening44;
	@Parameter
	byte pstPawnOpening45;
	@Parameter
	byte pstPawnOpening46;
	@Parameter
	byte pstPawnOpening47;
	@Parameter
	byte pstPawnOpening48;
	@Parameter
	byte pstPawnOpening49;
	@Parameter
	byte pstPawnOpening50;
	@Parameter
	byte pstPawnOpening51;
	@Parameter
	byte pstPawnOpening52;
	@Parameter
	byte pstPawnOpening53;
	@Parameter
	byte pstPawnOpening54;
	@Parameter
	byte pstPawnOpening55;

	@Parameter
	byte pstPawnEndgame8;
	@Parameter
	byte pstPawnEndgame9;
	@Parameter
	byte pstPawnEndgame10;
	@Parameter
	byte pstPawnEndgame11;
	@Parameter
	byte pstPawnEndgame12;
	@Parameter
	byte pstPawnEndgame13;
	@Parameter
	byte pstPawnEndgame14;
	@Parameter
	byte pstPawnEndgame15;
	@Parameter
	byte pstPawnEndgame16;
	@Parameter
	byte pstPawnEndgame17;
	@Parameter
	byte pstPawnEndgame18;
	@Parameter
	byte pstPawnEndgame19;
	@Parameter
	byte pstPawnEndgame20;
	@Parameter
	byte pstPawnEndgame21;
	@Parameter
	byte pstPawnEndgame22;
	@Parameter
	byte pstPawnEndgame23;
	@Parameter
	byte pstPawnEndgame24;
	@Parameter
	byte pstPawnEndgame25;
	@Parameter
	byte pstPawnEndgame26;
	@Parameter
	byte pstPawnEndgame27;
	@Parameter
	byte pstPawnEndgame28;
	@Parameter
	byte pstPawnEndgame29;
	@Parameter
	byte pstPawnEndgame30;
	@Parameter
	byte pstPawnEndgame31;
	@Parameter
	byte pstPawnEndgame32;
	@Parameter
	byte pstPawnEndgame33;
	@Parameter
	byte pstPawnEndgame34;
	@Parameter
	byte pstPawnEndgame35;
	@Parameter
	byte pstPawnEndgame36;
	@Parameter
	byte pstPawnEndgame37;
	@Parameter
	byte pstPawnEndgame38;
	@Parameter
	byte pstPawnEndgame39;
	@Parameter
	byte pstPawnEndgame40;
	@Parameter
	byte pstPawnEndgame41;
	@Parameter
	byte pstPawnEndgame42;
	@Parameter
	byte pstPawnEndgame43;
	@Parameter
	byte pstPawnEndgame44;
	@Parameter
	byte pstPawnEndgame45;
	@Parameter
	byte pstPawnEndgame46;
	@Parameter
	byte pstPawnEndgame47;
	@Parameter
	byte pstPawnEndgame48;
	@Parameter
	byte pstPawnEndgame49;
	@Parameter
	byte pstPawnEndgame50;
	@Parameter
	byte pstPawnEndgame51;
	@Parameter
	byte pstPawnEndgame52;
	@Parameter
	byte pstPawnEndgame53;
	@Parameter
	byte pstPawnEndgame54;
	@Parameter
	byte pstPawnEndgame55;

	@Parameter
	byte pstKnightOpening0;
	@Parameter
	byte pstKnightOpening1;
	@Parameter
	byte pstKnightOpening2;
	@Parameter
	byte pstKnightOpening3;
	@Parameter
	byte pstKnightOpening8;
	@Parameter
	byte pstKnightOpening9;
	@Parameter
	byte pstKnightOpening10;
	@Parameter
	byte pstKnightOpening11;
	@Parameter
	byte pstKnightOpening16;
	@Parameter
	byte pstKnightOpening17;
	@Parameter
	byte pstKnightOpening18;
	@Parameter
	byte pstKnightOpening19;
	@Parameter
	byte pstKnightOpening24;
	@Parameter
	byte pstKnightOpening25;
	@Parameter
	byte pstKnightOpening26;
	@Parameter
	byte pstKnightOpening27;
	@Parameter
	byte pstKnightOpening32;
	@Parameter
	byte pstKnightOpening33;
	@Parameter
	byte pstKnightOpening34;
	@Parameter
	byte pstKnightOpening35;
	@Parameter
	byte pstKnightOpening40;
	@Parameter
	byte pstKnightOpening41;
	@Parameter
	byte pstKnightOpening42;
	@Parameter
	byte pstKnightOpening43;
	@Parameter
	byte pstKnightOpening48;
	@Parameter
	byte pstKnightOpening49;
	@Parameter
	byte pstKnightOpening50;
	@Parameter
	byte pstKnightOpening51;
	@Parameter
	byte pstKnightOpening56;
	@Parameter
	byte pstKnightOpening57;
	@Parameter
	byte pstKnightOpening58;
	@Parameter
	byte pstKnightOpening59;

	@Parameter
	byte pstKnightEndgame0;
	@Parameter
	byte pstKnightEndgame1;
	@Parameter
	byte pstKnightEndgame2;
	@Parameter
	byte pstKnightEndgame3;
	@Parameter
	byte pstKnightEndgame8;
	@Parameter
	byte pstKnightEndgame9;
	@Parameter
	byte pstKnightEndgame10;
	@Parameter
	byte pstKnightEndgame11;
	@Parameter
	byte pstKnightEndgame16;
	@Parameter
	byte pstKnightEndgame17;
	@Parameter
	byte pstKnightEndgame18;
	@Parameter
	byte pstKnightEndgame19;
	@Parameter
	byte pstKnightEndgame24;
	@Parameter
	byte pstKnightEndgame25;
	@Parameter
	byte pstKnightEndgame26;
	@Parameter
	byte pstKnightEndgame27;
	@Parameter
	byte pstKnightEndgame32;
	@Parameter
	byte pstKnightEndgame33;
	@Parameter
	byte pstKnightEndgame34;
	@Parameter
	byte pstKnightEndgame35;
	@Parameter
	byte pstKnightEndgame40;
	@Parameter
	byte pstKnightEndgame41;
	@Parameter
	byte pstKnightEndgame42;
	@Parameter
	byte pstKnightEndgame43;
	@Parameter
	byte pstKnightEndgame48;
	@Parameter
	byte pstKnightEndgame49;
	@Parameter
	byte pstKnightEndgame50;
	@Parameter
	byte pstKnightEndgame51;
	@Parameter
	byte pstKnightEndgame56;
	@Parameter
	byte pstKnightEndgame57;
	@Parameter
	byte pstKnightEndgame58;
	@Parameter
	byte pstKnightEndgame59;

	@Parameter
	byte pstBishop0;
	@Parameter
	byte pstBishop1;
	@Parameter
	byte pstBishop2;
	@Parameter
	byte pstBishop3;
	@Parameter
	byte pstBishop8;
	@Parameter
	byte pstBishop9;
	@Parameter
	byte pstBishop10;
	@Parameter
	byte pstBishop11;
	@Parameter
	byte pstBishop16;
	@Parameter
	byte pstBishop17;
	@Parameter
	byte pstBishop18;
	@Parameter
	byte pstBishop19;
	@Parameter
	byte pstBishop24;
	@Parameter
	byte pstBishop25;
	@Parameter
	byte pstBishop26;
	@Parameter
	byte pstBishop27;
	@Parameter
	byte pstBishop32;
	@Parameter
	byte pstBishop33;
	@Parameter
	byte pstBishop34;
	@Parameter
	byte pstBishop35;
	@Parameter
	byte pstBishop40;
	@Parameter
	byte pstBishop41;
	@Parameter
	byte pstBishop42;
	@Parameter
	byte pstBishop43;
	@Parameter
	byte pstBishop48;
	@Parameter
	byte pstBishop49;
	@Parameter
	byte pstBishop50;
	@Parameter
	byte pstBishop51;
	@Parameter
	byte pstBishop56;
	@Parameter
	byte pstBishop57;
	@Parameter
	byte pstBishop58;
	@Parameter
	byte pstBishop59;

	@Parameter
	byte pstRookOpening0;
	@Parameter
	byte pstRookOpening1;
	@Parameter
	byte pstRookOpening2;
	@Parameter
	byte pstRookOpening3;
	@Parameter
	byte pstRookOpening4;
	@Parameter
	byte pstRookOpening5;
	@Parameter
	byte pstRookOpening6;
	@Parameter
	byte pstRookOpening7;
	@Parameter
	byte pstRookOpening8;
	@Parameter
	byte pstRookOpening9;
	@Parameter
	byte pstRookOpening10;
	@Parameter
	byte pstRookOpening11;
	@Parameter
	byte pstRookOpening12;
	@Parameter
	byte pstRookOpening13;
	@Parameter
	byte pstRookOpening14;
	@Parameter
	byte pstRookOpening15;
	@Parameter
	byte pstRookOpening16;
	@Parameter
	byte pstRookOpening17;
	@Parameter
	byte pstRookOpening18;
	@Parameter
	byte pstRookOpening19;
	@Parameter
	byte pstRookOpening20;
	@Parameter
	byte pstRookOpening21;
	@Parameter
	byte pstRookOpening22;
	@Parameter
	byte pstRookOpening23;
	@Parameter
	byte pstRookOpening24;
	@Parameter
	byte pstRookOpening25;
	@Parameter
	byte pstRookOpening26;
	@Parameter
	byte pstRookOpening27;
	@Parameter
	byte pstRookOpening28;
	@Parameter
	byte pstRookOpening29;
	@Parameter
	byte pstRookOpening30;
	@Parameter
	byte pstRookOpening31;
	@Parameter
	byte pstRookOpening32;
	@Parameter
	byte pstRookOpening33;
	@Parameter
	byte pstRookOpening34;
	@Parameter
	byte pstRookOpening35;
	@Parameter
	byte pstRookOpening36;
	@Parameter
	byte pstRookOpening37;
	@Parameter
	byte pstRookOpening38;
	@Parameter
	byte pstRookOpening39;
	@Parameter
	byte pstRookOpening40;
	@Parameter
	byte pstRookOpening41;
	@Parameter
	byte pstRookOpening42;
	@Parameter
	byte pstRookOpening43;
	@Parameter
	byte pstRookOpening44;
	@Parameter
	byte pstRookOpening45;
	@Parameter
	byte pstRookOpening46;
	@Parameter
	byte pstRookOpening47;
	@Parameter
	byte pstRookOpening48;
	@Parameter
	byte pstRookOpening49;
	@Parameter
	byte pstRookOpening50;
	@Parameter
	byte pstRookOpening51;
	@Parameter
	byte pstRookOpening52;
	@Parameter
	byte pstRookOpening53;
	@Parameter
	byte pstRookOpening54;
	@Parameter
	byte pstRookOpening55;
	@Parameter
	byte pstRookOpening56;
	@Parameter
	byte pstRookOpening57;
	@Parameter
	byte pstRookOpening58;
	@Parameter
	byte pstRookOpening59;
	@Parameter
	byte pstRookOpening60;
	@Parameter
	byte pstRookOpening61;
	@Parameter
	byte pstRookOpening62;
	@Parameter
	byte pstRookOpening63;

	@Parameter
	byte pstRookEndgame0;
	@Parameter
	byte pstRookEndgame1;
	@Parameter
	byte pstRookEndgame2;
	@Parameter
	byte pstRookEndgame3;
	@Parameter
	byte pstRookEndgame4;
	@Parameter
	byte pstRookEndgame5;
	@Parameter
	byte pstRookEndgame6;
	@Parameter
	byte pstRookEndgame7;
	@Parameter
	byte pstRookEndgame8;
	@Parameter
	byte pstRookEndgame9;
	@Parameter
	byte pstRookEndgame10;
	@Parameter
	byte pstRookEndgame11;
	@Parameter
	byte pstRookEndgame12;
	@Parameter
	byte pstRookEndgame13;
	@Parameter
	byte pstRookEndgame14;
	@Parameter
	byte pstRookEndgame15;
	@Parameter
	byte pstRookEndgame16;
	@Parameter
	byte pstRookEndgame17;
	@Parameter
	byte pstRookEndgame18;
	@Parameter
	byte pstRookEndgame19;
	@Parameter
	byte pstRookEndgame20;
	@Parameter
	byte pstRookEndgame21;
	@Parameter
	byte pstRookEndgame22;
	@Parameter
	byte pstRookEndgame23;
	@Parameter
	byte pstRookEndgame24;
	@Parameter
	byte pstRookEndgame25;
	@Parameter
	byte pstRookEndgame26;
	@Parameter
	byte pstRookEndgame27;
	@Parameter
	byte pstRookEndgame28;
	@Parameter
	byte pstRookEndgame29;
	@Parameter
	byte pstRookEndgame30;
	@Parameter
	byte pstRookEndgame31;
	@Parameter
	byte pstRookEndgame32;
	@Parameter
	byte pstRookEndgame33;
	@Parameter
	byte pstRookEndgame34;
	@Parameter
	byte pstRookEndgame35;
	@Parameter
	byte pstRookEndgame36;
	@Parameter
	byte pstRookEndgame37;
	@Parameter
	byte pstRookEndgame38;
	@Parameter
	byte pstRookEndgame39;
	@Parameter
	byte pstRookEndgame40;
	@Parameter
	byte pstRookEndgame41;
	@Parameter
	byte pstRookEndgame42;
	@Parameter
	byte pstRookEndgame43;
	@Parameter
	byte pstRookEndgame44;
	@Parameter
	byte pstRookEndgame45;
	@Parameter
	byte pstRookEndgame46;
	@Parameter
	byte pstRookEndgame47;
	@Parameter
	byte pstRookEndgame48;
	@Parameter
	byte pstRookEndgame49;
	@Parameter
	byte pstRookEndgame50;
	@Parameter
	byte pstRookEndgame51;
	@Parameter
	byte pstRookEndgame52;
	@Parameter
	byte pstRookEndgame53;
	@Parameter
	byte pstRookEndgame54;
	@Parameter
	byte pstRookEndgame55;
	@Parameter
	byte pstRookEndgame56;
	@Parameter
	byte pstRookEndgame57;
	@Parameter
	byte pstRookEndgame58;
	@Parameter
	byte pstRookEndgame59;
	@Parameter
	byte pstRookEndgame60;
	@Parameter
	byte pstRookEndgame61;
	@Parameter
	byte pstRookEndgame62;
	@Parameter
	byte pstRookEndgame63;

	@Parameter
	byte pstQueen0;
	@Parameter
	byte pstQueen1;
	@Parameter
	byte pstQueen2;
	@Parameter
	byte pstQueen3;
	@Parameter
	byte pstQueen4;
	@Parameter
	byte pstQueen5;
	@Parameter
	byte pstQueen6;
	@Parameter
	byte pstQueen7;
	@Parameter
	byte pstQueen8;
	@Parameter
	byte pstQueen9;
	@Parameter
	byte pstQueen10;
	@Parameter
	byte pstQueen11;
	@Parameter
	byte pstQueen12;
	@Parameter
	byte pstQueen13;
	@Parameter
	byte pstQueen14;
	@Parameter
	byte pstQueen15;
	@Parameter
	byte pstQueen16;
	@Parameter
	byte pstQueen17;
	@Parameter
	byte pstQueen18;
	@Parameter
	byte pstQueen19;
	@Parameter
	byte pstQueen20;
	@Parameter
	byte pstQueen21;
	@Parameter
	byte pstQueen22;
	@Parameter
	byte pstQueen23;
	@Parameter
	byte pstQueen24;
	@Parameter
	byte pstQueen25;
	@Parameter
	byte pstQueen26;
	@Parameter
	byte pstQueen27;
	@Parameter
	byte pstQueen28;
	@Parameter
	byte pstQueen29;
	@Parameter
	byte pstQueen30;
	@Parameter
	byte pstQueen31;
	@Parameter
	byte pstQueen32;
	@Parameter
	byte pstQueen33;
	@Parameter
	byte pstQueen34;
	@Parameter
	byte pstQueen35;
	@Parameter
	byte pstQueen36;
	@Parameter
	byte pstQueen37;
	@Parameter
	byte pstQueen38;
	@Parameter
	byte pstQueen39;
	@Parameter
	byte pstQueen40;
	@Parameter
	byte pstQueen41;
	@Parameter
	byte pstQueen42;
	@Parameter
	byte pstQueen43;
	@Parameter
	byte pstQueen44;
	@Parameter
	byte pstQueen45;
	@Parameter
	byte pstQueen46;
	@Parameter
	byte pstQueen47;
	@Parameter
	byte pstQueen48;
	@Parameter
	byte pstQueen49;
	@Parameter
	byte pstQueen50;
	@Parameter
	byte pstQueen51;
	@Parameter
	byte pstQueen52;
	@Parameter
	byte pstQueen53;
	@Parameter
	byte pstQueen54;
	@Parameter
	byte pstQueen55;
	@Parameter
	byte pstQueen56;
	@Parameter
	byte pstQueen57;
	@Parameter
	byte pstQueen58;
	@Parameter
	byte pstQueen59;
	@Parameter
	byte pstQueen60;
	@Parameter
	byte pstQueen61;
	@Parameter
	byte pstQueen62;
	@Parameter
	byte pstQueen63;

	@Parameter
	byte pstKingOpening0;
	@Parameter
	byte pstKingOpening1;
	@Parameter
	byte pstKingOpening2;
	@Parameter
	byte pstKingOpening3;
	@Parameter
	byte pstKingOpening4;
	@Parameter
	byte pstKingOpening5;
	@Parameter
	byte pstKingOpening6;
	@Parameter
	byte pstKingOpening7;
	@Parameter
	byte pstKingOpening8;
	@Parameter
	byte pstKingOpening9;
	@Parameter
	byte pstKingOpening10;
	@Parameter
	byte pstKingOpening11;
	@Parameter
	byte pstKingOpening12;
	@Parameter
	byte pstKingOpening13;
	@Parameter
	byte pstKingOpening14;
	@Parameter
	byte pstKingOpening15;
	@Parameter
	byte pstKingOpening16;
	@Parameter
	byte pstKingOpening17;
	@Parameter
	byte pstKingOpening18;
	@Parameter
	byte pstKingOpening19;
	@Parameter
	byte pstKingOpening20;
	@Parameter
	byte pstKingOpening21;
	@Parameter
	byte pstKingOpening22;
	@Parameter
	byte pstKingOpening23;
	@Parameter
	byte pstKingOpening24;
	@Parameter
	byte pstKingOpening25;
	@Parameter
	byte pstKingOpening26;
	@Parameter
	byte pstKingOpening27;
	@Parameter
	byte pstKingOpening28;
	@Parameter
	byte pstKingOpening29;
	@Parameter
	byte pstKingOpening30;
	@Parameter
	byte pstKingOpening31;
	@Parameter
	byte pstKingOpening32;
	@Parameter
	byte pstKingOpening33;
	@Parameter
	byte pstKingOpening34;
	@Parameter
	byte pstKingOpening35;
	@Parameter
	byte pstKingOpening36;
	@Parameter
	byte pstKingOpening37;
	@Parameter
	byte pstKingOpening38;
	@Parameter
	byte pstKingOpening39;
	@Parameter
	byte pstKingOpening40;
	@Parameter
	byte pstKingOpening41;
	@Parameter
	byte pstKingOpening42;
	@Parameter
	byte pstKingOpening43;
	@Parameter
	byte pstKingOpening44;
	@Parameter
	byte pstKingOpening45;
	@Parameter
	byte pstKingOpening46;
	@Parameter
	byte pstKingOpening47;
	@Parameter
	byte pstKingOpening48;
	@Parameter
	byte pstKingOpening49;
	@Parameter
	byte pstKingOpening50;
	@Parameter
	byte pstKingOpening51;
	@Parameter
	byte pstKingOpening52;
	@Parameter
	byte pstKingOpening53;
	@Parameter
	byte pstKingOpening54;
	@Parameter
	byte pstKingOpening55;
	@Parameter
	byte pstKingOpening56;
	@Parameter
	byte pstKingOpening57;
	@Parameter
	byte pstKingOpening58;
	@Parameter
	byte pstKingOpening59;
	@Parameter
	byte pstKingOpening60;
	@Parameter
	byte pstKingOpening61;
	@Parameter
	byte pstKingOpening62;
	@Parameter
	byte pstKingOpening63;

	@Parameter
	byte pstKingEndgame0;
	@Parameter
	byte pstKingEndgame1;
	@Parameter
	byte pstKingEndgame2;
	@Parameter
	byte pstKingEndgame3;
	@Parameter
	byte pstKingEndgame4;
	@Parameter
	byte pstKingEndgame5;
	@Parameter
	byte pstKingEndgame6;
	@Parameter
	byte pstKingEndgame7;
	@Parameter
	byte pstKingEndgame8;
	@Parameter
	byte pstKingEndgame9;
	@Parameter
	byte pstKingEndgame10;
	@Parameter
	byte pstKingEndgame11;
	@Parameter
	byte pstKingEndgame12;
	@Parameter
	byte pstKingEndgame13;
	@Parameter
	byte pstKingEndgame14;
	@Parameter
	byte pstKingEndgame15;
	@Parameter
	byte pstKingEndgame16;
	@Parameter
	byte pstKingEndgame17;
	@Parameter
	byte pstKingEndgame18;
	@Parameter
	byte pstKingEndgame19;
	@Parameter
	byte pstKingEndgame20;
	@Parameter
	byte pstKingEndgame21;
	@Parameter
	byte pstKingEndgame22;
	@Parameter
	byte pstKingEndgame23;
	@Parameter
	byte pstKingEndgame24;
	@Parameter
	byte pstKingEndgame25;
	@Parameter
	byte pstKingEndgame26;
	@Parameter
	byte pstKingEndgame27;
	@Parameter
	byte pstKingEndgame28;
	@Parameter
	byte pstKingEndgame29;
	@Parameter
	byte pstKingEndgame30;
	@Parameter
	byte pstKingEndgame31;
	@Parameter
	byte pstKingEndgame32;
	@Parameter
	byte pstKingEndgame33;
	@Parameter
	byte pstKingEndgame34;
	@Parameter
	byte pstKingEndgame35;
	@Parameter
	byte pstKingEndgame36;
	@Parameter
	byte pstKingEndgame37;
	@Parameter
	byte pstKingEndgame38;
	@Parameter
	byte pstKingEndgame39;
	@Parameter
	byte pstKingEndgame40;
	@Parameter
	byte pstKingEndgame41;
	@Parameter
	byte pstKingEndgame42;
	@Parameter
	byte pstKingEndgame43;
	@Parameter
	byte pstKingEndgame44;
	@Parameter
	byte pstKingEndgame45;
	@Parameter
	byte pstKingEndgame46;
	@Parameter
	byte pstKingEndgame47;
	@Parameter
	byte pstKingEndgame48;
	@Parameter
	byte pstKingEndgame49;
	@Parameter
	byte pstKingEndgame50;
	@Parameter
	byte pstKingEndgame51;
	@Parameter
	byte pstKingEndgame52;
	@Parameter
	byte pstKingEndgame53;
	@Parameter
	byte pstKingEndgame54;
	@Parameter
	byte pstKingEndgame55;
	@Parameter
	byte pstKingEndgame56;
	@Parameter
	byte pstKingEndgame57;
	@Parameter
	byte pstKingEndgame58;
	@Parameter
	byte pstKingEndgame59;
	@Parameter
	byte pstKingEndgame60;
	@Parameter
	byte pstKingEndgame61;
	@Parameter
	byte pstKingEndgame62;
	@Parameter
	byte pstKingEndgame63;
	
	/**
	 * Constructs an uninitialized instance.
	 * 
	 * @throws ParameterException If a static or non-primitive field is annotated as {@link #Parameter Parameter}.
	 */
	Params() throws ParameterException {
		super();
	}
	byte[] getPstPawnOpening() {
		return new byte[] {
				0, 0, 0, 0, 0, 0, 0, 0, pstPawnOpening8, pstPawnOpening9,
				pstPawnOpening10, pstPawnOpening11, pstPawnOpening12, pstPawnOpening13, pstPawnOpening14,
				pstPawnOpening15, pstPawnOpening16, pstPawnOpening17, pstPawnOpening18, pstPawnOpening19,
				pstPawnOpening20, pstPawnOpening21, pstPawnOpening22, pstPawnOpening23, pstPawnOpening24,
				pstPawnOpening25, pstPawnOpening26, pstPawnOpening27, pstPawnOpening28, pstPawnOpening29,
				pstPawnOpening30, pstPawnOpening31, pstPawnOpening32, pstPawnOpening33, pstPawnOpening34,
				pstPawnOpening35, pstPawnOpening36, pstPawnOpening37, pstPawnOpening38, pstPawnOpening39,
				pstPawnOpening40, pstPawnOpening41, pstPawnOpening42, pstPawnOpening43, pstPawnOpening44,
				pstPawnOpening45, pstPawnOpening46, pstPawnOpening47, pstPawnOpening48, pstPawnOpening49,
				pstPawnOpening50, pstPawnOpening51, pstPawnOpening52, pstPawnOpening53, pstPawnOpening54,
				pstPawnOpening55, 0, 0, 0, 0, 0, 0, 0, 0 };
	}
	byte[] getPstPawnEndgame() {
		return new byte[] {
				0, 0, 0, 0, 0, 0, 0, 0, pstPawnEndgame8, pstPawnEndgame9,
				pstPawnEndgame10, pstPawnEndgame11, pstPawnEndgame12, pstPawnEndgame13, pstPawnEndgame14,
				pstPawnEndgame15, pstPawnEndgame16, pstPawnEndgame17, pstPawnEndgame18, pstPawnEndgame19,
				pstPawnEndgame20, pstPawnEndgame21, pstPawnEndgame22, pstPawnEndgame23, pstPawnEndgame24,
				pstPawnEndgame25, pstPawnEndgame26, pstPawnEndgame27, pstPawnEndgame28, pstPawnEndgame29,
				pstPawnEndgame30, pstPawnEndgame31, pstPawnEndgame32, pstPawnEndgame33, pstPawnEndgame34,
				pstPawnEndgame35, pstPawnEndgame36, pstPawnEndgame37, pstPawnEndgame38, pstPawnEndgame39,
				pstPawnEndgame40, pstPawnEndgame41, pstPawnEndgame42, pstPawnEndgame43, pstPawnEndgame44,
				pstPawnEndgame45, pstPawnEndgame46, pstPawnEndgame47, pstPawnEndgame48, pstPawnEndgame49,
				pstPawnEndgame50, pstPawnEndgame51, pstPawnEndgame52, pstPawnEndgame53, pstPawnEndgame54,
				pstPawnEndgame55, 0, 0, 0, 0, 0, 0, 0, 0 };
	}
	byte[] getPstKnightOpening() {
		return new byte[] {
				pstKnightOpening0, pstKnightOpening1, pstKnightOpening2, pstKnightOpening3,
				pstKnightOpening3, pstKnightOpening2, pstKnightOpening1, pstKnightOpening0,
				pstKnightOpening8, pstKnightOpening9, pstKnightOpening10, pstKnightOpening11,
				pstKnightOpening11, pstKnightOpening10, pstKnightOpening9, pstKnightOpening8,
				pstKnightOpening16, pstKnightOpening17, pstKnightOpening18, pstKnightOpening19,
				pstKnightOpening19, pstKnightOpening18, pstKnightOpening17, pstKnightOpening16,
				pstKnightOpening24, pstKnightOpening25, pstKnightOpening26, pstKnightOpening27,
				pstKnightOpening27, pstKnightOpening26, pstKnightOpening25, pstKnightOpening24,
				pstKnightOpening32, pstKnightOpening33, pstKnightOpening34, pstKnightOpening35,
				pstKnightOpening35, pstKnightOpening34, pstKnightOpening33, pstKnightOpening32,
				pstKnightOpening40, pstKnightOpening41, pstKnightOpening42, pstKnightOpening43,
				pstKnightOpening43, pstKnightOpening42, pstKnightOpening41, pstKnightOpening40,
				pstKnightOpening48, pstKnightOpening49, pstKnightOpening50, pstKnightOpening51,
				pstKnightOpening51, pstKnightOpening50, pstKnightOpening49, pstKnightOpening48,
				pstKnightOpening56, pstKnightOpening57, pstKnightOpening58, pstKnightOpening59,
				pstKnightOpening59, pstKnightOpening58, pstKnightOpening57, pstKnightOpening56 };
	}
	byte[] getPstKnightEndgame() {
		return new byte[] {
				pstKnightEndgame0, pstKnightEndgame1, pstKnightEndgame2, pstKnightEndgame3,
				pstKnightEndgame3, pstKnightEndgame2, pstKnightEndgame1, pstKnightEndgame0,
				pstKnightEndgame8, pstKnightEndgame9, pstKnightEndgame10, pstKnightEndgame11,
				pstKnightEndgame11, pstKnightEndgame10, pstKnightEndgame9, pstKnightEndgame8,
				pstKnightEndgame16, pstKnightEndgame17, pstKnightEndgame18, pstKnightEndgame19,
				pstKnightEndgame19, pstKnightEndgame18, pstKnightEndgame17, pstKnightEndgame16,
				pstKnightEndgame24, pstKnightEndgame25, pstKnightEndgame26, pstKnightEndgame27,
				pstKnightEndgame27, pstKnightEndgame26, pstKnightEndgame25, pstKnightEndgame24,
				pstKnightEndgame32, pstKnightEndgame33, pstKnightEndgame34, pstKnightEndgame35,
				pstKnightEndgame35, pstKnightEndgame34, pstKnightEndgame33, pstKnightEndgame32,
				pstKnightEndgame40, pstKnightEndgame41, pstKnightEndgame42, pstKnightEndgame43,
				pstKnightEndgame43, pstKnightEndgame42, pstKnightEndgame41, pstKnightEndgame40,
				pstKnightEndgame48, pstKnightEndgame49, pstKnightEndgame50, pstKnightEndgame51,
				pstKnightEndgame51, pstKnightEndgame50, pstKnightEndgame49, pstKnightEndgame48,
				pstKnightEndgame56, pstKnightEndgame57, pstKnightEndgame58, pstKnightEndgame59,
				pstKnightEndgame59, pstKnightEndgame58, pstKnightEndgame57, pstKnightEndgame56 };
	}
	byte[] getPstBishop() {
		return new byte[] {
				pstBishop0, pstBishop1, pstBishop2, pstBishop3,
				pstBishop3, pstBishop2, pstBishop1, pstBishop0,
				pstBishop8, pstBishop9, pstBishop10, pstBishop11,
				pstBishop11, pstBishop10, pstBishop9, pstBishop8,
				pstBishop16, pstBishop17, pstBishop18, pstBishop19,
				pstBishop19, pstBishop18, pstBishop17, pstBishop16,
				pstBishop24, pstBishop25, pstBishop26, pstBishop27,
				pstBishop27, pstBishop26, pstBishop25, pstBishop24,
				pstBishop32, pstBishop33, pstBishop34, pstBishop35,
				pstBishop35, pstBishop34, pstBishop33, pstBishop32,
				pstBishop40, pstBishop41, pstBishop42, pstBishop43,
				pstBishop43, pstBishop42, pstBishop41, pstBishop40,
				pstBishop48, pstBishop49, pstBishop50, pstBishop51,
				pstBishop51, pstBishop50, pstBishop49, pstBishop48,
				pstBishop56, pstBishop57, pstBishop58, pstBishop59,
				pstBishop59, pstBishop58, pstBishop57, pstBishop56 };
	}
	byte[] getPstRookOpening() {
		return new byte[] {
				pstRookOpening0, pstRookOpening1, pstRookOpening2, pstRookOpening3, pstRookOpening4,
				pstRookOpening5, pstRookOpening6, pstRookOpening7, pstRookOpening8, pstRookOpening9,
				pstRookOpening10, pstRookOpening11, pstRookOpening12, pstRookOpening13, pstRookOpening14,
				pstRookOpening15, pstRookOpening16, pstRookOpening17, pstRookOpening18, pstRookOpening19,
				pstRookOpening20, pstRookOpening21, pstRookOpening22, pstRookOpening23, pstRookOpening24,
				pstRookOpening25, pstRookOpening26, pstRookOpening27, pstRookOpening28, pstRookOpening29,
				pstRookOpening30, pstRookOpening31, pstRookOpening32, pstRookOpening33, pstRookOpening34,
				pstRookOpening35, pstRookOpening36, pstRookOpening37, pstRookOpening38, pstRookOpening39,
				pstRookOpening40, pstRookOpening41, pstRookOpening42, pstRookOpening43, pstRookOpening44,
				pstRookOpening45, pstRookOpening46, pstRookOpening47, pstRookOpening48, pstRookOpening49,
				pstRookOpening50, pstRookOpening51, pstRookOpening52, pstRookOpening53, pstRookOpening54,
				pstRookOpening55, pstRookOpening56, pstRookOpening57, pstRookOpening58, pstRookOpening59,
				pstRookOpening60, pstRookOpening61, pstRookOpening62, pstRookOpening63 };
	}
	byte[] getPstRookEndgame() {
		return new byte[] {
				pstRookEndgame0, pstRookEndgame1, pstRookEndgame2, pstRookEndgame3, pstRookEndgame4,
				pstRookEndgame5, pstRookEndgame6, pstRookEndgame7, pstRookEndgame8, pstRookEndgame9,
				pstRookEndgame10, pstRookEndgame11, pstRookEndgame12, pstRookEndgame13, pstRookEndgame14,
				pstRookEndgame15, pstRookEndgame16, pstRookEndgame17, pstRookEndgame18, pstRookEndgame19,
				pstRookEndgame20, pstRookEndgame21, pstRookEndgame22, pstRookEndgame23, pstRookEndgame24,
				pstRookEndgame25, pstRookEndgame26, pstRookEndgame27, pstRookEndgame28, pstRookEndgame29,
				pstRookEndgame30, pstRookEndgame31, pstRookEndgame32, pstRookEndgame33, pstRookEndgame34,
				pstRookEndgame35, pstRookEndgame36, pstRookEndgame37, pstRookEndgame38, pstRookEndgame39,
				pstRookEndgame40, pstRookEndgame41, pstRookEndgame42, pstRookEndgame43, pstRookEndgame44,
				pstRookEndgame45, pstRookEndgame46, pstRookEndgame47, pstRookEndgame48, pstRookEndgame49,
				pstRookEndgame50, pstRookEndgame51, pstRookEndgame52, pstRookEndgame53, pstRookEndgame54,
				pstRookEndgame55, pstRookEndgame56, pstRookEndgame57, pstRookEndgame58, pstRookEndgame59,
				pstRookEndgame60, pstRookEndgame61, pstRookEndgame62, pstRookEndgame63 };
	}
	byte[] getPstQueen() {
		return new byte[] {
				pstQueen0, pstQueen1, pstQueen2, pstQueen3, pstQueen4,
				pstQueen5, pstQueen6, pstQueen7, pstQueen8, pstQueen9,
				pstQueen10, pstQueen11, pstQueen12, pstQueen13, pstQueen14,
				pstQueen15, pstQueen16, pstQueen17, pstQueen18, pstQueen19,
				pstQueen20, pstQueen21, pstQueen22, pstQueen23, pstQueen24,
				pstQueen25, pstQueen26, pstQueen27, pstQueen28, pstQueen29,
				pstQueen30, pstQueen31, pstQueen32, pstQueen33, pstQueen34,
				pstQueen35, pstQueen36, pstQueen37, pstQueen38, pstQueen39,
				pstQueen40, pstQueen41, pstQueen42, pstQueen43, pstQueen44,
				pstQueen45, pstQueen46, pstQueen47, pstQueen48, pstQueen49,
				pstQueen50, pstQueen51, pstQueen52, pstQueen53, pstQueen54,
				pstQueen55, pstQueen56, pstQueen57, pstQueen58, pstQueen59,
				pstQueen60, pstQueen61, pstQueen62, pstQueen63 };
	}
	byte[] getPstKingOpening() {
		return new byte[] {
				pstKingOpening0, pstKingOpening1, pstKingOpening2, pstKingOpening3, pstKingOpening4,
				pstKingOpening5, pstKingOpening6, pstKingOpening7, pstKingOpening8, pstKingOpening9,
				pstKingOpening10, pstKingOpening11, pstKingOpening12, pstKingOpening13, pstKingOpening14,
				pstKingOpening15, pstKingOpening16, pstKingOpening17, pstKingOpening18, pstKingOpening19,
				pstKingOpening20, pstKingOpening21, pstKingOpening22, pstKingOpening23, pstKingOpening24,
				pstKingOpening25, pstKingOpening26, pstKingOpening27, pstKingOpening28, pstKingOpening29,
				pstKingOpening30, pstKingOpening31, pstKingOpening32, pstKingOpening33, pstKingOpening34,
				pstKingOpening35, pstKingOpening36, pstKingOpening37, pstKingOpening38, pstKingOpening39,
				pstKingOpening40, pstKingOpening41, pstKingOpening42, pstKingOpening43, pstKingOpening44,
				pstKingOpening45, pstKingOpening46, pstKingOpening47, pstKingOpening48, pstKingOpening49,
				pstKingOpening50, pstKingOpening51, pstKingOpening52, pstKingOpening53, pstKingOpening54,
				pstKingOpening55, pstKingOpening56, pstKingOpening57, pstKingOpening58, pstKingOpening59,
				pstKingOpening60, pstKingOpening61, pstKingOpening62, pstKingOpening63 };
	}
	byte[] getPstKingEndgame() {
		return new byte[] {
				pstKingEndgame0, pstKingEndgame1, pstKingEndgame2, pstKingEndgame3, pstKingEndgame4,
				pstKingEndgame5, pstKingEndgame6, pstKingEndgame7, pstKingEndgame8, pstKingEndgame9,
				pstKingEndgame10, pstKingEndgame11, pstKingEndgame12, pstKingEndgame13, pstKingEndgame14,
				pstKingEndgame15, pstKingEndgame16, pstKingEndgame17, pstKingEndgame18, pstKingEndgame19,
				pstKingEndgame20, pstKingEndgame21, pstKingEndgame22, pstKingEndgame23, pstKingEndgame24,
				pstKingEndgame25, pstKingEndgame26, pstKingEndgame27, pstKingEndgame28, pstKingEndgame29,
				pstKingEndgame30, pstKingEndgame31, pstKingEndgame32, pstKingEndgame33, pstKingEndgame34,
				pstKingEndgame35, pstKingEndgame36, pstKingEndgame37, pstKingEndgame38, pstKingEndgame39,
				pstKingEndgame40, pstKingEndgame41, pstKingEndgame42, pstKingEndgame43, pstKingEndgame44,
				pstKingEndgame45, pstKingEndgame46, pstKingEndgame47, pstKingEndgame48, pstKingEndgame49,
				pstKingEndgame50, pstKingEndgame51, pstKingEndgame52, pstKingEndgame53, pstKingEndgame54,
				pstKingEndgame55, pstKingEndgame56, pstKingEndgame57, pstKingEndgame58, pstKingEndgame59,
				pstKingEndgame60, pstKingEndgame61, pstKingEndgame62, pstKingEndgame63 };
	}
	
}
