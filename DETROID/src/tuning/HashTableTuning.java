package tuning;

import util.*;

/**A class that provides simple methods for tuning the hash table's constant parameters by feeding value sets defined by bounds and increment measures
 * to the methods.
 * 
 * @author Viktor
 *
 */
public class HashTableTuning {

	public static void main(String[] args) {
		epsilon_Tuning(1.1F, 2.63F, 0.1F);
	}
	/**Returns the best value for the constant parameter EPSILON, disregarding everything else.
	 * 
	 * @param epsilLower
	 * @param epsilUpper
	 * @param epsilIncrement
	 * @return
	 */
	public static float epsilon_Tuning(float epsilLower, float epsilUpper, float epsilIncrement) {
		double val, bestVal = Long.MAX_VALUE;
		float bestPar = 0;
		long start = System.currentTimeMillis();
		for (float epsil = epsilLower; epsil <= epsilUpper; epsil += epsilIncrement) {
			if ((val = HashTable.tune("" + epsil)) < bestVal) {
				bestVal = val;
				bestPar = epsil;
			}
		}
		System.out.println("BEST EPSILON PARMETER = " + bestPar + "\ntook " + (System.currentTimeMillis() - start) + "ms");
		return bestPar;
	}
	/**Returns the best value pair for the constant parameter EPSILON and MAX_LOOP.
	 * 
	 * @param epsilLower
	 * @param epsilUpper
	 * @param epsilIncrement
	 * @param maxLlower
	 * @param maxLupper
	 * @param maxLincrement
	 * @return
	 */
	public static String[] epsilonAnd1PMLF_Tuning(float epsilLower, float epsilUpper, float epsilIncrement, int maxLlower, int maxLupper, int maxLincrement) {
		String[] par;
		double val, bestVal = Long.MAX_VALUE;
		String[] bestPar = null;
		long start = System.currentTimeMillis();
		for (float epsil = epsilLower; epsil <= epsilUpper; epsil += epsilIncrement) {
			for (int maxL = maxLlower; maxL <= maxLupper; maxL += maxLincrement) {
				par = new String[]{"" + epsil, "" + maxL};
				if ((val = HashTable.tune(par)) < bestVal){
					bestVal = val;
					bestPar = par;
				}
			}
		}
		System.out.println("BEST EPSILON PARMETER = " + bestPar[0] + "; BEST MAX_L PARMETER = " + bestPar[1] + "\ntook " + (System.currentTimeMillis() - start) + "ms");
		return bestPar;
	}
}
