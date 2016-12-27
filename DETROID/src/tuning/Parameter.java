package tuning;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation for denoting fields to be considered as engine parameters possibly eligible for tuning.
 * 
 * @author Viktor
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Parameter {
	
	/**
	 * The number of bits to include in the binary output for the annotated field counting from the least significant bit. 
	 * If it is zero or less, or higher than the maximum number of bits in an unsigned primitive or the maximum number of 
	 * bits minus one in a signed primitive, the field shall be ignored during tuning.
	 * 
	 * @return The number of bits to include in the binary output for the annotated field.
	 */
	byte binaryLengthLimit() default -1;
}
