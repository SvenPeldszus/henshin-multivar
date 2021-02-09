package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.matcher.PreparedVBRule;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;

import aima.core.logic.propositional.parsing.ast.Sentence;

public class MultiVarMatcher extends VBMatcher {

	private final Lifting lifting;
	// private Map<Node, Node> baseNodesOriginalToPrepares;
	private final ApplicationConditionMatcher acMatcher;

	public MultiVarMatcher(final Rule rule, final MultiVarEGraph graphP, final MultiVarEngine varEngine)
			throws InconsistentRuleException {
		super(rule, graphP, varEngine.getGlobalJavaImports());
		this.engine.getOptions().putAll(varEngine.getOptions());
		this.acMatcher = new ApplicationConditionMatcher(this.engine, rule);
		this.lifting = new Lifting(graphP);
	}

	@Override
	public Set<MultiVarMatch> findMatches() {
		final Set<MultiVarMatch> matches = new HashSet<>();

		// Line 1: findBasePreMatches
		final Set<Match> baseMatches = findBasePreMatches();
		if (baseMatches.isEmpty()) {
			return Collections.emptySet();
		}

		final Collection<BaseMatch> liftableBaseMatches = findLiftableBaseMatches(baseMatches);
		if (liftableBaseMatches.isEmpty()) {
			return Collections.emptySet();
		}

		// Prepare the rule preparators
		final Collection<PreparedVBRule> preparators = this.rulePreparator.prepareAllRules();

		for (final PreparedVBRule prep : preparators) {

			// Create the rules for NACs and PACs
			final List<ACRule> nacRules = this.acMatcher
					.createACRules(getNotRejectedConditions(prep, this.acMatcher.getNACs()))
					.map(acr -> acr.prepare(prep)).collect(Collectors.toList());
			final List<ACRule> pacRules = this.acMatcher
					.createACRules(getNotRejectedConditions(prep, this.acMatcher.getPACs()))
					.map(acr -> acr.prepare(prep)).collect(Collectors.toList());

			for (final BaseMatch basePreMatch : liftableBaseMatches) {
				// but remove base NAC rules that didn't match as they cannot match with more
				// context and do not have to be matched again
				final List<ACRule> filteredNACs = nacRules.parallelStream()
						.filter(acr -> !basePreMatch.getNotMatchingNACs().contains(acr.getRule()))
						.collect(Collectors.toList());

				// Line 8: Get and collect matches for concrete rule
				matches.addAll(extendAndLiftBaseMatches(prep, basePreMatch.getMatch(), filteredNACs, pacRules));

			}

			// Undo preparation
			pacRules.forEach(ACRule::restore);
			nacRules.forEach(ACRule::restore);
		}
		return matches;
	}

