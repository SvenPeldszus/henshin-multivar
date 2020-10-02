package org.eclipse.emf.henshin.variability.ui.wizard;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.VBRuleApplicationImpl;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;

public class VariabilityAwareInterpreterUtil {
	
	/**
	 * Apply a unit to the contents of a resource. This automatically creates an {@link EGraph} and updates the contents
	 * of the resource.
	 * 
	 * @param assignment Assignment to be used.
	 * @param engine Engine to be used.
	 * @param resource Resource containing the model to be transformed.
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
				for(VBMatch match : allMatches) {
					RuleApplication vbRuleApp = new VBRuleApplicationImpl(engine, graph, rule, configuration, match);
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
			UnitApplication application = new UnitApplicationImpl(engine, graph, assignment.getUnit(), assignment);
			result = application.execute(monitor);
		}

		// Remember the old root objects:
		Set<EObject> oldRoots = new HashSet<EObject>();
		oldRoots.addAll(graph.getRoots());

		// Apply the unit:
		

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

}
