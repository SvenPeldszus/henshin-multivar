package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;

public class MultiVarExecution {

	private final MultiVarProcessor processor;

	public MultiVarExecution(MultiVarProcessor processor) {
		this.processor = processor;
	}

	public Collection<Change> transformSPLWithVBRule(List<EObject> roots, Rule vbRule,
			MultiVarEngine engine, MultiVarEGraph graphP) throws InconsistentRuleException {
		Collection<Change> changes = new MultiVarMatcher(vbRule, graphP, engine).transform();
		this.processor.deleteObsoleteVariabilityAnnotations(roots, graphP.getPCS());
		this.processor.createNewVariabilityAnnotations(roots, graphP.getPCS());
		return changes;
	}

	@Deprecated
	public Collection<Change> transformSPLWithClassicRule(List<EObject> roots, Rule rule,
			MultiVarEngine engine, MultiVarEGraph graphP) {
		Lifting lifting = new Lifting(engine, graphP);

		Collection<Change> changes = new LinkedList<>();
		Iterator<Match> it = new EngineImpl().findMatches(rule, graphP, null).iterator();
		while(it.hasNext()) {
			MultiVarMatch match = new MultiVarMatch(it.next(), Collections.emptySet(), rule, null, Collections.emptyMap(), Collections.emptyMap());
			changes.add(lifting.liftAndApplyRule(match, rule));
		}

		this.processor.deleteObsoleteVariabilityAnnotations(roots, graphP.getPCS());
		this.processor.createNewVariabilityAnnotations(roots, graphP.getPCS());
		return changes;
	}
}
