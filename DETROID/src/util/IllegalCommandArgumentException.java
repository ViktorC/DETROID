package util;

/**A runtime exception sub-classing {@link #java.lang.IllegalArgumentException IllegalArgumentException} for when the argument(s) following a text
 * or any kind of command are invalid as in they are not accepted by the command interpreter.
 * 
 * @author Viktor
 *
 */
public class IllegalCommandArgumentException extends IllegalArgumentException {
	
	/*Auto-generated serial code.*/
	private static final long serialVersionUID = 8087521722058688364L;

	public IllegalCommandArgumentException() {
		
	}
	public IllegalCommandArgumentException(String message) {
		super(message);
	}
	public IllegalCommandArgumentException(Throwable cause) {
		super(cause);
	}
	public IllegalCommandArgumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
