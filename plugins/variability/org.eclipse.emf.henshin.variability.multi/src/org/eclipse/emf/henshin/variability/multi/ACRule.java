package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.PreparedVBRule;

public class ACRule {

	private final Rule rule;
	private final Map<Node, Node> contextNodesACToOriginal;
	private final Map<Node, Node> contextNodesOriginalToAc;
	private final Map<Edge, Edge> contextEdgesACToOriginal;
	private final Map<Edge, Edge> contextEdgesOriginalToAc;

	private final Set<Node> removeNodes;
	private final Set<RemovedEdge> removeEdges;

	public ACRule(final Rule rule, final Map<Node, Node> contextNodesACToOriginal,
			final Map<Node, Node> contextNodesOriginalToAc, final Map<Edge, Edge> contextEdgesACToOriginal, final Map<Edge, Edge> contextEdgesOriginalToAc) {
		this.rule = rule;
		this.contextNodesACToOriginal = contextNodesACToOriginal;
		this.contextEdgesACToOriginal = contextEdgesACToOriginal;
		this.contextNodesOriginalToAc = contextNodesOriginalToAc;
		this.contextEdgesOriginalToAc = contextEdgesOriginalToAc;
		this.removeEdges = new HashSet<>();
		this.removeNodes = new HashSet<>();
	}

	public Rule getRule() {
		return this.rule;
	}

	public Collection<Node> getContextNodesOfACRule() {
		return this.contextNodesACToOriginal.keySet();
	}

	public Collection<Node> getContextNodesOfOriginalRule() {
		return this.contextNodesACToOriginal.values();
	}

	public Map<Edge, Edge> getContextdEgesACToOriginal() {
		return this.contextEdgesACToOriginal;
	}

	public Node getOriginalNode(final Node contextNodeOfACRule) {
		return this.contextNodesACToOriginal.get(contextNodeOfACRule);
	}

	public void addRemovedNode(final Node removedNode) {
		getRemoveNodes().add(removedNode);
	}

	public Collection<Node> getRemoveNodes() {
		return this.removeNodes;
	}

	public void addRemovedEdge(final RemovedEdge removedEdge) {
		this.removeEdges.add(removedEdge);
	}

	public ACRule prepare(final PreparedVBRule preparedRule) {
		if(!this.removeEdges.isEmpty() || !this.removeNodes.isEmpty()) {
			restore();
		}
		final List<Edge> edgesToRemove = new LinkedList<>();
		for (final Node removedBaseNode : preparedRule.getRemovedBaseRuleNodes()) {
			final Node conditionNode = this.contextNodesOriginalToAc.get(removedBaseNode);
			if(conditionNode != null) {
				for(final Edge edge : conditionNode.getAllEdges()) {
					clearEdge(edge, edgesToRemove);
				}
				this.removeNodes.add(conditionNode);
			}
		}
		for(final Edge edge : preparedRule.getRemovedBaseRuleEdges()) {
			final Edge conditionEdge = this.contextEdgesOriginalToAc.get(edge);
			if((conditionEdge != null) && !edgesToRemove.contains(conditionEdge)) {
				clearEdge(conditionEdge, edgesToRemove);
			}

		}
		this.rule.getLhs().getNodes().removeAll(this.removeNodes);
		this.rule.getLhs().getEdges().removeAll(edgesToRemove);
		return this;
	}

	/**
	 * @param edge
	 * @param edgesToRemove
	 */
	private void clearEdge(final Edge edge, final List<Edge> edgesToRemove) {
		this.removeEdges.add(new RemovedEdge(edge, edge.getSource(), edge.getTarget()));
		edge.setSource(null);
		edge.setTarget(null);
		edgesToRemove.add(edge);
	}

	public void restore() {
		final Graph lhs = this.rule.getLhs();
		lhs.getNodes().addAll(this.removeNodes);
		lhs.getEdges().addAll(this.removeEdges.stream().map(RemovedEdge::restore).collect(Collectors.toList()));
		this.removeNodes.clear();
		this.removeEdges.clear();
	}

	public Set<Entry<Node, Node>> getContextNodesACToOriginalEntries() {
		return this.contextNodesACToOriginal.entrySet();
	}
}
