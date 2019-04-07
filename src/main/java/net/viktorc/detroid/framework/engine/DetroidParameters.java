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
  byte pstKnight0;
  @Parameter
  byte pstKnight1;
  @Parameter
  byte pstKnight2;
  @Parameter
  byte pstKnight3;
  @Parameter
  byte pstKnight8;
  @Parameter
  byte pstKnight9;
  @Parameter
  byte pstKnight10;
  @Parameter
  byte pstKnight11;
  @Parameter
  byte pstKnight16;
  @Parameter
  byte pstKnight17;
  @Parameter
  byte pstKnight18;
  @Parameter
  byte pstKnight19;
  @Parameter
  byte pstKnight24;
  @Parameter
  byte pstKnight25;
  @Parameter
  byte pstKnight26;
  @Parameter
  byte pstKnight27;
  @Parameter
  byte pstKnight32;
  @Parameter
  byte pstKnight33;
  @Parameter
  byte pstKnight34;
  @Parameter
  byte pstKnight35;
  @Parameter
  byte pstKnight40;
  @Parameter
  byte pstKnight41;
  @Parameter
  byte pstKnight42;
  @Parameter
  byte pstKnight43;
  @Parameter
  byte pstKnight48;
  @Parameter
  byte pstKnight49;
  @Parameter
  byte pstKnight50;
  @Parameter
  byte pstKnight51;
  @Parameter
  byte pstKnight56;
  @Parameter
  byte pstKnight57;
  @Parameter
  byte pstKnight58;
  @Parameter
  byte pstKnight59;

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
  byte pstRookEg8;
  @Parameter
  byte pstRookEg9;
  @Parameter
  byte pstRookEg10;
  @Parameter
  byte pstRookEg11;
  @Parameter
  byte pstRookEg16;
  @Parameter
  byte pstRookEg17;
  @Parameter
  byte pstRookEg18;
  @Parameter
  byte pstRookEg19;
  @Parameter
  byte pstRookEg24;
  @Parameter
  byte pstRookEg25;
  @Parameter
  byte pstRookEg26;
  @Parameter
  byte pstRookEg27;
  @Parameter
  byte pstRookEg32;
  @Parameter
  byte pstRookEg33;
  @Parameter
  byte pstRookEg34;
  @Parameter
  byte pstRookEg35;
  @Parameter
  byte pstRookEg40;
  @Parameter
  byte pstRookEg41;
  @Parameter
  byte pstRookEg42;
  @Parameter
  byte pstRookEg43;
  @Parameter
  byte pstRookEg48;
  @Parameter
  byte pstRookEg49;
  @Parameter
  byte pstRookEg50;
  @Parameter
  byte pstRookEg51;
  @Parameter
  byte pstRookEg56;
  @Parameter
  byte pstRookEg57;
  @Parameter
  byte pstRookEg58;
  @Parameter
  byte pstRookEg59;

  @Parameter
  byte pstQueen0;
  @Parameter
  byte pstQueen1;
  @Parameter
  byte pstQueen2;
  @Parameter
  byte pstQueen3;
  @Parameter
  byte pstQueen8;
  @Parameter
  byte pstQueen9;
  @Parameter
  byte pstQueen10;
  @Parameter
  byte pstQueen11;
  @Parameter
  byte pstQueen16;
  @Parameter
  byte pstQueen17;
  @Parameter
  byte pstQueen18;
  @Parameter
  byte pstQueen19;
  @Parameter
  byte pstQueen24;
  @Parameter
  byte pstQueen25;
  @Parameter
  byte pstQueen26;
  @Parameter
  byte pstQueen27;
  @Parameter
  byte pstQueen32;
  @Parameter
  byte pstQueen33;
  @Parameter
  byte pstQueen34;
  @Parameter
  byte pstQueen35;
  @Parameter
  byte pstQueen40;
  @Parameter
  byte pstQueen41;
  @Parameter
  byte pstQueen42;
  @Parameter
  byte pstQueen43;
  @Parameter
  byte pstQueen48;
  @Parameter
  byte pstQueen49;
  @Parameter
  byte pstQueen50;
  @Parameter
  byte pstQueen51;
  @Parameter
  byte pstQueen56;
  @Parameter
  byte pstQueen57;
  @Parameter
  byte pstQueen58;
  @Parameter
  byte pstQueen59;

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
  byte pstKingEg8;
  @Parameter
  byte pstKingEg9;
  @Parameter
  byte pstKingEg10;
  @Parameter
  byte pstKingEg11;
  @Parameter
  byte pstKingEg16;
  @Parameter
  byte pstKingEg17;
  @Parameter
  byte pstKingEg18;
  @Parameter
  byte pstKingEg19;
  @Parameter
  byte pstKingEg24;
  @Parameter
  byte pstKingEg25;
  @Parameter
  byte pstKingEg26;
  @Parameter
  byte pstKingEg27;
  @Parameter
  byte pstKingEg32;
  @Parameter
  byte pstKingEg33;
  @Parameter
  byte pstKingEg34;
  @Parameter
  byte pstKingEg35;
  @Parameter
  byte pstKingEg40;
  @Parameter
  byte pstKingEg41;
  @Parameter
  byte pstKingEg42;
  @Parameter
  byte pstKingEg43;
  @Parameter
  byte pstKingEg48;
  @Parameter
  byte pstKingEg49;
  @Parameter
  byte pstKingEg50;
  @Parameter
  byte pstKingEg51;
  @Parameter
  byte pstKingEg56;
  @Parameter
  byte pstKingEg57;
  @Parameter
  byte pstKingEg58;
  @Parameter
  byte pstKingEg59;

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

  byte[] getPstKnight() {
    return new byte[]{
        pstKnight0, pstKnight1, pstKnight2, pstKnight3, pstKnight3, pstKnight2, pstKnight1, pstKnight0,
        pstKnight8, pstKnight9, pstKnight10, pstKnight11, pstKnight11, pstKnight10, pstKnight9, pstKnight8,
        pstKnight16, pstKnight17, pstKnight18, pstKnight19, pstKnight19, pstKnight18, pstKnight17, pstKnight16,
        pstKnight24, pstKnight25, pstKnight26, pstKnight27, pstKnight27, pstKnight26, pstKnight25, pstKnight24,
        pstKnight32, pstKnight33, pstKnight34, pstKnight35, pstKnight35, pstKnight34, pstKnight33, pstKnight32,
        pstKnight40, pstKnight41, pstKnight42, pstKnight43, pstKnight43, pstKnight42, pstKnight41, pstKnight40,
        pstKnight48, pstKnight49, pstKnight50, pstKnight51, pstKnight51, pstKnight50, pstKnight49, pstKnight48,
        pstKnight56, pstKnight57, pstKnight58, pstKnight59, pstKnight59, pstKnight58, pstKnight57, pstKnight56
    };
  }

  byte[] getPstBishop() {
    return new byte[]{
        pstBishop0, pstBishop1, pstBishop2, pstBishop3, pstBishop3, pstBishop2, pstBishop1, pstBishop0,
        pstBishop8, pstBishop9, pstBishop10, pstBishop11, pstBishop11, pstBishop10, pstBishop9, pstBishop8,
        pstBishop16, pstBishop17, pstBishop18, pstBishop19, pstBishop19, pstBishop18, pstBishop17, pstBishop16,
        pstBishop24, pstBishop25, pstBishop26, pstBishop27, pstBishop27, pstBishop26, pstBishop25, pstBishop24,
        pstBishop32, pstBishop33, pstBishop34, pstBishop35, pstBishop35, pstBishop34, pstBishop33, pstBishop32,
        pstBishop40, pstBishop41, pstBishop42, pstBishop43, pstBishop43, pstBishop42, pstBishop41, pstBishop40,
        pstBishop48, pstBishop49, pstBishop50, pstBishop51, pstBishop51, pstBishop50, pstBishop49, pstBishop48,
        pstBishop56, pstBishop57, pstBishop58, pstBishop59, pstBishop59, pstBishop58, pstBishop57, pstBishop56
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
        pstRookEg0, pstRookEg1, pstRookEg2, pstRookEg3, pstRookEg3, pstRookEg2, pstRookEg1, pstRookEg0,
        pstRookEg8, pstRookEg9, pstRookEg10, pstRookEg11, pstRookEg11, pstRookEg10, pstRookEg9, pstRookEg8,
        pstRookEg16, pstRookEg17, pstRookEg18, pstRookEg19, pstRookEg19, pstRookEg18, pstRookEg17, pstRookEg16,
        pstRookEg24, pstRookEg25, pstRookEg26, pstRookEg27, pstRookEg27, pstRookEg26, pstRookEg25, pstRookEg24,
        pstRookEg32, pstRookEg33, pstRookEg34, pstRookEg35, pstRookEg35, pstRookEg34, pstRookEg33, pstRookEg32,
        pstRookEg40, pstRookEg41, pstRookEg42, pstRookEg43, pstRookEg43, pstRookEg42, pstRookEg41, pstRookEg40,
        pstRookEg48, pstRookEg49, pstRookEg50, pstRookEg51, pstRookEg51, pstRookEg50, pstRookEg49, pstRookEg48,
        pstRookEg56, pstRookEg57, pstRookEg58, pstRookEg59, pstRookEg59, pstRookEg58, pstRookEg57, pstRookEg56
    };
  }

  byte[] getPstQueen() {
    return new byte[]{
        pstQueen0, pstQueen1, pstQueen2, pstQueen3, pstQueen3, pstQueen2, pstQueen1, pstQueen0,
        pstQueen8, pstQueen9, pstQueen10, pstQueen11, pstQueen11, pstQueen10, pstQueen9, pstQueen8,
        pstQueen16, pstQueen17, pstQueen18, pstQueen19, pstQueen19, pstQueen18, pstQueen17, pstQueen16,
        pstQueen24, pstQueen25, pstQueen26, pstQueen27, pstQueen27, pstQueen26, pstQueen25, pstQueen24,
        pstQueen32, pstQueen33, pstQueen34, pstQueen35, pstQueen35, pstQueen34, pstQueen33, pstQueen32,
        pstQueen40, pstQueen41, pstQueen42, pstQueen43, pstQueen43, pstQueen42, pstQueen41, pstQueen40,
        pstQueen48, pstQueen49, pstQueen50, pstQueen51, pstQueen51, pstQueen50, pstQueen49, pstQueen48,
        pstQueen56, pstQueen57, pstQueen58, pstQueen59, pstQueen59, pstQueen58, pstQueen57, pstQueen56
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
        pstKingEg0, pstKingEg1, pstKingEg2, pstKingEg3, pstKingEg3, pstKingEg2, pstKingEg1, pstKingEg0,
        pstKingEg8, pstKingEg9, pstKingEg10, pstKingEg11, pstKingEg11, pstKingEg10, pstKingEg9, pstKingEg8,
        pstKingEg16, pstKingEg17, pstKingEg18, pstKingEg19, pstKingEg19, pstKingEg18, pstKingEg17, pstKingEg16,
        pstKingEg24, pstKingEg25, pstKingEg26, pstKingEg27, pstKingEg27, pstKingEg26, pstKingEg25, pstKingEg24,
        pstKingEg32, pstKingEg33, pstKingEg34, pstKingEg35, pstKingEg35, pstKingEg34, pstKingEg33, pstKingEg32,
        pstKingEg40, pstKingEg41, pstKingEg42, pstKingEg43, pstKingEg43, pstKingEg42, pstKingEg41, pstKingEg40,
        pstKingEg48, pstKingEg49, pstKingEg50, pstKingEg51, pstKingEg51, pstKingEg50, pstKingEg49, pstKingEg48,
        pstKingEg56, pstKingEg57, pstKingEg58, pstKingEg59, pstKingEg59, pstKingEg58, pstKingEg57, pstKingEg56
    };
  }

}
