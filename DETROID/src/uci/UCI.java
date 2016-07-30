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
	public synchronized void run(Engine engine) {
		ExecutorService exec = Executors.newFixedThreadPool(1);
		boolean over = false;
		String input = "";
		String[] tokens;
		String header;
		String option;
		this.engine = engine;
		while (!in.nextLine().trim().equals("uci"));
		this.engine.init();
		this.engine.getSearchInfo().addObserver(this);
		this.engine.getDebugInfo().addObserver(this);
		out.println("id name " + this.engine.getName());
		out.println("id author " + this.engine.getAuthor());
		for (Option<?> o : engine.getOptions()) {
			option = "option name " + o.getName() + " type ";
			if (o instanceof Option.CheckOption)
				option += "check default " + (boolean)o.getDefaultValue();
			else if (o instanceof Option.SpinOption)
				option += "spin default " + (Integer)o.getDefaultValue() + " min " + o.getMin() + " max " + o.getMax();
			else if (o instanceof Option.StringOption)
				option += "string default " + (String)o.getDefaultValue();
			else if (o instanceof Option.ComboOption) {
				option += "combo default " + (String)o.getDefaultValue() + " var";
				for (Object v : o.getAllowedValues())
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
					this.engine.debug(tokens[1].equals("on"));
				} break;
				case "isready": {
					out.println("readyok");
				} break;
				case "setoption": {
					for (Option<?> e : engine.getOptions()) {
						if (e.getName().equals(tokens[2])) {
							if (e instanceof Option.CheckOption)
								this.engine.setOption((Option.CheckOption)e, Boolean.parseBoolean(tokens[4]));
							else if (e instanceof Option.SpinOption)
								this.engine.setOption((Option.SpinOption)e, Integer.parseInt(tokens[4]));
							else if (e instanceof Option.StringOption)
								this.engine.setOption((Option.StringOption)e, tokens[4]);
							else if (e instanceof Option.ComboOption)
								this.engine.setOption((Option.ComboOption)e, tokens[4]);
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
					for (; i < tokens.length && !tokens[i].equals("moves"); i++)
						pos += " " + tokens[i];
					i++;
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
						String resultString;
						SearchResults results = this.engine.search(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
						resultString = "bestmove " + results.getBestMove();
						if (results.getSuggestedPonderMove() != null)
							resultString += " ponder " + results.getSuggestedPonderMove();
						out.println(resultString);
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
		this.engine.quit();
		this.engine = null;
		exec.shutdown();
	}
	@Override
	public void update(Observable p1, Object p2)
	{
		if (p1 instanceof SearchInfo) {
			SearchInfo stats = (SearchInfo)p1;
			String info = "info depth " + stats.getDepth() + " time " + stats.getTime() + " nodes " + stats.getNodes() + " ";
			if (stats.getCurrentMove() != null) {
				info += "currmove " + stats.getCurrentMove() + " ";
				if (stats.getCurrentMoveNumber() != 0)
					info += "currmovenumber " + stats.getCurrentMoveNumber() + " ";
			}
			String[] pV = stats.getPv();
			if (pV != null && pV.length > 0) {
				info += "pv ";
				for (String s : pV)
					info += s + " ";
			}
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
		else if (p1 instanceof DebugInfo) {
			DebugInfo debugInfo = (DebugInfo)p1;
			String content = debugInfo.getContent();
			String[] lines = content.split("\\r?\\n");
			for (String s : lines)
				out.println("info string " + s);
		}
	}
	@Override
	public void close() throws IOException {
		in.close();
		out.close();
	}
}
