package org.eclipse.emf.henshin.variability.multi;

import static org.eclipse.emf.henshin.model.Action.Type.PRESERVE;

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
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.impl.EdgeImpl;
import org.eclipse.emf.henshin.model.impl.MappingImpl;
import org.eclipse.emf.henshin.model.impl.NodeImpl;
import org.eclipse.emf.henshin.model.impl.RuleImpl;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher.VBMatchingInfo;
import org.eclipse.emf.henshin.variability.matcher.VBRuleInfo;
import org.eclipse.emf.henshin.variability.matcher.VBRulePreparator;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import aima.core.logic.propositional.kb.data.Clause;
import aima.core.logic.propositional.kb.data.Literal;
import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToCNF;

class MultiVarRuleUtil {

	private MultiVarRuleUtil() {
		// This class shouldn't be instantiated
	}

	public static Rule createPreserveRuleForAC(NestedCondition ac, Collection<Node> acContext, Map<Node,Node> acNodeToContextNode
			) {
		Graph graph = ac.getConclusion();
		String name = graph.getName();
		if (name == null) {
			name = ac.eContainer() instanceof Not ? "NAC" : "PAC";
		}
		List<Node> acNodes = graph.getNodes();

		List<Mapping> mappings = new ArrayList<>(acNodes.size());
		Map<Node, Node> lhsToRhs = new HashMap<>(acNodes.size());
		Map<Node, Node> nodeMapping = new HashMap<>(acNodes.size());
		for (Node acNode : acNodes) {
			// Create LHS
			Node lhsNode = new NodeImpl(acNode.getName(), acNode.getType());
			lhsNode.setAction(new Action(PRESERVE));
			Node contextNode = getContextNode(ac, acNode);
			if (contextNode != null) {
				acContext.add(lhsNode);
				acNodeToContextNode.put(lhsNode, contextNode);
			}
			nodeMapping.put(acNode, lhsNode);

			// Create RHS
			Node rhsNode = new NodeImpl(acNode.getName(), acNode.getType());
			lhsToRhs.put(lhsNode, rhsNode);

			// Create Mapping between LHS and RHS
			Mapping mapping = new MappingImpl();
			mapping.setOrigin(lhsNode);
			mapping.setImage(rhsNode);
			mappings.add(mapping);
		}

		Rule rule = new RuleImpl(name);
		rule.getLhs().getNodes().addAll(lhsToRhs.keySet());
		rule.getRhs().getNodes().addAll(lhsToRhs.values());
		rule.getMappings().addAll(mappings);

		for (Edge edge : graph.getEdges()) {
			Node src = nodeMapping.get(edge.getSource());
			Node trg = nodeMapping.get(edge.getTarget());
			EdgeImpl lhsEdge = new EdgeImpl(src, trg, edge.getType());
			lhsEdge.setAction(new Action(PRESERVE));
			rule.getLhs().getEdges().add(lhsEdge);
			rule.getRhs().getEdges().add(new EdgeImpl(lhsToRhs.get(src), lhsToRhs.get(trg), edge.getType()));
		}

		return rule;
	}

	/**
	 * @param ac
	 * @param acNode
	 * @return
	 */
	public static Node getContextNode(NestedCondition ac, Node acNode) {
		return ac.getMappings().stream().filter(mapping -> mapping.getImage().equals(acNode)).map(Mapping::getOrigin)
				.findAny().orElse(null);
	}

	public static boolean isContext(NestedCondition nac, Node node) {
		return nac.getMappings().stream().anyMatch(mapping -> mapping.getImage().equals(node));
	}

	static Iterable<VBMatch> flattenRuleAndMatch(EGraphImpl graphP, Rule rule) throws ScriptException {
		// Calculate allowed rule configurations
		LinkedList<List<String>> trueFeatureList = new LinkedList<>();
		LinkedList<List<String>> falseFeatureList = new LinkedList<>();

		VBRuleInfo ruleInfo = new VBRuleInfo(rule);
		calculateTrueAndFalseFeatures(rule, ruleInfo, trueFeatureList, falseFeatureList);

		LinkedList<VBMatch> matches = new LinkedList<>();
		MultiVarEngine engine = new MultiVarEngine();

		while (!trueFeatureList.isEmpty()) {
			List<String> trueFeatures = trueFeatureList.remove(0);
			List<String> falseFeatures = falseFeatureList.remove(0);

			Set<Sentence> elementsToRemove = calculateElementsToRemove(ruleInfo, trueFeatures, falseFeatures);

			// Flatten rule
			VBRulePreparator preparator = new VBRulePreparator(rule, trueFeatures, falseFeatures);
			VBMatchingInfo matchingInfo = new VBMatchingInfo(new ArrayList<>(ruleInfo.getExpressions().values()),
					ruleInfo, Collections.emptyList(), Collections.emptyList());
			BitSet reducedRule = preparator.prepare(ruleInfo, elementsToRemove, rule.isInjectiveMatching(), false,
					false);

			if (!matchingInfo.getMatchedSubrules().contains(reducedRule)) {

				// Find matches for flattened rule
				Iterator<Match> classicMatches = engine.findMatches(rule, graphP, null).iterator();

				while (classicMatches.hasNext()) {
					Match match = classicMatches.next();
					VBRulePreparator prep = preparator.getSnapShot();
					matches.add(new VBMatch(match, matchingInfo.getAssumedTrue(), rule, prep));
				}
				matchingInfo.getMatchedSubrules().add(reducedRule);

			}
			preparator.undo();
		}
		return matches;
	}

