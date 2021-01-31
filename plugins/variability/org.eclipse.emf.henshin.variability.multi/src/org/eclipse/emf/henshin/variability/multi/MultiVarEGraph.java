package org.eclipse.emf.henshin.variability.multi;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;

import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToCNF;

public class MultiVarEGraph extends EGraphImpl {

	/**
	 *
	 */
	private static final long serialVersionUID = 5926898947869020227L;

	private Map<EObject, Sentence> pcs;

	private Sentence fm;

	public MultiVarEGraph() {
	}

	public MultiVarEGraph(final List<EObject> roots, final Map<EObject, Sentence> pcs, final Sentence fm) {
		super(roots);
		this.pcs = pcs;
		this.fm = fm;
	}

	public MultiVarEGraph(final List<EObject> roots, final Map<EObject, String> pcsP, final String fmP) {
		this(roots, getSentences(pcsP), FeatureExpression.getExpr(fmP));
	}

	private static Map<EObject, Sentence> getSentences(final Map<EObject, String> pcsP) {
		return pcsP.entrySet().stream().filter(entry -> (entry.getValue() != null) && !entry.getValue().isEmpty())
				.collect(Collectors.toMap(Map.Entry::getKey,
						value -> ConvertToCNF.convert(FeatureExpression.getExpr(value.getValue()))));
	}

	public void setAndConvertPCS(final Map<EObject, String> pcs) {
		this.pcs = getSentences(pcs);
	}

	public void setPCS(final Map<EObject, Sentence> pcs) {
		this.pcs = pcs;
	}

	public Map<EObject, String> getPCsAsStrings() {
		return this.pcs.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
	}

	public Map<EObject, Sentence> getPCS() {
		return this.pcs;
	}

	public void setFM(final String fm) {
		this.fm = FeatureExpression.getExpr(fm);
	}

	public Sentence getFM() {
		return this.fm;
	}
}
