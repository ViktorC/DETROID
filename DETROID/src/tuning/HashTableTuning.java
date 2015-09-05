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
		epsilNmaxL_Tuning(0.4F, 2.2F, 0.1F, 1, 11, 1);
	}
	/**Returns the best value for the constant parameter EPSILON, disregarding everything else.
	 * 
	 * @param elipLower
	 * @param elipUpper
	 * @param elipIncrement
	 * @return
	 */
	public static float epsil_Tuning(float elipLower, float elipUpper, float elipIncrement) {
		long bestVal = Long.MAX_VALUE, val;
		float bestPar = 0;
		long start = System.currentTimeMillis();
		for (float epsil = elipLower; epsil < elipUpper; epsil += elipIncrement) {
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
	 * @param elipLower
	 * @param elipUpper
	 * @param elipIncrement
	 * @param maxLlower
	 * @param maxLupper
	 * @param maxLincrement
	 * @return
	 */
	public static String[] epsilNmaxL_Tuning(float elipLower, float elipUpper, float elipIncrement, int maxLlower, int maxLupper, int maxLincrement) {
		String[] par;
		long bestVal = Long.MAX_VALUE, val;
		String[] bestPar = null;
		long start = System.currentTimeMillis();
		for (float epsil = elipLower; epsil < elipUpper; epsil += elipIncrement) {
			for (int maxL = maxLlower; maxL < maxLupper; maxL += maxLincrement) {
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
