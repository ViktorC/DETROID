package engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Random;

/**
 * A class for parameter definitions used in the evaluation, search, and time management. It initializes instances by reading the values
 * from a txt file. The state of the instances can freely be changed and then written to txt files as well. It's main purpose is to provide
 * the infrastructure necessary for automated tuning.
 * 
 * @author Viktor
 *
 */
public final class Parameters {

	public final static String DEFAULT_PARAMETERS_FILE_PATH = "/params.txt";
	
	// Piece values.
	public short KING_VALUE;
	public short QUEEN_VALUE;
	public short ROOK_VALUE;
	public short BISHOP_VALUE;
	public short KNIGHT_VALUE;
	public short PAWN_VALUE;
	
	// Piece phase weights
	public byte KING_PHASE_WEIGHT;
	public byte QUEEN_PHASE_WEIGHT;
	public byte ROOK_PHASE_WEIGHT;
	public byte BISHOP_PHASE_WEIGHT;
	public byte KNIGHT_PHASE_WEIGHT;
	public byte PAWN_PHASE_WEIGHT;
	
	// Evaluation weights.
	public byte BLOCKED_PAWN_WEIGHT1;
	public byte BLOCKED_PAWN_WEIGHT2;
	public byte BLOCKED_PAWN_WEIGHT3;
	public byte PASSED_PAWN_WEIGHT;
	public byte ISOLATED_PAWN_WEIGHT;
	public byte BACKWARD_PAWN_WEIGHT1;
	public byte BACKWARD_PAWN_WEIGHT2;
	public byte SHIELDING_PAWN_WEIGHT1;
	public byte SHIELDING_PAWN_WEIGHT2;
	public byte SHIELD_THREATENING_PAWN_WEIGHT;
	public byte ATTACKED_KING_AREA_SQUARE_WEIGHT;
	public byte KING_PAWN_TROPISM_WEIGHT;
	public byte PINNED_QUEEN_WEIGHT;
	public byte PINNED_ROOK_WEIGHT;
	public byte PINNED_BISHOP_WEIGHT;
	public byte PINNED_KNIGHT_WEIGHT;
	public byte QUEEN_COVERED_SQUARE_WEIGHT;
	public byte ROOK_COVERED_SQUARE_WEIGHT;
	public byte BISHOP_COVERED_SQUARE_WEIGHT;
	public byte KNIGHT_COVERED_SQUARE_WEIGHT;
	public byte PAWN_DEFENDED_PIECE_WEIGHT;
	public byte PAWN_ATTACKED_PIECE_WEIGHT;
	public byte STOPPED_PAWN_WEIGHT;
	public byte PIECE_KING_TROPISM_WEIGHT;
	public byte KING_MOBILITY_WEIGHT;
	
	// Game phase intervals.
	public short GAME_PHASE_OPENING_LOWER;
	public short GAME_PHASE_OPENING_UPPER;
	public short GAME_PHASE_MIDGAME_LOWER;
	public short GAME_PHASE_MIDGAME_UPPER;
	public short GAME_PHASE_ENDGAME_LOWER;
	public short GAME_PHASE_ENDGAME_UPPER;
	
	// Search parameters.
	public byte NMR;						// Null move pruning reduction.
	public byte LMR;						// Late move reduction.
	public byte LMRMSM;						// Min. number of searched moves for late move reduction
	public short FMAR1;						// Futility margin.
	public short FMAR2;						// Extended futility margin.
	public short FMAR3;						// Razoring margin.
	public short A_DELTA;					// The aspiration delta within iterative deepening.
	public short Q_DELTA;					// The margin for delta-pruning in the quiescence search.
	public byte CHECK_EXT;					// Fractional check extension.
	public byte RECAP_EXT;					// Fractional recapture extension.
	public byte SINGLE_REPLY_EXT; 			// Fractional single reply extension.
	public byte MATE_THREAT_EXT;			// Fractional mate threat extension.
	public byte IID_MIN_ACTIVATION_DEPTH;	// The minimum depth at which IID is activated.
	public float IID_REL_DEPTH;				// The portion of the total depth to which the position will be searched with IID.
	
	// The relative history table's value depreciation factor.
	public byte RHT_DECREMENT_FACTOR;
	
	// The shares of the different hash tables of the total hash size.
	public byte TT_SHARE;
	public byte ET_SHARE;
	public byte PT_SHARE;
	// The number of turns for which the different hash table's entries are retained by default.
	public byte TT_ENTRY_LIFECYCLE;
	public byte ET_ENTRY_LIFECYCLE;
	public byte PT_ENTRY_LIFECYCLE;
	
	// The values considered when calculating search time extensions.
	public short SCORE_FLUCTUATION_LIMIT;
	public float FRACTION_OF_ORIG_SEARCH_TIME_SINCE_LAST_RESULT_CHANGE_LIMIT;
	public byte RESULT_CHANGES_PER_DEPTH_LIMIT;
	public byte MOVES_TO_GO_SAFETY_MARGIN;
	public float FRACTION_OF_TOTAL_TIME_TO_USE;
	
	// The margin for lazy evaluation. The extended score should be very unlikely to differ by more than this amount from the core score.
	public short LAZY_EVAL_MAR;
	
	// Piece-square tables based on and extending Tomasz Michniewski's "Unified Evaluation" tables.
	public byte[] PST_PAWN_OPENING;
	public byte[] PST_PAWN_ENDGAME;
	public byte[] PST_KNIGHT_OPENING;
	public byte[] PST_KNIGHT_ENDGAME;
	public byte[] PST_BISHOP;
	public byte[] PST_ROOK_OPENING;
	public byte[] PST_ROOK_ENDGAME;
	public byte[] PST_QUEEN;
	public byte[] PST_KING_OPENING;
	public byte[] PST_KING_ENDGAME;
	
	private long id;
	private String filePath;
	
	@SuppressWarnings("unused")
	private Parameters(boolean noIo) { }
	public Parameters(String filePath) throws IOException {
		setParameters(filePath);
		this.filePath = filePath;
		Random rand = new Random();
		id = rand.nextLong();
	}
	public Parameters() throws IOException {
		this(DEFAULT_PARAMETERS_FILE_PATH);
	}
	/**
	 * Returns the 64 bit ID number of the instance.
	 * 
	 * @return
	 */
	public long getId() {
		return id;
	}
	/**
	 * Returns the path to the file on which the object has been instantiated.
	 * 
	 * @return
	 */
	public String getFilePath() {
		return filePath;
	}
	/**
	 * Reads the parameter values from a file and sets them.
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	private boolean setParameters(String filePath) throws IOException {
		File file;
		String line;
		String name;
		String value;
		String[] arrayValues;
		byte[] byteArray;
		Class<? extends Parameters> clazz = this.getClass();
		Field field;
		int indexOfClosingNameTag;
		file = new File(filePath);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.exists() ? new FileInputStream(filePath) : 
			getClass().getResourceAsStream(filePath)));) {
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
						break;
					case "Long":
						field.set(this, Long.parseLong(value));
						break;
					case "Float":
						field.set(this, Float.parseFloat(value));
						break;
					case "Double":
						field.set(this, Double.parseDouble(value));
					}
				}
			}
			return true;
		}
		catch (FileNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * Overwrites the default parameters file, params.txt, with the state of this instance.
	 * 
	 * @return
	 */
	public boolean writeToFile() {
		return writeToFile(DEFAULT_PARAMETERS_FILE_PATH);
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
