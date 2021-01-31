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

	private final Match match;

	private final PreparedVBRule rulePreperator;

	public VBMatch(Match match, PreparedVBRule rule) {
		this.match = match;
		this.rulePreperator = rule;
	}

	public Match getMatch() {
		return this.match;
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
		return this.match.getUnit();
	}

	@Override
	public Object getParameterValue(Parameter param) {
		return this.match.getParameterValue(param);
	}

	@Override
	public void setParameterValue(Parameter param, Object value) {
		this.match.setParameterValue(param, value);
	}

	@Override
	public List<Object> getParameterValues() {
		return this.match.getParameterValues();
	}

	@Override
	public boolean isEmpty() {
		return this.match.isEmpty();
	}

	@Override
	public void clear() {
		this.match.clear();
	}

	@Override
	public boolean isResult() {
		return this.match.isResult();
	}

	@Override
	public EObject getNodeTarget(Node node) {
		return this.match.getNodeTarget(node);
	}

	@Override
	public void setNodeTarget(Node node, EObject target) {
		this.match.setNodeTarget(node, target);
	}

	@Override
	public List<EObject> getNodeTargets() {
		return this.match.getNodeTargets();
	}

	@Override
	public List<Match> getMultiMatches(Rule multiRule) {
		return this.match.getMultiMatches(multiRule);
	}

	@Override
	public boolean overlapsWith(Match match) {
		return this.match.overlapsWith(match);
	}

	@Override
	public boolean isComplete() {
		return this.match.isComplete();
	}

	@Override
	public boolean isValid() {
		return this.match.isValid();
	}
}
