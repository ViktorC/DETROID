package util;

import java.util.function.*;

/**A class for text command interpretation and execution. The TextCommand itself has its inherent functionality and execution conditions assigned to
 * it via method references on initialization. Instances of the TextCommand class can be tested with the method {@link #isCalled(String) isCalled}
 * to see if the input parameter instruction calls them. TextCommands can be executed by calling {@link #execute(String) execute} with the
 * instructions/script as the argument(s). Furthermore, a response to the caller can also be produced via {@link #respond(boolean) respond} which
 * should be called with a true boolean parameter if the command has been successfully executed, or with false if it did not.
 * 
 * @author Viktor
 */
public class TextCommand {

	private Predicate<String> predicate;		// The conditions for execution.
	private Consumer<String> consumer;			// The functionality of the command.
	private Function<Boolean, String> response;	// The expected response message depending on the success of the execution of the command.
	
	/**Initializes the command by assigning method references to it for execution conditions and command functionalities.
	 * 
	 * @param predicate The conditions for execution.
	 * @param consumer The functionality of the command. An {@link #utils.IllegalCommandArgumentException IllegalCommandArgumentException} should be
	 * thrown in case of an execution failure due to unexpected command instructions.
	 */
	public TextCommand(Predicate<String> predicate, Consumer<String> consumer, Function<Boolean, String> response) {
		this.predicate = predicate;
		this.consumer = consumer;
		this.response = response;
	}
	/**Returns whether the instruction addresses the command the caller object comprises.
	 * 
	 * @param instruction A script line or any kind of instruction type that extends or implements the actual type parameter of the Command instance.
	 * @return True if the instructions calls the command, false otherwise.
	 */
	public boolean isCalled(String commandLine) {
		return predicate.test(commandLine);
	}
	/**Tries to execute the argument command; if it is legal, it gets executed, else it throws an
	 * {@link #utils.IllegalCommandArgumentException IllegalCommandArgumentException}.
	 * 
	 * @param instruction A script line or any kind of instruction type that extends or implements the actual type parameter of the Command instance.
	 * @throws IllegalCommandArgumentException If the command can not be executed due to invalid argument(s).
	 */
	public void execute(String commandLine) throws IllegalCommandArgumentException {
		consumer.accept(commandLine);
	}
	/**A response to the caller of the command.
	 * 
	 * @param executionSuccessful Whether the execution of the command was successful or not. Should be false if
	 * {@link #TextCommand.execute(String) execute} threw an {@link #utils.IllegalCommandArgumentException IllegalCommandArgumentException}, true
	 * otherwise.
	 * 
	 * @return The appropriate response to the call of the command.
	 */
	public String respond(boolean executionSuccessful) {
		return response.apply(executionSuccessful);
	}
}
