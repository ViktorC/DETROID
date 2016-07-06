package engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Random;

public final class Parameters {

	// Piece values.
	short KING_VALUE = 20000;
	short KING_PHASE_WEIGHT = 0;
	short QUEEN_VALUE = 900;
	short QUEEN_PHASE_WEIGHT = 4;
	short ROOK_VALUE = 500;
	short ROOK_PHASE_WEIGHT = 2;
	short BISHOP_VALUE = 330;
	short BISHOP_PHASE_WEIGHT = 1;
	short KNIGHT_VALUE = 320;
	short KNIGHT_PHASE_WEIGHT = 1;
	short PAWN_VALUE = 100;
	short PAWN_PHASE_WEIGHT = 0;
	
	// Evaluation weights.
	byte DEFENDED_PAWN_WEIGHT = 20;
	byte BLOCKED_PAWN_WEIGHT1 = 30;
	byte BLOCKED_PAWN_WEIGHT2 = 15;
	byte BLOCKED_PAWN_WEIGHT3 = 5;
	byte PASSED_PAWN_WEIGHT = 40;
	byte ISOLATED_PAWN_WEIGHT = 10;
	byte BACKWARD_PAWN_WEIGHT1 = 20;
	byte BACKWARD_PAWN_WEIGHT2 = 10;
	byte SHIELDING_PAWN_WEIGHT1 = 15;
	byte SHIELDING_PAWN_WEIGHT2 = 5;
	byte SHIELDING_PAWN_WEIGHT3 = 10;
	byte SHIELD_THREATENING_PAWN_WEIGHT1 = 15;
	byte SHIELD_THREATENING_PAWN_WEIGHT2 = 10;
	byte DEFENDED_KING_AREA_SQUARE_WEIGHT1 = 15;
	byte DEFENDED_KING_AREA_SQUARE_WEIGHT2 = 10;
	byte DEFENDED_KING_AREA_SQUARE_WEIGHT3 = 5;
	byte KING_PAWN_TROPISM_WEIGHT = 4;
	byte PINNED_QUEEN_WEIGHT = 10;
	byte PINNED_ROOK_WEIGHT = 6;
	byte PINNED_BISHOP_WEIGHT = 4;
	byte PINNED_KNIGHT_WEIGHT = 4;
	byte COVERED_SQUARE_WEIGHT = 1;
	byte COVERED_FRIENDLY_OCCUPIED_SQUARE_WEIGHT = 1;
	byte PAWN_DEFENDED_PIECE_WEIGHT = 10;
	byte PIECE_KING_TROPISM_WEIGHT = 4;
	byte STOPPED_PAWN_WEIGHT = 10;
	
	// Game phase intervals.
	short GAME_PHASE_OPENING_LOWER = 0;
	short GAME_PHASE_OPENING_UPPER = 22;
	short GAME_PHASE_MIDDLE_GAME_LOWER = 23;
	short GAME_PHASE_MIDDLE_GAME_UPPER = 170;
	short GAME_PHASE_END_GAME_LOWER = 171;
	short GAME_PHASE_END_GAME_UPPER = 256;
	
	// Search parameters.
	int NMR = 2;		// Null move pruning reduction.
	int LMR = 1;		// Late move reduction.
	int LMRMSM = 4;		// Min. number of searched moves for late move reduction
	int FMAR1 = 330;	// Futility margin.
	int FMAR2 = 500;	// Extended futility margin.
	int FMAR3 = 900;	// Razoring margin.
	int A_DELTA = 100;	// The aspiration delta within iterative deepening.
	int Q_DELTA = 270;	// The margin for delta-pruning in the quiescence search.
	
	// The relative history table's value depreciation factor.
	byte RHT_DECREMENT_FACTOR = 4;
	
	// The margin for lazy evaluation. The extended score should be very unlikely to differ by more than this amount from the core score.
	int LAZY_EVAL_MAR = 187;
	
