package org.eclipse.emf.henshin.variability.ui.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.henshin.interpreter.ui.wizard.ModelSelector;
import org.eclipse.emf.henshin.interpreter.ui.wizard.ModelSelector.ModelSelectorListener;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.ImageHelper;
import org.eclipse.emf.henshin.variability.multi.MultiVarProcessor;
import org.eclipse.emf.henshin.variability.multi.extension.ExtensionPointHandler;
import org.eclipse.emf.henshin.variability.multi.extension.MultiVarProcessorExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class FeatureModelSelector implements ModelSelectorListener {
	
	public static interface FeatureModelSelectionListener {
		void featureModelURIChanged(String modelURI);
	}
	
	public static interface MultiVarProcessorSelectionListener {
		void mutliVarProcessorChanged(MultiVarProcessor processor);
	}
	
	protected List<MultiVarProcessorSelectionListener> multiVarProcessorSelectionListeners = new ArrayList<>();
	protected List<FeatureModelSelectionListener> featureModelSelectionListeners = new ArrayList<>();

	protected int CONTROL_OFFSET = 5;

	Group processorContainer;
	Combo productLineProcessorCombo;
	ModelSelector featureModelSelector;
	
	private Map<String, MultiVarProcessorExtension> registeredExtensions;

	public FeatureModelSelector(Composite parent) {
		try {
			List<MultiVarProcessorExtension> extensions;
			extensions = ExtensionPointHandler.getRegisteredProcessors();			
			if (!extensions.isEmpty()) {
				processorContainer = new Group(parent, SWT.NONE);
				processorContainer.setText("Product Line Processor");
				processorContainer.setLayout(new GridLayout(2, false));
				
				registeredExtensions = new HashMap<>();
	
				for (MultiVarProcessorExtension extension : extensions) {
					registeredExtensions.put(extension.getName(), extension);
				}
				productLineProcessorCombo = new Combo(processorContainer, SWT.READ_ONLY);
				productLineProcessorCombo.setItems(registeredExtensions.keySet().toArray(new String[0]));
				productLineProcessorCombo.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						// TODO Auto-generated method stub
						if (productLineProcessorCombo.getSelectionIndex() != -1 && registeredExtensions.get(productLineProcessorCombo.getText()).hasExternalFeatureModel()) {
							featureModelSelector.setEnabled(true);
							for (MultiVarProcessorSelectionListener listener : multiVarProcessorSelectionListeners) {
								listener.mutliVarProcessorChanged(registeredExtensions.get(productLineProcessorCombo.getText()).getProcessor());
							}
						} else {
							featureModelSelector.setEnabled(false);
							for (MultiVarProcessorSelectionListener listener : multiVarProcessorSelectionListeners) {
								listener.mutliVarProcessorChanged(null);
							}
							modelURIChanged(null);
						}
					}
				});
				productLineProcessorCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
				Button resetProcessorSelection = new Button(processorContainer, SWT.PUSH);
				resetProcessorSelection.setImage(ImageHelper.getImage("/icons/delete.png"));
				resetProcessorSelection.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						productLineProcessorCombo.deselectAll();
					}
				});

				featureModelSelector = new ModelSelector(processorContainer, null, "Feature Model", true);
				featureModelSelector.setEnabled(false);
				featureModelSelector.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				featureModelSelector.addModelSelectorListener(this);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Control getControl() {
		return processorContainer;
	}
	
	public boolean addFeatureModelSelectionListener(FeatureModelSelectionListener featureModelSelectionListener) {
		return this.featureModelSelectionListeners.add(featureModelSelectionListener);
	}
	
	public boolean addMultiVarProcessorSelectionListener(MultiVarProcessorSelectionListener multiVarProcessorSelectionListener) {
		return this.multiVarProcessorSelectionListeners.add(multiVarProcessorSelectionListener);
	}

	@Override
	public boolean modelURIChanged(String featureModelURI) {
		for (FeatureModelSelectionListener featureModelSelectionListener : featureModelSelectionListeners) {
			featureModelSelectionListener.featureModelURIChanged(featureModelURI);
		}
		return true;
	}

}
