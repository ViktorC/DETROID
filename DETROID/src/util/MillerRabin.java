package util;

import java.util.Random;

/**
 * Provides primality testing related functions based on the Miller-Rabin algorithm.
 * 
 * @author Viktor
 *
 */
public final class MillerRabin {
	
	private static final int DEFAULT_ACCURACY = 7;
	
	private static Random rand = new Random();
	
	private MillerRabin() {
		
	}
	/**
	 * Builds a random long within the specified range by adjoining two random integers;
	 * 
	 * @param upperBound Exclusive.
	 * @return
	 */
	private static long nextLong(long upperBound) {
		long hi, low;
		if (upperBound <= Integer.MAX_VALUE)
			return rand.nextInt((int)upperBound);
		upperBound = upperBound >>> 32;
		hi = rand.nextInt((int)upperBound) << 32;
		low = rand.nextInt();
		return hi | low;
	}
	/**
	 * Tests whether a number is probably prime or not using the Miller-Rabin algorithm.
	 * 
	 * @param n The number to be tested.
	 * @param k The accuracy of the primality test.
	 * @return
	 */
	public static boolean isPrime(long n, int k) {
		long d, s, a, x;
		if (n < 2)
			return false;
		if (n%2 == 0)
			return n == 2;
		if (n == 3)
			return true;
		s = 0;
		d = n - 1;
		while (d%2 == 0) {
			s++;
			d /= 2;
		}
		WitnessLoop: for (int i = 0; i < k; i++) {
			a = nextLong(n - 3) + 2;
			x = ((long)Math.pow(a, d))%n;
			if (x == 1 || x == n - 1)
				continue;
			for (int j = 1; j < s; j++) {
				x = (x*x)%n;
				if (x == 1)
					return false;
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
	public static boolean isPrime(long n) {
		return isPrime(n, DEFAULT_ACCURACY);
	}
	/**
	 * Finds and returns the smallest prime number that is greater than or equal to n.
	 * 
	 * @param n The number to be tested.
	 * @param k The accuracy of the primality test.
	 * @return
	 */
	public static long leastGEPrime(long n, int k) {
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
	public static long leastGEPrime(long n) {
		return leastGEPrime(n, DEFAULT_ACCURACY);
	}
	/**
	 * Finds and returns the greatest prime number that is lesser than or equal to n.
	 * 
	 * @param n The number to be tested.
	 * @param k The accuracy of the primality test.
	 * @return
	 */
	public static long greatestLEPrime(long n, int k) {
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
	public static long greatestLEPrime(long n) {
		return greatestLEPrime(n, DEFAULT_ACCURACY);
	}
	public static void main(String[] args) {
		System.out.println(isPrime(101));
	}
}
