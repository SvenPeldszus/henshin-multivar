package org.eclipse.emf.henshin.variability.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;

public class Lifting {
	private final MultiVarEGraph graph;
	private final MultiVarEngine engine;

	public Lifting(MultiVarEngine engine, MultiVarEGraph graph) {
		this.engine = engine;
		this.graph = graph;
	}

	public Change liftAndApplyRule(MultiVarMatch match, Rule rule) {
		MultiVarMatch resultMatch = liftMatch(match);
		if (resultMatch != null) {
			Change change = this.engine.createChange(rule, this.graph, match, new MatchImpl(rule, true));
			change.applyAndReverse();
			return change;
		}
		return null;
	}

	public MultiVarMatch liftMatch(MultiVarMatch match) {
		String phiApply = calculatePhiApply(match, match.getNACs(), match.getPACs());

		SatChecker satChecker = new SatChecker();
		boolean isSat = satChecker.isSatisfiable(phiApply);
		if (isSat) {
			match.setApplicationCondition(phiApply);
			return match;
		} else {
			return null;
		}
	}

	public String calculatePhiApply(Match match, Map<Rule, List<Match>> nacs, Map<Rule, List<Match>> pacs) {
		return Logic.and(computePhiApply(match), this.graph.getFM(), getPhiApplyPACs(pacs), getPhiApplyNACs(nacs));
	}


	private String getPhiApplyNACs(Map<Rule, List<Match>> nacs) {
		List<String> nacPhiApplies = nacs.values().parallelStream().flatMap(Collection::parallelStream)
				.map(this::computePhiApply).collect(Collectors.toList());
		return Logic.negate(Logic.or(nacPhiApplies));
	}

	private String getPhiApplyPACs(Map<Rule, List<Match>> pacs) {
		Collection<List<Match>> pacRuleMatches = pacs.values();
		List<String> pacPhiApplies = new ArrayList<>(pacRuleMatches.size());
		for (List<Match> pacMatches : pacRuleMatches) {
			pacPhiApplies.add(Logic.or(pacMatches.stream().map(this::computePhiApply).collect(Collectors.toList())));
		}
		return Logic.and(pacPhiApplies);
	}

	public String computePhiApply(Match match) {
		Map<EObject, String> pcsP = this.graph.getPCS();
		String phiApply = Logic.TRUE;
		for (EObject eObject : match.getNodeTargets()) {
			if (pcsP.containsKey(eObject)) {
				phiApply = Logic.and(phiApply, pcsP.getOrDefault(eObject, Logic.TRUE));
			}
		}
		return phiApply;
	}
}
