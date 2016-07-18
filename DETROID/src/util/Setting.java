package util;

/**
 * A parameterized type for storing a name, a default value, and an actual value for different setting types. For number based settings, a
 * minimum and a maximum value can be specified as well.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public class Setting<T> {
	
	private final String name;
	private final T defaultValue;
	private final Number min;
	private final Number max;
	
	private Setting(String name, T defaultValue, Number min, Number max) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
	}
	/**
	 * Returns the name ID of the setting.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * Returns the default value of the setting.
	 * 
	 * @return
	 */
	public T getDefaultValue() {
		return defaultValue;
	}
	/**
	 * Returns the minimum value for the setting. For non-number based setting types, it is always null.
	 * 
	 * @return
	 */
	public Number getMin() {
		return min;
	}
	/**
	 * Returns the maximum value for the setting. For non-number based setting types, it is always null.
	 * 
	 * @return
	 */
	public Number getMax() {
		return max;
	}
	
	/**
	 * A factory class for building different parameterized setting types.
	 * 
	 * @author Viktor
	 *
	 */
	public static class Builder {

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
}
