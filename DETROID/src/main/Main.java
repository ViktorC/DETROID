package main;

import engine.Detroid;
import uci.UCI;

public class Main {

	public static void main(String[] args) {
		UCI.getInstance().run(new Detroid());
	}

}
