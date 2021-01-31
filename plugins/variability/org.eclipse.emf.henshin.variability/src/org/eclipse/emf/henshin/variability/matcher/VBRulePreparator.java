package org.eclipse.emf.henshin.variability.matcher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.impl.EdgeImpl;
import org.eclipse.emf.henshin.model.impl.GraphImpl;
import org.eclipse.emf.henshin.model.impl.MappingImpl;
import org.eclipse.emf.henshin.model.impl.NodeImpl;
import org.eclipse.emf.henshin.model.impl.ParameterImpl;
import org.eclipse.emf.henshin.model.impl.RuleImpl;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToCNF;
import aima.core.logic.propositional.visitors.SymbolCollector;

public class VBRulePreparator {

	private final VBRuleInfo ruleInfo;
	private final PreparedVBRule baseRule;
	private final Collection<String> initiallyTrueFeatures;
	private final Collection<String> initiallyFalseFeatures;

	private final List<String> features;
	private final Map<BitSet, PreparedVBRule> ruleCache = new HashMap<>();

	private final SatChecker solver;
	private final Map<Sentence, SatChecker> solvers;

	public VBRulePreparator(final VBRuleInfo rule) throws ContradictionException {
		this(rule, Collections.emptyList(), Collections.emptyList());
	}

	public VBRulePreparator(final VBRuleInfo rule, final Collection<String> initiallyTrueFeatures,
			final Collection<String> initiallyFalseFeatures) throws ContradictionException {
		this.ruleInfo = rule;
		Sentence cnf = this.ruleInfo.getFeatureModel();
		if (!this.ruleInfo.isFeatureConstraintCNF()) {
			cnf = ConvertToCNF.convert(cnf);
		}
		this.solvers = new HashMap<>();
		this.solver = new SatChecker(cnf);
		this.solvers.put(cnf, this.solver);
		this.solvers.put(this.ruleInfo.getFeatureModel(), this.solver);
		this.features = new ArrayList<>(rule.getFeatures());
		this.initiallyTrueFeatures = Collections.unmodifiableCollection(initiallyTrueFeatures);
		this.initiallyFalseFeatures = Collections.unmodifiableCollection(initiallyFalseFeatures);
		this.baseRule = createRule(initiallyTrueFeatures, initiallyFalseFeatures);
	}

	private BitSet getBitSet(final Collection<String> trueFeatures, final Collection<String> falseFeatures) {
		final BitSet set = new BitSet(this.features.size());
		for (int i = 0; i < this.features.size(); i++) {
			final String feature = this.features.get(i);
			if (trueFeatures.contains(feature)) {
				set.set(i);
			} else if (falseFeatures.contains(feature)) {
				set.clear(i);
			} else {
				throw new IllegalArgumentException("The feature \"" + feature + "\" is unbound");
			}
		}
		return set;
	}

	public PreparedVBRule getRule(final Collection<String> trueFeatures, final Collection<String> falseFeatures) {
		final Set<String> allTrueFeatures = Stream
				.concat(this.initiallyTrueFeatures.parallelStream(), trueFeatures.parallelStream())
				.collect(Collectors.toSet());
		final Set<String> allFalseFeatures = Stream
				.concat(this.initiallyFalseFeatures.parallelStream(), falseFeatures.parallelStream())
				.collect(Collectors.toSet());
		final BitSet bs = getBitSet(allTrueFeatures, allFalseFeatures);
		PreparedVBRule rule = this.ruleCache.get(bs);
		if (rule != null) {
			return rule;
		}
		rule = createRule(allTrueFeatures, allFalseFeatures);
		this.ruleCache.put(bs, rule);
		return rule;
	}

