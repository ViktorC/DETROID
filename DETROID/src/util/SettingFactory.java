package util;

/**
 * A factory for building different parameterized setting types.
 * 
 * @author Viktor
 *
 */
public class SettingFactory {

	/**
	 * Constructs and returns a boolean setting.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public Setting<Boolean> buildBoolSetting(String name, Boolean defaultValue) {
		return new Setting<>(name, defaultValue, null, null);
	}
	/**
	 * Constructs and returns a string setting.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public Setting<String> buildStringSetting(String name, String defaultValue) {
		return new Setting<>(name, defaultValue, null, null);
	}
	/**
	 * Constructs and returns a number setting.
	 * 
	 * @param name
	 * @param defaultValue
	 * @param min
	 * @param max
	 * @return
	 */
	public Setting<Number> buildNumberSetting(String name, Number defaultValue, Number min, Number max) {
		return new Setting<>(name, defaultValue, min, max);
	}
}
