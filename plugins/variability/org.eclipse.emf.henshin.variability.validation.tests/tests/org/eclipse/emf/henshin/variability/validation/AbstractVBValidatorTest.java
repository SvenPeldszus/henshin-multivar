/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityConstants;

/**
 * An abstract class providen frequently required functionalities for testing validators
 * 
 * @author speldszus
 *
 */
public abstract class AbstractVBValidatorTest {

	/**
	 * Creates a PC annotation
	 * 
	 * @param pc The condition
	 * @return The annotation
	 */
	protected Annotation createPC(String pc) {
		Annotation annotation = HenshinFactory.eINSTANCE.createAnnotation();
		annotation.setKey(VariabilityConstants.PRESENCE_CONDITION.toString());
		annotation.setValue(pc);
		return annotation;
	}
	
	/**
	 * Creates a fm annotation
	 * 
	 * @param fm The condition
	 * @return The annotation
	 */
	protected Annotation createFM(String fm) {
		Annotation annotation = HenshinFactory.eINSTANCE.createAnnotation();
		annotation.setKey(VariabilityConstants.FEATURE_MODEL.toString());
		annotation.setValue(fm);
		return annotation;
	}
	
	/**
	 * Creates a features annotation
	 * 
	 * @param features The condition
	 * @return The annotation
	 */
	protected Annotation createFeatures(String features) {
		Annotation annotation = HenshinFactory.eINSTANCE.createAnnotation();
		annotation.setKey(VariabilityConstants.FEATURES.toString());
		annotation.setValue(features);
		return annotation;
	}

}