	private PreparedVBRule createRule(final Collection<String> trueFeatures, final Collection<String> falseFeatures) {
		final Rule vbRule = this.ruleInfo.getRule();

		final LinkedList<Node> notClonedLhsNodes = new LinkedList<>();
		final LinkedList<Edge> notClonedLhsEdges = new LinkedList<>();
		final BiMap<Node, Node> lhsNodeMapOriginalToPrepared = cloneNonVariable(vbRule.getLhs(), this.ruleInfo,
				trueFeatures, falseFeatures, notClonedLhsNodes);
		final BiMap<Edge, Edge> lhsEdgeMapOriginalToPrepared = cloneNonVariable(vbRule.getLhs(),
				lhsNodeMapOriginalToPrepared, this.ruleInfo, trueFeatures, falseFeatures,notClonedLhsEdges);
		final Graph preparedLhs = new GraphImpl(vbRule.getLhs().getName());
		preparedLhs.getNodes().addAll(lhsNodeMapOriginalToPrepared.values());
		preparedLhs.getEdges().addAll(lhsEdgeMapOriginalToPrepared.values());

		final BiMap<Node, Node> rhsNodeMapOriginalToPrepared = cloneNonVariable(vbRule.getRhs(), this.ruleInfo,
				trueFeatures, falseFeatures, new LinkedList<>());
		final BiMap<Edge, Edge> rhsEdgeMapOriginalToPrepared = cloneNonVariable(vbRule.getRhs(),
				rhsNodeMapOriginalToPrepared, this.ruleInfo, trueFeatures, falseFeatures, new LinkedList<>());
		final Graph rhs = new GraphImpl(vbRule.getRhs().getName());
		rhs.getNodes().addAll(rhsNodeMapOriginalToPrepared.values());
		rhs.getEdges().addAll(rhsEdgeMapOriginalToPrepared.values());

		final Collection<Mapping> preparedMappings = vbRule.getMappings().parallelStream()
				.map((final Mapping mapping) -> {
					final Node origin = lhsNodeMapOriginalToPrepared.getValue(mapping.getOrigin());
					if (origin == null) {
						return null;
					}
					final Node image = rhsNodeMapOriginalToPrepared.getValue(mapping.getImage());
					if (image == null) {
						return null;
					}
					return new MappingImpl(origin, image);
				}).filter(Objects::nonNull).collect(Collectors.toList());

		final BiMap<Parameter, Parameter> paramMapOriginalToPrepared = new BiMap<>();
		for(final Parameter parameter: vbRule.getParameters()) {
			final Parameter clone = new ParameterImpl(parameter.getName(), parameter.getType());
			clone.setKind(parameter.getKind());
			paramMapOriginalToPrepared.put(parameter, clone);
		}

		final Rule preparedRule = new RuleImpl(vbRule.getName());
		preparedRule.setCheckDangling(vbRule.isCheckDangling());
		Boolean injective = Logic.evaluate(this.ruleInfo.getInjectiveMatching(), trueFeatures, falseFeatures);
		if (injective == null) {
			injective = this.ruleInfo.getRule().isInjectiveMatching();
		}
		preparedRule.setInjectiveMatching(injective);
		preparedRule.setLhs(preparedLhs);
		preparedRule.setRhs(rhs);
		preparedRule.getMappings().addAll(preparedMappings);
		preparedRule.getParameters().addAll(paramMapOriginalToPrepared.values());
		return new PreparedVBRule(this, preparedRule, trueFeatures, falseFeatures, lhsNodeMapOriginalToPrepared,
				lhsEdgeMapOriginalToPrepared, paramMapOriginalToPrepared, notClonedLhsNodes, notClonedLhsEdges);
	}

	private BiMap<Edge, Edge> cloneNonVariable(final Graph originalLhs,
			final BiMap<Node, Node> lhsNodeMapOriginalToPrepared, final VBRuleInfo ruleInfo,
			final Collection<String> trueFeatures, final Collection<String> falseFeatures, final Collection<Edge> notCloned) {
		final BiMap<Edge, Edge> map = new BiMap<>();
		for (final Edge edge : originalLhs.getEdges()) {
			final Sentence pc = ruleInfo.getPC(edge);
			try {
				if (isPresent(pc, trueFeatures, falseFeatures)
						&& lhsNodeMapOriginalToPrepared.containsKey(edge.getSource())
						&& lhsNodeMapOriginalToPrepared.containsKey(edge.getTarget())) {
					final Node src = lhsNodeMapOriginalToPrepared.getValue(edge.getSource());
					final Node trg = lhsNodeMapOriginalToPrepared.getValue(edge.getTarget());
					map.put(edge, new EdgeImpl(src, trg, edge.getType()));
				}
				else {
					notCloned.add(edge);
				}
			} catch (ContradictionException | TimeoutException e1) {
				e1.printStackTrace();
			}
		}
		return map;
	}

