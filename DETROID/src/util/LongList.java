package util;

public abstract class LongList {
	
	protected long data;
	protected LongList next;
	
	public LongList() {
		
	}
	public LongList(long data) {
		this.data = data;
	}
	public long getData() {
		return this.data;
	}
	public LongList getNext() {
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
	public abstract void add(long data);
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
	public void remove(long data) {
		if (this.next == null) {
			if (this.data == data)
				this.data = 0;
		}
		else {
			LongList temp = this;
			while (temp.next != null) {
				if (temp.getNext().data == data) {
					LongList trash = temp.getNext();
					temp.next = temp.getNext().getNext();
					trash.next = null;
					break;
				}
				temp = temp.getNext();
			}
		}
	}
}
