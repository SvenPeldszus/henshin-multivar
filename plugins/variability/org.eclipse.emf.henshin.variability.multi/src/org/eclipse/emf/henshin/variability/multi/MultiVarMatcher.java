package org.eclipse.emf.henshin.variability.multi;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptException;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.matcher.VBRulePreparator;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;

import aima.core.logic.propositional.parsing.ast.Sentence;

public class MultiVarMatcher extends VBMatcher {

	private final Lifting lifting;
	private List<Node> baseNodes;
	private final ApplicationConditionMatcher acMatcher;

	public MultiVarMatcher(Rule rule, MultiVarEGraph graphP, MultiVarEngine varEngine)
			throws InconsistentRuleException {
		super(rule, graphP);
		this.engine = new EngineImpl(varEngine.getGlobalJavaImports());
		this.engine.getOptions().putAll(varEngine.getOptions());
		this.acMatcher = new ApplicationConditionMatcher(this.engine, rule);
		this.lifting = new Lifting(graphP);
	}

	@Override
	public Set<MultiVarMatch> findMatches() {
		Set<MultiVarMatch> matches = new HashSet<>();

		// Line 1: findBasePreMatches
		Set<Match> baseMatches = findBasePreMatches();
		if (baseMatches.isEmpty()) {
			return Collections.emptySet();
		}

		Map<Match, Map<Rule, List<Match>>> liftableBaseMatches = findLiftableBaseMatches(baseMatches);
		if (liftableBaseMatches.isEmpty()) {
			return Collections.emptySet();
		}

		// Prepare the rule preparators
		List<VBRulePreparator> preparators;
		try {
			preparators = prepareRulePreparators();
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}

		for (VBRulePreparator prep : preparators) {
			for (Entry<Match, Map<Rule, List<Match>>> baseMatchEntry : liftableBaseMatches.entrySet()) {
				Match basePreMatch = baseMatchEntry.getKey();

				// Create the rules for NACs and PACs
				Map<Rule, List<Node>> nacRules = this.acMatcher.createACRules(getNotRejectedConditions(prep, this.acMatcher.getNACs()));
				for (Entry<Rule, List<Match>> entry : baseMatchEntry.getValue().entrySet()) {
					if (entry.getValue().isEmpty()) {
						nacRules.remove(entry.getKey());
					}
				}
				Map<Rule, List<Node>> pacRules = this.acMatcher.createACRules(getNotRejectedConditions(prep, this.acMatcher.getPACs()));

				// Line 8: Get and collect matches for concrete rule
				matches.addAll(extendAndLiftBaseMatches(prep, basePreMatch, nacRules, pacRules));

			}

		}
		return matches;
	}

	private Collection<MultiVarMatch> extendAndLiftBaseMatches(VBRulePreparator preparator, Match basePreMatch,
			Map<Rule, List<Node>> nacRules, Map<Rule, List<Node>> pacRules) {
		preparator.doPreparation();

		if (!checkVariableEdgesAndAttributesWithinBasePart(basePreMatch)) {
			preparator.undo();
			return Collections.emptySet();
		}
		Collection<MultiVarMatch> matches = new LinkedList<>();
		Iterator<Match> classicMatches = this.engine.findMatches(this.rule, this.graph, basePreMatch).iterator();
		while (classicMatches.hasNext()) {
			Match nextMatch = classicMatches.next();
			MultiVarMatch liftedMatch = liftMatch(nextMatch, nacRules, pacRules, preparator);
			if (liftedMatch != null) {
				matches.add(liftedMatch);
			}
		}
		preparator.undo();
		return matches;
	}

	/**
	 * Lifts the match if possible
	 *
	 * @param match      The match to lift
	 * @param nacRules   The NACs to lift
	 * @param pacRules   The PACs to lift
	 * @param preparator The rule preparator of the rule to lift
	 * @return The lifted match or null if the match is not liftable
	 */
	private MultiVarMatch liftMatch(Match match, Map<Rule, List<Node>> nacRules, Map<Rule, List<Node>> pacRules,
			VBRulePreparator preparator) {
		if (!checkVariableExtensionsOfBasePart(match)) {
			return null;
		}

		Map<Rule, List<Match>> pacMatchMap = this.acMatcher.getPACMatches(pacRules, match, this.graph);
		if (pacMatchMap == null) {
			return null;
		}
		Map<Rule, List<Match>> nacMatchMap = this.acMatcher.getNACMatches(nacRules, match, this.graph);

		return this.getLifting().liftMatch(new MultiVarMatch(match, this.rule, preparator, pacMatchMap, nacMatchMap));
	}

