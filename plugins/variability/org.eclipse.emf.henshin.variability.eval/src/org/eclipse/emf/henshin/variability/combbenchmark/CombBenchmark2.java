///**
// * <copyright>
// * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
// * This program and the accompanying materials are made available 
// * under the terms of the Eclipse Public License v1.0 which 
// * accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// * </copyright>
// */
//package org.eclipse.emf.henshin.variability.combbenchmark;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.Map;
//
//import org.eclipse.core.runtime.Path;
//import org.eclipse.emf.common.util.BasicEList;
//import org.eclipse.emf.common.util.TreeIterator;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
//import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
//import org.eclipse.emf.henshin.interpreter.EGraph;
//import org.eclipse.emf.henshin.interpreter.Engine;
//import org.eclipse.emf.henshin.interpreter.UnitApplication;
//import org.eclipse.emf.henshin.interpreter.impl.BasicApplicationMonitor;
//import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
//import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
//import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
//import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
//import org.eclipse.emf.henshin.model.Module;
//import org.eclipse.emf.henshin.model.Unit;
//import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
//import org.eclipse.emf.henshin.trace.Trace;
//import org.eclipse.emf.henshin.variability.MatchingLog;
//import org.eclipse.emf.henshin.variability.MatchingLogEntry;
//import org.eclipse.emf.henshin.variability.VarUnitApplicationImpl;
//import org.eclipse.emf.henshin.variability.util.RuleUtil;
//import org.eclipse.ocl.examples.pivot.Class;
//import org.eclipse.ocl.examples.pivot.Constraint;
//import org.eclipse.ocl.examples.pivot.PivotPackage;
//import org.eclipse.ocl.examples.pivot.Root;
//
//import GraphConstraint.Graph;
//import GraphConstraint.GraphConstraintPackage;
//import GraphConstraint.NestedGraphConstraint;
//
//public class CombBenchmark2 {
//	private static final String FILE_PATH_RULES = "D:/git/mergein/org.eclipse.emf.henshin.variability.test/comb/";
//	private static final String FILE_NAME_RULES_CLASSIC = "RemoveNots.henshin";
//	private static final String FILE_NAME_RULES_VAR = "RemoveNots-var.henshin";
//	private static final String FILE_NAME_INSTANCE = "/01.GraphConstraint.xmi";
//	private static final String FILE_NAME_SAVE_INSTANCE = "/01.GraphConstraint.xmi";
//	
//	private static boolean printTrace = false;      // will print successful matching attempts
//	private static boolean printTraceDeep = false; // will print all matching attempts
//	enum mode { CLASSIC, MERGED }
//	
//
//	/**
//	 * Relative path to the model files.
//	 */
//	public static final String PATH = "files/ocl";
//
//	public static void main(String[] args) {
//			String[] examples = {
//					"0",
//					"1",
//					"2",
//					"3",
//					"4",
//					"5",
//					"6",
//					"7",
//					"8",
//					"9",
//					"10"
//					};
////			String[] examples = { "08", "09" };
////			boolean performVar = true;
////			boolean performclassic = false;
//			boolean performVar = true;
//			boolean performclassic = true;
//			int runs = 1;
//			
//			for (String example : examples) {
//				System.out.println("Example " +example);
//				if (performVar) {
//					System.out.println("Variability-aware:");
//					for (int i = 0; i < runs; i++) {
//						run(PATH, mode.MERGED, example);
//						System.gc();
//					}
//				}
//				if (performclassic) {
//					System.out.println("Classic:");
//					for (int i = 0; i < runs; i++) {
//						run(PATH, mode.CLASSIC, example);
//						System.gc();
//					}
//				}
//			}
//			// for (int i = 0; i < 10; i++) {
//			// run(PATH, false);
//			// }
//			// for (int i = 0; i < 10; i++) {
//			// run(PATH, true);
//			// }
//		}
//
//	/**
//	 * Run the benchmark.
//	 * 
//	 * @param path
//	 *            Relative path to the model files.
//	 * @param iterations
//	 *            Number of iterations.
//	 */
//	public static void run(String path, mode theMode,
//			String exampleID) {
//		GraphConstraintPackage.eINSTANCE.eClass();
//		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
//		Map<String, Object> m = reg.getExtensionToFactoryMap();
//		m.put("ecore", new XMIResourceFactoryImpl());
//		m.put("xmi", new XMIResourceFactoryImpl());
//
//		// Create a resource set with a base directory:
//		HenshinResourceSet resourceSet = new HenshinResourceSet(FILE_PATH_RULES);
//		// Load the module and find the rule:
//
//		String location = null;
//		switch(theMode) {
//		case CLASSIC: location = FILE_NAME_RULES_CLASSIC;
//		break;
//		case MERGED: location = FILE_NAME_RULES_VAR;
//		break;
//		
//		};
//		Module module = resourceSet.getModule(location, false);
//
//		Unit mainUnit = module.getUnit("main");
//
//		// Load the model into a graph:
//		Resource metamodel = GraphConstraintPackage.eINSTANCE.eResource();
////		Resource resource = resourceSet.getResource(FILE_PATH_INSTANCE + exampleID
////				+ FILE_NAME_INSTANCE);
//		NestedGraphConstraint root = (NestedGraphConstraint) resource.getContents().get(0);
//	
//	EGraph graph = new EGraphImpl(resource);
//		graph.addTree(metamodel.getContents().get(0));
//		
//		int graphInitially = graph.size();
//
//		// Create an engine and a rule application:
//		Engine engine = new EngineImpl();
//
//
//		UnitApplication mainUnitApplication = null;
//		if (theMode == mode.MERGED)
//			mainUnitApplication = new VarUnitApplicationImpl(engine, graph,
//					mainUnit, null);
//		else
//			mainUnitApplication = new VarUnitApplicationImpl(engine, graph,
//					mainUnit, null);
//
//		System.gc();
//
//		long startTime = System.currentTimeMillis();
//
//		BasicApplicationMonitor monitor = new BasicApplicationMonitor();
//		System.gc();
//
//		startTime = System.currentTimeMillis();
//		if (!mainUnitApplication.execute(monitor)) {
//			throw new RuntimeException("Error during transformation");
//		}
//		long runtime = (System.currentTimeMillis() - startTime);
////		resourceSet.saveEObject(root, FILE_PATH_INSTANCE+"/"+FILE_NAME_SAVE_INSTANCE);
//		
//		printInfo(graph, graphInitially, runtime);
//
//	}
//
//	private static void printInfo(EGraph graph, int graphInitially, long runtime) {
//		int varSuccessfully = 0;
//		int classicSuccessfully = 0;
//		int varFailed = 0;
//		int classicFailed = 0;
//
//		for (MatchingLogEntry e : MatchingLog.getEntries()) {
//			if (RuleUtil.isVarRule(e.getUnit())) {
//				if (e.isSuccessful())
//					varSuccessfully++;
//				else
//					varFailed++;
//			} else {
//				if (e.isSuccessful())
//					classicSuccessfully++;
//				else
//					classicFailed++;
//			}
//		}
//
//		// System.out.println(graphInitially + "\t" + graph.size() +
//		String t = "\t";
//		System.out.println(graph.size() + t + MatchingLog.getEntries().size() + t
//				+ (varSuccessfully + classicSuccessfully) + t
//				+ (varFailed + classicFailed) + t + varSuccessfully + t +
//				varFailed + t + classicSuccessfully + t + classicFailed + t
//				+ runtime);
//		if (printTraceDeep)
//			System.out.println(MatchingLog.createString());
//		if (printTrace)
//			System.out.println(MatchingLog.createStringForSuccessfulEntries());
//		MatchingLog.getEntries().clear();
//	}
//	
//	
//
//}
