package org.eclipse.emf.henshin.interpreter.ui.wizard;

import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;

/**
 * The default factory to be used if no custom wizards are registered to the extension point org.eclipse.emf.henshin.wizard.
 * 
 * @author Stefan Schulz
 */
public class DefaultHenshinWizardFactory implements HenshinWizardFactory {
	
	private static HenshinWizardFactory instance;
	protected static HenshinWizardFactory getInstance() {
		if (instance == null) {
			instance = new DefaultHenshinWizardFactory();
		}
		return instance;
	}

	@Override
	public HenshinWizard createWizard(Module module) {
		return new HenshinWizard(module);
	}

	@Override
	public HenshinWizard createWizard(Unit unit) {
		return new HenshinWizard(unit);
	}

	@Override
	public boolean canExecute(Module module) {
		return false;
	}

	@Override
	public boolean canExecute(Unit unit) {
		return false;
	}
}
