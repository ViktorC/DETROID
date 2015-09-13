package util;

/**A generic implementation of the quickSort algorithm to order arrays of objects.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public class QuickSort<T extends Comparable<T>> {

	private T[] array;
	
	public QuickSort(T[] array) {
		this.array = array;
		if (array.length != 0)
			sort();
	}
	public QuickSort(List<T> container) {
		array = container.toArray();
		if (array.length != 0)
			sort();
	}
	/**Returns the sorted container as an array.
	 * 
	 * @return
	 */
	public T[] getArray() {
		return array;
	}
	/**Returns the sorted container as a queue.
	 * 
	 * @return
	 */
	public Queue<T> getQueue() {
		Queue<T> q = new Queue<T>();
		for (T e : array)
			q.add(e);
		return q;
	}
	private void sort() {
		int left = 0;
		int right = array.length - 1;
		this.quickSort(left, right);
	}
	private void quickSort(int left, int right) {
		int index = partition(left, right);
		if (left < index - 1)
			quickSort(left, index - 1);
		if (index < right)
			quickSort(index, right);
	}
	private int partition(int left, int right) {
		T pivot = array[(left + right)/2];
		T temp;
		while (left <= right) {
			while (array[left].betterThan(pivot))
				left++;
			while (array[right].worseThan(pivot))
				right--;
			if (left <= right) {
				temp = array[left];
				array[left] = array[right];
				array[right] = temp;
				left++;
				right--;
			}
		}
		return left;
	}
}
