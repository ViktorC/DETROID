package main.java.engine;

import main.java.tuning.Parameter;
import main.java.tuning.ParameterException;
import main.java.tuning.ParameterType;
import main.java.tuning.EngineParameters;

/**
 * The definitions of the evaluation, search, and hash and time management parameters used in the chess engine.
 * 
 * The suffix '_HTH' marks that the number denotes a factor of one given in hundreths and thus should be divided by 100 when used.
 * 
 * @author Viktor
 *
 */
final class Params extends EngineParameters {

	// Piece values.
	@Parameter (binaryLengthLimit = 0)
	short KING_VALUE;
	@Parameter (binaryLengthLimit = 11)
	short QUEEN_VALUE;
	@Parameter (binaryLengthLimit = 10)
	short ROOK_VALUE;
	@Parameter (binaryLengthLimit = 9)
	short BISHOP_VALUE;
	@Parameter (binaryLengthLimit = 9)
	short KNIGHT_VALUE;
	@Parameter (binaryLengthLimit = 8)
	short PAWN_VALUE;

	// Piece phase weights
	@Parameter (binaryLengthLimit = 0)
	byte KING_PHASE_WEIGHT;
	@Parameter (binaryLengthLimit = 0)
	byte QUEEN_PHASE_WEIGHT;
	@Parameter (binaryLengthLimit = 0)
	byte ROOK_PHASE_WEIGHT;
	@Parameter (binaryLengthLimit = 0)
	byte BISHOP_PHASE_WEIGHT;
	@Parameter (binaryLengthLimit = 0)
	byte KNIGHT_PHASE_WEIGHT;
	@Parameter (binaryLengthLimit = 0)
	byte PAWN_PHASE_WEIGHT;

	// Evaluation weights.
	@Parameter
	byte BLOCKED_PAWN_WEIGHT1;
	@Parameter
	byte BLOCKED_PAWN_WEIGHT2;
	@Parameter
	byte BLOCKED_PAWN_WEIGHT3;
	@Parameter
	byte KING_FRIENDLY_NORMAL_PAWN_TROPISM_WEIGHT;
	@Parameter
	byte KING_OPPONENT_NORMAL_PAWN_TROPISM_WEIGHT;
	@Parameter
	byte KING_FRIENDLY_OPEN_BACKWARD_PAWN_TROPISM_WEIGHT;
	@Parameter
	byte KING_OPPONENT_OPEN_BACKWARD_PAWN_TROPISM_WEIGHT;
	@Parameter
	byte KING_OPPONENT_PASSED_PAWN_TROPISM_WEIGHT;
	@Parameter
	byte PASSED_PAWN_WEIGHT;
	@Parameter
	byte OPEN_BACKWARD_PAWN_WEIGHT;
	@Parameter
	byte STOPPED_PAWN_WEIGHT;
	@Parameter
	byte PAWN_DEFENDED_ROOK_WEIGHT;
	@Parameter
	byte PAWN_DEFENDED_BISHOP_WEIGHT;
	@Parameter
	byte PAWN_DEFENDED_KNIGHT_WEIGHT;
	@Parameter
	byte PAWN_DEFENDED_PAWN_WEIGHT;
	@Parameter
	byte PAWN_ATTACKED_QUEEN_WEIGHT;
	@Parameter
	byte PAWN_ATTACKED_ROOK_WEIGHT;
	@Parameter
	byte PAWN_ATTACKED_BISHOP_WEIGHT;
	@Parameter
	byte PAWN_ATTACKED_KNIGHT_WEIGHT;
	@Parameter
	byte PAWN_ATTACKED_PAWN_WEIGHT;
	@Parameter
	byte PINNED_QUEEN_WEIGHT;
	@Parameter
	byte PINNED_ROOK_WEIGHT;
	@Parameter
	byte PINNED_BISHOP_WEIGHT;
	@Parameter
	byte PINNED_KNIGHT_WEIGHT;
	@Parameter
	byte ROOK_MOBILITY_WEIGHT;
	@Parameter
	byte BISHOP_MOBILITY_WEIGHT;
	@Parameter
	byte KNIGHT_MOBILITY_WEIGHT;
	@Parameter
	byte QUEEN_KING_TROPISM_WEIGHT;
	@Parameter
	byte BISHOP_PAIR_ADVANTAGE;
	@Parameter
	byte LIVE_EP_ADVANTAGE;
	@Parameter
	byte TEMPO_ADVANTAGE;

	// The margin for lazy evaluation. The extended score should be very unlikely to differ by more than this amount from the core score.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 11)
	short LAZY_EVAL_MAR;

	// Game phase intervals.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	short GAME_PHASE_OPENING_LOWER;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	short GAME_PHASE_OPENING_UPPER;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 8)
	short GAME_PHASE_ENDGAME_LOWER;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	short GAME_PHASE_ENDGAME_UPPER;

	// Search parameters.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 2)
	byte NMR; // Null move pruning reduction.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 2)
	byte LMR; // Late move reduction.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 3)
	byte LMRMSM; // Min. number of searched moves for late move reduction.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 9)
	short FMAR1; // Futility margin.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 11)
	short FMAR2; // Extended futility margin.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 11)
	short FMAR3; // Deep futility margin.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 10)
	short RMAR1; // Razoring margin.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 11)
	short RMAR2; // Deep razoring margin.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 10)
	short A_DELTA; // The aspiration delta within iterative deepening.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 10)
	short Q_DELTA; // The margin for delta-pruning in the quiescence search.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte MAX_NOMINAL_SEARCH_DEPTH; // The maximum nominal search depth.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte FULL_PLY; // For fractional ply extensions.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 4)
	byte CHECK_EXT; // Fractional check extension.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 4)
	byte RECAP_EXT; // Fractional recapture extension.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 4)
	byte SINGLE_REPLY_EXT; // Fractional single reply extension.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 4)
	byte IID_MIN_ACTIVATION_DEPTH; // The minimum depth at which IID is activated.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER)
	byte IID_REL_DEPTH_HTH; // The portion of the total depth to which the position will be searched with IID.

	// The relative history table's value depreciation factor.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte RHT_DECREMENT_FACTOR;

	// The default hash size in megabytes.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte DEFAULT_HASH_SIZE;
	// The shares of the different hash tables of the total hash size.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte TT_SHARE;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte ET_SHARE;

	// The number of turns for which the different hash table's entries are retained by default.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte TT_ENTRY_LIFECYCLE;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte ET_ENTRY_LIFECYCLE;

