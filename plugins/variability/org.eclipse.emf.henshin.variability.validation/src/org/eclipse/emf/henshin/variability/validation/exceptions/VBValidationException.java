package org.eclipse.emf.henshin.variability.validation.exceptions;

/**
 * 
 * @author speldszus
 * 
 * This exception should be thrown if a VB validation failed
 *
 */
public class VBValidationException extends Exception {

	/**
	 * The generated serial version UID for this exception
	 */
	private static final long serialVersionUID = -263176566009576788L;

	/**
	 * Creates a new exception with the given message
	 * 
	 * @param message The message describing the cause for this exception
	 */
	public VBValidationException(String message) {
		super(message);
	}
}
