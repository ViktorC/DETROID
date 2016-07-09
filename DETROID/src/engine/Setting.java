package engine;

public class Setting<T> {
	
	private final String name;
	private final T defaultValue;
	private final Integer min;
	private final Integer max;
	private T value;
	
	private Setting(String name, T defaultValue, Integer min, Integer max) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.value = defaultValue;
	}
	public String getName() {
		return name;
	}
	public T getDefaultValue() {
		return defaultValue;
	}
	public Integer getMin() {
		return min;
	}
	public Integer getMax() {
		return max;
	}
	public T getValue() {
		return value;
	}
	public void setValue(T value) {
		this.value = value;
	}
	
	public static class Factory {
		
		
	}
}
