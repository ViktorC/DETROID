package net.viktorc.detroid.framework.engine;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A singleton class for reading and writing magic numbers and magic shifts for sliding pieces on specific squares.
 *
 * @author Viktor
 */
public class MagicsConfig {

	private static final String PROPERTIES_FILE_PATH = "magics.properties";
	private static final String ROOK_KEY_FORMAT = "R%d";
	private static final String BISHOP_KEY_FORMAT = "B%d";
	private static final String VALUE_SEPARATOR = "-";
	private static final String DESCRIPTION = "Magic numbers and magic shift values.";
	private static final MagicsConfig INSTANCE = new MagicsConfig();

	private Properties props;
	private boolean loaded;

	private MagicsConfig() {
		props = new Properties();
	}
	/**
	 * @return The one and only instance.
	 */
	public static MagicsConfig getInstance() {
		return INSTANCE;
	}
	private Map.Entry<Long,Byte> getMagics(String key) {
		String val = props.getProperty(key);
		if (val == null)
			return null;
		String[] magics = val.split(VALUE_SEPARATOR);
		return new AbstractMap.SimpleEntry<>(Long.valueOf(magics[0]), Byte.valueOf(magics[1]));
	}
	private void setMagics(String key, Map.Entry<Long,Byte> magics) {
		props.setProperty(key, magics.getKey() + VALUE_SEPARATOR + magics.getValue());
	}
	/**
	 * @param sqrInd The square index.
	 * @return A key-value pair containing the magic number and the magic shift for a rook on the specified square.
	 */
	public Map.Entry<Long,Byte> getRookMagics(int sqrInd) {
		return getMagics(String.format(ROOK_KEY_FORMAT, sqrInd));
	}
	/**
	 * @param sqrInd The square index.
	 * @param magics A key-value pair containing the magic number and the magic shift for a rook on the specified
	 * square.
	 */
	public synchronized void setRookMagics(int sqrInd, Map.Entry<Long,Byte> magics) {
		setMagics(String.format(ROOK_KEY_FORMAT, sqrInd), magics);
	}
	/**
	 * @param sqrInd The square index.
	 * @return A key-value pair containing the magic number and the magic shift for a bishop on the specified square.
	 */
	public Map.Entry<Long,Byte> getBishopMagics(int sqrInd) {
		return getMagics(String.format(BISHOP_KEY_FORMAT, sqrInd));
	}
	/**
	 * @param sqrInd The square index.
	 * @param magics A key-value pair containing the magic number and the magic shift for a bishop on the specified
	 * square.
	 */
	public synchronized void setBishopMagics(int sqrInd, Map.Entry<Long,Byte> magics) {
		setMagics(String.format(BISHOP_KEY_FORMAT, sqrInd), magics);
	}
	/**
	 * It loads the key-values from the properties file if they are not yet loaded.
	 *
	 * @throws IOException If the properties file cannot be read.
	 */
	public synchronized void load() throws IOException {
		if (loaded)
			return;
		try (FileInputStream in = new FileInputStream(PROPERTIES_FILE_PATH)) {
			props.load(in);
		}
		loaded = true;
	}
	/**
	 * It reloads the key-values from the properties file whether they have been loaded before or not.
	 *
	 * @throws IOException If the properties file cannot be read.
	 */
	public synchronized void reload() throws IOException {
		loaded = false;
		load();
	}
	/**
	 * It saves the key-values of the instance to the properties file.
	 *
	 * @throws IOException If the properties file cannot be written to.
	 */
	public synchronized void save() throws IOException {
		try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE_PATH)) {
			props.store(out, DESCRIPTION);
		}
	}

	public static void main(String[] args) throws Exception {
		MagicsConfig config = MagicsConfig.getInstance();
		List<Map.Entry<Long,Byte>> rookMagics = Bitboard.generateAllMagics(true);
		List<Map.Entry<Long,Byte>> bishopMagics = Bitboard.generateAllMagics(false);
		for (int i = 0; i < 64; i++) {
			config.setRookMagics(i, rookMagics.get(i));
			config.setBishopMagics(i, bishopMagics.get(i));
		}
		config.save();
	}

}
