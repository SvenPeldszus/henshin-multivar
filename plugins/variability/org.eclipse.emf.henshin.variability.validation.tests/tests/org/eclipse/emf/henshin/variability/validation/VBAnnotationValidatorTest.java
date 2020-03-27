/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator;
import org.junit.jupiter.api.Test;

/**
 * @author speldszus
 *
 */
class VBAnnotationValidatorTest extends AbstractVBValidatorTest{

	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#validatePresenceCondition(org.eclipse.emf.henshin.model.Annotation)}.
	 */
	@Test
	void testValidatePresenceConditionValid() {
		Annotation pc = createPC("A");
		Annotation fm = createFM("A");
		Annotation f = createFeatures("A");
		
		Rule rule = HenshinFactory.eINSTANCE.createRule();
		rule.getAnnotations().add(f);
		rule.getAnnotations().add(fm);
		
		Node node = rule.createNode(rule.eClass());
		node.getAnnotations().add(pc);
		
		IStatus status = VBAnnotationValidator.validatePresenceCondition(pc);
		
		assertEquals(status.getSeverity(), IStatus.OK);
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#validatePresenceCondition(org.eclipse.emf.henshin.model.Annotation)}.
	 */
	@Test
	void testValidatePresenceConditionNotRelevant() {
		IStatus status = VBAnnotationValidator.validatePresenceCondition(HenshinFactory.eINSTANCE.createAnnotation());
		
		assertEquals(status.getSeverity(), IStatus.OK);
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#validatePresenceCondition(org.eclipse.emf.henshin.model.Annotation)}.
	 */
	@Test
	void testValidatePresenceConditionNotOnRelevantElement() {
		Annotation pc = createPC("A");
		Rule rule = HenshinFactory.eINSTANCE.createRule("Dummy");
		rule.getAnnotations().add(pc);
		
		IStatus status = VBAnnotationValidator.validatePresenceCondition(pc);
		
		assertEquals(status.getSeverity(), IStatus.OK);
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#validatePresenceCondition(org.eclipse.emf.henshin.model.Annotation)}.
	 */
	@Test
	void testValidatePresenceConditionNoFM() {
		Annotation pc = createPC("A");
		
		Rule rule = HenshinFactory.eINSTANCE.createRule();
		
		Node node = rule.createNode(rule.eClass());
		node.getAnnotations().add(pc);
		
		IStatus status = VBAnnotationValidator.validatePresenceCondition(pc);
		
		assertEquals(status.getSeverity(), IStatus.ERROR);
	}
	
	/**
	 * Test method for {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#validatePresenceCondition(org.eclipse.emf.henshin.model.Annotation)}.
	 */
	@Test
	void testValidatePresenceConditionInvalidParent() {
		Annotation pc = createPC("A");
		Node node = HenshinFactory.eINSTANCE.createNode();
		node.getAnnotations().add(pc);
		
		IStatus status = VBAnnotationValidator.validatePresenceCondition(pc);
		
		assertEquals(status.getSeverity(), IStatus.ERROR);
	}
}
