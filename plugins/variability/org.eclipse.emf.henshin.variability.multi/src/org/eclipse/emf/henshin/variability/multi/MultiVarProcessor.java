package org.eclipse.emf.henshin.variability.multi;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public interface MultiVarProcessor {

	MultiVarEGraph createEGraphAndCollectPCs(List<EObject> roots, Map<EObject, String> pcsP, String fmP);

	void writePCsToModel(MultiVarEGraph graphP);
}
