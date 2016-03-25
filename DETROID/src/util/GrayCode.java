package util;

public class GrayCode {

	private long grayValue;
	private boolean isHammingWeightOdd;
	
	public GrayCode(long n) {
		grayValue = n^(n >>> 1);
		isHammingWeightOdd = BitOperations.getHammingWeight(n)%2 == 1;
	}
	public long getGrayValue() {
		return grayValue;
	}
	public long getDecimalValue() {
		long n;
		n = grayValue^(grayValue >> 32);
		n = n^(n >> 16);
		n = n^(n >> 8);
		n = n^(n >> 4);
	    n = n^(n >> 2);
	    n = n^(n >> 1);
	    return n;
	}
}
