package org.eclipse.emf.henshin.variability.multi.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
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
		final HenshinResourceSet rs = new HenshinResourceSet();
		rs.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("uml", new XMIResourceFactoryImpl());

		final Resource model = rs.getResource("models/Inheritance.uml");
		final Module module = rs.getModule("rules/nac.henshin");
		final Rule rule = module.getAllRules().get(0);
		final MultiVarProcessor<EPackage,String> multiVarProcessor = new MultiVarProcessor<EPackage,String>() {

			@Override
			public final MultiVarEGraph createEGraphAndCollectPCs(final List<EObject> roots, final String fmP) {
				final Map<EObject, String> pcs = new HashMap<>();
				for (final EObject e : roots) {
					e.eAllContents().forEachRemaining(c -> {
						if (c instanceof Generalization) {
							pcs.put(c, "A");
						}
					});
				}
				return new MultiVarEGraph(roots, pcs, fmP);
			}

			@Override
			public void writePCsToModel(final MultiVarEGraph graphP) {
				throw new UnsupportedOperationException();
			}

			@Override
			public final MultiVarEGraph createEGraphAndCollectPCs(final HenshinResourceSet set, final String modelLocation,
					final String featureModelLocation) {
				throw new UnsupportedOperationException();
			}

			@Override
			public EPackage getEPackage() {
				throw new UnsupportedOperationException();
			}
		};
		final MultiVarEGraph graph = multiVarProcessor.createEGraphAndCollectPCs(model.getContents(), "A | B");
		new MultiVarExecution(multiVarProcessor, new MultiVarEngine()).transformSPL(rule, graph);

		final long added = graph.getPCS().keySet().parallelStream().filter(Comment.class::isInstance).count();
		assertEquals(3, added);

	}
}
