package org.eclipse.emf.henshin.variability.matcher;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;

/**
 * One match as yielded by variability-aware matching, comprising a regular
 * match and a set of selected features producing the regular rule yielding the
 * match.
 *
 * @author Daniel Strüber
 *
 */
public class VBMatch implements Match {

	private final Match preparedRuleMatch;
	private Match originalRuleMatch;

	private final PreparedVBRule rulePreperator;

	public VBMatch(final Match match, final PreparedVBRule rule) {
		if(!match.getRule().equals(rule.getRule())) {
			throw new IllegalArgumentException("The prepared rule is not the rule of the match!");
		}
		this.preparedRuleMatch = match;
		this.rulePreperator = rule;
	}

	public Match getMatch() {
		return this.preparedRuleMatch;
	}

	public PreparedVBRule getPreparator() {
		return this.rulePreperator;
	}

	@Override
	public Rule getRule() {
		return this.rulePreperator.getRule();
	}

	@Override
	public Unit getUnit() {
		return this.preparedRuleMatch.getUnit();
	}

	@Override
	public Object getParameterValue(final Parameter param) {
		return this.preparedRuleMatch.getParameterValue(param);
	}

	@Override
	public void setParameterValue(final Parameter param, final Object value) {
		this.preparedRuleMatch.setParameterValue(param, value);
	}

	@Override
	public List<Object> getParameterValues() {
		return this.preparedRuleMatch.getParameterValues();
	}

	@Override
	public boolean isEmpty() {
		return this.preparedRuleMatch.isEmpty();
	}

	@Override
	public void clear() {
		this.preparedRuleMatch.clear();
	}

	@Override
	public boolean isResult() {
		return this.preparedRuleMatch.isResult();
	}

	@Override
	public EObject getNodeTarget(final Node node) {
		return this.preparedRuleMatch.getNodeTarget(node);
	}

	@Override
	public void setNodeTarget(final Node node, final EObject target) {
		this.preparedRuleMatch.setNodeTarget(node, target);
	}

	@Override
	public List<EObject> getNodeTargets() {
		return this.preparedRuleMatch.getNodeTargets();
	}

	@Override
	public List<Match> getMultiMatches(final Rule multiRule) {
		return this.preparedRuleMatch.getMultiMatches(multiRule);
	}

	@Override
	public boolean overlapsWith(final Match match) {
		return this.preparedRuleMatch.overlapsWith(match);
	}

	@Override
	public boolean isComplete() {
		return this.preparedRuleMatch.isComplete();
	}

	@Override
	public boolean isValid() {
		return this.preparedRuleMatch.isValid();
	}

	public Match getMatchOnOriginalRule() {
		if(this.originalRuleMatch == null) {
			this.originalRuleMatch = this.rulePreperator.getMatchOnOriginalRule(this.preparedRuleMatch);
		}
		return this.originalRuleMatch;
	}
}
