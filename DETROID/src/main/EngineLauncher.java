package main;

import java.io.IOException;

import engine.Detroid;
import uci.UCI;

public class EngineLauncher {

	public static void main(String[] args) {
		UCI uci = new UCI(System.in, System.out);
		uci.run(new Detroid());
		try {
			uci.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
