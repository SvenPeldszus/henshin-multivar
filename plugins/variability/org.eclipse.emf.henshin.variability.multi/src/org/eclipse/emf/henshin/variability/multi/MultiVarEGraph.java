package org.eclipse.emf.henshin.variability.multi;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;

public class MultiVarEGraph extends EGraphImpl {

	/**
	 *
	 */
	private static final long serialVersionUID = 5926898947869020227L;

	private Map<EObject, String> pcs;

	private String fm;


	public MultiVarEGraph() {
		super();
	}

	public MultiVarEGraph(List<EObject> roots, Map<EObject, String> pcs, String fm) {
		super(roots);
		this.pcs = pcs;
		this.fm = fm;
	}

	public void setPCS(Map<EObject, String> pcs) {
		this.pcs = pcs;
	}

	public Map<EObject, String> getPCS() {
		return this.pcs;
	}

	public void setFM(String fm) {
		this.fm = fm;
	}

	public String getFM() {
		return this.fm;
	}
}
