package util;

public abstract class IntList {
	
	protected int data;
	protected IntList next;
	
	public IntList() {
		
	}
	public IntList(int data) {
		this.data = data;
	}
	public int getData() {
		return this.data;
	}
	public IntList getNext() {
		return this.next;
	}
	public void setData(int data) {
		this.data = data;
	}
	public boolean isEmpty() {
		if (this != null) {
			if (this.data == 0 && this.next == null)
				return true;
		}
		return false;
	}
	public abstract void add(int data);
	public void pop() {
		if (this.next != null) {
			this.data = this.next.data;
			this.next = this.next.next;
		}
		else {
			this.data = 0;
			this.next = null;
		}
	}
	public void remove(int data) {
		if (this.next == null) {
			if (this.data == data)
				this.data = 0;
		}
		else {
			IntList temp = this;
			while (temp.next != null) {
				if (temp.getNext().data == data) {
					IntList trash = temp.getNext();
					temp.next = temp.getNext().getNext();
					trash.next = null;
					break;
				}
				temp = temp.getNext();
			}
		}
	}
}
