package org.eclipse.emf.henshin.variability.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

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
			} else if (o instanceof Edge) {
				edges2delete.add((Edge) o);
			} else if (o instanceof Attribute) {
				attribs2delete.add((Attribute) o);
			}
		}

		for (Attribute a : attribs2delete) {
			rule.removeAttribute(a, true);
		}
		for (Edge e : edges2delete) {
			rule.removeEdge(e, true);
		}
		for (Node n : nodes2delete) {
			rule.removeNode(n, true);
		}
	}

	public static boolean isVarRule(Unit unit) {
		return (unit instanceof Rule && VariabilityHelper.isVariabilityRule((Rule) unit));
	}

}
