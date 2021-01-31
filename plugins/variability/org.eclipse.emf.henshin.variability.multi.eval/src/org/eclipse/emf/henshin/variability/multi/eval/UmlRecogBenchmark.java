package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.Change.CompoundChange;
import org.eclipse.emf.henshin.interpreter.Change.ObjectChange;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.IBenchmarkReport;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.RuntimeBenchmarkReport;
import org.eclipse.uml2.uml.UMLPackage;

import com.google.gson.Gson;

import carisma.profile.umlsec.UmlsecPackage;
import carisma.profile.umlsec.variability.VariabilityPackage;
import symmetric.SemanticChangeSet;
import symmetric.SymmetricPackage;

public abstract class UmlRecogBenchmark {
	protected static String[] values = {
			"BMWExampleSPL", // 0
			"EndToEndSecurity", // 1
			//			"BCMS", // 2
			"jsse_openjdk", // 3
			"Notepad-Antenna", // 4
			"MobilePhoto07_OO", // 5
			"lampiro" // 6
	};

	protected static int maxRuns = 2;

	protected static boolean debug = false;

	protected static final String FILE_PATH = "umlrecog/";
	protected static final String FILE_NAME_INSTANCE_1 = "1.uml";
	protected static final String FILE_NAME_INSTANCE_2 = "2.uml";
	protected static final String FILE_NAME_INSTANCE_DIFF = "1-to-2.symmetric";
	protected static final String FILE_NAME_INSTANCE_FEATURE_MODEL = "model.fm.xml";

	public static void main(final String[] args) {
		if (args.length < 2) {
			printUsage();
			return;
		}
		final String benchmarkID = args[0];
		UmlRecogBenchmark benchmark;
		if (benchmarkID.equalsIgnoreCase("lifted")) {
			benchmark = new UmlRecogBenchmarkLifted();
		} else if (benchmarkID.equalsIgnoreCase("vb")) {
			benchmark = new UmlRecogBenchmarkVB();
		} else {
			printUsage();
			return;
		}
		String[] tmpValues;
		try {
			tmpValues = new String[] { values[Integer.parseInt(args[1])] };
		} catch (final NumberFormatException e) {
			printUsage();
			return;
		}
		for (int i = 2; i < args.length; i++) {
			switch (args[i]) {
			case ("--debug"):
				debug = true;
			break;
			case ("--runs"):
				maxRuns = Integer.parseInt(args[++i]);
			break;

			default:
				System.out.println("Unknown argument \"" + args[i] + '"');
				printUsage();
				return;
			}
		}
		values = tmpValues;
		System.out.println("Execute menchmar in "+benchmark.mode()+", writing data to \""+benchmark.getOutputPath()+'"');
		benchmark.run();
	}

	public static void printUsage() {
		System.out.println(
				"Please pass arguments as follows <\"lifted\"|\"vb\"> <exampleID> (--debug)? (--runs <number>)?.");
		System.out.println("Possible benchmark examples are:");
		for (int i = 0; i < values.length; i++) {
			System.out.println(i + ": " + values[i]);
		}
	}

	protected static HenshinResourceSet init() {
		UMLPackage.eINSTANCE.eClass();
		SymmetricPackage.eINSTANCE.eClass();
		UmlsecPackage.eINSTANCE.eClass();
		VariabilityPackage.eINSTANCE.eClass();
		final Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("symmetric", new XMIResourceFactoryImpl());
		m.put("uml", new XMIResourceFactoryImpl());

		// Create a resource set with a base directory:
		final HenshinResourceSet rs = new HenshinResourceSet(FILE_PATH);
		rs.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		rs.getPackageRegistry().put(SymmetricPackage.eINSTANCE.getNsURI(), SymmetricPackage.eINSTANCE);
		rs.getPackageRegistry().put(UmlsecPackage.eINSTANCE.getNsURI(), UmlsecPackage.eINSTANCE);
		rs.getPackageRegistry().put(VariabilityPackage.eINSTANCE.getNsURI(), VariabilityPackage.eINSTANCE);
		return rs;
	}

	String saveResult(final RuntimeBenchmarkReport runtimeBenchmarkReport, final String exampleID,
			final EObject instance) {
		final String outputPath = FILE_PATH + getOutputPath() +
				// runtimeBenchmarkReport.getDate() + "/" +
				exampleID;

		final HenshinResourceSet resourceSet = new HenshinResourceSet(new Path(outputPath).toOSString());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		resourceSet.saveEObject(instance, FILE_NAME_INSTANCE_DIFF);

		return outputPath;
	}

	protected abstract String getOutputPath();

	/**
	 * Searches the creation of a SemanticChangeSet and returns its name
	 *
	 * @param changes The set of changes in which should be searched
	 */
	protected static List<String> getNameOfAppliedRule(final Collection<Change> changes) {
		final List<String> names = new LinkedList<>();
		for (final Change change : changes) {
			if (change instanceof CompoundChange) {
				names.addAll(getNameOfAppliedRule(((CompoundChange) change).getChanges()));
			} else if (change instanceof ObjectChange) {
				final Object object = ((ObjectChange) change).getObject();
				if (object instanceof SemanticChangeSet) {
					names.add(((SemanticChangeSet) object).getName());
				}
			}
		}
		return names;
	}

	protected static void unload(HenshinResourceSet rs) {
		final Iterator<Resource> it = rs.getResources().iterator();
		while (it.hasNext()) {
			it.next().unload();
		}
		rs = null;
		System.gc();
	}

	public abstract void runPerformanceBenchmark(Module module, RuleSet set, String exampleID, File project,
			IBenchmarkReport report) throws IOException;

	/**
	 * @param set
	 * @param projectPath
	 * @return
	 */
	protected List<String> getExpectedApplications(final RuleSet set, final File projectPath) {
		List<String> expectedApplications;
		try {
			expectedApplications = new ArrayList<>(
					((Map<String, List<String>>) new Gson().fromJson(new FileReader("appliedRules.json"), Map.class)
							.get(projectPath.getName())).get(set.name()));
		} catch (final IOException e) {
			expectedApplications = null;
		}
		return expectedApplications;
	}

	public abstract void run();

	public abstract String mode();

}