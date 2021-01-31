package org.eclipse.emf.henshin.variability.multi.eval.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

public class RuleSetNormalizer {

	public static Rule normalizeRule(Rule rule) {
		return prepareRule(rule, false);
	}

	public static Rule prepareRule(Rule rule, boolean removeAttributes) {
		Module module = rule.getModule();
		Rule newRule = HenshinFactory.eINSTANCE.createRule();
		rule.getLhs().setFormula(null);
		newRule.setLhs(rule.getLhs());
		newRule.setRhs(rule.getRhs());
		newRule.getMappings().addAll(rule.getMappings());
		newRule.setName(rule.getName());
		newRule.getParameters().addAll(rule.getParameters());
		String featureModel = VariabilityHelper.INSTANCE.getFeatureConstraint(rule);
		if (featureModel != null && !featureModel.isEmpty()) {
			VariabilityHelper.INSTANCE.setFeatureConstraint(newRule, featureModel);
		}
		Set<String> features = VariabilityHelper.INSTANCE.getFeatures(rule);
		if (features != null && !features.isEmpty()) {
			VariabilityHelper.INSTANCE.setFeatures(newRule, features);
		}
		if (removeAttributes) {
			for (Node node : newRule.getLhs().getNodes()) {
				node.getAttributes().clear();
			}
			for (Node node : newRule.getRhs().getNodes()) {
				node.getAttributes().clear();
			}
		}

		if (module != null) {
			module.getUnits().remove(rule);
			module.getUnits().add(newRule);
		}
		return newRule;
	}

	public static List<Rule> prepareRules(Module module) {
		//		List<Rule> rules = new ArrayList<>();
		//		for (Rule rule : module.getAllRules()) {
		//			rules.add(normalizeRule(rule));
		//		}

		// remove duplicates
		HashSet<String> usedNames = new HashSet<>();
		for (Rule rule : module.getAllRules()) {
			int i = 0;
			while (usedNames.contains(rule.getName())) {
				rule.setName(rule.getName() + i++);
			}
			usedNames.add(rule.getName());
		}
		//		module.getUnits().retainAll(rules);
		return module.getAllRules();
	}
}
