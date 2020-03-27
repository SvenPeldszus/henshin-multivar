///**
// * <copyright>
// * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
// * This program and the accompanying materials are made available 
// * under the terms of the Eclipse Public License v1.0 which 
// * accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// * </copyright>
// */
//package org.eclipse.emf.henshin.variability.ngcbenchmark;
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
//import GraphConstraint.Formula;
//import GraphConstraint.Graph;
//import GraphConstraint.GraphConstraintFactory;
//import GraphConstraint.GraphConstraintPackage;
//import GraphConstraint.NestedGraphCondition;
//import GraphConstraint.NestedGraphConstraint;
//import GraphConstraint.Operator;
//import GraphConstraint.QuantifiedGraphCondition;
//import GraphConstraint.Quantifier;
//
//public class NgcBenchmarkDataCreator {
//	private static final String FILE_PATH_INSTANCE = "D:/git/mergein/org.eclipse.emf.henshin.variability.test/ngc/instances/";
//	private static final String FILE_NAME_SAVE_INSTANCE = "/01.GraphConstraint.xmi";
//
//	private static boolean printTrace = false; // will print successful matching
//												// attempts
//	private static boolean printTraceDeep = false; // will print all matching
//													// attempts
//
//	enum mode {
//		CLASSIC, MERGED
//	}
//
//	/**
//	 * Relative path to the model files.
//	 */
//	public static final String PATH = "files/ocl";
//
//	public static void main(String[] args) {
//		int[] exampleSizes = { 10, 100, 1000, 10000, 20000, 30000, 40000,
//				50000, 100000, 1000000, 10000000 };
//		for (int i = 0; i < exampleSizes.length; i++) {
//			run(i, exampleSizes[i]);
//		}
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
//	public static void run(int id, int size) {
//		GraphConstraintPackage.eINSTANCE.eClass();
//		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
//		Map<String, Object> m = reg.getExtensionToFactoryMap();
//		m.put("ecore", new XMIResourceFactoryImpl());
//		m.put("xmi", new XMIResourceFactoryImpl());
//
//		// Create a resource set with a base directory:
//		HenshinResourceSet resourceSet = new HenshinResourceSet(
//				FILE_PATH_INSTANCE);
//
//		NestedGraphConstraint root = GraphConstraintFactory.eINSTANCE
//				.createNestedGraphConstraint();
//		Graph graph1 = GraphConstraintFactory.eINSTANCE.createGraph();
//		Graph graph2 = GraphConstraintFactory.eINSTANCE.createGraph();
//		root.setEmptyDomain(graph1);
//		
//
//		Formula and = GraphConstraintFactory.eINSTANCE.createFormula();
//		and.setOp(Operator.AND);
//		root.setCondition(and);
//
//		for (int i = 0; i < size; i++) {
//			Formula not1 = GraphConstraintFactory.eINSTANCE.createFormula();
//			not1.setDomain(graph1);
//			not1.setOp(Operator.NOT);
//
//			QuantifiedGraphCondition qgc = GraphConstraintFactory.eINSTANCE
//					.createQuantifiedGraphCondition();
//			qgc.setDomain(graph1);
//			qgc.setQuantifier(Quantifier.FORALL);
//
//			Formula not2 = GraphConstraintFactory.eINSTANCE.createFormula();
//			not2.setDomain(graph1);
//			not2.setOp(Operator.NOT);
//			
//			NestedGraphCondition true_ = GraphConstraintFactory.eINSTANCE
//					.createTrue();
//			true_.setDomain(graph1);
//
//			and.getArgs().add(not1);
//			not1.getArgs().add(qgc);
//			qgc.setNested(not2);
//			not2.getArgs().add(true_);
//		}
//
//		resourceSet.saveEObject(root, FILE_PATH_INSTANCE + "/" + id + "/"
//				+ FILE_NAME_SAVE_INSTANCE);
//		System.out.println("saved graph with " + size + " elements");
//
//	}
//
//}
