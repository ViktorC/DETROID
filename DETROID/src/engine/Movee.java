package engine;

public class Movee {
	
	int from;
	int to;
	int type;
	int value;

	public Movee() {
		
	}
	public Movee(int from) {
		this.from = from;
	}
	public Movee(int from, int to, int type, int value) {
		this.from = from;
		this.to = to;
		this.type = type;
		this.value = value;
	}
}
