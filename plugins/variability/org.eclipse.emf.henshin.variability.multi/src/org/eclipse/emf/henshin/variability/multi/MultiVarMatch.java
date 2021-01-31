package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Map;

import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.PreparedVBRule;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;

import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 * Contains the match of a rule and the matches for the NACs and PACs for lifting
 *
 * @author speldszus
 *
 */
public class MultiVarMatch extends VBMatch {

	private final Map<Rule, Collection<Match>> nacMatches;
	private final Map<Rule, Collection<Match>> pacMatches;
	private Sentence applicationCondition;

	public MultiVarMatch(Match match, PreparedVBRule rulePreparator,
			Map<Rule, Collection<Match>> pacMatchMap, Map<Rule, Collection<Match>> nacMatchMap) {
		super(match, rulePreparator);
		this.nacMatches = nacMatchMap;
		this.pacMatches = pacMatchMap;
	}

	public Map<Rule, Collection<Match>> getNACs() {
		return this.nacMatches;
	}

	public Map<Rule, Collection<Match>> getPACs() {
		return this.pacMatches;
	}

	public void setApplicationCondition(Sentence applicationCondition) {
		this.applicationCondition = applicationCondition;
	}

	public Sentence getApplicationCondition() {
		return this.applicationCondition;
	}

	@Override
	public boolean isComplete() {
		return super.isComplete() && this.applicationCondition != null;
	}
}
