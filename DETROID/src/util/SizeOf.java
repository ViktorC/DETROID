package util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple enumeration type to help estimate the memory overhead of objects on the JVM heap.
 * 
 * @author Viktor
 *
 */
public enum SizeOf {

	MARK_WORD(getJVMMarkWordSize()),
	OBJECT_POINTER(getJVMOOPSize()),
	BYTE(1),
	BOOLEAN(1),
	SHORT(2),
	CHAR(2),
	INT(4),
	FLOAT(4),
	LONG(8),
	DOUBLE(8);

	/**
	 * The number of bytes occupied in memory. 
	 */
	public final byte numOfBytes;

	private SizeOf(int numOfBytes) {
		this.numOfBytes = (byte)numOfBytes;
	}
	/**
	 * Determines the space the mark word in an object header takes up in memory depending on the JVM.
	 * 
	 * @return
	 */
	private static int getJVMMarkWordSize() {
		return System.getProperty("sun.arch.data.model").equals("32") ? 4 : 8;
	}
	/**
	 * Determines the space object pointers take up in memory depending on the JVM.
	 * 
	 * @return
	 */
	private static int getJVMOOPSize() {
		RuntimeMXBean runtimeMX;
		boolean compressed, below32g;
		Pattern pattern;
		Matcher matcher;
		String xmxValue;
		int xmxValueNum;
		if (System.getProperty("sun.arch.data.model").equals("32"))
			return 4;
		else {
			compressed = System.getProperty("java.version").compareTo("1.6.0_23") >= 0;
			below32g = true;
			runtimeMX = ManagementFactory.getRuntimeMXBean();
			for (String s : runtimeMX.getInputArguments()) {
				if (s != null) {
					if (s.contains("+UseCompressedOops"))
						compressed = true;
					else if (s.contains("-UseCompressedOops"))
						compressed = false;
					else if (s.contains("Xmx")) {
						s = s.toLowerCase();
						pattern = Pattern.compile("[0-9]+[gmk]{1}");
						matcher = pattern.matcher(s);
						if (matcher.find()) {
							xmxValue = matcher.group();
							xmxValueNum = Integer.parseInt(xmxValue.substring(0, xmxValue.length() -1));
							switch (xmxValue.charAt(xmxValue.length() - 1)) {
								case 'g':
									below32g = xmxValueNum < 32;
									break;
								case 'm':
									below32g = xmxValueNum < (32 << 10);
									break;
								case 'k':
									below32g = xmxValueNum < (32 << 20);
									break;
							}
						}
					}
				}
			}
			return compressed && below32g ? 4 : 8;
		}
	}
}
