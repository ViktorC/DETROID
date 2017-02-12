package net.viktorc.detroid.framework.uci;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * An implementation of the Universal Chess Interface protocol which serves as a controller for a UCI compatible chess engine
 * when communicating with a UCI compatible GUI.
 * 
 * @author Viktor
 *
 */
public final class UCI implements Observer, Runnable, Closeable {
	
	private final Scanner in;
	private final PrintStream out;
	private final UCIEngine engine;
	private final ExecutorService executor;
	
	/**
	 * Constructs an instance to handle the UCI protocol.
	 * 
	 * @param engine The chess engine to run in UCI mode.
	 * @param in The input stream.
	 * @param out The output stream.
	 */
	public UCI(UCIEngine engine, InputStream in, OutputStream out) {
		this.engine = engine;
		this.in = new Scanner(in);
		this.out = new PrintStream(out);
		executor = Executors.newSingleThreadExecutor();
	}
	@Override
	public synchronized void run() {
		boolean over = false;
		String input = "";
		String[] tokens;
		String header;
		String option;
		while (!in.nextLine().trim().equals("uci"));
		if (!this.engine.isInit()) {
			try {
				this.engine.init();
			} catch (Exception e) {
				out.println("Failed to initialize engine: " + e.getMessage());
			}
		}
		this.engine.getSearchInfo().addObserver(this);
		this.engine.getDebugInfo().addObserver(this);
		out.println("id name " + this.engine.getName());
		out.println("id author " + this.engine.getAuthor());
		for (Entry<Option<?>,Object> e : engine.getOptions().entrySet()) {
			Option<?> o = e.getKey();
			option = "option name " + o.getName() + " type ";
			if (o instanceof Option.CheckOption)
				option += "check default " + (boolean) o.getDefaultValue().get();
			else if (o instanceof Option.SpinOption)
				option += "spin default " + (Integer) o.getDefaultValue().get() + " min " + o.getMin().get() +
				" max " + o.getMax().get();
			else if (o instanceof Option.StringOption)
				option += "string default " + (String) o.getDefaultValue().get();
			else if (o instanceof Option.ComboOption) {
				option += "combo default " + (String) o.getDefaultValue().get() + " var";
				for (Object v : o.getAllowedValues().get())
					option += " " + v;
			} else if (o instanceof Option.ButtonOption)
				option += "button";
			out.println(option);
		}
		out.println("uciok");
		while (!over) {
			input = in.nextLine().trim();
			tokens = input.split("\\s+");
			header = tokens[0];
			switch (header) {
				case "debug": {
					this.engine.setDebugMode(tokens[1].equals("on"));
				} break;
				case "isready": {
					out.println("readyok");
				} break;
				case "setoption": {
					String value;
					for (Entry<Option<?>,Object> e : engine.getOptions().entrySet()) {
						Option<?> o = e.getKey();
						if (o.getName().equals(tokens[2])) {
							if (o instanceof Option.CheckOption)
								this.engine.setOption((Option.CheckOption) o, Boolean.parseBoolean(tokens[4]));
							else if (o instanceof Option.SpinOption)
								this.engine.setOption((Option.SpinOption) o, Integer.parseInt(tokens[4]));
							else if (o instanceof Option.StringOption) {
								value = "";
								for (int i = 4; i < tokens.length; i++)
									value += tokens[i] + " ";
								value = value.trim();
								this.engine.setOption((Option.StringOption) o, value.equals("null") ? null : value);
							} else if (o instanceof Option.ComboOption) {
								value = "";
								for (int i = 4; i < tokens.length; i++)
									value += tokens[i] + " ";
								value = value.trim();
								this.engine.setOption((Option.ComboOption) o, value.equals("null") ? null : value);
							} else if (o instanceof Option.ButtonOption)
								this.engine.setOption((Option.ButtonOption) o, null);
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
					this.engine.setPosition(pos);
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
					executor.submit(() -> {
						String resultString;
						SearchResults results = this.engine.search(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
						resultString = "bestmove " + results.getBestMove();
						if (results.getSuggestedPonderMove().isPresent())
							resultString += " ponder " + results.getSuggestedPonderMove().get();
						out.println(resultString);
					});
				} break;
				case "stop": {
					this.engine.stop();
				} break;
				case "ponderhit": {
					this.engine.ponderHit();
				} break;
				case "quit": {
					over = true;
				}
			}
		}
		this.engine.getSearchInfo().deleteObserver(this);
		this.engine.getDebugInfo().deleteObserver(this);
	}
	@Override
	public void update(Observable p1, Object p2)
	{
		if (p1 instanceof SearchInformation) {
			SearchInformation stats = (SearchInformation) p1;
			String info = "info depth " + stats.getDepth() + " time " + stats.getTime() + " nodes " + stats.getNodes() + " ";
			if (stats.getCurrentMove() != null) {
				info += "currmove " + stats.getCurrentMove() + " ";
				if (stats.getCurrentMoveNumber() != 0)
					info += "currmovenumber " + stats.getCurrentMoveNumber() + " ";
			}
			if (stats.getPvNumber() != 0)
				info += "multipv " + stats.getPvNumber() + " ";
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
			info += stats.getScore() + " nps " + (int) 1000*stats.getNodes()/Math.max(1, stats.getTime());
			out.println(info);
			out.println("info hashfull " + engine.getHashLoadPermill());
		}
		else if (p1 instanceof DebugInformation) {
			DebugInformation debugInfo = (DebugInformation) p1;
			String content = debugInfo.getContent();
			String[] lines = content.split("\\r?\\n");
			for (String s : lines)
				out.println("info string " + s);
		}
	}
	@Override
	public void close() throws IOException {
		engine.quit();
		executor.shutdown();
		in.close();
		out.close();
	}
	
}
