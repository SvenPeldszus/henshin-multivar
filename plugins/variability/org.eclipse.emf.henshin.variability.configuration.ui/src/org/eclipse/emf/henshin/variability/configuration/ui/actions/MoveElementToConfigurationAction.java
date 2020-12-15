package org.eclipse.emf.henshin.variability.configuration.ui.actions;

import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.VariabilityModelHelper;
import org.eclipse.emf.henshin.variability.configuration.ui.providers.ConfigurationProvider;

import configuration.Configuration;

/**
 * This class allows moving a selected element to a configuration; i.e. changing its presence condition according to the current configuration.
 * 
 * @author Stefan Schulz
 *
 */
public class MoveElementToConfigurationAction extends AbstractModifyPresenceConditionAction {

	public final static String ID = "org.eclipse.emf.henshin.variability.ui.MoveElementToConfigurationActionID";
	
	@Override
	protected String getPresenceCondition(ModelElement modelElement) {
		Rule rule = ((GraphElement) modelElement).getGraph().getRule();
		Configuration configuration = ConfigurationProvider.getInstance().getConfiguration(rule);
		return VariabilityModelHelper.getPresenceCondition(configuration);
	}

	@Override
	protected String getDialogTitle() {
		return "Move to Configuration";
	}
}
