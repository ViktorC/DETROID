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
 * parameters. It provides methods for the serialization and deserialization of the fields of its sub classes which are utilized in the methods provided
 * for writing the contents of an instance to a file or reading, parsing, and setting the fields of an instance based on such a file. It is also possible
 * to retrieve a binary string that represents the genotype of the instance with each field's value gray coded and concatenated into a binary string. A
 * string like that can be then used to initilaize instances with the same procedure reversed. Fields of the subclasses can be annotated with the
 * {@link #LimitBinaryLength LimitBinaryLength} annotation which marks the number of bits to consider when tuning the parameter. Even though floating
 * points are allowed, their use is discouraged as it makes convergence less likely during optimization.
 * 
 * WARNING: It only supports non-static primitive fields declared in its subclasses! All other fields need to be marked transient or a
 * {@link #ClassFormatError ClassFormatError} is thrown by the constructor. When initializing from a file, transient fields will still be set if
 * declared in the file, but will not be included in any of the output formats. Fields with a binary length limited to 0 by a
 * {@link #LimitBinaryLength LimitBinaryLength} annotation, however, will be included in the {@link #toString() toString} output and thus written to file;
 * they will be only ignored during the optimization process.
 * 
 * WARNING: No negative values are supported, thus the most significant bit of each signed primitive (all except boolean and char) field will be ignored.
 * 
 * @author Viktor
 * 
 */
public abstract class Parameters {

	/**
	 * If this string is encountered while parsing a parameters file, parsing is terminated. It can be inserted into a file that contains information that
	 * should not be parsed, signalling the start of this part.
	 */
	public final transient static String END_OF_FILE_MARK = "#END#";
	
	private final transient ArrayList<Field> fields;
	
	/**
	 * Checks if the fields comply with the requirements of {@link #Parameters Parameters} and throws an {@link #ClassFormatError ClassFormatError} if not.
	 * 
	 * @throws ClassFormatError
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
	 * Sets the values of the fields based on the binaryString in which each character represents a bit in the string of the individual gray code strings of
	 * the non-transient fields of the instance in the order of declaration.
	 * 
	 * @param binaryString
	 * @param value
	 */
	public final boolean initFromGrayCodeString(String binaryString) {
		try {
			Class<?> fieldType;
			LimitBinaryLength ann;
			byte bitLimit;
			int i = 0;
			for (Field f : fields) {
				fieldType = f.getType();
				ann = f.getAnnotation(LimitBinaryLength.class);
				bitLimit = ann != null && ann.value() >= 0 ? ann.value() : 63;
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
					f.set(this, Float.intBitsToFloat((int) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 31))), 2))));
				else if (fieldType.equals(double.class))
					f.set(this, Double.longBitsToDouble(GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 63))), 2))));
				else if (fieldType.equals(char.class))
					f.set(this, (char) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i, (i += Math.min(bitLimit, 16))), 2)));
			}
			return true;
		} catch (IllegalAccessException e) { return false; }
	}
	/**
	 * Reads the parameter values from a file and sets them.
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	public final boolean initFromFile(String filePath) throws IOException {
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
				if (line.contains("#END#"))
					break;
				indexOfClosingNameTag = line.indexOf(']');
				name = line.substring(line.indexOf('[') + 1, indexOfClosingNameTag);
				field = clazz.getDeclaredField(name);
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
		catch (FileNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * Writes the parameters to the specified file. If it does not exist, this method will attempt to create it. The method returns whether writing to the file
	 * was successful.
	 * 
	 * @param filePath
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
	 * Returns a binary String of all the bits of all the fields of the instance concatenated field by field.
	 * 
	 * @return
	 */
	public final String toGrayCodeString() {
		Object fieldValue;
		LimitBinaryLength ann;
		byte bitLimit;
		String genome = "";
		try {
			for (Field f : fields) {
				fieldValue = f.get(this);
				ann = f.getAnnotation(LimitBinaryLength.class);
				bitLimit = (byte) (ann != null && ann.value() >= 0 ? 64 - ann.value() : 0);
				if (bitLimit == 64)
					continue;
				if (fieldValue instanceof Boolean)
					genome += ((Boolean) fieldValue) ? "1" : "0";
				else if (fieldValue instanceof Byte)
					genome += BitOperations.toBinaryString(GrayCode.encode((Byte) fieldValue)).substring(Math.max(bitLimit, 57));
				else if (fieldValue instanceof Short)
					genome += BitOperations.toBinaryString(GrayCode.encode((Short) fieldValue)).substring(Math.max(bitLimit, 49));
				else if (fieldValue instanceof Integer)
					genome += BitOperations.toBinaryString(GrayCode.encode((Integer) fieldValue)).substring(Math.max(bitLimit, 33));
				else if (fieldValue instanceof Long)
					genome += BitOperations.toBinaryString(GrayCode.encode((Long) fieldValue)).substring(Math.max(bitLimit, 1));
				else if (fieldValue instanceof Float)
					genome += BitOperations.toBinaryString(GrayCode.encode(Float.floatToIntBits((Float) fieldValue))).substring(Math.max(bitLimit, 33));
				else if (fieldValue instanceof Double)
					genome += BitOperations.toBinaryString(GrayCode.encode(Double.doubleToLongBits((Double) fieldValue))).substring(Math.max(bitLimit, 1));
				else if (fieldValue instanceof Character)
					genome += BitOperations.toBinaryString(GrayCode.encode(Character.getNumericValue((Character) fieldValue))).substring(Math.max(bitLimit, 48));
			}
			return genome;
		}
		catch (IllegalArgumentException | IllegalAccessException e) { return null; }
	}
	@Override
	public String toString() {
		String out = "";
		String subString;
		for (Field f : fields) {
			subString = "";
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
