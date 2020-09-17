package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptException;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.Change.CompoundChange;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.AttributeChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.CompoundChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.IndexChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.ObjectChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.ReferenceChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.interpreter.info.RuleChangeInfo;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

public class MultiVarEngine extends EngineImpl {

	private final String[] globalJavaImports;

	public MultiVarEngine(String... globalJavaImports) {
		super(globalJavaImports);
		this.globalJavaImports = globalJavaImports;
	}

	public Change createChange(Rule rule, MultiVarEGraph graph, MultiVarMatch completeMatch, Match resultMatch) {

		// We need a result match:
		if (resultMatch == null) {
			resultMatch = new MatchImpl(rule, true);
		}

		// Create the object changes:
		CompoundChangeImpl complexChange = new CompoundChangeImpl(graph);
		createChanges(rule, graph, completeMatch, resultMatch, complexChange);
		return complexChange;
	}

	private void createChanges(Rule rule, MultiVarEGraph graph, Match completeMatch, Match resultMatch,
			CompoundChangeImpl complexChange) {
		Map<EObject, String> pcs = graph.getPCS();

		// Get the rule change info and the object change list:
		RuleChangeInfo ruleChange = getRuleInfo(rule).getChangeInfo();
		List<Change> changes = complexChange.getChanges();

		for (Parameter param : rule.getParameters()) {
			Object value = completeMatch.getParameterValue(param);
			resultMatch.setParameterValue(param, value);
			this.scriptEngine.put(param.getName(), value);
		}

		// Created objects:
		String applicationCondition = Logic.TRUE;
		if (completeMatch instanceof MultiVarMatch) {
			applicationCondition = ((MultiVarMatch) completeMatch).getApplicationCondition();
		}
		for (Node node : ruleChange.getCreatedNodes()) {
			EClass type = node.getType();
			EObject createdObject = type.getEPackage().getEFactoryInstance().create(type);
			if (!applicationCondition.trim().equals(Logic.TRUE)) {
				pcs.put(createdObject, applicationCondition);
			}
			changes.add(new ObjectChangeImpl(graph, createdObject, true));
			resultMatch.setNodeTarget(node, createdObject);
		}

		// Deleted objects:
		HashSet<EObject> ignoreDeletion = new HashSet<>();
		HashSet<EObject> deleted = new HashSet<>();
		for (Node node : ruleChange.getDeletedNodes()) {
			EObject deletedObject = completeMatch.getNodeTarget(node);
			String phidtick;
			if (pcs.containsKey(deletedObject)) {
				String phid = pcs.get(deletedObject);
				phidtick = Logic.and(phid, Logic.negate(applicationCondition));
			} else {
				phidtick = Logic.negate(applicationCondition);
			}

			SatChecker satChecker = new SatChecker();
			boolean isSat = satChecker.isSatisfiable(Logic.and(graph.getFM(), phidtick));
			if (isSat) {
				pcs.put(deletedObject, phidtick);

				Collection<Setting> removedEdges = graph.getCrossReferenceAdapter().getInverseReferences(deletedObject);
				for (Setting edge : removedEdges) {
					ignoreDeletion.add(edge.getEObject());
				}
			} else {
				deleted.add(deletedObject);
				pcs.remove(deletedObject);
				changes.add(new ObjectChangeImpl(graph, deletedObject, false));
				// TODO: Shouldn't we check the rule options?
				if (!rule.isCheckDangling()) {
					Collection<Setting> removedEdges = graph.getCrossReferenceAdapter()
							.getInverseReferences(deletedObject);
					for (Setting edge : removedEdges) {
						changes.add(new ReferenceChangeImpl(graph, edge.getEObject(), deletedObject,
								(EReference) edge.getEStructuralFeature(), false));
					}
				}
			}
		}

		// Preserved objects:
		for (Node node : ruleChange.getPreservedNodes()) {
			Node lhsNode = rule.getMappings().getOrigin(node);
			resultMatch.setNodeTarget(node, completeMatch.getNodeTarget(lhsNode));
		}

		// Deleted edges:
		for (Edge edge : ruleChange.getDeletedEdges()) {
			if (!ignoreDeletion.contains(edge)) {
				changes.add(new ReferenceChangeImpl(graph, completeMatch.getNodeTarget(edge.getSource()),
						completeMatch.getNodeTarget(edge.getTarget()), edge.getType(), false));
			}
		}

		// Created edges:
		for (Edge edge : ruleChange.getCreatedEdges()) {
			changes.add(new ReferenceChangeImpl(graph, resultMatch.getNodeTarget(edge.getSource()),
					resultMatch.getNodeTarget(edge.getTarget()), edge.getType(), true));

		}

		// Edge index changes:
		for (Edge edge : ruleChange.getIndexChanges()) {
			Integer newIndex = edge.getIndexConstant();
			if (newIndex == null) {
				Parameter param = rule.getParameter(edge.getIndex());
				if (param != null) {
					newIndex = ((Number) resultMatch.getParameterValue(param)).intValue();
				} else {
					try {
						newIndex = ((Number) this.scriptEngine.eval(edge.getIndex(), Collections.emptyList()))
								.intValue();
					} catch (ScriptException e) {
						throw new RuntimeException(
								"Error evaluating edge index expression \"" + edge.getIndex() + "\": " + e.getMessage(),
								e);
					}
				}
			}
			changes.add(new IndexChangeImpl(graph, resultMatch.getNodeTarget(edge.getSource()),
					resultMatch.getNodeTarget(edge.getTarget()), edge.getType(), newIndex));
		}

		// Attribute changes:
		for (Attribute attribute : ruleChange.getAttributeChanges()) {
			EObject object = resultMatch.getNodeTarget(attribute.getNode());
			Object value;
			Parameter param = rule.getParameter(attribute.getValue());
			if (param != null) {
				value = castValueToDataType(resultMatch.getParameterValue(param),
						attribute.getType().getEAttributeType(), attribute.getType().isMany());
			} else {
				value = evalAttributeExpression(attribute, rule); // casting done here
				// automatically
			}
			changes.add(new AttributeChangeImpl(graph, object, attribute.getType(), value));
		}

		// Now recursively for the multi-rules:
		for (Rule multiRule : rule.getMultiRules()) {
			for (Match multiMatch : completeMatch.getMultiMatches(multiRule)) {
				Match multiResultMatch = new MatchImpl(multiRule, true);
				for (Mapping mapping : multiRule.getMultiMappings()) {
					if (mapping.getImage().getGraph().isRhs()) {
						multiResultMatch.setNodeTarget(mapping.getImage(),
								resultMatch.getNodeTarget(mapping.getOrigin()));
					}
				}
				createChanges(multiRule, graph, multiMatch, multiResultMatch, complexChange);
				resultMatch.getMultiMatches(multiRule).add(multiResultMatch);
			}
		}

	}

