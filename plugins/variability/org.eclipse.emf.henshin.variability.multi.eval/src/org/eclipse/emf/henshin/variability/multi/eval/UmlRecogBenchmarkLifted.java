
package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.FeatureModelHelper;
import org.eclipse.emf.henshin.variability.multi.MultiVarEGraph;
import org.eclipse.emf.henshin.variability.multi.MultiVarEngine;
import org.eclipse.emf.henshin.variability.multi.MultiVarExecution;
import org.eclipse.emf.henshin.variability.multi.SecPLUtil;
import org.eclipse.emf.henshin.variability.multi.eval.util.IBenchmarkReport;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.RuntimeBenchmarkReport;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

public class UmlRecogBenchmarkLifted extends UmlRecogBenchmark {

	public static final String mode = "LIFTED";
	private static final boolean SAVE = true;

	static {
		FILE_PATH_OUTPUT = "output/lifted/";
		FILE_PATH_RULES = "rules";
	}

	private static Map<String, Map<String, List<String>>> appliedRules = new HashMap<>();

	public static void main(String[] args) {
		FMCoreLibrary.getInstance().install();

		for (int choice = 0; choice < values.length; choice++) {
			for (RuleSet set : RuleSet.values()) {
				if (set == RuleSet.ALL || set == RuleSet.NOFILTER) {
					continue;
				}
				for (int i = 0; i <= MAX_RUNS; i++) {
					System.out.println(
							"[Info] Starting run " + i + " for " + set + " on " + UmlRecogBenchmark.values[choice]);
					HenshinResourceSet rs = init();
					Module module = LoadingHelper.loadAllRulesAsOneModule(rs, FILE_PATH, FILE_PATH_RULES, 2, set);
					// module.getUnits().removeIf(u ->
					// !u.getName().contains("createAssociation_IN_Class"));

					String projectName = values[choice];
					File projectPath = new File(FILE_PATH + "prepared/" + projectName);

					IBenchmarkReport reporter = new RuntimeBenchmarkReport(projectPath,
							UmlRecogBenchmarkClassic.class.getSimpleName(),
							FILE_PATH + FILE_PATH_OUTPUT + projectName + "/" + set + "/");
					reporter.start();

					List<String> examples = LoadingHelper.getModelLocations(FILE_PATH, "prepared/" + projectName + "/",
							FILE_NAME_INSTANCE_DIFF);

					for (String example : examples) {
						new UmlRecogBenchmarkLifted().runPerformanceBenchmark(module, set, example, projectPath,
								reporter);
					}

					unload(rs);
				}
			}
		}
		if (SAVE) {
			try (FileWriter fileWriter = new FileWriter("appliedRules.json")) {
				new Gson().toJson(appliedRules, fileWriter);
			} catch (JsonIOException | IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void runPerformanceBenchmark(Module module, RuleSet set, String exampleID, File projectPath,
			IBenchmarkReport report) {
		report.beginNewEntry(exampleID);
		HenshinResourceSet rs = (HenshinResourceSet) module.eResource().getResourceSet();

		// Load the model into a graph:
		Path fmFile = new File(projectPath, FILE_NAME_INSTANCE_FEATURE_MODEL).toPath();
		IFeatureModel modelFM = FeatureModelManager.load(fmFile);
		String fmCNF = FeatureModelHelper.getFMExpressionAsCNF(modelFM);

		Resource res1 = rs.getResource(exampleID + "/" + FILE_NAME_INSTANCE_1);
		Resource res2 = rs.getResource(exampleID + "/" + FILE_NAME_INSTANCE_2);
		EObject diff = rs.getEObject(exampleID + "/" + FILE_NAME_INSTANCE_DIFF);
		EcoreUtil.resolveAll(rs);

		List<EObject> roots = new ArrayList<>();
		roots.addAll(res1.getContents());
		roots.addAll(res2.getContents());
		roots.add(diff);
		Map<EObject, String> presenceConditions = new HashMap<>();
		MultiVarEGraph graph = new SecPLUtil().createEGraphAndCollectPCs(roots, presenceConditions, fmCNF);

		int graphInitially = graph.size();
		MultiVarEngine engine = new MultiVarEngine();

		// engine.getOptions().put(Engine. OPTION_SORT_VARIABLES, false);
		List<Rule> detectedRules = new ArrayList<>();
		List<String> expectedApplications = getExpectedApplications(set, projectPath);

		System.gc();
		long startTime = System.currentTimeMillis();

		for (Unit unit : module.getUnits()) {
			Rule rule = (Rule) unit;
			if (DEBUG) {
				System.out.println("Rule: " + rule);
			}

			long currentRunTime = System.currentTimeMillis();
			int graphInitial = graph.size();

			Collection<Change> changes = new MultiVarExecution(new SecPLUtil()).transformSPLWithClassicRule(roots,
					rule, engine, graph);

			long runtime = (System.currentTimeMillis() - currentRunTime);
			int graphChanged = graph.size();
			report.addSubEntry(unit, graphInitial, graphChanged, runtime);
			if (DEBUG || SAVE) {
				for (int i = graphInitial; i < graphChanged; i++) {
					detectedRules.add(rule);
				}
				List<String> names = getNameOfAppliedRule(changes);
				for (String name : names) {
					name = name.toLowerCase();
					appliedRules.computeIfAbsent(projectPath.getName(), x -> new HashMap<>())
					.computeIfAbsent(set.name(), y -> new LinkedList<>()).add(name);
				}
			}

			if (expectedApplications != null) {
				List<String> names = getNameOfAppliedRule(changes);
				for (String name : names) {
					name = name.toLowerCase();
					if (!expectedApplications.remove(name)) {
						System.err.println("FP: " + name);
					}
				}
			}
		}

		long runtime = (System.currentTimeMillis() - startTime);
		int graphChanged = graph.size();

		if (expectedApplications != null) {
			expectedApplications.forEach(s -> System.err.println("FN: " + s));
		}

		report.finishEntry(graphInitially, graphChanged, runtime, detectedRules, set);

	}
}
