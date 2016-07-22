package uci;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;

/**
 * An implementation of the Universal Chess Interface protocol which serves as a controller for a UCI compatible chess engine
 * when communicating with a UCI compatible GUI.
 * 
 * @author Viktor
 *
 */
public final class UCI implements Observer, Closeable {
	
	private static UCI INSTANCE = new UCI(System.in, System.out);
	
	private Scanner in;
	private PrintStream out;
	private Engine engine;
	
	public static UCI getInstance() {
		return INSTANCE;
	}
	private UCI(InputStream in, OutputStream out) {
		this.in = new Scanner(in);
		this.out = new PrintStream(out);
	}
	/**
	 * Runs the UCI protocol on the standard input and output streams controlling the specified UCI compatible engine.
	 * 
	 * @param engine
	 */
	@SuppressWarnings("unchecked")
	public synchronized void run(Engine engine) {
		ExecutorService exec = Executors.newFixedThreadPool(1);
		boolean over = false;
		String input = "";
		String[] tokens;
		String header;
		String option;
		Class<?> c;
		this.engine = engine;
		this.engine.init();
		while (!in.nextLine().trim().equals("uci"));
		out.println("id name " + this.engine.getName());
		out.println("id author " + this.engine.getAuthor());
		for (Setting<?> s : engine.getOptions()) {
			option = "option " + s.getName() + " type ";
			c = s.getClass();
			if (c == Setting.BooleanSetting.class)
				option += "check default " + (boolean)s.getDefaultValue();
			else if (c == Setting.IntegerSetting.class)
				option += "spin default " + (Integer)s.getDefaultValue() + " min " + s.getMin() + " max " + s.getMax();
			else if (c == Setting.StringSetting.class)
				option += "string default " + (String)s.getDefaultValue();
			else if (c == Setting.StringComboSetting.class) {
				option += "combo default " + (String)s.getDefaultValue() + " var";
				for (Object v : s.getAllowedValues())
					option += " " + v;
			}
			out.println(option);
		}
		out.println("uciok");
		while (!over) {
			input = in.nextLine().trim();
			tokens = input.split("\\s+");
			header = tokens[0];
			switch (header) {
				case "debug": {
					if (tokens[1].equals("on"))
						this.engine.getInfo().addObserver(this);
					else
						this.engine.getInfo().deleteObserver(this);
				} break;
				case "isready": {
					out.println("readyok");
				} break;
				case "setoption": {
					for (Setting<?> e : engine.getOptions()) {
						if (e.getName().equals(tokens[1])) {
							c = e.getDefaultValue().getClass();
							if (c == Boolean.class)
								this.engine.setOption((Setting<Boolean>)e, Boolean.parseBoolean(tokens[2]));
							else if (c == String.class)
								this.engine.setOption((Setting<String>)e, tokens[2]);
							else if (c == Integer.class)
								this.engine.setOption((Setting<Integer>)e, Integer.parseInt(tokens[2]));
							break;
						}
					}
				} break;
				case "register": {
					out.println("register " + engine.getAuthor());
				} break;
				case "ucinewgame": {
					this.engine.newGame();
				} break;
				case "position": {
					String pos = tokens[1];
					int i = 2;
					for (; i < tokens.length && !tokens[i].equals("moves"); i++) {
						pos += " " + tokens[i];
					}
					this.engine.position(pos);
					for (; i < tokens.length; i++)
						this.engine.play(tokens[i]);
				} break;
				case "go": {
					Set<String> searchMoves = null;
					Boolean ponder = null;
					Long whiteTime = null, blackTime = null;
					Long whiteIncrement = null, blackIncrement = null;
					Integer movesToGo = null;
					Integer depth = null;
					Long nodes = null;
					Integer mateDistance = null;
					Long searchTime = null;
					Boolean infinite = null;
					boolean moves = false;
					searchMoves = null;
					for (int i = 1; i < tokens.length; i++) {
						switch (tokens[i]) {
							case "searchmoves": {
								searchMoves = new HashSet<>();
								moves = true;
							} break;
							case "ponder": {
								ponder = true;
								moves = false;
							} break;
							case "wtime": {
								whiteTime = Long.parseLong(tokens[++i]);
								moves = false;
							} break;
							case "btime": {
								blackTime = Long.parseLong(tokens[++i]);
								moves = false;
							} break;
							case "winc": {
								whiteIncrement = Long.parseLong(tokens[++i]);
								moves = false;
							} break;
							case "binc": {
								blackIncrement = Long.parseLong(tokens[++i]);
								moves = false;
							} break;
							case "movestogo": {
								movesToGo = Integer.parseInt(tokens[++i]);
								moves = false;
							} break;
							case "depth": {
								depth = Integer.parseInt(tokens[++i]);
								moves = false;
							} break;
							case "nodes": {
								nodes = Long.parseLong(tokens[++i]);
								moves = false;
							} break;
							case "mate": {
								mateDistance = Integer.parseInt(tokens[++i]);
								moves = false;
							} break;
							case "movetime": {
								searchTime = Long.parseLong(tokens[++i]);
								moves = false;
							} break;
							case "infinite": {
								infinite = true;
								moves = false;
							} break;
							default: {
								if (moves)
									searchMoves.add(tokens[i]);
							}
						}
					}
					Set<String> p0 = searchMoves;
					Boolean p1 = ponder;
					Long p2 = whiteTime;
					Long p3 = blackTime;
					Long p4 = whiteIncrement;
					Long p5 = blackIncrement;
					Integer p6 = movesToGo;
					Integer p7 = depth;
					Long p8 = nodes;
					Integer p9 = mateDistance;
					Long p10 = searchTime;
					Boolean p11 = infinite;
					exec.submit(() -> {
						out.println(this.engine.search(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11));
					});
				} break;
				case "stop": {
					this.engine.stop();
				} break;
				case "ponderhit": {
					this.engine.ponderhit();
				} break;
				case "quit": {
					over = true;
				}
			}
		}
		this.engine.getInfo().deleteObserver(this);
		this.engine = null;
		exec.shutdown();
	}
	@Override
	public void update(Observable p1, Object p2)
	{
		SearchInfo stats = (SearchInfo)p1;
		String info = "info depth " + stats.getDepth() + " time " + stats.getTime() + " nodes " + stats.getNodes() + " pv ";
		for (String s : stats.getPv())
			info += s + " ";
		info += "score ";
		switch (stats.getScoreType()) {
		case EXACT:
			info += "cp ";
			break;
		case MATE:
			info += "mate ";
			break;
		case LOWER_BOUND:
			info += "lowerbound ";
			break;
		case UPPER_BOUND:
			info += "upperbound ";
			break;
		}
		info += stats.getScore() + " nps " + (int)1000*stats.getNodes()/Math.max(1, stats.getTime());
		out.println(info);
		out.println("info hashfull " + engine.getHashLoadPermill());
	}
	@Override
	public void close() throws IOException {
		in.close();
		out.close();
	}
}
