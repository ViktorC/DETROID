package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;
import java.util.Random;

public class Playground {

	public static void main(String[] args) {
		Random rand = new Random();
		LongList moves = new LongStack();
		for (int i = 0; i < 10; i++)
			moves.add(rand.nextLong());
		while (moves.hasNext()) {
			System.out.println(moves.next());
		}
	}
}
