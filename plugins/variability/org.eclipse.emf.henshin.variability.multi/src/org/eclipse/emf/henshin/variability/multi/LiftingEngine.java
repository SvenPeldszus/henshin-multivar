package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.impl.TrueImpl;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.util.SatChecker;

import aima.core.logic.propositional.parsing.ast.ComplexSentence;
import aima.core.logic.propositional.parsing.ast.Connective;
import aima.core.logic.propositional.parsing.ast.Sentence;

public class LiftingEngine extends EngineImpl {

	private final String[] globalJavaImports;

	public LiftingEngine(final String... globalJavaImports) {
		super(globalJavaImports);
		this.globalJavaImports = globalJavaImports;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<Match> findMatches(final Rule rule, final EGraph graph, final Match partialMatch) {
		if (graph instanceof MultiVarEGraph) {
			return (Iterable<Match>) findMatches(rule, (MultiVarEGraph) graph, partialMatch);
		}
		return super.findMatches(rule, graph, partialMatch);
	}

	public Iterable<? extends MultiVarMatch> findMatches(final Rule rule, final MultiVarEGraph graph, final Match partialMatch) {
		final Lifting lifting = new Lifting(graph);
		final ApplicationConditionMatcher acs = new ApplicationConditionMatcher(new EngineImpl(this.globalJavaImports), rule);
		final Formula formula = rule.getLhs().getFormula();
		rule.getLhs().setFormula(new TrueImpl());
		final Iterable<Match> matches = super.findMatches(rule, graph, partialMatch);
		final List<MultiVarMatch> liftedMatches = new LinkedList<>();
		for (final Match m : matches) {
			final Map<Rule, Collection<Match>> nacs = acs.getNACMatches(acs.createACRules(acs.getNACs()).collect(Collectors.toList()), m, graph);
			final Map<Rule, Iterator<Match>> pacMatchIterators = acs.getPACMatches(acs.createACRules(acs.getPACs()).collect(Collectors.toList()), m,
					graph);
			if (pacMatchIterators == null) {
				continue;
			}
			final Map<Rule, Collection<Match>> pacs = ApplicationConditionMatcher.getAllMatches(pacMatchIterators);
			final MultiVarMatch liftedMatch = lifting.liftMatch(new MultiVarMatch(m, new LiftedRule(rule), pacs, nacs));
			if (liftedMatch != null) {
				liftedMatches.add(liftedMatch);
			}
		}
		rule.getLhs().setFormula(formula);
		return liftedMatches;
	}

	@Override
	public Change createChange(final Rule rule, final EGraph graph, final Match completeMatch, final Match resultMatch) {
		if ((graph instanceof MultiVarEGraph) && (completeMatch instanceof MultiVarMatch)) {
			return createChange(rule, (MultiVarEGraph) graph, (MultiVarMatch) completeMatch, resultMatch);
		}
		return super.createChange(rule, graph, completeMatch, resultMatch);
	}

	public Change createChange(final Rule rule, final MultiVarEGraph graph, final MultiVarMatch completeMatch, final Match resultMatch) {
		final CompoundChange complexChange = new CompoundChangeImpl(graph);
		createChanges(rule, graph, completeMatch, resultMatch, complexChange);
		return complexChange;
	}

	private void createChanges(final Rule rule, final MultiVarEGraph graph, final MultiVarMatch completeMatch, final Match resultMatch,
			final CompoundChange complexChange) {
		final Map<EObject, Sentence> pcs = graph.getPCS();

		// Get the rule change info and the object change list:
		final RuleChangeInfo ruleChange = getRuleInfo(rule).getChangeInfo();
		final List<Change> changes = complexChange.getChanges();

		for (final Parameter param : rule.getParameters()) {
			final Object value = completeMatch.getParameterValue(param);
			resultMatch.setParameterValue(param, value);
			this.scriptEngine.put(param.getName(), value);
		}

		// Created objects:
		Sentence applicationCondition = FeatureExpression.TRUE;
		if (completeMatch instanceof MultiVarMatch) {
			applicationCondition = completeMatch.getApplicationCondition();
		}
		for (final Node node : ruleChange.getCreatedNodes()) {
			final EClass type = node.getType();
			final EObject createdObject = type.getEPackage().getEFactoryInstance().create(type);
			if (!applicationCondition.equals(FeatureExpression.TRUE)) {
				pcs.put(createdObject, applicationCondition);
			}
			changes.add(new ObjectChangeImpl(graph, createdObject, true));
			resultMatch.setNodeTarget(node, createdObject);
		}

		// Deleted objects:
		final HashSet<EObject> ignoreDeletion = new HashSet<>();
		final HashSet<EObject> deleted = new HashSet<>();
		for (final Node node : ruleChange.getDeletedNodes()) {
			final EObject deletedObject = completeMatch.getNodeTarget(node);
			Sentence phidtick;
			if (pcs.containsKey(deletedObject)) {
				final Sentence phid = pcs.get(deletedObject);
				phidtick = new ComplexSentence(Connective.AND, phid, new ComplexSentence(Connective.NOT,applicationCondition));
			} else {
				phidtick = new ComplexSentence(Connective.NOT,applicationCondition);
			}

			final boolean isSat = SatChecker.isSatisfiable(new ComplexSentence(Connective.AND, graph.getFM(), phidtick));
			if (isSat) {
				pcs.put(deletedObject, phidtick);

				final Collection<Setting> removedEdges = graph.getCrossReferenceAdapter().getInverseReferences(deletedObject);
				for (final Setting edge : removedEdges) {
					ignoreDeletion.add(edge.getEObject());
				}
			} else {
				deleted.add(deletedObject);
				pcs.remove(deletedObject);
				changes.add(new ObjectChangeImpl(graph, deletedObject, false));
				// TODO: Shouldn't we check the rule options?
				if (!rule.isCheckDangling()) {
					final Collection<Setting> removedEdges = graph.getCrossReferenceAdapter()
							.getInverseReferences(deletedObject);
					for (final Setting edge : removedEdges) {
						changes.add(new ReferenceChangeImpl(graph, edge.getEObject(), deletedObject,
								(EReference) edge.getEStructuralFeature(), false));
					}
				}
			}
		}

		// Preserved objects:
		for (final Node node : ruleChange.getPreservedNodes()) {
			final Node lhsNode = rule.getMappings().getOrigin(node);
			resultMatch.setNodeTarget(node, completeMatch.getNodeTarget(lhsNode));
		}

		// Deleted edges:
		for (final Edge edge : ruleChange.getDeletedEdges()) {
			if (!ignoreDeletion.contains(edge)) {
				changes.add(new ReferenceChangeImpl(graph, completeMatch.getNodeTarget(edge.getSource()),
						completeMatch.getNodeTarget(edge.getTarget()), edge.getType(), false));
			}
		}

		// Created edges:
		for (final Edge edge : ruleChange.getCreatedEdges()) {
			changes.add(new ReferenceChangeImpl(graph, resultMatch.getNodeTarget(edge.getSource()),
					resultMatch.getNodeTarget(edge.getTarget()), edge.getType(), true));

		}

		// Edge index changes:
		for (final Edge edge : ruleChange.getIndexChanges()) {
			Integer newIndex = edge.getIndexConstant();
			if (newIndex == null) {
				final Parameter param = rule.getParameter(edge.getIndex());
				if (param != null) {
					newIndex = ((Number) resultMatch.getParameterValue(param)).intValue();
				} else {
					try {
						newIndex = ((Number) this.scriptEngine.eval(edge.getIndex(), Collections.emptyList()))
								.intValue();
					} catch (final ScriptException e) {
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
		for (final Attribute attribute : ruleChange.getAttributeChanges()) {
			final EObject object = resultMatch.getNodeTarget(attribute.getNode());
			Object value;
			final Parameter param = rule.getParameter(attribute.getValue());
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
		for (final Rule multiRule : rule.getMultiRules()) {
			for (final Match multiMatch : completeMatch.getMultiMatches(multiRule)) {
				final Match multiResultMatch = new MatchImpl(multiRule, true);
				for (final Mapping mapping : multiRule.getMultiMappings()) {
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
	public void createChanges(final Rule rule, final EGraph graph, final Match completeMatch, final Match resultMatch,
			CompoundChange complexChange) {
		if (complexChange == null) {
			complexChange = new CompoundChangeImpl(graph);
		}
		if (graph instanceof MultiVarEGraph) {
			final MultiVarEGraph multiVarGraph = (MultiVarEGraph) graph;
			if (completeMatch instanceof MultiVarMatch) {
				final MultiVarMatch multiVarMatch = (MultiVarMatch) completeMatch;
				createChanges(rule, multiVarGraph, multiVarMatch, resultMatch, complexChange);
			} else {
				super.createChanges(rule, multiVarGraph, completeMatch, resultMatch, complexChange);
			}
		}
		if (completeMatch instanceof MultiVarMatch) {
			throw new IllegalStateException();
		} else {
			super.createChanges(rule, graph, completeMatch, resultMatch, complexChange);
		}
	}

}
