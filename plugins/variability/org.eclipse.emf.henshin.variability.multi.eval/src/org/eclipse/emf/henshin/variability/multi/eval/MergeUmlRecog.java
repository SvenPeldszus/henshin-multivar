package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.mergein.clone.CloneGroup;
import org.eclipse.emf.henshin.variability.mergein.clone.CloneGroupDetectionResult;
import org.eclipse.emf.henshin.variability.mergein.clone.ConqatBasedCloneGroupDetector;
import org.eclipse.emf.henshin.variability.mergein.clustering.Diagnostic;
import org.eclipse.emf.henshin.variability.mergein.clustering.GreedySubCloneClustererStaticThreshold;
import org.eclipse.emf.henshin.variability.mergein.clustering.MergeClusterer;
import org.eclipse.emf.henshin.variability.mergein.evaluation.RuleSetMetricsCalculator;
import org.eclipse.emf.henshin.variability.mergein.refactoring.logic.MergeInException;
import org.eclipse.emf.henshin.variability.mergein.refactoring.logic.NewMerger;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.uml2.uml.UMLPackage;

import mergeSuggestion.MergeRule;
import mergeSuggestion.MergeSuggestion;
import mergeSuggestion.MergeSuggestionPackage;
import symmetric.SymmetricPackage;

public class MergeUmlRecog {

	/**
	 * 
	 */
	private static final RuleSet KIND = RuleSet.MOVE;
	
	// private static final boolean CREATE = false; private static final boolean
	// MERGE = false;
	private static final boolean CREATE = true;
	private static final boolean MERGE = true;
	private static final boolean LOAD = false;
	private static final boolean RUN = true;
	
	private static final boolean PERFORMANCE_OPTIMIZED = true;

	private static final String FILE_PATH = "umlrecog/";
	private static final String FILE_PATH_RULES = "rules";
	private static final String FILE_PATH_RULES_RESULT = "vbrules/"+KIND.toString()+'/';
	private static final String FILE_PATH_EXPERIMENTAL_DATA = "merge/";
	private static final String FILE_EXTENSION_HENSHIN = ".henshin";
	private static final String FILE_NAME_MERGE_SUGGESTION = "mergeSuggestion.xmi";

	private static double clusterThreshold = 0.6;
	private static int minSubCloneSize = 6;
	private static boolean includeRhs = false;

