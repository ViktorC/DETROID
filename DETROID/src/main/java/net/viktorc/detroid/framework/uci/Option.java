package main.java.net.viktorc.detroid.framework.uci;

import java.util.Set;

/**
 * A parameterized type for storing a name, a default value, and possibly minimum, maximum values or a set of allowed values for different
 * setting types.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public class Option<T> {
	
	private final String name;
	private final T defaultValue;
	private final Set<T> allowedValues;
	private final Integer min;
	private final Integer max;
	
	private Option(String name, T defaultValue, Set<T> allowedValues, Integer min, Integer max) {
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
	 * A check option subclassing Setting with a Boolean type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class CheckOption extends Option<Boolean> {
		
		public CheckOption(String name, Boolean defaultValue) {
			super(name, defaultValue, null, null, null);
		}
	}
	/**
	 * A spin option with minimum and maximum values subclassing Setting with an Integer type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class SpinOption extends Option<Integer> {
		
		public SpinOption(String name, Integer defaultValue, Integer min, Integer max) {
			super(name, defaultValue, null, min, max);
		}
	}
	/**
	 * A simple string option subclassing Setting with a String type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class StringOption extends Option<String> {
		
		public StringOption(String name, String defaultValue) {
			super(name, defaultValue, null, null, null);
		}
	}
	/**
	 * A combo option with a set of allowed values subclassing Setting with a String type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class ComboOption extends Option<String> {
		
		public ComboOption(String name, String defaultValue, Set<String> allowedValues) {
			super(name, defaultValue, allowedValues, null, null);
		}
	}
	/**
	 * A button option which only has a name and serves as a parameterless command.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class ButtonOption extends Option<Object> {
		public ButtonOption(String name) {
			super(name, null, null, null, null);
		}
	}
	
}