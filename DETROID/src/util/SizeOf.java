package util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**A simple enumeration type to help estimate the memory overhead of objects.
 * 
 * @author Viktor
 *
 */
public enum SizeOf {

	POINTER(getJVMPointerSize()),
	BYTE(1),
	BOOLEAN(1),
	SHORT(2),
	CHAR(2),
	INT(4),
	FLOAT(4),
	LONG(8),
	DOUBLE(8);
	
	/**The number of bytes occupied in memory. */
	public final byte numOfBytes;
	
	private SizeOf(int numOfBytes) {
		this.numOfBytes = (byte)numOfBytes;
	}
	/**Determines the space pointers take up in memory depending on the JVM.
	 * 
	 * @return
	 */
	private static int getJVMPointerSize() {
		RuntimeMXBean runtimeMX;
		String dataModel = System.getProperty("sun.arch.data.model");
		if (dataModel.equals("32"))
			return 4;
		else {
			runtimeMX = ManagementFactory.getRuntimeMXBean();
			for (String s : runtimeMX.getInputArguments()) {
				if (s != null && s.contains("UseCompressedOops"))
					return 4;
			}
			return 8;
		}
	}
}
