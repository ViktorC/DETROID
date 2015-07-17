package util;

public class IntStack {
	
	protected int data;
	protected IntStack next;
	
	public IntStack() {

	}
	public IntStack(int data) {
		this.data = data;
	}
	public IntStack(IntStack stack) {
		this.data = stack.data;
		this.next = stack.next;
	}
	public int getData() {
		return this.data;
	}
	public IntStack getNext() {
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
	public void add(int data) {
		IntStack temp = new IntStack(this);
		this.data = data;
		this.next = temp;
	}
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
			IntStack temp = this;
			while (temp.next != null) {
				if (temp.getNext().data == data) {
					IntStack trash = temp.getNext();
					temp.next = temp.getNext().getNext();
					trash.next = null;
					break;
				}
				temp = temp.getNext();
			}
		}
	}
}
