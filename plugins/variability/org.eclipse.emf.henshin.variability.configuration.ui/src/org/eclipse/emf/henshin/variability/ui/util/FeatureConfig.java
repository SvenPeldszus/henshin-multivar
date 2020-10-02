package org.eclipse.emf.henshin.variability.ui.util;

import org.eclipse.emf.henshin.model.Rule;

import configuration.FeatureBinding;

/**
 * Helper class for managing feature bindings in the wizard.
 * 
 * @author Stefan Schulz
 */
public class FeatureConfig {
	
	String featureName;
	int featureBinding;
	Rule rule;
		
	public FeatureConfig(String featureName, FeatureBinding featureBinding, Rule rule) {
		this.featureName = featureName;
		this.featureBinding = featureBinding.getValue();
		this.rule = rule;
	}
	
	public FeatureConfig(String featureName, boolean bindingValue, Rule rule) {
		this(featureName, FeatureBinding.get(bindingValue), rule);
	}
	
	public FeatureConfig(String featureName, String bindingName, Rule rule) {
		this(featureName, FeatureBinding.getByName(bindingName), rule);
	}
	
	public FeatureConfig(String featureName, Rule rule) {
		this(featureName, FeatureBinding.UNBOUND, rule);
	}
	
	public String getFeatureName() {
		return featureName;
	}
	
	public int getFeatureBindingValue() {
		return featureBinding;
	}
	
	public FeatureBinding getFeatureBinding() {
		return FeatureBinding.get(featureBinding);
	}
	
	public void setFeatureBinding(FeatureBinding featureBinding) {
		this.featureBinding = featureBinding.getValue();
	}
	
	public void setFeatureBinding(int featureBinding) {
		this.featureBinding = featureBinding;
	}
	
	public void setFeatureBinding(String bindingName) {
		this.featureBinding = FeatureBinding.getByName(bindingName).getValue();
	}
	
	public String getFeatureBindingLiteral() {
		return FeatureBinding.get(featureBinding).getName();
	}
	
	public boolean isTrue() {
		return FeatureBinding.get(featureBinding) == FeatureBinding.TRUE;
	}
	
	public boolean isFalse() {
		return FeatureBinding.get(featureBinding) == FeatureBinding.FALSE;
	}
	
	public boolean isUnbound() {
		return FeatureBinding.get(featureBinding) == FeatureBinding.UNBOUND;
	}
	
	public boolean belongsToRule(Rule rule) {
		return this.rule.equals(rule);
	}
}
