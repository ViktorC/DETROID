package main;

import engine.Detroid;
import tuning.Arena;
import tuning.Elo;

public class TestLauncher {

	public static void main(String[] args) {
		Detroid d = new Detroid();
		d.init();
		d.getParameters().set("1100110110001010000010101100000010111111111101000101001101000001110101110000111000011010110010111110000100100010000100000111110011");
		System.out.println(d.getParameters());
		Detroid o = new Detroid();
		o.init();
		Arena a = new Arena(new Detroid());
		System.out.println(Elo.calculateDifference(a.match(d, o, 104, 2000, 20)));
		a.close();
		d.quit();
		o.quit();
	}

}
