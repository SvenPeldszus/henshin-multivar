package org.eclipse.emf.henshin.variability.matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.Sentence;

public class VBRuleInfo {
	Rule rule;
	Map<String, Sentence> usedExpressions;
	Map<Sentence, Set<GraphElement>> pc2elem;
	Map<Node, Set<Mapping>> node2Mapping;
	Map<NestedCondition, Rule> applicationConditions;
	Sentence featureModel;
	Sentence injectiveMatching;

	public VBRuleInfo(Rule rule) {
		this.rule = rule;
		this.featureModel = FeatureExpression.getExpr(VariabilityHelper.INSTANCE.getFeatureConstraint(this.rule));
		String injective = VariabilityHelper.INSTANCE.getInjectiveMatchingPresenceCondition(this.rule);
		if (injective == null) {
			injective = Boolean.toString(rule.isInjectiveMatching());
		}
		this.injectiveMatching = FeatureExpression.getExpr(injective);

		populateMaps();
	}

	public Map<Sentence, Set<GraphElement>> getPc2Elem() {
		return this.pc2elem;
	}

	public Map<String, Sentence> getExpressions() {
		return this.usedExpressions;
	}

	public Sentence getFeatureModel() {
		return this.featureModel;
	}

	public void populateMaps() {
		this.usedExpressions = new HashMap<>();
		this.node2Mapping = new HashMap<>();
		this.pc2elem = new HashMap<>();
		TreeIterator<EObject> it = this.rule.eAllContents();
		while (it.hasNext()) {
			EObject o = it.next();
			if (o instanceof Node || o instanceof Edge || o instanceof Attribute) {
				String pc = VariabilityHelper.INSTANCE.getPresenceConditionIfModelElement((GraphElement) o);
				if (!VBRuleInfo.presenceConditionEmpty(pc)) {
					Sentence expr = FeatureExpression.getExpr(pc);
					this.usedExpressions.put(pc, expr);
					if (!this.pc2elem.containsKey(expr)) {
						this.pc2elem.put(expr, new HashSet<GraphElement>());
					}
					this.pc2elem.get(expr).add((GraphElement) o);
				}
			}
			if (o instanceof Mapping) {
				Mapping m = (Mapping) o;

				Node image = m.getImage();
				Set<Mapping> set = this.node2Mapping.get(image);
				if (set == null) {
					set = new HashSet<>();
					this.node2Mapping.put(image, set);
				}
				set.add(m);
				Node origin = m.getOrigin();
				set = this.node2Mapping.get(origin);
				if (set == null) {
					set = new HashSet<>();
					this.node2Mapping.put(origin, set);
				}
				set.add(m);
			}
		}

		if (this.featureModel != null && !this.featureModel.toString().isEmpty()
				&& !this.pc2elem.containsKey(this.featureModel)) {
			this.pc2elem.put(this.featureModel, new HashSet<>());
		}

	}

	public Map<Node, Set<Mapping>> getNode2Mapping() {
		return this.node2Mapping;
	}

	public Sentence getInjectiveMatching() {
		return this.injectiveMatching;
	}

	private static boolean presenceConditionEmpty(String presenceCondition) {
		return (presenceCondition == null) || presenceCondition.isEmpty();
	}

	public boolean isFeatureConstraintCNF() {
		return  VariabilityHelper.INSTANCE.isFeatureConstraintCNF(this.rule);
	}

}