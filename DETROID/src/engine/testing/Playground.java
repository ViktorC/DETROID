package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		long start = System.currentTimeMillis();
		b.perftWithConsoleOutput(2, 0, 401, true);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
