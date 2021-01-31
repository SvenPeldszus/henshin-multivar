package org.eclipse.emf.henshin.variability.matcher;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
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
	private final Map<ModelElement, Sentence> pcs;
	private final Sentence featureModel;
	private final Sentence injectiveMatching;
	private final Collection<String> features;

	public VBRuleInfo(final Rule rule) throws InconsistentRuleException {
		this.pcs = new ConcurrentHashMap<>();
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
	}

	public Sentence getFeatureModel() {
		return this.featureModel;
	}

	private Rule fixInconsistencies(final Rule rule) {
		// Per definition, mapped nodes must have the same presence condition
		// in the LHS and the RHS.
		for (final Mapping mapping : rule.getMappings()) {
			final Node image = mapping.getImage();
			final Sentence originPresenceCondition = getPC(mapping.getOrigin());
			final Sentence imagePresenceCondition = getPC(image);
			if ((originPresenceCondition != null) && !originPresenceCondition.equals(imagePresenceCondition)) {
				VariabilityHelper.INSTANCE.setPresenceCondition(image, originPresenceCondition.toString());
				this.pcs.put(image, originPresenceCondition);
			}
		}
		return rule;
	}

	public Sentence getPC(final ModelElement ruleElement) {
		return this.pcs.computeIfAbsent(ruleElement, x -> FeatureExpression.getExpr(VariabilityHelper.INSTANCE.getPresenceConditionString(ruleElement)));
	}

	public Sentence getInjectiveMatching() {
		return this.injectiveMatching;
	}

	public boolean isFeatureConstraintCNF() {
		return VariabilityHelper.INSTANCE.isFeatureConstraintCNF(this.rule);
	}

	public boolean checkRule() {
		if (!VBRuleUtil.isVarRule(this.rule)) {
			return true;
		}

		final Stream<PropositionSymbol> fm = SymbolCollector.getSymbolsFrom(this.featureModel).parallelStream();
		final Stream<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(getPC(this.rule))
				.parallelStream();
		final List<String> symbolNames = Stream.concat(fm, symbols).map(PropositionSymbol::getSymbol).distinct()
				.collect(Collectors.toList());

		return this.features.containsAll(symbolNames);
	}

	public Rule getRule() {
		return this.rule;
	}

	public Collection<String> getFeatures() {
		return this.features;
	}

	public Set<ModelElement> getElementsWithPC(final Sentence expr) {
		return getAllPCs().parallelStream().filter(entry -> expr.equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	public Set<Mapping> getMappings(final ModelElement ge) {
		if(ge == null) {
			return Collections.emptySet();
		}
		return this.rule.getAllMappings().parallelStream().filter(mapping -> ge.equals(mapping.getOrigin()) || ge.equals(mapping.getImage())).collect(Collectors.toSet());
	}

	public Set<Entry<ModelElement, Sentence>> getAllPCs() {
		final TreeIterator<EObject> iterator = this.rule.eAllContents();
		while(iterator.hasNext()) {
			final EObject next = iterator.next();
			if(next instanceof ModelElement){
				getPC((ModelElement) next);
			}
		}
		return this.pcs.entrySet();
	}

}