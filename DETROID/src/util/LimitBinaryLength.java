package util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation for limiting the number of active bits included in the binary output of the subclasses of
 * {@link #util.Parameters Parameters}. This is also the number of bits that will be parsed for the 
 * respective field of the input of the method {@link #util.Parameters.set(String) setBasedOnGrayCodeString}. 
 * If {@link #value() value} is negative or higher than the maximum number of bits in an unsigned primitive 
 * or the maximum number of bits minus one in a signed primitive, it will be ignored. If it is zero, the 
 * parameter itself will be ignored.
 * 
 * @author Viktor
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface LimitBinaryLength {
	/**
	 * The number of bits to consider counting from the least significant bit.
	 * 
	 * @return
	 */
	byte value();
}
