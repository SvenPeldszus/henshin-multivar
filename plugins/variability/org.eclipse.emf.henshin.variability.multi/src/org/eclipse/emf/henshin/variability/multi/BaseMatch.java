package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;

public class BaseMatch {

	private final Match match;
	private final Collection<Rule> notMatchingNACs;

	public BaseMatch(final Match basePreMatch, final Map<Rule, Collection<Match>> nacMatches) {
		this.match = basePreMatch;
		this.notMatchingNACs = nacMatches.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Entry::getKey)
				.collect(Collectors.toList());
	}

	public Match getMatch() {
		return this.match;
	}

	public Collection<Rule> getNotMatchingNACs() {
		return this.notMatchingNACs;
	}

}
