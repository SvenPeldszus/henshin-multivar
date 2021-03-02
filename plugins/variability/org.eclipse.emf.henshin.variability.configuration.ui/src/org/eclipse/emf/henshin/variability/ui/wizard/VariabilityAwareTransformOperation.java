package org.eclipse.emf.henshin.variability.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.ui.HenshinInterpreterUIPlugin;
import org.eclipse.emf.henshin.interpreter.ui.util.ParameterConfig;
import org.eclipse.emf.henshin.interpreter.ui.util.TransformOperation;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.multi.MultiVarEngine;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;
import org.eclipse.emf.henshin.variability.ui.util.FeatureConfig;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import configuration.FeatureBinding;

public class VariabilityAwareTransformOperation extends TransformOperation {
	
	protected Collection<FeatureConfig> featureCfgs;
	protected String featureConstraint;	
	protected URI featureModelURI;
	protected MultiVarProcessor processor;
	
	public VariabilityAwareTransformOperation(TransformOperation op) {
		setUnit(op.getUnit(), op.getParameterConfigurations());
	}
	
	@Override
	public void applyTransformation(Assignment assignment, Engine engine, Resource input, ApplicationMonitor appMonitor, IProgressMonitor monitor) throws CoreException {
		Map<String, Boolean> configuration = new HashMap<String, Boolean>();
		for (FeatureConfig fCfg : featureCfgs) {
			if (!fCfg.isUnbound()) {
				configuration.put(fCfg.getFeatureName(), fCfg.isTrue());
			}
		}
		
		try {
			if (processor != null) {
				//TODO: Do multivar transformation
				VariabilityAwareInterpreterUtil.applyToResource(assignment, new MultiVarEngine(), input, appMonitor, configuration, processor, featureModelURI.toFileString());
			} else if (!VariabilityAwareInterpreterUtil.applyToResource(assignment, engine, input,
					appMonitor, configuration) && !monitor.isCanceled()) {
				throw new CoreException(
						new Status(IStatus.WARNING,
								HenshinInterpreterUIPlugin.PLUGIN_ID,
								"Transformation could not be applied to given input model."));
			}
		} catch (Throwable t) {
			// NOTE we should use an error dialog with a "details" section showing the caught exception
			throw new CoreException(
					new Status(IStatus.ERROR,
							HenshinInterpreterUIPlugin.PLUGIN_ID,
							"Error applying transformation: " + t.getMessage(), t));
		}
	}
		
	public void setProcessor(MultiVarProcessor processor) {
		this.processor = processor;
		this.featureModelURI = null;
	}
	
	public void setFeatureModel(URI featureModelURI) {
		this.featureModelURI = featureModelURI;
	}
	
	@Override
	public void setUnit(Unit unit, Collection<ParameterConfig> paramCfgs) {
		super.setUnit(unit, paramCfgs);
		featureConstraint =	VariabilityHelper.INSTANCE.getFeatureConstraint(unit);
	}
	
	public void setUnit(Unit unit, Collection<ParameterConfig> paramCfgs, Collection<FeatureConfig> featureCfgs) {
		super.setUnit(unit, paramCfgs);
		this.featureCfgs = featureCfgs;
		featureConstraint =	VariabilityHelper.INSTANCE.getFeatureConstraint(unit);
	}
	
	public void setFeatureConstraint(String constraint) {
		this.featureConstraint = constraint;
	}
	
	public void setFeatureConfigurations(Collection<FeatureConfig> featureCfgs) {
		this.featureCfgs = featureCfgs;
	}
	
	public FeatureBinding getFeatureBindingValue(String featureName) {
		FeatureConfig fCfg = getFeatureConfiguration(featureName);
		if (fCfg == null) {
			return null;
		}
		return fCfg.getFeatureBinding();
	}

	public FeatureConfig getFeatureConfiguration(String featureName) {
		for (FeatureConfig fCfg : featureCfgs) {
			if (fCfg.getFeatureName().equals(featureName)) {
				return fCfg;
			}
		}
		return null;
	}

	public Collection<FeatureConfig> getFeatureConfigurations() {
		return featureCfgs;
	}
	
	public String getFeatureConstraint() {
		return featureConstraint;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void clearFeatures() {
		for (FeatureConfig config : featureCfgs) {
			config.setFeatureBinding(FeatureBinding.UNBOUND);
		}
	}
}
