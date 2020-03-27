/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.validators.VBRuleInjectiveValidator;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityConstants;
import org.junit.jupiter.api.Test;

/**
 * @author speldszus
 *
 */
class VBRuleInjectiveMatchingPCValidatorTest extends AbstractVBValidatorTest {

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCNoVB() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCNoVBButAnnotation() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(HenshinFactory.eINSTANCE.createAnnotation());
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCValid() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createInjectiveMatchingPC("A & B"));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.OK, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCMultiplePCs() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createInjectiveMatchingPC("A & B"));
		rule.getAnnotations().add(createInjectiveMatchingPC("A | B"));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCNoPCButFeatures() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A"));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.WARNING, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCNullPC() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createInjectiveMatchingPC(null));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.WARNING, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCEmptyPC() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createInjectiveMatchingPC(" "));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.WARNING, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCUnknownFeature() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createFeatures("A, B"));
		rule.getAnnotations().add(createInjectiveMatchingPC("A & B | C"));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator#ValidateInjectiveMatchingPC(org.eclipse.emf.henshin.model.Rule)}.
	 */
	@Test
	void testValidateInjectiveMatchingPCNoFeatures() {
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(createInjectiveMatchingPC("A & B | C"));
		IStatus status = VBRuleInjectiveValidator.validateInjectiveMatchingPC(rule);
		assertEquals(IStatus.ERROR, status.getSeverity());
	}

	/**
	 * Creates an injective matching PC annotation
	 * 
	 * @param pc The presence condition
	 * @return The annotation
	 */
	private Annotation createInjectiveMatchingPC(String pc) {
		Annotation annotation = HenshinFactory.eINSTANCE.createAnnotation();
		annotation.setKey(VariabilityConstants.INJECTIVE_MATCHING_PC);
		annotation.setValue(pc);
		return annotation;
	}
}
