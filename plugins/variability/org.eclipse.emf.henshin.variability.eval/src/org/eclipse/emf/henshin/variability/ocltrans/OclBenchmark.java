/**
 * <copyright>
 * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.variability.ocltrans;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.BasicApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.trace.Trace;
import org.eclipse.emf.henshin.variability.MatchingLog;
import org.eclipse.emf.henshin.variability.MatchingLogEntry;
import org.eclipse.emf.henshin.variability.VBUnitApplicationImpl;
import org.eclipse.emf.henshin.variability.util.VBRuleUtil;

import GraphConstraint.GraphConstraintPackage;
import GraphConstraint.NestedGraphConstraint;

public class OclBenchmark {
	private static final String FILE_PATH_RULES = "../org.eclipse.emf.henshin.variability.test/ocl/";
	private static final String FILE_PATH_INSTANCE = "instances/";
	private static final String FILE_NAME_OCL_METAMODEL = "Pivot.ecore";
	private static final String FILE_NAME_RULES_CLASSIC = "OCL2NGC.henshin";
	private static final String FILE_NAME_RULES_VAR = "OCL2NGC-var.henshin";
	private static final String FILE_NAME_RULES_VAR_HAND = "OCL2NGC-var-hand.henshin";
	private static final String FILE_NAME_MODEL = "/PetriNetWithOCLPaper.ecore";
	private static final String FILE_NAME_MODEL_OCLAS = "/PetriNetWithOCLPaper.ecoreecore.oclas";

	private static final String TRACEROOT = "traceroot";
	private static final String INVARIANT = "invariant";
	private static final String NGC = "ngc";
	private static boolean printTrace = false; // will print successful matching
												// attempts
	private static boolean printTraceDeep = false; // will print all matching
													 // attempts
	static String[] examples = { "01",
			"02", "03"
			, "04", "05a", "05b", "06", "07",
			"08", "09" };
	
	enum mode {
		HANDCRAFTED, CLASSIC, MERGED
	}

	/**
	 * Relative path to the model files.
	 */
	public static final String PATH = "files/ocl";


	public static void main(String[] args) {
		boolean performVar = false;
		boolean performHand = true;
		boolean performclassic = false;
		int runs = 1;

		for (String example : examples) {
			System.out.print("Example " + example + "\t");
			if (performVar) {
				// System.out.println("Variability-aware:");
				for (int i = 0; i < runs; i++) {
					run(PATH, mode.MERGED, example);
					System.gc();
				}
			}
			if (performHand) {
				// System.out.println("Variability-aware (hand-crafted):");
				for (int i = 0; i < runs; i++) {
					run(PATH, mode.HANDCRAFTED, example);
					System.gc();
				}
			}
			if (performclassic) {
				// System.out.println("Classic:");
				for (int i = 0; i < runs; i++) {
					run(PATH, mode.CLASSIC, example);
					System.gc();
				}
			}
		}
	}

	public static String runWithVariability() {

		StringBuilder sb = new StringBuilder();
		for (String example : examples) {
				String info = run(PATH, mode.MERGED, example);
				System.gc();
		}
		return sb.toString();
		
	}
	public static String runClassically() {

		StringBuilder sb = new StringBuilder();
		for (String example : examples) {
				String info = run(PATH, mode.CLASSIC, example);
				System.gc();
		}
		return sb.toString();
	}
	/**
	 * Run the benchmark.
	 * 
	 * @param path
	 *            Relative path to the model files.
	 * @param iterations
	 *            Number of iterations.
	 */
	public static String run(String path, mode theMode, String exampleID) {
		GraphConstraintPackage.eINSTANCE.eClass();
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("ecore", new XMIResourceFactoryImpl());
		m.put("oclas", new XMIResourceFactoryImpl());

		// Create a resource set with a base directory:
		HenshinResourceSet resourceSet = new HenshinResourceSet(FILE_PATH_RULES);
		resourceSet.registerDynamicEPackages(FILE_NAME_OCL_METAMODEL);
		
		// Load the module and find the rule:

		String location = null;
		switch (theMode) {
		case CLASSIC:
			location = FILE_NAME_RULES_CLASSIC;
			break;
		case MERGED:
			location = FILE_NAME_RULES_VAR;
			break;
		case HANDCRAFTED:
			location = FILE_NAME_RULES_VAR_HAND;
			break;
		}
	
		Module module = resourceSet.getModule(location, false);

		Unit initUnit = module.getUnit("init");
		Unit mainUnit = module.getUnit("main");

		// Load the model into a graph:
		Resource metamodel = resourceSet.getResource(FILE_PATH_INSTANCE
				+ exampleID + FILE_NAME_MODEL);
		EObject root = resourceSet.getEObject(FILE_PATH_INSTANCE
				+ exampleID + FILE_NAME_MODEL_OCLAS);
		EGraph graph = new EGraphImpl();
		graph.addGraph(root);
		graph.addTree(metamodel.getContents().get(0));

		int graphInitially = graph.size();

		// Create an engine and a rule application:
		Engine engine = new EngineImpl();

		UnitApplication initUnitApplication = new UnitApplicationImpl(engine,
				graph, initUnit, null);
		ApplicationMonitor monitor = new BasicApplicationMonitor();
		System.gc();
		BasicEList<EObject> invariants = getInvariants(root);
		TreeIterator<EObject> iter = root.eAllContents();

		// TranslatorCopy.prepareOCLModel(root);

		long start = System.currentTimeMillis();
		Trace trace = null;
		NestedGraphConstraint nestedGraphConstraint = null;
		if (initUnitApplication != null) {
			Date date = new GregorianCalendar().getTime();
			for (EObject inv : invariants) {
				initUnitApplication.setParameterValue(INVARIANT, inv);
				InterpreterUtil.executeOrDie(initUnitApplication);
				nestedGraphConstraint = (NestedGraphConstraint) initUnitApplication
						.getResultParameterValue(NGC);
				trace = (Trace) initUnitApplication
						.getResultParameterValue(TRACEROOT);
			}
		}
		initUnitApplication.execute(monitor);
		if (!initUnitApplication.execute(monitor)) {
			throw new RuntimeException("Error during initialization");
		}
		// System.out.println("Initialization time: " + ((stop - start)) +
		// " millisec");

		UnitApplication mainUnitApplication = null;
		if (theMode == mode.HANDCRAFTED || theMode == mode.MERGED)
			mainUnitApplication = new VBUnitApplicationImpl(engine, graph,
					mainUnit, null);
		else
			mainUnitApplication = new VBUnitApplicationImpl(engine, graph,
					mainUnit, null);

		System.gc();

		long startTime = System.currentTimeMillis();

		monitor = new BasicApplicationMonitor();
		System.gc();

		startTime = System.currentTimeMillis();
		if (!mainUnitApplication.execute(monitor)) {
			throw new RuntimeException("Error during transformation");
		}
		long runtime = (System.currentTimeMillis() - startTime);

		String info = getInfo(graph, graphInitially, runtime);
		System.out.println(info);
		return info;
	}

	private static BasicEList<EObject> getInvariants(EObject root) {
		BasicEList<EObject> invariants = new BasicEList<EObject>();
		TreeIterator<EObject> iter = root.eAllContents();
		while (iter.hasNext()) {
			EObject eObject = iter.next();
			if (eObject.eClass().getName().equals("Class")) {
				{
					EStructuralFeature feature = eObject.eClass().getEStructuralFeature("ownedInvariant");
					invariants.addAll((Collection<EObject>) eObject.eGet(feature));
				}
			}
		}
		return invariants;
	}

	private static String getInfo(EGraph graph, int graphInitially, long runtime) {
		int varSuccessfully = 0;
		int classicSuccessfully = 0;
		int varFailed = 0;
		int classicFailed = 0;

		for (MatchingLogEntry e : MatchingLog.getEntries()) {
			if (VBRuleUtil.isVarRule(e.getUnit())) {
				if (e.isSuccessful())
					varSuccessfully++;
				else
					varFailed++;
			} else {
				if (e.isSuccessful())
					classicSuccessfully++;
				else
					classicFailed++;
			}
		}

		// System.out.println(graphInitially + "\t" + graph.size() +
		String t = "\t";
		// graph.size() + t
		StringBuilder sb = new StringBuilder();
		sb.append(graph.size() + t + MatchingLog.getEntries().size() + t
				+ (varSuccessfully + classicSuccessfully) + t
				+ (varFailed + classicFailed) + t + varSuccessfully + t
				+ varFailed + t + classicSuccessfully + t + classicFailed + t
				+ runtime);
		if (printTraceDeep)
			sb.append(MatchingLog.createString());
		if (printTrace)
			sb.append(MatchingLog.createStringForSuccessfulEntries());
		MatchingLog.getEntries().clear();
		return sb.toString();
	}

	private static void saveNestedGraphConstraint(Date date,
			NestedGraphConstraint ngc, EObject root, Trace trace) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
		String path = new Path(PATH + "/graphconstraints/" + sdf.format(date))
				.toOSString();
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
		resourceSet.saveEObject(ngc,
				path + "/" + ngc.getName().concat("GraphConstraint"));
		EStructuralFeature nameFeature = root.eClass().getEStructuralFeature("name");
		String rootName = ((String)root.eGet(nameFeature));
		resourceSet.saveEObject(root,
				path + "/" + rootName.concat(".ecore.oclass"));
		resourceSet.saveEObject(trace, path + "/"
				+ ngc.getName().concat("1" + ".trace"));
		resourceSet.saveEObject(trace, path + "/"
				+ ngc.getName().concat("2" + ".trace"));
		// try {
		// oclasFile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
		// } catch (CoreException e) {
		// e.printStackTrace();
		// }
	}

}
