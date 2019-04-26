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

  // Engine management parameters.
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 4)
  byte transTableShare16th; // The share of the transposition table of the total hash size.
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
  byte transTableEntryLifeCycle; // The number of turns for which the different hash table's entries are retained by default.
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 0)
  byte evalTableEntryLifeCycle;

  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 4)
  byte minTimePortionNeededForExtraDepth16th;
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 5)
  byte minMovesToGo;
  @Parameter(type = ParameterType.ENGINE_MANAGEMENT, binaryLengthLimit = 6)
  byte maxMovesToGo;

  // Search parameters.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte nullMoveReduction; // Null move pruning reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte nullMoveReductionMinDepthLeft; // Min. depth for null move reductions.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte extraNullMoveReduction; // Additional, depth dependent null move pruning reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte extraNullMoveReductionDepthLimit; // The depth limit at which the extra null move reduction is applied.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte lateMoveReduction; // Late move reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 3)
  byte lateMoveReductionMinDepthLeft; // Min. depth for late move reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 2)
  byte extraLateMoveReduction; // Additional, depth dependent late move pruning reduction.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte extraLateMoveReductionDepthLimit; // The depth limit at which the extra late move reduction is applied.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte minMovesSearchedForLmr; // Min. number of searched moves for late move reductions.
  @Parameter(type = ParameterType.SEARCH_CONTROL)
  boolean doRazor; // Whether razoring should be enabled.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 9)
  short revFutilityMargin1; // Reverse futility pruning margin for pre-frontier nodes.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 10)
  short revFutilityMargin2; // Reverse futility pruning margin for pre-pre-frontier nodes.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 11)
  short revFutilityMargin3; // Extended reverse futility pruning margin for pre-pre-pre-frontier nodes.
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
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte iidMinDepthLeft; // The minimum depth at which IID is activated.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 4)
  byte iidRelDepth16th; // The portion of the total depth to which the position will be searched with IID.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte maxDistFromRootForEgtbProbe; // The maximum allowed depth for any EGTB probe.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte maxDistFromRootForHardEgtbProbe; // The maximum allowed depth for a hard EGTB file probe.
  @Parameter(type = ParameterType.SEARCH_CONTROL, binaryLengthLimit = 0)
  byte nodeBusinessCheckMinDepthLeft; // The minimum depth left for rescheduling the search of nodes currently searched by other threads.

  // Evaluation parameters.
  @Parameter
  short queenValue;
  @Parameter
  short rookValue;
  @Parameter
  short bishopValue;
  @Parameter
  short knightValue;
  @Parameter
  short pawnValue;
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

  @Parameter
  byte pstPawnMg8;
  @Parameter
  byte pstPawnMg9;
  @Parameter
  byte pstPawnMg10;
  @Parameter
  byte pstPawnMg11;
  @Parameter
  byte pstPawnMg12;
  @Parameter
  byte pstPawnMg13;
  @Parameter
  byte pstPawnMg14;
  @Parameter
  byte pstPawnMg15;
  @Parameter
  byte pstPawnMg16;
  @Parameter
  byte pstPawnMg17;
  @Parameter
  byte pstPawnMg18;
  @Parameter
  byte pstPawnMg19;
  @Parameter
  byte pstPawnMg20;
  @Parameter
  byte pstPawnMg21;
  @Parameter
  byte pstPawnMg22;
  @Parameter
  byte pstPawnMg23;
  @Parameter
  byte pstPawnMg24;
  @Parameter
  byte pstPawnMg25;
  @Parameter
  byte pstPawnMg26;
  @Parameter
  byte pstPawnMg27;
  @Parameter
  byte pstPawnMg28;
  @Parameter
  byte pstPawnMg29;
  @Parameter
  byte pstPawnMg30;
  @Parameter
  byte pstPawnMg31;
  @Parameter
  byte pstPawnMg32;
  @Parameter
  byte pstPawnMg33;
  @Parameter
  byte pstPawnMg34;
  @Parameter
  byte pstPawnMg35;
  @Parameter
  byte pstPawnMg36;
  @Parameter
  byte pstPawnMg37;
  @Parameter
  byte pstPawnMg38;
  @Parameter
  byte pstPawnMg39;
  @Parameter
  byte pstPawnMg40;
  @Parameter
  byte pstPawnMg41;
  @Parameter
  byte pstPawnMg42;
  @Parameter
  byte pstPawnMg43;
  @Parameter
  byte pstPawnMg44;
  @Parameter
  byte pstPawnMg45;
  @Parameter
  byte pstPawnMg46;
  @Parameter
  byte pstPawnMg47;
  @Parameter
  byte pstPawnMg48;
  @Parameter
  byte pstPawnMg49;
  @Parameter
  byte pstPawnMg50;
  @Parameter
  byte pstPawnMg51;
  @Parameter
  byte pstPawnMg52;
  @Parameter
  byte pstPawnMg53;
  @Parameter
  byte pstPawnMg54;
  @Parameter
  byte pstPawnMg55;

  @Parameter
  byte pstPawnEg8;
  @Parameter
  byte pstPawnEg9;
  @Parameter
  byte pstPawnEg10;
  @Parameter
  byte pstPawnEg11;
  @Parameter
  byte pstPawnEg16;
  @Parameter
  byte pstPawnEg17;
  @Parameter
  byte pstPawnEg18;
  @Parameter
  byte pstPawnEg19;
  @Parameter
  byte pstPawnEg24;
  @Parameter
  byte pstPawnEg25;
  @Parameter
  byte pstPawnEg26;
  @Parameter
  byte pstPawnEg27;
  @Parameter
  byte pstPawnEg32;
  @Parameter
  byte pstPawnEg33;
  @Parameter
  byte pstPawnEg34;
  @Parameter
  byte pstPawnEg35;
  @Parameter
  byte pstPawnEg40;
  @Parameter
  byte pstPawnEg41;
  @Parameter
  byte pstPawnEg42;
  @Parameter
  byte pstPawnEg43;
  @Parameter
  byte pstPawnEg48;
  @Parameter
  byte pstPawnEg49;
  @Parameter
  byte pstPawnEg50;
  @Parameter
  byte pstPawnEg51;

  @Parameter
  byte pstKnightMg0;
  @Parameter
  byte pstKnightMg1;
  @Parameter
  byte pstKnightMg2;
  @Parameter
  byte pstKnightMg3;
  @Parameter
  byte pstKnightMg4;
  @Parameter
  byte pstKnightMg5;
  @Parameter
  byte pstKnightMg6;
  @Parameter
  byte pstKnightMg7;
  @Parameter
  byte pstKnightMg8;
  @Parameter
  byte pstKnightMg9;
  @Parameter
  byte pstKnightMg10;
  @Parameter
  byte pstKnightMg11;
  @Parameter
  byte pstKnightMg12;
  @Parameter
  byte pstKnightMg13;
  @Parameter
  byte pstKnightMg14;
  @Parameter
  byte pstKnightMg15;
  @Parameter
  byte pstKnightMg16;
  @Parameter
  byte pstKnightMg17;
  @Parameter
  byte pstKnightMg18;
  @Parameter
  byte pstKnightMg19;
  @Parameter
  byte pstKnightMg20;
  @Parameter
  byte pstKnightMg21;
  @Parameter
  byte pstKnightMg22;
  @Parameter
  byte pstKnightMg23;
  @Parameter
  byte pstKnightMg24;
  @Parameter
  byte pstKnightMg25;
  @Parameter
  byte pstKnightMg26;
  @Parameter
  byte pstKnightMg27;
  @Parameter
  byte pstKnightMg28;
  @Parameter
  byte pstKnightMg29;
  @Parameter
  byte pstKnightMg30;
  @Parameter
  byte pstKnightMg31;
  @Parameter
  byte pstKnightMg32;
  @Parameter
  byte pstKnightMg33;
  @Parameter
  byte pstKnightMg34;
  @Parameter
  byte pstKnightMg35;
  @Parameter
  byte pstKnightMg36;
  @Parameter
  byte pstKnightMg37;
  @Parameter
  byte pstKnightMg38;
  @Parameter
  byte pstKnightMg39;
  @Parameter
  byte pstKnightMg40;
  @Parameter
  byte pstKnightMg41;
  @Parameter
  byte pstKnightMg42;
  @Parameter
  byte pstKnightMg43;
  @Parameter
  byte pstKnightMg44;
  @Parameter
  byte pstKnightMg45;
  @Parameter
  byte pstKnightMg46;
  @Parameter
  byte pstKnightMg47;
  @Parameter
  byte pstKnightMg48;
  @Parameter
  byte pstKnightMg49;
  @Parameter
  byte pstKnightMg50;
  @Parameter
  byte pstKnightMg51;
  @Parameter
  byte pstKnightMg52;
  @Parameter
  byte pstKnightMg53;
  @Parameter
  byte pstKnightMg54;
  @Parameter
  byte pstKnightMg55;
  @Parameter
  byte pstKnightMg56;
  @Parameter
  byte pstKnightMg57;
  @Parameter
  byte pstKnightMg58;
  @Parameter
  byte pstKnightMg59;
  @Parameter
  byte pstKnightMg60;
  @Parameter
  byte pstKnightMg61;
  @Parameter
  byte pstKnightMg62;
  @Parameter
  byte pstKnightMg63;

  @Parameter
  byte pstKnightEg0;
  @Parameter
  byte pstKnightEg1;
  @Parameter
  byte pstKnightEg2;
  @Parameter
  byte pstKnightEg3;
  @Parameter
  byte pstKnightEg4;
  @Parameter
  byte pstKnightEg5;
  @Parameter
  byte pstKnightEg6;
  @Parameter
  byte pstKnightEg7;
  @Parameter
  byte pstKnightEg8;
  @Parameter
  byte pstKnightEg9;
  @Parameter
  byte pstKnightEg10;
  @Parameter
  byte pstKnightEg11;
  @Parameter
  byte pstKnightEg12;
  @Parameter
  byte pstKnightEg13;
  @Parameter
  byte pstKnightEg14;
  @Parameter
  byte pstKnightEg15;
  @Parameter
  byte pstKnightEg16;
  @Parameter
  byte pstKnightEg17;
  @Parameter
  byte pstKnightEg18;
  @Parameter
  byte pstKnightEg19;
  @Parameter
  byte pstKnightEg20;
  @Parameter
  byte pstKnightEg21;
  @Parameter
  byte pstKnightEg22;
  @Parameter
  byte pstKnightEg23;
  @Parameter
  byte pstKnightEg24;
  @Parameter
  byte pstKnightEg25;
  @Parameter
  byte pstKnightEg26;
  @Parameter
  byte pstKnightEg27;
  @Parameter
  byte pstKnightEg28;
  @Parameter
  byte pstKnightEg29;
  @Parameter
  byte pstKnightEg30;
  @Parameter
  byte pstKnightEg31;
  @Parameter
  byte pstKnightEg32;
  @Parameter
  byte pstKnightEg33;
  @Parameter
  byte pstKnightEg34;
  @Parameter
  byte pstKnightEg35;
  @Parameter
  byte pstKnightEg36;
  @Parameter
  byte pstKnightEg37;
  @Parameter
  byte pstKnightEg38;
  @Parameter
  byte pstKnightEg39;
  @Parameter
  byte pstKnightEg40;
  @Parameter
  byte pstKnightEg41;
  @Parameter
  byte pstKnightEg42;
  @Parameter
  byte pstKnightEg43;
  @Parameter
  byte pstKnightEg44;
  @Parameter
  byte pstKnightEg45;
  @Parameter
  byte pstKnightEg46;
  @Parameter
  byte pstKnightEg47;
  @Parameter
  byte pstKnightEg48;
  @Parameter
  byte pstKnightEg49;
  @Parameter
  byte pstKnightEg50;
  @Parameter
  byte pstKnightEg51;
  @Parameter
  byte pstKnightEg52;
  @Parameter
  byte pstKnightEg53;
  @Parameter
  byte pstKnightEg54;
  @Parameter
  byte pstKnightEg55;
  @Parameter
  byte pstKnightEg56;
  @Parameter
  byte pstKnightEg57;
  @Parameter
  byte pstKnightEg58;
  @Parameter
  byte pstKnightEg59;
  @Parameter
  byte pstKnightEg60;
  @Parameter
  byte pstKnightEg61;
  @Parameter
  byte pstKnightEg62;
  @Parameter
  byte pstKnightEg63;

  @Parameter
  byte pstBishopMg0;
  @Parameter
  byte pstBishopMg1;
  @Parameter
  byte pstBishopMg2;
  @Parameter
  byte pstBishopMg3;
  @Parameter
  byte pstBishopMg4;
  @Parameter
  byte pstBishopMg5;
  @Parameter
  byte pstBishopMg6;
  @Parameter
  byte pstBishopMg7;
  @Parameter
  byte pstBishopMg8;
  @Parameter
  byte pstBishopMg9;
  @Parameter
  byte pstBishopMg10;
  @Parameter
  byte pstBishopMg11;
  @Parameter
  byte pstBishopMg12;
  @Parameter
  byte pstBishopMg13;
  @Parameter
  byte pstBishopMg14;
  @Parameter
  byte pstBishopMg15;
  @Parameter
  byte pstBishopMg16;
  @Parameter
  byte pstBishopMg17;
  @Parameter
  byte pstBishopMg18;
  @Parameter
  byte pstBishopMg19;
  @Parameter
  byte pstBishopMg20;
  @Parameter
  byte pstBishopMg21;
  @Parameter
  byte pstBishopMg22;
  @Parameter
  byte pstBishopMg23;
  @Parameter
  byte pstBishopMg24;
  @Parameter
  byte pstBishopMg25;
  @Parameter
  byte pstBishopMg26;
  @Parameter
  byte pstBishopMg27;
  @Parameter
  byte pstBishopMg28;
  @Parameter
  byte pstBishopMg29;
  @Parameter
  byte pstBishopMg30;
  @Parameter
  byte pstBishopMg31;
  @Parameter
  byte pstBishopMg32;
  @Parameter
  byte pstBishopMg33;
  @Parameter
  byte pstBishopMg34;
  @Parameter
  byte pstBishopMg35;
  @Parameter
  byte pstBishopMg36;
  @Parameter
  byte pstBishopMg37;
  @Parameter
  byte pstBishopMg38;
  @Parameter
  byte pstBishopMg39;
  @Parameter
  byte pstBishopMg40;
  @Parameter
  byte pstBishopMg41;
  @Parameter
  byte pstBishopMg42;
  @Parameter
  byte pstBishopMg43;
  @Parameter
  byte pstBishopMg44;
  @Parameter
  byte pstBishopMg45;
  @Parameter
  byte pstBishopMg46;
  @Parameter
  byte pstBishopMg47;
  @Parameter
  byte pstBishopMg48;
  @Parameter
  byte pstBishopMg49;
  @Parameter
  byte pstBishopMg50;
  @Parameter
  byte pstBishopMg51;
  @Parameter
  byte pstBishopMg52;
  @Parameter
  byte pstBishopMg53;
  @Parameter
  byte pstBishopMg54;
  @Parameter
  byte pstBishopMg55;
  @Parameter
  byte pstBishopMg56;
  @Parameter
  byte pstBishopMg57;
  @Parameter
  byte pstBishopMg58;
  @Parameter
  byte pstBishopMg59;
  @Parameter
  byte pstBishopMg60;
  @Parameter
  byte pstBishopMg61;
  @Parameter
  byte pstBishopMg62;
  @Parameter
  byte pstBishopMg63;

  @Parameter
  byte pstBishopEg0;
  @Parameter
  byte pstBishopEg1;
  @Parameter
  byte pstBishopEg2;
  @Parameter
  byte pstBishopEg3;
  @Parameter
  byte pstBishopEg4;
  @Parameter
  byte pstBishopEg5;
  @Parameter
  byte pstBishopEg6;
  @Parameter
  byte pstBishopEg7;
  @Parameter
  byte pstBishopEg8;
  @Parameter
  byte pstBishopEg9;
  @Parameter
  byte pstBishopEg10;
  @Parameter
  byte pstBishopEg11;
  @Parameter
  byte pstBishopEg12;
  @Parameter
  byte pstBishopEg13;
  @Parameter
  byte pstBishopEg14;
  @Parameter
  byte pstBishopEg15;
  @Parameter
  byte pstBishopEg16;
  @Parameter
  byte pstBishopEg17;
  @Parameter
  byte pstBishopEg18;
  @Parameter
  byte pstBishopEg19;
  @Parameter
  byte pstBishopEg20;
  @Parameter
  byte pstBishopEg21;
  @Parameter
  byte pstBishopEg22;
  @Parameter
  byte pstBishopEg23;
  @Parameter
  byte pstBishopEg24;
  @Parameter
  byte pstBishopEg25;
  @Parameter
  byte pstBishopEg26;
  @Parameter
  byte pstBishopEg27;
  @Parameter
  byte pstBishopEg28;
  @Parameter
  byte pstBishopEg29;
  @Parameter
  byte pstBishopEg30;
  @Parameter
  byte pstBishopEg31;
  @Parameter
  byte pstBishopEg32;
  @Parameter
  byte pstBishopEg33;
  @Parameter
  byte pstBishopEg34;
  @Parameter
  byte pstBishopEg35;
  @Parameter
  byte pstBishopEg36;
  @Parameter
  byte pstBishopEg37;
  @Parameter
  byte pstBishopEg38;
  @Parameter
  byte pstBishopEg39;
  @Parameter
  byte pstBishopEg40;
  @Parameter
  byte pstBishopEg41;
  @Parameter
  byte pstBishopEg42;
  @Parameter
  byte pstBishopEg43;
  @Parameter
  byte pstBishopEg44;
  @Parameter
  byte pstBishopEg45;
  @Parameter
  byte pstBishopEg46;
  @Parameter
  byte pstBishopEg47;
  @Parameter
  byte pstBishopEg48;
  @Parameter
  byte pstBishopEg49;
  @Parameter
  byte pstBishopEg50;
  @Parameter
  byte pstBishopEg51;
  @Parameter
  byte pstBishopEg52;
  @Parameter
  byte pstBishopEg53;
  @Parameter
  byte pstBishopEg54;
  @Parameter
  byte pstBishopEg55;
  @Parameter
  byte pstBishopEg56;
  @Parameter
  byte pstBishopEg57;
  @Parameter
  byte pstBishopEg58;
  @Parameter
  byte pstBishopEg59;
  @Parameter
  byte pstBishopEg60;
  @Parameter
  byte pstBishopEg61;
  @Parameter
  byte pstBishopEg62;
  @Parameter
  byte pstBishopEg63;

  @Parameter
  byte pstRookMg0;
  @Parameter
  byte pstRookMg1;
  @Parameter
  byte pstRookMg2;
  @Parameter
  byte pstRookMg3;
  @Parameter
  byte pstRookMg4;
  @Parameter
  byte pstRookMg5;
  @Parameter
  byte pstRookMg6;
  @Parameter
  byte pstRookMg7;
  @Parameter
  byte pstRookMg8;
  @Parameter
  byte pstRookMg9;
  @Parameter
  byte pstRookMg10;
  @Parameter
  byte pstRookMg11;
  @Parameter
  byte pstRookMg12;
  @Parameter
  byte pstRookMg13;
  @Parameter
  byte pstRookMg14;
  @Parameter
  byte pstRookMg15;
  @Parameter
  byte pstRookMg16;
  @Parameter
  byte pstRookMg17;
  @Parameter
  byte pstRookMg18;
  @Parameter
  byte pstRookMg19;
  @Parameter
  byte pstRookMg20;
  @Parameter
  byte pstRookMg21;
  @Parameter
  byte pstRookMg22;
  @Parameter
  byte pstRookMg23;
  @Parameter
  byte pstRookMg24;
  @Parameter
  byte pstRookMg25;
  @Parameter
  byte pstRookMg26;
  @Parameter
  byte pstRookMg27;
  @Parameter
  byte pstRookMg28;
  @Parameter
  byte pstRookMg29;
  @Parameter
  byte pstRookMg30;
  @Parameter
  byte pstRookMg31;
  @Parameter
  byte pstRookMg32;
  @Parameter
  byte pstRookMg33;
  @Parameter
  byte pstRookMg34;
  @Parameter
  byte pstRookMg35;
  @Parameter
  byte pstRookMg36;
  @Parameter
  byte pstRookMg37;
  @Parameter
  byte pstRookMg38;
  @Parameter
  byte pstRookMg39;
  @Parameter
  byte pstRookMg40;
  @Parameter
  byte pstRookMg41;
  @Parameter
  byte pstRookMg42;
  @Parameter
  byte pstRookMg43;
  @Parameter
  byte pstRookMg44;
  @Parameter
  byte pstRookMg45;
  @Parameter
  byte pstRookMg46;
  @Parameter
  byte pstRookMg47;
  @Parameter
  byte pstRookMg48;
  @Parameter
  byte pstRookMg49;
  @Parameter
  byte pstRookMg50;
  @Parameter
  byte pstRookMg51;
  @Parameter
  byte pstRookMg52;
  @Parameter
  byte pstRookMg53;
  @Parameter
  byte pstRookMg54;
  @Parameter
  byte pstRookMg55;
  @Parameter
  byte pstRookMg56;
  @Parameter
  byte pstRookMg57;
  @Parameter
  byte pstRookMg58;
  @Parameter
  byte pstRookMg59;
  @Parameter
  byte pstRookMg60;
  @Parameter
  byte pstRookMg61;
  @Parameter
  byte pstRookMg62;
  @Parameter
  byte pstRookMg63;

  @Parameter
  byte pstRookEg0;
  @Parameter
  byte pstRookEg1;
  @Parameter
  byte pstRookEg2;
  @Parameter
  byte pstRookEg3;
  @Parameter
  byte pstRookEg4;
  @Parameter
  byte pstRookEg5;
  @Parameter
  byte pstRookEg6;
  @Parameter
  byte pstRookEg7;
  @Parameter
  byte pstRookEg8;
  @Parameter
  byte pstRookEg9;
  @Parameter
  byte pstRookEg10;
  @Parameter
  byte pstRookEg11;
  @Parameter
  byte pstRookEg12;
  @Parameter
  byte pstRookEg13;
  @Parameter
  byte pstRookEg14;
  @Parameter
  byte pstRookEg15;
  @Parameter
  byte pstRookEg16;
  @Parameter
  byte pstRookEg17;
  @Parameter
  byte pstRookEg18;
  @Parameter
  byte pstRookEg19;
  @Parameter
  byte pstRookEg20;
  @Parameter
  byte pstRookEg21;
  @Parameter
  byte pstRookEg22;
  @Parameter
  byte pstRookEg23;
  @Parameter
  byte pstRookEg24;
  @Parameter
  byte pstRookEg25;
  @Parameter
  byte pstRookEg26;
  @Parameter
  byte pstRookEg27;
  @Parameter
  byte pstRookEg28;
  @Parameter
  byte pstRookEg29;
  @Parameter
  byte pstRookEg30;
  @Parameter
  byte pstRookEg31;
  @Parameter
  byte pstRookEg32;
  @Parameter
  byte pstRookEg33;
  @Parameter
  byte pstRookEg34;
  @Parameter
  byte pstRookEg35;
  @Parameter
  byte pstRookEg36;
  @Parameter
  byte pstRookEg37;
  @Parameter
  byte pstRookEg38;
  @Parameter
  byte pstRookEg39;
  @Parameter
  byte pstRookEg40;
  @Parameter
  byte pstRookEg41;
  @Parameter
  byte pstRookEg42;
  @Parameter
  byte pstRookEg43;
  @Parameter
  byte pstRookEg44;
  @Parameter
  byte pstRookEg45;
  @Parameter
  byte pstRookEg46;
  @Parameter
  byte pstRookEg47;
  @Parameter
  byte pstRookEg48;
  @Parameter
  byte pstRookEg49;
  @Parameter
  byte pstRookEg50;
  @Parameter
  byte pstRookEg51;
  @Parameter
  byte pstRookEg52;
  @Parameter
  byte pstRookEg53;
  @Parameter
  byte pstRookEg54;
  @Parameter
  byte pstRookEg55;
  @Parameter
  byte pstRookEg56;
  @Parameter
  byte pstRookEg57;
  @Parameter
  byte pstRookEg58;
  @Parameter
  byte pstRookEg59;
  @Parameter
  byte pstRookEg60;
  @Parameter
  byte pstRookEg61;
  @Parameter
  byte pstRookEg62;
  @Parameter
  byte pstRookEg63;

  @Parameter
  byte pstQueenMg0;
  @Parameter
  byte pstQueenMg1;
  @Parameter
  byte pstQueenMg2;
  @Parameter
  byte pstQueenMg3;
  @Parameter
  byte pstQueenMg4;
  @Parameter
  byte pstQueenMg5;
  @Parameter
  byte pstQueenMg6;
  @Parameter
  byte pstQueenMg7;
  @Parameter
  byte pstQueenMg8;
  @Parameter
  byte pstQueenMg9;
  @Parameter
  byte pstQueenMg10;
  @Parameter
  byte pstQueenMg11;
  @Parameter
  byte pstQueenMg12;
  @Parameter
  byte pstQueenMg13;
  @Parameter
  byte pstQueenMg14;
  @Parameter
  byte pstQueenMg15;
  @Parameter
  byte pstQueenMg16;
  @Parameter
  byte pstQueenMg17;
  @Parameter
  byte pstQueenMg18;
  @Parameter
  byte pstQueenMg19;
  @Parameter
  byte pstQueenMg20;
  @Parameter
  byte pstQueenMg21;
  @Parameter
  byte pstQueenMg22;
  @Parameter
  byte pstQueenMg23;
  @Parameter
  byte pstQueenMg24;
  @Parameter
  byte pstQueenMg25;
  @Parameter
  byte pstQueenMg26;
  @Parameter
  byte pstQueenMg27;
  @Parameter
  byte pstQueenMg28;
  @Parameter
  byte pstQueenMg29;
  @Parameter
  byte pstQueenMg30;
  @Parameter
  byte pstQueenMg31;
  @Parameter
  byte pstQueenMg32;
  @Parameter
  byte pstQueenMg33;
  @Parameter
  byte pstQueenMg34;
  @Parameter
  byte pstQueenMg35;
  @Parameter
  byte pstQueenMg36;
  @Parameter
  byte pstQueenMg37;
  @Parameter
  byte pstQueenMg38;
  @Parameter
  byte pstQueenMg39;
  @Parameter
  byte pstQueenMg40;
  @Parameter
  byte pstQueenMg41;
  @Parameter
  byte pstQueenMg42;
  @Parameter
  byte pstQueenMg43;
  @Parameter
  byte pstQueenMg44;
  @Parameter
  byte pstQueenMg45;
  @Parameter
  byte pstQueenMg46;
  @Parameter
  byte pstQueenMg47;
  @Parameter
  byte pstQueenMg48;
  @Parameter
  byte pstQueenMg49;
  @Parameter
  byte pstQueenMg50;
  @Parameter
  byte pstQueenMg51;
  @Parameter
  byte pstQueenMg52;
  @Parameter
  byte pstQueenMg53;
  @Parameter
  byte pstQueenMg54;
  @Parameter
  byte pstQueenMg55;
  @Parameter
  byte pstQueenMg56;
  @Parameter
  byte pstQueenMg57;
  @Parameter
  byte pstQueenMg58;
  @Parameter
  byte pstQueenMg59;
  @Parameter
  byte pstQueenMg60;
  @Parameter
  byte pstQueenMg61;
  @Parameter
  byte pstQueenMg62;
  @Parameter
  byte pstQueenMg63;

  @Parameter
  byte pstQueenEg0;
  @Parameter
  byte pstQueenEg1;
  @Parameter
  byte pstQueenEg2;
  @Parameter
  byte pstQueenEg3;
  @Parameter
  byte pstQueenEg4;
  @Parameter
  byte pstQueenEg5;
  @Parameter
  byte pstQueenEg6;
  @Parameter
  byte pstQueenEg7;
  @Parameter
  byte pstQueenEg8;
  @Parameter
  byte pstQueenEg9;
  @Parameter
  byte pstQueenEg10;
  @Parameter
  byte pstQueenEg11;
  @Parameter
  byte pstQueenEg12;
  @Parameter
  byte pstQueenEg13;
  @Parameter
  byte pstQueenEg14;
  @Parameter
  byte pstQueenEg15;
  @Parameter
  byte pstQueenEg16;
  @Parameter
  byte pstQueenEg17;
  @Parameter
  byte pstQueenEg18;
  @Parameter
  byte pstQueenEg19;
  @Parameter
  byte pstQueenEg20;
  @Parameter
  byte pstQueenEg21;
  @Parameter
  byte pstQueenEg22;
  @Parameter
  byte pstQueenEg23;
  @Parameter
  byte pstQueenEg24;
  @Parameter
  byte pstQueenEg25;
  @Parameter
  byte pstQueenEg26;
  @Parameter
  byte pstQueenEg27;
  @Parameter
  byte pstQueenEg28;
  @Parameter
  byte pstQueenEg29;
  @Parameter
  byte pstQueenEg30;
  @Parameter
  byte pstQueenEg31;
  @Parameter
  byte pstQueenEg32;
  @Parameter
  byte pstQueenEg33;
  @Parameter
  byte pstQueenEg34;
  @Parameter
  byte pstQueenEg35;
  @Parameter
  byte pstQueenEg36;
  @Parameter
  byte pstQueenEg37;
  @Parameter
  byte pstQueenEg38;
  @Parameter
  byte pstQueenEg39;
  @Parameter
  byte pstQueenEg40;
  @Parameter
  byte pstQueenEg41;
  @Parameter
  byte pstQueenEg42;
  @Parameter
  byte pstQueenEg43;
  @Parameter
  byte pstQueenEg44;
  @Parameter
  byte pstQueenEg45;
  @Parameter
  byte pstQueenEg46;
  @Parameter
  byte pstQueenEg47;
  @Parameter
  byte pstQueenEg48;
  @Parameter
  byte pstQueenEg49;
  @Parameter
  byte pstQueenEg50;
  @Parameter
  byte pstQueenEg51;
  @Parameter
  byte pstQueenEg52;
  @Parameter
  byte pstQueenEg53;
  @Parameter
  byte pstQueenEg54;
  @Parameter
  byte pstQueenEg55;
  @Parameter
  byte pstQueenEg56;
  @Parameter
  byte pstQueenEg57;
  @Parameter
  byte pstQueenEg58;
  @Parameter
  byte pstQueenEg59;
  @Parameter
  byte pstQueenEg60;
  @Parameter
  byte pstQueenEg61;
  @Parameter
  byte pstQueenEg62;
  @Parameter
  byte pstQueenEg63;

  @Parameter
  byte pstKingMg0;
  @Parameter
  byte pstKingMg1;
  @Parameter
  byte pstKingMg2;
  @Parameter
  byte pstKingMg3;
  @Parameter
  byte pstKingMg4;
  @Parameter
  byte pstKingMg5;
  @Parameter
  byte pstKingMg6;
  @Parameter
  byte pstKingMg7;
  @Parameter
  byte pstKingMg8;
  @Parameter
  byte pstKingMg9;
  @Parameter
  byte pstKingMg10;
  @Parameter
  byte pstKingMg11;
  @Parameter
  byte pstKingMg12;
  @Parameter
  byte pstKingMg13;
  @Parameter
  byte pstKingMg14;
  @Parameter
  byte pstKingMg15;
  @Parameter
  byte pstKingMg16;
  @Parameter
  byte pstKingMg17;
  @Parameter
  byte pstKingMg18;
  @Parameter
  byte pstKingMg19;
  @Parameter
  byte pstKingMg20;
  @Parameter
  byte pstKingMg21;
  @Parameter
  byte pstKingMg22;
  @Parameter
  byte pstKingMg23;
  @Parameter
  byte pstKingMg24;
  @Parameter
  byte pstKingMg25;
  @Parameter
  byte pstKingMg26;
  @Parameter
  byte pstKingMg27;
  @Parameter
  byte pstKingMg28;
  @Parameter
  byte pstKingMg29;
  @Parameter
  byte pstKingMg30;
  @Parameter
  byte pstKingMg31;
  @Parameter
  byte pstKingMg32;
  @Parameter
  byte pstKingMg33;
  @Parameter
  byte pstKingMg34;
  @Parameter
  byte pstKingMg35;
  @Parameter
  byte pstKingMg36;
  @Parameter
  byte pstKingMg37;
  @Parameter
  byte pstKingMg38;
  @Parameter
  byte pstKingMg39;
  @Parameter
  byte pstKingMg40;
  @Parameter
  byte pstKingMg41;
  @Parameter
  byte pstKingMg42;
  @Parameter
  byte pstKingMg43;
  @Parameter
  byte pstKingMg44;
  @Parameter
  byte pstKingMg45;
  @Parameter
  byte pstKingMg46;
  @Parameter
  byte pstKingMg47;
  @Parameter
  byte pstKingMg48;
  @Parameter
  byte pstKingMg49;
  @Parameter
  byte pstKingMg50;
  @Parameter
  byte pstKingMg51;
  @Parameter
  byte pstKingMg52;
  @Parameter
  byte pstKingMg53;
  @Parameter
  byte pstKingMg54;
  @Parameter
  byte pstKingMg55;
  @Parameter
  byte pstKingMg56;
  @Parameter
  byte pstKingMg57;
  @Parameter
  byte pstKingMg58;
  @Parameter
  byte pstKingMg59;
  @Parameter
  byte pstKingMg60;
  @Parameter
  byte pstKingMg61;
  @Parameter
  byte pstKingMg62;
  @Parameter
  byte pstKingMg63;

  @Parameter
  byte pstKingEg0;
  @Parameter
  byte pstKingEg1;
  @Parameter
  byte pstKingEg2;
  @Parameter
  byte pstKingEg3;
  @Parameter
  byte pstKingEg4;
  @Parameter
  byte pstKingEg5;
  @Parameter
  byte pstKingEg6;
  @Parameter
  byte pstKingEg7;
  @Parameter
  byte pstKingEg8;
  @Parameter
  byte pstKingEg9;
  @Parameter
  byte pstKingEg10;
  @Parameter
  byte pstKingEg11;
  @Parameter
  byte pstKingEg12;
  @Parameter
  byte pstKingEg13;
  @Parameter
  byte pstKingEg14;
  @Parameter
  byte pstKingEg15;
  @Parameter
  byte pstKingEg16;
  @Parameter
  byte pstKingEg17;
  @Parameter
  byte pstKingEg18;
  @Parameter
  byte pstKingEg19;
  @Parameter
  byte pstKingEg20;
  @Parameter
  byte pstKingEg21;
  @Parameter
  byte pstKingEg22;
  @Parameter
  byte pstKingEg23;
  @Parameter
  byte pstKingEg24;
  @Parameter
  byte pstKingEg25;
  @Parameter
  byte pstKingEg26;
  @Parameter
  byte pstKingEg27;
  @Parameter
  byte pstKingEg28;
  @Parameter
  byte pstKingEg29;
  @Parameter
  byte pstKingEg30;
  @Parameter
  byte pstKingEg31;
  @Parameter
  byte pstKingEg32;
  @Parameter
  byte pstKingEg33;
  @Parameter
  byte pstKingEg34;
  @Parameter
  byte pstKingEg35;
  @Parameter
  byte pstKingEg36;
  @Parameter
  byte pstKingEg37;
  @Parameter
  byte pstKingEg38;
  @Parameter
  byte pstKingEg39;
  @Parameter
  byte pstKingEg40;
  @Parameter
  byte pstKingEg41;
  @Parameter
  byte pstKingEg42;
  @Parameter
  byte pstKingEg43;
  @Parameter
  byte pstKingEg44;
  @Parameter
  byte pstKingEg45;
  @Parameter
  byte pstKingEg46;
  @Parameter
  byte pstKingEg47;
  @Parameter
  byte pstKingEg48;
  @Parameter
  byte pstKingEg49;
  @Parameter
  byte pstKingEg50;
  @Parameter
  byte pstKingEg51;
  @Parameter
  byte pstKingEg52;
  @Parameter
  byte pstKingEg53;
  @Parameter
  byte pstKingEg54;
  @Parameter
  byte pstKingEg55;
  @Parameter
  byte pstKingEg56;
  @Parameter
  byte pstKingEg57;
  @Parameter
  byte pstKingEg58;
  @Parameter
  byte pstKingEg59;
  @Parameter
  byte pstKingEg60;
  @Parameter
  byte pstKingEg61;
  @Parameter
  byte pstKingEg62;
  @Parameter
  byte pstKingEg63;

  static final String[] PST_PAWN_MG_PARAM_NAMES = new String[]{
      null, null, null, null, null, null, null, null,
      "pstPawnMg8", "pstPawnMg9", "pstPawnMg10", "pstPawnMg11", "pstPawnMg12", "pstPawnMg13", "pstPawnMg14", "pstPawnMg15",
      "pstPawnMg16", "pstPawnMg17", "pstPawnMg18", "pstPawnMg19", "pstPawnMg20", "pstPawnMg21", "pstPawnMg22", "pstPawnMg23",
      "pstPawnMg24", "pstPawnMg25", "pstPawnMg26", "pstPawnMg27", "pstPawnMg28", "pstPawnMg29", "pstPawnMg30", "pstPawnMg31",
      "pstPawnMg32", "pstPawnMg33", "pstPawnMg34", "pstPawnMg35", "pstPawnMg36", "pstPawnMg37", "pstPawnMg38", "pstPawnMg39",
      "pstPawnMg40", "pstPawnMg41", "pstPawnMg42", "pstPawnMg43", "pstPawnMg44", "pstPawnMg45", "pstPawnMg46", "pstPawnMg47",
      "pstPawnMg48", "pstPawnMg49", "pstPawnMg50", "pstPawnMg51", "pstPawnMg52", "pstPawnMg53", "pstPawnMg54", "pstPawnMg55",
      null, null, null, null, null, null, null, null
  };

  static final String[] PST_PAWN_EG_PARAM_NAMES = new String[]{
      null, null, null, null, null, null, null, null,
      "pstPawnEg8", "pstPawnEg9", "pstPawnEg10", "pstPawnEg11", "pstPawnEg11", "pstPawnEg10", "pstPawnEg9", "pstPawnEg8",
      "pstPawnEg16", "pstPawnEg17", "pstPawnEg18", "pstPawnEg19", "pstPawnEg19", "pstPawnEg18", "pstPawnEg17", "pstPawnEg16",
      "pstPawnEg24", "pstPawnEg25", "pstPawnEg26", "pstPawnEg27", "pstPawnEg27", "pstPawnEg26", "pstPawnEg25", "pstPawnEg24",
      "pstPawnEg32", "pstPawnEg33", "pstPawnEg34", "pstPawnEg35", "pstPawnEg35", "pstPawnEg34", "pstPawnEg33", "pstPawnEg32",
      "pstPawnEg40", "pstPawnEg41", "pstPawnEg42", "pstPawnEg43", "pstPawnEg43", "pstPawnEg42", "pstPawnEg41", "pstPawnEg40",
      "pstPawnEg48", "pstPawnEg49", "pstPawnEg50", "pstPawnEg51", "pstPawnEg51", "pstPawnEg50", "pstPawnEg49", "pstPawnEg48",
      null, null, null, null, null, null, null, null
  };

  static final String[] PST_KNIGHT_MG_PARAM_NAMES =  new String[]{
      "pstKnightMg0", "pstKnightMg1", "pstKnightMg2", "pstKnightMg3", "pstKnightMg4", "pstKnightMg5", "pstKnightMg6", "pstKnightMg7",
      "pstKnightMg8", "pstKnightMg9", "pstKnightMg10", "pstKnightMg11", "pstKnightMg12", "pstKnightMg13", "pstKnightMg14", "pstKnightMg15",
      "pstKnightMg16", "pstKnightMg17", "pstKnightMg18", "pstKnightMg19", "pstKnightMg20", "pstKnightMg21", "pstKnightMg22", "pstKnightMg23",
      "pstKnightMg24", "pstKnightMg25", "pstKnightMg26", "pstKnightMg27", "pstKnightMg28", "pstKnightMg29", "pstKnightMg30", "pstKnightMg31",
      "pstKnightMg32", "pstKnightMg33", "pstKnightMg34", "pstKnightMg35", "pstKnightMg36", "pstKnightMg37", "pstKnightMg38", "pstKnightMg39",
      "pstKnightMg40", "pstKnightMg41", "pstKnightMg42", "pstKnightMg43", "pstKnightMg44", "pstKnightMg45", "pstKnightMg46", "pstKnightMg47",
      "pstKnightMg48", "pstKnightMg49", "pstKnightMg50", "pstKnightMg51", "pstKnightMg52", "pstKnightMg53", "pstKnightMg54", "pstKnightMg55",
      "pstKnightMg56", "pstKnightMg57", "pstKnightMg58", "pstKnightMg59", "pstKnightMg60", "pstKnightMg61", "pstKnightMg62", "pstKnightMg63"
  };

  static final String[] PST_KNIGHT_EG_PARAM_NAMES =  new String[]{
      "pstKnightEg0", "pstKnightEg1", "pstKnightEg2", "pstKnightEg3", "pstKnightEg4", "pstKnightEg5", "pstKnightEg6", "pstKnightEg7",
      "pstKnightEg8", "pstKnightEg9", "pstKnightEg10", "pstKnightEg11", "pstKnightEg12", "pstKnightEg13", "pstKnightEg14", "pstKnightEg15",
      "pstKnightEg16", "pstKnightEg17", "pstKnightEg18", "pstKnightEg19", "pstKnightEg20", "pstKnightEg21", "pstKnightEg22", "pstKnightEg23",
      "pstKnightEg24", "pstKnightEg25", "pstKnightEg26", "pstKnightEg27", "pstKnightEg28", "pstKnightEg29", "pstKnightEg30", "pstKnightEg31",
      "pstKnightEg32", "pstKnightEg33", "pstKnightEg34", "pstKnightEg35", "pstKnightEg36", "pstKnightEg37", "pstKnightEg38", "pstKnightEg39",
      "pstKnightEg40", "pstKnightEg41", "pstKnightEg42", "pstKnightEg43", "pstKnightEg44", "pstKnightEg45", "pstKnightEg46", "pstKnightEg47",
      "pstKnightEg48", "pstKnightEg49", "pstKnightEg50", "pstKnightEg51", "pstKnightEg52", "pstKnightEg53", "pstKnightEg54", "pstKnightEg55",
      "pstKnightEg56", "pstKnightEg57", "pstKnightEg58", "pstKnightEg59", "pstKnightEg60", "pstKnightEg61", "pstKnightEg62", "pstKnightEg63"
  };

  static final String[] PST_BISHOP_MG_PARAM_NAMES =  new String[]{
      "pstBishopMg0", "pstBishopMg1", "pstBishopMg2", "pstBishopMg3", "pstBishopMg4", "pstBishopMg5", "pstBishopMg6", "pstBishopMg7",
      "pstBishopMg8", "pstBishopMg9", "pstBishopMg10", "pstBishopMg11", "pstBishopMg12", "pstBishopMg13", "pstBishopMg14", "pstBishopMg15",
      "pstBishopMg16", "pstBishopMg17", "pstBishopMg18", "pstBishopMg19", "pstBishopMg20", "pstBishopMg21", "pstBishopMg22", "pstBishopMg23",
      "pstBishopMg24", "pstBishopMg25", "pstBishopMg26", "pstBishopMg27", "pstBishopMg28", "pstBishopMg29", "pstBishopMg30", "pstBishopMg31",
      "pstBishopMg32", "pstBishopMg33", "pstBishopMg34", "pstBishopMg35", "pstBishopMg36", "pstBishopMg37", "pstBishopMg38", "pstBishopMg39",
      "pstBishopMg40", "pstBishopMg41", "pstBishopMg42", "pstBishopMg43", "pstBishopMg44", "pstBishopMg45", "pstBishopMg46", "pstBishopMg47",
      "pstBishopMg48", "pstBishopMg49", "pstBishopMg50", "pstBishopMg51", "pstBishopMg52", "pstBishopMg53", "pstBishopMg54", "pstBishopMg55",
      "pstBishopMg56", "pstBishopMg57", "pstBishopMg58", "pstBishopMg59", "pstBishopMg60", "pstBishopMg61", "pstBishopMg62", "pstBishopMg63"
  };

  static final String[] PST_BISHOP_EG_PARAM_NAMES =  new String[]{
      "pstBishopEg0", "pstBishopEg1", "pstBishopEg2", "pstBishopEg3", "pstBishopEg4", "pstBishopEg5", "pstBishopEg6", "pstBishopEg7",
      "pstBishopEg8", "pstBishopEg9", "pstBishopEg10", "pstBishopEg11", "pstBishopEg12", "pstBishopEg13", "pstBishopEg14", "pstBishopEg15",
      "pstBishopEg16", "pstBishopEg17", "pstBishopEg18", "pstBishopEg19", "pstBishopEg20", "pstBishopEg21", "pstBishopEg22", "pstBishopEg23",
      "pstBishopEg24", "pstBishopEg25", "pstBishopEg26", "pstBishopEg27", "pstBishopEg28", "pstBishopEg29", "pstBishopEg30", "pstBishopEg31",
      "pstBishopEg32", "pstBishopEg33", "pstBishopEg34", "pstBishopEg35", "pstBishopEg36", "pstBishopEg37", "pstBishopEg38", "pstBishopEg39",
      "pstBishopEg40", "pstBishopEg41", "pstBishopEg42", "pstBishopEg43", "pstBishopEg44", "pstBishopEg45", "pstBishopEg46", "pstBishopEg47",
      "pstBishopEg48", "pstBishopEg49", "pstBishopEg50", "pstBishopEg51", "pstBishopEg52", "pstBishopEg53", "pstBishopEg54", "pstBishopEg55",
      "pstBishopEg56", "pstBishopEg57", "pstBishopEg58", "pstBishopEg59", "pstBishopEg60", "pstBishopEg61", "pstBishopEg62", "pstBishopEg63"
  };

  static final String[] PST_ROOK_MG_PARAM_NAMES =  new String[]{
      "pstRookMg0", "pstRookMg1", "pstRookMg2", "pstRookMg3", "pstRookMg4", "pstRookMg5", "pstRookMg6", "pstRookMg7",
      "pstRookMg8", "pstRookMg9", "pstRookMg10", "pstRookMg11", "pstRookMg12", "pstRookMg13", "pstRookMg14", "pstRookMg15",
      "pstRookMg16", "pstRookMg17", "pstRookMg18", "pstRookMg19", "pstRookMg20", "pstRookMg21", "pstRookMg22", "pstRookMg23",
      "pstRookMg24", "pstRookMg25", "pstRookMg26", "pstRookMg27", "pstRookMg28", "pstRookMg29", "pstRookMg30", "pstRookMg31",
      "pstRookMg32", "pstRookMg33", "pstRookMg34", "pstRookMg35", "pstRookMg36", "pstRookMg37", "pstRookMg38", "pstRookMg39",
      "pstRookMg40", "pstRookMg41", "pstRookMg42", "pstRookMg43", "pstRookMg44", "pstRookMg45", "pstRookMg46", "pstRookMg47",
      "pstRookMg48", "pstRookMg49", "pstRookMg50", "pstRookMg51", "pstRookMg52", "pstRookMg53", "pstRookMg54", "pstRookMg55",
      "pstRookMg56", "pstRookMg57", "pstRookMg58", "pstRookMg59", "pstRookMg60", "pstRookMg61", "pstRookMg62", "pstRookMg63"
  };

  static final String[] PST_ROOK_EG_PARAM_NAMES =  new String[]{
      "pstRookEg0", "pstRookEg1", "pstRookEg2", "pstRookEg3", "pstRookEg4", "pstRookEg5", "pstRookEg6", "pstRookEg7",
      "pstRookEg8", "pstRookEg9", "pstRookEg10", "pstRookEg11", "pstRookEg12", "pstRookEg13", "pstRookEg14", "pstRookEg15",
      "pstRookEg16", "pstRookEg17", "pstRookEg18", "pstRookEg19", "pstRookEg20", "pstRookEg21", "pstRookEg22", "pstRookEg23",
      "pstRookEg24", "pstRookEg25", "pstRookEg26", "pstRookEg27", "pstRookEg28", "pstRookEg29", "pstRookEg30", "pstRookEg31",
      "pstRookEg32", "pstRookEg33", "pstRookEg34", "pstRookEg35", "pstRookEg36", "pstRookEg37", "pstRookEg38", "pstRookEg39",
      "pstRookEg40", "pstRookEg41", "pstRookEg42", "pstRookEg43", "pstRookEg44", "pstRookEg45", "pstRookEg46", "pstRookEg47",
      "pstRookEg48", "pstRookEg49", "pstRookEg50", "pstRookEg51", "pstRookEg52", "pstRookEg53", "pstRookEg54", "pstRookEg55",
      "pstRookEg56", "pstRookEg57", "pstRookEg58", "pstRookEg59", "pstRookEg60", "pstRookEg61", "pstRookEg62", "pstRookEg63"
  };

  static final String[] PST_QUEEN_MG_PARAM_NAMES = new String[]{
      "pstQueenMg0", "pstQueenMg1", "pstQueenMg2", "pstQueenMg3", "pstQueenMg4", "pstQueenMg5", "pstQueenMg6", "pstQueenMg7",
      "pstQueenMg8", "pstQueenMg9", "pstQueenMg10", "pstQueenMg11", "pstQueenMg12", "pstQueenMg13", "pstQueenMg14", "pstQueenMg15",
      "pstQueenMg16", "pstQueenMg17", "pstQueenMg18", "pstQueenMg19", "pstQueenMg20", "pstQueenMg21", "pstQueenMg22", "pstQueenMg23",
      "pstQueenMg24", "pstQueenMg25", "pstQueenMg26", "pstQueenMg27", "pstQueenMg28", "pstQueenMg29", "pstQueenMg30", "pstQueenMg31",
      "pstQueenMg32", "pstQueenMg33", "pstQueenMg34", "pstQueenMg35", "pstQueenMg36", "pstQueenMg37", "pstQueenMg38", "pstQueenMg39",
      "pstQueenMg40", "pstQueenMg41", "pstQueenMg42", "pstQueenMg43", "pstQueenMg44", "pstQueenMg45", "pstQueenMg46", "pstQueenMg47",
      "pstQueenMg48", "pstQueenMg49", "pstQueenMg50", "pstQueenMg51", "pstQueenMg52", "pstQueenMg53", "pstQueenMg54", "pstQueenMg55",
      "pstQueenMg56", "pstQueenMg57", "pstQueenMg58", "pstQueenMg59", "pstQueenMg60", "pstQueenMg61", "pstQueenMg62", "pstQueenMg63"
  };

  static final String[] PST_QUEEN_EG_PARAM_NAMES = new String[]{
      "pstQueenEg0", "pstQueenEg1", "pstQueenEg2", "pstQueenEg3", "pstQueenEg4", "pstQueenEg5", "pstQueenEg6", "pstQueenEg7",
      "pstQueenEg8", "pstQueenEg9", "pstQueenEg10", "pstQueenEg11", "pstQueenEg12", "pstQueenEg13", "pstQueenEg14", "pstQueenEg15",
      "pstQueenEg16", "pstQueenEg17", "pstQueenEg18", "pstQueenEg19", "pstQueenEg20", "pstQueenEg21", "pstQueenEg22", "pstQueenEg23",
      "pstQueenEg24", "pstQueenEg25", "pstQueenEg26", "pstQueenEg27", "pstQueenEg28", "pstQueenEg29", "pstQueenEg30", "pstQueenEg31",
      "pstQueenEg32", "pstQueenEg33", "pstQueenEg34", "pstQueenEg35", "pstQueenEg36", "pstQueenEg37", "pstQueenEg38", "pstQueenEg39",
      "pstQueenEg40", "pstQueenEg41", "pstQueenEg42", "pstQueenEg43", "pstQueenEg44", "pstQueenEg45", "pstQueenEg46", "pstQueenEg47",
      "pstQueenEg48", "pstQueenEg49", "pstQueenEg50", "pstQueenEg51", "pstQueenEg52", "pstQueenEg53", "pstQueenEg54", "pstQueenEg55",
      "pstQueenEg56", "pstQueenEg57", "pstQueenEg58", "pstQueenEg59", "pstQueenEg60", "pstQueenEg61", "pstQueenEg62", "pstQueenEg63"
  };

  static final String[] PST_KING_MG_PARAM_NAMES =  new String[]{
      "pstKingMg0", "pstKingMg1", "pstKingMg2", "pstKingMg3", "pstKingMg4", "pstKingMg5", "pstKingMg6", "pstKingMg7",
      "pstKingMg8", "pstKingMg9", "pstKingMg10", "pstKingMg11", "pstKingMg12", "pstKingMg13", "pstKingMg14", "pstKingMg15",
      "pstKingMg16", "pstKingMg17", "pstKingMg18", "pstKingMg19", "pstKingMg20", "pstKingMg21", "pstKingMg22", "pstKingMg23",
      "pstKingMg24", "pstKingMg25", "pstKingMg26", "pstKingMg27", "pstKingMg28", "pstKingMg29", "pstKingMg30", "pstKingMg31",
      "pstKingMg32", "pstKingMg33", "pstKingMg34", "pstKingMg35", "pstKingMg36", "pstKingMg37", "pstKingMg38", "pstKingMg39",
      "pstKingMg40", "pstKingMg41", "pstKingMg42", "pstKingMg43", "pstKingMg44", "pstKingMg45", "pstKingMg46", "pstKingMg47",
      "pstKingMg48", "pstKingMg49", "pstKingMg50", "pstKingMg51", "pstKingMg52", "pstKingMg53", "pstKingMg54", "pstKingMg55",
      "pstKingMg56", "pstKingMg57", "pstKingMg58", "pstKingMg59", "pstKingMg60", "pstKingMg61", "pstKingMg62", "pstKingMg63"
  };

  static final String[] PST_KING_EG_PARAM_NAMES =  new String[]{
      "pstKingEg0", "pstKingEg1", "pstKingEg2", "pstKingEg3", "pstKingEg4", "pstKingEg5", "pstKingEg6", "pstKingEg7",
      "pstKingEg8", "pstKingEg9", "pstKingEg10", "pstKingEg11", "pstKingEg12", "pstKingEg13", "pstKingEg14", "pstKingEg15",
      "pstKingEg16", "pstKingEg17", "pstKingEg18", "pstKingEg19", "pstKingEg20", "pstKingEg21", "pstKingEg22", "pstKingEg23",
      "pstKingEg24", "pstKingEg25", "pstKingEg26", "pstKingEg27", "pstKingEg28", "pstKingEg29", "pstKingEg30", "pstKingEg31",
      "pstKingEg32", "pstKingEg33", "pstKingEg34", "pstKingEg35", "pstKingEg36", "pstKingEg37", "pstKingEg38", "pstKingEg39",
      "pstKingEg40", "pstKingEg41", "pstKingEg42", "pstKingEg43", "pstKingEg44", "pstKingEg45", "pstKingEg46", "pstKingEg47",
      "pstKingEg48", "pstKingEg49", "pstKingEg50", "pstKingEg51", "pstKingEg52", "pstKingEg53", "pstKingEg54", "pstKingEg55",
      "pstKingEg56", "pstKingEg57", "pstKingEg58", "pstKingEg59", "pstKingEg60", "pstKingEg61", "pstKingEg62", "pstKingEg63"
  };

  /**
   * Constructs an uninitialized instance.
   *
   * @throws ParameterException If a static or non-primitive field is annotated as a parameter.
   */
  DetroidParameters() throws ParameterException {
    super();
  }

  byte[] getPstPawnMg() {
    return new byte[]{
        0, 0, 0, 0, 0, 0, 0, 0,
        pstPawnMg8, pstPawnMg9, pstPawnMg10, pstPawnMg11, pstPawnMg12, pstPawnMg13, pstPawnMg14, pstPawnMg15,
        pstPawnMg16, pstPawnMg17, pstPawnMg18, pstPawnMg19, pstPawnMg20, pstPawnMg21, pstPawnMg22, pstPawnMg23,
        pstPawnMg24, pstPawnMg25, pstPawnMg26, pstPawnMg27, pstPawnMg28, pstPawnMg29, pstPawnMg30, pstPawnMg31,
        pstPawnMg32, pstPawnMg33, pstPawnMg34, pstPawnMg35, pstPawnMg36, pstPawnMg37, pstPawnMg38, pstPawnMg39,
        pstPawnMg40, pstPawnMg41, pstPawnMg42, pstPawnMg43, pstPawnMg44, pstPawnMg45, pstPawnMg46, pstPawnMg47,
        pstPawnMg48, pstPawnMg49, pstPawnMg50, pstPawnMg51, pstPawnMg52, pstPawnMg53, pstPawnMg54, pstPawnMg55,
        0, 0, 0, 0, 0, 0, 0, 0
    };
  }

  byte[] getPstPawnEg() {
    return new byte[]{
        0, 0, 0, 0, 0, 0, 0, 0,
        pstPawnEg8, pstPawnEg9, pstPawnEg10, pstPawnEg11, pstPawnEg11, pstPawnEg10, pstPawnEg9, pstPawnEg8,
        pstPawnEg16, pstPawnEg17, pstPawnEg18, pstPawnEg19, pstPawnEg19, pstPawnEg18, pstPawnEg17, pstPawnEg16,
        pstPawnEg24, pstPawnEg25, pstPawnEg26, pstPawnEg27, pstPawnEg27, pstPawnEg26, pstPawnEg25, pstPawnEg24,
        pstPawnEg32, pstPawnEg33, pstPawnEg34, pstPawnEg35, pstPawnEg35, pstPawnEg34, pstPawnEg33, pstPawnEg32,
        pstPawnEg40, pstPawnEg41, pstPawnEg42, pstPawnEg43, pstPawnEg43, pstPawnEg42, pstPawnEg41, pstPawnEg40,
        pstPawnEg48, pstPawnEg49, pstPawnEg50, pstPawnEg51, pstPawnEg51, pstPawnEg50, pstPawnEg49, pstPawnEg48,
        0, 0, 0, 0, 0, 0, 0, 0
    };
  }

  byte[] getPstKnightMg() {
    return new byte[]{
        pstKnightMg0, pstKnightMg1, pstKnightMg2, pstKnightMg3, pstKnightMg4, pstKnightMg5, pstKnightMg6, pstKnightMg7,
        pstKnightMg8, pstKnightMg9, pstKnightMg10, pstKnightMg11, pstKnightMg12, pstKnightMg13, pstKnightMg14, pstKnightMg15,
        pstKnightMg16, pstKnightMg17, pstKnightMg18, pstKnightMg19, pstKnightMg20, pstKnightMg21, pstKnightMg22, pstKnightMg23,
        pstKnightMg24, pstKnightMg25, pstKnightMg26, pstKnightMg27, pstKnightMg28, pstKnightMg29, pstKnightMg30, pstKnightMg31,
        pstKnightMg32, pstKnightMg33, pstKnightMg34, pstKnightMg35, pstKnightMg36, pstKnightMg37, pstKnightMg38, pstKnightMg39,
        pstKnightMg40, pstKnightMg41, pstKnightMg42, pstKnightMg43, pstKnightMg44, pstKnightMg45, pstKnightMg46, pstKnightMg47,
        pstKnightMg48, pstKnightMg49, pstKnightMg50, pstKnightMg51, pstKnightMg52, pstKnightMg53, pstKnightMg54, pstKnightMg55,
        pstKnightMg56, pstKnightMg57, pstKnightMg58, pstKnightMg59, pstKnightMg60, pstKnightMg61, pstKnightMg62, pstKnightMg63
    };
  }

  byte[] getPstKnightEg() {
    return new byte[]{
        pstKnightEg0, pstKnightEg1, pstKnightEg2, pstKnightEg3, pstKnightEg4, pstKnightEg5, pstKnightEg6, pstKnightEg7,
        pstKnightEg8, pstKnightEg9, pstKnightEg10, pstKnightEg11, pstKnightEg12, pstKnightEg13, pstKnightEg14, pstKnightEg15,
        pstKnightEg16, pstKnightEg17, pstKnightEg18, pstKnightEg19, pstKnightEg20, pstKnightEg21, pstKnightEg22, pstKnightEg23,
        pstKnightEg24, pstKnightEg25, pstKnightEg26, pstKnightEg27, pstKnightEg28, pstKnightEg29, pstKnightEg30, pstKnightEg31,
        pstKnightEg32, pstKnightEg33, pstKnightEg34, pstKnightEg35, pstKnightEg36, pstKnightEg37, pstKnightEg38, pstKnightEg39,
        pstKnightEg40, pstKnightEg41, pstKnightEg42, pstKnightEg43, pstKnightEg44, pstKnightEg45, pstKnightEg46, pstKnightEg47,
        pstKnightEg48, pstKnightEg49, pstKnightEg50, pstKnightEg51, pstKnightEg52, pstKnightEg53, pstKnightEg54, pstKnightEg55,
        pstKnightEg56, pstKnightEg57, pstKnightEg58, pstKnightEg59, pstKnightEg60, pstKnightEg61, pstKnightEg62, pstKnightEg63
    };
  }

  byte[] getPstBishopMg() {
    return new byte[]{
        pstBishopMg0, pstBishopMg1, pstBishopMg2, pstBishopMg3, pstBishopMg4, pstBishopMg5, pstBishopMg6, pstBishopMg7,
        pstBishopMg8, pstBishopMg9, pstBishopMg10, pstBishopMg11, pstBishopMg12, pstBishopMg13, pstBishopMg14, pstBishopMg15,
        pstBishopMg16, pstBishopMg17, pstBishopMg18, pstBishopMg19, pstBishopMg20, pstBishopMg21, pstBishopMg22, pstBishopMg23,
        pstBishopMg24, pstBishopMg25, pstBishopMg26, pstBishopMg27, pstBishopMg28, pstBishopMg29, pstBishopMg30, pstBishopMg31,
        pstBishopMg32, pstBishopMg33, pstBishopMg34, pstBishopMg35, pstBishopMg36, pstBishopMg37, pstBishopMg38, pstBishopMg39,
        pstBishopMg40, pstBishopMg41, pstBishopMg42, pstBishopMg43, pstBishopMg44, pstBishopMg45, pstBishopMg46, pstBishopMg47,
        pstBishopMg48, pstBishopMg49, pstBishopMg50, pstBishopMg51, pstBishopMg52, pstBishopMg53, pstBishopMg54, pstBishopMg55,
        pstBishopMg56, pstBishopMg57, pstBishopMg58, pstBishopMg59, pstBishopMg60, pstBishopMg61, pstBishopMg62, pstBishopMg63
    };
  }

  byte[] getPstBishopEg() {
    return new byte[]{
        pstBishopEg0, pstBishopEg1, pstBishopEg2, pstBishopEg3, pstBishopEg4, pstBishopEg5, pstBishopEg6, pstBishopEg7,
        pstBishopEg8, pstBishopEg9, pstBishopEg10, pstBishopEg11, pstBishopEg12, pstBishopEg13, pstBishopEg14, pstBishopEg15,
        pstBishopEg16, pstBishopEg17, pstBishopEg18, pstBishopEg19, pstBishopEg20, pstBishopEg21, pstBishopEg22, pstBishopEg23,
        pstBishopEg24, pstBishopEg25, pstBishopEg26, pstBishopEg27, pstBishopEg28, pstBishopEg29, pstBishopEg30, pstBishopEg31,
        pstBishopEg32, pstBishopEg33, pstBishopEg34, pstBishopEg35, pstBishopEg36, pstBishopEg37, pstBishopEg38, pstBishopEg39,
        pstBishopEg40, pstBishopEg41, pstBishopEg42, pstBishopEg43, pstBishopEg44, pstBishopEg45, pstBishopEg46, pstBishopEg47,
        pstBishopEg48, pstBishopEg49, pstBishopEg50, pstBishopEg51, pstBishopEg52, pstBishopEg53, pstBishopEg54, pstBishopEg55,
        pstBishopEg56, pstBishopEg57, pstBishopEg58, pstBishopEg59, pstBishopEg60, pstBishopEg61, pstBishopEg62, pstBishopEg63
    };
  }

  byte[] getPstRookMg() {
    return new byte[]{
        pstRookMg0, pstRookMg1, pstRookMg2, pstRookMg3, pstRookMg4, pstRookMg5, pstRookMg6, pstRookMg7,
        pstRookMg8, pstRookMg9, pstRookMg10, pstRookMg11, pstRookMg12, pstRookMg13, pstRookMg14, pstRookMg15,
        pstRookMg16, pstRookMg17, pstRookMg18, pstRookMg19, pstRookMg20, pstRookMg21, pstRookMg22, pstRookMg23,
        pstRookMg24, pstRookMg25, pstRookMg26, pstRookMg27, pstRookMg28, pstRookMg29, pstRookMg30, pstRookMg31,
        pstRookMg32, pstRookMg33, pstRookMg34, pstRookMg35, pstRookMg36, pstRookMg37, pstRookMg38, pstRookMg39,
        pstRookMg40, pstRookMg41, pstRookMg42, pstRookMg43, pstRookMg44, pstRookMg45, pstRookMg46, pstRookMg47,
        pstRookMg48, pstRookMg49, pstRookMg50, pstRookMg51, pstRookMg52, pstRookMg53, pstRookMg54, pstRookMg55,
        pstRookMg56, pstRookMg57, pstRookMg58, pstRookMg59, pstRookMg60, pstRookMg61, pstRookMg62, pstRookMg63
    };
  }

  byte[] getPstRookEg() {
    return new byte[]{
        pstRookEg0, pstRookEg1, pstRookEg2, pstRookEg3, pstRookEg4, pstRookEg5, pstRookEg6, pstRookEg7,
        pstRookEg8, pstRookEg9, pstRookEg10, pstRookEg11, pstRookEg12, pstRookEg13, pstRookEg14, pstRookEg15,
        pstRookEg16, pstRookEg17, pstRookEg18, pstRookEg19, pstRookEg20, pstRookEg21, pstRookEg22, pstRookEg23,
        pstRookEg24, pstRookEg25, pstRookEg26, pstRookEg27, pstRookEg28, pstRookEg29, pstRookEg30, pstRookEg31,
        pstRookEg32, pstRookEg33, pstRookEg34, pstRookEg35, pstRookEg36, pstRookEg37, pstRookEg38, pstRookEg39,
        pstRookEg40, pstRookEg41, pstRookEg42, pstRookEg43, pstRookEg44, pstRookEg45, pstRookEg46, pstRookEg47,
        pstRookEg48, pstRookEg49, pstRookEg50, pstRookEg51, pstRookEg52, pstRookEg53, pstRookEg54, pstRookEg55,
        pstRookEg56, pstRookEg57, pstRookEg58, pstRookEg59, pstRookEg60, pstRookEg61, pstRookEg62, pstRookEg63
    };
  }

  byte[] getPstQueenMg() {
    return new byte[]{
        pstQueenMg0, pstQueenMg1, pstQueenMg2, pstQueenMg3, pstQueenMg4, pstQueenMg5, pstQueenMg6, pstQueenMg7,
        pstQueenMg8, pstQueenMg9, pstQueenMg10, pstQueenMg11, pstQueenMg12, pstQueenMg13, pstQueenMg14, pstQueenMg15,
        pstQueenMg16, pstQueenMg17, pstQueenMg18, pstQueenMg19, pstQueenMg20, pstQueenMg21, pstQueenMg22, pstQueenMg23,
        pstQueenMg24, pstQueenMg25, pstQueenMg26, pstQueenMg27, pstQueenMg28, pstQueenMg29, pstQueenMg30, pstQueenMg31,
        pstQueenMg32, pstQueenMg33, pstQueenMg34, pstQueenMg35, pstQueenMg36, pstQueenMg37, pstQueenMg38, pstQueenMg39,
        pstQueenMg40, pstQueenMg41, pstQueenMg42, pstQueenMg43, pstQueenMg44, pstQueenMg45, pstQueenMg46, pstQueenMg47,
        pstQueenMg48, pstQueenMg49, pstQueenMg50, pstQueenMg51, pstQueenMg52, pstQueenMg53, pstQueenMg54, pstQueenMg55,
        pstQueenMg56, pstQueenMg57, pstQueenMg58, pstQueenMg59, pstQueenMg60, pstQueenMg61, pstQueenMg62, pstQueenMg63
    };
  }

  byte[] getPstQueenEg() {
    return new byte[]{
        pstQueenEg0, pstQueenEg1, pstQueenEg2, pstQueenEg3, pstQueenEg4, pstQueenEg5, pstQueenEg6, pstQueenEg7,
        pstQueenEg8, pstQueenEg9, pstQueenEg10, pstQueenEg11, pstQueenEg12, pstQueenEg13, pstQueenEg14, pstQueenEg15,
        pstQueenEg16, pstQueenEg17, pstQueenEg18, pstQueenEg19, pstQueenEg20, pstQueenEg21, pstQueenEg22, pstQueenEg23,
        pstQueenEg24, pstQueenEg25, pstQueenEg26, pstQueenEg27, pstQueenEg28, pstQueenEg29, pstQueenEg30, pstQueenEg31,
        pstQueenEg32, pstQueenEg33, pstQueenEg34, pstQueenEg35, pstQueenEg36, pstQueenEg37, pstQueenEg38, pstQueenEg39,
        pstQueenEg40, pstQueenEg41, pstQueenEg42, pstQueenEg43, pstQueenEg44, pstQueenEg45, pstQueenEg46, pstQueenEg47,
        pstQueenEg48, pstQueenEg49, pstQueenEg50, pstQueenEg51, pstQueenEg52, pstQueenEg53, pstQueenEg54, pstQueenEg55,
        pstQueenEg56, pstQueenEg57, pstQueenEg58, pstQueenEg59, pstQueenEg60, pstQueenEg61, pstQueenEg62, pstQueenEg63
    };
  }

  byte[] getPstKingMg() {
    return new byte[]{
        pstKingMg0, pstKingMg1, pstKingMg2, pstKingMg3, pstKingMg4, pstKingMg5, pstKingMg6, pstKingMg7,
        pstKingMg8, pstKingMg9, pstKingMg10, pstKingMg11, pstKingMg12, pstKingMg13, pstKingMg14, pstKingMg15,
        pstKingMg16, pstKingMg17, pstKingMg18, pstKingMg19, pstKingMg20, pstKingMg21, pstKingMg22, pstKingMg23,
        pstKingMg24, pstKingMg25, pstKingMg26, pstKingMg27, pstKingMg28, pstKingMg29, pstKingMg30, pstKingMg31,
        pstKingMg32, pstKingMg33, pstKingMg34, pstKingMg35, pstKingMg36, pstKingMg37, pstKingMg38, pstKingMg39,
        pstKingMg40, pstKingMg41, pstKingMg42, pstKingMg43, pstKingMg44, pstKingMg45, pstKingMg46, pstKingMg47,
        pstKingMg48, pstKingMg49, pstKingMg50, pstKingMg51, pstKingMg52, pstKingMg53, pstKingMg54, pstKingMg55,
        pstKingMg56, pstKingMg57, pstKingMg58, pstKingMg59, pstKingMg60, pstKingMg61, pstKingMg62, pstKingMg63
    };
  }

  byte[] getPstKingEg() {
    return new byte[]{
        pstKingEg0, pstKingEg1, pstKingEg2, pstKingEg3, pstKingEg4, pstKingEg5, pstKingEg6, pstKingEg7,
        pstKingEg8, pstKingEg9, pstKingEg10, pstKingEg11, pstKingEg12, pstKingEg13, pstKingEg14, pstKingEg15,
        pstKingEg16, pstKingEg17, pstKingEg18, pstKingEg19, pstKingEg20, pstKingEg21, pstKingEg22, pstKingEg23,
        pstKingEg24, pstKingEg25, pstKingEg26, pstKingEg27, pstKingEg28, pstKingEg29, pstKingEg30, pstKingEg31,
        pstKingEg32, pstKingEg33, pstKingEg34, pstKingEg35, pstKingEg36, pstKingEg37, pstKingEg38, pstKingEg39,
        pstKingEg40, pstKingEg41, pstKingEg42, pstKingEg43, pstKingEg44, pstKingEg45, pstKingEg46, pstKingEg47,
        pstKingEg48, pstKingEg49, pstKingEg50, pstKingEg51, pstKingEg52, pstKingEg53, pstKingEg54, pstKingEg55,
        pstKingEg56, pstKingEg57, pstKingEg58, pstKingEg59, pstKingEg60, pstKingEg61, pstKingEg62, pstKingEg63
    };
  }

}
