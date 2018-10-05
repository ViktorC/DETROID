package net.viktorc.detroid.framework.util;

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

	private final byte numOfBytes;

	SizeOf(int numOfBytes) {
		this.numOfBytes = (byte)numOfBytes;
	}
	/**
	 * @return The number of bytes occupied in memory.
	 */
	public byte getNumOfBytes() {
		return numOfBytes;
	}
	/**
	 * Determines the space the mark word in an object header takes up in memory depending on the JVM.
	 * 
	 * @return The space the mark word in an object header takes up in memory
	 */
	private static int getJVMMarkWordSize() {
		return System.getProperty("sun.arch.data.model").equals("32") ? 4 : 8;
	}
	/**
	 * Determines the space object pointers take up in memory depending on the JVM.
	 * 
	 * @return The space object pointers take up in memory.
	 */
	private static int getJVMOOPSize() {
		RuntimeMXBean runtimeMX;
		boolean compressed, below32g;
		Pattern pattern;
		Matcher matcher;
		String xmxValueMagnitude;
		int xmxValueNum;
		if (System.getProperty("sun.arch.data.model").equals("32"))
			return 4;
		else {
			// As of Java 1.6.0_23, the JVM uses compressed OOPs by default.
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
						s = s.trim().toLowerCase();
						pattern = Pattern.compile("([0-9]+)([gmk]?)$");
						matcher = pattern.matcher(s);
						if (matcher.find()) {
							xmxValueNum = Integer.parseInt(matcher.group(1));
							xmxValueMagnitude = matcher.group(2);
							switch (xmxValueMagnitude) {
								case "g":
									below32g = xmxValueNum < 32;
									break;
								case "m":
									below32g = xmxValueNum < (32L << 10);
									break;
								case "k":
									below32g = xmxValueNum < (32L << 20);
									break;
								default:
									below32g = xmxValueNum < (32L << 30);
							}
						}
					}
				}
			}
			return compressed && below32g ? 4 : 8;
		}
	}
	
}
