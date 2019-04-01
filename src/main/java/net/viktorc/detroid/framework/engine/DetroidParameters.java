package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.Parameter;
import net.viktorc.detroid.framework.tuning.ParameterException;
import net.viktorc.detroid.framework.tuning.ParameterType;

/**
 * The definitions of the evaluation, search, and hash and time management parameters used in the chess engine.
 *
 * @author Viktor
 */
class DetroidParameters extends EngineParameters {

  // Piece values.
  @Parameter
  short kingValue;
  @Parameter
  short queenValue;
  @Parameter
  short rookValue;
  @Parameter
  short bishopValue;
  @Parameter
  short knightValue;
  @Parameter(binaryLengthLimit = 0)
  short pawnValue;

  // Evaluation weights.
  @Parameter
  byte pawnEndgameBonus;
  @Parameter
  byte blockedPawnWeight1;
  @Parameter
  byte blockedPawnWeight2;
  @Parameter
  byte blockedPawnWeight3;
  @Parameter
  byte passedPawnWeight;
  @Parameter
  byte isolatedPawnWeight;
  @Parameter
  byte backwardPawnWeight;
  @Parameter
  byte stoppedPawnWeight;
  @Parameter
  byte bishopPairAdvantage;
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
  byte queenMobilityWeight;
  @Parameter
  byte rookMobilityWeight;
  @Parameter
  byte bishopMobilityWeight;
  @Parameter
  byte knightMobilityWeight;
  @Parameter
  byte friendlyNormalPawnTropismWeight;
  @Parameter
  byte friendlyWeakPawnTropismWeight;
  @Parameter
  byte friendlyPassedPawnTropismWeight;
  @Parameter
  byte opponentNormalPawnTropismWeight;
  @Parameter
  byte opponentWeakPawnTropismWeight;
  @Parameter
  byte opponentPassedPawnTropismWeight;
  @Parameter
  byte opponentQueenTropismWeight;
  @Parameter
  byte tempoAdvantage;

  // Search parameters.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte nullMoveReduction; // Null move pruning reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte nullMoveReductionMinDepthLeft; // Min. depth for null move reductions.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte extraNullMoveReduction; // Additional, depth dependent null move pruning reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte extraNullMoveReductionDepthLimit; // The depth limit at which the extra null move reduction is applied.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte lateMoveReduction; // Late move reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 3)
  byte lateMoveReductionMinDepthLeft; // Min. depth for late move reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte extraLateMoveReduction; // Additional, depth dependent late move pruning reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte extraLateMoveReductionDepthLimit; // The depth limit at which the extra late move reduction is applied.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte minMovesSearchedForLmr; // Min. number of searched moves for late move reductions.
  @Parameter(type = ParameterType.SEARCH_CONTROL)
  boolean doRazor; // Whether razoring should be enabled.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
  short revFutitilityMargin1; // Reverse futility pruning margin for pre-frontier nodes.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
  short revFutitilityMargin2; // Reverse futility pruning margin for pre-pre-frontier nodes.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 11)
  short revFutitilityMargin3; // Extended reverse futility pruning margin for pre-pre-pre-frontier nodes.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
  short futilityMargin1; // Futility margin.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
  short futilityMargin2; // Extended futility margin.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
  short futilityMargin3; // Deep futility margin.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
  short futilityMargin4; // Deep+ futility margin.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 11)
  short futilityMargin5; // Deep++ futility margin.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
  short deltaPruningMargin; // The margin for delta-pruning in the quiescence search.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 8)
  short aspirationDelta; // The aspiration delta within iterative deepening.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte maxNominalSearchDepth; // The maximum nominal search depth.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte fullPly; // For fractional ply extensions.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte checkExtension; // Fractional check extension.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte recapExtension; // Fractional recapture extension.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte singleReplyExtension; // Fractional single reply extension.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte pawnPushExtension; // Pawn push extension.
  @Parameter(type = ParameterType.SEARCH_CONTROL)
  boolean doIid; // Whether IID should be applied.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte iidMinDepthLeft; // The minimum depth at which IID is activated.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte iidRelDepth64th; // The portion of the total depth to which the position will be searched with IID.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte maxDistFromRootForEgtbProbe; // The maximum allowed depth for any EGTB probe.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte maxDistFromRootForHardEgtbProbe; // The maximum allowed depth for a hard EGTB file probe.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte nodeBusinessCheckMinDepthLeft; // The minimum depth left for rescheduling the search of nodes currently searched by other threads.

