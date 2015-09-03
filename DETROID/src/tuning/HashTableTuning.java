package tuning;

import util.*;

public class HashTableTuning {

	public static void main(String[] args) {
		epsil_Tuning(0.2F, 0.5F, 0.1F);
	}
	public static float epsil_Tuning(float elipLower, float elipUpper, float elipIncrement) {
		long bestVal = Long.MAX_VALUE;
		float bestPar = 0;
		for (float epsil = elipLower; epsil < elipUpper; epsil += elipIncrement) {
			if (HashTable.tune("" + epsil) < bestVal)
				bestPar = epsil;
		}
		System.out.println("BEST EPSILON PARMETER = " + bestPar);
		return bestPar;
	}
	public static String[] epsilNmaxL_Tuning(float elipLower, float elipUpper, float elipIncrement, int maxLlower, int maxLupper, int maxLincrement) {
		String[] par;
		long bestVal = Long.MAX_VALUE;
		String[] bestPar = null;
		for (float epsil = elipLower; epsil < elipUpper; epsil += elipIncrement) {
			for (int maxL = maxLlower; maxL < maxLupper; maxL += maxLincrement) {
				par = new String[]{"" + epsil, "" + maxL};
				if (HashTable.tune(par) < bestVal)
					bestPar = par;
			}
		}
		System.out.println("BEST EPSILON PARMETER = " + bestPar[0] + "; BEST MAX_L PARMETER = " + bestPar[1]);
		return bestPar;
	}
}
