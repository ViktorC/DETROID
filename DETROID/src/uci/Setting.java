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
	
	protected Setting(String name, T defaultValue, Set<T> allowedValues, Integer min, Integer max) {
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
	 * A boolean setting subclassing Setting with a Boolean type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static class BooleanSetting extends Setting<Boolean> {
		
		public BooleanSetting(String name, Boolean defaultValue) {
			super(name, defaultValue, null, null, null);
		}
	}
	/**
	 * An integer setting with minimum and maximum values subclassing Setting with an Integer type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static class IntegerSetting extends Setting<Integer> {
		
		public IntegerSetting(String name, Integer defaultValue, Integer min, Integer max) {
			super(name, defaultValue, null, min, max);
		}
	}
	/**
	 * A simple string setting subclassing Setting with a String type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static class StringSetting extends Setting<String> {
		
		public StringSetting(String name, String defaultValue) {
			super(name, defaultValue, null, null, null);
		}
	}
	/**
	 * A combo string setting with a set of allowed values subclassing Setting with a String type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static class StringComboSetting extends Setting<String> {
		
		public StringComboSetting(String name, String defaultValue, Set<String> allowedValues) {
			super(name, defaultValue, allowedValues, null, null);
		}
	}
}