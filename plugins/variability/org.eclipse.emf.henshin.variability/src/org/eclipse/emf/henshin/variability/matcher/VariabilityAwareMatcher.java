package org.eclipse.emf.henshin.variability.matcher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.util.RuleUtil;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 * Applies the algorithm described in [1] to determine all variability-aware
 * matches.
 * 
 * [1] <a href=
 * "https://www.uni-marburg.de/fb12/swt/forschung/publikationen/2015/SRCT15.pdf"
 * >Str�ber, Julia , Chechik, Taentzer (2015): A Variability-Based Approach to
 * Reusable and Efficient Model Transformations</a>.
 * 
 * @author Daniel Str�ber
 *
 */
public class VariabilityAwareMatcher {

	protected Rule rule;
	protected EGraph graph;
	protected EngineImpl engine;

	protected Map<String, Sentence> expressions;

	protected RuleInfo ruleInfo;
	protected RulePreparator rulePreparator;

	protected Collection<String> initiallyTrueFeatures;
	protected Collection<String> initiallyFalseFeatures;

	/**
	 * Variability-based matching needs to create a new matching engine for each
	 * base match. Hence, if the number of base matches is too great, performance
	 * will suffer due to the initialization effort.
	 */
	protected static final int THRESHOLD_MAXIMUM_BASE_MATCHES = 10;

	private static Map<Rule, RuleInfo> ruleInfoRegistry = new HashMap<>();

	/**
	 * Creates a new engine for the execution of a rule on a graph
	 * 
	 * @param rule  The rule to be executed
	 * @param graph The graph on which the rule should be executed
	 * @throws InconsistentRuleException If the rule is inconsistent
	 */
	public VariabilityAwareMatcher(Rule rule, EGraph graph) throws InconsistentRuleException {
		this(rule, graph, new ArrayList<>(), new ArrayList<>());
	}
	
	/**
	 * Creates a new engine for the execution of a rule on a graph
	 * 
	 * @param rule           The rule to be executed
	 * @param graph          The graph on which the rule should be executed
	 * @param initiallyTrue  All features set to 'true'
	 * @param initiallyFalse All features set to 'false'
	 * @throws InconsistentRuleException If the rule is inconsistent
	 */
	public VariabilityAwareMatcher(Rule rule, EGraph graph, Map<String, Boolean> configuration)
			throws InconsistentRuleException {
		this(rule, graph,
				configuration.entrySet().parallelStream().filter(Entry::getValue).map(Entry::getKey)
						.collect(Collectors.toSet()),
				configuration.entrySet().parallelStream().filter(e -> !e.getValue()).map(Entry::getKey)
						.collect(Collectors.toSet()));
	}

	/**
	 * Creates a new engine for the execution of a rule on a graph
	 * 
	 * @param rule           The rule to be executed
	 * @param graph          The graph on which the rule should be executed
	 * @param initiallyTrue  All features set to 'true'
	 * @param initiallyFalse All features set to 'false'
	 * @throws InconsistentRuleException If the rule is inconsistent
	 */
	public VariabilityAwareMatcher(Rule rule, EGraph graph, Collection<String> initiallyTrue,
			Collection<String> initiallyFalse) throws InconsistentRuleException {
		super();
		fixInconsistencies(rule);
		if (!RuleUtil.checkRule(rule)) {
			throw new InconsistentRuleException();
		}
		this.rule = rule;
		this.graph = graph;
		this.engine = new EngineImpl();
		this.rulePreparator = new RulePreparator(rule);

		this.initiallyTrueFeatures = initiallyTrue;
		this.initiallyFalseFeatures = initiallyFalse;

		if (!ruleInfoRegistry.containsKey(rule)) {
			ruleInfoRegistry.put(rule, new RuleInfo(rule));
		}
		this.ruleInfo = ruleInfoRegistry.get(rule);
		populateExpressionMap();
	}

