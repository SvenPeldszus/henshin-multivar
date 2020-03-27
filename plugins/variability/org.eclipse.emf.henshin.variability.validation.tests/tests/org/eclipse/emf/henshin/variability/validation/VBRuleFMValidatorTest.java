/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator;
import org.junit.jupiter.api.Test;

/**
 * @author speldszus
 *
 */
class VBRuleFMValidatorTest extends AbstractVBValidatorTest {

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelNoVB() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelNoVBButAnnotation() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(HenshinFactory.eINSTANCE.createAnnotation());
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelValid() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createFM("A & B"));
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelMultipleFMs() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createFM("A & B"));
		rule.getAnnotations().add(createFM("A | B"));
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelNullFM() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createFM(null));
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelEmptyFM() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createFM(" "));
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelUnknownFeature() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createFM("A & B | C"));
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#validateFeatureModel(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateFeatureModelNoFeatures() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFM("A & B | C"));
		IStatus status = VBRuleFMValidator.validateFeatureModel(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}
}
