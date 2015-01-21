package org.eclipse.emf.henshin.tests.giraph;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.giraph.GiraphGenerator;
import org.eclipse.emf.henshin.giraph.GiraphRunner;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.IteratedUnit;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GiraphTest {

	private static Module TEST_MODULE;

	private static IProject TEST_PROJECT;
/*
	@BeforeClass
	public static void init() {
		HenshinResourceSet resourceSet = new HenshinResourceSet("src/org/eclipse/emf/henshin/tests/giraph");
		TEST_MODULE = resourceSet.getModule("GiraphTests.henshin");
		Assert.assertNotNull(TEST_MODULE);
	}

	@Test
	public void fork() {
		run("ForkMain", "ForkStart", 1, 0);
	}

	@Test
	public void parallelNodes() {
		run("ParallelNodes", "ParallelNodesStart", 0, 0);
	}

	@Test
	public void parallelEdges() {
		run("ParallelEdges", "ParallelEdgesStart", 0, 0);
	}

	@Test
	public void parallelTriangles() {
		run("ParallelTriangles", "ParallelTrianglesStart", 0, 0);
	}

	@Test
	public void parallelV() {
		run("ParallelV", "ParallelVStart", 0, 0);
	}

	@Test
	public void requireOne() {
		run("RequireOne", "RequireStart", 5, 8);
	}

	@Test
	public void requireTwo() {
		run("RequireTwo", "RequireStart", 5, 8);
	}

	@Test
	public void sierpinski1() {
		runIterated("SierpinskiMain", 1, "Sierpinski", 6, 9);
	}

	@Test
	public void sierpinski2() {
		runIterated("SierpinskiMain", 2, "Sierpinski", 15, 27);
	}

	@Test
	public void sierpinski3() {
		runIterated("SierpinskiMain", 3, "Sierpinski", 42, 81);
	}

	@Test
	public void sierpinski6() {
		runIterated("SierpinskiMain", 6, "Sierpinski", 1095, 2187);
	}

	@Test
	public void sierpinski9() {
		runIterated("SierpinskiMain", 9, "Sierpinski", 29526, 59049);
	}

	@Test
	public void star() {
		run("StarMain", "StarStart", 1, 0);
	}

	@Test
	public void twoTimesTwo1() {
		run("TwoTimesTwo", "TwoTimesTwoStart1", 7, 10);
	}

	@Test
	public void twoTimesTwo2() {
		run("TwoTimesTwo", "TwoTimesTwoStart2", 7, 10);
	}

	@Test
	public void twoTimesTwo3() {
		run("TwoTimesTwo", "TwoTimesTwoStart3", 10, 15);
	}

	@Test
	public void twoTimesThree() {
		run("TwoTimesThree", "TwoTimesThree", 3, 0);
	}

	@Test
	public void wheel() {
		run("WheelMain", "WheelStart", 3, 3);
	}
*/
	private void runIterated(String mainUnitName, int iterations, String inputRuleName, int aggregateVertices,
			int aggregateEdges) {

		// Prepare iterated unit:
		IteratedUnit iteratedUnit = (IteratedUnit) TEST_MODULE.getUnit(mainUnitName);
		Assert.assertNotNull(iteratedUnit);
		IteratedUnit backup = EcoreUtil.copy(iteratedUnit);
		iteratedUnit.setIterations(iterations + "");
		iteratedUnit.setName(iteratedUnit.getName() + iterations);

		// Run test:
		run(iteratedUnit, ((Rule) TEST_MODULE.getUnit(inputRuleName)).getLhs(), aggregateVertices, aggregateEdges);

		// Restore iterated unit:
		iteratedUnit.setIterations(backup.getIterations());
		iteratedUnit.setName(backup.getName());

	}

	private void run(String mainUnitName, String inputRuleName, int aggregateVertices, int aggregateEdges) {
		run(TEST_MODULE.getUnit(mainUnitName), ((Rule) TEST_MODULE.getUnit(inputRuleName)).getLhs(), aggregateVertices,
				aggregateEdges);
	}

	private void run(Unit mainUnit, Graph inputGraph, int aggregateVertices, int aggregateEdges) {
		GiraphGenerator generator = new GiraphGenerator();
		System.out.println("Generating Giraph code for " + mainUnit.getName() + "...");
		if (TEST_PROJECT == null) {
			System.out.println("Installing Hadoop Test Environment (may take a couple of minutes)...");
			generator.setTestEnvironment(true);
		}

		try {
			TEST_PROJECT = generator.generate(mainUnit, inputGraph, mainUnit.getName(), inputGraph.getRule().getName(),
					null).getProject();

			System.out.println("Starting Giraph build for " + mainUnit.getName() + "...");
			IFile antFile = TEST_PROJECT.getFolder("launch").getFile(mainUnit.getName() + ".xml");
			GiraphRunner runner = new GiraphRunner();
			Assert.assertTrue(runner.run(antFile));
			Assert.assertEquals(aggregateVertices, runner.getAggregateVertices());
			Assert.assertEquals(aggregateEdges, runner.getAggregateEdges());

		} catch (CoreException e) {
			throw new AssertionError(e);
		}
	}

}
