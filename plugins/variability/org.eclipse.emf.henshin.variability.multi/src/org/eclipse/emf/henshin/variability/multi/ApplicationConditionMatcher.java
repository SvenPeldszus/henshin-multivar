package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;

public class ApplicationConditionMatcher {

	private final Map<NestedCondition, ACRule> acCache;
	private final List<NestedCondition> nacs;
	private final List<NestedCondition> pacs;
	private final Engine engine;

	public ApplicationConditionMatcher(final Engine engine, final Rule rule) {
		this.engine = engine;
		this.nacs = new LinkedList<>();
		this.pacs = new LinkedList<>();
		this.acCache = new ConcurrentHashMap<>();
		fillACs(rule);
	}

	/**
	 * Searches all PACs and NACs in the rule and adds them to the lists of PACs and
	 * NACs
	 *
	 * @param rule
	 */
	private void fillACs(final Rule rule) {
		final Deque<Formula> formulas = new LinkedList<>();
		formulas.add(rule.getLhs().getFormula());
		while (!formulas.isEmpty()) {
			final Formula next = formulas.pop();
			if (next instanceof Not) {
				getNACs().add((NestedCondition) ((Not) next).getChild());
			} else if (next instanceof And) {
				formulas.push(((And) next).getLeft());
				formulas.push(((And) next).getRight());
			} else if (next instanceof NestedCondition) {
				getPACs().add((NestedCondition) next);
			}
		}
	}

	/**
	 * @param rulePreparator            The rule preparator
	 * @param nodeToOriginalNodeMapping A map to which a mapping from the nodes of
	 *                                  the new rules to the nodes of original rule
	 *                                  should be added
	 * @param trueFeatures              The feature conditions of the rule that
	 *                                  evaluate to true
	 * @return A map of the crated rules and their context nodes
	 */
	Stream<ACRule> createACRules(final Collection<NestedCondition> acs) {
		return acs.stream().map(ac -> this.acCache.computeIfAbsent(ac, MultiVarRuleUtil::createPreserveRuleForAC));
	}

	/**
	 * @param nacs       A mapping between the extracted NAC rules and the context
	 *                   nodes of the NACs
	 * @param originalRuleMatch      The match of the original rule
	 * @return The matches for the NAC rules
	 */
	Map<Rule, Collection<Match>> getNACMatches(final List<ACRule> nacs, final Match originalRuleMatch,
			final EGraph graph) {
		final Object prev  = this.engine.getOptions().put(Engine.OPTION_CHECK_DANGLING, false);
		final Map<Rule, Collection<Match>> nacMatchMap = new HashMap<>();
		for (final ACRule entry : nacs) {
			final Rule nac = entry.getRule();
			final Match preMatch = prepare(originalRuleMatch, entry);
			final Collection<Match> matches = new LinkedList<>();
			for (final Match nacMatch : this.engine.findMatches(nac, graph, preMatch)) {
				matches.add(nacMatch);
			}
			nacMatchMap.put(nac, matches);
		}
		this.engine.getOptions().put(Engine.OPTION_CHECK_DANGLING, prev);
		return nacMatchMap;
	}

	private Match prepare(final Match originalRuleMatch, final ACRule entry) {
		final Rule rule = entry.getRule();
		final Match preMatch = new MatchImpl(rule);
		for (final Node contextNode : entry.getContextNodesOfACRule()) {
			final EObject value = originalRuleMatch.getNodeTarget(entry.getOriginalNode(contextNode));
			if (value != null) {
				preMatch.setNodeTarget(contextNode, value);
			}
		}
		return preMatch;
	}

	/**
	 * @param nodeToOriginalNodeMapping A mapping between the nodes of the extracted
	 *                                  NAC rules and the corresponding nodes from
	 *                                  the original rule
	 * @param pacs                      A mapping between the extracted PAC rules
	 *                                  and the context nodes of the PACs
	 * @param originalRuleMatch                     The match of the original rule
	 * @param graph                     The graph the pac should be matched on
	 * @return The matches for the PAC rules or null, if there have been no matches
	 *         for a rule
	 */
	Map<Rule, Iterator<Match>> getPACMatches(final List<ACRule> pacs, final Match originalRuleMatch, final EGraph graph) {
		final Object prev  = this.engine.getOptions().put(Engine.OPTION_CHECK_DANGLING, false);
		final Map<Rule, Iterator<Match>> matchIterators = new HashMap<>();
		for (final ACRule entry : pacs) {
			final Rule rule = entry.getRule();
			final Match preMatch = prepare(originalRuleMatch, entry);
			final Iterator<Match> iterator = this.engine.findMatches(rule, graph, preMatch).iterator();
			if (!iterator.hasNext()) {
				return null;
			}
			matchIterators.put(rule, iterator);
		}
		this.engine.getOptions().put(Engine.OPTION_CHECK_DANGLING, prev);
		return matchIterators;
	}

	public static Map<Rule, Collection<Match>> getAllMatches(final Map<Rule, Iterator<Match>> matchMap) {
		if (matchMap == null) {
			return null;
		}
		return matchMap.entrySet().parallelStream().collect(Collectors.toMap(Entry::getKey, v -> {
			final Collection<Match> matches = new LinkedList<>();
			final Iterator<Match> it = v.getValue();
			while (it.hasNext()) {
				matches.add(it.next());
			}
			return matches;
		}));
	}

	/**
	 * @return the nacs
	 */
	public List<NestedCondition> getNACs() {
		return this.nacs;
	}

	/**
	 * @return the pacs
	 */
	public List<NestedCondition> getPACs() {
		return this.pacs;
	}
}