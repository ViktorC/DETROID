package tuning;

/**
 * An exception for when a field declared as an engine parameter by the {@link #Parameter Parameter} annotation is a static or 
 * non-primitive field.
 * 
 * @author Viktor
 *
 */
public class ParameterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an exception instance with the specified error message.
	 * 
	 * @param message The cause of the exception.
	 */
	public ParameterException(String message) {
		super(message);
	}
}
