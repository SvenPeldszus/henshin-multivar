package org.eclipse.emf.henshin.variability.configuration.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityTransactionHelper;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * This class enables moving a selected element to the base rule.
 * 
 * @author Stefan Schulz
 *
 */
public class MoveElementToBaseRuleAction implements IActionDelegate {

	public final static String ID = "org.eclipse.emf.henshin.variability.ui.MoveElementToBaseRuleActionID";
	private ArrayList<ModelElement> selectedModelElementList = new ArrayList<ModelElement>();

	@Override
	public void run(IAction action) {
		if (selectedModelElementList != null && !selectedModelElementList.isEmpty()) {
			List<String> changedElements = new ArrayList<String>();
			for (ModelElement modelElement : selectedModelElementList) {
				String pc = VariabilityTransactionHelper.INSTANCE.getPresenceCondition(modelElement);
				if (pc != null && !pc.isEmpty()) {
					changedElements.add(modelElement.toString());
				}
			}

			if (!changedElements.isEmpty()) {
				MessageDialog messageDialog = new MessageDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Move to Base Rule", null,
						"The presence conditions of the following elements and all attached edges will be overwritten:\n"
								+ String.join(", ", changedElements) + "\n\nDo you want to continue?",
						MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);
				if (messageDialog.open() != 0)
					return;
			}
			for (ModelElement modelElement : selectedModelElementList) {
				VariabilityTransactionHelper.INSTANCE.setPresenceCondition(modelElement, null);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedModelElementList.clear();
		if (selection instanceof IStructuredSelection) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();

			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof AbstractGraphicalEditPart) {
					AbstractGraphicalEditPart editPart = (AbstractGraphicalEditPart) o;
					if (editPart.getModel() instanceof View) {
						View view = (View) editPart.getModel();
						if (view.getElement() instanceof ModelElement) {
							selectedModelElementList.add((ModelElement) view.getElement());
						}
					}
				}
			}
		}
	}

}
