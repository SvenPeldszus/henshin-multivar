package org.eclipse.emf.henshin.variability.validation.validators;

import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.AbstractVBValidator;
import org.eclipse.emf.henshin.variability.validation.exceptions.VBNotAplicibleException;
import org.eclipse.emf.henshin.variability.validation.exceptions.VBValidationException;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityConstants;

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
			String fm = null;
			try {
				fm = getFM(rule);
			} catch (VBValidationException e) {
				return new Status(Status.ERROR, "TODO", e.getMessage());
			} catch (VBNotAplicibleException e) {
				// Ignore this exception
			}
			
			List<String> features = null;
			try {
				features = getFeatures(rule);
			} catch (VBValidationException | VBNotAplicibleException e) {
				// Ignore this exception as this exception is considered in an other check
			}

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

	/**
	 * Searches the feature model annotation of the rule
	 * 
	 * @param rule The rule
	 * @return The feature model
	 * @throws VBValidationException   If there are more than one feature models
	 * @throws VBNotAplicibleException If there is no feature model given
	 */
	private static String getFM(Rule rule) throws VBValidationException, VBNotAplicibleException {
		List<Annotation> featureModels = getAnnotations(VariabilityConstants.FEATURE_MODEL, rule);
		if (featureModels.isEmpty()) {
			throw new VBNotAplicibleException(
					"There is no feature model given for the rule \"" + rule.getName() + "\"");
		}
		if (featureModels.size() > 1) {
			throw new VBValidationException(
					"There are multiple feature model given for the rule \"" + rule.getName() + "\"");
		}
		return featureModels.get(0).getValue();
	}
}
