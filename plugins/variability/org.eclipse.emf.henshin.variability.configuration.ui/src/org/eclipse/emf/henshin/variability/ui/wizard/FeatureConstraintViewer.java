package org.eclipse.emf.henshin.variability.ui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

public class FeatureConstraintViewer {
	
	protected int CONTROL_OFFSET = 5;
	
	protected Text constraint;

	private Group constraintContainer;
	
	public FeatureConstraintViewer(Composite parent) {
		constraintContainer = new Group(parent, SWT.NONE);
		constraintContainer.setText("Feature Constraint");
		constraintContainer.setLayout(new GridLayout(1, false));
		
		constraint = new Text(constraintContainer, SWT.BORDER  | SWT.WRAP | SWT.READ_ONLY);
		
		GridData constraintData = new GridData();
		constraintData.grabExcessHorizontalSpace = true;
		constraintData.horizontalAlignment = SWT.FILL;
		constraint.setLayoutData(constraintData);
	}
	
	public Control getControl() {
		return constraintContainer;
	}
	
	public void setConstraint(String constraintString) {
		if (constraintString != null) {			
			constraint.setText(constraintString);
			constraint.setToolTipText(constraintString);
		} else {
			constraint.setText("");
			constraint.setToolTipText(null);
		}
	}

}
