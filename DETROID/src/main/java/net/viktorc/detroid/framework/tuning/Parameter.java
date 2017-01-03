package main.java.net.viktorc.detroid.framework.tuning;

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
	 * The type of the parameter e.g. static evaluation parameter or engine or search control parameter. As in most engines the 
	 * great majority of parameters, especially those wished to be tuned, are static evaluation parameters, so is the default 
	 * value for this attribute.
	 * 
	 * @return The type of the parameter.
	 */
	ParameterType type() default ParameterType.STATIC_EVALUATION_PARAMETER;
	/**
	 * The number of bits to include in the binary output for the annotated field counting from the least significant bit. 
	 * If it is less than zero or higher than the maximum number of bits in an unsigned primitive or the maximum number of 
	 * bits minus one in a signed primitive, no binary length limit shall be imposed. If it is zero, the field shall be ignored 
	 * during tuning. It's default value is -1 which means the binary length limit shall be ignored by default.
	 * 
	 * @return The number of bits to include in the binary output for the annotated field.
	 */
	byte binaryLengthLimit() default -1;
	
}
