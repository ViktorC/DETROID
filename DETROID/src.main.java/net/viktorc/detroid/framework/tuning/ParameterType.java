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
	 * Parameters that function in way or another as search control parameters such as margins or reductions. 
	 * Parameters used both as static evaluation parameters and search control parameters should be categorized 
	 * as the latter.
	 */
	SEARCH_CONTROL_PARAMETER,
	/**
	 * Time management parameters, hash entry life-cycle parameters, etc. Parameters that can only be tuned by 
	 * game play. If a parameter is both a static evaluation or search control parameter and an engine management 
	 * parameter, it should be categorized as the latter.
	 */
	ENGINE_MANAGEMENT_PARAMETER;
	
}
