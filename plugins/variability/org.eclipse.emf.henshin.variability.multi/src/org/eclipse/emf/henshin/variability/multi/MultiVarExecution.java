package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;

public class MultiVarExecution {

	private final MultiVarProcessor processor;
	private final MultiVarEngine engine;

	public MultiVarExecution(MultiVarProcessor processor, MultiVarEngine engine) {
		this.processor = processor;
		this.engine = engine;
	}

	public Collection<Change> transformSPL(Rule rule,
			MultiVarEGraph graphP) throws InconsistentRuleException {
		MultiVarMatcher matcher = new MultiVarMatcher(rule, graphP, this.engine);
		Collection<Change> changes = liftAndAppy(matcher.findMatches(), graphP, matcher.getLifting());
		if (!changes.isEmpty()) {
			this.processor.writePCsToModel(graphP);
		}
		return changes;
	}

	public Collection<Change> liftAndAppy(Iterable<MultiVarMatch> matches, MultiVarEGraph graph, Lifting lifting) {
		Collection<Change> changes = new LinkedList<>();
		for (MultiVarMatch match : matches) {
			MultiVarMatch resultMatch = lifting.liftMatch(match);
			if (resultMatch != null) {
				Rule rule = match.getRule();
				Change change = this.engine.createChange(rule, graph, match, new MatchImpl(rule, true));
				change.applyAndReverse();
				changes.add(change);
			}
		}
		return changes;
	}

}
