package org.eclipse.emf.henshin.variability.multi.eval.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityFactory;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityRule;

public class RuleSetNormalizer {

	public static Rule normalizeRule(Rule rule) {
		return prepareRule(rule, false);
	}
	
	

	public static Rule prepareRule(Rule rule, boolean removeAttributes ) {
		
		Module module = rule.getModule();
		VariabilityRule newRule = VariabilityFactory.INSTANCE.createVariabilityRule();
		rule.getLhs().setFormula(null);
		newRule.setLhs(rule.getLhs());
		newRule.setRhs(rule.getRhs());
		newRule.getMappings().addAll(rule.getMappings());
		newRule.setName(rule.getName());
		newRule.getParameters().addAll(rule.getParameters());
		VariabilityRule vbRule = VariabilityFactory.INSTANCE.createVariabilityRule(rule);
		newRule.setFeatureModel(vbRule.getFeatureModel());
		newRule.setFeatures(vbRule.getFeatures());
		if (removeAttributes) {
		for (Node node : newRule.getLhs().getNodes()) {
			node.getAttributes().clear();
		}
		}
		for (Node node : newRule.getRhs().getNodes()) {
			node.getAttributes().clear();
		}
		if (module != null) {
			module.getUnits().remove(rule);
			module.getUnits().add(newRule);
		}
		return newRule;
	}

	public static List<Rule> prepareRules(Module module) {
		List<Rule> rules = new ArrayList<Rule>();
		for (Rule rule : module.getAllRules()) {
			rules.add(normalizeRule(rule));
		}
		
		// remove duplicates
		HashSet<String> usedNames = new HashSet<String>(); 
		for (Rule rule : module.getAllRules()) {
			if (usedNames.contains(rule.getName()))
				rules.remove(rule);
			else
				usedNames.add(rule.getName());
		}
		module.getUnits().retainAll(rules);
		return rules;
	}
}
