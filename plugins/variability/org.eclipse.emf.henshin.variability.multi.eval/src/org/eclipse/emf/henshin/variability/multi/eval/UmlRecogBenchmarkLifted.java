
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
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
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

	@Override
	public String mode() {
		return "LIFTED";
	}

	private static final boolean SAVE = false;


	private final static String FILE_PATH_RULES = "rules";


	private static Map<String, Map<String, List<String>>> appliedRules = new HashMap<>();

	public static void main(final String[] args) {
		new UmlRecogBenchmarkLifted().run();
	}

	@Override
	public void run() {
		FMCoreLibrary.getInstance().install();

		for (int choice = 0; choice < values.length; choice++) {
			for (final RuleSet set : RuleSet.values()) {
				if ((set == RuleSet.ALL) || (set == RuleSet.NOFILTER)) {
					continue;
				}
				for (int i = 0; i <= maxRuns; i++) {
					System.out.println(
							"[Info] Starting run " + i + " for " + set + " on " + UmlRecogBenchmark.values[choice]);
					final HenshinResourceSet rs = init();
					final Module module = LoadingHelper.loadAllRulesAsOneModule(rs, FILE_PATH, FILE_PATH_RULES, 2, set);
					// module.getUnits().removeIf(u ->
					// !u.getName().contains("createAssociation_IN_Class"));

					final String projectName = values[choice];
					final File projectPath = new File(FILE_PATH + "prepared/" + projectName);

					final IBenchmarkReport reporter = new RuntimeBenchmarkReport(projectPath,
							UmlRecogBenchmarkClassic.class.getSimpleName(),
							FILE_PATH + getOutputPath() + projectName + "/" + set + "/");
					reporter.start();

					final List<String> examples = LoadingHelper.getModelLocations(FILE_PATH, "prepared/" + projectName + "/",
							FILE_NAME_INSTANCE_DIFF);

					for (final String example : examples) {
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
	protected String getOutputPath() {
		return "output/lifted/";
	}

	@Override
	public void runPerformanceBenchmark(final Module module, final RuleSet set, final String exampleID, final File projectPath,
			final IBenchmarkReport report) {
		report.beginNewEntry(exampleID);
		final HenshinResourceSet rs = (HenshinResourceSet) module.eResource().getResourceSet();

		// Load the model into a graph:
		final Path fmFile = new File(projectPath, FILE_NAME_INSTANCE_FEATURE_MODEL).toPath();
		final IFeatureModel modelFM = FeatureModelManager.load(fmFile);

		final Resource res1 = rs.getResource(exampleID + "/" + FILE_NAME_INSTANCE_1);
		final Resource res2 = rs.getResource(exampleID + "/" + FILE_NAME_INSTANCE_2);
		final EObject diff = rs.getEObject(exampleID + "/" + FILE_NAME_INSTANCE_DIFF);
		//		EcoreUtil.resolveAll(rs);

		final List<EObject> roots = new ArrayList<>(res1.getContents());
		roots.addAll(res2.getContents());
		roots.add(diff);
		final MultiVarEGraph graph = new SecPLUtil().createEGraphAndCollectPCs(roots, modelFM);

		final int graphInitially = graph.size();
		final MultiVarEngine engine = new MultiVarEngine();

		// engine.getOptions().put(Engine. OPTION_SORT_VARIABLES, false);
		final List<Rule> detectedRules = new ArrayList<>();
		final List<String> expectedApplications = getExpectedApplications(set, projectPath);

		System.gc();
		final long startTime = System.currentTimeMillis();

		for (final Unit unit : module.getUnits()) {
			final Rule rule = (Rule) unit;
			if (debug) {
				System.out.println("Rule: " + rule);
			}

			final long currentRunTime = System.currentTimeMillis();
			final int graphInitial = graph.size();

			Collection<Change> changes;
			try {
				changes = new MultiVarExecution(new SecPLUtil(), engine).transformSPL(rule, graph);
			} catch (final InconsistentRuleException e) {
				throw new IllegalStateException(e);
			}

			final long runtime = (System.currentTimeMillis() - currentRunTime);
			final int graphChanged = graph.size();
			report.addSubEntry(unit, graphInitial, graphChanged, runtime);
			if (debug || SAVE) {
				for (int i = graphInitial; i < graphChanged; i++) {
					detectedRules.add(rule);
				}
				final List<String> names = getNameOfAppliedRule(changes);
				for (String name : names) {
					name = name.toLowerCase();
					appliedRules.computeIfAbsent(projectPath.getName(), x -> new HashMap<>())
					.computeIfAbsent(set.name(), y -> new LinkedList<>()).add(name);
				}
			}

			if (expectedApplications != null) {
				final List<String> names = getNameOfAppliedRule(changes);
				for (String name : names) {
					name = name.toLowerCase();
					if (!expectedApplications.remove(name)) {
						System.err.println("FP: " + name);
					}
				}
			}
		}

		final long runtime = (System.currentTimeMillis() - startTime);
		final int graphChanged = graph.size();

		if (expectedApplications != null) {
			expectedApplications.forEach(s -> System.err.println("FN: " + s));
		}

		report.finishEntry(graphInitially, graphChanged, runtime, detectedRules, set);


		graph.clear();
		res1.unload();
		res2.unload();
	}
}
