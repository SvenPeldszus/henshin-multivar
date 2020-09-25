package org.eclipse.emf.henshin.variability.matcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.util.VBRuleUtil;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.SymbolCollector;

public class VBRuleInfo {
	private final Rule rule;
	private final Map<String, Sentence> usedExpressions;
	private final Map<Sentence, Set<GraphElement>> pc2elem;
	private final Map<ModelElement, String> pcs;
	private final Map<Node, Set<Mapping>> node2Mapping;
	private final Sentence featureModel;
	private final Sentence injectiveMatching;
	private final Collection<String> features;

	public VBRuleInfo(Rule rule) throws InconsistentRuleException {
		this.pcs = new HashMap<>();
		this.rule = fixInconsistencies(rule);
		this.featureModel = FeatureExpression.getExpr(VariabilityHelper.INSTANCE.getFeatureConstraint(this.rule));
		this.features = VariabilityHelper.INSTANCE.getFeatures(this.rule);
		if(!checkRule()) {
			throw new InconsistentRuleException();
		}
		String injective = VariabilityHelper.INSTANCE.getInjectiveMatchingPresenceCondition(this.rule);
		if (injective == null) {
			injective = Boolean.toString(rule.isInjectiveMatching());
		}
		this.injectiveMatching = FeatureExpression.getExpr(injective);
		this.usedExpressions = new HashMap<>();
		this.node2Mapping = new HashMap<>();
		this.pc2elem = new HashMap<>();
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

	private void populateMaps() {
		TreeIterator<EObject> it = this.rule.eAllContents();
		while (it.hasNext()) {
			EObject o = it.next();
			if (o instanceof Node || o instanceof Edge || o instanceof Attribute) {
				String pc = getPC((ModelElement) o);
				if (!VBRuleInfo.presenceConditionEmpty(pc)) {
					Sentence expr = FeatureExpression.getExpr(pc);
					this.usedExpressions.put(pc, expr);
					if (!this.pc2elem.containsKey(expr)) {
						this.pc2elem.put(expr, new HashSet<>());
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

	private Rule fixInconsistencies(Rule rule) {
		// Per definition, mapped nodes must have the same presence condition
		// in the LHS and the RHS.
		for (Mapping mapping : rule.getMappings()) {
			Node image = mapping.getImage();
			String originPresenceCondition = getPC(mapping.getOrigin());
			if (!originPresenceCondition.equals(getPC(image))) {
				VariabilityHelper.INSTANCE.setPresenceCondition(image, originPresenceCondition);
				this.pcs.put(image, originPresenceCondition);
			}
		}
		return rule;
	}

	public String getPC(ModelElement ruleElement) {
		return this.pcs.computeIfAbsent(ruleElement, x -> VariabilityHelper.INSTANCE.getPresenceCondition(ruleElement));
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
		return VariabilityHelper.INSTANCE.isFeatureConstraintCNF(this.rule);
	}

	public boolean checkRule() {
		if (!VBRuleUtil.isVarRule(this.rule)) {
			return true;
		}

		Stream<PropositionSymbol> fm = SymbolCollector.getSymbolsFrom(this.featureModel).parallelStream();
		Stream<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(FeatureExpression.getExpr(getPC(this.rule)))
				.parallelStream();
		List<String> symbolNames = Stream.concat(fm, symbols).map(PropositionSymbol::getSymbol).distinct()
				.collect(Collectors.toList());

		return this.features.containsAll(symbolNames);
	}

	public Rule getRule() {
		return this.rule;
	}

	public Collection<String> getFeatures() {
		return this.features;
	}

}