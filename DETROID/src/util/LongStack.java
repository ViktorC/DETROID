package util;

public class LongStack {

	protected long data;
	protected LongStack next;

	public LongStack() {

	}
	public LongStack(long data) {
		this.data = data;
	}
	public LongStack(LongStack stack) {
		this.data = stack.data;
		this.next = stack.next;
	}
	public long getData() {
		return this.data;
	}
	public LongStack getNext() {
		return this.next;
	}
	public void setData(long data) {
		this.data = data;
	}
	public boolean isEmpty() {
		if (this != null) {
			if (this.data == 0 && this.next == null)
				return true;
		}
		return false;
	}
	public void add(long data) {
		LongStack temp = new LongStack(this);
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
			LongStack temp = this;
			while (temp.next != null) {
				if (temp.getNext().data == data) {
					LongStack trash = temp.getNext();
					temp.next = temp.getNext().getNext();
					trash.next = null;
					break;
				}
				temp = temp.getNext();
			}
		}
	}
}
