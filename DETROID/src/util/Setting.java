package util;

/**
 * A parameterized type for storing a name, a default value, and an actual value for different setting types. For number based settings, a minimum
 * and a maximum value can be specified as well.
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
	private T value;
	
	protected Setting(String name, T defaultValue, Number min, Number max) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.value = defaultValue;
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
	 * Returns the actual value of the setting.
	 * 
	 * @return
	 */
	public T getValue() {
		return value;
	}
	/**
	 * Sets the actual value of the setting.
	 * 
	 * @param value
	 */
	public void setValue(T value) {
		this.value = value;
	}
}