	/**
	 * Calculates all possible rule products and their feature configurations
	 *
	 * @param rule
	 * @param ruleInfo
	 * @param trueFeatureList
	 * @param falseFeatureList
	 * @return
	 */
	static Status calculateTrueAndFalseFeatures(Rule rule, VBRuleInfo ruleInfo, List<List<String>> trueFeatureList,
			List<List<String>> falseFeatureList) {
		// Line 5: calculate Phi_rule
		List<String> features = VariabilityHelper.INSTANCE.getFeatures(rule).stream().collect(Collectors.toList());

		// Line 6: get all Solutions for Phi_rule
		Sentence phiRule = ruleInfo.getFeatureModel();

		Map<Integer, String> symbolsToIndices = new HashMap<>();
		Sentence cnf;
		if (!ruleInfo.isFeatureConstraintCNF()) {
			cnf = phiRule;
		} else {
			cnf = ConvertToCNF.convert(phiRule);
		}
		ISolver solver = SatChecker.createModelIterator(cnf, symbolsToIndices);

		// Remove contained features
		int numFeatures = features.size();
		ArrayList<String> unusedFeatures = new ArrayList<>(numFeatures);
		for (String next : features) {
			if (!symbolsToIndices.containsValue(next)) {
				unusedFeatures.add(next);
				// } else {
				// features.remove(next);
			}
		}

		// Line 6: iterate over all Solutions of Phi_rule
		try {
			while (solver.isSatisfiable()) {
				int[] model = solver.model();
				List<String> tmpTrueFeatures = new LinkedList<>();
				List<String> tmpFalseFeatures = new LinkedList<>();
				for (int selection : model) {
					int abs = Math.abs(selection);
					if (selection > 0) {
						tmpTrueFeatures.add(symbolsToIndices.get(abs));
					} else {
						tmpFalseFeatures.add(symbolsToIndices.get(abs));
					}
				}
				if (unusedFeatures.isEmpty()) {
					trueFeatureList.add(tmpTrueFeatures);
					falseFeatureList.add(tmpFalseFeatures);
				} else {
					int bitVector = (int) Math.pow(2, features.size() - 1);
					while (bitVector >= 0) {
						LinkedList<String> trueFeatures = new LinkedList<>(tmpTrueFeatures);
						LinkedList<String> falseFeatures = new LinkedList<>(tmpFalseFeatures);
						for (int i = 0; i < features.size(); i++) {
							if (((1 << features.size() - i - 1 & bitVector) != 0)) {
								trueFeatures.add(unusedFeatures.get(i));
							} else {
								falseFeatures.add(unusedFeatures.get(i));
							}
						}
						trueFeatureList.add(trueFeatures);
						falseFeatureList.add(falseFeatures);
						bitVector--;
					}
				}

			}
		} catch (TimeoutException e1) {
			return Status.SATTimeout;
		}
		return Status.OK;
	}

	static Set<Sentence> calculateElementsToRemove(VBRuleInfo ruleInfo, List<String> trueFeatures,
			List<String> falseFeatures) throws ScriptException {
		// Line 7: get all Features set to false and remove false
		// Line 7: remove all pcs of rule evaluating to false
		Set<Sentence> elementsToRemove = new HashSet<>();
		for (Entry<Sentence, Set<GraphElement>> pcElementPair : ruleInfo.getPc2Elem().entrySet()) {
			String pc = pcElementPair.getKey().toString();
			// pcElementPair.getKey().toString().replaceAll(pattern,
			// "$2");
			Boolean reduced;
			if (pc.trim().equalsIgnoreCase(Logic.TRUE.trim())) {
				reduced = Boolean.TRUE;
			} else {
				reduced = Logic.evaluate(pc, trueFeatures, falseFeatures);
			}
			if (!reduced.booleanValue()) {
				elementsToRemove.add(pcElementPair.getKey());
			}
		}
		return elementsToRemove;
	}

	private static Map<PropositionSymbol, Integer> getSymbol2IndexMap(Set<PropositionSymbol> symbols) {
		Map<PropositionSymbol, Integer> list2Index = new HashMap<>(symbols.size());
		int counter = 1;
		for (PropositionSymbol symbol : symbols) {
			list2Index.put(symbol, counter);
			counter++;
		}
		return list2Index;
	}

	private static int[] convertToArray(Clause clause, Map<PropositionSymbol, Integer> indices) {
		Set<Literal> literals = clause.getLiterals();
		int[] result = new int[literals.size()];
		int counter = 0;
		for (Literal literal : literals) {
			int sign = literal.isPositiveLiteral() ? 1 : -1;
			PropositionSymbol symbol = literal.getAtomicSentence();
			int index = indices.get(symbol);
			result[counter] = sign * index;
			counter++;
		}
		return result;
	}
}
