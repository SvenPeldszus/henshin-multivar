package org.eclipse.emf.henshin.interpreter.ui.wizard;

import org.eclipse.emf.henshin.model.Unit;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.henshin.model.Module;

/**
 * This class helps selecting a HenshinWizard for a given unit.
 * New wizards can be added via the extension point org.eclipse.emf.henshin.wizard.
 * 
 * @author Stefan Schulz
 */
public class HenshinWizardSelector {

	private static ArrayList getContributors() throws CoreException {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint("org.eclipse.emf.henshin.wizard");
		IExtension[] extensions = ep.getExtensions();
		ArrayList contributors = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			IExtension ext = extensions[i];
			IConfigurationElement[] ce = ext.getConfigurationElements();
			for (int j = 0; j < ce.length; j++) {
				Object obj = ce[j].createExecutableExtension("class");
				contributors.add(obj);
			}
		}
		return contributors;
	}
	
	private static HenshinWizardFactory selectFactory(Unit unit) {
		try {
			ArrayList contributors = getContributors();
			if (!contributors.isEmpty()) {
				for (Object contributor : contributors) {
					HenshinWizardFactory wizardFactory = (HenshinWizardFactory) contributor;
					if (wizardFactory.canExecute(unit)) {
						return wizardFactory;
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return DefaultHenshinWizardFactory.getInstance();
	}
	
	private static HenshinWizardFactory selectFactory(Module module) {
		try {
			ArrayList contributors = getContributors();
			if (!contributors.isEmpty()) {
				for (Object contributor : contributors) {
					HenshinWizardFactory wizardFactory = (HenshinWizardFactory) contributor;
					if (wizardFactory.canExecute(module)) {
						return wizardFactory;
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return DefaultHenshinWizardFactory.getInstance();
	}

	public static HenshinWizard getWizard(Unit unit) {
		HenshinWizardFactory factory = selectFactory(unit);
		return factory.createWizard(unit);
	}
	
	public static HenshinWizard getWizard(Module module) {
		HenshinWizardFactory factory = selectFactory(module);
		return factory.createWizard(module);
	}

}
