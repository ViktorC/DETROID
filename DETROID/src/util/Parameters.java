package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An abstract base class for tunable parameter definitions used in a system such as a chess engine or other applications requiring highly optimized
 * parameters. It provides methods for the retrieval of the values of the fields of its sub classes which are utilized in the methods provided for 
 * writing the contents of an instance to a file or reading, parsing, and setting the fields of an instance based on such a file. It is possible to 
 * retrieve the values as an array of doubles or as a binary string that represents the genotype of the instance with each field's value gray coded 
 * and concatenated into a binary string. An array or string like that can be then used to set the values of the fields of instances with the same 
 * procedure reversed. Fields of the subclasses can be annotated with the {@link #LimitBinaryLength LimitBinaryLength} annotation which marks the 
 * number of bits to consider when tuning the parameter. When applied to floating point values, it's effects are less intuituve as instead of ignoring 
 * bits of the number's binary representation, it just sets a maximum for the values it can take on which will be equal to what this maximum would be 
 * for integer numbers (2^[limit] - 1).
 * 
 * WARNING: It only supports non-static primitive fields declared in its subclasses! All other fields need to be marked transient or a {@link 
 * #ClassFormatError ClassFormatError} is thrown by the constructor. When initializing from a file, transient fields will still be set if declared in 
 * the file, but will not be included in any of the output formats. Fields with a binary length limited to 0 by a {@link #LimitBinaryLength LimitBinaryLength} 
 * annotation, however, will be included in the {@link #toString() toString} output and thus written to file; they will be only ignored during the 
 * optimization process.
 * 
 * WARNING: No negative values are supported, thus the most significant bit of each signed primitive (all except boolean and char) field will be ignored
 * when generating the binary string or setting the valeues of the fields based on a binary string and negative values will default to 0 when setting the
 * fields based on a double array.
 * 
 * @author Viktor
 * 
 */
public abstract class Parameters {

	/**
	 * If this string is encountered while parsing a parameters file, parsing is terminated. It can be inserted into a file that contains information that
	 * should not be parsed, signalling the start of this part.
	 */
	public final transient static String END_OF_FILE_MARK = "#EoF!";
	
	private final transient ArrayList<Field> fields;
	
	/**
	 * Checks if the fields comply with the requirements of {@link #Parameters Parameters} and throws an {@link #ClassFormatError ClassFormatError} if not.
	 * 
	 * @throws ClassFormatError If the fields do not comply with the contract of {@link #Parameters Parameters}.
	 */
	protected Parameters() throws ClassFormatError {
		fields = new ArrayList<>();
		int modifiers;
		Class<?> fieldType;
		Class<?> clazz = getClass();
		Class<?> temp = clazz;
		ArrayList<Field> allfields = new ArrayList<>();
		while (temp != null) {
			allfields.addAll(Arrays.asList(temp.getDeclaredFields()));
			temp = temp.getSuperclass();
		}
		for (Field f : allfields) {
			f.setAccessible(true);
			modifiers = f.getModifiers();
			if (Modifier.isTransient(modifiers))
				continue;
			fieldType = f.getType();
			if (!fieldType.isPrimitive() || Modifier.isStatic(modifiers))
				throw new ClassFormatError("Illegal field: " + f.getName() + "; With the exception of transient fields, only non-static primitive, that is" +
						"boolean, byte, short, int, long, float, double, and char fields are allowed in subclasses of Parameters.");
			fields.add(f);
		}
	}
	/**
	 * Sets the values of the non-transient fields of the instance. If a value in the array is negative, it will be taken for 0; if a value is greater than
	 * what the respective field's type or bitlimit specified by the {@link #LimitBinaryLength LimitBinaryLength} annotation would allow for, the field will 
	 * be set to its maximum allowed value. If the array is longer than the number of non-transient fields, the extra elementes will be ignored; if it is 
	 * shorter, the fields indexed higher than the length of the array will not be set. For boolean fields, a value greater than or equal to 1 will default 
	 * to true, everything else will default to false.
	 * 
	 * @param values An array of values for the non-transient fields of the instance.
	 * @return Whether at least some of the fields could be successfully set.
	 */
	public final boolean set(double[] values) {
		try {
			if (values == null)
				return false;
			int lim = Math.min(fields.size(), values.length);
			for (int i = 0; i < lim; i++) {
				Field f = fields.get(i);
				LimitBinaryLength ann = f.getAnnotation(LimitBinaryLength.class);
				byte bitLimit = ann != null && ann.value() >= 0 ? ann.value() : 63;
				if (bitLimit == 0)
					continue;
				Class<?> fieldType = f.getType();
				double value = values[i];
				value = Math.max(value, 0);
				value = Math.min(value, (1L << bitLimit) - 1);
				if (fieldType.equals(boolean.class))
					f.setBoolean(this, value >= 1);
				else if (fieldType.equals(byte.class))
					f.setByte(this, (byte) Math.min(value, Byte.MAX_VALUE));
				else if (fieldType.equals(short.class))
					f.setShort(this, (short) Math.min(value, Short.MAX_VALUE));
				else if (fieldType.equals(int.class))
					f.setInt(this, (int) Math.min(value, Integer.MAX_VALUE));
				else if (fieldType.equals(long.class))
					f.setLong(this, (long) Math.min(value, Long.MAX_VALUE));
				else if (fieldType.equals(float.class))
					f.setFloat(this, (float) Math.min(value, Float.MAX_VALUE));
				else if (fieldType.equals(double.class))
					f.setDouble(this, Math.min(value, Double.MAX_VALUE));
				else if (fieldType.equals(char.class))
					f.setChar(this, (char) Math.min(value, Character.MAX_VALUE));
			}
			return true;
		} catch (IllegalAccessException e) {
			return false;
		}
	}
	/**
	 * Sets the values of the fields based on the binaryString in which each character represents a bit in the string of the individual gray code strings of
	 * the non-transient fields of the instance in the order of declaration.
	 * 
	 * @param binaryString A binary string of gray code such as the output of {@link #toGrayCodeString() toGrayCodeString}.
	 * @return Whether at least some of the fields could be successfully set.
	 */
	public final boolean set(String binaryString) {
		try {
			int i = 0;
			for (Field f : fields) {
				Class<?> fieldType = f.getType();
				LimitBinaryLength ann = f.getAnnotation(LimitBinaryLength.class);
				byte bitLimit = ann != null && ann.value() >= 0 ? ann.value() : 63;
				if (bitLimit == 0)
					continue;
				if (fieldType.equals(boolean.class))
					f.set(this, "1".equals(binaryString.substring(i, ++i)));
				else if (fieldType.equals(byte.class)) 
					f.set(this, (byte) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 7))), 2)));
				else if (fieldType.equals(short.class))
					f.set(this, (short) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 15))), 2)));
				else if (fieldType.equals(int.class))
					f.set(this, (int) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 31))), 2)));
				else if (fieldType.equals(long.class))
					f.set(this, GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 63))), 2)));
				else if (fieldType.equals(float.class))
					f.set(this, (float) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 31))), 2)));
				else if (fieldType.equals(double.class))
					f.set(this, GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 63))), 2)));
				else if (fieldType.equals(char.class))
					f.set(this, (char) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 16))), 2)));
			}
			return true;
		} catch (IllegalAccessException e) {
			return false;
		}
	}
	/**
	 * Reads the parameter values from a file until the end of file mark or the actual end of the file is reached and sets the instance's fields accordingly.
	 * 
	 * @param filePath The path to the file.
	 * @return Whether at least some of the fields could be successfully set.
	 * @throws IOException
	 */
	public final boolean loadFrom(String filePath) throws IOException {
		File file;
		String line;
		String name;
		String value;
		Class<?> clazz = getClass();
		Field field;
		Class<?> fieldType;
		int indexOfClosingNameTag;
		file = new File(filePath);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.exists() ? new FileInputStream(filePath) : 
			clazz.getResourceAsStream(filePath)));) {
			while ((line = reader.readLine()) != null) {
				if (line.contains(END_OF_FILE_MARK))
					break;
				indexOfClosingNameTag = line.indexOf(']');
				name = line.substring(line.indexOf('[') + 1, indexOfClosingNameTag);
				try {
					field = clazz.getDeclaredField(name);
				} catch (NoSuchFieldException | SecurityException e1) {
					e1.printStackTrace();
					continue;
				}
				field.setAccessible(true);
				value = line.substring(indexOfClosingNameTag + 4, line.length());
				if (value.length() == 0)
					continue;
				fieldType = field.getType();
				if (fieldType.equals(boolean.class))
					field.set(this, Boolean.parseBoolean(value));
				else if (fieldType.equals(byte.class))
					field.set(this, Byte.parseByte(value));
				else if (fieldType.equals(short.class))
					field.set(this, Short.parseShort(value));
				else if (fieldType.equals(int.class))
					field.set(this, Integer.parseInt(value));
				else if (fieldType.equals(long.class))
					field.set(this, Long.parseLong(value));
				else if (fieldType.equals(float.class))
					field.set(this, Float.parseFloat(value));
				else if (fieldType.equals(double.class))
					field.set(this, Double.parseDouble(value));
				else if (fieldType.equals(char.class))
					field.set(this, value.charAt(0));
			}
			return true;
		}
		catch (FileNotFoundException | IllegalArgumentException | IllegalAccessException e2) {
			e2.printStackTrace();
			return false;
		}
	}
	/**
	 * Writes the parameters to the specified file. If it does not exist, this method will attempt to create it. The method returns whether writing to the file
	 * was successful.
	 * 
	 * @param filePath The path to the file.
	 * @return
	 */
	public final boolean writeToFile(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			try {
				Files.createFile(file.toPath());
			} catch (IOException e1) {
				return false;
			}
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(toString());
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	/**
	 * Returns an array of doubles holding the values of all non-transient fields of the instance. Boolean values will be converted 
	 * to either 1 or 0 depending on whether they are true or false.
	 * 
	 * @return
	 */
	public final double[] values() {
		double[] arr = new double[fields.size()];
		int i = 0;
		for (Field f : fields) {
			try {
				if (f.getType().equals(boolean.class))
					arr[i++] = (f.getBoolean(this) ? 1 : 0);
				else
					arr[i++] = f.getDouble(this);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return arr;
	}
	/**
	 * Returns an array of doubles holding the maximum allowed values for all non-transient fields of the instance determined by the 
	 * field type and the {@link #LimitBinaryLength LimitBinaryLength} annotations.
	 * 
	 * @return
	 */
	public final double[] maxValues() {
		double[] arr = new double[fields.size()];
		int i = 0;
		for (Field f : fields) {
			Class<?> fieldType = f.getType();
			LimitBinaryLength ann = f.getAnnotation(LimitBinaryLength.class);
			long max = (ann != null ? 1L << ann.value() - 1 : Long.MAX_VALUE);
			if (max == 0)
				continue;
			if (fieldType.equals(boolean.class))
				arr[i++] = 1;
			else if (fieldType.equals(byte.class))
				arr[i++] = Math.min(max, Byte.MAX_VALUE);
			else if (fieldType.equals(short.class))
				arr[i++] = Math.min(max, Short.MAX_VALUE);
			else if (fieldType.equals(int.class))
				arr[i++] = Math.min(max, Integer.MAX_VALUE);
			else if (fieldType.equals(long.class))
				arr[i++] = max;
			else if (fieldType.equals(float.class))
				arr[i++] = Math.min(max, Integer.MAX_VALUE);
			else if (fieldType.equals(double.class))
				arr[i++] = max;
			else if (fieldType.equals(char.class))
				arr[i++] = Math.min(max, Character.MAX_VALUE);
		}
		return arr;
	}
	/**
	 * Returns a binary string of all the bits of all the non-tansient fields of the instance concatenated field by field. Floating point 
	 * values will be cast to integer values (float to int, double to long) for their binary representation which may result in information 
	 * loss.
	 * 
	 * @return
	 */
	public final String toGrayCodeString() {
		String genome = "";
		try {
			for (Field f : fields) {
				Object fieldValue = f.get(this);
				LimitBinaryLength ann = f.getAnnotation(LimitBinaryLength.class);
				byte bitLimit = (byte) (ann != null && ann.value() >= 0 ? 64 - ann.value() : 0);
				if (bitLimit == 64)
					continue;
				if (fieldValue instanceof Boolean)
					genome += f.getBoolean(this) ? "1" : "0";
				else if (fieldValue instanceof Byte)
					genome += BitOperations.toBinaryString(GrayCode.encode(f.getByte(this))).substring(Math.max(bitLimit, 57));
				else if (fieldValue instanceof Short)
					genome += BitOperations.toBinaryString(GrayCode.encode(f.getShort(this))).substring(Math.max(bitLimit, 49));
				else if (fieldValue instanceof Integer)
					genome += BitOperations.toBinaryString(GrayCode.encode(f.getInt(this))).substring(Math.max(bitLimit, 33));
				else if (fieldValue instanceof Long)
					genome += BitOperations.toBinaryString(GrayCode.encode(f.getLong(this))).substring(Math.max(bitLimit, 1));
				else if (fieldValue instanceof Float)
					genome += BitOperations.toBinaryString(GrayCode.encode((int) f.getFloat(this))).substring(Math.max(bitLimit, 33));
				else if (fieldValue instanceof Double)
					genome += BitOperations.toBinaryString(GrayCode.encode((long) f.getDouble(this))).substring(Math.max(bitLimit, 1));
				else if (fieldValue instanceof Character)
					genome += BitOperations.toBinaryString(GrayCode.encode(Character.getNumericValue(f.getChar(this)))).substring(Math.max(bitLimit, 48));
			}
			return genome;
		}
		catch (IllegalArgumentException | IllegalAccessException e) { return null; }
	}
	@Override
	public String toString() {
		String out = "";
		for (Field f : fields) {
			String subString = "";
			try {
				subString += "[" + f.getName() + "] = " + f.get(this).toString() + "\n";
			} catch (IllegalArgumentException | IllegalAccessException e) {
				subString = "";
			}
			out += subString;
		}
		return out;
	}
}