  // The shares of the different hash tables of the total hash size.
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
  byte transTableShare;
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 4)
  byte evalTableShare;

  // The number of turns for which the different hash table's entries are retained by default.
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
  byte transTableEntryLifeCycle;
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
  byte evalTableEntryLifeCycle;

  // The values considered when calculating search time extensions.
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 6)
  byte minTimePortionNeededForExtraDepth64th;
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 5)
  byte minMovesToGo;
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 6)
  byte maxMovesToGo;

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
  byte pstBishopOpening0;
  @Parameter
  byte pstBishopOpening1;
  @Parameter
  byte pstBishopOpening2;
  @Parameter
  byte pstBishopOpening3;
  @Parameter
  byte pstBishopOpening8;
  @Parameter
  byte pstBishopOpening9;
  @Parameter
  byte pstBishopOpening10;
  @Parameter
  byte pstBishopOpening11;
  @Parameter
  byte pstBishopOpening16;
  @Parameter
  byte pstBishopOpening17;
  @Parameter
  byte pstBishopOpening18;
  @Parameter
  byte pstBishopOpening19;
  @Parameter
  byte pstBishopOpening24;
  @Parameter
  byte pstBishopOpening25;
  @Parameter
  byte pstBishopOpening26;
  @Parameter
  byte pstBishopOpening27;
  @Parameter
  byte pstBishopOpening32;
  @Parameter
  byte pstBishopOpening33;
  @Parameter
  byte pstBishopOpening34;
  @Parameter
  byte pstBishopOpening35;
  @Parameter
  byte pstBishopOpening40;
  @Parameter
  byte pstBishopOpening41;
  @Parameter
  byte pstBishopOpening42;
  @Parameter
  byte pstBishopOpening43;
  @Parameter
  byte pstBishopOpening48;
  @Parameter
  byte pstBishopOpening49;
  @Parameter
  byte pstBishopOpening50;
  @Parameter
  byte pstBishopOpening51;
  @Parameter
  byte pstBishopOpening56;
  @Parameter
  byte pstBishopOpening57;
  @Parameter
  byte pstBishopOpening58;
  @Parameter
  byte pstBishopOpening59;

  @Parameter
  byte pstBishopEnding0;
  @Parameter
  byte pstBishopEnding1;
  @Parameter
  byte pstBishopEnding2;
  @Parameter
  byte pstBishopEnding3;
  @Parameter
  byte pstBishopEnding8;
  @Parameter
  byte pstBishopEnding9;
  @Parameter
  byte pstBishopEnding10;
  @Parameter
  byte pstBishopEnding11;
  @Parameter
  byte pstBishopEnding16;
  @Parameter
  byte pstBishopEnding17;
  @Parameter
  byte pstBishopEnding18;
  @Parameter
  byte pstBishopEnding19;
  @Parameter
  byte pstBishopEnding24;
  @Parameter
  byte pstBishopEnding25;
  @Parameter
  byte pstBishopEnding26;
  @Parameter
  byte pstBishopEnding27;
  @Parameter
  byte pstBishopEnding32;
  @Parameter
  byte pstBishopEnding33;
  @Parameter
  byte pstBishopEnding34;
  @Parameter
  byte pstBishopEnding35;
  @Parameter
  byte pstBishopEnding40;
  @Parameter
  byte pstBishopEnding41;
  @Parameter
  byte pstBishopEnding42;
  @Parameter
  byte pstBishopEnding43;
  @Parameter
  byte pstBishopEnding48;
  @Parameter
  byte pstBishopEnding49;
  @Parameter
  byte pstBishopEnding50;
  @Parameter
  byte pstBishopEnding51;
  @Parameter
  byte pstBishopEnding56;
  @Parameter
  byte pstBishopEnding57;
  @Parameter
  byte pstBishopEnding58;
  @Parameter
  byte pstBishopEnding59;

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
  byte pstQueenOpening0;
  @Parameter
  byte pstQueenOpening1;
  @Parameter
  byte pstQueenOpening2;
  @Parameter
  byte pstQueenOpening3;
  @Parameter
  byte pstQueenOpening4;
  @Parameter
  byte pstQueenOpening5;
  @Parameter
  byte pstQueenOpening6;
  @Parameter
  byte pstQueenOpening7;
  @Parameter
  byte pstQueenOpening8;
  @Parameter
  byte pstQueenOpening9;
  @Parameter
  byte pstQueenOpening10;
  @Parameter
  byte pstQueenOpening11;
  @Parameter
  byte pstQueenOpening12;
  @Parameter
  byte pstQueenOpening13;
  @Parameter
  byte pstQueenOpening14;
  @Parameter
  byte pstQueenOpening15;
  @Parameter
  byte pstQueenOpening16;
  @Parameter
  byte pstQueenOpening17;
  @Parameter
  byte pstQueenOpening18;
  @Parameter
  byte pstQueenOpening19;
  @Parameter
  byte pstQueenOpening20;
  @Parameter
  byte pstQueenOpening21;
  @Parameter
  byte pstQueenOpening22;
  @Parameter
  byte pstQueenOpening23;
  @Parameter
  byte pstQueenOpening24;
  @Parameter
  byte pstQueenOpening25;
  @Parameter
  byte pstQueenOpening26;
  @Parameter
  byte pstQueenOpening27;
  @Parameter
  byte pstQueenOpening28;
  @Parameter
  byte pstQueenOpening29;
  @Parameter
  byte pstQueenOpening30;
  @Parameter
  byte pstQueenOpening31;
  @Parameter
  byte pstQueenOpening32;
  @Parameter
  byte pstQueenOpening33;
  @Parameter
  byte pstQueenOpening34;
  @Parameter
  byte pstQueenOpening35;
  @Parameter
  byte pstQueenOpening36;
  @Parameter
  byte pstQueenOpening37;
  @Parameter
  byte pstQueenOpening38;
  @Parameter
  byte pstQueenOpening39;
  @Parameter
  byte pstQueenOpening40;
  @Parameter
  byte pstQueenOpening41;
  @Parameter
  byte pstQueenOpening42;
  @Parameter
  byte pstQueenOpening43;
  @Parameter
  byte pstQueenOpening44;
  @Parameter
  byte pstQueenOpening45;
  @Parameter
  byte pstQueenOpening46;
  @Parameter
  byte pstQueenOpening47;
  @Parameter
  byte pstQueenOpening48;
  @Parameter
  byte pstQueenOpening49;
  @Parameter
  byte pstQueenOpening50;
  @Parameter
  byte pstQueenOpening51;
  @Parameter
  byte pstQueenOpening52;
  @Parameter
  byte pstQueenOpening53;
  @Parameter
  byte pstQueenOpening54;
  @Parameter
  byte pstQueenOpening55;
  @Parameter
  byte pstQueenOpening56;
  @Parameter
  byte pstQueenOpening57;
  @Parameter
  byte pstQueenOpening58;
  @Parameter
  byte pstQueenOpening59;
  @Parameter
  byte pstQueenOpening60;
  @Parameter
  byte pstQueenOpening61;
  @Parameter
  byte pstQueenOpening62;
  @Parameter
  byte pstQueenOpening63;

  @Parameter
  byte pstQueenEnding0;
  @Parameter
  byte pstQueenEnding1;
  @Parameter
  byte pstQueenEnding2;
  @Parameter
  byte pstQueenEnding3;
  @Parameter
  byte pstQueenEnding4;
  @Parameter
  byte pstQueenEnding5;
  @Parameter
  byte pstQueenEnding6;
  @Parameter
  byte pstQueenEnding7;
  @Parameter
  byte pstQueenEnding8;
  @Parameter
  byte pstQueenEnding9;
  @Parameter
  byte pstQueenEnding10;
  @Parameter
  byte pstQueenEnding11;
  @Parameter
  byte pstQueenEnding12;
  @Parameter
  byte pstQueenEnding13;
  @Parameter
  byte pstQueenEnding14;
  @Parameter
  byte pstQueenEnding15;
  @Parameter
  byte pstQueenEnding16;
  @Parameter
  byte pstQueenEnding17;
  @Parameter
  byte pstQueenEnding18;
  @Parameter
  byte pstQueenEnding19;
  @Parameter
  byte pstQueenEnding20;
  @Parameter
  byte pstQueenEnding21;
  @Parameter
  byte pstQueenEnding22;
  @Parameter
  byte pstQueenEnding23;
  @Parameter
  byte pstQueenEnding24;
  @Parameter
  byte pstQueenEnding25;
  @Parameter
  byte pstQueenEnding26;
  @Parameter
  byte pstQueenEnding27;
  @Parameter
  byte pstQueenEnding28;
  @Parameter
  byte pstQueenEnding29;
  @Parameter
  byte pstQueenEnding30;
  @Parameter
  byte pstQueenEnding31;
  @Parameter
  byte pstQueenEnding32;
  @Parameter
  byte pstQueenEnding33;
  @Parameter
  byte pstQueenEnding34;
  @Parameter
  byte pstQueenEnding35;
  @Parameter
  byte pstQueenEnding36;
  @Parameter
  byte pstQueenEnding37;
  @Parameter
  byte pstQueenEnding38;
  @Parameter
  byte pstQueenEnding39;
  @Parameter
  byte pstQueenEnding40;
  @Parameter
  byte pstQueenEnding41;
  @Parameter
  byte pstQueenEnding42;
  @Parameter
  byte pstQueenEnding43;
  @Parameter
  byte pstQueenEnding44;
  @Parameter
  byte pstQueenEnding45;
  @Parameter
  byte pstQueenEnding46;
  @Parameter
  byte pstQueenEnding47;
  @Parameter
  byte pstQueenEnding48;
  @Parameter
  byte pstQueenEnding49;
  @Parameter
  byte pstQueenEnding50;
  @Parameter
  byte pstQueenEnding51;
  @Parameter
  byte pstQueenEnding52;
  @Parameter
  byte pstQueenEnding53;
  @Parameter
  byte pstQueenEnding54;
  @Parameter
  byte pstQueenEnding55;
  @Parameter
  byte pstQueenEnding56;
  @Parameter
  byte pstQueenEnding57;
  @Parameter
  byte pstQueenEnding58;
  @Parameter
  byte pstQueenEnding59;
  @Parameter
  byte pstQueenEnding60;
  @Parameter
  byte pstQueenEnding61;
  @Parameter
  byte pstQueenEnding62;
  @Parameter
  byte pstQueenEnding63;

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
   * @throws ParameterException If a static or non-primitive field is annotated as a parameter.
   */
  DetroidParameters() throws ParameterException {
    super();
  }

  byte[] getPstPawnOpening() {
    return new byte[]{
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
        pstPawnOpening55, 0, 0, 0, 0, 0, 0, 0, 0};
  }

  byte[] getPstPawnEndgame() {
    return new byte[]{
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
        pstPawnEndgame55, 0, 0, 0, 0, 0, 0, 0, 0};
  }

  byte[] getPstKnightOpening() {
    return new byte[]{
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
        pstKnightOpening59, pstKnightOpening58, pstKnightOpening57, pstKnightOpening56};
  }

  byte[] getPstKnightEndgame() {
    return new byte[]{
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
        pstKnightEndgame59, pstKnightEndgame58, pstKnightEndgame57, pstKnightEndgame56};
  }

  byte[] getPstBishopOpening() {
    return new byte[]{
        pstBishopOpening0, pstBishopOpening1, pstBishopOpening2, pstBishopOpening3,
        pstBishopOpening3, pstBishopOpening2, pstBishopOpening1, pstBishopOpening0,
        pstBishopOpening8, pstBishopOpening9, pstBishopOpening10, pstBishopOpening11,
        pstBishopOpening11, pstBishopOpening10, pstBishopOpening9, pstBishopOpening8,
        pstBishopOpening16, pstBishopOpening17, pstBishopOpening18, pstBishopOpening19,
        pstBishopOpening19, pstBishopOpening18, pstBishopOpening17, pstBishopOpening16,
        pstBishopOpening24, pstBishopOpening25, pstBishopOpening26, pstBishopOpening27,
        pstBishopOpening27, pstBishopOpening26, pstBishopOpening25, pstBishopOpening24,
        pstBishopOpening32, pstBishopOpening33, pstBishopOpening34, pstBishopOpening35,
        pstBishopOpening35, pstBishopOpening34, pstBishopOpening33, pstBishopOpening32,
        pstBishopOpening40, pstBishopOpening41, pstBishopOpening42, pstBishopOpening43,
        pstBishopOpening43, pstBishopOpening42, pstBishopOpening41, pstBishopOpening40,
        pstBishopOpening48, pstBishopOpening49, pstBishopOpening50, pstBishopOpening51,
        pstBishopOpening51, pstBishopOpening50, pstBishopOpening49, pstBishopOpening48,
        pstBishopOpening56, pstBishopOpening57, pstBishopOpening58, pstBishopOpening59,
        pstBishopOpening59, pstBishopOpening58, pstBishopOpening57, pstBishopOpening56};
  }

  byte[] getPstBishopEnding() {
    return new byte[]{
        pstBishopEnding0, pstBishopEnding1, pstBishopEnding2, pstBishopEnding3,
        pstBishopEnding3, pstBishopEnding2, pstBishopEnding1, pstBishopEnding0,
        pstBishopEnding8, pstBishopEnding9, pstBishopEnding10, pstBishopEnding11,
        pstBishopEnding11, pstBishopEnding10, pstBishopEnding9, pstBishopEnding8,
        pstBishopEnding16, pstBishopEnding17, pstBishopEnding18, pstBishopEnding19,
        pstBishopEnding19, pstBishopEnding18, pstBishopEnding17, pstBishopEnding16,
        pstBishopEnding24, pstBishopEnding25, pstBishopEnding26, pstBishopEnding27,
        pstBishopEnding27, pstBishopEnding26, pstBishopEnding25, pstBishopEnding24,
        pstBishopEnding32, pstBishopEnding33, pstBishopEnding34, pstBishopEnding35,
        pstBishopEnding35, pstBishopEnding34, pstBishopEnding33, pstBishopEnding32,
        pstBishopEnding40, pstBishopEnding41, pstBishopEnding42, pstBishopEnding43,
        pstBishopEnding43, pstBishopEnding42, pstBishopEnding41, pstBishopEnding40,
        pstBishopEnding48, pstBishopEnding49, pstBishopEnding50, pstBishopEnding51,
        pstBishopEnding51, pstBishopEnding50, pstBishopEnding49, pstBishopEnding48,
        pstBishopEnding56, pstBishopEnding57, pstBishopEnding58, pstBishopEnding59,
        pstBishopEnding59, pstBishopEnding58, pstBishopEnding57, pstBishopEnding56};
  }

  byte[] getPstRookOpening() {
    return new byte[]{
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
        pstRookOpening60, pstRookOpening61, pstRookOpening62, pstRookOpening63};
  }

  byte[] getPstRookEndgame() {
    return new byte[]{
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
        pstRookEndgame60, pstRookEndgame61, pstRookEndgame62, pstRookEndgame63};
  }

  byte[] getPstQueenOpening() {
    return new byte[]{
        pstQueenOpening0, pstQueenOpening1, pstQueenOpening2, pstQueenOpening3, pstQueenOpening4,
        pstQueenOpening5, pstQueenOpening6, pstQueenOpening7, pstQueenOpening8, pstQueenOpening9,
        pstQueenOpening10, pstQueenOpening11, pstQueenOpening12, pstQueenOpening13, pstQueenOpening14,
        pstQueenOpening15, pstQueenOpening16, pstQueenOpening17, pstQueenOpening18, pstQueenOpening19,
        pstQueenOpening20, pstQueenOpening21, pstQueenOpening22, pstQueenOpening23, pstQueenOpening24,
        pstQueenOpening25, pstQueenOpening26, pstQueenOpening27, pstQueenOpening28, pstQueenOpening29,
        pstQueenOpening30, pstQueenOpening31, pstQueenOpening32, pstQueenOpening33, pstQueenOpening34,
        pstQueenOpening35, pstQueenOpening36, pstQueenOpening37, pstQueenOpening38, pstQueenOpening39,
        pstQueenOpening40, pstQueenOpening41, pstQueenOpening42, pstQueenOpening43, pstQueenOpening44,
        pstQueenOpening45, pstQueenOpening46, pstQueenOpening47, pstQueenOpening48, pstQueenOpening49,
        pstQueenOpening50, pstQueenOpening51, pstQueenOpening52, pstQueenOpening53, pstQueenOpening54,
        pstQueenOpening55, pstQueenOpening56, pstQueenOpening57, pstQueenOpening58, pstQueenOpening59,
        pstQueenOpening60, pstQueenOpening61, pstQueenOpening62, pstQueenOpening63};
  }

  byte[] getPstQueenEnding() {
    return new byte[]{
        pstQueenEnding0, pstQueenEnding1, pstQueenEnding2, pstQueenEnding3, pstQueenEnding4,
        pstQueenEnding5, pstQueenEnding6, pstQueenEnding7, pstQueenEnding8, pstQueenEnding9,
        pstQueenEnding10, pstQueenEnding11, pstQueenEnding12, pstQueenEnding13, pstQueenEnding14,
        pstQueenEnding15, pstQueenEnding16, pstQueenEnding17, pstQueenEnding18, pstQueenEnding19,
        pstQueenEnding20, pstQueenEnding21, pstQueenEnding22, pstQueenEnding23, pstQueenEnding24,
        pstQueenEnding25, pstQueenEnding26, pstQueenEnding27, pstQueenEnding28, pstQueenEnding29,
        pstQueenEnding30, pstQueenEnding31, pstQueenEnding32, pstQueenEnding33, pstQueenEnding34,
        pstQueenEnding35, pstQueenEnding36, pstQueenEnding37, pstQueenEnding38, pstQueenEnding39,
        pstQueenEnding40, pstQueenEnding41, pstQueenEnding42, pstQueenEnding43, pstQueenEnding44,
        pstQueenEnding45, pstQueenEnding46, pstQueenEnding47, pstQueenEnding48, pstQueenEnding49,
        pstQueenEnding50, pstQueenEnding51, pstQueenEnding52, pstQueenEnding53, pstQueenEnding54,
        pstQueenEnding55, pstQueenEnding56, pstQueenEnding57, pstQueenEnding58, pstQueenEnding59,
        pstQueenEnding60, pstQueenEnding61, pstQueenEnding62, pstQueenEnding63};
  }

  byte[] getPstKingOpening() {
    return new byte[]{
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
        pstKingOpening60, pstKingOpening61, pstKingOpening62, pstKingOpening63};
  }

  byte[] getPstKingEndgame() {
    return new byte[]{
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
        pstKingEndgame60, pstKingEndgame61, pstKingEndgame62, pstKingEndgame63};
  }

}
