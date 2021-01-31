package org.eclipse.emf.henshin.variability.configuration.ui.actions;

import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.VariabilityModelHelper;
import org.eclipse.emf.henshin.variability.configuration.ui.providers.ConfigurationProvider;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityTransactionHelper;

import configuration.Configuration;

/**
 * This class allows moving a selected element to a configuration; i.e. changing its presence condition according to the current configuration.
 *
 * @author Stefan Schulz
 *
 */
public class AddElementToConfigurationAction extends AbstractModifyPresenceConditionAction {

	public final static String ID = "org.eclipse.emf.henshin.variability.ui.AddElementToConfigurationActionID";

	@Override
	protected String getPresenceCondition(final ModelElement modelElement) {
		final Rule rule = ((GraphElement) modelElement).getGraph().getRule();
		final Configuration configuration = ConfigurationProvider.getInstance().getConfiguration(rule);
		final String configurationPC = VariabilityModelHelper.getPresenceCondition(configuration);
		String result = VariabilityTransactionHelper.INSTANCE.getPresenceConditionString(modelElement);

		if ((result == null) || result.isEmpty()) {
			result = configurationPC;
		} else {
			result = String.format("(%s) | (%s)", result, configurationPC);
		}

		return result;
	}

	@Override
	protected String getDialogTitle() {
		return "Add to Configuration";
	}
}
