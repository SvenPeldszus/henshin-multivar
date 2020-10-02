package org.eclipse.emf.henshin.variability.ui.wizard;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.henshin.interpreter.ui.wizard.HenshinWizard;
import org.eclipse.emf.henshin.interpreter.ui.wizard.HenshinWizardFactory;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityConstants;

public class VariabilityHenshinFactory implements HenshinWizardFactory {
	
	boolean containsVariability(Unit unit, boolean checkSubUnits) {
		EList<Annotation> annotations = unit.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation.getKey().equals(VariabilityConstants.FEATURE_CONSTRAINT.toString())) {
				return true;
			}
		}
		
		if (checkSubUnits) {
			EList<Unit> subUnits = unit.getSubUnits(true);
			for (Unit subUnit : subUnits) {
				if (containsVariability(subUnit, false)) {		
					return true;
				}
			}
		}
		return false;
	}
	
	boolean containsVariability(Module module) {
		EList<Rule> rules = module.getAllRules();
		for (Rule rule : rules) {
			if (containsVariability(rule, true)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public HenshinWizard createWizard(Module module) {
		return new VariabilityAwareHenshinWizard(module);
	}

	@Override
	public HenshinWizard createWizard(Unit unit) {
		return new VariabilityAwareHenshinWizard(unit);
	}

	@Override
	public boolean canExecute(Module module) {
		return containsVariability(module);
	}

	@Override
	public boolean canExecute(Unit unit) {
		return containsVariability(unit, true);
	}

}
