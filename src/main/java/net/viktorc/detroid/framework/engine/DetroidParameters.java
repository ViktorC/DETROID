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
  short blockedPawnWeight1;
  @Parameter
  short blockedPawnWeight2;
  @Parameter
  short blockedPawnWeight3;
  @Parameter
  short passedPawnWeight;
  @Parameter
  short isolatedPawnWeight;
  @Parameter
  short backwardPawnWeight;
  @Parameter
  short stoppedPawnWeight;
  @Parameter
  short bishopPairAdvantage;
  @Parameter
  short pinnedQueenWeight;
  @Parameter
  short pinnedRookWeight;
  @Parameter
  short pinnedBishopWeight;
  @Parameter
  short pinnedKnightWeight;
  @Parameter
  short pinnedPawnWeight;
  @Parameter
  short queenMobilityWeight;
  @Parameter
  short rookMobilityWeight;
  @Parameter
  short bishopMobilityWeight;
  @Parameter
  short knightMobilityWeight;
  @Parameter
  short friendlyNormalPawnTropismWeight;
  @Parameter
  short friendlyWeakPawnTropismWeight;
  @Parameter
  short friendlyPassedPawnTropismWeight;
  @Parameter
  short opponentNormalPawnTropismWeight;
  @Parameter
  short opponentWeakPawnTropismWeight;
  @Parameter
  short opponentPassedPawnTropismWeight;
  @Parameter
  short opponentQueenTropismWeight;
  @Parameter
  short tempoAdvantage;

  @Parameter
  short pstPawnMg8;
  @Parameter
  short pstPawnMg9;
  @Parameter
  short pstPawnMg10;
  @Parameter
  short pstPawnMg11;
  @Parameter
  short pstPawnMg12;
  @Parameter
  short pstPawnMg13;
  @Parameter
  short pstPawnMg14;
  @Parameter
  short pstPawnMg15;
  @Parameter
  short pstPawnMg16;
  @Parameter
  short pstPawnMg17;
  @Parameter
  short pstPawnMg18;
  @Parameter
  short pstPawnMg19;
  @Parameter
  short pstPawnMg20;
  @Parameter
  short pstPawnMg21;
  @Parameter
  short pstPawnMg22;
  @Parameter
  short pstPawnMg23;
  @Parameter
  short pstPawnMg24;
  @Parameter
  short pstPawnMg25;
  @Parameter
  short pstPawnMg26;
  @Parameter
  short pstPawnMg27;
  @Parameter
  short pstPawnMg28;
  @Parameter
  short pstPawnMg29;
  @Parameter
  short pstPawnMg30;
  @Parameter
  short pstPawnMg31;
  @Parameter
  short pstPawnMg32;
  @Parameter
  short pstPawnMg33;
  @Parameter
  short pstPawnMg34;
  @Parameter
  short pstPawnMg35;
  @Parameter
  short pstPawnMg36;
  @Parameter
  short pstPawnMg37;
  @Parameter
  short pstPawnMg38;
  @Parameter
  short pstPawnMg39;
  @Parameter
  short pstPawnMg40;
  @Parameter
  short pstPawnMg41;
  @Parameter
  short pstPawnMg42;
  @Parameter
  short pstPawnMg43;
  @Parameter
  short pstPawnMg44;
  @Parameter
  short pstPawnMg45;
  @Parameter
  short pstPawnMg46;
  @Parameter
  short pstPawnMg47;
  @Parameter
  short pstPawnMg48;
  @Parameter
  short pstPawnMg49;
  @Parameter
  short pstPawnMg50;
  @Parameter
  short pstPawnMg51;
  @Parameter
  short pstPawnMg52;
  @Parameter
  short pstPawnMg53;
  @Parameter
  short pstPawnMg54;
  @Parameter
  short pstPawnMg55;

  @Parameter
  short pstPawnEg8;
  @Parameter
  short pstPawnEg9;
  @Parameter
  short pstPawnEg10;
  @Parameter
  short pstPawnEg11;
  @Parameter
  short pstPawnEg16;
  @Parameter
  short pstPawnEg17;
  @Parameter
  short pstPawnEg18;
  @Parameter
  short pstPawnEg19;
  @Parameter
  short pstPawnEg24;
  @Parameter
  short pstPawnEg25;
  @Parameter
  short pstPawnEg26;
  @Parameter
  short pstPawnEg27;
  @Parameter
  short pstPawnEg32;
  @Parameter
  short pstPawnEg33;
  @Parameter
  short pstPawnEg34;
  @Parameter
  short pstPawnEg35;
  @Parameter
  short pstPawnEg40;
  @Parameter
  short pstPawnEg41;
  @Parameter
  short pstPawnEg42;
  @Parameter
  short pstPawnEg43;
  @Parameter
  short pstPawnEg48;
  @Parameter
  short pstPawnEg49;
  @Parameter
  short pstPawnEg50;
  @Parameter
  short pstPawnEg51;

  @Parameter
  short pstKnightMg0;
  @Parameter
  short pstKnightMg1;
  @Parameter
  short pstKnightMg2;
  @Parameter
  short pstKnightMg3;
  @Parameter
  short pstKnightMg4;
  @Parameter
  short pstKnightMg5;
  @Parameter
  short pstKnightMg6;
  @Parameter
  short pstKnightMg7;
  @Parameter
  short pstKnightMg8;
  @Parameter
  short pstKnightMg9;
  @Parameter
  short pstKnightMg10;
  @Parameter
  short pstKnightMg11;
  @Parameter
  short pstKnightMg12;
  @Parameter
  short pstKnightMg13;
  @Parameter
  short pstKnightMg14;
  @Parameter
  short pstKnightMg15;
  @Parameter
  short pstKnightMg16;
  @Parameter
  short pstKnightMg17;
  @Parameter
  short pstKnightMg18;
  @Parameter
  short pstKnightMg19;
  @Parameter
  short pstKnightMg20;
  @Parameter
  short pstKnightMg21;
  @Parameter
  short pstKnightMg22;
  @Parameter
  short pstKnightMg23;
  @Parameter
  short pstKnightMg24;
  @Parameter
  short pstKnightMg25;
  @Parameter
  short pstKnightMg26;
  @Parameter
  short pstKnightMg27;
  @Parameter
  short pstKnightMg28;
  @Parameter
  short pstKnightMg29;
  @Parameter
  short pstKnightMg30;
  @Parameter
  short pstKnightMg31;
  @Parameter
  short pstKnightMg32;
  @Parameter
  short pstKnightMg33;
  @Parameter
  short pstKnightMg34;
  @Parameter
  short pstKnightMg35;
  @Parameter
  short pstKnightMg36;
  @Parameter
  short pstKnightMg37;
  @Parameter
  short pstKnightMg38;
  @Parameter
  short pstKnightMg39;
  @Parameter
  short pstKnightMg40;
  @Parameter
  short pstKnightMg41;
  @Parameter
  short pstKnightMg42;
  @Parameter
  short pstKnightMg43;
  @Parameter
  short pstKnightMg44;
  @Parameter
  short pstKnightMg45;
  @Parameter
  short pstKnightMg46;
  @Parameter
  short pstKnightMg47;
  @Parameter
  short pstKnightMg48;
  @Parameter
  short pstKnightMg49;
  @Parameter
  short pstKnightMg50;
  @Parameter
  short pstKnightMg51;
  @Parameter
  short pstKnightMg52;
  @Parameter
  short pstKnightMg53;
  @Parameter
  short pstKnightMg54;
  @Parameter
  short pstKnightMg55;
  @Parameter
  short pstKnightMg56;
  @Parameter
  short pstKnightMg57;
  @Parameter
  short pstKnightMg58;
  @Parameter
  short pstKnightMg59;
  @Parameter
  short pstKnightMg60;
  @Parameter
  short pstKnightMg61;
  @Parameter
  short pstKnightMg62;
  @Parameter
  short pstKnightMg63;

  @Parameter
  short pstKnightEg0;
  @Parameter
  short pstKnightEg1;
  @Parameter
  short pstKnightEg2;
  @Parameter
  short pstKnightEg3;
  @Parameter
  short pstKnightEg4;
  @Parameter
  short pstKnightEg5;
  @Parameter
  short pstKnightEg6;
  @Parameter
  short pstKnightEg7;
  @Parameter
  short pstKnightEg8;
  @Parameter
  short pstKnightEg9;
  @Parameter
  short pstKnightEg10;
  @Parameter
  short pstKnightEg11;
  @Parameter
  short pstKnightEg12;
  @Parameter
  short pstKnightEg13;
  @Parameter
  short pstKnightEg14;
  @Parameter
  short pstKnightEg15;
  @Parameter
  short pstKnightEg16;
  @Parameter
  short pstKnightEg17;
  @Parameter
  short pstKnightEg18;
  @Parameter
  short pstKnightEg19;
  @Parameter
  short pstKnightEg20;
  @Parameter
  short pstKnightEg21;
  @Parameter
  short pstKnightEg22;
  @Parameter
  short pstKnightEg23;
  @Parameter
  short pstKnightEg24;
  @Parameter
  short pstKnightEg25;
  @Parameter
  short pstKnightEg26;
  @Parameter
  short pstKnightEg27;
  @Parameter
  short pstKnightEg28;
  @Parameter
  short pstKnightEg29;
  @Parameter
  short pstKnightEg30;
  @Parameter
  short pstKnightEg31;
  @Parameter
  short pstKnightEg32;
  @Parameter
  short pstKnightEg33;
  @Parameter
  short pstKnightEg34;
  @Parameter
  short pstKnightEg35;
  @Parameter
  short pstKnightEg36;
  @Parameter
  short pstKnightEg37;
  @Parameter
  short pstKnightEg38;
  @Parameter
  short pstKnightEg39;
  @Parameter
  short pstKnightEg40;
  @Parameter
  short pstKnightEg41;
  @Parameter
  short pstKnightEg42;
  @Parameter
  short pstKnightEg43;
  @Parameter
  short pstKnightEg44;
  @Parameter
  short pstKnightEg45;
  @Parameter
  short pstKnightEg46;
  @Parameter
  short pstKnightEg47;
  @Parameter
  short pstKnightEg48;
  @Parameter
  short pstKnightEg49;
  @Parameter
  short pstKnightEg50;
  @Parameter
  short pstKnightEg51;
  @Parameter
  short pstKnightEg52;
  @Parameter
  short pstKnightEg53;
  @Parameter
  short pstKnightEg54;
  @Parameter
  short pstKnightEg55;
  @Parameter
  short pstKnightEg56;
  @Parameter
  short pstKnightEg57;
  @Parameter
  short pstKnightEg58;
  @Parameter
  short pstKnightEg59;
  @Parameter
  short pstKnightEg60;
  @Parameter
  short pstKnightEg61;
  @Parameter
  short pstKnightEg62;
  @Parameter
  short pstKnightEg63;

  @Parameter
  short pstBishopMg0;
  @Parameter
  short pstBishopMg1;
  @Parameter
  short pstBishopMg2;
  @Parameter
  short pstBishopMg3;
  @Parameter
  short pstBishopMg4;
  @Parameter
  short pstBishopMg5;
  @Parameter
  short pstBishopMg6;
  @Parameter
  short pstBishopMg7;
  @Parameter
  short pstBishopMg8;
  @Parameter
  short pstBishopMg9;
  @Parameter
  short pstBishopMg10;
  @Parameter
  short pstBishopMg11;
  @Parameter
  short pstBishopMg12;
  @Parameter
  short pstBishopMg13;
  @Parameter
  short pstBishopMg14;
  @Parameter
  short pstBishopMg15;
  @Parameter
  short pstBishopMg16;
  @Parameter
  short pstBishopMg17;
  @Parameter
  short pstBishopMg18;
  @Parameter
  short pstBishopMg19;
  @Parameter
  short pstBishopMg20;
  @Parameter
  short pstBishopMg21;
  @Parameter
  short pstBishopMg22;
  @Parameter
  short pstBishopMg23;
  @Parameter
  short pstBishopMg24;
  @Parameter
  short pstBishopMg25;
  @Parameter
  short pstBishopMg26;
  @Parameter
  short pstBishopMg27;
  @Parameter
  short pstBishopMg28;
  @Parameter
  short pstBishopMg29;
  @Parameter
  short pstBishopMg30;
  @Parameter
  short pstBishopMg31;
  @Parameter
  short pstBishopMg32;
  @Parameter
  short pstBishopMg33;
  @Parameter
  short pstBishopMg34;
  @Parameter
  short pstBishopMg35;
  @Parameter
  short pstBishopMg36;
  @Parameter
  short pstBishopMg37;
  @Parameter
  short pstBishopMg38;
  @Parameter
  short pstBishopMg39;
  @Parameter
  short pstBishopMg40;
  @Parameter
  short pstBishopMg41;
  @Parameter
  short pstBishopMg42;
  @Parameter
  short pstBishopMg43;
  @Parameter
  short pstBishopMg44;
  @Parameter
  short pstBishopMg45;
  @Parameter
  short pstBishopMg46;
  @Parameter
  short pstBishopMg47;
  @Parameter
  short pstBishopMg48;
  @Parameter
  short pstBishopMg49;
  @Parameter
  short pstBishopMg50;
  @Parameter
  short pstBishopMg51;
  @Parameter
  short pstBishopMg52;
  @Parameter
  short pstBishopMg53;
  @Parameter
  short pstBishopMg54;
  @Parameter
  short pstBishopMg55;
  @Parameter
  short pstBishopMg56;
  @Parameter
  short pstBishopMg57;
  @Parameter
  short pstBishopMg58;
  @Parameter
  short pstBishopMg59;
  @Parameter
  short pstBishopMg60;
  @Parameter
  short pstBishopMg61;
  @Parameter
  short pstBishopMg62;
  @Parameter
  short pstBishopMg63;

  @Parameter
  short pstBishopEg0;
  @Parameter
  short pstBishopEg1;
  @Parameter
  short pstBishopEg2;
  @Parameter
  short pstBishopEg3;
  @Parameter
  short pstBishopEg4;
  @Parameter
  short pstBishopEg5;
  @Parameter
  short pstBishopEg6;
  @Parameter
  short pstBishopEg7;
  @Parameter
  short pstBishopEg8;
  @Parameter
  short pstBishopEg9;
  @Parameter
  short pstBishopEg10;
  @Parameter
  short pstBishopEg11;
  @Parameter
  short pstBishopEg12;
  @Parameter
  short pstBishopEg13;
  @Parameter
  short pstBishopEg14;
  @Parameter
  short pstBishopEg15;
  @Parameter
  short pstBishopEg16;
  @Parameter
  short pstBishopEg17;
  @Parameter
  short pstBishopEg18;
  @Parameter
  short pstBishopEg19;
  @Parameter
  short pstBishopEg20;
  @Parameter
  short pstBishopEg21;
  @Parameter
  short pstBishopEg22;
  @Parameter
  short pstBishopEg23;
  @Parameter
  short pstBishopEg24;
  @Parameter
  short pstBishopEg25;
  @Parameter
  short pstBishopEg26;
  @Parameter
  short pstBishopEg27;
  @Parameter
  short pstBishopEg28;
  @Parameter
  short pstBishopEg29;
  @Parameter
  short pstBishopEg30;
  @Parameter
  short pstBishopEg31;
  @Parameter
  short pstBishopEg32;
  @Parameter
  short pstBishopEg33;
  @Parameter
  short pstBishopEg34;
  @Parameter
  short pstBishopEg35;
  @Parameter
  short pstBishopEg36;
  @Parameter
  short pstBishopEg37;
  @Parameter
  short pstBishopEg38;
  @Parameter
  short pstBishopEg39;
  @Parameter
  short pstBishopEg40;
  @Parameter
  short pstBishopEg41;
  @Parameter
  short pstBishopEg42;
  @Parameter
  short pstBishopEg43;
  @Parameter
  short pstBishopEg44;
  @Parameter
  short pstBishopEg45;
  @Parameter
  short pstBishopEg46;
  @Parameter
  short pstBishopEg47;
  @Parameter
  short pstBishopEg48;
  @Parameter
  short pstBishopEg49;
  @Parameter
  short pstBishopEg50;
  @Parameter
  short pstBishopEg51;
  @Parameter
  short pstBishopEg52;
  @Parameter
  short pstBishopEg53;
  @Parameter
  short pstBishopEg54;
  @Parameter
  short pstBishopEg55;
  @Parameter
  short pstBishopEg56;
  @Parameter
  short pstBishopEg57;
  @Parameter
  short pstBishopEg58;
  @Parameter
  short pstBishopEg59;
  @Parameter
  short pstBishopEg60;
  @Parameter
  short pstBishopEg61;
  @Parameter
  short pstBishopEg62;
  @Parameter
  short pstBishopEg63;

  @Parameter
  short pstRookMg0;
  @Parameter
  short pstRookMg1;
  @Parameter
  short pstRookMg2;
  @Parameter
  short pstRookMg3;
  @Parameter
  short pstRookMg4;
  @Parameter
  short pstRookMg5;
  @Parameter
  short pstRookMg6;
  @Parameter
  short pstRookMg7;
  @Parameter
  short pstRookMg8;
  @Parameter
  short pstRookMg9;
  @Parameter
  short pstRookMg10;
  @Parameter
  short pstRookMg11;
  @Parameter
  short pstRookMg12;
  @Parameter
  short pstRookMg13;
  @Parameter
  short pstRookMg14;
  @Parameter
  short pstRookMg15;
  @Parameter
  short pstRookMg16;
  @Parameter
  short pstRookMg17;
  @Parameter
  short pstRookMg18;
  @Parameter
  short pstRookMg19;
  @Parameter
  short pstRookMg20;
  @Parameter
  short pstRookMg21;
  @Parameter
  short pstRookMg22;
  @Parameter
  short pstRookMg23;
  @Parameter
  short pstRookMg24;
  @Parameter
  short pstRookMg25;
  @Parameter
  short pstRookMg26;
  @Parameter
  short pstRookMg27;
  @Parameter
  short pstRookMg28;
  @Parameter
  short pstRookMg29;
  @Parameter
  short pstRookMg30;
  @Parameter
  short pstRookMg31;
  @Parameter
  short pstRookMg32;
  @Parameter
  short pstRookMg33;
  @Parameter
  short pstRookMg34;
  @Parameter
  short pstRookMg35;
  @Parameter
  short pstRookMg36;
  @Parameter
  short pstRookMg37;
  @Parameter
  short pstRookMg38;
  @Parameter
  short pstRookMg39;
  @Parameter
  short pstRookMg40;
  @Parameter
  short pstRookMg41;
  @Parameter
  short pstRookMg42;
  @Parameter
  short pstRookMg43;
  @Parameter
  short pstRookMg44;
  @Parameter
  short pstRookMg45;
  @Parameter
  short pstRookMg46;
  @Parameter
  short pstRookMg47;
  @Parameter
  short pstRookMg48;
  @Parameter
  short pstRookMg49;
  @Parameter
  short pstRookMg50;
  @Parameter
  short pstRookMg51;
  @Parameter
  short pstRookMg52;
  @Parameter
  short pstRookMg53;
  @Parameter
  short pstRookMg54;
  @Parameter
  short pstRookMg55;
  @Parameter
  short pstRookMg56;
  @Parameter
  short pstRookMg57;
  @Parameter
  short pstRookMg58;
  @Parameter
  short pstRookMg59;
  @Parameter
  short pstRookMg60;
  @Parameter
  short pstRookMg61;
  @Parameter
  short pstRookMg62;
  @Parameter
  short pstRookMg63;

  @Parameter
  short pstRookEg0;
  @Parameter
  short pstRookEg1;
  @Parameter
  short pstRookEg2;
  @Parameter
  short pstRookEg3;
  @Parameter
  short pstRookEg4;
  @Parameter
  short pstRookEg5;
  @Parameter
  short pstRookEg6;
  @Parameter
  short pstRookEg7;
  @Parameter
  short pstRookEg8;
  @Parameter
  short pstRookEg9;
  @Parameter
  short pstRookEg10;
  @Parameter
  short pstRookEg11;
  @Parameter
  short pstRookEg12;
  @Parameter
  short pstRookEg13;
  @Parameter
  short pstRookEg14;
  @Parameter
  short pstRookEg15;
  @Parameter
  short pstRookEg16;
  @Parameter
  short pstRookEg17;
  @Parameter
  short pstRookEg18;
  @Parameter
  short pstRookEg19;
  @Parameter
  short pstRookEg20;
  @Parameter
  short pstRookEg21;
  @Parameter
  short pstRookEg22;
  @Parameter
  short pstRookEg23;
  @Parameter
  short pstRookEg24;
  @Parameter
  short pstRookEg25;
  @Parameter
  short pstRookEg26;
  @Parameter
  short pstRookEg27;
  @Parameter
  short pstRookEg28;
  @Parameter
  short pstRookEg29;
  @Parameter
  short pstRookEg30;
  @Parameter
  short pstRookEg31;
  @Parameter
  short pstRookEg32;
  @Parameter
  short pstRookEg33;
  @Parameter
  short pstRookEg34;
  @Parameter
  short pstRookEg35;
  @Parameter
  short pstRookEg36;
  @Parameter
  short pstRookEg37;
  @Parameter
  short pstRookEg38;
  @Parameter
  short pstRookEg39;
  @Parameter
  short pstRookEg40;
  @Parameter
  short pstRookEg41;
  @Parameter
  short pstRookEg42;
  @Parameter
  short pstRookEg43;
  @Parameter
  short pstRookEg44;
  @Parameter
  short pstRookEg45;
  @Parameter
  short pstRookEg46;
  @Parameter
  short pstRookEg47;
  @Parameter
  short pstRookEg48;
  @Parameter
  short pstRookEg49;
  @Parameter
  short pstRookEg50;
  @Parameter
  short pstRookEg51;
  @Parameter
  short pstRookEg52;
  @Parameter
  short pstRookEg53;
  @Parameter
  short pstRookEg54;
  @Parameter
  short pstRookEg55;
  @Parameter
  short pstRookEg56;
  @Parameter
  short pstRookEg57;
  @Parameter
  short pstRookEg58;
  @Parameter
  short pstRookEg59;
  @Parameter
  short pstRookEg60;
  @Parameter
  short pstRookEg61;
  @Parameter
  short pstRookEg62;
  @Parameter
  short pstRookEg63;

  @Parameter
  short pstQueenMg0;
  @Parameter
  short pstQueenMg1;
  @Parameter
  short pstQueenMg2;
  @Parameter
  short pstQueenMg3;
  @Parameter
  short pstQueenMg4;
  @Parameter
  short pstQueenMg5;
  @Parameter
  short pstQueenMg6;
  @Parameter
  short pstQueenMg7;
  @Parameter
  short pstQueenMg8;
  @Parameter
  short pstQueenMg9;
  @Parameter
  short pstQueenMg10;
  @Parameter
  short pstQueenMg11;
  @Parameter
  short pstQueenMg12;
  @Parameter
  short pstQueenMg13;
  @Parameter
  short pstQueenMg14;
  @Parameter
  short pstQueenMg15;
  @Parameter
  short pstQueenMg16;
  @Parameter
  short pstQueenMg17;
  @Parameter
  short pstQueenMg18;
  @Parameter
  short pstQueenMg19;
  @Parameter
  short pstQueenMg20;
  @Parameter
  short pstQueenMg21;
  @Parameter
  short pstQueenMg22;
  @Parameter
  short pstQueenMg23;
  @Parameter
  short pstQueenMg24;
  @Parameter
  short pstQueenMg25;
  @Parameter
  short pstQueenMg26;
  @Parameter
  short pstQueenMg27;
  @Parameter
  short pstQueenMg28;
  @Parameter
  short pstQueenMg29;
  @Parameter
  short pstQueenMg30;
  @Parameter
  short pstQueenMg31;
  @Parameter
  short pstQueenMg32;
  @Parameter
  short pstQueenMg33;
  @Parameter
  short pstQueenMg34;
  @Parameter
  short pstQueenMg35;
  @Parameter
  short pstQueenMg36;
  @Parameter
  short pstQueenMg37;
  @Parameter
  short pstQueenMg38;
  @Parameter
  short pstQueenMg39;
  @Parameter
  short pstQueenMg40;
  @Parameter
  short pstQueenMg41;
  @Parameter
  short pstQueenMg42;
  @Parameter
  short pstQueenMg43;
  @Parameter
  short pstQueenMg44;
  @Parameter
  short pstQueenMg45;
  @Parameter
  short pstQueenMg46;
  @Parameter
  short pstQueenMg47;
  @Parameter
  short pstQueenMg48;
  @Parameter
  short pstQueenMg49;
  @Parameter
  short pstQueenMg50;
  @Parameter
  short pstQueenMg51;
  @Parameter
  short pstQueenMg52;
  @Parameter
  short pstQueenMg53;
  @Parameter
  short pstQueenMg54;
  @Parameter
  short pstQueenMg55;
  @Parameter
  short pstQueenMg56;
  @Parameter
  short pstQueenMg57;
  @Parameter
  short pstQueenMg58;
  @Parameter
  short pstQueenMg59;
  @Parameter
  short pstQueenMg60;
  @Parameter
  short pstQueenMg61;
  @Parameter
  short pstQueenMg62;
  @Parameter
  short pstQueenMg63;

  @Parameter
  short pstQueenEg0;
  @Parameter
  short pstQueenEg1;
  @Parameter
  short pstQueenEg2;
  @Parameter
  short pstQueenEg3;
  @Parameter
  short pstQueenEg4;
  @Parameter
  short pstQueenEg5;
  @Parameter
  short pstQueenEg6;
  @Parameter
  short pstQueenEg7;
  @Parameter
  short pstQueenEg8;
  @Parameter
  short pstQueenEg9;
  @Parameter
  short pstQueenEg10;
  @Parameter
  short pstQueenEg11;
  @Parameter
  short pstQueenEg12;
  @Parameter
  short pstQueenEg13;
  @Parameter
  short pstQueenEg14;
  @Parameter
  short pstQueenEg15;
  @Parameter
  short pstQueenEg16;
  @Parameter
  short pstQueenEg17;
  @Parameter
  short pstQueenEg18;
  @Parameter
  short pstQueenEg19;
  @Parameter
  short pstQueenEg20;
  @Parameter
  short pstQueenEg21;
  @Parameter
  short pstQueenEg22;
  @Parameter
  short pstQueenEg23;
  @Parameter
  short pstQueenEg24;
  @Parameter
  short pstQueenEg25;
  @Parameter
  short pstQueenEg26;
  @Parameter
  short pstQueenEg27;
  @Parameter
  short pstQueenEg28;
  @Parameter
  short pstQueenEg29;
  @Parameter
  short pstQueenEg30;
  @Parameter
  short pstQueenEg31;
  @Parameter
  short pstQueenEg32;
  @Parameter
  short pstQueenEg33;
  @Parameter
  short pstQueenEg34;
  @Parameter
  short pstQueenEg35;
  @Parameter
  short pstQueenEg36;
  @Parameter
  short pstQueenEg37;
  @Parameter
  short pstQueenEg38;
  @Parameter
  short pstQueenEg39;
  @Parameter
  short pstQueenEg40;
  @Parameter
  short pstQueenEg41;
  @Parameter
  short pstQueenEg42;
  @Parameter
  short pstQueenEg43;
  @Parameter
  short pstQueenEg44;
  @Parameter
  short pstQueenEg45;
  @Parameter
  short pstQueenEg46;
  @Parameter
  short pstQueenEg47;
  @Parameter
  short pstQueenEg48;
  @Parameter
  short pstQueenEg49;
  @Parameter
  short pstQueenEg50;
  @Parameter
  short pstQueenEg51;
  @Parameter
  short pstQueenEg52;
  @Parameter
  short pstQueenEg53;
  @Parameter
  short pstQueenEg54;
  @Parameter
  short pstQueenEg55;
  @Parameter
  short pstQueenEg56;
  @Parameter
  short pstQueenEg57;
  @Parameter
  short pstQueenEg58;
  @Parameter
  short pstQueenEg59;
  @Parameter
  short pstQueenEg60;
  @Parameter
  short pstQueenEg61;
  @Parameter
  short pstQueenEg62;
  @Parameter
  short pstQueenEg63;

  @Parameter
  short pstKingMg0;
  @Parameter
  short pstKingMg1;
  @Parameter
  short pstKingMg2;
  @Parameter
  short pstKingMg3;
  @Parameter
  short pstKingMg4;
  @Parameter
  short pstKingMg5;
  @Parameter
  short pstKingMg6;
  @Parameter
  short pstKingMg7;
  @Parameter
  short pstKingMg8;
  @Parameter
  short pstKingMg9;
  @Parameter
  short pstKingMg10;
  @Parameter
  short pstKingMg11;
  @Parameter
  short pstKingMg12;
  @Parameter
  short pstKingMg13;
  @Parameter
  short pstKingMg14;
  @Parameter
  short pstKingMg15;
  @Parameter
  short pstKingMg16;
  @Parameter
  short pstKingMg17;
  @Parameter
  short pstKingMg18;
  @Parameter
  short pstKingMg19;
  @Parameter
  short pstKingMg20;
  @Parameter
  short pstKingMg21;
  @Parameter
  short pstKingMg22;
  @Parameter
  short pstKingMg23;
  @Parameter
  short pstKingMg24;
  @Parameter
  short pstKingMg25;
  @Parameter
  short pstKingMg26;
  @Parameter
  short pstKingMg27;
  @Parameter
  short pstKingMg28;
  @Parameter
  short pstKingMg29;
  @Parameter
  short pstKingMg30;
  @Parameter
  short pstKingMg31;
  @Parameter
  short pstKingMg32;
  @Parameter
  short pstKingMg33;
  @Parameter
  short pstKingMg34;
  @Parameter
  short pstKingMg35;
  @Parameter
  short pstKingMg36;
  @Parameter
  short pstKingMg37;
  @Parameter
  short pstKingMg38;
  @Parameter
  short pstKingMg39;
  @Parameter
  short pstKingMg40;
  @Parameter
  short pstKingMg41;
  @Parameter
  short pstKingMg42;
  @Parameter
  short pstKingMg43;
  @Parameter
  short pstKingMg44;
  @Parameter
  short pstKingMg45;
  @Parameter
  short pstKingMg46;
  @Parameter
  short pstKingMg47;
  @Parameter
  short pstKingMg48;
  @Parameter
  short pstKingMg49;
  @Parameter
  short pstKingMg50;
  @Parameter
  short pstKingMg51;
  @Parameter
  short pstKingMg52;
  @Parameter
  short pstKingMg53;
  @Parameter
  short pstKingMg54;
  @Parameter
  short pstKingMg55;
  @Parameter
  short pstKingMg56;
  @Parameter
  short pstKingMg57;
  @Parameter
  short pstKingMg58;
  @Parameter
  short pstKingMg59;
  @Parameter
  short pstKingMg60;
  @Parameter
  short pstKingMg61;
  @Parameter
  short pstKingMg62;
  @Parameter
  short pstKingMg63;

  @Parameter
  short pstKingEg0;
  @Parameter
  short pstKingEg1;
  @Parameter
  short pstKingEg2;
  @Parameter
  short pstKingEg3;
  @Parameter
  short pstKingEg4;
  @Parameter
  short pstKingEg5;
  @Parameter
  short pstKingEg6;
  @Parameter
  short pstKingEg7;
  @Parameter
  short pstKingEg8;
  @Parameter
  short pstKingEg9;
  @Parameter
  short pstKingEg10;
  @Parameter
  short pstKingEg11;
  @Parameter
  short pstKingEg12;
  @Parameter
  short pstKingEg13;
  @Parameter
  short pstKingEg14;
  @Parameter
  short pstKingEg15;
  @Parameter
  short pstKingEg16;
  @Parameter
  short pstKingEg17;
  @Parameter
  short pstKingEg18;
  @Parameter
  short pstKingEg19;
  @Parameter
  short pstKingEg20;
  @Parameter
  short pstKingEg21;
  @Parameter
  short pstKingEg22;
  @Parameter
  short pstKingEg23;
  @Parameter
  short pstKingEg24;
  @Parameter
  short pstKingEg25;
  @Parameter
  short pstKingEg26;
  @Parameter
  short pstKingEg27;
  @Parameter
  short pstKingEg28;
  @Parameter
  short pstKingEg29;
  @Parameter
  short pstKingEg30;
  @Parameter
  short pstKingEg31;
  @Parameter
  short pstKingEg32;
  @Parameter
  short pstKingEg33;
  @Parameter
  short pstKingEg34;
  @Parameter
  short pstKingEg35;
  @Parameter
  short pstKingEg36;
  @Parameter
  short pstKingEg37;
  @Parameter
  short pstKingEg38;
  @Parameter
  short pstKingEg39;
  @Parameter
  short pstKingEg40;
  @Parameter
  short pstKingEg41;
  @Parameter
  short pstKingEg42;
  @Parameter
  short pstKingEg43;
  @Parameter
  short pstKingEg44;
  @Parameter
  short pstKingEg45;
  @Parameter
  short pstKingEg46;
  @Parameter
  short pstKingEg47;
  @Parameter
  short pstKingEg48;
  @Parameter
  short pstKingEg49;
  @Parameter
  short pstKingEg50;
  @Parameter
  short pstKingEg51;
  @Parameter
  short pstKingEg52;
  @Parameter
  short pstKingEg53;
  @Parameter
  short pstKingEg54;
  @Parameter
  short pstKingEg55;
  @Parameter
  short pstKingEg56;
  @Parameter
  short pstKingEg57;
  @Parameter
  short pstKingEg58;
  @Parameter
  short pstKingEg59;
  @Parameter
  short pstKingEg60;
  @Parameter
  short pstKingEg61;
  @Parameter
  short pstKingEg62;
  @Parameter
  short pstKingEg63;

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

  short[] getPstPawnMg() {
    return new short[]{
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

  short[] getPstPawnEg() {
    return new short[]{
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

  short[] getPstKnightMg() {
    return new short[]{
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

  short[] getPstKnightEg() {
    return new short[]{
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

  short[] getPstBishopMg() {
    return new short[]{
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

  short[] getPstBishopEg() {
    return new short[]{
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

  short[] getPstRookMg() {
    return new short[]{
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

  short[] getPstRookEg() {
    return new short[]{
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

  short[] getPstQueenMg() {
    return new short[]{
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

  short[] getPstQueenEg() {
    return new short[]{
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

  short[] getPstKingMg() {
    return new short[]{
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

  short[] getPstKingEg() {
    return new short[]{
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