	private void fixInconsistencies(Rule rule) {
		// Per definition, mapped nodes must have the same presence condition
		// in the LHS and the RHS.
		for (Mapping mapping : rule.getMappings()) {
			Node image = mapping.getImage();
			String originPresenceCondition = VariabilityHelper.INSTANCE.getPresenceCondition(mapping.getOrigin());
			if (!originPresenceCondition.equals(VariabilityHelper.INSTANCE.getPresenceCondition(image))) {
				VariabilityHelper.INSTANCE.setPresenceCondition(image, originPresenceCondition);
			}
		}
	}

	private void populateExpressionMap() {
		if (ruleInfoRegistry.containsKey(rule)) {
			expressions = ruleInfo.getExpressions();
		}
	}

	public Set<VariabilityAwareMatch> findMatches() {
		List<Sentence> conditions = new LinkedList<Sentence>();
		conditions.addAll(expressions.values());
		MatchingInfo mo = new MatchingInfo(conditions, ruleInfo, initiallyTrueFeatures, initiallyFalseFeatures);

		// Remove everything except for the base rule
		Set<Sentence> nonTauotologies = getNonTautologies(mo);
		rulePreparator.prepare(ruleInfo, nonTauotologies, rule.isInjectiveMatching(), true);

		Set<Match> baseMatches = new HashSet<Match>();
		Iterator<Match> it = engine.findMatches(rule, graph, null).iterator();
		while (it.hasNext()) {
			if (baseMatches.size() < THRESHOLD_MAXIMUM_BASE_MATCHES) {
				baseMatches.add(it.next());
			} else {
				baseMatches.clear();
				baseMatches.add(null);
				System.out.println("Too many base matches:" + rule);
				break;
			}
		}
		rulePreparator.undo();
//		System.err.println(System.currentTimeMillis()-time);

		Set<VariabilityAwareMatch> matches = new HashSet<VariabilityAwareMatch>();
		if (!baseMatches.isEmpty()) {
			mo.set(ruleInfo.getFeatureModel(), null, true);
			findMatches(rule, mo, baseMatches, matches);
			mo.set(ruleInfo.getFeatureModel(), true, null);
		}

		return matches;
	}

	private Set<Sentence> getNonTautologies(MatchingInfo matchingInfo) {
		Set<Sentence> newImplicated = getNewImplicated(matchingInfo);
		matchingInfo.setAll(newImplicated, null, true);

		Set<Sentence> result = new HashSet<Sentence>(matchingInfo.getNeutrals());
		result.addAll(matchingInfo.getAssumedFalse());
		return result;
	}

	private Set<VariabilityAwareMatch> findMatches(Rule rule, MatchingInfo matchingInfo, Set<Match> baseMatches,
			Set<VariabilityAwareMatch> matches) {
		Sentence current = getFirstNeutral(matchingInfo);
		if (current == null) {
			findMatchInner(rule, matchingInfo, baseMatches, matches);
		} else {
			matchingInfo.set(current, null, true);
			findMatchInner(rule, matchingInfo, baseMatches, matches);

			matchingInfo.set(current, true, false);
			findMatchInner(rule, matchingInfo, baseMatches, matches);

			matchingInfo.set(current, false, null);
		}
		return matches;
	}

	private Sentence getFirstNeutral(MatchingInfo matchingInfo) {
		Set<Sentence> contradictory = getNewContradictory(matchingInfo);
		for (Sentence e : matchingInfo.getInfo().keySet()) {
			if (matchingInfo.getInfo().get(e) == null && !contradictory.contains(e))
				return e;
		}
		return null;
	}

