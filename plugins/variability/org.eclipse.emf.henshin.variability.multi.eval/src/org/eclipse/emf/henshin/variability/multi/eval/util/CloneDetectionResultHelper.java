package org.eclipse.emf.henshin.variability.multi.eval.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.mergein.clone.CloneGroupDetectionResult;

import mergeSuggestion.CloneDetectionResult;
import mergeSuggestion.CloneGroupElement;
import mergeSuggestion.MergeSuggestionFactory;

import org.eclipse.emf.henshin.variability.mergein.clone.CloneGroup;

public class CloneDetectionResultHelper {
	public static CloneDetectionResult toPersistableResult(CloneGroupDetectionResult input) {
		MergeSuggestionFactory factory = MergeSuggestionFactory.eINSTANCE;
		CloneDetectionResult result = factory.createCloneDetectionResult();
		for (org.eclipse.emf.henshin.variability.mergein.clone.CloneGroup cg : input.getCloneGroups()) {
			mergeSuggestion.CloneGroup resultCG = factory.createCloneGroup();
			result.getCloneGroups().add(resultCG);
			resultCG.getRules().addAll(cg.getRules());

			Set<Map<Rule, ?>> considered = new HashSet<>();
			for (Map<Rule, Edge> map : cg.getEdgeMappings().values()) {
				if (!considered.contains(map)) {
					CloneGroupElement resultElem = factory.createCloneGroupElement();
					resultElem.getElements().addAll(map.values());
					resultCG.getElements().add(resultElem);
					considered.add(map);
				}
			}
			
			considered.clear();
			for (Map<Rule, Attribute> map : cg.getAttributeMappings().values()) {
				if (!considered.contains(map)) {
				CloneGroupElement resultElem = factory.createCloneGroupElement();
				resultElem.getElements().addAll(map.values());
				resultCG.getElements().add(resultElem);
				considered.add(map);
				}
			}
		}
		return result;
	}

	public static CloneGroupDetectionResult fromPersistableResult(CloneDetectionResult input) {
		List<CloneGroup> resultCGs = new ArrayList<>();
		for (mergeSuggestion.CloneGroup cg : input.getCloneGroups()) {
			Map<Edge, Map<Rule, Edge>> edgeMappings = new HashMap<>();
			Map<Attribute, Map<Rule, Attribute>> attributeMappings = new HashMap<>();

			for (CloneGroupElement el : cg.getElements()) {
				if (el.getElements().get(0) instanceof Edge) {
					Map<Rule, Edge> inner = new HashMap<>();
					for (GraphElement edge : el.getElements()) {
						inner.put(edge.getGraph().getRule(), (Edge) edge);
						edgeMappings.put((Edge) edge, inner);
					}
				} else if (el.getElements().get(0) instanceof Attribute) {
					Map<Rule, Attribute> inner = new HashMap<>();
					for (GraphElement attribute : el.getElements()) {
						inner.put(attribute.getGraph().getRule(), (Attribute) attribute);
						attributeMappings.put((Attribute) attribute, inner);
					}
				}
			}

			CloneGroup resultCG = new CloneGroup(cg.getRules(), edgeMappings, attributeMappings);
			resultCGs.add(resultCG);
		}

		return new CloneGroupDetectionResult(resultCGs);
	}

}
