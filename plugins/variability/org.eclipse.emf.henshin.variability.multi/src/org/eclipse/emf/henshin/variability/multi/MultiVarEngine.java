package org.eclipse.emf.henshin.variability.multi;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

public class MultiVarEngine extends LiftingEngine {

	private final String[] globalJavaImports;

	public MultiVarEngine(String... globalJavaImports) {
		super(globalJavaImports);
		this.globalJavaImports = globalJavaImports;
	}

	@Override
	public Iterable<Match> findMatches(Rule rule, EGraph graph, Match partialMatch) {
		boolean isVBRule = VariabilityHelper.isVariabilityRule(rule);
		if (graph instanceof MultiVarEGraph && !((MultiVarEGraph) graph).getPCS().isEmpty()) {
			if (isVBRule) {
				// MultiVar if VB rule and application to SPL
				return (Iterable<Match>) findMatches(rule, (MultiVarEGraph) graph, partialMatch);
			}
			// Lifting  if only application to SPL but not VB rule
			return (Iterable<Match>) super.findMatches(rule, (MultiVarEGraph) graph, partialMatch);
		}
		if (isVBRule) {
			// Use VB Matcher if VB rule but not applied to SPL
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
		// Rely on super implementation to figure out application of classic rule to single product
		return super.findMatches(rule, graph, partialMatch);
	}

	@Override
	public Iterable<? extends MultiVarMatch> findMatches(Rule rule, MultiVarEGraph graph, Match partialMatch) {
		MultiVarMatcher matcher;
		try {
			matcher = new MultiVarMatcher(rule, graph, this);
		} catch (InconsistentRuleException e) {
			throw new IllegalStateException(e);
		}
		return matcher.findMatches();
	}

	public String[] getGlobalJavaImports() {
		return this.globalJavaImports;
	}
}
