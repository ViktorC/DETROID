package util;

public class GrayCode {

	private long refBinNum;
	
	public GrayCode(long n) {
		if (n < 0)
			throw new IllegalArgumentException("n has to be unsigned (~positive).");
		refBinNum = n^(n >>> 1);
	}
	public long getGray() {
		return refBinNum;
	}
	public long getDecimal() {
		long n;
		n = refBinNum^(refBinNum >> 32);
		n = n^(n >> 16);
		n = n^(n >> 8);
		n = n^(n >> 4);
	    n = n^(n >> 2);
	    n = n^(n >> 1);
	    return n;
	}
}
