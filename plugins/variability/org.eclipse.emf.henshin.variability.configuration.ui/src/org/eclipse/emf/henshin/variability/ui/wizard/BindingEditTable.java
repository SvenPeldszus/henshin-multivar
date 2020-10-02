package org.eclipse.emf.henshin.variability.ui.wizard;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.emf.henshin.interpreter.ui.util.ParameterConfig;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.ImageHelper;
import org.eclipse.emf.henshin.variability.ui.util.FeatureConfig;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import configuration.FeatureBinding;

public class BindingEditTable {
	
	protected int CONTROL_OFFSET = 5;
	
	protected Collection<FeatureChangeListener> listeners = new ArrayList<FeatureChangeListener>();
	protected Collection<FeatureClearListener> clearListeners = new ArrayList<FeatureClearListener>();
	
	protected TableViewer tableViewer;

	private Group tableContainer;

	public BindingEditTable(Composite parent) {
		GridLayout grid = new GridLayout(2, false);
//		grid.marginLeft = -5;
//		grid.marginRight = -5;
//		grid.marginTop = -5;
//		grid.marginBottom = -5;
		grid.horizontalSpacing = 0;
		tableContainer = new Group(parent, SWT.NONE);
		tableContainer.setText("Feature Bindings");
		tableContainer.setLayout(grid);
		
		ToolBar buttonToolBar = new ToolBar(tableContainer, SWT.FLAT);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(buttonToolBar);
		buttonToolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		new ToolItem(buttonToolBar, SWT.SEPARATOR);
		ToolItem clear = new ToolItem(buttonToolBar, SWT.PUSH);
		clear.setImage(ImageHelper.getImage("/icons/clear.png"));
		clear.setToolTipText("Clear feature bindings");
		clear.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				for (FeatureClearListener listener : clearListeners) {
					listener.clearFeatures();
				}
				tableViewer.refresh();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		tableViewer = new TableViewer(tableContainer, SWT.FULL_SELECTION | SWT.BORDER);
		
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		tableViewer.getTable().setLayoutData(gridData);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		buildColumns();
		
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				@SuppressWarnings("unchecked")
				Collection<FeatureConfig> featureCfgs = (Collection<FeatureConfig>) inputElement;				
				return featureCfgs.toArray();
			}
			
			@Override
			public void dispose() {
			}
		});
	}
	
	public void addFeatureChangeListener(FeatureChangeListener listener) {
		listeners.add(listener);
	}
	
	public void addFeatureClearListener(FeatureClearListener listener) {
		clearListeners.add(listener);
	}
	
	public static interface FeatureChangeListener {
		void featureChanged(FeatureConfig featureCfg);
	}
	
	public static interface FeatureClearListener {
		void clearFeatures();
	}
	
	public Control getControl() {
		return tableContainer;
	}
		
	public void setFeatures(Collection<FeatureConfig> featureCfgs) {
		tableViewer.setInput(featureCfgs);
		tableViewer.refresh();
	}
	
	private void buildColumns() {
		TableViewerColumn featureNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		{
			featureNameColumn.getColumn().setText("Feature");
			featureNameColumn.getColumn().setWidth(300);
			featureNameColumn.setLabelProvider(new ColumnLabelProvider() {
				
				@Override
				public String getText(Object entry) {
					return ((FeatureConfig)entry).getFeatureName();
				}
			});
			TableViewerColumn featureBindingColumn = new TableViewerColumn(tableViewer, SWT.NONE);
			featureBindingColumn.getColumn().setText("Binding");
			featureBindingColumn.getColumn().setWidth(150);
			featureBindingColumn.setLabelProvider(new ColumnLabelProvider() {
				
				@Override
				public String getText(Object entry) {
					return ((FeatureConfig)entry).getFeatureBindingLiteral();
				}
			});
			featureBindingColumn.setEditingSupport(new EditingSupport(tableViewer) {
				
				@Override
				protected void setValue(Object element, Object value) {
					FeatureConfig featureConfig = (FeatureConfig) element;
					featureConfig.setFeatureBinding(FeatureBinding.get((Integer) value));
					
					for (FeatureChangeListener l : listeners) {
						l.featureChanged(featureConfig);
					}
					
					tableViewer.refresh();
				}
				
				@Override
				protected Object getValue(Object element) {
					return ((FeatureConfig)element).getFeatureBindingValue();
				}
				
				
				@Override
				protected CellEditor getCellEditor(Object element) {
					return new ComboBoxCellEditor(tableViewer.getTable(),
							FeatureBinding.getNames(),
							SWT.READ_ONLY);
				}
				
				@Override
				protected boolean canEdit(Object element) {
					return true;
				}
			});
		}
	}
	
}
