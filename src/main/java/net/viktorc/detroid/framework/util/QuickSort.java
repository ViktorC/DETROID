package net.viktorc.detroid.framework.util;

/**
 * An implementation of the quickSort algorithm to sort arrays of generic objects. The objects to be sorted have to implement the
 * {@link java.lang.Comparable} interface.
 * 
 * @author Viktor
 *
 */
public final class QuickSort {
	
	private QuickSort() {
		
	}
	/**
	 * Sorts and returns the array. The elements have to implement the {@link java.lang.Comparable} interface.
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
	 * @param array The array to sort.
	 * @param left The index of the left head.
	 * @param right The index of the right head.
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
