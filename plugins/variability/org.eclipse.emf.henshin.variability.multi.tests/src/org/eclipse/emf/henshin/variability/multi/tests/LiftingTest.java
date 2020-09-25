package org.eclipse.emf.henshin.variability.multi.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.multi.MultiVarEGraph;
import org.eclipse.emf.henshin.variability.multi.MultiVarEngine;
import org.eclipse.emf.henshin.variability.multi.MultiVarExecution;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.UMLPackage;
import org.junit.Test;

public class LiftingTest {

	@Test
	public void liftNAC() throws InconsistentRuleException {
		HenshinResourceSet rs = new HenshinResourceSet();
		rs.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("uml", new XMIResourceFactoryImpl());

		Resource model = rs.getResource("models/Inheritance.uml");
		Module module = rs.getModule("rules/nac.henshin");
		Rule rule = module.getAllRules().get(0);

		MultiVarProcessor multiVarProcessor = new MultiVarProcessor() {

			@Override
			public void writePCsToModel(MultiVarEGraph graphP) {

			}

			@Override
			public MultiVarEGraph createEGraphAndCollectPCs(List<EObject> roots, final Map<EObject, String> pcsP,
					String fmP) {
				for (EObject e : roots) {
					e.eAllContents().forEachRemaining(c -> {
						if (c instanceof Generalization) {
							pcsP.put(c, "A");
						}
					});
				}
				return new MultiVarEGraph(roots, pcsP, fmP);
			}
		};
		MultiVarEGraph graph = multiVarProcessor.createEGraphAndCollectPCs(model.getContents(),
				new HashMap<EObject, String>(), "A | B");
		new MultiVarExecution(multiVarProcessor, new MultiVarEngine()).transformSPL(rule, graph);

		long added = graph.getPCS().keySet().parallelStream().filter(Comment.class::isInstance).count();
		assertEquals(3, added);

	}
}
