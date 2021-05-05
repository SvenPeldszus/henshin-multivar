package org.eclipse.emf.henshin.variability.multi.extension;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;

public class ExtensionPointHandler {
	private static final String FEATUREMODEL_PROCESSOR_ID = "org.eclipse.emf.henshin.multi.processor";

	public static List<MultiVarProcessorExtension> getRegisteredProcessors() throws CoreException {
		final IExtensionRegistry reg = Platform.getExtensionRegistry();
		final IExtensionPoint ep = reg.getExtensionPoint(FEATUREMODEL_PROCESSOR_ID);
		final IExtension[] extensions = ep.getExtensions();
		final List registeredProcessors = new ArrayList();
		for (final IExtension ext : extensions) {
			final IConfigurationElement[] ce = ext.getConfigurationElements();
			for (final IConfigurationElement element : ce) {
				final Object obj = element.createExecutableExtension("class");
				if (obj instanceof MultiVarProcessor) {
					final String name = element.getAttribute("name");
					final Boolean usesEmbeddedFeatureModel = Boolean.valueOf(element.getAttribute("hasExternalFeatureModel"));
					registeredProcessors.add(new MultiVarProcessorExtension<>(name, usesEmbeddedFeatureModel, (MultiVarProcessor) obj));
				}
			}
		}
		return registeredProcessors;
	}
}
