package org.eclipse.emf.henshin.variability.util;

import static org.eclipse.emf.henshin.model.Action.Type.FORBID;
import static org.eclipse.emf.henshin.model.Action.Type.PRESERVE;
import static org.eclipse.emf.henshin.model.Action.Type.REQUIRE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.impl.EdgeImpl;
import org.eclipse.emf.henshin.model.impl.MappingImpl;
import org.eclipse.emf.henshin.model.impl.NodeImpl;
import org.eclipse.emf.henshin.model.impl.RuleImpl;
import org.eclipse.emf.henshin.variability.matcher.FeatureExpression;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.visitors.SymbolCollector;

/**
 * 
 * @author Daniel Strüber
 * @author Sven Peldszus
 *
 */
public class VBRuleUtil {

	public static void removeElementsFromRule(Rule rule, List<GraphElement> elementsToRemove) {
		Set<Node> nodes2delete = new HashSet<>();
		Set<Edge> edges2delete = new HashSet<>();
		Set<Attribute> attribs2delete = new HashSet<>();

		for (GraphElement o : elementsToRemove) {
			if (o instanceof Node) {
				Node n = (Node) o;
				nodes2delete.add(n);
				edges2delete.addAll(n.getIncoming());
				edges2delete.addAll(n.getOutgoing());
			} else if (o instanceof Edge)
				edges2delete.add((Edge) o);
			else if (o instanceof Attribute)
				attribs2delete.add((Attribute) o);
		}

		for (Attribute a : attribs2delete)
			rule.removeAttribute(a, true);
		for (Edge e : edges2delete)
			rule.removeEdge(e, true);
		for (Node n : nodes2delete)
			rule.removeNode(n, true);
	}

	public static boolean isVarRule(Unit unit) {
		return (unit instanceof Rule && VariabilityHelper.isVariabilityRule((Rule) unit));
	}

	public static boolean checkRule(Rule rule) {
		if (!isVarRule(rule)) {
			return true;
		}
		Set<String> features = VariabilityHelper.INSTANCE.getFeatures(rule);

		Stream<PropositionSymbol> fm = SymbolCollector
				.getSymbolsFrom(FeatureExpression.getExpr(VariabilityHelper.INSTANCE.getFeatureConstraint(rule)))
				.parallelStream();
		Stream<PropositionSymbol> symbols = SymbolCollector
				.getSymbolsFrom(FeatureExpression.getExpr(VariabilityHelper.INSTANCE.getPresenceCondition(rule)))
				.parallelStream();
		List<String> symbolNames = Stream.concat(fm, symbols).map(PropositionSymbol::getSymbol).distinct()
				.collect(Collectors.toList());

		return features.containsAll(symbolNames);
	}

}
