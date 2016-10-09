package tuning;

import java.util.Random;
import java.util.logging.Logger;

import uci.SearchResults;
import uci.UCIEngine;
import ui_base.ControllerEngine;
import ui_base.GameState;
import util.SingularTimer;

/**
 * A class for pitting two UCI compatible engines against each other, supervised by a controller engine.
 * 
 * @see uci.UCIEngine
 * @see ui_base.ControllerEngine
 * @author Viktor
 *
 */
public class Arena implements AutoCloseable {

	/**
	 * The arena event's impromptu name.
	 */
	public final static String EVENT = "Viktor Csomor's Chess Arena";
	
	private final static long TIMER_RESOLUTION = 15;
	private final static long TIMER_ADDITIONAL_DELAY = 10;
	
	private ControllerEngine controller;
	private SingularTimer timer;
	private Random rand;
	private boolean doLog;
	private Logger logger;
	private long id;
	
	/**
	 * Constructs an arena controlled by the specified engine. A logger can be provided that will be used to log game
	 * results.
	 * 
	 * @param controller The engine to control the match.
	 * @param logger The logger to log the game results.
	 */
	public Arena(ControllerEngine controller, Logger logger) {
		this.controller = controller;
		if (!this.controller.isInit())
			this.controller.init();
		timer = new SingularTimer(null, Long.MAX_VALUE, TIMER_RESOLUTION);
		doLog = logger != null;
		if (doLog)
			this.logger = logger;
		rand = new Random(System.nanoTime());
		id = rand.nextLong();
	}
	/**
	 * Constructs an arena controlled by the specified engine.
	 * 
	 * @param controller The engine to control the match.
	 */
	public Arena(ControllerEngine controller) {
		this(controller, null);
	}
	/**
	 * Returns the Arena instance's id number.
	 * 
	 * @return
	 */
	public long getId() {
		return id;
	}
	/**
	 * Pits the two engines against each other playing the specified number of games with the number of milliseconds per game
	 * allotted for each engine to make their moves. The engines play alternating their colours after each game so it is
	 * recommended to specify an even number for the number of games to play.
	 * 
	 * @param engine1 Contender number one.
	 * @param engine2 Contender number two.
	 * @param games The number of games to play. Should be an even number.
	 * @param timePerGame The number of milliseconds each engine will have to make all their moves during the course of each game.
	 * If it is less than 500, it will default to 250.
	 * @return The results of the match.
	 */
	public synchronized MatchResult match(UCIEngine engine1, UCIEngine engine2, int games, long timePerGame) {
		int engine1Wins = 0;
		int engine2Wins = 0;
		int draws = 0;
		games = Math.max(0, games);
		timePerGame = Math.max(250, timePerGame);
		if (!engine1.isInit())
			engine1.init();
		if (!engine2.isInit())
			engine2.init();
		if (doLog) logger.info("--------------------------------------MATCH STARTED--------------------------------------\n" +
				"Arena: " + id + "\n" +
				"Engine1: " + engine1.getName() + " - Engine2: " + engine2.getName() + "\n" +
				"Games: " + games + " TC: " + timePerGame + "ms per Game\n\n");
		timer.start();
		timer.pause();
		boolean engine1White = rand.nextBoolean();
		Games: for (int i = 0; i < games; i++, engine1White = !engine1White) {
			String move = null;
			long engine1Time = timePerGame;
			long engine2Time = timePerGame;
			boolean engine1Turn = engine1White;
			engine1.newGame();
			engine2.newGame();
			controller.newGame();
			engine1.position("startpos");
			engine2.position("startpos");
			controller.position("startpos");
			if (doLog) {
				controller.setPlayers(engine1White ? "Engine1" : "Engine2", engine1White ? "Engine2" : "Engine1");
				controller.setEvent(EVENT);
				controller.setSite("?");
			}
			while (controller.getGameState() == GameState.IN_PROGRESS) {
				if (!timer.isAlive()) {
					timer.setDelay(Long.MAX_VALUE);
					timer.start();
					timer.pause();
				}
				long start = System.nanoTime();
				move = null;
				if (engine1Turn) {
					try {
						timer.setCallBack(() -> engine1.stop());
						timer.setDelay(engine1Time + TIMER_ADDITIONAL_DELAY);
						timer.goOn();
						SearchResults searchRes = engine1.search(null, false, engine1White ? engine1Time : engine2Time, engine1White ?
								engine2Time : engine1Time, null, null, null, null, null, null, null, null);
						timer.pause();
						move = searchRes.getBestMove();
						engine1Time -= ((System.nanoTime() - start)/1000000);
						if (engine1Time <= 0 || !controller.play(move)) {
							engine2Wins++;
							if (doLog) logger.info("Arena: " + id + "\n" + "Engine2 WINS: " + (engine1Time <= 0 ? "Engine1 lost on time." :
									"Engine1 returned an illegal move: " + move + ".") + "\n" + controller.toPGN() + "\n" +
									"STANDINGS: " + engine1Wins + " - " + draws + " - " + engine2Wins + "\n\n");
							continue Games;
						}
					} catch (Exception e) {
						timer.pause();
						engine2Wins++;
						if (doLog) {
							logger.info("Arena: " + id + "\n" + "Engine2 WINS: Engine1 lost due to error.\n" +
									"STANDINGS: " + engine1Wins + " - " + draws + " - " + engine2Wins + "\n\n");
						}
						continue Games;
					}
				} else {
					try {
						timer.setCallBack(() -> engine2.stop());
						timer.setDelay(engine2Time + TIMER_ADDITIONAL_DELAY);
						timer.goOn();
						SearchResults searchRes = engine2.search(null, false, engine1White ? engine1Time : engine2Time, engine1White ?
								engine2Time : engine1Time, null, null, null, null, null, null, null, null);
						timer.pause();
						move = searchRes.getBestMove();
						engine2Time -= ((System.nanoTime() - start)/1000000);
						if (engine2Time <= 0 || !controller.play(move)) {
							engine1Wins++;
							if (doLog) logger.info("Arena: " + id + "\n" + "Engine1 WINS: " + (engine2Time <= 0 ? "Engine2 lost on time." :
									"Engine2 returned an illegal move: " + move + ".") + "\n" + controller.toPGN() + "\n" +
									"STANDINGS: " + engine1Wins + " - " + draws + " - " + engine2Wins + "\n\n");
							continue Games;
						}
					} catch (Exception e) {
						timer.pause();
						engine1Wins++;
						if (doLog) {
							logger.info("Arena: " + id + "\n" + "Engine1 WINS: Engine2 lost due to error.\n" +
									"STANDINGS: " + engine1Wins + " - " + draws + " - " + engine2Wins + "\n\n");
						}
						continue Games;
					}
				}
				engine1.play(move);
				engine2.play(move);
				engine1Turn = !engine1Turn;
			}
			GameState state = controller.getGameState();
			if (state == GameState.WHITE_MATES) {
				if (engine1White)
					engine1Wins++;
				else
					engine2Wins++;
				if (doLog) logger.info("Arena: " + id + "\n" + (engine1White ? "Engine1 WINS: " : "Engine2 WINS: ") + "Check mate.\n" +
						controller.toPGN() + "\n" + "STANDINGS: " + engine1Wins + " - " + draws + " - " + engine2Wins + "\n\n");
			}
			else if (state == GameState.BLACK_MATES) {
				if (engine1White)
					engine2Wins++;
				else
					engine1Wins++;
				if (doLog) logger.info("Arena: " + id + "\n" + (engine1White ? "Engine2 WINS: " : "Engine1 WINS: ") + "Check mate.\n" +
						controller.toPGN() + "\n" + "STANDINGS: " + engine1Wins + " - "  + draws + " - " + engine2Wins + "\n\n");
			}
			else {
				draws++;
				if (doLog) {
					String gameRes = "Arena: " + id + "\n" + "DRAW: ";
					String pgnAndStandings = controller.toPGN() + "\n" + "STANDINGS: " + engine1Wins + " - " +
							draws + " - " + engine2Wins + "\n\n";
					String reason = "";
					switch (state) {
						case STALE_MATE:
							reason = "Stale mate.\n";
							break;
						case DRAW_BY_INSUFFICIENT_MATERIAL:
							reason = "Insufficient material.\n";
							break;
						case DRAW_BY_3_FOLD_REPETITION:
							reason = "Three fold repetition.\n";
							break;
						case DRAW_BY_50_MOVE_RULE:
							reason = "Fifty move rule.\n";
							break;
						default:
							break;
					}
					logger.info(gameRes + reason + pgnAndStandings);
				}
			}
		}
		timer.cancel();
		return new MatchResult(engine1Wins, engine2Wins, draws);
	}
	@Override
	public void close() {
		timer.close();
		controller.quit();
	}

}
