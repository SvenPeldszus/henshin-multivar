package org.eclipse.emf.henshin.variability.multi.extension;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;

public class MultiVarProcessorExtension<F> {
	
	private final String name;
	private final boolean hasExternalFeatureModel;
	private final MultiVarProcessor<EPackage, F> processor;
	
	public MultiVarProcessorExtension(String name, boolean hasExternalFeatureModel, MultiVarProcessor processor) {
		this.name = name;
		this.hasExternalFeatureModel = hasExternalFeatureModel;
		this.processor = processor;		
	}

	public String getName() {
		return name;
	}

	public boolean hasExternalFeatureModel() {
		return hasExternalFeatureModel;
	}

	public MultiVarProcessor<EPackage, F> getProcessor() {
		return processor;
	}
}