	// Piece-square tables based on and extending Tomasz Michniewski's "Unified Evaluation" tables.
	byte[] PST_PAWN_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  50, 50, 50, 50, 50, 50, 50, 50,
		  10, 20, 25, 30, 30, 25, 20, 10,
		   5, 15, 20, 20, 20, 20, 15,  5,
		   0, 10, 15, 15, 15, 15, 10,  0,
		   0,  8, 10, 10, 10, 10,  8,  0,
		   5,  5,  5,  0,  0,  5,  5,  5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	byte[] PST_PAWN_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  70, 75, 80, 80, 80, 80, 75, 70,
		  20, 30, 40, 45, 45, 40, 30, 20,
		  10, 20, 25, 30, 30, 25, 20, 10,
		   5, 10, 10, 15, 15, 10, 10,  5,
		   0,  0, -5, -5, -5, -5,  0,  0,
		  -5,-10,-10,-20,-20,-10,-10, -5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	byte[] PST_KNIGHT_OPENING =
		{-15,-10, -5, -5, -5, -5,-10,-15,
		 -10,-10,  0,  0,  0,  0,-10,-10,
		  -5,  0,  5, 10, 10,  5,  0, -5,
		  -5,  0, 10, 15, 15, 10,  0, -5,
		  -5,  0, 10, 15, 15, 10,  0, -5,
		  -5,  0,  5, 10, 10,  5,  0, -5,
		 -10, -5,  0,  0,  0,  0, -5,-10,
		 -15,-10, -5, -5, -5, -5,-10,-15};
	byte[] PST_KNIGHT_ENDGAME =
		{-50,-40,-30,-30,-30,-30,-40,-50,
		 -40,-20, -5,  0,  0, -5,-20,-40,
		 -30,  0,  0,  0,  0,  0, -5,-30,
		 -30,  0,  5,  5,  5,  5,  0,-30,
		 -30, -5,  5,  5,  5,  5, -5,-30,
		 -30,  0,  0,  0,  0,  0,  0,-30,
		 -40,-20, -5,  0,  0, -5,-20,-40,
		 -50,-40,-30,-30,-30,-30,-40,-50};
	byte[] PST_BISHOP =
		{-20,-10,-10,-10,-10,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5, 10, 10,  5,  0,-10,
		 -10,  5,  5, 15, 15,  5,  5,-10,
		 -10,  0, 10, 15, 15, 10,  0,-10,
		 -10, 10, 10, 10, 10, 10, 10,-10,
		 -10,  5,  0,  0,  0,  0,  5,-10,
		 -20,-10,-10,-10,-10,-10,-10,-20};
	byte[] PST_ROOK_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  10, -5,  0, 20,  5, 20, -5, 10};
	byte[] PST_ROOK_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		   0,  0,  5,  5,  5,  5,  0,  0};
	byte[] PST_QUEEN =
		{-20,-10,-10, -5, -5,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5,  5,  5,  5,  0,-10,
		  -5,  0,  5,  5,  5,  5,  0, -5,
		   0,  0,  5,  5,  5,  5,  0, -5,
		 -10,  0,  5,  5,  5,  5,  0,-10,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -20,-10,-10, -5, -5,-10,-10,-20};
	byte[] PST_KING_OPENING =
		{-30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -20,-30,-30,-40,-40,-30,-30,-20,
		 -10,-20,-20,-20,-20,-20,-20,-10,
		  10, 10,-10,-10,-10,-10, 10, 10,
		  20, 30, 30,-20,  0,-20, 40, 30};
	byte[] PST_KING_ENDGAME =
		{-50,-40,-30,-20,-20,-30,-40,-50,
		 -30,-20,-10,  0,  0,-10,-20,-30,
		 -30,-10, 20, 30, 30, 20,-10,-30,
		 -30,-10, 30, 40, 40, 30,-10,-30,
		 -30,-10, 30, 40, 40, 30,-10,-30,
		 -30,-10, 20, 30, 30, 20,-10,-30,
		 -30,-30,  0,  0,  0,  0,-30,-30,
		 -50,-30,-30,-30,-30,-30,-30,-50};
	
	private long id;
	
	public final static String DEFAULT_PARAMTERS_FILE_PATH = "params.txt";
	
	@SuppressWarnings("unused")
	private Parameters(boolean noIo) { }
	public Parameters(String filePath) {
		setParameters(filePath);
		Random rand = new Random();
		id = rand.nextLong();
	}
	public Parameters() {
		this(DEFAULT_PARAMTERS_FILE_PATH);
	}
	public long getId() {
		return id;
	}
	private boolean setParameters(String filePath) {
		String line;
		String name;
		String value;
		String[] arrayValues;
		byte[] byteArray;
		Class<? extends Parameters> clazz = this.getClass();
		Field field;
		int indexOfClosingNameTag;
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath));) {
			while ((line = reader.readLine()) != null) {
				indexOfClosingNameTag = line.indexOf(']');
				name = line.substring(line.indexOf('[') + 1, indexOfClosingNameTag);
				field = clazz.getDeclaredField(name);
				if (line.charAt(indexOfClosingNameTag + 3) == '(') {
					value = line.substring(indexOfClosingNameTag + 4, line.indexOf(')'));
					arrayValues = value.split(",");
					byteArray = new byte[64];
					for (int i = 0; i < 64; i++)
						byteArray[i] = Byte.parseByte(arrayValues[i]);
					field.set(this, byteArray);
				}
				else {
					value = line.substring(indexOfClosingNameTag + 4, line.length());
					switch (field.get(this).getClass().getSimpleName()) {
					case "Byte":
						field.set(this, Byte.parseByte(value));
						break;
					case "Short":
						field.set(this, Short.parseShort(value));
						break;
					case "Integer":
						field.set(this, Integer.parseInt(value));
					}
				}
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	/**
	 * Overwrites the default parameters file, params.txt, with the state of this instance.
	 * 
	 * @return
	 */
	public boolean writeToFile() {
		return writeToFile(DEFAULT_PARAMTERS_FILE_PATH);
	}
	/**
	 * Writes the parameters to the specified file.
	 * 
	 * @param fileName
	 * @return
	 */
	public boolean writeToFile(String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write(toString());
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	@Override
	public String toString() {
		String out = "";
		String subString;
		@SuppressWarnings("rawtypes")
		Class fieldClass;
		Object fieldValue;
		int length;
		Object arrayElement;
		Class<? extends Parameters> clazz = this.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field f : fields) {
			subString = "";
			try {
				fieldClass = f.getType();
				if (fieldClass.isArray()) {
					fieldValue = f.get(this);
					subString += "[" + f.getName() + "] =(";
					length = Array.getLength(fieldValue);
				    for (int i = 0; i < length; i ++) {
				        arrayElement = Array.get(fieldValue, i);
				        subString += arrayElement.toString();
				        if (i != length - 1)
				        	subString += ",";
				    }
				    subString += ")";
				}
				else {
					subString += "[" + f.getName() + "] = " + f.get(this).toString();
				}
				subString += "\n";
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				subString = "";
			}
			out += subString;
		}
		return out;
	}
}
