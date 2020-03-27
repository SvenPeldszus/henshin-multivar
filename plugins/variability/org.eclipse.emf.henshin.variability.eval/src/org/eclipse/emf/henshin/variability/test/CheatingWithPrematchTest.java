package org.eclipse.emf.henshin.variability.test;

import java.util.Iterator;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EcorePackageImpl;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;

public class CheatingWithPrematchTest {
	public static void main(String[] args) {
		HenshinResourceSet rs = new HenshinResourceSet("D:/git/mergein/org.eclipse.emf.henshin.variability.test/test/test1");
		Module module = rs.getModule("ecorerefactorings.henshin");
		Rule rule = module.getAllRules().get(0);
		Node a = rule.getLhs().getNode("A");
		Node b= rule.getLhs().getNode("B");
		
		System.out.println(a);
		System.out.println(b);
		
		
		EPackage pack = (EPackage) rs.getEObject("my.ecore");
		EClassifier aClass = pack.getEClassifier("A");
		EClassifier bClass = pack.getEClassifier("B");
		
		Engine engine = new EngineImpl(); 
		Match match = new MatchImpl(rule);
		match.setNodeTarget(a, aClass);
		match.setNodeTarget(b, bClass);
		
		EGraph graph = new EGraphImpl(pack);
		graph.addGraph(EcorePackageImpl.eINSTANCE);
		Iterator<Match> matches = engine.findMatches(rule, graph, match).iterator();
		while (matches.hasNext()) {
			EObject next = (EObject) matches.next();
			System.out.println(next);
		}
		System.out.println("Done ");
		
	}
}
