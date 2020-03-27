//package org.eclipse.emf.henshin.variability.test;
//
//import static org.junit.Assert.*;
//
//import org.eclipse.emf.common.util.EList;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.ecore.EPackage;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
//import org.eclipse.emf.henshin.interpreter.EGraph;
//import org.eclipse.emf.henshin.interpreter.Engine;
//import org.eclipse.emf.henshin.interpreter.impl.BasicApplicationMonitor;
//import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
//import org.eclipse.emf.henshin.interpreter.impl.EngineImpl; 
//import org.eclipse.emf.henshin.model.Module;
//import org.eclipse.emf.henshin.model.Rule; 
//import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
//import org.eclipse.emf.henshin.variability.VarUnitApplicationImpl;
//import org.eclipse.ocl.examples.pivot.Root;
//import org.junit.Test;
//
//public class InterpretationTest {
//
//	@SuppressWarnings("deprecation")
//	@Test
//	public void testInterpretRule8() {
//		HenshinResourceSet rs = new HenshinResourceSet("../org.eclipse.emf.henshin.variability.test/javarefactorings/javarefactorings8/");
//		
//		EPackage javaPackage = rs.registerDynamicEPackages("Java.ecore").get(0);
//		rs.getPackageRegistry().put(javaPackage.getNsURI(), javaPackage);
//		EObject rootPackage = rs.getEObject("Package.xmi");
//		
//		Module module = (Module) rs.getEObject("Refactorings-var.henshin");
//		Rule rule = module.getRules().get(0);
//		EGraph graph = new EGraphImpl(rootPackage);
//		Engine engine = new EngineImpl();
//		ApplicationMonitor monitor = new BasicApplicationMonitor(); 
//		printPackage(rootPackage);
//		
//		VarUnitApplicationImpl ua = new VarUnitApplicationImpl(engine);
//		ua.setEGraph(graph);
//		ua.setUnit(rule);
//		ua.execute(monitor);
//		
//		printPackage(rootPackage);
//	}
//
//	private void printPackage(EObject rootPackage) {
//		EList<EObject> classes = (EList<EObject>) rootPackage.eGet(rootPackage.eClass().getEStructuralFeature("classes"));
//		for (EObject class_ : classes) {
//			EList<EObject> methods = (EList<EObject>) class_.eGet(class_.eClass().getEStructuralFeature("methods"));
//			System.out.println("Class "+class_.eGet(class_.eClass().getEStructuralFeature("name")));
//			for (EObject method :methods) {
//				System.out.println(" * Method  "+method.eGet(method.eClass().getEStructuralFeature("name")));
//			}
//		}
//	} 
//
//}
