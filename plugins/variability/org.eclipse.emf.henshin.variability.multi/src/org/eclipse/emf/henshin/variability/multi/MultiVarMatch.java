package org.eclipse.emf.henshin.variability.multi;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBRulePreparator;

import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 * Contains the match of a rule and the matches for the NACs and PACs for lifting
 *
 * @author speldszus
 *
 */
public class MultiVarMatch extends VBMatch {

	private final Map<Rule, List<Match>> nacMatches;
	private final Map<Rule, List<Match>> pacMatches;
	private String applicationCondition;

	public MultiVarMatch(Match match, Set<Sentence> selected, Rule rule, VBRulePreparator rulePreparator,
			Map<Rule, List<Match>> pacMatchMap, Map<Rule, List<Match>> nacMatchMap) {
		super(match, selected, rule, rulePreparator);
		this.nacMatches = nacMatchMap;
		this.pacMatches = pacMatchMap;
	}

	public Map<Rule, List<Match>> getNACs() {
		return this.nacMatches;
	}

	public Map<Rule, List<Match>> getPACs() {
		return this.pacMatches;
	}

	public void setApplicationCondition(String applicationCondition) {
		this.applicationCondition = applicationCondition;
	}

	public String getApplicationCondition() {
		return this.applicationCondition;
	}

	@Override
	public boolean isComplete() {
		return super.isComplete() && this.applicationCondition != null;
	}
}
