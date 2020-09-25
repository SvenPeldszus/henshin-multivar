package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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

	private final Map<NestedCondition, Object[]> acCache;
	private final List<NestedCondition> nacs;
	private final List<NestedCondition> pacs;
	private final Map<Node, Node> nodeMapping;
	private final Engine engine;

	public ApplicationConditionMatcher(Engine engine, Rule rule) {
		this.engine = engine;
		this.nacs = new LinkedList<>();
		this.pacs = new LinkedList<>();
		this.acCache = new ConcurrentHashMap<>();
		this.nodeMapping = new HashMap<>();
		fillACs(rule);
	}

	/**
	 * Searches all PACs and NACs in the rule and adds them to the lists of PACs and
	 * NACs
	 *
	 * @param rule
	 */
	private void fillACs(Rule rule) {
		Deque<Formula> formulas = new LinkedList<>();
		formulas.add(rule.getLhs().getFormula());
		while (!formulas.isEmpty()) {
			Formula next = formulas.pop();
			if (next instanceof Not) {
				this.getNACs().add((NestedCondition) ((Not) next).getChild());
			} else if (next instanceof And) {
				formulas.push(((And) next).getLeft());
				formulas.push(((And) next).getRight());
			} else if (next instanceof NestedCondition) {
				this.getPACs().add((NestedCondition) next);
			}
		}
	}

	/**
	 * @param rulePreparator The rule preparator
	 * @param nodeMapping    A map to which a mapping from the nodes of the new
	 *                       rules to the nodes of original rule should be added
	 * @param trueFeatures   The feature conditions of the rule that evaluate to
	 *                       true
	 * @return A map of the crated rules and their context nodes
	 */
	@SuppressWarnings("unchecked")
	Map<Rule, List<Node>> createACRules(Collection<NestedCondition> acs) {
		Map<Rule, List<Node>> map = new HashMap<>();
		for (NestedCondition ac : acs) {
			Object[] entry = this.acCache.computeIfAbsent(ac, key -> {
				List<Node> context = new LinkedList<>();
				Rule nacRule = MultiVarRuleUtil.createPreserveRuleForAC(key, context, this.nodeMapping);
				return new Object[] { nacRule, context };
			});
			map.put((Rule) entry[0], (List<Node>) entry[1]);
		}
		return map;
	}

	/**
	 * @param nodeMapping A mapping between the nodes of the extracted NAC rules and
	 *                    the corresponding nodes from the original rule
	 * @param nacs        A mapping between the extracted NAC rules and the context
	 *                    nodes of the NACs
	 * @param match       The match of the original rule
	 * @param graph
	 * @return The matches for the NAC rules or null, if there have been no matches
	 *         for a rule
	 */
	Map<Rule, List<Match>> getNACMatches(Map<Rule, List<Node>> nacs, Match match, EGraph graph) {
		Map<Rule, List<Match>> matches = new HashMap<>();
		for (Entry<Rule, List<Node>> entry : nacs.entrySet()) {
			Match preMatch = new MatchImpl(entry.getKey());
			for (Node contextNode : entry.getValue()) {
				EObject value = match.getNodeTarget(this.nodeMapping.get(contextNode));
				preMatch.setNodeTarget(contextNode, value);
			}
			List<Match> nacMatches = new LinkedList<>();
			this.engine.findMatches(entry.getKey(), graph, preMatch).forEach(nacMatches::add);
			matches.put(entry.getKey(), nacMatches);

		}
		return matches;
	}

	/**
	 * @param nodeMapping A mapping between the nodes of the extracted NAC rules and
	 *                    the corresponding nodes from the original rule
	 * @param pacs        A mapping between the extracted PAC rules and the context
	 *                    nodes of the PACs
	 * @param match       The match of the original rule
	 * @param graph       The graph the pac should be matched on
	 * @return The matches for the PAC rules or null, if there have been no matches
	 *         for a rule
	 */
	Map<Rule, List<Match>> getPACMatches(Map<Rule, List<Node>> pacs, Match match, EGraph graph) {
		Map<Rule, List<Match>> pacMatchMap = new HashMap<>();
		for (Entry<Rule, List<Node>> entry : pacs.entrySet()) {
			Match preMatch = new MatchImpl(entry.getKey());
			for (Node contextNode : entry.getValue()) {
				EObject value = match.getNodeTarget(this.nodeMapping.get(contextNode));
				preMatch.setNodeTarget(contextNode, value);
			}
			List<Match> pacMatches = new LinkedList<>();
			this.engine.findMatches(entry.getKey(), graph, preMatch).forEach(pacMatches::add);
			if (pacMatches.isEmpty()) {
				return null;
			} else {
				pacMatchMap.put(entry.getKey(), pacMatches);
			}
		}
		return pacMatchMap;
	}

	/**
	 * @return the nacs
	 */
	public List<NestedCondition> getNACs() {
		return nacs;
	}

	/**
	 * @return the pacs
	 */
	public List<NestedCondition> getPACs() {
		return pacs;
	}
}