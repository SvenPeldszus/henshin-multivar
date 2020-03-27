/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator;
import org.junit.jupiter.api.Test;

/**
 * @author speldszus
 *
 */
class VBRuleFeaturesValidatorTest extends AbstractVBValidatorTest {

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesNoVB() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesNoVBButAnnotation() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(HenshinFactory.eINSTANCE.createAnnotation());
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesValid() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesDuplicates() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B, A"));
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.WARNING, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesMultipleFeatureLists() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createFeatures("A, C"));
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesNull() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures(null));
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesEmpty() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures(" "));
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator#validateFeatures(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeaturesInvalidFeature() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B, C#"));
		IStatus status = VBRuleFeaturesValidator.validateFeatures(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}
}
