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
public class VBRuleFeaturesValidator extends AbstractVBValidator {

	@Override
	public IStatus validate(EObject eObject) {
		Rule rule = (Rule) eObject;
		return validateFeatures(rule);

	}

	/**
	 * Validates the features annotation
	 * 
	 * @param rule A rule
	 * @return If the features are valid
	 */
	public static IStatus validateFeatures(Rule rule) {
		EList<Annotation> annotations = rule.getAnnotations();
		if (!annotations.isEmpty()) {
			Set<String> features = VariabilityHelper.INSTANCE.getFeatures(rule);
			
			for(String feature : features) {
				if(!feature.matches("^[a-zA-Z0-9\\-|_]+")) {
					return new Status(IStatus.ERROR, "TODO", "The list of features contains invalid feature names!");
				}
			}
			if (features.parallelStream().distinct().count() < features.size()) {
				return new Status(IStatus.WARNING, "TODO", "The list of features contains duplicates!");
			}
		}
		return Status.OK_STATUS;
	}
}
