
package org.eclipse.emf.henshin.variability.multi.eval;

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
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.RuntimeBenchmarkReport;

public class UmlRecogBenchmarkClassic extends UmlRecogBenchmark {
	
	public static final String mode = "CLASSIC";
	
	static {
		FILE_PATH_OUTPUT = "output/classic/";
		FILE_PATH_RULES = "rules/";
	}	
		
	public static void main(String[] args) {
		HenshinResourceSet rs = init();
		Module module = LoadingHelper.loadAllRulesAsOneModule(rs, FILE_PATH, FILE_PATH_RULES, RuleSet.NOFILTER);
//		Module module = rs.getModule("rules\\generated\\CREATE\\rr_CREATE_Class_IN_Class_(nestedClassifier)_execute.henshin");
		
		// // Uncomment the following line to print statistics about the
		// transformation.
		// MaintainabilityBenchmarkUtil.runMaintainabilityBenchmark(module);

		RuntimeBenchmarkReport reporter = new RuntimeBenchmarkReport(UmlRecogBenchmarkClassic.class.getSimpleName(),
				FILE_PATH + FILE_PATH_OUTPUT);
		reporter.start();
		
		List<String> examples = LoadingHelper.getModelLocations(FILE_PATH, FILE_PATH_INSTANCES,
				FILE_NAME_INSTANCE_DIFF);

		for (String example : examples) {
			System.out.println("Run: "+example);
			try {
				new UmlRecogBenchmarkClassic().runPerformanceBenchmark(module, example, reporter);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Run the performance benchmark.
	 * 
	 * @param report
	 * 
	 * @param path
	 *            Relative path to the model files.
	 * @param iterations
	 *            Number of iterations.
	 * @throws IOException 
	 */
	@Override
	public void runPerformanceBenchmark(Module module, String exampleID, RuntimeBenchmarkReport report) throws IOException {
		report.beginNewEntry(exampleID);
		HenshinResourceSet rs = (HenshinResourceSet) module.eResource().getResourceSet();
	
		// Load the model into a graph:
		EObject instance = rs.getEObject(exampleID + "/" + FILE_NAME_INSTANCE_DIFF);
		EGraph graph = new EGraphImpl(instance);
		graph.addGraph(instance);
		
		ApplicationMonitor monitor = new BasicApplicationMonitor();
		
		int graphInitially = graph.size();
		Engine engine = new EngineImpl();
		// engine.getOptions().put(Engine. OPTION_SORT_VARIABLES, false);
		List<Rule> detectedRules = new ArrayList<>();
		
		System.gc();
		long startTime = System.currentTimeMillis();
	
		for (Unit unit : module.getUnits()) {
			Rule rule = (Rule) unit;
			long currentRunTime = System.currentTimeMillis();
			int graphCurrent = graph.size();
			boolean successful = false;
			
			List<Match> matches = InterpreterUtil.findAllMatches(engine, rule, graph, null);
			for (Match m : matches) {
				UnitApplication mainUnitApplication = new UnitApplicationImpl(engine, graph, unit, m);
				successful |= mainUnitApplication.execute(monitor);	
			}
			
			long runtime = (System.currentTimeMillis() - currentRunTime);
			int graphChanged = graph.size();
			report.addSubEntry(unit, graphCurrent, graphChanged, runtime);
			if (successful)
				detectedRules.add(rule);
		}
	
		long runtime = (System.currentTimeMillis() - startTime);
		int graphChanged = graph.size();
	
		report.finishEntry(graphInitially, graphChanged, runtime, detectedRules);
		
		
//		String resultPath = saveResult(report, exampleID, instance);
//		String referencePath = FILE_PATH + FILE_PATH_REFERENCE_OUTPUT + exampleID;
//		CorrectnessCheckUtil.performCorrectnessCheck(resultPath, referencePath, FILE_EXTENSION_SYMMETRIC, report);
	}
	
}
