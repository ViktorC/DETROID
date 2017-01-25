package net.viktorc.detroid.framework.tuning;

/**
 * An enumeration for engine parameter type definitions.
 * 
 * @author Viktor
 *
 */
public enum ParameterType {

	/**
	 * Parameters only used in the static evaluation function. E.g. material values.
	 */
	STATIC_EVALUATION_PARAMETER,
	/**
	 * Parameters that function in way or another as search control parameters such as margins or reductions 
	 * and engine control parameters such as time spent on search, hash size distribution, etc. Parameters 
	 * used both as static evaluation parameters and engine or search control parameters should be categorized 
	 * as the latter.
	 */
	ENGINE_OR_SEARCH_CONTROL_PARAMETER;
	
}
