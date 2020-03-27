/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.model.IConstraintStatus;

/**
 * The validation status of a VB constraint
 * 
 * @author speldszus
 *
 */
public class VBConstraintStatus implements IConstraintStatus {

	private final IStatus status;
	private final EObject target;
	private final AbstractVBValidator constraint;

	/**
	 * Creates a new validation status from a standard Eclipse IStatus
	 * 
	 * @param status The validation status
	 * @param target The EObject which has been validated
	 * @param constraint The executed VB validator
	 */
	public VBConstraintStatus(IStatus status, EObject target, AbstractVBValidator constraint) {
		this.status = status;
		this.target = target;
		this.constraint = constraint;
	}

	@Override
	public AbstractVBValidator getConstraint() {
		return this.constraint;
	}

	@Override
	public EObject getTarget() {
		return this.target;
	}

	@Override
	public Set<EObject> getResultLocus() {
		if(this.status.isOK()) {
			return Collections.emptySet();
		}
		return Collections.singleton(this.target);
	}

	@Override
	public IStatus[] getChildren() {
		return this.status.getChildren();
	}

	@Override
	public int getCode() {
		return this.status.getCode();
	}

	@Override
	public Throwable getException() {
		return this.status.getException();
	}

	@Override
	public String getMessage() {
		return this.status.getMessage();
	}

	@Override
	public String getPlugin() {
		return this.status.getPlugin();
	}

	@Override
	public int getSeverity() {
		return this.status.getSeverity();
	}

	@Override
	public boolean isMultiStatus() {
		return this.status.isMultiStatus();
	}

	@Override
	public boolean isOK() {
		return status.isOK();
	}

	@Override
	public boolean matches(int severityMask) {
		return status.matches(severityMask);
	}

}