	// The values considered when calculating search time extensions.
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 11)
	short SCORE_FLUCTUATION_LIMIT;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER)
	byte FRACTION_OF_ORIG_SEARCH_TIME_SINCE_LAST_RESULT_CHANGE_LIMIT_HTH;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 4)
	byte RESULT_CHANGES_PER_DEPTH_LIMIT;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER, binaryLengthLimit = 0)
	byte AVG_MOVES_PER_GAME;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER)
	byte MOVES_TO_GO_SAFETY_MARGIN;
	@Parameter (type = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER)
	byte FRACTION_OF_TOTAL_TIME_TO_USE_HTH;

	// Piece-square tables for openings and end games.
	@Parameter
	byte PST_PAWN_OPENING_8;
	@Parameter
	byte PST_PAWN_OPENING_9;
	@Parameter
	byte PST_PAWN_OPENING_10;
	@Parameter
	byte PST_PAWN_OPENING_11;
	@Parameter
	byte PST_PAWN_OPENING_12;
	@Parameter
	byte PST_PAWN_OPENING_13;
	@Parameter
	byte PST_PAWN_OPENING_14;
	@Parameter
	byte PST_PAWN_OPENING_15;
	@Parameter
	byte PST_PAWN_OPENING_16;
	@Parameter
	byte PST_PAWN_OPENING_17;
	@Parameter
	byte PST_PAWN_OPENING_18;
	@Parameter
	byte PST_PAWN_OPENING_19;
	@Parameter
	byte PST_PAWN_OPENING_20;
	@Parameter
	byte PST_PAWN_OPENING_21;
	@Parameter
	byte PST_PAWN_OPENING_22;
	@Parameter
	byte PST_PAWN_OPENING_23;
	@Parameter
	byte PST_PAWN_OPENING_24;
	@Parameter
	byte PST_PAWN_OPENING_25;
	@Parameter
	byte PST_PAWN_OPENING_26;
	@Parameter
	byte PST_PAWN_OPENING_27;
	@Parameter
	byte PST_PAWN_OPENING_28;
	@Parameter
	byte PST_PAWN_OPENING_29;
	@Parameter
	byte PST_PAWN_OPENING_30;
	@Parameter
	byte PST_PAWN_OPENING_31;
	@Parameter
	byte PST_PAWN_OPENING_32;
	@Parameter
	byte PST_PAWN_OPENING_33;
	@Parameter
	byte PST_PAWN_OPENING_34;
	@Parameter
	byte PST_PAWN_OPENING_35;
	@Parameter
	byte PST_PAWN_OPENING_36;
	@Parameter
	byte PST_PAWN_OPENING_37;
	@Parameter
	byte PST_PAWN_OPENING_38;
	@Parameter
	byte PST_PAWN_OPENING_39;
	@Parameter
	byte PST_PAWN_OPENING_40;
	@Parameter
	byte PST_PAWN_OPENING_41;
	@Parameter
	byte PST_PAWN_OPENING_42;
	@Parameter
	byte PST_PAWN_OPENING_43;
	@Parameter
	byte PST_PAWN_OPENING_44;
	@Parameter
	byte PST_PAWN_OPENING_45;
	@Parameter
	byte PST_PAWN_OPENING_46;
	@Parameter
	byte PST_PAWN_OPENING_47;
	@Parameter
	byte PST_PAWN_OPENING_48;
	@Parameter
	byte PST_PAWN_OPENING_49;
	@Parameter
	byte PST_PAWN_OPENING_50;
	@Parameter
	byte PST_PAWN_OPENING_51;
	@Parameter
	byte PST_PAWN_OPENING_52;
	@Parameter
	byte PST_PAWN_OPENING_53;
	@Parameter
	byte PST_PAWN_OPENING_54;
	@Parameter
	byte PST_PAWN_OPENING_55;

	@Parameter
	byte PST_PAWN_ENDGAME_8;
	@Parameter
	byte PST_PAWN_ENDGAME_9;
	@Parameter
	byte PST_PAWN_ENDGAME_10;
	@Parameter
	byte PST_PAWN_ENDGAME_11;
	@Parameter
	byte PST_PAWN_ENDGAME_12;
	@Parameter
	byte PST_PAWN_ENDGAME_13;
	@Parameter
	byte PST_PAWN_ENDGAME_14;
	@Parameter
	byte PST_PAWN_ENDGAME_15;
	@Parameter
	byte PST_PAWN_ENDGAME_16;
	@Parameter
	byte PST_PAWN_ENDGAME_17;
	@Parameter
	byte PST_PAWN_ENDGAME_18;
	@Parameter
	byte PST_PAWN_ENDGAME_19;
	@Parameter
	byte PST_PAWN_ENDGAME_20;
	@Parameter
	byte PST_PAWN_ENDGAME_21;
	@Parameter
	byte PST_PAWN_ENDGAME_22;
	@Parameter
	byte PST_PAWN_ENDGAME_23;
	@Parameter
	byte PST_PAWN_ENDGAME_24;
	@Parameter
	byte PST_PAWN_ENDGAME_25;
	@Parameter
	byte PST_PAWN_ENDGAME_26;
	@Parameter
	byte PST_PAWN_ENDGAME_27;
	@Parameter
	byte PST_PAWN_ENDGAME_28;
	@Parameter
	byte PST_PAWN_ENDGAME_29;
	@Parameter
	byte PST_PAWN_ENDGAME_30;
	@Parameter
	byte PST_PAWN_ENDGAME_31;
	@Parameter
	byte PST_PAWN_ENDGAME_32;
	@Parameter
	byte PST_PAWN_ENDGAME_33;
	@Parameter
	byte PST_PAWN_ENDGAME_34;
	@Parameter
	byte PST_PAWN_ENDGAME_35;
	@Parameter
	byte PST_PAWN_ENDGAME_36;
	@Parameter
	byte PST_PAWN_ENDGAME_37;
	@Parameter
	byte PST_PAWN_ENDGAME_38;
	@Parameter
	byte PST_PAWN_ENDGAME_39;
	@Parameter
	byte PST_PAWN_ENDGAME_40;
	@Parameter
	byte PST_PAWN_ENDGAME_41;
	@Parameter
	byte PST_PAWN_ENDGAME_42;
	@Parameter
	byte PST_PAWN_ENDGAME_43;
	@Parameter
	byte PST_PAWN_ENDGAME_44;
	@Parameter
	byte PST_PAWN_ENDGAME_45;
	@Parameter
	byte PST_PAWN_ENDGAME_46;
	@Parameter
	byte PST_PAWN_ENDGAME_47;
	@Parameter
	byte PST_PAWN_ENDGAME_48;
	@Parameter
	byte PST_PAWN_ENDGAME_49;
	@Parameter
	byte PST_PAWN_ENDGAME_50;
	@Parameter
	byte PST_PAWN_ENDGAME_51;
	@Parameter
	byte PST_PAWN_ENDGAME_52;
	@Parameter
	byte PST_PAWN_ENDGAME_53;
	@Parameter
	byte PST_PAWN_ENDGAME_54;
	@Parameter
	byte PST_PAWN_ENDGAME_55;

	@Parameter
	byte PST_KNIGHT_OPENING_0;
	@Parameter
	byte PST_KNIGHT_OPENING_1;
	@Parameter
	byte PST_KNIGHT_OPENING_2;
	@Parameter
	byte PST_KNIGHT_OPENING_3;
	@Parameter
	byte PST_KNIGHT_OPENING_8;
	@Parameter
	byte PST_KNIGHT_OPENING_9;
	@Parameter
	byte PST_KNIGHT_OPENING_10;
	@Parameter
	byte PST_KNIGHT_OPENING_11;
	@Parameter
	byte PST_KNIGHT_OPENING_16;
	@Parameter
	byte PST_KNIGHT_OPENING_17;
	@Parameter
	byte PST_KNIGHT_OPENING_18;
	@Parameter
	byte PST_KNIGHT_OPENING_19;
	@Parameter
	byte PST_KNIGHT_OPENING_24;
	@Parameter
	byte PST_KNIGHT_OPENING_25;
	@Parameter
	byte PST_KNIGHT_OPENING_26;
	@Parameter
	byte PST_KNIGHT_OPENING_27;
	@Parameter
	byte PST_KNIGHT_OPENING_32;
	@Parameter
	byte PST_KNIGHT_OPENING_33;
	@Parameter
	byte PST_KNIGHT_OPENING_34;
	@Parameter
	byte PST_KNIGHT_OPENING_35;
	@Parameter
	byte PST_KNIGHT_OPENING_40;
	@Parameter
	byte PST_KNIGHT_OPENING_41;
	@Parameter
	byte PST_KNIGHT_OPENING_42;
	@Parameter
	byte PST_KNIGHT_OPENING_43;
	@Parameter
	byte PST_KNIGHT_OPENING_48;
	@Parameter
	byte PST_KNIGHT_OPENING_49;
	@Parameter
	byte PST_KNIGHT_OPENING_50;
	@Parameter
	byte PST_KNIGHT_OPENING_51;
	@Parameter
	byte PST_KNIGHT_OPENING_56;
	@Parameter
	byte PST_KNIGHT_OPENING_57;
	@Parameter
	byte PST_KNIGHT_OPENING_58;
	@Parameter
	byte PST_KNIGHT_OPENING_59;

	@Parameter
	byte PST_KNIGHT_ENDGAME_0;
	@Parameter
	byte PST_KNIGHT_ENDGAME_1;
	@Parameter
	byte PST_KNIGHT_ENDGAME_2;
	@Parameter
	byte PST_KNIGHT_ENDGAME_3;
	@Parameter
	byte PST_KNIGHT_ENDGAME_8;
	@Parameter
	byte PST_KNIGHT_ENDGAME_9;
	@Parameter
	byte PST_KNIGHT_ENDGAME_10;
	@Parameter
	byte PST_KNIGHT_ENDGAME_11;
	@Parameter
	byte PST_KNIGHT_ENDGAME_16;
	@Parameter
	byte PST_KNIGHT_ENDGAME_17;
	@Parameter
	byte PST_KNIGHT_ENDGAME_18;
	@Parameter
	byte PST_KNIGHT_ENDGAME_19;
	@Parameter
	byte PST_KNIGHT_ENDGAME_24;
	@Parameter
	byte PST_KNIGHT_ENDGAME_25;
	@Parameter
	byte PST_KNIGHT_ENDGAME_26;
	@Parameter
	byte PST_KNIGHT_ENDGAME_27;
	@Parameter
	byte PST_KNIGHT_ENDGAME_32;
	@Parameter
	byte PST_KNIGHT_ENDGAME_33;
	@Parameter
	byte PST_KNIGHT_ENDGAME_34;
	@Parameter
	byte PST_KNIGHT_ENDGAME_35;
	@Parameter
	byte PST_KNIGHT_ENDGAME_40;
	@Parameter
	byte PST_KNIGHT_ENDGAME_41;
	@Parameter
	byte PST_KNIGHT_ENDGAME_42;
	@Parameter
	byte PST_KNIGHT_ENDGAME_43;
	@Parameter
	byte PST_KNIGHT_ENDGAME_48;
	@Parameter
	byte PST_KNIGHT_ENDGAME_49;
	@Parameter
	byte PST_KNIGHT_ENDGAME_50;
	@Parameter
	byte PST_KNIGHT_ENDGAME_51;
	@Parameter
	byte PST_KNIGHT_ENDGAME_56;
	@Parameter
	byte PST_KNIGHT_ENDGAME_57;
	@Parameter
	byte PST_KNIGHT_ENDGAME_58;
	@Parameter
	byte PST_KNIGHT_ENDGAME_59;

	@Parameter
	byte PST_BISHOP_0;
	@Parameter
	byte PST_BISHOP_1;
	@Parameter
	byte PST_BISHOP_2;
	@Parameter
	byte PST_BISHOP_3;
	@Parameter
	byte PST_BISHOP_8;
	@Parameter
	byte PST_BISHOP_9;
	@Parameter
	byte PST_BISHOP_10;
	@Parameter
	byte PST_BISHOP_11;
	@Parameter
	byte PST_BISHOP_16;
	@Parameter
	byte PST_BISHOP_17;
	@Parameter
	byte PST_BISHOP_18;
	@Parameter
	byte PST_BISHOP_19;
	@Parameter
	byte PST_BISHOP_24;
	@Parameter
	byte PST_BISHOP_25;
	@Parameter
	byte PST_BISHOP_26;
	@Parameter
	byte PST_BISHOP_27;
	@Parameter
	byte PST_BISHOP_32;
	@Parameter
	byte PST_BISHOP_33;
	@Parameter
	byte PST_BISHOP_34;
	@Parameter
	byte PST_BISHOP_35;
	@Parameter
	byte PST_BISHOP_40;
	@Parameter
	byte PST_BISHOP_41;
	@Parameter
	byte PST_BISHOP_42;
	@Parameter
	byte PST_BISHOP_43;
	@Parameter
	byte PST_BISHOP_48;
	@Parameter
	byte PST_BISHOP_49;
	@Parameter
	byte PST_BISHOP_50;
	@Parameter
	byte PST_BISHOP_51;
	@Parameter
	byte PST_BISHOP_56;
	@Parameter
	byte PST_BISHOP_57;
	@Parameter
	byte PST_BISHOP_58;
	@Parameter
	byte PST_BISHOP_59;

	@Parameter
	byte PST_ROOK_OPENING_0;
	@Parameter
	byte PST_ROOK_OPENING_1;
	@Parameter
	byte PST_ROOK_OPENING_2;
	@Parameter
	byte PST_ROOK_OPENING_3;
	@Parameter
	byte PST_ROOK_OPENING_4;
	@Parameter
	byte PST_ROOK_OPENING_5;
	@Parameter
	byte PST_ROOK_OPENING_6;
	@Parameter
	byte PST_ROOK_OPENING_7;
	@Parameter
	byte PST_ROOK_OPENING_8;
	@Parameter
	byte PST_ROOK_OPENING_9;
	@Parameter
	byte PST_ROOK_OPENING_10;
	@Parameter
	byte PST_ROOK_OPENING_11;
	@Parameter
	byte PST_ROOK_OPENING_12;
	@Parameter
	byte PST_ROOK_OPENING_13;
	@Parameter
	byte PST_ROOK_OPENING_14;
	@Parameter
	byte PST_ROOK_OPENING_15;
	@Parameter
	byte PST_ROOK_OPENING_16;
	@Parameter
	byte PST_ROOK_OPENING_17;
	@Parameter
	byte PST_ROOK_OPENING_18;
	@Parameter
	byte PST_ROOK_OPENING_19;
	@Parameter
	byte PST_ROOK_OPENING_20;
	@Parameter
	byte PST_ROOK_OPENING_21;
	@Parameter
	byte PST_ROOK_OPENING_22;
	@Parameter
	byte PST_ROOK_OPENING_23;
	@Parameter
	byte PST_ROOK_OPENING_24;
	@Parameter
	byte PST_ROOK_OPENING_25;
	@Parameter
	byte PST_ROOK_OPENING_26;
	@Parameter
	byte PST_ROOK_OPENING_27;
	@Parameter
	byte PST_ROOK_OPENING_28;
	@Parameter
	byte PST_ROOK_OPENING_29;
	@Parameter
	byte PST_ROOK_OPENING_30;
	@Parameter
	byte PST_ROOK_OPENING_31;
	@Parameter
	byte PST_ROOK_OPENING_32;
	@Parameter
	byte PST_ROOK_OPENING_33;
	@Parameter
	byte PST_ROOK_OPENING_34;
	@Parameter
	byte PST_ROOK_OPENING_35;
	@Parameter
	byte PST_ROOK_OPENING_36;
	@Parameter
	byte PST_ROOK_OPENING_37;
	@Parameter
	byte PST_ROOK_OPENING_38;
	@Parameter
	byte PST_ROOK_OPENING_39;
	@Parameter
	byte PST_ROOK_OPENING_40;
	@Parameter
	byte PST_ROOK_OPENING_41;
	@Parameter
	byte PST_ROOK_OPENING_42;
	@Parameter
	byte PST_ROOK_OPENING_43;
	@Parameter
	byte PST_ROOK_OPENING_44;
	@Parameter
	byte PST_ROOK_OPENING_45;
	@Parameter
	byte PST_ROOK_OPENING_46;
	@Parameter
	byte PST_ROOK_OPENING_47;
	@Parameter
	byte PST_ROOK_OPENING_48;
	@Parameter
	byte PST_ROOK_OPENING_49;
	@Parameter
	byte PST_ROOK_OPENING_50;
	@Parameter
	byte PST_ROOK_OPENING_51;
	@Parameter
	byte PST_ROOK_OPENING_52;
	@Parameter
	byte PST_ROOK_OPENING_53;
	@Parameter
	byte PST_ROOK_OPENING_54;
	@Parameter
	byte PST_ROOK_OPENING_55;
	@Parameter
	byte PST_ROOK_OPENING_56;
	@Parameter
	byte PST_ROOK_OPENING_57;
	@Parameter
	byte PST_ROOK_OPENING_58;
	@Parameter
	byte PST_ROOK_OPENING_59;
	@Parameter
	byte PST_ROOK_OPENING_60;
	@Parameter
	byte PST_ROOK_OPENING_61;
	@Parameter
	byte PST_ROOK_OPENING_62;
	@Parameter
	byte PST_ROOK_OPENING_63;

	@Parameter
	byte PST_ROOK_ENDGAME_0;
	@Parameter
	byte PST_ROOK_ENDGAME_1;
	@Parameter
	byte PST_ROOK_ENDGAME_2;
	@Parameter
	byte PST_ROOK_ENDGAME_3;
	@Parameter
	byte PST_ROOK_ENDGAME_4;
	@Parameter
	byte PST_ROOK_ENDGAME_5;
	@Parameter
	byte PST_ROOK_ENDGAME_6;
	@Parameter
	byte PST_ROOK_ENDGAME_7;
	@Parameter
	byte PST_ROOK_ENDGAME_8;
	@Parameter
	byte PST_ROOK_ENDGAME_9;
	@Parameter
	byte PST_ROOK_ENDGAME_10;
	@Parameter
	byte PST_ROOK_ENDGAME_11;
	@Parameter
	byte PST_ROOK_ENDGAME_12;
	@Parameter
	byte PST_ROOK_ENDGAME_13;
	@Parameter
	byte PST_ROOK_ENDGAME_14;
	@Parameter
	byte PST_ROOK_ENDGAME_15;
	@Parameter
	byte PST_ROOK_ENDGAME_16;
	@Parameter
	byte PST_ROOK_ENDGAME_17;
	@Parameter
	byte PST_ROOK_ENDGAME_18;
	@Parameter
	byte PST_ROOK_ENDGAME_19;
	@Parameter
	byte PST_ROOK_ENDGAME_20;
	@Parameter
	byte PST_ROOK_ENDGAME_21;
	@Parameter
	byte PST_ROOK_ENDGAME_22;
	@Parameter
	byte PST_ROOK_ENDGAME_23;
	@Parameter
	byte PST_ROOK_ENDGAME_24;
	@Parameter
	byte PST_ROOK_ENDGAME_25;
	@Parameter
	byte PST_ROOK_ENDGAME_26;
	@Parameter
	byte PST_ROOK_ENDGAME_27;
	@Parameter
	byte PST_ROOK_ENDGAME_28;
	@Parameter
	byte PST_ROOK_ENDGAME_29;
	@Parameter
	byte PST_ROOK_ENDGAME_30;
	@Parameter
	byte PST_ROOK_ENDGAME_31;
	@Parameter
	byte PST_ROOK_ENDGAME_32;
	@Parameter
	byte PST_ROOK_ENDGAME_33;
	@Parameter
	byte PST_ROOK_ENDGAME_34;
	@Parameter
	byte PST_ROOK_ENDGAME_35;
	@Parameter
	byte PST_ROOK_ENDGAME_36;
	@Parameter
	byte PST_ROOK_ENDGAME_37;
	@Parameter
	byte PST_ROOK_ENDGAME_38;
	@Parameter
	byte PST_ROOK_ENDGAME_39;
	@Parameter
	byte PST_ROOK_ENDGAME_40;
	@Parameter
	byte PST_ROOK_ENDGAME_41;
	@Parameter
	byte PST_ROOK_ENDGAME_42;
	@Parameter
	byte PST_ROOK_ENDGAME_43;
	@Parameter
	byte PST_ROOK_ENDGAME_44;
	@Parameter
	byte PST_ROOK_ENDGAME_45;
	@Parameter
	byte PST_ROOK_ENDGAME_46;
	@Parameter
	byte PST_ROOK_ENDGAME_47;
	@Parameter
	byte PST_ROOK_ENDGAME_48;
	@Parameter
	byte PST_ROOK_ENDGAME_49;
	@Parameter
	byte PST_ROOK_ENDGAME_50;
	@Parameter
	byte PST_ROOK_ENDGAME_51;
	@Parameter
	byte PST_ROOK_ENDGAME_52;
	@Parameter
	byte PST_ROOK_ENDGAME_53;
	@Parameter
	byte PST_ROOK_ENDGAME_54;
	@Parameter
	byte PST_ROOK_ENDGAME_55;
	@Parameter
	byte PST_ROOK_ENDGAME_56;
	@Parameter
	byte PST_ROOK_ENDGAME_57;
	@Parameter
	byte PST_ROOK_ENDGAME_58;
	@Parameter
	byte PST_ROOK_ENDGAME_59;
	@Parameter
	byte PST_ROOK_ENDGAME_60;
	@Parameter
	byte PST_ROOK_ENDGAME_61;
	@Parameter
	byte PST_ROOK_ENDGAME_62;
	@Parameter
	byte PST_ROOK_ENDGAME_63;

	@Parameter
	byte PST_QUEEN_0;
	@Parameter
	byte PST_QUEEN_1;
	@Parameter
	byte PST_QUEEN_2;
	@Parameter
	byte PST_QUEEN_3;
	@Parameter
	byte PST_QUEEN_4;
	@Parameter
	byte PST_QUEEN_5;
	@Parameter
	byte PST_QUEEN_6;
	@Parameter
	byte PST_QUEEN_7;
	@Parameter
	byte PST_QUEEN_8;
	@Parameter
	byte PST_QUEEN_9;
	@Parameter
	byte PST_QUEEN_10;
	@Parameter
	byte PST_QUEEN_11;
	@Parameter
	byte PST_QUEEN_12;
	@Parameter
	byte PST_QUEEN_13;
	@Parameter
	byte PST_QUEEN_14;
	@Parameter
	byte PST_QUEEN_15;
	@Parameter
	byte PST_QUEEN_16;
	@Parameter
	byte PST_QUEEN_17;
	@Parameter
	byte PST_QUEEN_18;
	@Parameter
	byte PST_QUEEN_19;
	@Parameter
	byte PST_QUEEN_20;
	@Parameter
	byte PST_QUEEN_21;
	@Parameter
	byte PST_QUEEN_22;
	@Parameter
	byte PST_QUEEN_23;
	@Parameter
	byte PST_QUEEN_24;
	@Parameter
	byte PST_QUEEN_25;
	@Parameter
	byte PST_QUEEN_26;
	@Parameter
	byte PST_QUEEN_27;
	@Parameter
	byte PST_QUEEN_28;
	@Parameter
	byte PST_QUEEN_29;
	@Parameter
	byte PST_QUEEN_30;
	@Parameter
	byte PST_QUEEN_31;
	@Parameter
	byte PST_QUEEN_32;
	@Parameter
	byte PST_QUEEN_33;
	@Parameter
	byte PST_QUEEN_34;
	@Parameter
	byte PST_QUEEN_35;
	@Parameter
	byte PST_QUEEN_36;
	@Parameter
	byte PST_QUEEN_37;
	@Parameter
	byte PST_QUEEN_38;
	@Parameter
	byte PST_QUEEN_39;
	@Parameter
	byte PST_QUEEN_40;
	@Parameter
	byte PST_QUEEN_41;
	@Parameter
	byte PST_QUEEN_42;
	@Parameter
	byte PST_QUEEN_43;
	@Parameter
	byte PST_QUEEN_44;
	@Parameter
	byte PST_QUEEN_45;
	@Parameter
	byte PST_QUEEN_46;
	@Parameter
	byte PST_QUEEN_47;
	@Parameter
	byte PST_QUEEN_48;
	@Parameter
	byte PST_QUEEN_49;
	@Parameter
	byte PST_QUEEN_50;
	@Parameter
	byte PST_QUEEN_51;
	@Parameter
	byte PST_QUEEN_52;
	@Parameter
	byte PST_QUEEN_53;
	@Parameter
	byte PST_QUEEN_54;
	@Parameter
	byte PST_QUEEN_55;
	@Parameter
	byte PST_QUEEN_56;
	@Parameter
	byte PST_QUEEN_57;
	@Parameter
	byte PST_QUEEN_58;
	@Parameter
	byte PST_QUEEN_59;
	@Parameter
	byte PST_QUEEN_60;
	@Parameter
	byte PST_QUEEN_61;
	@Parameter
	byte PST_QUEEN_62;
	@Parameter
	byte PST_QUEEN_63;

	@Parameter
	byte PST_KING_OPENING_0;
	@Parameter
	byte PST_KING_OPENING_1;
	@Parameter
	byte PST_KING_OPENING_2;
	@Parameter
	byte PST_KING_OPENING_3;
	@Parameter
	byte PST_KING_OPENING_4;
	@Parameter
	byte PST_KING_OPENING_5;
	@Parameter
	byte PST_KING_OPENING_6;
	@Parameter
	byte PST_KING_OPENING_7;
	@Parameter
	byte PST_KING_OPENING_8;
	@Parameter
	byte PST_KING_OPENING_9;
	@Parameter
	byte PST_KING_OPENING_10;
	@Parameter
	byte PST_KING_OPENING_11;
	@Parameter
	byte PST_KING_OPENING_12;
	@Parameter
	byte PST_KING_OPENING_13;
	@Parameter
	byte PST_KING_OPENING_14;
	@Parameter
	byte PST_KING_OPENING_15;
	@Parameter
	byte PST_KING_OPENING_16;
	@Parameter
	byte PST_KING_OPENING_17;
	@Parameter
	byte PST_KING_OPENING_18;
	@Parameter
	byte PST_KING_OPENING_19;
	@Parameter
	byte PST_KING_OPENING_20;
	@Parameter
	byte PST_KING_OPENING_21;
	@Parameter
	byte PST_KING_OPENING_22;
	@Parameter
	byte PST_KING_OPENING_23;
	@Parameter
	byte PST_KING_OPENING_24;
	@Parameter
	byte PST_KING_OPENING_25;
	@Parameter
	byte PST_KING_OPENING_26;
	@Parameter
	byte PST_KING_OPENING_27;
	@Parameter
	byte PST_KING_OPENING_28;
	@Parameter
	byte PST_KING_OPENING_29;
	@Parameter
	byte PST_KING_OPENING_30;
	@Parameter
	byte PST_KING_OPENING_31;
	@Parameter
	byte PST_KING_OPENING_32;
	@Parameter
	byte PST_KING_OPENING_33;
	@Parameter
	byte PST_KING_OPENING_34;
	@Parameter
	byte PST_KING_OPENING_35;
	@Parameter
	byte PST_KING_OPENING_36;
	@Parameter
	byte PST_KING_OPENING_37;
	@Parameter
	byte PST_KING_OPENING_38;
	@Parameter
	byte PST_KING_OPENING_39;
	@Parameter
	byte PST_KING_OPENING_40;
	@Parameter
	byte PST_KING_OPENING_41;
	@Parameter
	byte PST_KING_OPENING_42;
	@Parameter
	byte PST_KING_OPENING_43;
	@Parameter
	byte PST_KING_OPENING_44;
	@Parameter
	byte PST_KING_OPENING_45;
	@Parameter
	byte PST_KING_OPENING_46;
	@Parameter
	byte PST_KING_OPENING_47;
	@Parameter
	byte PST_KING_OPENING_48;
	@Parameter
	byte PST_KING_OPENING_49;
	@Parameter
	byte PST_KING_OPENING_50;
	@Parameter
	byte PST_KING_OPENING_51;
	@Parameter
	byte PST_KING_OPENING_52;
	@Parameter
	byte PST_KING_OPENING_53;
	@Parameter
	byte PST_KING_OPENING_54;
	@Parameter
	byte PST_KING_OPENING_55;
	@Parameter
	byte PST_KING_OPENING_56;
	@Parameter
	byte PST_KING_OPENING_57;
	@Parameter
	byte PST_KING_OPENING_58;
	@Parameter
	byte PST_KING_OPENING_59;
	@Parameter
	byte PST_KING_OPENING_60;
	@Parameter
	byte PST_KING_OPENING_61;
	@Parameter
	byte PST_KING_OPENING_62;
	@Parameter
	byte PST_KING_OPENING_63;

	@Parameter
	byte PST_KING_ENDGAME_0;
	@Parameter
	byte PST_KING_ENDGAME_1;
	@Parameter
	byte PST_KING_ENDGAME_2;
	@Parameter
	byte PST_KING_ENDGAME_3;
	@Parameter
	byte PST_KING_ENDGAME_4;
	@Parameter
	byte PST_KING_ENDGAME_5;
	@Parameter
	byte PST_KING_ENDGAME_6;
	@Parameter
	byte PST_KING_ENDGAME_7;
	@Parameter
	byte PST_KING_ENDGAME_8;
	@Parameter
	byte PST_KING_ENDGAME_9;
	@Parameter
	byte PST_KING_ENDGAME_10;
	@Parameter
	byte PST_KING_ENDGAME_11;
	@Parameter
	byte PST_KING_ENDGAME_12;
	@Parameter
	byte PST_KING_ENDGAME_13;
	@Parameter
	byte PST_KING_ENDGAME_14;
	@Parameter
	byte PST_KING_ENDGAME_15;
	@Parameter
	byte PST_KING_ENDGAME_16;
	@Parameter
	byte PST_KING_ENDGAME_17;
	@Parameter
	byte PST_KING_ENDGAME_18;
	@Parameter
	byte PST_KING_ENDGAME_19;
	@Parameter
	byte PST_KING_ENDGAME_20;
	@Parameter
	byte PST_KING_ENDGAME_21;
	@Parameter
	byte PST_KING_ENDGAME_22;
	@Parameter
	byte PST_KING_ENDGAME_23;
	@Parameter
	byte PST_KING_ENDGAME_24;
	@Parameter
	byte PST_KING_ENDGAME_25;
	@Parameter
	byte PST_KING_ENDGAME_26;
	@Parameter
	byte PST_KING_ENDGAME_27;
	@Parameter
	byte PST_KING_ENDGAME_28;
	@Parameter
	byte PST_KING_ENDGAME_29;
	@Parameter
	byte PST_KING_ENDGAME_30;
	@Parameter
	byte PST_KING_ENDGAME_31;
	@Parameter
	byte PST_KING_ENDGAME_32;
	@Parameter
	byte PST_KING_ENDGAME_33;
	@Parameter
	byte PST_KING_ENDGAME_34;
	@Parameter
	byte PST_KING_ENDGAME_35;
	@Parameter
	byte PST_KING_ENDGAME_36;
	@Parameter
	byte PST_KING_ENDGAME_37;
	@Parameter
	byte PST_KING_ENDGAME_38;
	@Parameter
	byte PST_KING_ENDGAME_39;
	@Parameter
	byte PST_KING_ENDGAME_40;
	@Parameter
	byte PST_KING_ENDGAME_41;
	@Parameter
	byte PST_KING_ENDGAME_42;
	@Parameter
	byte PST_KING_ENDGAME_43;
	@Parameter
	byte PST_KING_ENDGAME_44;
	@Parameter
	byte PST_KING_ENDGAME_45;
	@Parameter
	byte PST_KING_ENDGAME_46;
	@Parameter
	byte PST_KING_ENDGAME_47;
	@Parameter
	byte PST_KING_ENDGAME_48;
	@Parameter
	byte PST_KING_ENDGAME_49;
	@Parameter
	byte PST_KING_ENDGAME_50;
	@Parameter
	byte PST_KING_ENDGAME_51;
	@Parameter
	byte PST_KING_ENDGAME_52;
	@Parameter
	byte PST_KING_ENDGAME_53;
	@Parameter
	byte PST_KING_ENDGAME_54;
	@Parameter
	byte PST_KING_ENDGAME_55;
	@Parameter
	byte PST_KING_ENDGAME_56;
	@Parameter
	byte PST_KING_ENDGAME_57;
	@Parameter
	byte PST_KING_ENDGAME_58;
	@Parameter
	byte PST_KING_ENDGAME_59;
	@Parameter
	byte PST_KING_ENDGAME_60;
	@Parameter
	byte PST_KING_ENDGAME_61;
	@Parameter
	byte PST_KING_ENDGAME_62;
	@Parameter
	byte PST_KING_ENDGAME_63;
	
	/**
	 * Constructs an unitialized instance.
	 * 
	 * @throws ParameterException If a static or non-primitive field is annotated as {@link #Parameter Parameter}.
	 */
	Params() throws ParameterException {
		super();
	}
	byte[] getPST_PAWN_OPENING() {
		return new byte[] {
				0, 0, 0, 0, 0, 0, 0, 0, PST_PAWN_OPENING_8, PST_PAWN_OPENING_9,
				PST_PAWN_OPENING_10, PST_PAWN_OPENING_11, PST_PAWN_OPENING_12, PST_PAWN_OPENING_13, PST_PAWN_OPENING_14,
				PST_PAWN_OPENING_15, PST_PAWN_OPENING_16, PST_PAWN_OPENING_17, PST_PAWN_OPENING_18, PST_PAWN_OPENING_19,
				PST_PAWN_OPENING_20, PST_PAWN_OPENING_21, PST_PAWN_OPENING_22, PST_PAWN_OPENING_23, PST_PAWN_OPENING_24,
				PST_PAWN_OPENING_25, PST_PAWN_OPENING_26, PST_PAWN_OPENING_27, PST_PAWN_OPENING_28, PST_PAWN_OPENING_29,
				PST_PAWN_OPENING_30, PST_PAWN_OPENING_31, PST_PAWN_OPENING_32, PST_PAWN_OPENING_33, PST_PAWN_OPENING_34,
				PST_PAWN_OPENING_35, PST_PAWN_OPENING_36, PST_PAWN_OPENING_37, PST_PAWN_OPENING_38, PST_PAWN_OPENING_39,
				PST_PAWN_OPENING_40, PST_PAWN_OPENING_41, PST_PAWN_OPENING_42, PST_PAWN_OPENING_43, PST_PAWN_OPENING_44,
				PST_PAWN_OPENING_45, PST_PAWN_OPENING_46, PST_PAWN_OPENING_47, PST_PAWN_OPENING_48, PST_PAWN_OPENING_49,
				PST_PAWN_OPENING_50, PST_PAWN_OPENING_51, PST_PAWN_OPENING_52, PST_PAWN_OPENING_53, PST_PAWN_OPENING_54,
				PST_PAWN_OPENING_55, 0, 0, 0, 0, 0, 0, 0, 0 };
	}
	byte[] getPST_PAWN_ENDGAME() {
		return new byte[] {
				0, 0, 0, 0, 0, 0, 0, 0, PST_PAWN_ENDGAME_8, PST_PAWN_ENDGAME_9,
				PST_PAWN_ENDGAME_10, PST_PAWN_ENDGAME_11, PST_PAWN_ENDGAME_12, PST_PAWN_ENDGAME_13, PST_PAWN_ENDGAME_14,
				PST_PAWN_ENDGAME_15, PST_PAWN_ENDGAME_16, PST_PAWN_ENDGAME_17, PST_PAWN_ENDGAME_18, PST_PAWN_ENDGAME_19,
				PST_PAWN_ENDGAME_20, PST_PAWN_ENDGAME_21, PST_PAWN_ENDGAME_22, PST_PAWN_ENDGAME_23, PST_PAWN_ENDGAME_24,
				PST_PAWN_ENDGAME_25, PST_PAWN_ENDGAME_26, PST_PAWN_ENDGAME_27, PST_PAWN_ENDGAME_28, PST_PAWN_ENDGAME_29,
				PST_PAWN_ENDGAME_30, PST_PAWN_ENDGAME_31, PST_PAWN_ENDGAME_32, PST_PAWN_ENDGAME_33, PST_PAWN_ENDGAME_34,
				PST_PAWN_ENDGAME_35, PST_PAWN_ENDGAME_36, PST_PAWN_ENDGAME_37, PST_PAWN_ENDGAME_38, PST_PAWN_ENDGAME_39,
				PST_PAWN_ENDGAME_40, PST_PAWN_ENDGAME_41, PST_PAWN_ENDGAME_42, PST_PAWN_ENDGAME_43, PST_PAWN_ENDGAME_44,
				PST_PAWN_ENDGAME_45, PST_PAWN_ENDGAME_46, PST_PAWN_ENDGAME_47, PST_PAWN_ENDGAME_48, PST_PAWN_ENDGAME_49,
				PST_PAWN_ENDGAME_50, PST_PAWN_ENDGAME_51, PST_PAWN_ENDGAME_52, PST_PAWN_ENDGAME_53, PST_PAWN_ENDGAME_54,
				PST_PAWN_ENDGAME_55, 0, 0, 0, 0, 0, 0, 0, 0 };
	}
	byte[] getPST_KNIGHT_OPENING() {
		return new byte[] {
				PST_KNIGHT_OPENING_0, PST_KNIGHT_OPENING_1, PST_KNIGHT_OPENING_2, PST_KNIGHT_OPENING_3,
				PST_KNIGHT_OPENING_3, PST_KNIGHT_OPENING_2, PST_KNIGHT_OPENING_1, PST_KNIGHT_OPENING_0,
				PST_KNIGHT_OPENING_8, PST_KNIGHT_OPENING_9, PST_KNIGHT_OPENING_10, PST_KNIGHT_OPENING_11,
				PST_KNIGHT_OPENING_11, PST_KNIGHT_OPENING_10, PST_KNIGHT_OPENING_9, PST_KNIGHT_OPENING_8,
				PST_KNIGHT_OPENING_16, PST_KNIGHT_OPENING_17, PST_KNIGHT_OPENING_18, PST_KNIGHT_OPENING_19,
				PST_KNIGHT_OPENING_19, PST_KNIGHT_OPENING_18, PST_KNIGHT_OPENING_17, PST_KNIGHT_OPENING_16,
				PST_KNIGHT_OPENING_24, PST_KNIGHT_OPENING_25, PST_KNIGHT_OPENING_26, PST_KNIGHT_OPENING_27,
				PST_KNIGHT_OPENING_27, PST_KNIGHT_OPENING_26, PST_KNIGHT_OPENING_25, PST_KNIGHT_OPENING_24,
				PST_KNIGHT_OPENING_32, PST_KNIGHT_OPENING_33, PST_KNIGHT_OPENING_34, PST_KNIGHT_OPENING_35,
				PST_KNIGHT_OPENING_35, PST_KNIGHT_OPENING_34, PST_KNIGHT_OPENING_33, PST_KNIGHT_OPENING_32,
				PST_KNIGHT_OPENING_40, PST_KNIGHT_OPENING_41, PST_KNIGHT_OPENING_42, PST_KNIGHT_OPENING_43,
				PST_KNIGHT_OPENING_43, PST_KNIGHT_OPENING_42, PST_KNIGHT_OPENING_41, PST_KNIGHT_OPENING_40,
				PST_KNIGHT_OPENING_48, PST_KNIGHT_OPENING_49, PST_KNIGHT_OPENING_50, PST_KNIGHT_OPENING_51,
				PST_KNIGHT_OPENING_51, PST_KNIGHT_OPENING_50, PST_KNIGHT_OPENING_49, PST_KNIGHT_OPENING_48,
				PST_KNIGHT_OPENING_56, PST_KNIGHT_OPENING_57, PST_KNIGHT_OPENING_58, PST_KNIGHT_OPENING_59,
				PST_KNIGHT_OPENING_59, PST_KNIGHT_OPENING_58, PST_KNIGHT_OPENING_57, PST_KNIGHT_OPENING_56 };
	}
	byte[] getPST_KNIGHT_ENDGAME() {
		return new byte[] {
				PST_KNIGHT_ENDGAME_0, PST_KNIGHT_ENDGAME_1, PST_KNIGHT_ENDGAME_2, PST_KNIGHT_ENDGAME_3,
				PST_KNIGHT_ENDGAME_3, PST_KNIGHT_ENDGAME_2, PST_KNIGHT_ENDGAME_1, PST_KNIGHT_ENDGAME_0,
				PST_KNIGHT_ENDGAME_8, PST_KNIGHT_ENDGAME_9, PST_KNIGHT_ENDGAME_10, PST_KNIGHT_ENDGAME_11,
				PST_KNIGHT_ENDGAME_11, PST_KNIGHT_ENDGAME_10, PST_KNIGHT_ENDGAME_9, PST_KNIGHT_ENDGAME_8,
				PST_KNIGHT_ENDGAME_16, PST_KNIGHT_ENDGAME_17, PST_KNIGHT_ENDGAME_18, PST_KNIGHT_ENDGAME_19,
				PST_KNIGHT_ENDGAME_19, PST_KNIGHT_ENDGAME_18, PST_KNIGHT_ENDGAME_17, PST_KNIGHT_ENDGAME_16,
				PST_KNIGHT_ENDGAME_24, PST_KNIGHT_ENDGAME_25, PST_KNIGHT_ENDGAME_26, PST_KNIGHT_ENDGAME_27,
				PST_KNIGHT_ENDGAME_27, PST_KNIGHT_ENDGAME_26, PST_KNIGHT_ENDGAME_25, PST_KNIGHT_ENDGAME_24,
				PST_KNIGHT_ENDGAME_32, PST_KNIGHT_ENDGAME_33, PST_KNIGHT_ENDGAME_34, PST_KNIGHT_ENDGAME_35,
				PST_KNIGHT_ENDGAME_35, PST_KNIGHT_ENDGAME_34, PST_KNIGHT_ENDGAME_33, PST_KNIGHT_ENDGAME_32,
				PST_KNIGHT_ENDGAME_40, PST_KNIGHT_ENDGAME_41, PST_KNIGHT_ENDGAME_42, PST_KNIGHT_ENDGAME_43,
				PST_KNIGHT_ENDGAME_43, PST_KNIGHT_ENDGAME_42, PST_KNIGHT_ENDGAME_41, PST_KNIGHT_ENDGAME_40,
				PST_KNIGHT_ENDGAME_48, PST_KNIGHT_ENDGAME_49, PST_KNIGHT_ENDGAME_50, PST_KNIGHT_ENDGAME_51,
				PST_KNIGHT_ENDGAME_51, PST_KNIGHT_ENDGAME_50, PST_KNIGHT_ENDGAME_49, PST_KNIGHT_ENDGAME_48,
				PST_KNIGHT_ENDGAME_56, PST_KNIGHT_ENDGAME_57, PST_KNIGHT_ENDGAME_58, PST_KNIGHT_ENDGAME_59,
				PST_KNIGHT_ENDGAME_59, PST_KNIGHT_ENDGAME_58, PST_KNIGHT_ENDGAME_57, PST_KNIGHT_ENDGAME_56 };
	}
	byte[] getPST_BISHOP() {
		return new byte[] {
				PST_BISHOP_0, PST_BISHOP_1, PST_BISHOP_2, PST_BISHOP_3,
				PST_BISHOP_3, PST_BISHOP_2, PST_BISHOP_1, PST_BISHOP_0,
				PST_BISHOP_8, PST_BISHOP_9, PST_BISHOP_10, PST_BISHOP_11,
				PST_BISHOP_11, PST_BISHOP_10, PST_BISHOP_9, PST_BISHOP_8,
				PST_BISHOP_16, PST_BISHOP_17, PST_BISHOP_18, PST_BISHOP_19,
				PST_BISHOP_19, PST_BISHOP_18, PST_BISHOP_17, PST_BISHOP_16,
				PST_BISHOP_24, PST_BISHOP_25, PST_BISHOP_26, PST_BISHOP_27,
				PST_BISHOP_27, PST_BISHOP_26, PST_BISHOP_25, PST_BISHOP_24,
				PST_BISHOP_32, PST_BISHOP_33, PST_BISHOP_34, PST_BISHOP_35,
				PST_BISHOP_35, PST_BISHOP_34, PST_BISHOP_33, PST_BISHOP_32,
				PST_BISHOP_40, PST_BISHOP_41, PST_BISHOP_42, PST_BISHOP_43,
				PST_BISHOP_43, PST_BISHOP_42, PST_BISHOP_41, PST_BISHOP_40,
				PST_BISHOP_48, PST_BISHOP_49, PST_BISHOP_50, PST_BISHOP_51,
				PST_BISHOP_51, PST_BISHOP_50, PST_BISHOP_49, PST_BISHOP_48,
				PST_BISHOP_56, PST_BISHOP_57, PST_BISHOP_58, PST_BISHOP_59,
				PST_BISHOP_59, PST_BISHOP_58, PST_BISHOP_57, PST_BISHOP_56 };
	}
	byte[] getPST_ROOK_OPENING() {
		return new byte[] {
				PST_ROOK_OPENING_0, PST_ROOK_OPENING_1, PST_ROOK_OPENING_2, PST_ROOK_OPENING_3, PST_ROOK_OPENING_4,
				PST_ROOK_OPENING_5, PST_ROOK_OPENING_6, PST_ROOK_OPENING_7, PST_ROOK_OPENING_8, PST_ROOK_OPENING_9,
				PST_ROOK_OPENING_10, PST_ROOK_OPENING_11, PST_ROOK_OPENING_12, PST_ROOK_OPENING_13, PST_ROOK_OPENING_14,
				PST_ROOK_OPENING_15, PST_ROOK_OPENING_16, PST_ROOK_OPENING_17, PST_ROOK_OPENING_18, PST_ROOK_OPENING_19,
				PST_ROOK_OPENING_20, PST_ROOK_OPENING_21, PST_ROOK_OPENING_22, PST_ROOK_OPENING_23, PST_ROOK_OPENING_24,
				PST_ROOK_OPENING_25, PST_ROOK_OPENING_26, PST_ROOK_OPENING_27, PST_ROOK_OPENING_28, PST_ROOK_OPENING_29,
				PST_ROOK_OPENING_30, PST_ROOK_OPENING_31, PST_ROOK_OPENING_32, PST_ROOK_OPENING_33, PST_ROOK_OPENING_34,
				PST_ROOK_OPENING_35, PST_ROOK_OPENING_36, PST_ROOK_OPENING_37, PST_ROOK_OPENING_38, PST_ROOK_OPENING_39,
				PST_ROOK_OPENING_40, PST_ROOK_OPENING_41, PST_ROOK_OPENING_42, PST_ROOK_OPENING_43, PST_ROOK_OPENING_44,
				PST_ROOK_OPENING_45, PST_ROOK_OPENING_46, PST_ROOK_OPENING_47, PST_ROOK_OPENING_48, PST_ROOK_OPENING_49,
				PST_ROOK_OPENING_50, PST_ROOK_OPENING_51, PST_ROOK_OPENING_52, PST_ROOK_OPENING_53, PST_ROOK_OPENING_54,
				PST_ROOK_OPENING_55, PST_ROOK_OPENING_56, PST_ROOK_OPENING_57, PST_ROOK_OPENING_58, PST_ROOK_OPENING_59,
				PST_ROOK_OPENING_60, PST_ROOK_OPENING_61, PST_ROOK_OPENING_62, PST_ROOK_OPENING_63 };
	}
	byte[] getPST_ROOK_ENDGAME() {
		return new byte[] {
				PST_ROOK_ENDGAME_0, PST_ROOK_ENDGAME_1, PST_ROOK_ENDGAME_2, PST_ROOK_ENDGAME_3, PST_ROOK_ENDGAME_4,
				PST_ROOK_ENDGAME_5, PST_ROOK_ENDGAME_6, PST_ROOK_ENDGAME_7, PST_ROOK_ENDGAME_8, PST_ROOK_ENDGAME_9,
				PST_ROOK_ENDGAME_10, PST_ROOK_ENDGAME_11, PST_ROOK_ENDGAME_12, PST_ROOK_ENDGAME_13, PST_ROOK_ENDGAME_14,
				PST_ROOK_ENDGAME_15, PST_ROOK_ENDGAME_16, PST_ROOK_ENDGAME_17, PST_ROOK_ENDGAME_18, PST_ROOK_ENDGAME_19,
				PST_ROOK_ENDGAME_20, PST_ROOK_ENDGAME_21, PST_ROOK_ENDGAME_22, PST_ROOK_ENDGAME_23, PST_ROOK_ENDGAME_24,
				PST_ROOK_ENDGAME_25, PST_ROOK_ENDGAME_26, PST_ROOK_ENDGAME_27, PST_ROOK_ENDGAME_28, PST_ROOK_ENDGAME_29,
				PST_ROOK_ENDGAME_30, PST_ROOK_ENDGAME_31, PST_ROOK_ENDGAME_32, PST_ROOK_ENDGAME_33, PST_ROOK_ENDGAME_34,
				PST_ROOK_ENDGAME_35, PST_ROOK_ENDGAME_36, PST_ROOK_ENDGAME_37, PST_ROOK_ENDGAME_38, PST_ROOK_ENDGAME_39,
				PST_ROOK_ENDGAME_40, PST_ROOK_ENDGAME_41, PST_ROOK_ENDGAME_42, PST_ROOK_ENDGAME_43, PST_ROOK_ENDGAME_44,
				PST_ROOK_ENDGAME_45, PST_ROOK_ENDGAME_46, PST_ROOK_ENDGAME_47, PST_ROOK_ENDGAME_48, PST_ROOK_ENDGAME_49,
				PST_ROOK_ENDGAME_50, PST_ROOK_ENDGAME_51, PST_ROOK_ENDGAME_52, PST_ROOK_ENDGAME_53, PST_ROOK_ENDGAME_54,
				PST_ROOK_ENDGAME_55, PST_ROOK_ENDGAME_56, PST_ROOK_ENDGAME_57, PST_ROOK_ENDGAME_58, PST_ROOK_ENDGAME_59,
				PST_ROOK_ENDGAME_60, PST_ROOK_ENDGAME_61, PST_ROOK_ENDGAME_62, PST_ROOK_ENDGAME_63 };
	}
	byte[] getPST_QUEEN() {
		return new byte[] {
				PST_QUEEN_0, PST_QUEEN_1, PST_QUEEN_2, PST_QUEEN_3, PST_QUEEN_4,
				PST_QUEEN_5, PST_QUEEN_6, PST_QUEEN_7, PST_QUEEN_8, PST_QUEEN_9,
				PST_QUEEN_10, PST_QUEEN_11, PST_QUEEN_12, PST_QUEEN_13, PST_QUEEN_14,
				PST_QUEEN_15, PST_QUEEN_16, PST_QUEEN_17, PST_QUEEN_18, PST_QUEEN_19,
				PST_QUEEN_20, PST_QUEEN_21, PST_QUEEN_22, PST_QUEEN_23, PST_QUEEN_24,
				PST_QUEEN_25, PST_QUEEN_26, PST_QUEEN_27, PST_QUEEN_28, PST_QUEEN_29,
				PST_QUEEN_30, PST_QUEEN_31, PST_QUEEN_32, PST_QUEEN_33, PST_QUEEN_34,
				PST_QUEEN_35, PST_QUEEN_36, PST_QUEEN_37, PST_QUEEN_38, PST_QUEEN_39,
				PST_QUEEN_40, PST_QUEEN_41, PST_QUEEN_42, PST_QUEEN_43, PST_QUEEN_44,
				PST_QUEEN_45, PST_QUEEN_46, PST_QUEEN_47, PST_QUEEN_48, PST_QUEEN_49,
				PST_QUEEN_50, PST_QUEEN_51, PST_QUEEN_52, PST_QUEEN_53, PST_QUEEN_54,
				PST_QUEEN_55, PST_QUEEN_56, PST_QUEEN_57, PST_QUEEN_58, PST_QUEEN_59,
				PST_QUEEN_60, PST_QUEEN_61, PST_QUEEN_62, PST_QUEEN_63 };
	}
	byte[] getPST_KING_OPENING() {
		return new byte[] {
				PST_KING_OPENING_0, PST_KING_OPENING_1, PST_KING_OPENING_2, PST_KING_OPENING_3, PST_KING_OPENING_4,
				PST_KING_OPENING_5, PST_KING_OPENING_6, PST_KING_OPENING_7, PST_KING_OPENING_8, PST_KING_OPENING_9,
				PST_KING_OPENING_10, PST_KING_OPENING_11, PST_KING_OPENING_12, PST_KING_OPENING_13, PST_KING_OPENING_14,
				PST_KING_OPENING_15, PST_KING_OPENING_16, PST_KING_OPENING_17, PST_KING_OPENING_18, PST_KING_OPENING_19,
				PST_KING_OPENING_20, PST_KING_OPENING_21, PST_KING_OPENING_22, PST_KING_OPENING_23, PST_KING_OPENING_24,
				PST_KING_OPENING_25, PST_KING_OPENING_26, PST_KING_OPENING_27, PST_KING_OPENING_28, PST_KING_OPENING_29,
				PST_KING_OPENING_30, PST_KING_OPENING_31, PST_KING_OPENING_32, PST_KING_OPENING_33, PST_KING_OPENING_34,
				PST_KING_OPENING_35, PST_KING_OPENING_36, PST_KING_OPENING_37, PST_KING_OPENING_38, PST_KING_OPENING_39,
				PST_KING_OPENING_40, PST_KING_OPENING_41, PST_KING_OPENING_42, PST_KING_OPENING_43, PST_KING_OPENING_44,
				PST_KING_OPENING_45, PST_KING_OPENING_46, PST_KING_OPENING_47, PST_KING_OPENING_48, PST_KING_OPENING_49,
				PST_KING_OPENING_50, PST_KING_OPENING_51, PST_KING_OPENING_52, PST_KING_OPENING_53, PST_KING_OPENING_54,
				PST_KING_OPENING_55, PST_KING_OPENING_56, PST_KING_OPENING_57, PST_KING_OPENING_58, PST_KING_OPENING_59,
				PST_KING_OPENING_60, PST_KING_OPENING_61, PST_KING_OPENING_62, PST_KING_OPENING_63 };
	}
	byte[] getPST_KING_ENDGAME() {
		return new byte[] {
				PST_KING_ENDGAME_0, PST_KING_ENDGAME_1, PST_KING_ENDGAME_2, PST_KING_ENDGAME_3, PST_KING_ENDGAME_4,
				PST_KING_ENDGAME_5, PST_KING_ENDGAME_6, PST_KING_ENDGAME_7, PST_KING_ENDGAME_8, PST_KING_ENDGAME_9,
				PST_KING_ENDGAME_10, PST_KING_ENDGAME_11, PST_KING_ENDGAME_12, PST_KING_ENDGAME_13, PST_KING_ENDGAME_14,
				PST_KING_ENDGAME_15, PST_KING_ENDGAME_16, PST_KING_ENDGAME_17, PST_KING_ENDGAME_18, PST_KING_ENDGAME_19,
				PST_KING_ENDGAME_20, PST_KING_ENDGAME_21, PST_KING_ENDGAME_22, PST_KING_ENDGAME_23, PST_KING_ENDGAME_24,
				PST_KING_ENDGAME_25, PST_KING_ENDGAME_26, PST_KING_ENDGAME_27, PST_KING_ENDGAME_28, PST_KING_ENDGAME_29,
				PST_KING_ENDGAME_30, PST_KING_ENDGAME_31, PST_KING_ENDGAME_32, PST_KING_ENDGAME_33, PST_KING_ENDGAME_34,
				PST_KING_ENDGAME_35, PST_KING_ENDGAME_36, PST_KING_ENDGAME_37, PST_KING_ENDGAME_38, PST_KING_ENDGAME_39,
				PST_KING_ENDGAME_40, PST_KING_ENDGAME_41, PST_KING_ENDGAME_42, PST_KING_ENDGAME_43, PST_KING_ENDGAME_44,
				PST_KING_ENDGAME_45, PST_KING_ENDGAME_46, PST_KING_ENDGAME_47, PST_KING_ENDGAME_48, PST_KING_ENDGAME_49,
				PST_KING_ENDGAME_50, PST_KING_ENDGAME_51, PST_KING_ENDGAME_52, PST_KING_ENDGAME_53, PST_KING_ENDGAME_54,
				PST_KING_ENDGAME_55, PST_KING_ENDGAME_56, PST_KING_ENDGAME_57, PST_KING_ENDGAME_58, PST_KING_ENDGAME_59,
				PST_KING_ENDGAME_60, PST_KING_ENDGAME_61, PST_KING_ENDGAME_62, PST_KING_ENDGAME_63 };
	}
	
}
