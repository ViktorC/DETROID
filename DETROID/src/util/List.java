package util;

import java.util.Collection;
import java.util.Iterator;

/**
 * A low level generic abstract class for providing the basics for list data structures such as stacks and queues. It is also an iterator itself
 * and has to be manually reset when needed.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public abstract class List<T> implements Collection<T>, Iterator<T> {

	/**
	 * A list entry containing two fields, 'data' that holds the data, and 'next' referencing the subsequent element in the list
	 * 
	 * @author Viktor
	 *
	 */
	protected class ListItem {
		
		protected T data;
		protected ListItem next;
		
		ListItem(T data) {
			this.data = data;
		}
	}
	
	protected ListItem head;
	protected ListItem iterator;			// Used for keeping track of the current node while iterating over the list.
	
	/**
	 * Returns the data held in the first element of the list. If the list is empty, it returns null.
	 * 
	 * @return
	 */
	public T getHead() {
		if (head != null)
			return head.data;
		return null;
	}
	/**
	 * Adds all nodes of another list of the same generic type to this list.
	 * 
	 * @param list
	 */
	@Override
	public boolean addAll(Collection<? extends T> collection) {
		if (collection == null)
			return false;
		for (T e : collection)
			add(e);
		return true;
	}
	/**
	 * Returns whether the pointer/iterator has more nodes to process or has already reached the end of the list. Once the iterator has no more
	 * nodes no to process, the method will return 'false' and the iterator will be reset.
	 * 
	 * @return
	 */
	@Override
	public boolean hasNext() {
		if (iterator == null) {
			reset();
			return false;
		}
		return true;
	}
	/**
	 * Returns the data held in the pointer and sets it to the next element.
	 * 
	 * @return
	 */
	@Override
	public T next() {
		T next = iterator.data;
		iterator = iterator.next;
		return next;
	}
	/**
	 * Resets the iterator to the head of the list.
	 */
	public void reset() {
		iterator = head;
	}
	/**
	 * Removes the head node form the list and returns the data stored in it.
	 *  
	 * If there is nothing to pop, it returns null.
	 * 
	 * @return
	 */
	public T pop() {
		if (head != null) {
			T data = head.data;
			if (head.next != null)
				head = head.next;
			else
				head = null;
			reset();
			return data;
		}
		return null;
	}
	@Override
	public Iterator<T> iterator() {
		return this;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <S> S[] toArray(S[] a) {
		int i = 0;
		if (head == null)
			return null;
		while (hasNext()) {
			a[i] = (S) next();
			i++;
		}
		return a;
	}
	@Override
	public Object[] toArray() {
		Object[] a = new Object[size()];
		return toArray(a);
	}
	@Override
	public void clear() {
		head = null;
		reset();
	}
	@Override
	public boolean contains(Object o) {
		while (hasNext()) {
			if (next().equals(o)) {
				reset();
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (!contains(e))
				return false;
		}
		return true;
	}
	@Override
	public boolean isEmpty() {
		return head == null;
	}
	@Override
	public boolean remove(Object o) {
		ListItem item, prevItem;
		if (head == null)
			return false;
		item = head;
		prevItem = null;
		do {
			if (item.data.equals(o)) {
				if (prevItem != null)
					prevItem.next = item.next;
				else
					head = item.next;
				return true;
			}
			item = item.next;
		}
		while (item != null);
		return false;
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		for (Object e : c) {
			if (!remove(e))
				return false;
		}
		return true;
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		T e;
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		while (hasNext()) {
			e = next();
			if (!c.contains(e))
				remove(e);
		}
		return true;
	}
	/**
	 * Returns the data held in the last element of the list. If the list is empty, it returns null.
	 *
	 * @return
	 */
	public abstract T getTail();
}
