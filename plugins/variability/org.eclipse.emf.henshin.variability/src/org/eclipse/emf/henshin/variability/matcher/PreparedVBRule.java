package org.eclipse.emf.henshin.variability.matcher;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;

public class PreparedVBRule {

	private final Rule rule;
	private final Collection<String> trueFeatures;
	private final Collection<String> falseFeatures;
	private final BiMap<Node, Node> lhsOriginalToPreparedNodeMap;
	//	private final Map<Node, Node> lhsPreparedToOriginalNodeMap;
	private final BiMap<Edge, Edge> lhsOriginalToPreparedEdgeMap;
	private final BiMap<Parameter, Parameter> paramOriginalToPreparedMap;
	//	private final Map<Parameter, Parameter> paramPreparedToOriginalMap;
	private final VBRulePreparator preparator;
	private final List<Node> removedBaseRuleNodes;
	private final List<Edge> removedBaseRuleEdges;

	protected PreparedVBRule(final VBRulePreparator preparator, final Rule rule,
			final Collection<String> trueFeatures, final Collection<String> falseFeatures,
			final BiMap<Node, Node> lhsOriginalToPreparedNodeMap, final BiMap<Edge, Edge> lhsOriginalToPreparedEdgeMap,
			final BiMap<Parameter, Parameter> paramOriginalToPreparedMap, final List<Node> notClonedLhsNodes, final List<Edge> notClonedLhsEdges) {
		this.preparator = preparator;
		this.rule = rule;
		this.trueFeatures = trueFeatures;
		this.falseFeatures = falseFeatures;
		this.lhsOriginalToPreparedNodeMap = lhsOriginalToPreparedNodeMap;
		//		this.lhsPreparedToOriginalNodeMap = lhsOriginalToPreparedNodeMap.entrySet().parallelStream()
		//				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		this.lhsOriginalToPreparedEdgeMap = lhsOriginalToPreparedEdgeMap;
		this.paramOriginalToPreparedMap = paramOriginalToPreparedMap;
		//		this.paramPreparedToOriginalMap = paramOriginalToPreparedMap.entrySet().parallelStream()
		//				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		this.removedBaseRuleNodes = notClonedLhsNodes;
		this.removedBaseRuleEdges = notClonedLhsEdges;

	}

	protected PreparedVBRule(final Rule rule) {
		this.rule = rule;
		this.trueFeatures = Collections.emptyList();
		this.falseFeatures = Collections.emptyList();
		this.lhsOriginalToPreparedNodeMap = new BiMap<>();
		this.lhsOriginalToPreparedEdgeMap = new BiMap<>();
		this.paramOriginalToPreparedMap = new BiMap<>();
		this.preparator = null;
		this.removedBaseRuleNodes = Collections.emptyList();
		this.removedBaseRuleEdges = Collections.emptyList();
	}

	public Match getMatchOnOriginalRule(final Match prepared) {
		final Match match = new MatchImpl(this.preparator.getVBRule());
		for (final Node node : prepared.getRule().getLhs().getNodes()) {
			final EObject value = prepared.getNodeTarget(node);
			if (value != null) {
				match.setNodeTarget(this.lhsOriginalToPreparedNodeMap.getKey(node), value);
			}
		}
		for (final Parameter param : prepared.getRule().getParameters()) {
			final Object value = prepared.getParameterValue(param);
			if (value != null) {
				match.setParameterValue(this.paramOriginalToPreparedMap.getKey(param), value);
			}
		}
		return match;
	}

	public Match getMatchOnPreparedRule(final Match original) {
		final Match match = new MatchImpl(getRule());
		for (final Node node : original.getRule().getLhs().getNodes()) {
			final EObject value = original.getNodeTarget(node);
			if (value != null) {
				match.setNodeTarget(this.lhsOriginalToPreparedNodeMap.getValue(node), value);
			}
		}
		for (final Parameter param : original.getRule().getParameters()) {
			final Object value = original.getParameterValue(param);
			if (value != null) {
				match.setParameterValue(this.paramOriginalToPreparedMap.getValue(param), value);
			}
		}
		return match;
	}

	public Rule getRule() {
		return this.rule;
	}

	public Collection<String> getTrueFeatures() {
		return this.trueFeatures;
	}

	public Collection<String> getFalseFeatures() {
		return this.falseFeatures;
	}

	public Collection<Edge> getRemovedBaseRuleEdges() {
		return this.removedBaseRuleEdges;
	}

	public Collection<Node> getRemovedBaseRuleNodes() {
		return this.removedBaseRuleNodes;
	}

	Match getBaseMatch(final Match basePreMatch) {
		final Match match = new MatchImpl(this.rule);
		for (final Node baseNode : basePreMatch.getRule().getLhs().getNodes()) {
			final Node node = this.lhsOriginalToPreparedNodeMap.getValue(this.lhsOriginalToPreparedNodeMap.getKey(baseNode));
			match.setNodeTarget(node, basePreMatch.getNodeTarget(baseNode));
		}
		return match;
	}

	public Collection<Node> getPreservedBaseRuleNodes() {
		return this.lhsOriginalToPreparedNodeMap.keys();
	}
}