package org.eclipse.emf.henshin.variability.validation.validators;

import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.AbstractVBValidator;
import org.eclipse.emf.henshin.variability.validation.Activator;
import org.eclipse.emf.henshin.variability.validation.exceptions.VBNotAplicibleException;
import org.eclipse.emf.henshin.variability.validation.exceptions.VBValidationException;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityConstants;

/**
 * 
 * A valiator for VB annotations
 * 
 * @author speldszus
 *
 */
public class VBAnnotationValidator extends AbstractVBValidator {

	/**
	 * The Henshin Rule EClass
	 */
	private static final EClass RULE = HenshinPackage.eINSTANCE.getRule();

	@Override
	public IStatus validate(EObject eObject) {
		Annotation annotation = (Annotation) eObject;
		return validatePresenceCondition(annotation);
	}

	/**
	 * Validates a PC annotation
	 * 
	 * @param annotation The annotation
	 * @return If the anntation is valid
	 */
	public static IStatus validatePresenceCondition(Annotation annotation) {
		if (RULE.isInstance(annotation.eContainer())) {
			// We skip the annotations on rules as they have a separate validator
			return Status.OK_STATUS;
		}
		if (VariabilityConstants.PRESENCE_CONDITION.equals(annotation.getKey())) {
			EObject annotatedElement = annotation.eContainer();
			EObject parent = annotatedElement;
			while (parent != null && !RULE.isInstance(parent)) {
				parent = parent.eContainer();
			}
			if (parent == null) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Feature model not found!");
			}
			List<String> features;
			try {
				features = VBRuleFMValidator.getFeatures((Rule) parent);
			} catch (VBValidationException | VBNotAplicibleException e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			}
			return checkConstraint(features, annotation.getValue());
		}
		return Status.OK_STATUS;
	}

}
