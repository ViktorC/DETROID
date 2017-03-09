package net.viktorc.detroid.framework.uci;

import java.util.Optional;
import java.util.Set;

/**
 * A parameterized type for storing a name, and possibly a default value, minimum and maximum values and a set of allowed values for different
 * setting types.
 * 
 * @author Viktor
 *
 * @param <T> The type that determines the type of the default value and the allowed values of the option.
 */
public class Option<T> {
	
	private final String name;
	private final Optional<T> defaultValue;
	private final Optional<Set<T>> allowedValues;
	private final Optional<Integer> min;
	private final Optional<Integer> max;
	
	/**
	 * Constructs an instance using the specified parameters.
	 * 
	 * @throws NullPointerException If the name is null.
	 */
	private Option(String name, Optional<T> defaultValue, Optional<Set<T>> allowedValues, Optional<Integer> min,
			Optional<Integer> max) throws NullPointerException {
		if (name == null)
			throw new NullPointerException();
		this.name = name;
		this.defaultValue = defaultValue == null ? Optional.ofNullable(null) : defaultValue;
		this.allowedValues = allowedValues == null ? Optional.ofNullable(null) : allowedValues;
		this.min = min == null ? Optional.ofNullable(null) : min;
		this.max = max == null ? Optional.ofNullable(null) : max;
	}
	/**
	 * Returns the name ID of the setting.
	 * 
	 * @return The name ID of the setting.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Returns the optional default value of the setting. For a {@link #ButtonOption}, it is never present; for all other 
	 * option types, it is always present.
	 * 
	 * @return The default value of the option.
	 */
	public Optional<T> getDefaultValue() {
		return defaultValue;
	}
	/**
	 * Returns an optional set of values a {@link #ComboOption} can possibly take on. For all other option types, it is 
	 * never present.
	 * 
	 * @return The set of allowed values for this option.
	 */
	public Optional<Set<T>> getAllowedValues() {
		return allowedValues;
	}
	/**
	 * Returns the optional minimum value for the setting. For option types other than {@link #SpinOption}, it is never 
	 * present.
	 * 
	 * @return The minimum allowed value for this option.
	 */
	public Optional<Integer> getMin() {
		return min;
	}
	/**
	 * Returns the optional maximum value for the setting. For option types other than {@link #SpinOption}, it is never 
	 * present.
	 * 
	 * @return The maximum allowed value for this option.
	 */
	public Optional<Integer> getMax() {
		return max;
	}

	/**
	 * A check option subclassing Setting with a Boolean type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class CheckOption extends Option<Boolean> {
		
		/**
		 * Constructs a check option instance. None of the parameters can be null.
		 * 
		 * @param name The name of the option. Cannot be null.
		 * @param defaultValue Whether it is set by default. Cannot be null.
		 * @throws NullPointerException If any of the parameters are null.
		 */
		public CheckOption(String name, Boolean defaultValue) throws NullPointerException {
			super(name, Optional.of(defaultValue), null, null, null);
		}
		
	}
	/**
	 * A spin option with minimum and maximum values subclassing Setting with an Integer type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class SpinOption extends Option<Integer> {
		
		/**
		 * Constructs a spin option instance. None of the parameters can be null.
		 * 
		 * @param name The name of the option. Cannot be null.
		 * @param defaultValue The value of the spin option by default. Cannot be null.
		 * @param min The minimum accepted value for the option (inclusive). Cannot be null.
		 * @param max The maximum accepted value for the option (inclusive). Cannot be null.
		 * @throws NullPointerException If any of the parameters are null.
		 */
		public SpinOption(String name, Integer defaultValue, Integer min, Integer max) throws NullPointerException {
			super(name, Optional.of(defaultValue), null, Optional.of(min), Optional.of(max));
		}
		
	}
	/**
	 * A simple string option subclassing Setting with a String type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class StringOption extends Option<String> {
		
		/**
		 * Constructs a string option instance. None of the parameters can be null.
		 * 
		 * @param name The name of the option. Cannot be null.
		 * @param defaultValue The value of the string option by default. Cannot be null.
		 * @throws NullPointerException If any of the parameters are null.
		 */
		public StringOption(String name, String defaultValue) {
			super(name, Optional.of(defaultValue), null, null, null);
		}
		
	}
	/**
	 * A combo option with a set of allowed values subclassing Setting with a String type parameter.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class ComboOption extends Option<String> {
		
		/**
		 * Constructs a combo option instance.
		 * 
		 * @param name The name of the option. Cannot be null.
		 * @param defaultValue The value of the combo option by default. Cannot be null.
		 * @param allowedValues The set of allowed values the option can take on. Cannot be null.
		 * @throws NullPointerException If any of the parameters are null.
		 */
		public ComboOption(String name, String defaultValue, Set<String> allowedValues) throws NullPointerException {
			super(name, Optional.of(defaultValue), Optional.of(allowedValues), null, null);
		}
		
	}
	/**
	 * A button option which only has a name and serves as a parameterless command.
	 * 
	 * @author Viktor
	 *
	 */
	public static final class ButtonOption extends Option<Object> {
		
		/**
		 * Constructs a button option.
		 * 
		 * @param name The name of the option. Cannot be null.
		 * @throws NullPointerException If the name is null.
		 */
		public ButtonOption(String name) throws NullPointerException {
			super(name, null, null, null, null);
		}
		
	}
	
}