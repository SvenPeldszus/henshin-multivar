///**
// * <copyright>
// * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
// * This program and the accompanying materials are made available 
// * under the terms of the Eclipse Public License v1.0 which 
// * accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// * </copyright>
// */
//package org.eclipse.emf.henshin.variability.ocltrans;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.Map;
//
//import org.eclipse.core.resources.IFile;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.emf.common.util.BasicEList;
//import org.eclipse.emf.common.util.TreeIterator;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.ecore.EPackage;
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
//import java.io.ObjectInputStream.GetField;
//import java.net.URI;
//import java.net.URISyntaxException;
//
//import GraphConstraint.GraphConstraintPackage;
//import GraphConstraint.NestedGraphConstraint;
//
//public class OclBenchmark2 {
//	private static final String FILE_PATH_RULES = "D:/git/mergein/org.eclipse.emf.henshin.variability.test/ocl/";
//	private static final String FILE_PATH_RESULT = "D:/git/mergein/org.eclipse.emf.henshin.variability.test/ocl/ngc/";
//	private static final String FILE_PATH_INSTANCE = "D:/git/mergein/org.eclipse.emf.henshin.variability.test/ocl/instances/";
//	private static final String FILE_NAME_RULES_CLASSIC = "OCL2NGC.henshin";
//	private static final String FILE_NAME_RULES_VAR = "OCL2NGC-var.henshin";
//	private static final String FILE_NAME_RULES_VAR_HAND = "OCL2NGC-var-hand.henshin";
//	private static final String FILE_NAME_INSTANCE_ECORE = "/PetriNetWithOCLPaper.ecore";
//	private static final String FILE_NAME_INSTANCE_AS = "/PetriNetWithOCLPaper.ecore.oclas";
//
//	private static final String TRACEROOT = "traceroot";
//	private static final String INVARIANT = "invariant";
//	private static final String NGC = "ngc";
//	private static boolean printTrace = false; // will print successful matching
//												// attempts
//	private static boolean printTraceDeep = false; // will print all matching
//													// attempts
//
//	enum mode {
//		HANDCRAFTED, CLASSIC, MERGED
//	}
//
//	/**
//	 * Relative path to the model files.
//	 */
//	public static final String PATH = "files/ocl";
//
//	public static void main(String[] args) {
//		String[] examples = { 
////				"09"
//		// "02", "03",
//		 "04",
//		// "05a",
//		// "05b",
//		// "06" ,
//		// "07",
//		// "08",
//		// "09"
//		};
//		// String[] examples = { "08", "09" };
//		// boolean performVar = true;
//		// boolean performclassic = false;
//		boolean performVar = false;
//		boolean performHand = false;
//		boolean performclassic = true;
//		int runs = 1;
//
//		for (String example : examples) {
//			System.out.println("Example " + example);
//			if (performVar) {
//				System.out.println("Variability-aware:");
//				for (int i = 0; i < runs; i++) {
//					run(PATH, mode.MERGED, example);
//					System.gc();
//				}
//			}
//			if (performHand) {
//				System.out.println("Variability-aware (hand-crafted):");
//				for (int i = 0; i < runs; i++) {
//					run(PATH, mode.HANDCRAFTED, example);
//					System.gc();
//				}
//			}
//			if (performclassic) {
//				System.out.println("Classic:");
//				for (int i = 0; i < runs; i++) {
//					run(PATH, mode.CLASSIC, example);
//					System.gc();
//				}
//			}
//		}
//		// for (int i = 0; i < 10; i++) {
//		// run(PATH, false);
//		// }
//		// for (int i = 0; i < 10; i++) {
//		// run(PATH, true);
//		// }
//	}
//
//	/**
//	 * Run the benchmark.
//	 * 
//	 * @param path
//	 *            Relative path to the model files.
//	 * @param iterations
//	 *            Number of iterations.
//	 */
//	public static void run(String path, mode theMode, String exampleID) {
//		String fileNameRules = null;
//		switch (theMode) {
//		case CLASSIC: fileNameRules = FILE_NAME_RULES_CLASSIC;
//		break;
//		case HANDCRAFTED: fileNameRules = FILE_NAME_RULES_VAR_HAND;
//		break;
//		case MERGED: fileNameRules = FILE_NAME_RULES_VAR;
//		break;
//		}
//		
//		TranslatorCopy translator = new TranslatorCopy(FILE_PATH_RULES, fileNameRules,
//				FILE_PATH_INSTANCE + exampleID + FILE_NAME_INSTANCE_AS,
//				FILE_PATH_INSTANCE + exampleID + FILE_NAME_INSTANCE_ECORE,
//				FILE_PATH_RESULT, exampleID);
//		UnitApplication initUnitApp = translator.getInitUnitApp();
//		if (translator.getInitUnitApp() != null) {
//			Date date = new GregorianCalendar().getTime();
////			System.out.println(translator.getInvariants());
//			for (Constraint inv : translator.getInvariants()) {
//				initUnitApp.setParameterValue(INVARIANT, inv);
//				InterpreterUtil.executeOrDie(initUnitApp);
//				NestedGraphConstraint nestedGraphConstraint = (NestedGraphConstraint) initUnitApp
//						.getResultParameterValue(NGC);
//				Trace trace = (Trace) initUnitApp
//						.getResultParameterValue(TRACEROOT);
//
//				Unit mainUnit = translator.getMainUnit();
//				int graphInitially = translator.getGraph().size();
//
//				long start = System.currentTimeMillis();
////				VarUnitApplicationImpl mainUnitApp = new VarUnitApplicationImpl(
////						new EngineImpl(), translator.getGraph(), mainUnit, null);
//				UnitApplicationImpl mainUnitApp = new UnitApplicationImpl(translator.getEngine(), translator.getGraph(), mainUnit, null);		
//				System.out.println("Starting execution");
//				mainUnitApp.execute(null);
//				System.out.println("Finished execution");
//				long stop = System.currentTimeMillis();
//				printInfo(translator.getGraph(), graphInitially, (stop - start));
//				translator.saveNestedGraphConstraint(date,
//						nestedGraphConstraint, trace);
//			}
//		}
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
//		// graph.size() + t
//		System.out.println(graph.size() + MatchingLog.getEntries().size() + t
//				+ (varSuccessfully + classicSuccessfully) + t
//				+ (varFailed + classicFailed) + t + varSuccessfully + t
//				+ varFailed + t + classicSuccessfully + t + classicFailed + t
//				+ runtime);
//		if (printTraceDeep)
//			System.out.println(MatchingLog.createString());
//		if (printTrace)
//			System.out.println(MatchingLog.createStringForSuccessfulEntries());
//		MatchingLog.getEntries().clear();
//	}
//
//}
