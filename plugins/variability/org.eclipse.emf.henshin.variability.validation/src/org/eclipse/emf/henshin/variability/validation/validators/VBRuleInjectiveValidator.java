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
			String pc = null;
			try {
				pc = getInjectiveMatchingPC(rule);
			} catch (VBValidationException e) {
				return new Status(Status.ERROR, "TODO", e.getMessage());
			} catch (VBNotAplicibleException e) {
				// Ignore this exception for now
			}

			List<String> features = null;
			try {
				features = getFeatures(rule);
			} catch (VBValidationException | VBNotAplicibleException e) {
				// Ignore this exceptions as they are covered by an other check
			}

			if (features == null && pc == null) {
				return Status.OK_STATUS;
			}

			if (pc == null || pc.trim().length() == 0) {
				return new Status(Status.WARNING, "TODO", "The injective matching PC is empty, assuming default value!");
			}
			IStatus pcOk = checkConstraint(features, pc);
			if (!pcOk.isOK()) {
				return pcOk;
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * Accesses the injective matching PC of the rule
	 * 
	 * @param rule A rule
	 * @return the pc
	 * @throws VBNotAplicibleException If the rule has no PC
	 * @throws VBValidationException If there are multiple PCs
	 */
	private static String getInjectiveMatchingPC(Rule rule) throws VBNotAplicibleException, VBValidationException {
		List<Annotation> injectiveMatchingPCs = getAnnotations(VariabilityConstants.INJECTIVE_MATCHING_PC, rule);
		if (injectiveMatchingPCs.isEmpty()) {
			throw new VBNotAplicibleException(
					"There is no injective matching PC given for the rule \"" + rule.getName() + "\"");
		}
		if (injectiveMatchingPCs.size() > 1) {
			throw new VBValidationException(
					"There are multiple injective matching PCs given for the rule \"" + rule.getName() + "\"");
		}
		return injectiveMatchingPCs.get(0).getValue();
	}
}
