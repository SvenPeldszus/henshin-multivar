/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.junit.jupiter.api.Test;

/**
 * @author speldszus
 *
 */
class VBConstraintValidatorTest {

	/**
	 * Test method for
	 * {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#checkConstraint(java.util.List, java.lang.String)}.
	 */
	@Test
	void testCheckConstraintAllOK() {
		IStatus status = AbstractVBValidator.checkConstraint(Arrays.asList("A", "B"), "A && B");
		System.out.println(status.getMessage());
		assertEquals(status.getSeverity(), IStatus.OK);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#checkConstraint(java.util.List, java.lang.String)}.
	 */
	@Test
	void testCheckConstraintUnknownFeatures() {
		IStatus status = AbstractVBValidator.checkConstraint(Collections.emptyList(), "A && B");
		System.out.println(status.getMessage());
		assertEquals(status.getSeverity(), IStatus.ERROR);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.emf.henshin.variability.validation.validators.VBAnnotationValidator#checkConstraint(java.util.List, java.lang.String)}.
	 */
	@Test
	void testCheckConstraintInvalidConstraint() {
		IStatus status = AbstractVBValidator.checkConstraint(Arrays.asList("A", "B"), "Hello World");
		System.out.println(status.getMessage());
		assertEquals(status.getSeverity(), IStatus.ERROR);
	}

}
