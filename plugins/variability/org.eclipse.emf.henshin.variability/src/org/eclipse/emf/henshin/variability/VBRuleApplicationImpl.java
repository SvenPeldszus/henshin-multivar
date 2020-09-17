package org.eclipse.emf.henshin.variability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.interpreter.impl.RuleApplicationImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.util.VBRuleUtil;

/**
 * Variability-aware {@link org.eclipse.emf.henshin.interpreter.RuleApplication
 * RuleApplication} implementation.
 *
 * @author Daniel Strüber
 * @author Sven Peldszus
 */
public class VBRuleApplicationImpl extends RuleApplicationImpl {
	private VBMatch completeVarMatch;
	private Map<String, Boolean> configuration;

	public VBRuleApplicationImpl(Engine engine, EGraph graph, Rule rule, Assignment partialMatch) {
		super(engine, graph, rule, partialMatch);
		this.completeVarMatch = null;
		this.configuration = new HashMap<>();
	}

	/**
	 *
	 * @param engine
	 * @param graph
	 * @param rule
	 * @param completeMatchVar If available, a complete match
	 * @param configuration A (potentially partial) map of feature names to a boolean value
	 */
	public VBRuleApplicationImpl(Engine engine, EGraph graph, Rule rule, Map<String,Boolean> configuration, VBMatch completeMatchVar) {
		super(engine, graph, rule, null);
		if (configuration == null) {
			this.configuration = new HashMap<>();
		} else {
			this.configuration = configuration;
		}
		this.completeVarMatch = completeMatchVar;
	}

	public VBRuleApplicationImpl(Engine engine) {
		super(engine);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.emf.henshin.interpreter.UnitApplication#execute(org.eclipse
	 * .emf.henshin.interpreter.ApplicationMonitor)
	 */
	@Override
	public boolean execute(ApplicationMonitor monitor) {
		if (this.unit == null) {
			throw new NullPointerException("No transformation unit set");
		}
		// Already executed?
		if (this.isExecuted) {
			if (this.isCompleteMatchDerived) {
				this.completeMatch = null; // reset the complete match if it was
				// derived
				this.isCompleteMatchDerived = false;
			}
			this.isExecuted = false;
			this.isUndone = false;
			this.change = null;
			this.resultMatch = null;
		}
		// Do we need to derive a complete match?

		if (this.completeVarMatch != null) {
			this.completeMatch = this.completeVarMatch.getMatch();
		} else {
			long startTime = System.currentTimeMillis();

			if (this.completeMatch == null) {
				if (!VBRuleUtil.isVarRule(this.unit)) {
					this.completeMatch = this.engine.findMatches((Rule) this.unit, this.graph, this.partialMatch).iterator().next();
				} else {
					VBMatcher vbEngine;
					try {
						List<String> initiallyTrue = this.configuration.keySet().stream().filter(s -> this.configuration.get(s)).collect(Collectors.toList());
						List<String> initiallyFalse = this.configuration.keySet().stream().filter(s -> !this.configuration.get(s)).collect(Collectors.toList());
						vbEngine = new VBMatcher((Rule) this.unit, this.graph, initiallyTrue, initiallyFalse);
					} catch (InconsistentRuleException e) {
						return false;
					}
					Set<? extends VBMatch> matches = vbEngine.findMatches();
					if (!matches.isEmpty()) {
						this.completeVarMatch = matches.iterator().next();
						this.completeMatch = this.completeVarMatch.getMatch();
						this.unit = this.completeVarMatch.getMatch().getRule();

					}

				}
				this.isCompleteMatchDerived = true;
			}
			long runtime = (System.currentTimeMillis() - startTime);
			MatchingLog.getEntries().add(new MatchingLogEntry(this.unit, this.completeMatch != null, runtime, this.graph.size(), 0)); // InterpreterUtil.countEdges(graph)));
			if (this.completeMatch == null) {
				if (monitor != null) {
					monitor.notifyExecute(this, false);
				}
				return false;
			}
		}
		if (this.completeVarMatch != null) {
			this.completeVarMatch.prepareRule();
		}
		this.resultMatch = new MatchImpl((Rule) this.unit, true);
		this.change = this.engine.createChange((Rule) this.unit, this.graph, this.completeMatch, this.resultMatch);
		if (this.change == null) {
			if (monitor != null) {
				monitor.notifyExecute(this, false);
			}
			if (this.completeVarMatch != null) {
				this.completeVarMatch.undoPreparation();
			}
			return false;
		}
		this.change.applyAndReverse();
		this.isExecuted = true;
		if (monitor != null) {
			monitor.notifyExecute(this, true);
		}
		if (this.completeVarMatch != null) {
			this.completeVarMatch.undoPreparation();
		}
		return true;
	}

}
