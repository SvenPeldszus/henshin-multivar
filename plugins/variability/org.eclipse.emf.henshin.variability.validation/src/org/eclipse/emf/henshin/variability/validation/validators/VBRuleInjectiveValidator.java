package org.eclipse.emf.henshin.variability.validation.validators;

import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.AbstractVBValidator;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

/**
 * A validator for VB rules
 * 
 * @author speldszus
 *
 */
public class VBRuleInjectiveValidator extends AbstractVBValidator {

	@Override
	public IStatus validate(EObject eObject) {
		Rule rule = (Rule) eObject;
		return validateInjectiveMatchingPC(rule);

	}

	/**
	 * Validates the injective matching PC
	 * 
	 * @param rule A rule
	 * @return If the pc is valid
	 */
	public static IStatus validateInjectiveMatchingPC(Rule rule) {
		EList<Annotation> annotations = rule.getAnnotations();
		if (!annotations.isEmpty()) {
			String pc = VariabilityHelper.INSTANCE.getInjectiveMatchingPresenceCondition(rule);
			Set<String> features = VariabilityHelper.INSTANCE.getFeatures(rule);
			
			if (features == null && pc == null) {
				return Status.OK_STATUS;
			}

			if (pc == null || pc.trim().length() == 0) {
				return new Status(IStatus.WARNING, "TODO", "The injective matching PC is empty, assuming default value!");
			}
			IStatus pcOk = checkConstraint(features, pc);
			if (!pcOk.isOK()) {
				return pcOk;
			}
		}
		return Status.OK_STATUS;
	}
}
