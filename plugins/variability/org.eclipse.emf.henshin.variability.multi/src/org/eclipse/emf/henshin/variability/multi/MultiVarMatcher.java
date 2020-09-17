package org.eclipse.emf.henshin.variability.multi;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptException;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.matcher.VBRulePreparator;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.Sentence;

public class MultiVarMatcher extends VBMatcher {

	private final Lifting lifting;
	private final Map<NestedCondition, Object[]> acCache;
	private final List<NestedCondition> nacs;
	private final List<NestedCondition> pacs;

	private List<Node> baseNodes;

	public MultiVarMatcher(Rule rule, MultiVarEGraph graphP, MultiVarEngine varEngine)
			throws InconsistentRuleException {
		super(rule, graphP);
		this.nacs = new LinkedList<>();
		this.pacs = new LinkedList<>();
		Deque<Formula> formulas = new LinkedList<>();
		formulas.add(rule.getLhs().getFormula());
		while (!formulas.isEmpty()) {
			Formula next = formulas.pop();
			if (next instanceof Not) {
				this.nacs.add((NestedCondition) ((Not) next).getChild());
			} else if (next instanceof And) {
				formulas.push(((And) next).getLeft());
				formulas.push(((And) next).getRight());
			} else if (next instanceof NestedCondition) {
				this.pacs.add((NestedCondition) next);
			}
		}
		this.engine = new EngineImpl(varEngine.getGlobalJavaImports());
		this.engine.getOptions().putAll(varEngine.getOptions());
		this.acCache = new ConcurrentHashMap<>();
		this.lifting = new Lifting(varEngine, graphP);
	}

	public Collection<Change> transform() {
		return liftAndAppy(findMatches());
	}

	public Collection<Change> liftAndAppy(Iterable<MultiVarMatch> matches) {
		Collection<Change> changes = new LinkedList<>();
		for (MultiVarMatch match : matches) {
			match.prepareRule();
			changes.add(this.lifting.liftAndApplyRule(match, this.rule));
			match.undoPreparation();
		}
		return changes;
	}

	@Override
	public Set<MultiVarMatch> findMatches() {

		Set<MultiVarMatch> matches = new HashSet<>();

		// Line 1: findBasePreMatches
		Set<Match> baseMatches = findBasePreMatches();

		// Create rules for the base PACs and NACs
		Map<Node, Node> nodeMapping = new HashMap<>();
		Map<Rule, List<Node>> baseNacRules = createACRules(getBaseNACs(), nodeMapping);
		Map<Rule, List<Node>> basePACRules = createACRules(getBasePACs(), nodeMapping);

		List<VBRulePreparator> preparators = null;
		List<Set<Sentence>> assumedTrue = null;

		// Line 2: iterate over all base-matches
		for (Match basePreMatch : baseMatches) {
			boolean isLiftAble = true;
			Map<Rule, List<Match>> nacMatches;

			// If there is no base-match the rule is liftable in all cases
			// (optimization for too many base-matches)
			if (basePreMatch != null) {
				// Line 3: calculate Phi_apply and AND FM from Line 4

				String phiApply = this.lifting.calculatePhiApply(basePreMatch,
						nacMatches = getNACMatches(nodeMapping, baseNacRules, basePreMatch),
						getPACMatches(nodeMapping, basePACRules, basePreMatch));

				// Line 4: check if Phi_apply & FM is SAT
				SatChecker satChecker = new SatChecker();
				isLiftAble = satChecker.isSatisfiable(phiApply);
			} else {
				nacMatches = Collections.emptyMap();
			}

			if (isLiftAble) {
				if (preparators == null) {
					preparators = new ArrayList<>();
					assumedTrue = new ArrayList<>();
					try {
						prepareRulePreparators(preparators, assumedTrue);
					} catch (ScriptException e) {
						throw new RuntimeException(e);
					}
				}

				for (int i = 0; i < preparators.size(); i++) {
					VBRulePreparator prep = preparators.get(i);
					Set<Sentence> theTrue = assumedTrue.get(i);

					Map<Rule, List<Node>> nacRules = createACRules(getNotRejectedConditions(prep, this.nacs),
							nodeMapping);
					for(Entry<Rule, List<Match>> entry : nacMatches.entrySet()) {
						if(entry.getValue().isEmpty()) {
							nacRules.remove(entry.getKey());
						}
					}
					Map<Rule, List<Node>> pacRules = createACRules(getNotRejectedConditions(prep, this.pacs),
							nodeMapping);

					// Line 8: Get and collect matches for concrete rule
					prep.doPreparation();

					if (!checkBasePart(basePreMatch)) {
						prep.undo();
						continue;
					}

					Iterator<Match> classicMatches = this.engine.findMatches(this.rule, this.graph, basePreMatch)
							.iterator();
					while (classicMatches.hasNext()) {
						Match nextMatch = classicMatches.next();
						if (!checkVariableBasePart(nextMatch)) {
							continue;
						}

						Map<Rule, List<Match>> pacMatchMap = getPACMatches(nodeMapping, pacRules, nextMatch);
						if (pacMatchMap == null) {
							continue;
						}
						Map<Rule, List<Match>> nacMatchMap = getNACMatches(nodeMapping, nacRules, nextMatch);

						MultiVarMatch liftedMatch = this.lifting.liftMatch(
								new MultiVarMatch(nextMatch, theTrue, this.rule, prep, pacMatchMap, nacMatchMap));
						if (liftedMatch != null) {
							matches.add(liftedMatch);
						}
					}
					prep.undo();

				}

			}
		}
		return matches;
	}