	private Collection<MultiVarMatch> extendAndLiftBaseMatches(final PreparedVBRule preparator,
			final Match basePreMatch, final List<ACRule> nacRules, final List<ACRule> pacRules) {
		if (!checkVariableEdgesAndAttributesWithinBasePart(basePreMatch)) {
			return Collections.emptySet();
		}
		final Collection<MultiVarMatch> matches = new LinkedList<>();
		final Iterator<Match> classicMatches = this.engine
				.findMatches(preparator.getRule(), this.graph, preparator.getMatchOnPreparedRule(basePreMatch))
				.iterator();
		while (classicMatches.hasNext()) {
			final Match nextMatch = classicMatches.next();
			final MultiVarMatch liftedMatch = liftMatch(nextMatch, nacRules, pacRules, preparator);
			if (liftedMatch != null) {
				matches.add(liftedMatch);
			}
		}
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
	private MultiVarMatch liftMatch(final Match match, final List<ACRule> nacRules, final List<ACRule> pacRules,
			final PreparedVBRule preparator) {
		// if (!checkVariableExtensionsOfBasePart(match)) {
		// return null;
		// }
		final Match originalRuleMatch = preparator.getMatchOnOriginalRule(match);
		final Map<Rule, Iterator<Match>> pacMatchMap = this.acMatcher.getPACMatches(pacRules, originalRuleMatch,
				this.graph);
		if (pacMatchMap == null) {
			return null;
		}
		final Map<Rule, Collection<Match>> nacMatchMap = this.acMatcher.getNACMatches(nacRules, originalRuleMatch,
				this.graph);

		return getLifting().liftMatch(new MultiVarMatch(match, preparator,
				ApplicationConditionMatcher.getAllMatches(pacMatchMap), nacMatchMap));
	}

	/**
	 * Matches the NACs and PACs of the base rule and checks if the base match can
	 * be lifted
	 *
	 * @param basePreMatches The matches of the VB rule's LHS
	 * @return The liftable base matches mapped to the matches of the base rule's
	 *         NACs
	 */
	private Collection<BaseMatch> findLiftableBaseMatches(final Set<Match> basePreMatches) {
		final PreparedVBRule baseRule = this.rulePreparator.getBaseRule();

		final List<ACRule> baseNacRules = this.acMatcher.createACRules(getBaseNACs()).map(r -> r.prepare(baseRule))
				.collect(Collectors.toList());
		final List<ACRule> basePacRules = this.acMatcher.createACRules(getBasePACs()).map(r -> r.prepare(baseRule))
				.collect(Collectors.toList());

		// Line 2: iterate over all base-matches
		final Collection<BaseMatch> liftableBaseMatches = new LinkedList<>();
		for (final Match basePreMatch : basePreMatches) {
			boolean isLiftAble = true;
			Map<Rule, Collection<Match>> nacMatches;

			// If there is no base-match the rule is liftable in all cases
			// (optimization for too many base-matches)
			if (basePreMatch != null) {
				nacMatches = this.acMatcher.getNACMatches(baseNacRules, basePreMatch, this.graph);

				// Line 3: calculate Phi_apply and AND FM from Line 4
				final Map<Rule, Iterator<Match>> pacMatches = this.acMatcher.getPACMatches(basePacRules, basePreMatch,
						this.graph);
				if (pacMatches == null) {
					continue;
				}
				final Sentence phiApply = getLifting().calculatePhiApply(basePreMatch, nacMatches,
						Collections.emptyMap());

				// Line 4: check if Phi_apply & FM is SAT
				isLiftAble = SatChecker.isCNFSatisfiable(phiApply);
			} else {
				nacMatches = Collections.emptyMap();
			}

			if (isLiftAble) {
				liftableBaseMatches.add(new BaseMatch(basePreMatch, nacMatches));
			}
		}

		baseNacRules.forEach(ACRule::restore);
		basePacRules.forEach(ACRule::restore);
		return liftableBaseMatches;
	}

	/**
	 * @param prep
	 * @param conditions
	 * @return
	 */
	private Collection<NestedCondition> getNotRejectedConditions(final PreparedVBRule prep,
			final Collection<NestedCondition> conditions) {
		return conditions.parallelStream().filter(ac -> {
			final Sentence pc = getPC(ac);
			if (pc == null) {
				return true;
			}
			return Logic.evaluate(pc, prep.getTrueFeatures(), prep.getFalseFeatures());
		}).collect(Collectors.toList());
	}

	private Collection<NestedCondition> getBasePACs() {
		final PreparedVBRule prepared = this.rulePreparator.getBaseRule();
		final List<NestedCondition> baseNACs = new LinkedList<>();
		Stream<Node> vbNodes = prepared.getRemovedBaseRuleNodes().parallelStream();
		for (final NestedCondition nac : this.acMatcher.getPACs()) {
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
	 * @return
	 */
	private List<NestedCondition> getBaseNACs() {
		final PreparedVBRule prepared = this.rulePreparator.getBaseRule();
		final List<NestedCondition> baseNACs = new LinkedList<>();
		Stream<Node> vbNodes = prepared.getRemovedBaseRuleNodes().parallelStream();
		for (final NestedCondition nac : this.acMatcher.getNACs()) {
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
	private boolean hasNoPC(final NestedCondition nc) {
		final Sentence pc = getPC(nc);
		if (pc == null) {
			return true;
		}
		return FeatureExpression.TRUE.equals(pc);
	}

	/**
	 * @param nc
	 * @return
	 */
	private Sentence getPC(final NestedCondition nc) {
		return nc.getConclusion().getNodes().stream().map(this.ruleInfo::getPC).filter(Objects::nonNull).findAny()
				.orElse(null);
	}

	private boolean checkVariableExtensionsOfBasePart(final Match match) {
		final Collection<Node> baseNodes = this.rulePreparator.getBaseRule().getPreservedBaseRuleNodes();
		for (final Node node : baseNodes) {
			final EObject src = match.getNodeTarget(node);
			for (final Edge edge : node.getOutgoing()) {
				// Check edges pointing to variable nodes
				if (!baseNodes.contains(edge.getTarget())) {
					final EObject expect = match.getNodeTarget(edge.getTarget());
					final EReference type = edge.getType();
					final Object is = src.eGet(type);
					if (type.getUpperBound() == 1) {
						if (expect != is) {
							return false;
						}
					} else if (!((Collection<?>) is).contains(expect)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * @param basePreMatch
	 */
	private boolean checkVariableEdgesAndAttributesWithinBasePart(final Match basePreMatch) {
		if (basePreMatch == null) {
			// Nothing to check
			return true;
		}
		final Collection<Node> baseNodes = this.rulePreparator.getBaseRule().getPreservedBaseRuleNodes();
		for (final Node node : baseNodes) {
			final EObject src = basePreMatch.getNodeTarget(node);
			for (final Edge edge : node.getOutgoing()) {
				// Check variable edges pointing to base nodes
				if (baseNodes.contains(edge.getTarget()) && (this.ruleInfo.getPC(edge) != null)) {
					final EObject expect = basePreMatch.getNodeTarget(edge.getTarget());
					final EReference type = edge.getType();
					final Object is = src.eGet(type);
					if (type.getUpperBound() == 1) {
						if (expect != is) {
							return false;
						}
					} else if (!((Collection<?>) is).contains(expect)) {
						return false;
					}
				}
			}
			for (final Attribute attribute : node.getAttributes()) {
				String expectedValue = attribute.getValue();
				if ((expectedValue.charAt(0) == '"') && (expectedValue.charAt(expectedValue.length() - 1) == '"')) {
					expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
				} else {
					final Object parameterValue = basePreMatch.getParameterValue(this.rule.getParameter(expectedValue));
					expectedValue = parameterValue != null ? parameterValue.toString() : null;
				}
				if (expectedValue != null) {
					// Applies if an attribute has a pc
					final Object tmp = src.eGet(attribute.getType());
					final String isValue = tmp != null ? tmp.toString() : null;
					if (!expectedValue.equals(isValue)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Finds matches for the base part of the VB rule's LHS
	 *
	 * @return The base matches using the nodes of the original VB rule or a set
	 *         only containing null, iff there are too many base matches
	 */
	private Set<Match> findBasePreMatches() {
		final PreparedVBRule baseRule = this.rulePreparator.getBaseRule();
		final Set<Match> baseMatches = new HashSet<>();
		for (final Match match : this.engine.findMatches(baseRule.getRule(), this.graph, null)) {
			if ((THRESHOLD_MAXIMUM_BASE_MATCHES < 0) || (baseMatches.size() < THRESHOLD_MAXIMUM_BASE_MATCHES)) {
				baseMatches.add(baseRule.getMatchOnOriginalRule(match));
			} else {
				baseMatches.clear();
				baseMatches.add(null);
				System.out.println("Too many base matches:" + this.rule);
				break;
			}
		}
		return baseMatches;
	}

	/**
	 * @return the lifting
	 */
	public Lifting getLifting() {
		return this.lifting;
	}
}