	@Override
	public Change createChange(Rule rule, EGraph graph, Match completeMatch, Match resultMatch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createChanges(Rule rule, EGraph graph, Match completeMatch, Match resultMatch,
			CompoundChange complexChange) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Match> findMatches(Rule rule, EGraph graph, Match partialMatch) {
		if (graph instanceof MultiVarEGraph) {
			MultiVarEGraph multiVarGrah = (MultiVarEGraph) graph;
			if (!multiVarGrah.getPCS().isEmpty()) {
				Iterable<? extends Match> matches;
				try {
					matches = findMatches(rule, multiVarGrah, partialMatch);
				} catch (InconsistentRuleException e) {
					throw new IllegalArgumentException(e);
				}
				return (Iterable<Match>) matches;
			}
		}
		if (VariabilityHelper.isVariabilityRule(rule)) {
			try {
				Set<? extends VBMatch> vbMatches = new VBMatcher(rule, graph).findMatches();
				Stream<? extends VBMatch> stream;
				if (vbMatches.size() > 10) {
					stream = vbMatches.parallelStream();
				} else {
					stream = vbMatches.stream();
				}
				return stream.map(VBMatch::getMatch).collect(Collectors.toList());
			} catch (InconsistentRuleException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return super.findMatches(rule, graph, partialMatch);
	}

	public Iterable<? extends MultiVarMatch> findMatches(Rule rule, MultiVarEGraph graph, Match partialMatch)
			throws InconsistentRuleException {
		MultiVarMatcher matcher = new MultiVarMatcher(rule, graph, this);
		return matcher.findMatches();
	}

	public String[] getGlobalJavaImports() {
		return this.globalJavaImports;
	}
}
