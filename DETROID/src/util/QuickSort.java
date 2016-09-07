package util;

import java.util.Collection;

/**
 * An implementation of the quickSort algorithm to sort arrays of generic objects. The objects to be sorted have to implement the
 * {@link #Comparable Comparable} interface.
 * 
 * @author Viktor
 *
 */
public final class QuickSort {
	
	private QuickSort() {
		
	}
	/**
	 * Returns a sorted array of the container's elements if its length is greater than 0. The elements have to implement the {@link #Comparable Comparable}
	 * interface. It is recommended to use {@link #java.util.Collection Collection}'s {@link #java.util.Collection.sort sort} method if performance is important.
	 * 
	 * @param container The list to be sorted.
	 * @return A sorted array.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> T[] sort(Collection<T> container) {
		int left, right;
		T[] array;
		if (container.size() != 0) {
			array = (T[]) container.toArray();
			left = 0;
			right = array.length - 1;
			quickSort(array, left, right);
			return array;
		}
		return null;
	}
	/**
	 * Sorts and returns the array. The elements have to implement the {@link #Comparable Comparable} interface.
	 * 
	 * @param array The array to be sorted.
	 * @return The sorted array.
	 */
	public static <T extends Comparable<T>> T[] sort(T[] array) {
		int left, right;
		if (array.length != 0) {
			left = 0;
			right = array.length - 1;
			quickSort(array, left, right);
		}
		return array;
	}
	/**
	 * The quickSort algorithm.
	 * 
	 * @param array
	 * @param left
	 * @param right
	 */
	private static <T extends Comparable<T>> void quickSort(T[] array, int left, int right) {
		int index, partLeft = left, partRight = right;
		T temp, pivot = array[(left + right)/2];
		while (partLeft <= partRight) {
			while (array[partLeft].compareTo(pivot) > 0)
				partLeft++;
			while (array[partRight].compareTo(pivot) < 0)
				partRight--;
			if (partLeft <= partRight) {
				temp = array[partLeft];
				array[partLeft] = array[partRight];
				array[partRight] = temp;
				partLeft++;
				partRight--;
			}
		}
		index = partLeft;
		if (left < index - 1)
			quickSort(array, left, index - 1);
		if (index < right)
			quickSort(array, index, right);
	}
}
