package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;

public class BaseMatch {

	private final VBMatch match;
	private final Collection<Rule> notMatchingNACs;

	public BaseMatch(final VBMatch basePreMatch, final Map<Rule, Collection<Match>> nacMatches) {
		this.match = basePreMatch;
		this.notMatchingNACs = nacMatches.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Entry::getKey)
				.collect(Collectors.toList());
	}

	public VBMatch getMatch() {
		return this.match;
	}

	public Collection<Rule> getNotMatchingNACs() {
		return this.notMatchingNACs;
	}

}
