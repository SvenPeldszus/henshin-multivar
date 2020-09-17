/**
 * <copyright>
 * Copyright (c) 2010-2014 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.variability.combbenchmark;

import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;

import org.eclipse.emf.henshin.variability.matcher.VBMatcher;

/**
 * Comb pattern example for Henshin. This class implements several benchmarks
 * and consistency checks for constructing grid structures and matching comb
 * patterns. This class contains no transformation logic. All transformation
 * logic is specified in the Henshin transformation.
 * 
 * @author Christian Krause
 */
public class CombBenchmark {

	/**
	 * Relative path to the example files.
	 */
	public static final String PATH = "D:/git/mergein/org.eclipse.emf.henshin.variability.test/comb/";
	public static final String MODULE_CLASSIC = "comb.henshin";
	public static final String MODULE_VAR = "comb-var.henshin";

	// Used resource set:
	final HenshinResourceSet resourceSet;

	// Used transformation engine:
	final Engine engine;

	private Module moduleClassic;

	private Module moduleVar;

	/**
	 * Default constructor.
	 * 
	 * @param path
	 *            Path to the example files.
	 */
	public CombBenchmark(String path) {
		resourceSet = new HenshinResourceSet(path);
		engine = new EngineImpl();
		moduleClassic = resourceSet.getModule(MODULE_CLASSIC);
		moduleVar = resourceSet.getModule(MODULE_VAR);
	}

	/**
	 * Build a grid structure which will be stored in the argument graph. It is
	 * assumed that the graph is empty on the invocation.
	 * 
	 * @param graph
	 *            Target graph.
	 * @param width
	 *            Width of the grid.
	 * @param height
	 *            Height of the grid.
	 * @param sparse
	 *            Determines whether the grid is spares (separated columns)
	 * @return Application time in milliseconds.
	 */
	public long buildGrid(EGraph graph, int width, int height, boolean sparse) {

		// Load the module and unit:
		Module module = resourceSet.getModule(sparse ? "grid-sparse.henshin"
				: "grid-full.henshin", false);
		Unit unit = module.getUnit("buildGrid");

		// Apply the unit:
		UnitApplication application = new UnitApplicationImpl(engine);
		application.setUnit(unit);
		application.setEGraph(graph);
		application.setParameterValue("width", width);
		application.setParameterValue("height", height);

		long time = System.currentTimeMillis();
		InterpreterUtil.executeOrDie(application);
		time = System.currentTimeMillis() - time;

		// Sanity check whether to make sure the grid is correct:
		int expectedNodes = height * width + 1;
		int expectedEdges = sparse ? (width * height) + (width / 2)
				* (3 * height - 2) : (width * height) + (height - 1) * width
				+ height * (width - 1);

		if (graph.size() != expectedNodes) {
			throw new AssertionError("Generated grid incorrect");
		}
		if (InterpreterUtil.countEdges(graph) != expectedEdges) {
			throw new AssertionError("Generated grid incorrect");
		}
		return time;

	}

	/**
	 * Find all matches for the comb pattern in the grid. This checks whether
	 * the number of matches is correct and returns the required time for the
	 * match finding in milliseconds.
	 * @throws InconsistentRuleException 
	 */
	public long matchCombPattern(EGraph graph, int gridWidth, int gridHeight,
			boolean sparse, int patternWidth, Rule combPattern, boolean variabilityAware) throws InconsistentRuleException {

		long time = System.currentTimeMillis();
		int foundMatches;
		if (variabilityAware)
			foundMatches = new VBMatcher(combPattern, graph).findMatches().size();
		else
			foundMatches = InterpreterUtil.findAllMatches(engine, combPattern,
				graph, null).size();
		time = System.currentTimeMillis() - time;

		// Check whether the number of matches is correct:
		int expectedMatches = expectedCombMatchCount(gridWidth, gridHeight,
				sparse, patternWidth);
		if (expectedMatches != foundMatches) {
			throw new AssertionError("Expected " + expectedMatches
					+ " for the comb pattern, but found " + foundMatches);
		}
		return time;

	}

	/**
	 * Compute the expected number of matches for the comb pattern.
	 */
	public static int expectedCombMatchCount(int gridWidth, int gridHeight,
			boolean sparse, int patternWidth) {
		return sparse ? 0 : (gridWidth - patternWidth + 1) * (gridWidth - 1);
	}

