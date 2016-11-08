package main;

import engine.Detroid;

public class TestLauncher {

	public static void main(String[] args) {
		Detroid d = new Detroid();
		d.init();
		d.getParameters().set("011110111110111011101010000001111000110110110011000011101010001001100111000111000101001001110110100000110010001001111000011000100001001100111001111100");
		System.out.println(d.getParameters());
	}

}
