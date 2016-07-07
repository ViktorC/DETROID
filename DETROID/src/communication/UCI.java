package communication;

import util.KeyValuePair;

/**
 * An interface supporting the UCI protocol as specified by Stefan-Meyer Kahlen.
 * 
 * @author Viktor
 *
 */
public interface UCI {

	String uci();
	String id();
	void debug(boolean on);
	String isReady();
	KeyValuePair<String, String>[] options();
	void setOption(KeyValuePair<String, String> option);
	String register();
	void uciNewGame();
	void position(String fen);
	String go(KeyValuePair<String, String>[] params);
	void stop();
	void ponderHit();
	void quit();
}
