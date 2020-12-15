package org.eclipse.emf.henshin.variability.configuration.ui.actions;

import org.eclipse.emf.henshin.model.ModelElement;

/**
 * This class enables moving a selected element to the base rule.
 * 
 * @author Stefan Schulz
 *
 */
public class MoveElementToBaseRuleAction extends AbstractModifyPresenceConditionAction {

	public final static String ID = "org.eclipse.emf.henshin.variability.ui.MoveElementToBaseRuleActionID";

	@Override
	protected String getPresenceCondition(ModelElement modelElement) {
		return "";
	}

	@Override
	protected String getDialogTitle() {
		return "Move to Base Rule";
	}
}
