package org.eclipse.emf.henshin.variability.wrapper;

/**
 * A collection of String constants for the variability feature.
 * 
 * @author Stefan Schulz
 *
 */
public enum VariabilityConstants {
	PRESENCE_CONDITION("presenceCondition"), 
	FEATURE_MODEL("featureModel"),
	INJECTIVE_MATCHING_PC("injectiveMatchingPresenceCondition"), 
	FEATURES("features");

	private final String value;

	private VariabilityConstants(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}
}
