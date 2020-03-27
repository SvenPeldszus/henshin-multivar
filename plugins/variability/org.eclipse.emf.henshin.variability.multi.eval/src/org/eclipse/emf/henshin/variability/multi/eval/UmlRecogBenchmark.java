package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.eval.util.RuntimeBenchmarkReport;
import org.eclipse.uml2.uml.UMLPackage;

import carisma.profile.umlsec.UmlsecPackage;
import carisma.profile.umlsec.variability.VariabilityPackage;
import symmetric.SymmetricPackage;

public abstract class UmlRecogBenchmark {
	protected static final String[] values = {
			"BMWExampleSPL",	// 0
			"EndToEndSecurity",	// 1
			"BCMS",				// 2
			"jsse_openjdk",		// 3
			"Notepad-Antenna",	// 4
			"MobilePhoto07_OO",	// 5
			"lampiro"			// 6
			};
	protected static final int choice = 1;
	
	protected static final int MAX_RUNS = 1;
	
	protected static final String FILE_PATH = "umlrecog/";
	protected static final String FILE_PATH_INSTANCES = "prepared/"+values[choice]+"/";
	protected static final String FILE_NAME_INSTANCE_1 = "1.uml";
	protected static final String FILE_NAME_INSTANCE_2 = "2.uml";
	protected static final String FILE_NAME_INSTANCE_DIFF = "1-to-2.symmetric";
	protected static final String FILE_NAME_INSTANCE_FEATURE_MODEL = "model.fm.xml";

	protected static String FILE_PATH_OUTPUT, FILE_PATH_RULES;
	
	protected static HenshinResourceSet init() {
		UMLPackage.eINSTANCE.eClass();
		SymmetricPackage.eINSTANCE.eClass();
		UmlsecPackage.eINSTANCE.eClass();
		VariabilityPackage.eINSTANCE.eClass();
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("symmetric", new XMIResourceFactoryImpl());
		m.put("uml", new XMIResourceFactoryImpl());
	
		// Create a resource set with a base directory:
		HenshinResourceSet rs = new HenshinResourceSet(FILE_PATH);
		rs.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		rs.getPackageRegistry().put(SymmetricPackage.eINSTANCE.getNsURI(), SymmetricPackage.eINSTANCE);
		rs.getPackageRegistry().put(UmlsecPackage.eINSTANCE.getNsURI(), UmlsecPackage.eINSTANCE);
		rs.getPackageRegistry().put(VariabilityPackage.eINSTANCE.getNsURI(), VariabilityPackage.eINSTANCE);
		return rs;
	}

	static String saveResult(RuntimeBenchmarkReport runtimeBenchmarkReport, String exampleID, EObject instance) {
		String outputPath = FILE_PATH + FILE_PATH_OUTPUT + 
				//runtimeBenchmarkReport.getDate() + "/" + 
				exampleID;
	
		HenshinResourceSet resourceSet = new HenshinResourceSet(new Path(outputPath).toOSString());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		resourceSet.saveEObject(instance, FILE_NAME_INSTANCE_DIFF);
	
		return outputPath;
	}

	public abstract void runPerformanceBenchmark(Module module, String exampleID, RuntimeBenchmarkReport report) throws IOException;
		

}