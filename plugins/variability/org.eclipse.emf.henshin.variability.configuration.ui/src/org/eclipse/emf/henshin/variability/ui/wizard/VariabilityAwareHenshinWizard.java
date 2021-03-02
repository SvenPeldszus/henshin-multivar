package org.eclipse.emf.henshin.variability.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.henshin.interpreter.ui.wizard.HenshinWizard;
import org.eclipse.emf.henshin.interpreter.ui.wizard.ModelSelector.ModelSelectorListener;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.VariabilityModelHelper;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;
import org.eclipse.emf.henshin.variability.ui.util.FeatureConfig;
import org.eclipse.emf.henshin.variability.ui.wizard.VariabilityRuleViewer.FeatureChangeListener;
import org.eclipse.emf.henshin.variability.ui.wizard.VariabilityRuleViewer.FeatureClearListener;
import org.eclipse.emf.henshin.variability.ui.wizard.FeatureModelSelector.FeatureModelSelectionListener;
import org.eclipse.emf.henshin.variability.ui.wizard.FeatureModelSelector.MultiVarProcessorSelectionListener;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityConstants;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 * This class provides a Henshin wizard with variability capabilities.
 * 
 * @author Stefan Schulz
 * 
 */
public class VariabilityAwareHenshinWizard extends HenshinWizard implements FeatureChangeListener, FeatureClearListener, MultiVarProcessorSelectionListener, FeatureModelSelectionListener {

	public VariabilityAwareHenshinWizard(Module module) {
		super(module);
	}

	public VariabilityAwareHenshinWizard(Unit unit) {
		super(unit);
	}
	
	protected void setInput(Unit unit) {
		VariabilityAwareTransformOperation operation = (VariabilityAwareTransformOperation) transformOperation;
		VariabilityAwareHenshinWizardPage vbPage = (VariabilityAwareHenshinWizardPage) page;
		String constraint = VariabilityHelper.INSTANCE.getFeatureConstraint(unit);
		operation.setUnit(unit, getParameterPreferences(unit), getFeatureConfigurations(unit));
		vbPage.bindingSelector.setConstraint(constraint);
		vbPage.bindingSelector.setFeatures(operation.getFeatureConfigurations());
	}

	@Override
	protected void initData() {
		super.initData();
		transformOperation = new VariabilityAwareTransformOperation(transformOperation);
		setInput(initialUnit);
		((VariabilityAwareHenshinWizardPage) page).processorSelector.addFeatureModelSelectionListener(this);
		((VariabilityAwareHenshinWizardPage) page).processorSelector.addMultiVarProcessorSelectionListener(this);
		((VariabilityAwareHenshinWizardPage) page).bindingSelector.addFeatureChangeListener(this);
		((VariabilityAwareHenshinWizardPage) page).bindingSelector.addFeatureClearListener(this);
	}

	@Override
	public void addPages() {
		addPage(page = new VariabilityAwareHenshinWizardPage());
		page.module = module;
	}

	protected List<FeatureConfig> getFeatureConfigurations(Unit unit) {
		List<FeatureConfig> result = new ArrayList<FeatureConfig>();
		Annotation features = VariabilityHelper.INSTANCE.getAnnotation(unit, VariabilityConstants.FEATURES);
		if (features != null) {
			for (String featureName : features.getValue().split(",")) {
				result.add(new FeatureConfig(featureName.trim(), (Rule) unit));
			}
		}
		return result;
	}

	@Override
	public boolean unitSelected(int idx, boolean showInnerUnits) {
		super.unitSelected(idx, showInnerUnits);
		Unit unit = showInnerUnits ? this.allUnits.get(idx) : this.outerUnits.get(idx);
		setInput(unit);
		fireCompletionChange();
		return false;
	}

	@Override
	public void featureChanged(FeatureConfig featureCfg) {
		fireCompletionChange();
	}

	@Override
	public boolean canFinish() {
		boolean contradicts = false;
		VariabilityAwareTransformOperation varOp = (VariabilityAwareTransformOperation) this.transformOperation;
		if (varOp.getFeatureConstraint() != null) {
			Sentence binding = VariabilityModelHelper.getFeatureExpression(varOp.getFeatureConfigurations());
			Sentence featureConstraint = FeatureExpression.getExpr(varOp.getFeatureConstraint());
			contradicts = FeatureExpression.contradicts(featureConstraint, binding);
		}
		
		if (contradicts) {
			page.setErrorMessage("Error: Feature bindings contradict feature constraint.");
		} else {
			page.setErrorMessage(null);
		}
		
		return super.canFinish() && !contradicts;
	}

	@Override
	public void clearFeatures() {
		((VariabilityAwareTransformOperation) transformOperation).clearFeatures();
		fireCompletionChange();
	}

	@Override
	public void featureModelURIChanged(String featureModelURI) {
		// TODO Auto-generated method stub
		((VariabilityAwareTransformOperation) transformOperation).setFeatureModel(URI.createURI(featureModelURI));
		fireCompletionChange();
	}

	@Override
	public void mutliVarProcessorChanged(MultiVarProcessor processor) {
		// TODO Auto-generated method stub
		((VariabilityAwareTransformOperation) transformOperation).setProcessor(processor);
		fireCompletionChange();
	}
}
