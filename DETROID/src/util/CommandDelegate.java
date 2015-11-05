package util;

import java.util.function.*;

/**A generic class for command handling. The Command object itself has its inherent functionality and execution conditions assigned to it on
 * initialization. Commands can be executed by calling {@link #execute(T) execute} with the command as the argument.
 * 
 * @author Viktor
 *
 * @param <T> The type of the expected command.
 */
public class CommandDelegate<T> {

	private Predicate<T> predicate;	// The conditions for execution.
	private Consumer<T> consumer;	// The functionality of the command.
	
	/**Initializes the handler by assigning the execution conditions and command functionalities to it.
	 * 
	 * @param predicate The conditions for execution.
	 * @param function The functionality of the command.
	 */
	public CommandDelegate(Predicate<T> predicate, Consumer<T> consumer) {
		this.predicate = predicate;
		this.consumer = consumer;
	}
	/**Tries to execute the argument command; if it fits the conditions, it gets executed and the method returns true, else it returns false.
	 * 
	 * @param command The command to be executed.
	 * @return True if the command was accepted and thus executed, false otherwise.
	 */
	public boolean execute(T command) {
		if (predicate.test(command)) {
			consumer.accept(command);
		}
	}
}