	/**
	 * @param baseMatches
	 * @param acMatcher.nodeMapping
	 * @param baseNacRules
	 * @param basePACRules
	 * @return
	 */
	private Map<Match, Map<Rule, List<Match>>> findLiftableBaseMatches(Set<Match> baseMatches) {
		Map<Rule, List<Node>> baseNacRules = this.acMatcher.createACRules(getBaseNACs());
		Map<Rule, List<Node>> basePACRules = this.acMatcher.createACRules(getBasePACs());

		// Line 2: iterate over all base-matches
		Map<Match, Map<Rule, List<Match>>> liftableBaseMatches = new HashMap<>(baseMatches.size());
		for (Match basePreMatch : baseMatches) {
			boolean isLiftAble = true;
			Map<Rule, List<Match>> nacMatches;

			// If there is no base-match the rule is liftable in all cases
			// (optimization for too many base-matches)
			if (basePreMatch != null) {
				nacMatches = this.acMatcher.getNACMatches(baseNacRules, basePreMatch, this.graph);

				// Line 3: calculate Phi_apply and AND FM from Line 4
				String phiApply = this.getLifting().calculatePhiApply(basePreMatch, nacMatches,
						this.acMatcher.getPACMatches(basePACRules, basePreMatch, this.graph));

				// Line 4: check if Phi_apply & FM is SAT
				SatChecker satChecker = new SatChecker();
				isLiftAble = satChecker.isSatisfiable(phiApply);
			} else {
				nacMatches = Collections.emptyMap();
			}

			if (isLiftAble) {
				liftableBaseMatches.put(basePreMatch, nacMatches);
			}
		}
		return liftableBaseMatches;
	}