	private Set<VariabilityAwareMatch> findMatchInner(Rule rule, MatchingInfo matchingInfo, Set<Match> baseMatches,
			Set<VariabilityAwareMatch> matches) {
		Set<Sentence> newContradictory = getNewContradictory(matchingInfo);
		matchingInfo.setAll(newContradictory, null, false);

		Set<Sentence> newImplicated = getNewImplicated(matchingInfo);
		matchingInfo.setAll(newImplicated, null, true);

		// If there are no presence conditions contradicting or implied by the
		// current assignment (= neutral is empty), calculate the matches
		// classically.
		if (matchingInfo.getNeutrals().isEmpty()) {
			BitSet reducedRule = rulePreparator.prepare(ruleInfo, matchingInfo.getAssumedFalse(),
					determineInjectiveMatching(matchingInfo), false);
			// The following check ensures that we will not match the same
			// sub-rule twice.
			if (!matchingInfo.getMatchedSubrules().contains(reducedRule)) {
				for (Match bm : baseMatches) {
					Iterator<Match> classicMatches = engine.findMatches(rule, graph, bm).iterator();
					RulePreparator prep = rulePreparator.getSnapShot();
					while (classicMatches.hasNext()) {
						Match next = classicMatches.next();
						matches.add(new VariabilityAwareMatch(next, matchingInfo.getAssumedTrue(), rule, prep));
					}
				}
				matchingInfo.getMatchedSubrules().add(reducedRule);

			}
			rulePreparator.undo();

		}
		// Otherwise, analyse all of the remaining presence conditions,
		else {
			findMatches(rule, matchingInfo, baseMatches, matches);
		}

		// clean up
		matchingInfo.setAll(newImplicated, true, null);
		matchingInfo.setAll(newContradictory, false, null);
		return matches;
	}

	private boolean determineInjectiveMatching(MatchingInfo matchingInfo) {
		return (FeatureExpression.contradicts(ruleInfo.getInjectiveMatching(), getKnowledgeBase(matchingInfo)));
	}

	private Set<Sentence> getNewContradictory(MatchingInfo mo) {
		Set<Sentence> result = new HashSet<>();
		Sentence knowledge = getKnowledgeBase(mo);
		for (Sentence e : mo.getNeutrals())
			if (FeatureExpression.contradicts(knowledge, e))
				result.add(e);
		return result;
	}

	private Set<Sentence> getNewImplicated(MatchingInfo mo) {
		Set<Sentence> result = new HashSet<Sentence>();
		Sentence knowledge = getKnowledgeBase(mo);
		for (Sentence e : mo.getNeutrals())
			if (FeatureExpression.implies(knowledge, e))
				result.add(e);
		return result;
	}

	private Sentence getKnowledgeBase(MatchingInfo mo) {
		Sentence fe = FeatureExpression.TRUE;
		for (Sentence t : mo.getAssumedTrue()) {
			fe = FeatureExpression.and(fe, t);
		}
		for (Sentence f : mo.getAssumedFalse()) {
			fe = FeatureExpression.andNot(fe, f);
		}
		return fe;
	}

	public static class RuleInfo {
		Rule rule;
		Map<String, Sentence> usedExpressions;
		Map<Sentence, Set<GraphElement>> pc2elem;
		Map<Node, Set<Mapping>> node2Mapping;
		Sentence featureModel;
		Sentence injectiveMatching;

		public RuleInfo(Rule rule) {
			this.rule = rule;
			this.featureModel = FeatureExpression.getExpr(VariabilityHelper.INSTANCE.getFeatureModel(this.rule));
			String injective = VariabilityHelper.INSTANCE.getInjectiveMatchingPresenceCondition(this.rule);
			if (injective == null) {
				injective = Boolean.toString(rule.isInjectiveMatching());
			}
			this.injectiveMatching = FeatureExpression.getExpr(injective);

			populateMaps();
		}

		public Map<Sentence, Set<GraphElement>> getPc2Elem() {
			return pc2elem;
		}

		public Map<String, Sentence> getExpressions() {
			return usedExpressions;
		}

		public Sentence getFeatureModel() {
			return featureModel;
		}

