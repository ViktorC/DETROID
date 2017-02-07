package net.viktorc.detroid.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A singleton utility class for estimating the memory footprints of objects on the (HotSpot) JVM heap.
 * 
 * @author Viktor
 *
 */
public class SizeEstimator {

	private static final SizeEstimator INSTANCE = new SizeEstimator();
	
	/**
	 * Returns the single existing SizeEstimator instance.
	 * 
	 * @return A singleton instance.
	 */
	public static SizeEstimator getInstance() {
		return INSTANCE;
	}
	private SizeEstimator() {
		
	}
	/**
	 * Returns the total memory overhead in bytes.
	 * 
	 * @param baseSize The raw size of the object including the object header.
	 * @return The estimated memory the object consumes in the heap.
	 */
	private long roundedSize(long baseSize) {
		// The JVM rounds the allocated memory up to the closest multiple of 8.
		return baseSize%8 == 0 ? baseSize : baseSize + 8 - baseSize%8;
	}
	/**
	 * Returns an array list of all the non-static fields of a class including the non-static fields of all of its ancestors.
	 * 
	 * @param clazz The class.
	 * @return A list of its non-static fields including those inherited.
	 */
	public ArrayList<Field> getNonStaticFields(Class<?> clazz) {
		ArrayList<Field> fields = new ArrayList<>();
		// Get all non-static fields, including those of the super classes if there are any.
		do {
			for (Field f : clazz.getDeclaredFields()) {
				if (!Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					fields.add(f);
				}
			}
			clazz = clazz.getSuperclass();
		} while (clazz != null);
		return fields;
	}
	/**
	 * Returns whether all instances of the specified class must have the same memory requirements. This can be only guaranteed if all the non static
	 * fields of the class are primitives.
	 * 
	 * @param clazz The class.
	 * @return Whether the size of its instances is constant.
	 */
	public boolean isInstanceSizeConstant(Class<?> clazz) {
		ArrayList<Field> fields = getNonStaticFields(clazz);
		for (Field f : fields) {
			if (!f.getType().isPrimitive())
				return false;
		}
		return true;
	}
	/**
	 * Returns the size of a primitive field type.
	 * 
	 * @param fieldType The type of the field.
	 * @return The size of it.
	 */
	private byte sizeOfPrimitiveField(Class<?> fieldType) {
		if (fieldType.equals(boolean.class))
			return SizeOf.BOOLEAN.numOfBytes;
		else if (fieldType.equals(byte.class))
			return SizeOf.BYTE.numOfBytes;
		else if (fieldType.equals(short.class))
			return SizeOf.SHORT.numOfBytes;
		else if (fieldType.equals(int.class))
			return SizeOf.INT.numOfBytes;
		else if (fieldType.equals(long.class))
			return SizeOf.LONG.numOfBytes;
		else if (fieldType.equals(float.class))
			return SizeOf.FLOAT.numOfBytes;
		else if (fieldType.equals(double.class))
			return SizeOf.DOUBLE.numOfBytes;
		else if (fieldType.equals(char.class))
			return SizeOf.CHAR.numOfBytes;
		else
			throw new IllegalArgumentException();
	}
	/**
	 * Estimates the size of an object including the sizes of all objects referenced in the object graph.
	 * 
	 * @param o The object.
	 * @param visitedObjects A set of all visited objects in the object graph.
	 * @return The size of the object graph that the specified object is the root of.
	 */
	private long sizeOf(Object o, Set<Object> visitedObjects) {
		long size;
		Class<?> clazz;
		int arrLength;
		ArrayList<Field> fields;
		Class<?> fieldType;
		if (o == null)
			return SizeOf.OBJECT_POINTER.numOfBytes;
		// If the object has already been visited, only count the reference size to avoid loops.
		if (visitedObjects.contains(o))
			return SizeOf.OBJECT_POINTER.numOfBytes;
		clazz = o.getClass();
		// Only one instance exists on the heap per enum definition.
		if (clazz.isEnum())
			return SizeOf.OBJECT_POINTER.numOfBytes;
		// Add current node to the visited objects.
		visitedObjects.add(o);
		if (clazz.isArray()) {
			// Array header size.
			size = SizeOf.MARK_WORD.numOfBytes + SizeOf.OBJECT_POINTER.numOfBytes;
			size += SizeOf.INT.numOfBytes;
			arrLength = Array.getLength(o);
			if (arrLength > 0) {
				if (o instanceof boolean[])
					size += arrLength*SizeOf.BOOLEAN.numOfBytes;
				else if (o instanceof byte[])
					size += arrLength*SizeOf.BYTE.numOfBytes;
				else if (o instanceof short[])
					size += arrLength*SizeOf.SHORT.numOfBytes;
				else if (o instanceof int[])
					size += arrLength*SizeOf.INT.numOfBytes;
				else if (o instanceof long[])
					size += arrLength*SizeOf.LONG.numOfBytes;
				else if (o instanceof float[])
					size += arrLength*SizeOf.FLOAT.numOfBytes;
				else if (o instanceof double[])
					size += arrLength*SizeOf.DOUBLE.numOfBytes;
				else if (o instanceof char[])
					size += arrLength*SizeOf.CHAR.numOfBytes;
				else {
					for (int i = 0; i < arrLength; i ++)
						size += sizeOf(Array.get(o, i), visitedObjects);
				}
			}
		} else {
			// OO header size.
			size = SizeOf.MARK_WORD.numOfBytes + SizeOf.OBJECT_POINTER.numOfBytes;
			fields = getNonStaticFields(clazz);
			for (Field f : fields) {
				fieldType = f.getType();
				if (fieldType.isPrimitive())
					size += sizeOfPrimitiveField(fieldType);
				else {
					try {
						size += sizeOf(f.get(o), visitedObjects);
					} catch (IllegalAccessException e) { e.printStackTrace(); }
				}
			}
		}
		// Align the result by 8 bytes.
		return roundedSize(size);
	}
	/**
	 * Estimates the size of an object including the sizes of all objects referenced in the object graph. It traverses an acyclic graph by not visiting nodes more
	 * than once and thus ensuring that even if there are multiple references pointing to the same object, its size is added to the graph's total size only once.
	 * 
	 * @param o The object.
	 * @return The size of the object.
	 */
	public long sizeOf(Object o) {
		Set<Object> visitedNodes = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
		return sizeOf(o, visitedNodes);
	}
	/**
	 * Statically estimates the size of the instance of a class. The estimate is only reliable if
	 * {@link #isInstanceSizeConstant(Class<?>) isInstanceSizeConstant} returns true, i.e. all the non static fields of the class and its ancestors
	 * are primitives. The non-primitive non-static fields are assumed to be null references.
	 * 
	 * @param clazz The class.
	 * @return The static size of the class.
	 */
	public long sizeOf(Class<?> clazz) {
		Class<?> fieldType;
		// Object header.
		long size = SizeOf.MARK_WORD.numOfBytes + SizeOf.OBJECT_POINTER.numOfBytes;
		ArrayList<Field> fields = getNonStaticFields(clazz);
		for (Field f : fields) {
			fieldType = f.getType();
			if (fieldType.isPrimitive())
				size += sizeOfPrimitiveField(fieldType);
			else // Assume to be a null reference.
				size += SizeOf.OBJECT_POINTER.numOfBytes;
		}
		// 8-byte-alignment.
		return roundedSize(size);
	}
	
}
