package util;

/**A functional interface to serve as some kind of a method pointer to parameterless methods without a return type.
 * Its only declared method is {@link #execute() execute} which executes the procedure.
 * 
 * @author Viktor
 *
 */
public interface Procedure {

	/**Executes some functionality without any required parameters or returned values.*/
	void execute();
	
}
