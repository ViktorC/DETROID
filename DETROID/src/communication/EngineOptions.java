package communication;

public class EngineOptions<T> {

	private T default_;
	private final Integer min;
	private final Integer max;
	
	public EngineOptions(T default_, Integer min, Integer max) {
		this.default_ = default_;
		this.min = min;
		this.max = max;
	}
	public EngineOptions(T default_) {
		this(default_, null, null);
	}
	public T getDefault_() {
		return default_;
	}
	public void setDefault_(T default_) {
		this.default_ = default_;
	}
	public int getMin() {
		return min;
	}
	public int getMax() {
		return max;
	}
}