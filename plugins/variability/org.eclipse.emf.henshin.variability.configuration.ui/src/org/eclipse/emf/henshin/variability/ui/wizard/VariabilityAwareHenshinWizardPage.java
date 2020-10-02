package org.eclipse.emf.henshin.variability.ui.wizard;

import org.eclipse.emf.henshin.interpreter.ui.wizard.HenshinWizardPage;
import org.eclipse.emf.henshin.interpreter.ui.wizard.UnitSelector;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class VariabilityAwareHenshinWizardPage extends HenshinWizardPage {
	
	FeatureConstraintViewer featureConstraintViewer;	
	BindingEditTable bindingSelector;

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite)super.getControl();
		featureConstraintViewer = new FeatureConstraintViewer(composite);
		featureConstraintViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));
		bindingSelector = new BindingEditTable(composite);
		bindingSelector.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));
		
		//Move comparison checkbox to the last position of the wizard
		Control[] children = composite.getChildren();
		int childCount = children.length;
		openCompare.moveBelow(children[childCount - 1]);
	}
}
