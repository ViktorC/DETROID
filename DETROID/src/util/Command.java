package util;

import java.util.function.*;

/**A generic class for command interpretation and execution. The Command object itself has its inherent functionality and execution conditions
 * assigned to it via method references on initialization. Instances of the Command class can be tested with the method {@link #isCalled(T) isCalled}
 * to see if the input parameter instruction calls them. Commands can be executed by calling {@link #execute(T) execute} with the instructions/script
 * as the argument(s).
 * 
 * @author Viktor
 *
 * @param <T> The type of the expected instructions.
 */
public class Command<T> {

	private Predicate<T> predicate;	// The conditions for execution.
	private Consumer<T> consumer;	// The functionality of the command.
	
	/**Initializes the command by assigning method references to it for execution conditions and command functionalities.
	 * 
	 * @param predicate The conditions for execution.
	 * @param consumer The functionality of the command. An {@link #utils.IllegalCommandArgumentException IllegalCommandArgumentException} should be
	 * thrown in case of an execution failure due to unexpected command instructions.
	 */
	public Command(Predicate<T> predicate, Consumer<T> consumer) {
		this.predicate = predicate;
		this.consumer = consumer;
	}
	/**Returns whether the instruction addresses the command the caller object comprises.
	 * 
	 * @param instruction A script line or any kind of instruction type that extends or implements the actual type parameter of the Command instance.
	 * @return True if the instructions calls the command, false otherwise.
	 */
	public boolean isCalled(T instruction) {
		return predicate.test(instruction);
	}
	/**Tries to execute the argument command; if it is legal, it gets executed, else it throws an
	 * {@link #utils.IllegalCommandArgumentException IllegalCommandArgumentException}.
	 * 
	 * @param instruction A script line or any kind of instruction type that extends or implements the actual type parameter of the Command instance.
	 * @throws IllegalCommandArgumentException If the command can not be executed due to invalid argument(s).
	 */
	public void execute(T instruction) throws IllegalCommandArgumentException {
		consumer.accept(instruction);
	}
}
