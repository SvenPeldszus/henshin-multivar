package org.eclipse.emf.henshin.variability.multi;

import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.interpreter.impl.RuleApplicationImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;

public class MultiVarRuleApplication extends RuleApplicationImpl {

	public MultiVarRuleApplication(MultiVarEngine engine) {
		super(engine);
	}

	public MultiVarRuleApplication(MultiVarEngine engine, MultiVarEGraph graph, Rule rule, VBMatch match) {
		super(engine, graph, rule, match);
	}

	@Override
	public boolean execute(ApplicationMonitor monitor) {
		if(completeMatch == null) {
			completeMatch = engine.findMatches(getRule(), graph, partialMatch).iterator().next();
		}
		resultMatch = new MatchImpl((Rule) unit, true);
		change = ((MultiVarEngine) engine).createChange(getRule(), getEGraph(), completeMatch, resultMatch);
		return true;
	}
}
