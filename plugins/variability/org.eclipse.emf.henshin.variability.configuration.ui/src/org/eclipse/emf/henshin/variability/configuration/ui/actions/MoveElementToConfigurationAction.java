package org.eclipse.emf.henshin.variability.configuration.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.VariabilityModelHelper;
import org.eclipse.emf.henshin.variability.configuration.ui.providers.ConfigurationProvider;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityTransactionHelper;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import configuration.Configuration;

/**
 * This class allows moving a selected element to a configuration; i.e. changing its presence condition according to the current configuration.
 * 
 * @author Stefan Schulz
 *
 */
public class MoveElementToConfigurationAction implements IActionDelegate {

	public final static String ID = "org.eclipse.emf.henshin.variability.ui.MoveElementToConfigurationActionID";
	private ArrayList<GraphElement> selectedGraphElementList = new ArrayList<GraphElement>();
	
	@Override
	public void run(IAction action) {
		if(selectedGraphElementList != null && !selectedGraphElementList.isEmpty()) {
			List<String> changedElements = new ArrayList<String>();
			for (GraphElement graphElement : selectedGraphElementList) {
				String pc = VariabilityTransactionHelper.INSTANCE.getPresenceCondition((ModelElement) graphElement);
				if (pc != null && !pc.isEmpty()) {
					changedElements.add(graphElement.toString());
				}
			}

			if (!changedElements.isEmpty()) {
				MessageDialog messageDialog = new MessageDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Move to Configuration", null,
						"The presence conditions of the following elements and all attached edges will be overwritten:\n"
								+ String.join(", ", changedElements) + "\n\nDo you want to continue?",
						MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);
				if (messageDialog.open() != 0)
					return;
			}
			
			for(GraphElement graphElement : selectedGraphElementList) {
				if (graphElement instanceof ModelElement) {
					Rule rule = graphElement.getGraph().getRule();
					Configuration configuration = ConfigurationProvider.getInstance().getConfiguration(rule);
					String presenceCondition = VariabilityModelHelper.getPresenceCondition(configuration);					
					VariabilityTransactionHelper.INSTANCE.setPresenceCondition((ModelElement) graphElement, presenceCondition);
				}
			}
		}
		
	}
		
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedGraphElementList.clear();
		if (selection instanceof IStructuredSelection) {
			Iterator<?> it = ((IStructuredSelection)selection).iterator();
			
			while(it.hasNext()) {
				Object o = it.next();
				if (o instanceof AbstractGraphicalEditPart) {
					AbstractGraphicalEditPart editPart = (AbstractGraphicalEditPart) o;
					if(editPart.getModel() instanceof View) {
						View view = (View) editPart.getModel();
						if(view.getElement() instanceof GraphElement) {
							selectedGraphElementList.add((GraphElement) view.getElement());
						}
					}
				}
			}
		}
	}

}
