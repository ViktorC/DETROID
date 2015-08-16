package util;

/**A generic implementation of the quickSort algorithm to order arrays of objects.
 * 
 * @author Viktor
 *
 * @param <Entry>
 */
public class QuickSort<Entry extends Comparable<Entry>> {

	private Entry[] array;
	
	public QuickSort(Entry[] array) {
		this.array = array;
		this.sort();
	}
	public QuickSort(Queue<Entry> container) {
		this.array = container.toArray();
		this.sort();
	}
	/**Returns the sorted container as an array.
	 * 
	 * @return
	 */
	public Entry[] getArray() {
		return this.array;
	}
	/**Returns the sorted container as a queue.
	 * 
	 * @return
	 */
	public Queue<Entry> getQueue() {
		Queue<Entry> q = new Queue<Entry>();
		for (Entry e : this.array)
			q.add(e);
		return q;
	}
	private void sort() {
		int left = 0;
		int right = this.array.length - 1;
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
		Entry pivot = this.array[(left + right)/2];
		Entry temp;
		while (left <= right) {
			while (this.array[left].greaterThan(pivot))
				left++;
			while (this.array[right].smallerThan(pivot))
				right--;
			if (left <= right) {
				temp = this.array[left];
				this.array[left] = this.array[right];
				this.array[right] = temp;
				left++;
				right--;
			}
		}
		return left;
	}
}
