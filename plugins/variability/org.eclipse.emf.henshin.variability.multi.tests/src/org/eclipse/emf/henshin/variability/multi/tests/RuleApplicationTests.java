package org.eclipse.emf.henshin.variability.multi.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.multi.MultiVarEGraph;
import org.eclipse.emf.henshin.variability.multi.MultiVarEngine;
import org.eclipse.emf.henshin.variability.multi.MultiVarMatch;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;
import org.eclipse.emf.henshin.variability.util.Logic;
import org.eclipse.uml2.uml.UMLPackage;
import org.junit.Test;

public class RuleApplicationTests {

	private Resource model;
	private Rule rule;
	private MultiVarEGraph graph;

	@Test
	public void testSimpleVarNAC() throws InconsistentRuleException {
		MultiVarEngine engine = init("Inheritance.uml", "nac.henshin");
		Collection<? extends MultiVarMatch> matches = (Collection<? extends MultiVarMatch>) engine
				.findMatches(this.rule, this.graph, null);
		assertEquals(7, matches.size());
	}

	@Test
	public void testBaseVarNAC() throws InconsistentRuleException {
		MultiVarEngine engine = init("Inheritance.uml", "baseNac.henshin");
		Collection<? extends MultiVarMatch> matches = (Collection<? extends MultiVarMatch>) engine
				.findMatches(this.rule, this.graph, null);
		assertEquals(4, matches.size());
	}

	@Test
	public void testVarEdge() throws InconsistentRuleException {
		MultiVarEngine engine = init("Inheritance.uml", "VarEdge.henshin");
		Iterable<? extends MultiVarMatch> matches = engine.findMatches(this.rule, this.graph, null);
		Collection<Change> changes = new LinkedList<>();
		for (MultiVarMatch m : matches) {
			changes.add(engine.createChange(this.rule, this.graph, m, null));
		}
	}

	/**
	 * @param modelName
	 * @param ruleName
	 * @return
	 */
	private MultiVarEngine init(String modelName, String ruleName) {
		HenshinResourceSet rs = new HenshinResourceSet();
		rs.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("uml", new XMIResourceFactoryImpl());

		this.model = rs.getResource("models/" + modelName);
		Module module = rs.getModule("rules/" + ruleName);
		this.rule = module.getAllRules().get(0);

		this.graph = new MultiVarProcessor() {

			@Override
			public MultiVarEGraph createEGraphAndCollectPCs(List<EObject> roots, Map<EObject, String> pcsP, String fm) {
				return createEGraph(roots);
			}

			private MultiVarEGraph createEGraph(List<EObject> roots) {
				return new MultiVarEGraph(roots, Collections.emptyMap(), Logic.TRUE);
			}

			@Override
			public void writePCsToModel(MultiVarEGraph graphP) {
			}
		}.createEGraph(this.model.getContents());
		return new MultiVarEngine();
	}
}
