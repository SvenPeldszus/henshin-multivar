package org.eclipse.emf.henshin.variability.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.sat4j.specs.ContradictionException;

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
public class VBMatcher {

	protected EGraph graph;
	protected Engine engine;

	protected final VBRuleInfo ruleInfo;
	protected VBRulePreparator rulePreparator;

	protected final Collection<String> initiallyTrueFeatures;
	protected final Collection<String> initiallyFalseFeatures;
	protected final Rule rule;

	/**
	 * Variability-based matching needs to create a new matching engine for each
	 * base match. Hence, if the number of base matches is too great, performance
	 * will suffer due to the initialization effort.
	 */
	protected static final int THRESHOLD_MAXIMUM_BASE_MATCHES = -1;

	private static Map<Rule, VBRuleInfo> ruleInfoRegistry = new HashMap<>();

	/**
	 * Creates a new engine for the execution of a rule on a graph
	 *
	 * @param rule  The rule to be executed
	 * @param graph The graph on which the rule should be executed
	 * @throws InconsistentRuleException If the rule is inconsistent
	 */
	public VBMatcher(final Rule rule, final EGraph graph, final String... globalJavaImports) throws InconsistentRuleException {
		this(rule, graph, new ArrayList<>(), new ArrayList<>(), globalJavaImports);
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
	public VBMatcher(final Rule rule, final EGraph graph, final Map<String, Boolean> configuration) throws InconsistentRuleException {
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
	public VBMatcher(final Rule rule, final EGraph graph, final Collection<String> initiallyTrue, final Collection<String> initiallyFalse,
			final String... globalJavaImports) throws InconsistentRuleException {
		this.ruleInfo = getRuleInfo(rule);
		this.rule = rule;
		this.graph = graph;
		this.engine = new EngineImpl(globalJavaImports);
		try {
			this.rulePreparator = new VBRulePreparator(this.ruleInfo, initiallyTrue, initiallyFalse);
		} catch (final ContradictionException e) {
			throw new InconsistentRuleException();
		}

		this.initiallyTrueFeatures = initiallyTrue;
		this.initiallyFalseFeatures = initiallyFalse;
	}

	/**
	 * Returns a VBRuleInfo for the given rule. If there is a cached rule info, this
	 * one is used
	 *
	 * @param rule
	 * @return
	 * @throws InconsistentRuleException
	 */
	private VBRuleInfo getRuleInfo(final Rule rule) throws InconsistentRuleException {
		VBRuleInfo modifyableRuleInfo = ruleInfoRegistry.get(rule);
		if (modifyableRuleInfo != null) {
			return modifyableRuleInfo;
		}
		modifyableRuleInfo = new VBRuleInfo(rule);
		ruleInfoRegistry.put(rule, modifyableRuleInfo);
		return modifyableRuleInfo;
	}

	public Set<? extends VBMatch> findMatches() {
		final Set<Match> baseMatches = new HashSet<>();
		final Iterator<Match> it = this.engine.findMatches(this.rulePreparator.getBaseRule().getRule(), this.graph, null)
				.iterator();
		while (it.hasNext()) {
			if ((THRESHOLD_MAXIMUM_BASE_MATCHES < 0) || (baseMatches.size() < THRESHOLD_MAXIMUM_BASE_MATCHES)) {
				baseMatches.add(it.next());
			} else {
				baseMatches.clear();
				baseMatches.add(null);
				System.out.println("Too many base matches:" + this.rule);
				break;
			}
		}

		final Set<VBMatch> matches = new HashSet<>();
		if (!baseMatches.isEmpty()) {
			findProductMatches(baseMatches, matches);
		}

		return matches;
	}

	private void findProductMatches(final Set<Match> baseMatches, final Set<VBMatch> matches) {
		for (final PreparedVBRule preparedRule : this.rulePreparator.prepareAllRules()) {
			for (final Match baseMatch : baseMatches) {
				for (final Match match : this.engine.findMatches(preparedRule.getRule(), this.graph, preparedRule.getBaseMatch(baseMatch))) {
					matches.add(new VBMatch(match, preparedRule));
				}
			}
		}
	}

	//	private Set<VBMatch> findMatches(VBMatchingInfo matchingInfo, Set<Match> baseMatches,
	//			Set<VBMatch> matches) {
	//		Sentence current = getFirstNeutral(matchingInfo);
	//		if (current == null) {
	//			findMatchInner(matchingInfo, baseMatches, matches);
	//		} else {
	//			matchingInfo.set(current, null, true);
	//			findMatchInner(matchingInfo, baseMatches, matches);
	//
	//			matchingInfo.set(current, true, false);
	//			findMatchInner(matchingInfo, baseMatches, matches);
	//
	//			matchingInfo.set(current, false, null);
	//		}
	//		return matches;
	//	}

	//	private static Sentence getFirstNeutral(VBMatchingInfo matchingInfo) {
	//		Set<Sentence> contradictory = getNewContradictory(matchingInfo);
	//		for (Sentence e : matchingInfo.getInfo().keySet()) {
	//			if (matchingInfo.getInfo().get(e) == null && !contradictory.contains(e)) {
	//				return e;
	//			}
	//		}
	//		return null;
	//	}

	//	private Set<VBMatch> findMatchInner(VBMatchingInfo matchingInfo, Set<Match> baseMatches,
	//			Set<VBMatch> matches) {
	//		Set<Sentence> newContradictory = getNewContradictory(matchingInfo);
	//		matchingInfo.setAll(newContradictory, null, false);
	//
	//		Set<Sentence> newImplicated = VBRulePreparator.getNewImplicated(matchingInfo);
	//		matchingInfo.setAll(newImplicated, null, true);
	//
	//		// If there are no presence conditions contradicting or implied by the
	//		// current assignment (= neutral is empty), calculate the matches
	//		// classically.
	//		if (matchingInfo.getNeutrals().isEmpty()) {
	//			BitSet reducedRule = this.rulePreparator.prepare(matchingInfo.getAssumedFalse(),
	//					determineInjectiveMatching(matchingInfo), false, false);
	//			// The following check ensures that we will not match the same
	//			// sub-rule twice.
	//			if (!matchingInfo.getMatchedSubrules().contains(reducedRule)) {
	//				for (Match bm : baseMatches) {
	//					Iterator<Match> classicMatches = this.engine.findMatches(this.rule, this.graph, bm).iterator();
	//					OldRulePreparator prep = this.rulePreparator.getSnapShot();
	//					while (classicMatches.hasNext()) {
	//						Match next = classicMatches.next();
	//						matches.add(new VBMatch(next, this.rule, prep));
	//					}
	//				}
	//				matchingInfo.getMatchedSubrules().add(reducedRule);
	//
	//			}
	////			this.rulePreparator.undo();
	//
	//		}
	//		// Otherwise, analyse all of the remaining presence conditions,
	//		else {
	//			findMatches(matchingInfo, baseMatches, matches);
	//		}
	//
	//		// clean up
	//		matchingInfo.setAll(newImplicated, true, null);
	//		matchingInfo.setAll(newContradictory, false, null);
	//		return matches;
	//	}

	//	private boolean determineInjectiveMatching(VBMatchingInfo matchingInfo) {
	//		return !(FeatureExpression.contradicts(this.ruleInfo.getInjectiveMatching(),
	//				VBRulePreparator.getKnowledgeBase(matchingInfo)));
	//	}
	//
	//	private static Set<Sentence> getNewContradictory(VBMatchingInfo mo) {
	//		Set<Sentence> result = new HashSet<>();
	//		Sentence knowledge = VBRulePreparator.getKnowledgeBase(mo);
	//		for (Sentence e : mo.getNeutrals()) {
	//			if (FeatureExpression.contradicts(knowledge, e)) {
	//				result.add(e);
	//			}
	//		}
	//		return result;
	//	}

	//	public static class VBMatchingInfo {
	//		private final Map<Sentence, Boolean> info = new LinkedHashMap<>();
	//		private final Set<Sentence> assumedTrue = new HashSet<>();
	//		private final Set<Sentence> assumedFalse = new HashSet<>();
	//		private final Set<Sentence> neutrals = new HashSet<>();
	//		private final Set<BitSet> matchedSubRules = new HashSet<>();
	//
	//		public VBMatchingInfo(Collection<Sentence> conditions, VBRuleInfo ruleInfo, Collection<String> initiallyTrue,
	//				Collection<String> initiallyFalse) {
	//			for (Sentence expr : conditions) {
	//				this.info.put(expr, null);
	//			}
	//			this.assumedTrue.add(ruleInfo.getFeatureModel());
	//			initiallyTrue.forEach(f -> this.assumedTrue.add(FeatureExpression.getExpr(f)));
	//			initiallyFalse.forEach(f -> this.assumedFalse.add(FeatureExpression.getExpr(f)));
	//			this.neutrals.addAll(conditions);
	//		}
	//
	//		public Set<BitSet> getMatchedSubrules() {
	//			return this.matchedSubRules;
	//		}
	//
	//		public void setAll(Iterable<Sentence> exprs, Boolean old, Boolean new_) {
	//			for (Sentence expr : exprs) {
	//				set(expr, old, new_);
	//			}
	//		}
	//
	//		private void set(Sentence expr, Boolean old, Boolean new_) {
	//			if (old == null) {
	//				this.neutrals.remove(expr);
	//			} else if (Boolean.TRUE.equals(old)) {
	//				this.assumedTrue.remove(expr);
	//			} else {
	//				this.assumedFalse.remove(expr);
	//			}
	//
	//			if (new_ == null) {
	//				this.neutrals.add(expr);
	//			} else if (Boolean.TRUE.equals(new_)) {
	//				this.assumedTrue.add(expr);
	//			} else {
	//				this.assumedFalse.add(expr);
	//			}
	//
	//			this.info.put(expr, new_);
	//		}
	//
	//		public Set<Sentence> getAssumedTrue() {
	//			return this.assumedTrue;
	//		}
	//
	//		public Set<Sentence> getAssumedFalse() {
	//			return this.assumedFalse;
	//		}
	//
	//		public Map<Sentence, Boolean> getInfo() {
	//			return this.info;
	//		}
	//
	//		public Set<Sentence> getNeutrals() {
	//			return this.neutrals;
	//		}
	//	}

}
