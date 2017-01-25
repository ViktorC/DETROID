package net.viktorc.detroid.util;

import java.util.Random;

/**
 * Provides primality testing related functions based on the Miller-Rabin algorithm.
 * 
 * @author Viktor
 *
 */
public final class MillerRabin {
	
	// The default number of times the witness loop is run.
	private static final int DEFAULT_ACCURACY = 7;
	
	private Random rand;
	
	public MillerRabin() {
		rand = new Random(System.currentTimeMillis());
	}
	/**
	 * Returns random long within the specified range by adjoining two random integers;
	 * 
	 * @param upperBound Exclusive.
	 * @return
	 */
	private long nextLong(long upperBound) {
		long r = rand.nextLong() & ((1L << 63) - 1); // Taking the absolute value of the random long.
		return r%upperBound;
	}
	/**
	 * Performs modular exponentiation using the right-to-left binary method.
	 * 
	 * @param base The base of the modular exponentiation.
	 * @param exp The exponent.
	 * @param mod The modulus;
	 * @return
	 */
	private static long modPow(long base, long exp, long mod) {
		long res;
		if (mod == 1)
			return 0;
		res = 1;
		base = base%mod;
		while (exp > 0) {
			if (exp%2 == 1)
				res = (res*base)%mod;
			exp = exp >>> 1;
			base = (base*base)%mod;
		}
		return res;
	}
	/**
	 * Tests whether a number is probably prime or not using the Miller-Rabin algorithm.
	 * 
	 * @param n The number to be tested.
	 * @param k The accuracy of the primality test.
	 * @return
	 */
	public boolean isPrime(long n, int k) {
		long d, s, a, x;
		if (n < 2)
			return false;
		if (n%2 == 0)
			return n == 2;
		if (n == 3)
			return true;
		s = 0;
		d = n - 1;
		// n - 1 = (2^d)*s
		while (d%2 == 0) {
			s++;
			d = d >>> 1;
		}
		WitnessLoop: for (int i = 0; i < k; i++) {
			// a: random integer between 2 and n - 1
			a = nextLong(n - 3) + 2;
			// x = (a^d)%n - The straightforward method would result in huge numbers for a^b that would overflow a long.
			x = modPow(a, d, n);
			if (x == 1 || x == n - 1)
				continue;
			// a^((2^r)*d) - If (a^d)%n is not 1 or -1, for n to be prime, there has to be an r, 1 < r < s, for which a^((2^r)*d)%n is -1
			for (int r = 1; r < s; r++) {
				x = (x*x)%n;
				// A residual of 1 that when squared resulted in neither -1 nor 1 proves n is composite as it has a nontrivial sqrt of 1 mod.
				if (x == 1)
					break;
				if (x == n - 1)
					continue WitnessLoop;
			}
			return false;
		}
		return true;
	}
	/**
	 * Tests whether a number is probably prime or not using the Miller-Rabin algorithm.
	 * 
	 * @param n The number to be tested.
	 * @return
	 */
	public boolean isPrime(long n) {
		return isPrime(n, DEFAULT_ACCURACY);
	}
	/**
	 * Finds and returns the smallest prime number that is greater than or equal to n.
	 * 
	 * @param n The number to be tested.
	 * @param k The accuracy of the primality test.
	 * @return
	 */
	public long leastGEPrime(long n, int k) {
		if (n < 0)
			throw new IllegalArgumentException("n has to be positive.");
		if (n%2 == 0 && n != 2)
			n++;
		while (!isPrime(n, k))
			n += 2;
		return n;
	}
	/**
	 * Finds and returns the smallest prime number that is greater than or equal to n.
	 * 
	 * @param n The number to be tested.
	 * @return
	 */
	public long leastGEPrime(long n) {
		return leastGEPrime(n, DEFAULT_ACCURACY);
	}
	/**
	 * Finds and returns the greatest prime number that is lesser than or equal to n.
	 * 
	 * @param n The number to be tested.
	 * @param k The accuracy of the primality test.
	 * @return
	 */
	public long greatestLEPrime(long n, int k) {
		if (n < 2)
			throw new IllegalArgumentException("n has to be >= 2.");
		if (n%2 == 0 && n != 2)
			n--;
		while (!isPrime(n, k) && n >= 0)
			n -= 2;
		return n;
	}
	/**
	 * Finds and returns the greatest prime number that is lesser than or equal to n.
	 * 
	 * @param n The number to be tested.
	 * @return
	 */
	public long greatestLEPrime(long n) {
		return greatestLEPrime(n, DEFAULT_ACCURACY);
	}
	
}