	/**
	 * @param prep
	 * @param conditions
	 * @return
	 */
	private Collection<NestedCondition> getNotRejectedConditions(VBRulePreparator prep,
			Collection<NestedCondition> conditions) {
		List<NestedCondition> collect = conditions.parallelStream().filter(ac -> {
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
		return collect;
	}

	private Collection<NestedCondition> getBasePACs() {
		return this.pacs.parallelStream().filter(this::hasNoPC).collect(Collectors.toList());
	}

	/**
	 * @return
	 */
	private List<NestedCondition> getBaseNACs() {
		List<NestedCondition> baseNACs = new LinkedList<>();
		Stream<Node> vbNodes = this.rulePreparator.removeNodes.parallelStream();
		for (NestedCondition nac : this.nacs) {
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
		return nc.getConclusion().getNodes().stream().map(VariabilityHelper.INSTANCE::getPresenceCondition)
				.filter(Objects::nonNull).filter(value -> !value.isEmpty()).findAny().orElse(null);
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
	private Map<Rule, List<Node>> createACRules(Collection<NestedCondition> acs, Map<Node, Node> nodeMapping) {
		Map<Rule, List<Node>> map = new HashMap<>();
		for (NestedCondition ac : acs) {
			Object[] entry = this.acCache.computeIfAbsent(ac, key -> {
				List<Node> context = new LinkedList<>();
				Rule nacRule = MultiVarRuleUtil.createPreserveRuleForAC(key, context, nodeMapping);
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
	 * @return The matches for the NAC rules or null, if there have been no matches
	 *         for a rule
	 */
	private Map<Rule, List<Match>> getNACMatches(Map<Node, Node> nodeMapping, Map<Rule, List<Node>> nacs, Match match) {
		Map<Rule, List<Match>> matches = new HashMap<>();
		for (Entry<Rule, List<Node>> entry : nacs.entrySet()) {
			Match preMatch = new MatchImpl(entry.getKey());
			for (Node contextNode : entry.getValue()) {
				EObject value = match.getNodeTarget(nodeMapping.get(contextNode));
				preMatch.setNodeTarget(contextNode, value);
			}
			List<Match> nacMatches = new LinkedList<>();
			this.engine.findMatches(entry.getKey(), this.graph, preMatch).forEach(nacMatches::add);
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
	 * @return The matches for the PAC rules or null, if there have been no matches
	 *         for a rule
	 */
	private Map<Rule, List<Match>> getPACMatches(Map<Node, Node> nodeMapping, Map<Rule, List<Node>> pacs, Match match) {
		Map<Rule, List<Match>> pacMatchMap = new HashMap<>();
		for (Entry<Rule, List<Node>> entry : pacs.entrySet()) {
			Match preMatch = new MatchImpl(entry.getKey());
			for (Node contextNode : entry.getValue()) {
				EObject value = match.getNodeTarget(nodeMapping.get(contextNode));
				preMatch.setNodeTarget(contextNode, value);
			}
			List<Match> pacMatches = new LinkedList<>();
			this.engine.findMatches(entry.getKey(), this.graph, preMatch).forEach(pacMatches::add);
			if (pacMatches.isEmpty()) {
				return null;
			} else {
				pacMatchMap.put(entry.getKey(), pacMatches);
			}
		}
		return pacMatchMap;
	}

	private boolean checkVariableBasePart(Match match) {
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
	private boolean checkBasePart(Match basePreMatch) {
		if (basePreMatch == null) {
			// Nothing to check
			return true;
		}
		for (Node node : this.baseNodes) {
			EObject src = basePreMatch.getNodeTarget(node);
			for (Edge edge : node.getOutgoing()) {
				// Check variable edges pointing to base nodes
				if (this.baseNodes.contains(edge.getTarget())) { // TODO: Check if is baseEdge
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

	public void prepareRulePreparators(List<VBRulePreparator> preparators, List<Set<Sentence>> assumedTrue)
			throws ScriptException {

		// Calculate all possible configurations of concrete rule
		LinkedList<List<String>> trueFeatureList = new LinkedList<>();
		LinkedList<List<String>> falseFeatureList = new LinkedList<>();
		MultiVarRuleUtil.calculateTrueAndFalseFeatures(this.rule, this.ruleInfo, trueFeatureList, falseFeatureList);

		// Iterate over all concrete rules and collect matches
		for (int i = 0; i < trueFeatureList.size(); i++) {
			List<String> trueFeatures = trueFeatureList.get(i);
			List<String> falseFeatures = falseFeatureList.get(i);

			// Generate concrete rule
			Set<Sentence> elementsToRemove = MultiVarRuleUtil.calculateElementsToRemove(this.ruleInfo, trueFeatures,
					falseFeatures);
			VBMatchingInfo matchingInfo = new VBMatchingInfo(new ArrayList<>(this.expressions.values()), this.ruleInfo,
					Collections.emptyList(), Collections.emptyList());
			VBRulePreparator preparator = new VBRulePreparator(this.rule, trueFeatures, falseFeatures);
			BitSet reducedRule = preparator.prepare(this.ruleInfo, elementsToRemove, this.rule.isInjectiveMatching(),
					false, false);
			if (!matchingInfo.getMatchedSubrules().contains(reducedRule)) {

				VBRulePreparator prep = preparator.getSnapShot();
				Set<Sentence> theTrue = matchingInfo.getAssumedTrue();
				preparators.add(prep);
				assumedTrue.add(theTrue);
				matchingInfo.getMatchedSubrules().add(reducedRule);

			}
			preparator.undo();
		}
	}

	private Set<Match> findBasePreMatches() {
		BitSet bs = this.rulePreparator.prepare(this.ruleInfo, this.ruleInfo.getPc2Elem().keySet(),
				this.rule.isInjectiveMatching(), true, false);
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
}