	/**
	 * @param prep
	 * @param conditions
	 * @return
	 */
	private Collection<NestedCondition> getNotRejectedConditions(VBRulePreparator prep,
			Collection<NestedCondition> conditions) {
		return conditions.parallelStream().filter(ac -> {
			String pc = getPC(ac);
			if (pc == null) {
				return true;
			}
			try {
				return Logic.evaluate(pc, prep.getTrueFeatures(), prep.getFalseFeatures());
			} catch (ScriptException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());
	}

	private Collection<NestedCondition> getBasePACs() {
		return this.acMatcher.getPACs().parallelStream().filter(this::hasNoPC).collect(Collectors.toList());
	}

	/**
	 * @return
	 */
	private List<NestedCondition> getBaseNACs() {
		List<NestedCondition> baseNACs = new LinkedList<>();
		Stream<Node> vbNodes = this.rulePreparator.removeNodes.parallelStream();
		for (NestedCondition nac : this.acMatcher.getNACs()) {
			if (hasNoPC(nac)) {
				baseNACs.add(nac);
			} else {
				vbNodes = Stream.concat(vbNodes, nac.getConclusion().getNodes().parallelStream()
						.filter(node -> !MultiVarRuleUtil.isContext(nac, node)));
			}
		}
		final Set<EClass> vbTypes = vbNodes.map(Node::getType).collect(Collectors.toSet());
		return baseNACs.parallelStream()
				.filter(nac -> nac.getConclusion().getNodes().parallelStream()
						.filter(node -> !MultiVarRuleUtil.isContext(nac, node))
						.noneMatch(node -> vbTypes.contains(node.getType())))
				.collect(Collectors.toList());
	}

	/**
	 * @param nc
	 * @return
	 */
	private boolean hasNoPC(NestedCondition nc) {
		String pc = getPC(nc);
		if (pc == null) {
			return true;
		}
		pc = pc.trim();
		return pc.isEmpty() || Logic.TRUE.equals(pc);
	}

	/**
	 * @param nc
	 * @return
	 */
	private String getPC(NestedCondition nc) {
		return nc.getConclusion().getNodes().stream().map(this.ruleInfo::getPC).filter(Objects::nonNull)
				.filter(value -> !value.isEmpty()).findAny().orElse(null);
	}

	private boolean checkVariableExtensionsOfBasePart(Match match) {
		for (Node node : this.baseNodes) {
			EObject src = match.getNodeTarget(node);
			for (Edge edge : node.getOutgoing()) {
				// Check edges pointing to variable nodes
				if (!this.baseNodes.contains(edge.getTarget())) {
					EObject expect = match.getNodeTarget(edge.getTarget());
					EReference type = edge.getType();
					Object is = src.eGet(type);
					if (type.getUpperBound() == 1) {
						if (expect != is) {
							return false;
						}
					} else {
						if (!((Collection<?>) is).contains(expect)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * @param basePreMatch
	 */
	private boolean checkVariableEdgesAndAttributesWithinBasePart(Match basePreMatch) {
		if (basePreMatch == null) {
			// Nothing to check
			return true;
		}
		for (Node node : this.baseNodes) {
			EObject src = basePreMatch.getNodeTarget(node);
			for (Edge edge : node.getOutgoing()) {
				// Check variable edges pointing to base nodes
				if (this.baseNodes.contains(edge.getTarget()) && this.ruleInfo.getPC(edge) != null) {
					EObject expect = basePreMatch.getNodeTarget(edge.getTarget());
					EReference type = edge.getType();
					Object is = src.eGet(type);
					if (type.getUpperBound() == 1) {
						if (expect != is) {
							return false;
						}
					} else {
						if (!((Collection<?>) is).contains(expect)) {
							return false;
						}
					}
				}
			}
			for (Attribute attribute : node.getAttributes()) {
				String expectedValue = attribute.getValue();
				if (expectedValue.charAt(0) == '"' && expectedValue.charAt(expectedValue.length() - 1) == '"') {
					expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
				} else {
					Object parameterValue = basePreMatch.getParameterValue(this.rule.getParameter(expectedValue));
					expectedValue = parameterValue != null ? parameterValue.toString() : null;
				}
				if (expectedValue != null) {
					// Applies if an attribute has a pc
					Object tmp = src.eGet(attribute.getType());
					String isValue = tmp != null ? tmp.toString() : null;
					if (!expectedValue.equals(isValue)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private List<VBRulePreparator> prepareRulePreparators() throws ScriptException {
		// Calculate all possible configurations of concrete rule
		LinkedList<List<String>> trueFeatureList = new LinkedList<>();
		LinkedList<List<String>> falseFeatureList = new LinkedList<>();
		MultiVarRuleUtil.calculateTrueAndFalseFeatures(this.ruleInfo, trueFeatureList, falseFeatureList);

		// Iterate over all concrete rules and collect matches
		List<VBRulePreparator> preparators = new ArrayList<>(trueFeatureList.size());
		for (int i = 0; i < trueFeatureList.size(); i++) {
			List<String> trueFeatures = trueFeatureList.get(i);
			List<String> falseFeatures = falseFeatureList.get(i);

			// Generate concrete rule
			Set<Sentence> elementsToRemove = MultiVarRuleUtil.calculateElementsToRemove(this.ruleInfo, trueFeatures,
					falseFeatures);
			VBMatchingInfo matchingInfo = new VBMatchingInfo(new ArrayList<>(this.expressions.values()), this.ruleInfo,
					Collections.emptyList(), Collections.emptyList());
			VBRulePreparator preparator = new VBRulePreparator(this.ruleInfo, trueFeatures, falseFeatures);
			BitSet reducedRule = preparator.prepare(elementsToRemove, this.rule.isInjectiveMatching(), false, false);
			if (!matchingInfo.getMatchedSubrules().contains(reducedRule)) {
				preparators.add(preparator.getSnapShot());
				matchingInfo.getMatchedSubrules().add(reducedRule);

			}
			preparator.undo();
		}
		return preparators;
	}

	private Set<Match> findBasePreMatches() {
		this.rulePreparator.prepare(this.ruleInfo.getPc2Elem().keySet(), this.rule.isInjectiveMatching(), true, false);
		this.baseNodes = new ArrayList<>(this.rule.getLhs().getNodes());
		Set<Match> baseMatches = new HashSet<>();
		Iterator<Match> it = this.engine.findMatches(this.rule, this.graph, null).iterator();
		while (it.hasNext()) {
			if (baseMatches.size() < THRESHOLD_MAXIMUM_BASE_MATCHES) {
				baseMatches.add(it.next());
			} else {
				baseMatches.clear();
				baseMatches.add(null);
				System.out.println("Too many base matches:" + this.rule);
				break;
			}
		}
		this.rulePreparator.undo();
		return baseMatches;
	}

	/**
	 * @return the lifting
	 */
	public Lifting getLifting() {
		return lifting;
	}
}
