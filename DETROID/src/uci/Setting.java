package uci;

import java.util.Set;

/**
 * A parameterized type for storing a name, a default value, and possibly minimum, maximum values or a set of allowed values for different
 * setting types.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public class Setting<T> {
	
	private final String name;
	private final T defaultValue;
	private final Set<T> allowedValues;
	private final Integer min;
	private final Integer max;
	
	private Setting(String name, T defaultValue, Set<T> allowedValues, Integer min, Integer max) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.allowedValues = allowedValues;
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
	 * Returns the values a 'combobox' type setting can possibly take on.
	 * 
	 * @return
	 */
	public Set<T> getAllowedValues() {
		return allowedValues;
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
		public Setting<Boolean> buildBooleanSetting(String name, Boolean defaultValue) {
			return new Setting<>(name, defaultValue, null, null, null);
		}
		/**
		 * Constructs and returns a string setting.
		 * 
		 * @param name
		 * @param defaultValue
		 * @return
		 */
		public Setting<String> buildStringSetting(String name, String defaultValue) {
			return new Setting<>(name, defaultValue, null, null, null);
		}
		/**
		 * Constructs and returns an integer setting.
		 * 
		 * @param name
		 * @param defaultValue
		 * @param min
		 * @param max
		 * @return
		 */
		public Setting<Integer> buildIntegerSetting(String name, Integer defaultValue, Integer min, Integer max) {
			return new Setting<>(name, defaultValue, null, min, max);
		}
		/**
		 * Constructs and returns string combobox setting.
		 * 
		 * @param name
		 * @param defaultValue
		 * @param min
		 * @param max
		 * @return
		 */
		public Setting<String> buildStringComboSetting(String name, String defaultValue, Set<String> possibleValues) {
			return new Setting<>(name, defaultValue, possibleValues, null, null);
		}
	}
}