	/**
	 * Run the complete benchmark.
	 * 
	 * @param path
	 *            Path to the example files.
	 * @throws InconsistentRuleException 
	 */
	public static void runFindNoMatch(String path) throws InconsistentRuleException {

		System.out.println("Starting comb benchmark...");
		System.out.println("MaxMemory: " + Runtime.getRuntime().maxMemory()
				/ 1024 / 1024 + "M\n");

		CombBenchmark benchmark = new CombBenchmark(path);
		benchmark.engine.getOptions().put(Engine.OPTION_SORT_VARIABLES, false);
		EGraph grid = new EGraphImpl();

		int maxFullLen = 60;
		int maxSparseLen = 1000;

		// Benchmark for generating sparse grid:
//		System.out.println("Benchmark for generating sparse grid...");
//		System.out.println("Length\tNodes\tTime");
//		for (int i = 10; i <= maxSparseLen; i += 10) {
//			System.out.println(i + "\t" + (i * i) + "\t"
//					+ benchmark.buildGrid(grid, i, i, true));
//			grid.clear();
//		}

		/*
		 * // Benchmark for full grid:
		 * System.out.println("\nBenchmark for generating full grid...");
		 * System.out.println("Length\tNodes\tTime"); for (int i=10;
		 * i<=maxFullLen; i+=10) { System.out.println(i + "\t" + (i*i) + "\t" +
		 * benchmark.buildGrid(grid, i, i, false)); grid.clear(); }
		 * 
		 * // Benchmark for matching comb pattern in the full grid:
		 * System.out.println
		 * ("\nBenchmark for matching comb pattern in full grid...");
		 * System.out.println("GridLen\tPatLen\tMatches\tTime");
		 * benchmark.buildGrid(grid, maxFullLen, maxFullLen, false); for (int
		 * j=10; j<=maxFullLen; j+=10) { for (Rule rule :
		 * benchmark.moduleClassic.getRules()) { System.out.println(maxFullLen +
		 * "\t" + j + "\t" + expectedCombMatchCount(maxFullLen, maxFullLen,
		 * false, j) + "\t" + benchmark.matchCombPattern(grid, maxFullLen,
		 * maxFullLen, false, j, rule));
		 * 
		 * }
		 * 
		 * } grid.clear();
		 */

		// Benchmark for matching comb pattern in the sparse grid (no matches):
		System.out
				.println("\nBenchmark for matching comb pattern in sparse grid (no matches)...");
		System.out.println("GridLen\tPatLen\tTime");
		for (int j = 280; j <= maxSparseLen; j += 20) {
			grid.clear();
			benchmark.buildGrid(grid, j, j, true);
			int m = 3;
			long combined = 0;
			for (Rule rule : benchmark.moduleClassic.getAllRules()) {
				combined += benchmark.matchCombPattern(grid, j, j, true, m,
						rule, false);
			}
			System.out.println(j + "\t" + j + "\t" + combined);
			for (Rule rule : benchmark.moduleVar.getAllRules()) {
				System.out
						.println(j
								+ "\t"
								+ j
								+ "\t"
								+ benchmark.matchCombPattern(grid, j, j, true,
										m, rule, true));
			}
		}
		grid.clear();

	}
	

	/**
	 * Run the complete benchmark.
	 * 
	 * @param path
	 *            Path to the example files.
	 * @throws InconsistentRuleException 
	 */
	public static void runFindAllMatches(String path) throws InconsistentRuleException {

		System.out.println("Starting comb benchmark...");
		System.out.println("MaxMemory: " + Runtime.getRuntime().maxMemory()
				/ 1024 / 1024 + "M\n");

		CombBenchmark benchmark = new CombBenchmark(path);
		benchmark.engine.getOptions().put(Engine.OPTION_SORT_VARIABLES, false);
		EGraph grid = new EGraphImpl();

		int maxFullLen = 1000;

		// Benchmark for generating sparse grid:
//		System.out.println("Benchmark for generating sparse grid...");
//		System.out.println("Length\tNodes\tTime");
//		for (int i = 10; i <= maxSparseLen; i += 10) {
//			System.out.println(i + "\t" + (i * i) + "\t"
//					+ benchmark.buildGrid(grid, i, i, true));
//			grid.clear();
//		}

		/*
		 * // Benchmark for full grid:
		 * System.out.println("\nBenchmark for generating full grid...");
		 * System.out.println("Length\tNodes\tTime"); for (int i=10;
		 * i<=maxFullLen; i+=10) { System.out.println(i + "\t" + (i*i) + "\t" +
		 * benchmark.buildGrid(grid, i, i, false)); grid.clear(); }
		 * 
		 * // Benchmark for matching comb pattern in the full grid:
		 * System.out.println
		 * ("\nBenchmark for matching comb pattern in full grid...");
		 * System.out.println("GridLen\tPatLen\tMatches\tTime");
		 * benchmark.buildGrid(grid, maxFullLen, maxFullLen, false); for (int
		 * j=10; j<=maxFullLen; j+=10) { for (Rule rule :
		 * benchmark.moduleClassic.getRules()) { System.out.println(maxFullLen +
		 * "\t" + j + "\t" + expectedCombMatchCount(maxFullLen, maxFullLen,
		 * false, j) + "\t" + benchmark.matchCombPattern(grid, maxFullLen,
		 * maxFullLen, false, j, rule));
		 * 
		 * }
		 * 
		 * } grid.clear();
		 */

		// Benchmark for matching comb pattern in the sparse grid (no matches):
		System.out
				.println("\nBenchmark for matching comb pattern in sparse grid (no matches)...");
		System.out.println("GridLen\tPatLen\tTime");
		for (int j = 280; j <= maxFullLen; j += 20) {
			grid.clear();
			benchmark.buildGrid(grid, j, j, true);
			int m = 3;
			long combined = 0;
			for (Rule rule : benchmark.moduleClassic.getAllRules()) {
				combined += benchmark.matchCombPattern(grid, j, j, false, m,
						rule, false);
			}
			System.out.println(j + "\t" + j + "\t" + combined);
			for (Rule rule : benchmark.moduleVar.getAllRules()) {
				System.out
						.println(j
								+ "\t"
								+ j
								+ "\t"
								+ benchmark.matchCombPattern(grid, j, j, false,
										m, rule, true));
			}
		}
		grid.clear();

	}

	public static void main(String... args) throws InconsistentRuleException {
		runFindNoMatch(PATH);
	}

}
