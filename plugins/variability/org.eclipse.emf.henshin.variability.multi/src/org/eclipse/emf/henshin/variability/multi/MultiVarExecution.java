package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;

public class MultiVarExecution {

	private final MultiVarProcessor<?,?> processor;
	private final MultiVarEngine engine;

	public MultiVarExecution(final MultiVarProcessor<?,?> processor, final MultiVarEngine engine) {
		this.processor = processor;
		this.engine = engine;
	}

	public Collection<Change> transformSPL(final Rule rule,
			final MultiVarEGraph graphP) throws InconsistentRuleException {
		final MultiVarMatcher matcher = new MultiVarMatcher(rule, graphP, this.engine);
		final Collection<Change> changes = liftAndAppy(matcher.findMatches(), graphP, matcher.getLifting());
		if (!changes.isEmpty()) {
			this.processor.writePCsToModel(graphP);
		}
		return changes;
	}

	public Collection<Change> liftAndAppy(final Iterable<MultiVarMatch> matches, final MultiVarEGraph graph, final Lifting lifting) {
		final Collection<Change> changes = new LinkedList<>();
		for (final MultiVarMatch match : matches) {
			final MultiVarMatch resultMatch = lifting.liftMatch(match);
			if (resultMatch != null) {
				final Rule rule = match.getRule();
				final Change change = this.engine.createChange(rule, graph, match, new MatchImpl(rule, true));
				change.applyAndReverse();
				changes.add(change);
			}
		}
		return changes;
	}

}