		public void populateMaps() {
			usedExpressions = new HashMap<>();
			node2Mapping = new HashMap<>();
			pc2elem = new HashMap<>();
			TreeIterator<EObject> it = rule.eAllContents();
			while (it.hasNext()) {
				EObject o = it.next();
				if (o instanceof Node || o instanceof Edge || o instanceof Attribute) {
					String pc = VariabilityHelper.INSTANCE.getPresenceConditionIfModelElement((GraphElement) o);
					if (!RuleInfo.presenceConditionEmpty(pc)) {
						Sentence expr = FeatureExpression.getExpr(pc);
						usedExpressions.put(pc, expr);
						if (!pc2elem.containsKey(expr))
							pc2elem.put(expr, new HashSet<GraphElement>());
						pc2elem.get(expr).add((GraphElement) o);
					}
				}
				if (o instanceof Mapping) {
					Mapping m = (Mapping) o;

					Node image = m.getImage();
					Set<Mapping> set = node2Mapping.get(image);
					if (set == null) {
						set = new HashSet<Mapping>();
						node2Mapping.put(image, set);
					}
					set.add(m);
					Node origin = m.getOrigin();
					set = node2Mapping.get(origin);
					if (set == null) {
						set = new HashSet<>();
						node2Mapping.put(origin, set);
					}
					set.add(m);
				}
			}

			if (featureModel != null && !featureModel.equals("") 
					&& !pc2elem.containsKey(featureModel)) {
				pc2elem.put(featureModel, new HashSet<GraphElement>());
			}

		}

		public Map<Node, Set<Mapping>> getNode2Mapping() {
			return node2Mapping;
		}

		public Sentence getInjectiveMatching() {
			return injectiveMatching;
		}

		private static boolean presenceConditionEmpty(String presenceCondition) {
			return (presenceCondition == null) || presenceCondition.isEmpty();
		}

	}

	public static class MatchingInfo {
		private Map<Sentence, Boolean> info = new LinkedHashMap<>();
		private Set<Sentence> assumedTrue = new HashSet<>();
		private Set<Sentence> assumedFalse = new HashSet<>();
		private Set<Sentence> neutrals = new HashSet<>();
		private Set<BitSet> matchedSubRules = new HashSet<>();

		public MatchingInfo(Collection<Sentence> conditions, RuleInfo ruleInfo, Collection<String> initiallyTrue,
				Collection<String> initiallyFalse) {
			for (Sentence expr : conditions) {
				info.put(expr, null);
			}
			assumedTrue.add(ruleInfo.getFeatureModel());
			initiallyTrue.forEach(f -> assumedTrue.add(FeatureExpression.getExpr(f)));
			initiallyFalse.forEach(f -> assumedFalse.add(FeatureExpression.getExpr(f)));
			neutrals.addAll(conditions);
		}

		public Set<BitSet> getMatchedSubrules() {
			return matchedSubRules;
		}

		public void setAll(Collection<Sentence> exprs, Boolean old, Boolean new_) {
			for (Sentence expr : exprs) {
				set(expr, old, new_);
			}
		}

		private void set(Sentence expr, Boolean old, Boolean new_) {
			if (old == null) {
				neutrals.remove(expr);
			} else if (Boolean.TRUE.equals(old)) {
				assumedTrue.remove(expr);
			} else {
				assumedFalse.remove(expr);
			}

			if (new_ == null) {
				neutrals.add(expr);
			} else if (Boolean.TRUE.equals(new_)) {
				assumedTrue.add(expr);
			} else {
				assumedFalse.add(expr);
			}

			info.put(expr, new_);
		}

		public Set<Sentence> getAssumedTrue() {
			return assumedTrue;
		}

		public Set<Sentence> getAssumedFalse() {
			return assumedFalse;
		}

		public Map<Sentence, Boolean> getInfo() {
			return info;
		}

		public Set<Sentence> getNeutrals() {
			return neutrals;
		}
	}

}
