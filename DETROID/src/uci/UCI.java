package uci;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * An implementation of the Universal Chess Interface protocol which serves as a controller for a UCI compatible chess engine
 * when communicating with a UCI compatible GUI.
 * 
 * @author Viktor
 *
 */
public final class UCI implements Observer {
	
	private static UCI INSTANCE = new UCI(System.in, System.out);
	
	private Scanner in;
	private PrintWriter out;
	private Engine engine;
	
	public UCI getInstance() {
		return INSTANCE;
	}
	private UCI(InputStream in, OutputStream out) {
		this.in = new Scanner(in);
		this.out = new PrintWriter(out);
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
		while (!in.nextLine().trim().equals("uci"));
		out.println("id name " + this.engine.getName());
		out.println("id author " + this.engine.getAuthor());
		for (Setting<?> s : engine.getOptions()) {
			option = "option " + s.getName() + " type ";
			c = s.getDefaultValue().getClass();
			if (c == Boolean.class)
				option += "check default " + (boolean)s.getDefaultValue();
			else if (c == String.class) {
				if (s.getAllowedValues() != null) {
					option += "combo default " + (String)s.getDefaultValue() + " var";
					for (Object v : s.getAllowedValues())
						option += " " + v;
				}
				else
					option += "string default " + (String)s.getDefaultValue();
			}
			else if (c == Integer.class)
				option += "spin default " + (Integer)s.getDefaultValue() + " min " + s.getMin() + " max " + s.getMax();
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
					this.engine.position(tokens[1]);
					for (int i = 3; i < tokens.length; i++)
						this.engine.play(tokens[i]);
				} break;
				case "go": {
					
				} break;
				case "stop": {
					this.engine.stop();
				} break;
				case "ponderhit": {
					
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
			info = s + " ";
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
		info += stats.getScore();
		out.println(info);
		out.println("info hashfull " + (int)(1000*engine.getHashLoad()));
	}
}
