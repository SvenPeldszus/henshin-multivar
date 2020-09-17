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
public class VBRuleFMValidator extends AbstractVBValidator {

	@Override
	public IStatus validate(EObject eObject) {
		Rule rule = (Rule) eObject;
		return validateFeatureModel(rule);

	}

	/**
	 * Validates the feature model of the rule
	 * 
	 * @param rule A rule
	 * @return If the fm is valid
	 */
	public static IStatus validateFeatureModel(Rule rule) {
		EList<Annotation> annotations = rule.getAnnotations();
		if (!annotations.isEmpty()) {
			String fm = VariabilityHelper.INSTANCE.getFeatureConstraint(rule);
			Set<String> features = VariabilityHelper.INSTANCE.getFeatures(rule);

			if (features == null && fm == null) {
				return Status.OK_STATUS;
			}

			if (fm == null || fm.trim().length() == 0) {
				return new Status(Status.ERROR, "TODO", "The feature model is empty");
			}

			IStatus fmOk = checkConstraint(features, fm);
			if (!fmOk.isOK()) {
				return fmOk;
			}

		}
		return Status.OK_STATUS;
	}
}