	@SuppressWarnings({ "unused", "deprecation" })
	public static void main(String[] args) {
		String expDataPath = FILE_PATH_EXPERIMENTAL_DATA + clusterThreshold + " "
				+ (includeRhs ? "includeRhs" : "noRhs") + "/";
		// String expDataPath = FILE_PATH_EXPERIMENTAL_DATA + "classic/";
		String tracePath = expDataPath;
		deleteFolder(new File(new File(FILE_PATH) , FILE_PATH_RULES_RESULT));

		HenshinResourceSet rs = new HenshinResourceSet(FILE_PATH);
		initialize(rs);
		StringBuilder trace = new StringBuilder();
		RuleSetMetricsCalculator calc = new RuleSetMetricsCalculator();

		Module module = null;
		CloneGroupDetectionResult cloneGroupDetectionResult = null;

		// module = createDummyModule(rs);
		module = LoadingHelper.loadAllRulesAsOneModule(rs, FILE_PATH, FILE_PATH_RULES, 2, KIND);
		List<Rule> rules = module.getAllRules();
		String pre = calc.calculcate(rules).createPresentationString();

		long start = System.currentTimeMillis();
		System.out.println("[Info] Begin clone detection.");
		ConqatBasedCloneGroupDetector cq = new ConqatBasedCloneGroupDetector(rules, minSubCloneSize, includeRhs);
		cq.detectCloneGroups();
		System.out.println("[Info] Found " + cq.getResultOrderedByNumberOfCommonElements().getCloneGroups().size()
				+ " clones in " + (System.currentTimeMillis() - start) / 1000.0 + " s.");
		cloneGroupDetectionResult = cq.getResultOrderedByNumberOfCommonElements();
		prefilterCloneDetectionResult(cloneGroupDetectionResult);

		pre = calc.calculcate(rules).createPresentationString();

		println("Merging with cluster threshold = " + clusterThreshold + ", includeRhs = " + includeRhs
				+ ", minSubCloneSize = " + minSubCloneSize + "\n", trace);
		MergeSuggestion result = null;
		if (CREATE) {
			start = System.currentTimeMillis();

			System.out.println("[Info] Starting merge construction.");
			MergeClusterer mc = new GreedySubCloneClustererStaticThreshold(cloneGroupDetectionResult, 0,
					clusterThreshold, includeRhs);

			result = mc.createMergeSuggestion();
			rs.saveEObject(result, FILE_NAME_MERGE_SUGGESTION);
			rs.saveEObject(result, expDataPath + FILE_NAME_MERGE_SUGGESTION);
			Diagnostic.findInconsistencies(result);
			println("[Info] Created merge suggestion in " + (System.currentTimeMillis() - start) + " ms.", trace);
		} else {
			result = (MergeSuggestion) rs.getEObject(FILE_NAME_MERGE_SUGGESTION);
		}

		List<Rule> resultRules = new ArrayList<>();
		if (MERGE) {
			try {
				Set<Rule> notmerged = new HashSet<>(module.getAllRules());
				for (MergeRule mergeRule : result.getMergeClusters()) {

					NewMerger merger = new NewMerger(mergeRule, false, PERFORMANCE_OPTIMIZED);
					merger.merge();
					Rule masterRule = mergeRule.getMasterRule();
					Diagnostic.findDanglingObject(mergeRule.getMasterRule());

					Module newModule = HenshinFactory.eINSTANCE.createModule();
					newModule.getImports().addAll(module.getImports());
					newModule.getUnits().add(masterRule);
					String oldName = masterRule.getName();
					String name = oldName.substring(0, Math.min(50, oldName.length())).replaceAll("[^A-Za-z0-9]", "");
					rs.saveEObject(newModule, FILE_PATH_RULES_RESULT + name + "-var" + FILE_EXTENSION_HENSHIN);
					notmerged.removeAll(mergeRule.getRules());
					resultRules.add(masterRule);
//					System.out.println(masterRule.getName() +": "+ mergeRule.getRules());
				}

				for (Rule rule : notmerged) {
					Module newModule = HenshinFactory.eINSTANCE.createModule();
					newModule.getImports().addAll(module.getImports());
					newModule.getUnits().add(rule);
					rs.saveEObject(newModule, FILE_PATH_RULES_RESULT + rule.getName().replaceAll("[^A-Za-z0-9]", "")
							+ FILE_EXTENSION_HENSHIN);
					resultRules.add(rule);
				}

			} catch (MergeInException e) {
				e.printStackTrace();
			}
		}

		// } else if (LOAD) {
		// module = (Module) rs
		// .getEObject(FILE_PATH_RULES_RESULT + FILE_NAME_RULES_RESULT + "-var"
		// + FILE_EXTENSION_HENSHIN);
		// }
		println("[Info] Merged rule set.", trace);
		println("[Info] Old rule set: " + pre, trace);
		println("[Info] New rule set: " + calc.calculcate(resultRules).createPresentationString(), trace);

		if (RUN) {
			// String executionResult =
			String[] bla = null;
			UmlRecogBenchmarkVB.main(bla);
			// executionTrace.append(executionResult);
			// try {
			//// BufferedWriter writer = new BufferedWriter(new
			// FileWriter("./"+
			//// "recognitionrules/"
			//// + tracePath + "trace " + i + ".txt"));
			// writer.write(executionTrace.toString());
			// writer.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

		}

	}

	private static void prefilterCloneDetectionResult(CloneGroupDetectionResult cloneGroupDetectionResult) {
		List<CloneGroup> del = new ArrayList<CloneGroup>();
		for (CloneGroup cg : cloneGroupDetectionResult.getCloneGroups()) {
			Set<EPackage> packages = cg.getNodeMappings().keySet().stream().map(n -> n.getType())
					.map(e -> e.getEPackage()).collect(Collectors.toSet());
			if (packages.contains(SymmetricPackage.eINSTANCE))
				continue;
			del.add(cg);
		}
		System.out.println("Removed " + del.size() + " clone groups");
		cloneGroupDetectionResult.getCloneGroups().removeAll(del);
	}

	private Module createDummyModule(HenshinResourceSet rs) {
		Module module;
		module = rs.getModule("rules\\generated\\CREATE\\rr_CREATE_Class_IN_Class_(nestedClassifier)_execute.henshin");
		Copier copier = new Copier();
		Rule rule2 = (Rule) copier.copy(module.getAllRules().get(0));
		copier.copyReferences();
		module.getUnits().add(rule2);
		return module;
	}

	private static void println(String string, StringBuilder trace) {
		System.out.println(string);
		trace.append(string);
		trace.append('\n');
	}

	private void removeRedundantRules(Module module, List<Rule> rules, MergeSuggestion result) {
		for (Rule rule : rules) {
			if (result.findMergeRule(rule).getMasterRule() != rule)
				module.getUnits().remove(rule);
		}
	}

	private static void initialize(HenshinResourceSet rs) {
		UMLPackage.eINSTANCE.eClass();
		MergeSuggestionPackage.eINSTANCE.eClass();
		SymmetricPackage.eINSTANCE.eClass();
		rs.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		rs.getPackageRegistry().put(MergeSuggestionPackage.eINSTANCE.getNsURI(), MergeSuggestionPackage.eINSTANCE);
		rs.getPackageRegistry().put(SymmetricPackage.eINSTANCE.getNsURI(), SymmetricPackage.eINSTANCE);
	}

	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
}