package org.eclipse.emf.henshin.variability.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.emf.henshin.variability.util.SatChecker;

public class Lifting {

	private final String fm;
	private final Map<EObject, String> pcs;

	public Lifting(MultiVarEGraph graph) {
		this.pcs = graph.getPCS();
		this.fm = graph.getFM();
	}

	public Lifting(Map<EObject, String> pcs, String fm) {
		this.pcs = pcs;
		this.fm = fm;
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
		return Logic.and(computePhiApply(match), this.fm, getPhiApplyPACs(pacs), getPhiApplyNACs(nacs));
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
		List<String> pcs = new LinkedList<>();
		for (EObject eObject : match.getNodeTargets()) {
			EObject next = eObject;
			while (next instanceof EObject) {
				String pc = this.pcs.get(next);
				if (pc != null && !pc.isEmpty()) {
					pcs.add(pc);
				}
				next = next.eContainer();
			}
		}
		return Logic.and(pcs);
	}
}
