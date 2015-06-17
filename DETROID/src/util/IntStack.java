package util;

public class IntStack extends IntList {

	public IntStack() {
		
	}
	public IntStack(int data) {
		this.data = data;
	}
	public IntStack(IntStack stack) {
		this.data = stack.data;
		this.next = stack.next;
	}
	public void add(int data) {
		IntList temp = new IntStack(this);
		this.data = data;
		this.next = temp;
	}
}
