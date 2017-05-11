package net.viktorc.detroid.framework.uci;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import net.viktorc.detroid.framework.uci.Option.*;

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
		if (!engine.isInit()) {
			try {
				engine.init();
			} catch (Exception e) {
				out.println("Failed to initialize engine: " + e.getMessage());
			}
		}
		engine.getSearchInfo().addObserver(this);
		engine.getDebugInfo().addObserver(this);
		out.println("id name " + engine.getName());
		out.println("id author " + engine.getAuthor());
		for (Entry<Option<?>,Object> e : engine.getOptions().entrySet()) {
			Option<?> o = e.getKey();
			option = "option name " + o.getName() + " type ";
			if (o instanceof CheckOption)
				option += "check default " + ((CheckOption) o).getDefaultValue().get();
			else if (o instanceof SpinOption)
				option += "spin default " + ((SpinOption) o).getDefaultValue().get() + " min " +
						o.getMin().get() + " max " + o.getMax().get();
			else if (o instanceof StringOption)
				option += "string default " + ((StringOption) o).getDefaultValue().get();
			else if (o instanceof ComboOption) {
				option += "combo default " + ((ComboOption) o).getDefaultValue().get() + " var";
				for (Object v : o.getAllowedValues().get())
					option += " " + v;
			} else if (o instanceof ButtonOption)
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
							if (o instanceof CheckOption)
								this.engine.setOption((CheckOption) o, Boolean.parseBoolean(tokens[4]));
							else if (o instanceof SpinOption)
								this.engine.setOption((SpinOption) o, Integer.parseInt(tokens[4]));
							else if (o instanceof StringOption) {
								value = "";
								for (int i = 4; i < tokens.length; i++)
									value += tokens[i] + " ";
								value = value.trim();
								this.engine.setOption((StringOption) o, value.equals("null") ? null : value);
							} else if (o instanceof ComboOption) {
								value = "";
								for (int i = 4; i < tokens.length; i++)
									value += tokens[i] + " ";
								value = value.trim();
								this.engine.setOption((ComboOption) o, value.equals("null") ? null : value);
							} else if (o instanceof ButtonOption)
								this.engine.setOption((ButtonOption) o, null);
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
					int i = 1;
					/* In case the position definition looks e.g. like this: 
					 * position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 */
					if ("fen".equals(tokens[i]))
						i++;
					String pos = tokens[i++];
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
						SearchResults results = engine.search(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
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
			String info = "info depth " + stats.getDepth() + " seldepth " + stats.getSelectiveDepth() + " time " +
					stats.getTime() + " nodes " + stats.getNodes() + " ";
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
		engine.close();
		executor.shutdown();
		in.close();
		out.close();
	}
	
}
