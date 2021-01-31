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

public abstract class AbstractModifyPresenceConditionAction implements IActionDelegate  {

	protected ArrayList<ModelElement> selectedModelElementList = new ArrayList<>();
	protected String dialogTitle;
	protected String dialogMessage;


	@Override
	public void run(final IAction action) {
		if ((this.selectedModelElementList != null) && !this.selectedModelElementList.isEmpty()) {
			final List<String> changedElements = new ArrayList<>();
			for (final ModelElement modelElement : this.selectedModelElementList) {
				final String pc = VariabilityTransactionHelper.INSTANCE.getPresenceConditionString(modelElement);
				if ((pc != null) && !pc.isEmpty() && !pc.equals(getPresenceCondition(modelElement))) {
					changedElements.add(modelElement.toString());
				}
			}

			if (!changedElements.isEmpty()) {
				final MessageDialog messageDialog = new MessageDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						getDialogTitle(),
						null,
						getDialogMessage(changedElements),
						MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);
				if (messageDialog.open() != 0) {
					return;
				}
			}
			for (final ModelElement modelElement : this.selectedModelElementList) {
				VariabilityTransactionHelper.INSTANCE.setPresenceCondition(modelElement, getPresenceCondition(modelElement));
			}
		}
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selectedModelElementList.clear();
		if (selection instanceof IStructuredSelection) {
			final Iterator<?> it = ((IStructuredSelection) selection).iterator();

			while (it.hasNext()) {
				final Object o = it.next();
				if (o instanceof AbstractGraphicalEditPart) {
					final AbstractGraphicalEditPart editPart = (AbstractGraphicalEditPart) o;
					if (editPart.getModel() instanceof View) {
						final View view = (View) editPart.getModel();
						if (view.getElement() instanceof ModelElement) {
							this.selectedModelElementList.add((ModelElement) view.getElement());
						}
					}
				}
			}
		}
	}

	protected String getDialogMessage(final List<String> changedElements) {
		// TODO Auto-generated method stub
		return String.format(
				"The presence conditions of the following elements and all attached edges will be overwritten:\n%s\n\nDo you want to continue?",
				String.join(", ", changedElements));
	}

	protected abstract String getPresenceCondition(ModelElement modelElement);
	protected abstract String getDialogTitle();

}
