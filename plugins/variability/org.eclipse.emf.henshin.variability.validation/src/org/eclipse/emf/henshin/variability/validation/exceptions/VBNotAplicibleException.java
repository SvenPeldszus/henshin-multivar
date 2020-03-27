/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation.exceptions;

/**
 * @author speldszus
 * 
 *         This exception is thrown if a VB operation is applied to a non VB
 *         element
 */
public class VBNotAplicibleException extends Exception {

	/**
	 * The generated serial version UID of this Exception
	 */
	private static final long serialVersionUID = -8712998525613591238L;

	/**
	 * Creates a new exception with the given message
	 * 
	 * @param message The message describing the cause for this exception
	 */
	public VBNotAplicibleException(String message) {
		super(message);
	}
}
