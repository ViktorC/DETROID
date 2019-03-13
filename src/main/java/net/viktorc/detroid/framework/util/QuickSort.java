package net.viktorc.detroid.framework.util;

/**
 * An implementation of the quickSort algorithm to sort arrays of generic objects. The objects to be sorted have to implement the {@link
 * java.lang.Comparable} interface.
 *
 * @author Viktor
 */
public final class QuickSort {

  private QuickSort() {

  }

  /**
   * Sorts and returns the array. The elements have to implement the {@link java.lang.Comparable} interface.
   *
   * @param <T> The type of the comparable elements.
   * @param array The array to be sorted.
   * @return The sorted array.
   */
  public static <T extends Comparable<T>> T[] sort(T[] array) {
    if (array.length > 1) {
      quickSort(array, 0, array.length - 1);
    }
    return array;
  }

  /**
   * The quicksort algorithm.
   *
   * @param array The array to sort.
   * @param beg The index of the first element of the array partition.
   * @param end The index of the last element of the array partition.
   */
  private static <T extends Comparable<T>> void quickSort(T[] array, int beg, int end) {
    T pivot = array[(beg + end) / 2];
    int j = end;
    for (int i = beg; ; ) {
      while (array[i].compareTo(pivot) > 0) {
        i++;
      }
      while (array[j].compareTo(pivot) < 0) {
        j--;
      }
      if (i < j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
        i++;
        j--;
        if (i >= j) {
          break;
        }
      } else {
        break;
      }
    }
    if (beg < j) {
      quickSort(array, beg, j);
    }
    if (j + 1 < end) {
      quickSort(array, j + 1, end);
    }
  }

}
