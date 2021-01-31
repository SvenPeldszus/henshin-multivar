
package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.BasicApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.IBenchmarkReport;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.RuntimeBenchmarkReport;

public class UmlRecogBenchmarkClassic extends UmlRecogBenchmark {

	@Override
	public String mode() {
		return "CLASSIC";
	}

	@Override
	public String getOutputPath() {
		return "output/classic/";
	}

	public static final String FILE_PATH_RULES = "rules/";

	public static void main(final String[] args) {
		new UmlRecogBenchmarkClassic().run();
	}

	@Override
	public void run() {
		final HenshinResourceSet rs = init();
		final Module module = LoadingHelper.loadAllRulesAsOneModule(rs, FILE_PATH, FILE_PATH_RULES, RuleSet.NOFILTER);
		// Module module =
		// rs.getModule("rules\\generated\\CREATE\\rr_CREATE_Class_IN_Class_(nestedClassifier)_execute.henshin");

		// // Uncomment the following line to print statistics about the
		// transformation.
		// MaintainabilityBenchmarkUtil.runMaintainabilityBenchmark(module);

		for (final String projectName : values) {
			for (final RuleSet set : RuleSet.values()) {
				final File projectPath = new File(FILE_PATH + "prepared/" + projectName);

				final RuntimeBenchmarkReport reporter = new RuntimeBenchmarkReport(projectPath,
						UmlRecogBenchmarkClassic.class.getSimpleName(), FILE_PATH + getOutputPath());
				reporter.start();

				final List<String> examples = LoadingHelper.getModelLocations(FILE_PATH,
						"prepared/" + projectName + "/", FILE_NAME_INSTANCE_DIFF);

				for (final String example : examples) {
					System.out.println("Run: " + example);
					try {
						new UmlRecogBenchmarkClassic().runPerformanceBenchmark(module, set, example, projectPath,
								reporter);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Run the performance benchmark.
	 *
	 * @param report
	 *
	 * @param path       Relative path to the model files.
	 * @param iterations Number of iterations.
	 * @throws IOException
	 */
	@Override
	public void runPerformanceBenchmark(final Module module, final RuleSet set, final String exampleID,
			final File project, final IBenchmarkReport report) throws IOException {
		report.beginNewEntry(exampleID);
		final HenshinResourceSet rs = (HenshinResourceSet) module.eResource().getResourceSet();

		// Load the model into a graph:
		final EObject instance = rs.getEObject(exampleID + "/" + FILE_NAME_INSTANCE_DIFF);
		final EGraph graph = new EGraphImpl(instance);
		graph.addGraph(instance);

		final ApplicationMonitor monitor = new BasicApplicationMonitor();

		final int graphInitially = graph.size();
		final Engine engine = new EngineImpl();
		// engine.getOptions().put(Engine. OPTION_SORT_VARIABLES, false);
		final List<Rule> detectedRules = new ArrayList<>();

		System.gc();
		final long startTime = System.currentTimeMillis();

		for (final Unit unit : module.getUnits()) {
			final Rule rule = (Rule) unit;
			final long currentRunTime = System.currentTimeMillis();
			final int graphCurrent = graph.size();
			boolean successful = false;

			final List<Match> matches = InterpreterUtil.findAllMatches(engine, rule, graph, null);
			for (final Match m : matches) {
				final UnitApplication mainUnitApplication = new UnitApplicationImpl(engine, graph, unit, m);
				successful |= mainUnitApplication.execute(monitor);
			}

			final long runtime = (System.currentTimeMillis() - currentRunTime);
			final int graphChanged = graph.size();
			report.addSubEntry(unit, graphCurrent, graphChanged, runtime);
			if (successful) {
				detectedRules.add(rule);
			}
		}

		final long runtime = (System.currentTimeMillis() - startTime);
		final int graphChanged = graph.size();

		report.finishEntry(graphInitially, graphChanged, runtime, detectedRules, set);

		// String resultPath = saveResult(report, exampleID, instance);
		// String referencePath = FILE_PATH + FILE_PATH_REFERENCE_OUTPUT + exampleID;
		// CorrectnessCheckUtil.performCorrectnessCheck(resultPath, referencePath,
		// FILE_EXTENSION_SYMMETRIC, report);
	}

}
