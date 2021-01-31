package org.eclipse.emf.henshin.variability.util;

import java.util.Collection;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.matcher.VBRuleInfo;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;
import aima.core.logic.propositional.parsing.ast.ComplexSentence;
import aima.core.logic.propositional.parsing.ast.Connective;
import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToCNF;

/**
 *
 * @author Daniel Strüber
 * @author Sven Peldszus
 *
 */
public class VBRuleUtil {

	private VBRuleUtil() {
		// This class shouldn't be instantiated
	}

//	public static void removeElementsFromRule(Rule rule, List<GraphElement> elementsToRemove) {
//		Set<Node> nodes2delete = new HashSet<>();
//		Set<Edge> edges2delete = new HashSet<>();
//		Set<Attribute> attribs2delete = new HashSet<>();
//
//		for (GraphElement o : elementsToRemove) {
//			if (o instanceof Node) {
//				Node n = (Node) o;
//				nodes2delete.add(n);
//				edges2delete.addAll(n.getIncoming());
//				edges2delete.addAll(n.getOutgoing());
//			} else if (o instanceof Edge) {
//				edges2delete.add((Edge) o);
//			} else if (o instanceof Attribute) {
//				attribs2delete.add((Attribute) o);
//			}
//		}
//
//		for (Attribute a : attribs2delete) {
//			rule.removeAttribute(a, true);
//		}
//		for (Edge e : edges2delete) {
//			rule.removeEdge(e, true);
//		}
//		for (Node n : nodes2delete) {
//			rule.removeNode(n, true);
//		}
//	}

	private static Sentence buildCNF(VBRuleInfo ruleInfo, Collection<String> intitiallyTrue,
			Collection<String> intitiallyFalse) {
		Sentence phiRule = ruleInfo.getFeatureModel();

		Sentence cnf;
		if (ruleInfo.isFeatureConstraintCNF()) {
			cnf = phiRule;
		} else {
			cnf = ConvertToCNF.convert(phiRule);
		}
		for (String feature : intitiallyTrue) {
			cnf = new ComplexSentence(Connective.AND, cnf, new PropositionSymbol(feature));
		}
		for (String feature : intitiallyFalse) {
			cnf = new ComplexSentence(Connective.AND, cnf,
					new ComplexSentence(Connective.NOT, new PropositionSymbol(feature)));
		}
		return cnf;
	}

//	public static Set<Sentence> calculateElementsToRemove(VBRuleInfo ruleInfo, List<String> trueFeatures,
//			List<String> falseFeatures) throws ScriptException {
//		// Line 7: get all Features set to false and remove false
//		// Line 7: remove all pcs of rule evaluating to false
//		Set<Sentence> elementsToRemove = new HashSet<>();
//		for (Entry<Sentence, Set<GraphElement>> pcElementPair : ruleInfo.getPc2ElementEntrySet()) {
//			String pc = pcElementPair.getKey().toString();
//			// pcElementPair.getKey().toString().replaceAll(pattern,
//			// "$2");
//			Boolean reduced;
//			if (pc.trim().equalsIgnoreCase(Logic.TRUE.trim())) {
//				reduced = Boolean.TRUE;
//			} else {
//				reduced = Logic.evaluate(pc, trueFeatures, falseFeatures);
//			}
//			if (!reduced.booleanValue()) {
//				elementsToRemove.add(pcElementPair.getKey());
//			}
//		}
//		return elementsToRemove;
//	}
//
//	private static Map<PropositionSymbol, Integer> getSymbol2IndexMap(Set<PropositionSymbol> symbols) {
//		Map<PropositionSymbol, Integer> list2Index = new HashMap<>(symbols.size());
//		int counter = 1;
//		for (PropositionSymbol symbol : symbols) {
//			list2Index.put(symbol, counter);
//			counter++;
//		}
//		return list2Index;
//	}
//
//	private static int[] convertToArray(Clause clause, Map<PropositionSymbol, Integer> indices) {
//		Set<Literal> literals = clause.getLiterals();
//		int[] result = new int[literals.size()];
//		int counter = 0;
//		for (Literal literal : literals) {
//			int sign = literal.isPositiveLiteral() ? 1 : -1;
//			PropositionSymbol symbol = literal.getAtomicSentence();
//			int index = indices.get(symbol);
//			result[counter] = sign * index;
//			counter++;
//		}
//		return result;
//	}

	public static boolean isVarRule(Unit unit) {
		return (unit instanceof Rule && VariabilityHelper.isVariabilityRule((Rule) unit));
	}

}
