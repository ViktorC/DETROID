package net.viktorc.detroid.framework.tuning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An EPD entry from the Strategic Test Suite in which moves are scored and a command operation holds the 
 * accepted moves and their respective scores.
 * 
 * @author Viktor
 *
 */
public class STSEPD extends EPD {

	private static final String MOVE_SCORES_OP_CODE = "c0 ";
	
	private Map<String, Integer> moveScores;
	
	/**
	 * Constructs an instance based on the specified EPD record.
	 * 
	 * @param epd The EPD record.
	 */
	public STSEPD(String epd) {
		super(epd);
		moveScores = new HashMap<>();
		for (String op : ops) {
			op = op.trim();
			if (MOVE_SCORES_OP_CODE.startsWith(op)) {
				op = op.substring(BEST_MOVE_OP_CODE.length(), op.length()).trim();
				op = op.replace("\"", "");
				String[] moveScorePairs = op.split(",");
				for (String moveScorePair : moveScorePairs) {
					moveScorePair = moveScorePair.trim();
					int delimiterIndex = moveScorePair.lastIndexOf('=');
					String move = moveScorePair.substring(0, delimiterIndex);
					Integer score = Integer.parseInt(moveScorePair.substring(delimiterIndex + 1));
					moveScores.put(move, score);
				}
			}
		}
	}
	/**
	 * Returns a set of move and score pairs containing the moves that are worth a score in Standard Algebraic Notation 
	 * and their respective values ranging from 1 to 10.
	 * 
	 * @return The set of moves worth a score paired with the score.
	 */
	public Set<Entry<String, Integer>> getMoveScorePairs() {
		return new HashSet<>(moveScores.entrySet());
	}

}
