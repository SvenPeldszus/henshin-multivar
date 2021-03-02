package org.eclipse.emf.henshin.variability.ui.wizard;

import org.eclipse.emf.henshin.interpreter.ui.wizard.HenshinWizardPage;
import org.eclipse.emf.henshin.interpreter.ui.wizard.ModelSelector;
import org.eclipse.emf.henshin.interpreter.ui.wizard.UnitSelector;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class VariabilityAwareHenshinWizardPage extends HenshinWizardPage {
	
	Composite composite;
	
	FeatureModelSelector processorSelector;
//	FeatureConstraintViewer featureConstraintViewer;	
	VariabilityRuleViewer bindingSelector;

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		composite = (Composite)super.getControl();
		processorSelector = new FeatureModelSelector(composite);
		processorSelector.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));
		
//		featureConstraintViewer = new FeatureConstraintViewer((Composite)super.unitSelector.getControl());
//		{
//			FormData data = new FormData();
//			data.top = new FormAttachment(super.unitSelector.getUnitFilter(), 2*CONTROL_OFFSET);
//			data.left = new FormAttachment(0, CONTROL_OFFSET);
//			data.right = new FormAttachment(100, -CONTROL_OFFSET);
//			featureConstraintViewer.getControl().setLayoutData(data);
//		}
//		featureConstraintViewer.getControl().setLayoutData(
//				new GridData(SWT.FILL, SWT.FILL, true, false));
		bindingSelector = new VariabilityRuleViewer((Composite)super.unitSelector.getControl());
		{
			FormData data = new FormData();
			data.top = new FormAttachment(super.parameterEditor.getControl(), 2*CONTROL_OFFSET);
			data.left = new FormAttachment(0, CONTROL_OFFSET);
			data.right = new FormAttachment(100, -CONTROL_OFFSET);
			data.bottom = new FormAttachment(100, -CONTROL_OFFSET);
			bindingSelector.getControl().setLayoutData(data);
		}
		
		//Move comparison checkbox to the last position of the wizard
		Control[] children = composite.getChildren();
		int childCount = children.length;
		openCompare.moveBelow(children[childCount - 1]);
	}
}
