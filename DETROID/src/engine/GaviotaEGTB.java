package engine;

class GaviotaEGTB {

	static {
		System.loadLibrary("/gtb");
	}
	
	private final native static boolean init(String[] paths, long cacheSize);
	private final native static int availability();
	private final native static int probeDTM(boolean stm, int epsq, int castles, int[] wSQ, int[] bSQ,
			byte[] wPC, byte[] bPC, int[] res);
	
	GaviotaEGTB(String tableBasePath, long cacheSizeInBytes) {
		
	}
}
