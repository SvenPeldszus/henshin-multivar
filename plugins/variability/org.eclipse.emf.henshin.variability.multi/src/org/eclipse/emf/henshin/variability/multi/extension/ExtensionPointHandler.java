package org.eclipse.emf.henshin.variability.multi.extension;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;

public class ExtensionPointHandler {
	private static final String FEATUREMODEL_PROCESSOR_ID = "org.eclipse.emf.henshin.multi.processor";
	
	public static ArrayList<MultiVarProcessorExtension> getRegisteredProcessors() throws CoreException {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(FEATUREMODEL_PROCESSOR_ID);
		IExtension[] extensions = ep.getExtensions();
		ArrayList<MultiVarProcessorExtension> registeredProcessors = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			IExtension ext = extensions[i];
			IConfigurationElement[] ce = ext.getConfigurationElements();
			for (int j = 0; j < ce.length; j++) {
				Object obj = ce[j].createExecutableExtension("class");
				if (obj instanceof MultiVarProcessor) {
					String name = ce[j].getAttribute("name");
					Boolean usesEmbeddedFeatureModel = Boolean.valueOf(ce[j].getAttribute("hasExternalFeatureModel"));				
					registeredProcessors.add(new MultiVarProcessorExtension<>(name, usesEmbeddedFeatureModel, (MultiVarProcessor) obj));
				}
			}
		}
		return registeredProcessors;
	}
}
