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
	public short KING_VALUE = 20000;
	public short KING_PHASE_WEIGHT = 0;
	public short QUEEN_VALUE = 900;
	public short QUEEN_PHASE_WEIGHT = 4;
	public short ROOK_VALUE = 500;
	public short ROOK_PHASE_WEIGHT = 2;
	public short BISHOP_VALUE = 330;
	public short BISHOP_PHASE_WEIGHT = 1;
	public short KNIGHT_VALUE = 320;
	public short KNIGHT_PHASE_WEIGHT = 1;
	public short PAWN_VALUE = 100;
	public short PAWN_PHASE_WEIGHT = 0;
	
	// Evaluation weights.
	public byte DEFENDED_PAWN_WEIGHT = 20;
	public byte BLOCKED_PAWN_WEIGHT1 = 30;
	public byte BLOCKED_PAWN_WEIGHT2 = 15;
	public byte BLOCKED_PAWN_WEIGHT3 = 5;
	public byte PASSED_PAWN_WEIGHT = 40;
	public byte ISOLATED_PAWN_WEIGHT = 10;
	public byte BACKWARD_PAWN_WEIGHT1 = 20;
	public byte BACKWARD_PAWN_WEIGHT2 = 10;
	public byte SHIELDING_PAWN_WEIGHT1 = 15;
	public byte SHIELDING_PAWN_WEIGHT2 = 5;
	public byte SHIELDING_PAWN_WEIGHT3 = 10;
	public byte SHIELD_THREATENING_PAWN_WEIGHT1 = 15;
	public byte SHIELD_THREATENING_PAWN_WEIGHT2 = 10;
	public byte DEFENDED_KING_AREA_SQUARE_WEIGHT1 = 15;
	public byte DEFENDED_KING_AREA_SQUARE_WEIGHT2 = 10;
	public byte DEFENDED_KING_AREA_SQUARE_WEIGHT3 = 5;
	public byte KING_PAWN_TROPISM_WEIGHT = 4;
	public byte PINNED_QUEEN_WEIGHT = 10;
	public byte PINNED_ROOK_WEIGHT = 6;
	public byte PINNED_BISHOP_WEIGHT = 4;
	public byte PINNED_KNIGHT_WEIGHT = 4;
	public byte COVERED_SQUARE_WEIGHT = 1;
	public byte COVERED_FRIENDLY_OCCUPIED_SQUARE_WEIGHT = 1;
	public byte PAWN_DEFENDED_PIECE_WEIGHT = 10;
	public byte PIECE_KING_TROPISM_WEIGHT = 4;
	public byte STOPPED_PAWN_WEIGHT = 10;
	
	// Game phase intervals.
	public short GAME_PHASE_OPENING_LOWER = 0;
	public short GAME_PHASE_OPENING_UPPER = 22;
	public short GAME_PHASE_MIDDLE_GAME_LOWER = 23;
	public short GAME_PHASE_MIDDLE_GAME_UPPER = 170;
	public short GAME_PHASE_END_GAME_LOWER = 171;
	public short GAME_PHASE_END_GAME_UPPER = 256;
	
	// Search parameters.
	public int NMR = 2;		// Null move pruning reduction.
	public int LMR = 1;		// Late move reduction.
	public int LMRMSM = 4;		// Min. number of searched moves for late move reduction
	public int FMAR1 = 330;	// Futility margin.
	public int FMAR2 = 500;	// Extended futility margin.
	public int FMAR3 = 900;	// Razoring margin.
	public int A_DELTA = 100;	// The aspiration delta within iterative deepening.
	public int Q_DELTA = 270;	// The margin for delta-pruning in the quiescence search.
	
	// The relative history table's value depreciation factor.
	public byte RHT_DECREMENT_FACTOR = 4;
	
	// The shares of the different hash tables of the total hash size.
	public byte TT_SHARE = 16;
	public byte ET_SHARE = 15;
	public byte PT_SHARE = 1;
	
	// The margin for lazy evaluation. The extended score should be very unlikely to differ by more than this amount from the core score.
	public int LAZY_EVAL_MAR = 187;
	
	// Piece-square tables based on and extending Tomasz Michniewski's "Unified Evaluation" tables.
	public byte[] PST_PAWN_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  50, 50, 50, 50, 50, 50, 50, 50,
		  10, 20, 25, 30, 30, 25, 20, 10,
		   5, 15, 20, 20, 20, 20, 15,  5,
		   0, 10, 15, 15, 15, 15, 10,  0,
		   0,  8, 10, 10, 10, 10,  8,  0,
		   5,  5,  5,  0,  0,  5,  5,  5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	public byte[] PST_PAWN_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  70, 75, 80, 80, 80, 80, 75, 70,
		  20, 30, 40, 45, 45, 40, 30, 20,
		  10, 20, 25, 30, 30, 25, 20, 10,
		   5, 10, 10, 15, 15, 10, 10,  5,
		   0,  0, -5, -5, -5, -5,  0,  0,
		  -5,-10,-10,-20,-20,-10,-10, -5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	public byte[] PST_KNIGHT_OPENING =
		{-15,-10, -5, -5, -5, -5,-10,-15,
		 -10,-10,  0,  0,  0,  0,-10,-10,
		  -5,  0,  5, 10, 10,  5,  0, -5,
		  -5,  0, 10, 15, 15, 10,  0, -5,
		  -5,  0, 10, 15, 15, 10,  0, -5,
		  -5,  0,  5, 10, 10,  5,  0, -5,
		 -10, -5,  0,  0,  0,  0, -5,-10,
		 -15,-10, -5, -5, -5, -5,-10,-15};
	public byte[] PST_KNIGHT_ENDGAME =
		{-50,-40,-30,-30,-30,-30,-40,-50,
		 -40,-20, -5,  0,  0, -5,-20,-40,
		 -30,  0,  0,  0,  0,  0, -5,-30,
		 -30,  0,  5,  5,  5,  5,  0,-30,
		 -30, -5,  5,  5,  5,  5, -5,-30,
		 -30,  0,  0,  0,  0,  0,  0,-30,
		 -40,-20, -5,  0,  0, -5,-20,-40,
		 -50,-40,-30,-30,-30,-30,-40,-50};
	public byte[] PST_BISHOP =
		{-20,-10,-10,-10,-10,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5, 10, 10,  5,  0,-10,
		 -10,  5,  5, 15, 15,  5,  5,-10,
		 -10,  0, 10, 15, 15, 10,  0,-10,
		 -10, 10, 10, 10, 10, 10, 10,-10,
		 -10,  5,  0,  0,  0,  0,  5,-10,
		 -20,-10,-10,-10,-10,-10,-10,-20};
	public byte[] PST_ROOK_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  10, -5,  0, 20,  5, 20, -5, 10};
	public byte[] PST_ROOK_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		   0,  0,  5,  5,  5,  5,  0,  0};
	public byte[] PST_QUEEN =
		{-20,-10,-10, -5, -5,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5,  5,  5,  5,  0,-10,
		  -5,  0,  5,  5,  5,  5,  0, -5,
		   0,  0,  5,  5,  5,  5,  0, -5,
		 -10,  0,  5,  5,  5,  5,  0,-10,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -20,-10,-10, -5, -5,-10,-10,-20};
	public byte[] PST_KING_OPENING =
		{-30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -20,-30,-30,-40,-40,-30,-30,-20,
		 -10,-20,-20,-20,-20,-20,-20,-10,
		  10, 10,-10,-10,-10,-10, 10, 10,
		  20, 30, 30,-20,  0,-20, 40, 30};
	public byte[] PST_KING_ENDGAME =
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