	private boolean isPresent(final Sentence pc, final Collection<String> trueFeatures,
			final Collection<String> falseFeatures) throws ContradictionException, TimeoutException {
		if (FeatureExpression.TRUE.equals(pc)) {
			return true;
		}
		if (trueFeatures.isEmpty() && falseFeatures.isEmpty()) {
			return false;
		}
		final boolean unboundVariables = SymbolCollector.getSymbolsFrom(pc).parallelStream().map(Object::toString)
				.noneMatch(f -> trueFeatures.contains(f) || falseFeatures.contains(f));
		if (unboundVariables) {
			return false;
		}
		SatChecker pcSolver = this.solvers.get(pc);
		if (pcSolver == null) {
			pcSolver = new SatChecker(pc);
			this.solvers.put(pc, pcSolver);
		}
		return pcSolver.isSatisfiable(trueFeatures, falseFeatures);
	}

	private BiMap<Node, Node> cloneNonVariable(final Graph graph, final VBRuleInfo ruleInfo,
			final Collection<String> trueFeatures, final Collection<String> falseFeatures, final Collection<Node> notCloned) {
		final BiMap<Node, Node> map = new BiMap<>();
		for (final Node node : graph.getNodes()) {
			final Sentence pc = ruleInfo.getPC(node);
			try {
				if (isPresent(pc, trueFeatures, falseFeatures)) {
					map.put(node, cloneNode(node, trueFeatures, falseFeatures));
				}
				else {
					notCloned.add(node);
				}
			} catch (ContradictionException | TimeoutException e) {
			}
		}
		return map;
	}

	private Node cloneNode(final Node node, final Collection<String> trueFeatures,
			final Collection<String> falseFeatures) {
		final Node baseNode = new NodeImpl(node.getName(), node.getType());
		for (final Attribute attribute : node.getAttributes()) {
			try {
				if (isPresent(this.ruleInfo.getPC(attribute), trueFeatures, falseFeatures)) {
					HenshinFactory.eINSTANCE.createAttribute(baseNode, attribute.getType(), attribute.getValue());
				}
			} catch (ContradictionException | TimeoutException e) {
				e.printStackTrace();
			}
		}
		return baseNode;
	}

	// static Set<Sentence> getNonTautologies(VBMatchingInfo matchingInfo) {
	// Set<Sentence> newImplicated = getNewImplicated(matchingInfo);
	// matchingInfo.setAll(newImplicated, null, true);
	//
	// Set<Sentence> result = new HashSet<>(matchingInfo.getNeutrals());
	// result.addAll(matchingInfo.getAssumedFalse());
	// return result;
	// }
	//
	// static Set<Sentence> getNewImplicated(VBMatchingInfo mo) {
	// Set<Sentence> result = new HashSet<>();
	// Sentence knowledge = getKnowledgeBase(mo);
	// for (Sentence e : mo.getNeutrals()) {
	// if (FeatureExpression.implies(knowledge, e)) {
	// result.add(e);
	// }
	// }
	// return result;
	// }
	//
	// static Sentence getKnowledgeBase(VBMatchingInfo mo) {
	// Sentence fe = FeatureExpression.TRUE;
	// for (Sentence t : mo.getAssumedTrue()) {
	// fe = FeatureExpression.and(fe, t);
	// }
	// for (Sentence f : mo.getAssumedFalse()) {
	// fe = FeatureExpression.andNot(fe, f);
	// }
	// return fe;
	// }

	public PreparedVBRule getBaseRule() {
		return this.baseRule;
	}

	public Collection<String> getInitiallyTrueFeatures() {
		return this.initiallyTrueFeatures;
	}

	public Collection<String> getInitiallyFalseFeatures() {
		return this.initiallyFalseFeatures;
	}

	public List<String> getFeatures() {
		return this.features;
	}

	public Collection<PreparedVBRule> prepareAllRules() {
		// Calculate all possible configurations of concrete rule
		final LinkedList<List<String>> trueFeatureList = new LinkedList<>();
		final LinkedList<List<String>> falseFeatureList = new LinkedList<>();
		this.solver.calculateTrueAndFalseFeatures(this.initiallyTrueFeatures, this.initiallyFalseFeatures,
				trueFeatureList, falseFeatureList, this.ruleInfo.getFeatures());

		// Iterate over all concrete rules and collect matches
		final Collection<PreparedVBRule> rules = new ArrayList<>(trueFeatureList.size());
		for (int i = 0; i < trueFeatureList.size(); i++) {
			final List<String> trueFeatures = trueFeatureList.get(i);
			final List<String> falseFeatures = falseFeatureList.get(i);

			rules.add(getRule(trueFeatures, falseFeatures));
		}
		return rules;
	}

	public Rule getVBRule() {
		return this.ruleInfo.getRule();
	}
}