package util;

/**A functional interface for tuning. It defines the method {@link #tune(String[]) tune} that should assign the input parameters to the variables to be
 * tuned and return some kind of quantified score of the parameters' performance.
 * 
 * @author Viktor
 *
 */
public interface Tunable {
	
	/**Takes an arbitrary number of String arguments which it assigns to the respective variables to be tuned then evaluates the performance and returns
	 * it in a quantified form as a long.
	 * 
	 * @param args
	 * @return
	 */
	static long tune(String... args) {
		return -1;
	}
	
}
