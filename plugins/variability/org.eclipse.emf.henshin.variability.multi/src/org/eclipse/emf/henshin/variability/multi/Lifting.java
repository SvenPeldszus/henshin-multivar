package org.eclipse.emf.henshin.variability.multi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.util.SatChecker;

import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToCNF;

public class Lifting {

	private final Sentence fm;
	private final Map<EObject, Sentence> pcs;

	public Lifting(final MultiVarEGraph graph) {
		this(graph.getPCS(), graph.getFM());
	}

	public Lifting(final Map<EObject, Sentence> pcs, final Sentence fm) {
		this.pcs = pcs;
		this.fm = fm;
	}

	public MultiVarMatch liftMatch(final MultiVarMatch match) {
		final Sentence phiApply = calculatePhiApply(match, match.getNACs(), match.getPACs());
		final boolean isSat = SatChecker.isCNFSatisfiable(phiApply);
		if (isSat) {
			match.setApplicationCondition(phiApply);
			return match;
		} else {
			return null;
		}
	}

	public Sentence calculatePhiApply(final Match match, final Map<Rule, Collection<Match>> nacs,
			final Map<Rule, Collection<Match>> pacs) {
		return FeatureExpression.and(computePhiApply(match), this.fm, getPhiApplyPACs(pacs), getPhiApplyNACs(nacs));
	}

	private Sentence getPhiApplyNACs(final Map<Rule, Collection<Match>> nacs) {
		final Set<Sentence> nacPhiApplies = nacs.values().parallelStream().flatMap(Collection::parallelStream)
				.map(this::computePhiApply).map(FeatureExpression::negate).map(ConvertToCNF::convert).collect(Collectors.toSet());
		if (nacPhiApplies.isEmpty()) {
			return FeatureExpression.TRUE;
		}
		return FeatureExpression.and(nacPhiApplies);
	}

	private Sentence getPhiApplyPACs(final Map<Rule, Collection<Match>> pacs) {
		if (pacs.isEmpty()) {
			return FeatureExpression.TRUE;
		}
		final Iterator<Collection<Match>> pacRuleMatches = pacs.values().iterator();
		final Sentence[] pacPhiApplies = new Sentence[pacs.size()];
		for (int i = 0; i < pacs.size(); i++) {
			pacPhiApplies[i] = FeatureExpression.or(pacRuleMatches.next().stream().map(this::computePhiApply).toArray(Sentence[]::new));
		}
		return FeatureExpression.and(Arrays.asList(pacPhiApplies));
	}

	public Sentence computePhiApply(final Match match) {
		final List<Sentence> values = new LinkedList<>();
		for (final EObject eObject : match.getNodeTargets()) {
			EObject next = eObject;
			while (next instanceof EObject) {
				final Sentence pc = this.pcs.get(next);
				if (pc != null) {
					values.add(pc);
				}
				next = next.eContainer();
			}
		}
		if (values.isEmpty()) {
			return FeatureExpression.TRUE;
		}
		return FeatureExpression.and(values);
	}
}
