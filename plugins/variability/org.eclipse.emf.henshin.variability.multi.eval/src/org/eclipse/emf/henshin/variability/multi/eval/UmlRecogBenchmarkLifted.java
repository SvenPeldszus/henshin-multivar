
package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.RuleToProductLineEngine;
import org.eclipse.emf.henshin.variability.multi.SecPLUtil;
import org.eclipse.emf.henshin.variability.multi.VBExecution;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.RuntimeBenchmarkReport;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

public class UmlRecogBenchmarkLifted extends UmlRecogBenchmark {

	public static final String mode = "LIFTED";

	static {
		FILE_PATH_OUTPUT = "output/lifted/";
		FILE_PATH_RULES = "rules";
	}

	public static void main(String[] args) {
		FMCoreLibrary.getInstance().install();
		
		for (RuleSet set : RuleSet.values()) {
			if (set == RuleSet.ALL || set == RuleSet.NOFILTER) {
				continue;
			}
			for (int i = 0; i <= MAX_RUNS; i++) {
				System.out.println("[Info] Starting run " + i + " for " + set + " on "
						+ UmlRecogBenchmark.values[UmlRecogBenchmark.choice]);
				HenshinResourceSet rs = init();
				Module module = LoadingHelper.loadAllRulesAsOneModule(rs, FILE_PATH, FILE_PATH_RULES, 2, set);
				// module.getUnits().removeIf(u ->
				// !u.getName().contains("createAssociation_IN_Class"));

				RuntimeBenchmarkReport reporter = new RuntimeBenchmarkReport(
						UmlRecogBenchmarkClassic.class.getSimpleName(),
						FILE_PATH + FILE_PATH_OUTPUT + values[choice] + "/" + set + "/");
				reporter.start();

				List<String> examples = LoadingHelper.getModelLocations(FILE_PATH, FILE_PATH_INSTANCES,
						FILE_NAME_INSTANCE_DIFF);

				for (String example : examples) {
					new UmlRecogBenchmarkLifted().runPerformanceBenchmark(module, example, reporter);
				}
			}
		}

	}

	@Override
	public void runPerformanceBenchmark(Module module, String exampleID, RuntimeBenchmarkReport report) {
		report.beginNewEntry(exampleID);
		HenshinResourceSet rs = (HenshinResourceSet) module.eResource().getResourceSet();

		// Load the model into a graph:
		Path fmFile = new File(FILE_PATH + FILE_PATH_INSTANCES + FILE_NAME_INSTANCE_FEATURE_MODEL).toPath();
		IFeatureModel modelFM = FeatureModelManager.load(fmFile);

		Resource res1 = rs.getResource(exampleID + "/" + FILE_NAME_INSTANCE_1);
		Resource res2 = rs.getResource(exampleID + "/" + FILE_NAME_INSTANCE_2);
		EObject diff = rs.getEObject(exampleID + "/" + FILE_NAME_INSTANCE_DIFF);
		EcoreUtil.resolveAll(rs);
		
		List<EObject> roots = new ArrayList<EObject>();
		roots.addAll(res1.getContents());
		roots.addAll(res2.getContents());
		roots.add(diff);
		Map<EObject, String> presenceConditions = new HashMap<>();
		EGraphImpl graph = new SecPLUtil().createEGraphAndCollectPCs(roots, presenceConditions);

		int graphInitially = graph.size();
		RuleToProductLineEngine engine = new RuleToProductLineEngine();

		// engine.getOptions().put(Engine. OPTION_SORT_VARIABLES, false);
		List<Rule> detectedRules = new ArrayList<Rule>();

		System.gc();
		long startTime = System.currentTimeMillis();

		for (Unit unit : module.getUnits()) {
			Rule rule = (Rule) unit;

			long currentRunTime = System.currentTimeMillis();
			int graphCurrent = graph.size();
			boolean successful = false;

			new VBExecution(new SecPLUtil()).transformSecPlModelWithClassicRule(modelFM, roots, rule, engine, presenceConditions, graph);

			long runtime = (System.currentTimeMillis() - currentRunTime);
			int graphChanged = graph.size();
			report.addSubEntry(unit, graphCurrent, graphChanged, runtime);
			if (successful)
				detectedRules.add(rule);
		}

		long runtime = (System.currentTimeMillis() - startTime);
		int graphChanged = graph.size();

		report.finishEntry(graphInitially, graphChanged, runtime, detectedRules);
	}
}
