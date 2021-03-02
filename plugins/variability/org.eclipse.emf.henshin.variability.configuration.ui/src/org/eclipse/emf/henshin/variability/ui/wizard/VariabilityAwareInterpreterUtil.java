package org.eclipse.emf.henshin.variability.ui.wizard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResource;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.VBRuleApplicationImpl;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.multi.MultiVarEGraph;
import org.eclipse.emf.henshin.variability.multi.MultiVarEngine;
import org.eclipse.emf.henshin.variability.multi.MultiVarExecution;
import org.eclipse.emf.henshin.variability.multi.MultiVarMatch;
import org.eclipse.emf.henshin.variability.multi.MultiVarMatcher;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;

public class VariabilityAwareInterpreterUtil {

	/**
	 * Apply a unit to the contents of a resource. This automatically creates an
	 * {@link EGraph} and updates the contents of the resource.
	 * 
	 * @param assignment Assignment to be used.
	 * @param engine     Engine to be used.
	 * @param resource   Resource containing the model to be transformed.
	 * @return <code>true</code> if the unit was successfully applied.
	 */
	public static boolean applyToResource(Assignment assignment, Engine engine, Resource resource,
			ApplicationMonitor monitor, Map<String, Boolean> configuration) {

		// Create the graph and the unit application:
		EGraph graph = new EGraphImpl(resource);
		boolean result = false;

		if (assignment.getUnit() instanceof Rule) {
			Rule rule = (Rule) assignment.getUnit();
			VBMatcher matcher;
			try {
				boolean foundMatch = false;
				matcher = new VBMatcher(rule, graph, configuration);
				Set<? extends VBMatch> allMatches = matcher.findMatches();
				for (VBMatch match : allMatches) {
					RuleApplication vbRuleApp = new VBRuleApplicationImpl(engine, graph, rule, match);
					foundMatch = vbRuleApp.execute(null);
					if (foundMatch) {
						result = true;
					}
				}
			} catch (InconsistentRuleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Apply the unit:
			UnitApplication application = new UnitApplicationImpl(engine, graph, assignment.getUnit(), assignment);
			result = application.execute(monitor);
		}

		// Remember the old root objects:
		Set<EObject> oldRoots = new HashSet<EObject>();
		oldRoots.addAll(graph.getRoots());

		// Sync root objects:
		List<EObject> roots = graph.getRoots();
		Iterator<EObject> it = resource.getContents().iterator();
		while (it.hasNext()) {
			if (!roots.contains(it.next())) {
				it.remove();
			}
		}
		for (EObject root : roots) {
			if (!oldRoots.contains(root)) {
				resource.getContents().add(root);
			}
		}
		return result;

	}

	public static boolean applyToResource(Assignment assignment, MultiVarEngine engine, Resource resource,
			ApplicationMonitor monitor, Map<String, Boolean> configuration, MultiVarProcessor<?, ?> processor,
			String featureModelPath) {
		final Collection<Change> changes = new LinkedList<>();
		MultiVarEGraph graph = processor.createEGraphAndCollectPCs(resource, featureModelPath);
		if (assignment.getUnit() instanceof Rule) {
			MultiVarMatcher matcher;
			try {
				matcher = new MultiVarMatcher((Rule) assignment.getUnit(), graph, engine);
				for (final MultiVarMatch match : matcher.findMatches()) {
					final MultiVarMatch resultMatch = matcher.getLifting().liftMatch(match);
					if (resultMatch != null) {
						final Rule rule = match.getRule();
						final Change change = engine.createChange(rule, graph, match, new MatchImpl(rule, true));
						change.applyAndReverse();
						changes.add(change);
					}
					if (!changes.isEmpty()) {
						processor.writePCsToModel(graph);
					}
				}
			} catch (InconsistentRuleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return !changes.isEmpty();
	}

}
