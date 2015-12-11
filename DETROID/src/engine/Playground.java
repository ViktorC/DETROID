package engine;

public class Playground {

	static String tP1 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
	static String tP2 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
	static String tP3 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
	
	public static void main(String[] args) {
		Position p = new Position(tP1);
		long start = System.currentTimeMillis();
		Game g = Game.getInstance("[Event \"F/S Return Match\"]" +
				"[Site \"Belgrade, Serbia Yugoslavia|JUG\"]" +
						"[Date \"1992.11.04\"]" +
						"[Round \"29\"]" +
						"[White \"Fischer, Robert J.\"]" +
						"[Black \"Spassky, Boris V.\"]" +
						"[Result \"*\"]" +
						"		" +
						"1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 {This opening is called the Ruy Lopez.}\n" +
						"4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Nb8  10. d4 Nbd7\n" +
						"11. c4 c6 12. cxb5 axb5 13. Nc3 Bb7 14. Bg5 b4 15. Nb1 h6 16. Bh4 c5 17. dxe5\n" +
						"Nxe4 18. Bxe7 Qxe7 19. exd6 Qf6 20. Nbd2 Nxd6 21. Nc4 Nxc4 22. Bxc4 Nb6\n" +
						"23. Ne5 Rae8 24. Bxf7+ Rxf7 25. Nxf7 Rxe1+ 26. Qxe1 Kxf7 27. Qe3 Qg5 28. Qxg5\n" +
						"hxg5 29. b3 Ke6 30. a3 Kd6 ");
		System.out.println(g);;
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
