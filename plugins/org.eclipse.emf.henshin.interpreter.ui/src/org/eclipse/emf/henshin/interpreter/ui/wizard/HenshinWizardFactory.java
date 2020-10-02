package org.eclipse.emf.henshin.interpreter.ui.wizard;

import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;

public interface HenshinWizardFactory {
	public HenshinWizard createWizard(Module module);
	public HenshinWizard createWizard(Unit unit);
	public boolean canExecute(Module module);
	public boolean canExecute(Unit unit);
}
