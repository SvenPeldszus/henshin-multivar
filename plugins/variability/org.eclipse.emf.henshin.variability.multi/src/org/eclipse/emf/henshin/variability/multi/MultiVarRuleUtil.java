package org.eclipse.emf.henshin.variability.multi;

import static org.eclipse.emf.henshin.model.Action.Type.PRESERVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.impl.EdgeImpl;
import org.eclipse.emf.henshin.model.impl.MappingImpl;
import org.eclipse.emf.henshin.model.impl.NodeImpl;
import org.eclipse.emf.henshin.model.impl.RuleImpl;

public class MultiVarRuleUtil {

	private MultiVarRuleUtil() {
		// This class shouldn't be instantiated
	}

	public static ACRule createPreserveRuleForAC(final NestedCondition condition) {
		final Graph conditionGraph = condition.getConclusion();
		String name = conditionGraph.getName();
		if (name == null) {
			name = condition.eContainer() instanceof Not ? "NAC" : "PAC";
		}
		final List<Node> conditionNodes = conditionGraph.getNodes();

		final Map<Node, Node> conditionNodeToOriginalNode = buildConditionNodeToOriginalNodeMap(condition);
		final Map<Edge, Edge> conditionEdgeToOriginalEdge = buildConditionEdgeToOriginalEdgeMap(conditionGraph,
				conditionNodeToOriginalNode);

		final List<Mapping> mappings = new ArrayList<>(conditionNodes.size());
		final Map<Node, Node> lhsToRhs = new HashMap<>(conditionNodes.size());
		final Map<Node, Node> allAcNodeToConditionNode = new HashMap<>(conditionNodes.size());
		final Map<Node, Node> acNodeToOriginalNode = new HashMap<>(conditionNodes.size());
		final Map<Node, Node> originalNodeToAcNode = new HashMap<>(conditionNodes.size());

		for (final Node conditionNode : conditionNodes) {
			// Create LHS
			final Node lhsNode = new NodeImpl(conditionNode.getName(), conditionNode.getType());
			lhsNode.setAction(new Action(PRESERVE));

			final Node contextNode = conditionNodeToOriginalNode.get(conditionNode);
			if (contextNode != null) {
				acNodeToOriginalNode.put(lhsNode, contextNode);
				originalNodeToAcNode.put(contextNode, lhsNode);
			}
			allAcNodeToConditionNode.put(conditionNode, lhsNode);

			// Create RHS
			final Node rhsNode = new NodeImpl(conditionNode.getName(), conditionNode.getType());
			lhsToRhs.put(lhsNode, rhsNode);

			// Create Mapping between LHS and RHS
			final Mapping mapping = new MappingImpl();
			mapping.setOrigin(lhsNode);
			mapping.setImage(rhsNode);
			mappings.add(mapping);

			// Copy attributes
			for (final Attribute attribute : conditionNode.getAttributes()) {
				if ((contextNode != null) && (contextNode.getAttribute(attribute.getType()) != null)) {
					continue;
				}
				HenshinFactory.eINSTANCE.createAttribute(lhsNode, attribute.getType(), attribute.getValue());
				HenshinFactory.eINSTANCE.createAttribute(rhsNode, attribute.getType(), attribute.getValue());
			}
		}

		final Rule rule = new RuleImpl(name);
		rule.getLhs().getNodes().addAll(allAcNodeToConditionNode.values());
		rule.getRhs().getNodes().addAll(lhsToRhs.values());
		rule.getMappings().addAll(mappings);

		final Map<Edge, Edge> acEdgeToOriginalEdge = new HashMap<>();
		final Map<Edge, Edge> originalEdgeTAcEdge = new HashMap<>();
		for (final Edge conditionEdge : conditionGraph.getEdges()) {
			final Node src = allAcNodeToConditionNode.get(conditionEdge.getSource());
			final Node trg = allAcNodeToConditionNode.get(conditionEdge.getTarget());
			final EdgeImpl lhsEdge = new EdgeImpl(src, trg, conditionEdge.getType());
			lhsEdge.setAction(new Action(PRESERVE));
			rule.getLhs().getEdges().add(lhsEdge);
			// rule.getRhs().getEdges().add(new EdgeImpl(lhsToRhs.get(src),
			// lhsToRhs.get(trg), edge.getType()));
			final Edge originalEdge = conditionEdgeToOriginalEdge.get(conditionEdge);
			if (originalEdge != null) {
				acEdgeToOriginalEdge.put(lhsEdge, originalEdge);
				originalEdgeTAcEdge.put(originalEdge, lhsEdge);
			}
		}
		return new ACRule(rule, acNodeToOriginalNode, originalNodeToAcNode, acEdgeToOriginalEdge, originalEdgeTAcEdge);
	}

	private static Map<Node, Node> buildConditionNodeToOriginalNodeMap(final NestedCondition condition) {
		return condition.getMappings().parallelStream()
				.collect(Collectors.toMap(Mapping::getImage, Mapping::getOrigin));
	}

	private static Map<Edge, Edge> buildConditionEdgeToOriginalEdgeMap(final Graph conditionGraph,
			final Map<Node, Node> conditionNodeToOriginalNode) {
		return conditionGraph.getEdges().parallelStream().map(e -> {
			final Node s = conditionNodeToOriginalNode.get(e.getSource());
			final Node t = conditionNodeToOriginalNode.get(e.getTarget());
			if ((s == null) || (t == null)) {
				return null;
			}
			final Optional<Edge> match = s.getOutgoing(e.getType()).parallelStream()
					.filter(ex -> t.equals(ex.getTarget())).findAny();
			if (match.isPresent()) {
				return new Edge[] { e, match.get() };
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(k -> k[0], v -> v[1]));
	}

	public static boolean isContext(final NestedCondition nac, final Node node) {
		return nac.getMappings().stream().anyMatch(mapping -> {
			final boolean equals = node.equals(mapping.getImage());
			return equals;
		});
	}
}
