package org.eclipse.emf.henshin.variability.ui.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.databinding.EMFProperties;
import org.eclipse.emf.ecore.util.FeatureMapUtil.FeatureValue;
import org.eclipse.emf.henshin.diagram.edit.parts.NodeCompartmentEditPart;
import org.eclipse.emf.henshin.diagram.edit.parts.RuleEditPart;
import org.eclipse.emf.henshin.diagram.edit.policies.NodeCompartmentItemSemanticEditPolicy;
import org.eclipse.emf.henshin.diagram.edit.policies.NodeItemSemanticEditPolicy;
import org.eclipse.emf.henshin.diagram.edit.policies.RuleCompartmentItemSemanticEditPolicy;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.configuration.ui.actions.LoadFavoriteConfigurationAction;
import org.eclipse.emf.henshin.variability.configuration.ui.controls.DropDownMenuAction;
import org.eclipse.emf.henshin.variability.configuration.ui.dialogs.NameDialog;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.CreationMode;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.FigureVisibilityConcealingStrategy;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.ImageHelper;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.RuleEditPartVisibilityHelper;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.ShapeAlphaConcealingStrategy;
import org.eclipse.emf.henshin.variability.configuration.ui.helpers.VariabilityModelHelper;
import org.eclipse.emf.henshin.variability.configuration.ui.parts.IContentView;
import org.eclipse.emf.henshin.variability.configuration.ui.parts.ILinkedWithEditorView;
import org.eclipse.emf.henshin.variability.configuration.ui.parts.ITableViewerSynchronizedPart;
import org.eclipse.emf.henshin.variability.configuration.ui.parts.LinkWithEditorSelectionListener;
import org.eclipse.emf.henshin.variability.configuration.ui.parts.SynchronizedTableViewer;
import org.eclipse.emf.henshin.variability.configuration.ui.policies.NodeVariabilityEditPolicy;
import org.eclipse.emf.henshin.variability.configuration.ui.policies.NodeVariabilityItemSemanticEditPolicy;
import org.eclipse.emf.henshin.variability.configuration.ui.policies.RuleVariabilityEditPolicy;
import org.eclipse.emf.henshin.variability.configuration.ui.providers.ConfigurationProvider;
import org.eclipse.emf.henshin.variability.ui.viewer.util.FeatureViewerBindingEditingSupport;
import org.eclipse.emf.henshin.variability.ui.viewer.util.FeatureViewerComparator;
import org.eclipse.emf.henshin.variability.ui.viewer.util.FeatureViewerContentProvider;
import org.eclipse.emf.henshin.variability.ui.viewer.util.FeatureViewerNameEditingSupport;
import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.eclipse.emf.henshin.variability.validation.AbstractVBValidator;
import org.eclipse.emf.henshin.variability.validation.VBConctraintDescriptor;
import org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator;
import org.eclipse.emf.henshin.variability.validation.validators.VBRuleFeaturesValidator;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityTransactionHelper;
import org.eclipse.emf.transaction.ResourceSetChangeEvent;
import org.eclipse.emf.transaction.ResourceSetListenerImpl;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.validation.preferences.EMFModelValidationPreferences;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import configuration.Configuration;
import configuration.ConfigurationFactory;
import configuration.Favorite;
import configuration.Feature;
import configuration.FeatureBinding;

/**
 * Provides a view that enables users to use variability features in the
 * graphical editor.
 * 
 * @author Stefan Schulz
 *
 */
public class VariabilityView extends ViewPart
		implements ILinkedWithEditorView, IContentView<Configuration>, ITableViewerSynchronizedPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.emf.henshin.variability.ui.views.VariabilityView";

	private SynchronizedTableViewer viewer;
	private Action showBaseRuleAction, showConfiguredRuleAction, showFullRuleAction, linkWithEditorAction,
			fadeConcealingAction, visibilityConcealingAction, linkToViewingMode, createInBase, createInConfiguration;
	private DropDownMenuAction loadFavoritesMenu, elementCreationMenu;
	private LinkWithEditorSelectionListener linkWithEditorSelectionListener = new LinkWithEditorSelectionListener(this);
	private boolean linkingActive;
	private Text featureConstraintText;
	private DataBindingContext featureConstraintTextBindingContext;
	private ObservableFeatureConstraintValue<?> observableFeatureConstraintValue;
	private FeatureViewerComparator comparator;
	private ConfigurationProvider configurationProvider = ConfigurationProvider.getInstance();
	private WritableValue<Rule> writableValue;
	private CreationMode creationMode = CreationMode.SELECTION;
	private Configuration config;

	private RuleEditPart selectedRuleEditPart;

	private Label ruleNameLabel;

	private ToolItem createFeatures, featureConstraintCNFIndicator, featureConstraintValidityIndicator, add, delete, clear, refresh, selectedFavorite, deleteFavorite;
	private ToolBar favoriteToolBar, featureConstraintToolbar;

	private boolean isCNF;

	public RuleEditPart getSelectedRuleEditPart() {
		return selectedRuleEditPart;
	}

	public void setSelectedRuleEditPart(RuleEditPart selectedRuleEditPart) {
		this.selectedRuleEditPart = selectedRuleEditPart;
	}

	public VariabilityView() {
		super();
	}

	/**
	 * @see IViewPart.init(IViewSite)
	 */
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
	}

	private Composite createViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout grid = new GridLayout(2, false);
		grid.marginLeft = -5;
		grid.marginRight = -5;
		grid.marginTop = -5;
		grid.marginBottom = -5;
		grid.horizontalSpacing = 0;

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		composite.setLayout(grid);

		favoriteToolBar = new ToolBar(composite, SWT.FLAT);
		favoriteToolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

//		deleteFavorite = new ToolItem(favoriteToolBar, SWT.PUSH);
//		deleteFavorite.setImage(ImageHelper.getImage("/icons/trash.png"));
//		deleteFavorite.setToolTipText("Create feature");
		selectedFavorite = new ToolItem(favoriteToolBar, SWT.FLAT);
		selectedFavorite.setText("Configuration");

		ToolBar buttonToolBar = new ToolBar(composite, SWT.FLAT);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(buttonToolBar);
		buttonToolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

		new ToolItem(buttonToolBar, SWT.SEPARATOR);
		add = new ToolItem(buttonToolBar, SWT.PUSH);
		add.setImage(ImageHelper.getImage("/icons/add.png"));
		add.setToolTipText("Create feature");
		add.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Rule rule;
				if (selectedRuleEditPart == null) {
					rule = writableValue.doGetValue();
				} else {
					rule = VariabilityModelHelper.getRuleForEditPart(selectedRuleEditPart);
				}

				NameDialog dialog = new NameDialog(getViewSite().getShell(), "Feature",
						VariabilityHelper.INSTANCE.getFeatures(rule));

				if (dialog.open() == Window.OK) {
					String featureName = dialog.getName().trim();
					Feature feature = ConfigurationFactory.eINSTANCE.createFeature();
					feature.setName(featureName);
					config.addFeature(feature);
					refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		add.setEnabled(false);

		delete = new ToolItem(buttonToolBar, SWT.PUSH);
		delete.setImage(ImageHelper.getImage("/icons/delete.png"));
		delete.setToolTipText("Delete selected features");
		delete.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Rule rule = VariabilityModelHelper.getRuleForEditPart(selectedRuleEditPart);
				StructuredSelection selection = (StructuredSelection) viewer.getSelection();
				ArrayList<Feature> selectedFeatures = new ArrayList<Feature>();
				Iterator<?> it = selection.iterator();

				while (it.hasNext()) {
					Object obj = it.next();

					if (obj instanceof Feature) {
						selectedFeatures.add((Feature) obj);
					}
				}

				MessageDialog messageDialog = new MessageDialog(getViewSite().getShell(), "Delete features", null,
						"Do you really want to delete the selected features?\nDoing so may render the rule's feature model invalid.",
						MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);

				if (messageDialog.open() == 0) {
					for (Feature feature : selectedFeatures) {
						config.removeFeature(feature);
					}
					configurationProvider.clearFavorites(config);
					refreshFavorites(rule);
					refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		delete.setEnabled(false);

		clear = new ToolItem(buttonToolBar, SWT.PUSH);
		clear.setImage(ImageHelper.getImage("/icons/clear.png"));
		clear.setToolTipText("Clear feature bindings");
		clear.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (config != null) {
					clearFavorite();
					for (Feature feature : config.getFeatures()) {
						feature.setBinding(FeatureBinding.UNBOUND);
					}
					refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		clear.setEnabled(false);

		refresh = new ToolItem(buttonToolBar, SWT.PUSH);
		refresh.setImage(ImageHelper.getImage("/icons/refresh.png"));
		refresh.setToolTipText("Refresh");
		refresh.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		clear.setEnabled(false);

		Composite tableComposite = new Composite(parent, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);

		viewer = new SynchronizedTableViewer(tableComposite,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER, this);
		createColumns(tableComposite, tableColumnLayout, viewer);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new FeatureViewerContentProvider());
		viewer.setInput(config);

		getSite().setSelectionProvider(viewer);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);

		comparator = new FeatureViewerComparator();
		viewer.setComparator(comparator);

		return tableComposite;
	}

	private void createColumns(final Composite parent, final TableColumnLayout tableColumnLayout,
			final TableViewer viewer) {
		String[] titles = { "Feature", "Binding" };

		TableViewerColumn col = createTableViewerColumn(titles[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Feature vp = (Feature) element;
				return vp.getName();
			}

			@Override
			public Image getImage(Object element) {
				return ImageHelper.getImage("/icons/table_default.png");
			}
		});
		col.setEditingSupport(new FeatureViewerNameEditingSupport(viewer));
		tableColumnLayout.setColumnData(col.getColumn(), new ColumnWeightData(60, false));

		col = createTableViewerColumn(titles[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Feature vp = (Feature) element;
				return vp.getBinding().getName();
			}

			@Override
			public Image getImage(Object element) {
				return ImageHelper.getImage("/icons/table_default.png");
				// return ImageHelper.getImage("/icons/" + ((Feature)
				// element).getBinding().getName().toLowerCase() + ".png");
			}
		});
		col.setEditingSupport(new FeatureViewerBindingEditingSupport(viewer));
		tableColumnLayout.setColumnData(col.getColumn(), new ColumnWeightData(40, false));
	}

	private TableViewerColumn createTableViewerColumn(String title, int index) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE, index);
		final TableColumn column = viewerColumn.getColumn();

		column.setText(title);
		column.setResizable(false);
		column.setMoveable(false);

		column.addSelectionListener(getSelectionAdapter(column, index));

		return viewerColumn;
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl_parent = new GridLayout(1, false);
		gl_parent.verticalSpacing = 0;
		parent.setLayout(gl_parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		composite.setLayout(new GridLayout(2, false));

		ruleNameLabel = new Label(composite, SWT.NONE);
		ruleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		ruleNameLabel.setText("No rule selected");

		Label separatorName = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separatorName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		Label featureConstraintLabel = new Label(composite, SWT.NONE);
		featureConstraintLabel.setImage(ImageHelper.getImage("/icons/variability.gif"));
		featureConstraintLabel.setText("Feature constraint");
		featureConstraintLabel.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, false, false, 1, 1));
		featureConstraintToolbar = new ToolBar(composite, SWT.FLAT);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(featureConstraintToolbar);
		featureConstraintToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		
		featureConstraintCNFIndicator = new ToolItem(featureConstraintToolbar, SWT.NONE);
		featureConstraintCNFIndicator.setEnabled(false);
		
		featureConstraintValidityIndicator = new ToolItem(featureConstraintToolbar, SWT.NONE);
		featureConstraintValidityIndicator.setEnabled(false);
				
		createFeatures = new ToolItem(featureConstraintToolbar, SWT.PUSH);
		createFeatures.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Rule rule = VariabilityModelHelper.getRuleForEditPart(selectedRuleEditPart);
				String[] missingFeatures = VariabilityHelper.INSTANCE.getMissingFeatures(config.getRule());
				for (String featureName : missingFeatures) {
					Feature feature = ConfigurationFactory.eINSTANCE.createFeature();
					feature.setName(featureName);
					config.addFeature(feature);
				}
				createFeatures.setEnabled(false);
				refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		createFeatures.setEnabled(false);
		featureConstraintToolbar.setVisible(false);

		featureConstraintText = new Text(composite, SWT.BORDER);
		featureConstraintText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		IValueProperty<Widget, String> property = WidgetProperties.text(SWT.Modify);
		IObservableValue<String> target = property.observe(featureConstraintText);
		featureConstraintTextBindingContext = new DataBindingContext();
		writableValue = new WritableValue<>();
		IObservableValue<String> model = EMFProperties.value(HenshinPackage.Literals.MODEL_ELEMENT__ANNOTATIONS)
				.observeDetail(writableValue);
		observableFeatureConstraintValue = new ObservableFeatureConstraintValue<Object>(model);
		featureConstraintTextBindingContext.bindValue(target, observableFeatureConstraintValue);		
		featureConstraintText.addKeyListener(new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				boolean isValid = updateErrorIndicators(config.getRule());
				boolean hasChanged = updateCNFIndicator(featureConstraintText.getText());
				updateMissingFeaturesButton(config.getRule());
				
				if (isValid && hasChanged) {
					VariabilityTransactionHelper.INSTANCE.setFeatureConstraintIsCNF(config.getRule(), isCNF);
				} else {
					toggleRuleValidation(true);
					VariabilityTransactionHelper.INSTANCE.setFeatureConstraintIsCNF(config.getRule(), isCNF);
					toggleRuleValidation(false);
				}
				featureConstraintToolbar.redraw();
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
		});		
		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		GridData tableCompositeGridData = new GridData();
		tableCompositeGridData.grabExcessHorizontalSpace = true;
		tableCompositeGridData.grabExcessVerticalSpace = true;
		tableCompositeGridData.horizontalAlignment = GridData.FILL;
		tableCompositeGridData.verticalAlignment = GridData.FILL;
		tableCompositeGridData.horizontalSpan = 2;
		Composite tableComposite = createViewer(parent);
		tableComposite.setLayoutData(tableCompositeGridData);

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),
				"org.eclipse.emf.henshin.variability.ui.viewer");
		createActions(parent);
		createMenu();
		createToolbar();
		toggleLinking(true);
	}
	
	private void toggleRuleValidation(boolean disabled) {
		//TODO: Read validator extension points for rules
		String featureModel = "org.eclipse.emf.henshin.variability.validation.featureModel";
		String injectiveMatchingPC = "org.eclipse.emf.henshin.variability.validation.injectiveMatchingPC";
		String featureList = "org.eclipse.emf.henshin.variability.validation.featuresList";
		EMFModelValidationPreferences.setConstraintDisabled(featureModel, disabled);
		EMFModelValidationPreferences.setConstraintDisabled(injectiveMatchingPC, disabled);
		EMFModelValidationPreferences.setConstraintDisabled(featureList, disabled);
	}

	private void updateEditPolicy(RuleEditPart ruleEditPart) {
		if (ruleEditPart == null) {
			return;
		}

		AbstractEditPart parent = (AbstractEditPart) ruleEditPart.getChildren().get(1);

		if (creationMode == CreationMode.CONFIGURATION
				|| (creationMode == CreationMode.SELECTION && !showBaseRuleAction.isChecked())) {
			installConfigurationEditPolicy(parent);
		} else {
			installBasePolicy(parent);
		}
	}

	protected void installBasePolicy(AbstractEditPart editPart) {
		editPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new RuleCompartmentItemSemanticEditPolicy());

		for (Object child : editPart.getChildren()) {
			if (child instanceof NodeEditPart) {
				NodeEditPart nodeEditPart = (NodeEditPart) child;
				nodeEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new NodeItemSemanticEditPolicy());
				NodeCompartmentEditPart nodeCompartmentEditPart = (NodeCompartmentEditPart) nodeEditPart.getChildren()
						.get(2);
				nodeCompartmentEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
						new NodeCompartmentItemSemanticEditPolicy());

			}
		}
	}

	private void installConfigurationEditPolicy(AbstractEditPart editPart) {
		editPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new RuleVariabilityEditPolicy(config));

		for (Object child : editPart.getChildren()) {
			if (child instanceof NodeEditPart) {
				NodeEditPart nodeEditPart = (NodeEditPart) child;
				nodeEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
						new NodeVariabilityItemSemanticEditPolicy(config));
				NodeCompartmentEditPart nodeCompartmentEditPart = (NodeCompartmentEditPart) nodeEditPart.getChildren()
						.get(2);
				nodeCompartmentEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
						new NodeVariabilityEditPolicy(config));
			}
		}
	}

	private void createActions(Composite parent) {
		elementCreationMenu = new DropDownMenuAction("Element creation mode", parent);
		elementCreationMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/creation_mode.gif"));

		linkToViewingMode = new Action("Link to viewing mode", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				creationMode = CreationMode.SELECTION;
				updateEditPolicy(selectedRuleEditPart);
			}
		};
		linkToViewingMode.setImageDescriptor(ImageHelper.getImageDescriptor("icons/add_to_selection.gif"));

		createInBase = new Action("Create in base rule", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				creationMode = CreationMode.BASE;
				updateEditPolicy(selectedRuleEditPart);
			}
		};
		createInBase.setImageDescriptor(ImageHelper.getImageDescriptor("icons/add_to_base.gif"));

		createInConfiguration = new Action("Create in configuration", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				creationMode = CreationMode.CONFIGURATION;
				updateEditPolicy(selectedRuleEditPart);
			}
		};
		createInConfiguration.setImageDescriptor(ImageHelper.getImageDescriptor("icons/add_to_configuration.gif"));

		elementCreationMenu.addActionToMenu(linkToViewingMode);
		elementCreationMenu.addActionToMenu(createInBase);
		elementCreationMenu.addActionToMenu(createInConfiguration);

		visibilityConcealingAction = new Action("Visibility", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				RuleEditPartVisibilityHelper.showFullRule(selectedRuleEditPart);
				RuleEditPartVisibilityHelper.setFadingStrategy(new FigureVisibilityConcealingStrategy());
				runSelectedVisibilityAction();
			};
		};
		visibilityConcealingAction.setChecked(true);
		fadeConcealingAction = new Action("Fading", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				RuleEditPartVisibilityHelper.showFullRule(selectedRuleEditPart);
				RuleEditPartVisibilityHelper.setFadingStrategy(new ShapeAlphaConcealingStrategy());
				runSelectedVisibilityAction();
			};
		};

		showBaseRuleAction = new Action("Show base rule", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					super.run();
					RuleEditPartVisibilityHelper.showBaseRule(selectedRuleEditPart);
					if (creationMode == CreationMode.SELECTION) {
						updateEditPolicy(selectedRuleEditPart);
					}
				}
			}
		};
		showBaseRuleAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/rule_base.gif"));

		showConfiguredRuleAction = new Action("Show current configuration", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked() && selectedRuleEditPart != null) {
					super.run();
					RuleEditPartVisibilityHelper.showConfiguredRule(selectedRuleEditPart, config,
							VariabilityHelper.INSTANCE.getFeatureConstraint(config.getRule()));
					if (creationMode == CreationMode.SELECTION) {
						updateEditPolicy(selectedRuleEditPart);
					}
				}
			}
		};
		showConfiguredRuleAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/rule_configured.gif"));

		showFullRuleAction = new Action("Show full rule", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					super.run();
					RuleEditPartVisibilityHelper.showFullRule(selectedRuleEditPart);
				}
			}
		};
		showFullRuleAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/rule_full.gif"));
		showFullRuleAction.setChecked(true);

		loadFavoritesMenu = new DropDownMenuAction("Manage favorites", parent) {
			@Override
			public void runWithEvent(Event event) {
				if (config == null) {
					return;
				}

				// Star button was clicked
				if (event.detail == 0) {
					if (!configurationProvider.isFavorite(config)) {
						Rule rule = VariabilityModelHelper.getRuleForEditPart(selectedRuleEditPart);
						Set<String> favoriteNames = new HashSet<>();
						Set<Favorite> favorites = configurationProvider.getFavorites(rule);

						if (favorites != null) {
							for (Favorite fav : favorites) {
								favoriteNames.add(fav.getName());
							}
						}
						NameDialog dialog = new NameDialog(getViewSite().getShell(), "Favorite", favoriteNames);
						if (dialog.open() == Window.OK) {
							String name = dialog.getName();
							Favorite favorite = configurationProvider.addConfigurationToFavorites(rule, name, config);
							LoadFavoriteConfigurationAction loadConfigurationAction = new LoadFavoriteConfigurationAction(
									favorite, VariabilityView.this);
							this.addActionToMenu(loadConfigurationAction);
							this.uncheckAll();
							selectFavorite(config);
							loadConfigurationAction.setChecked(true);
						} else {
							return;
						}
					} else {
						configurationProvider.removeConfigurationFromFavorites(config);
						refreshFavorites(config.getRule());
					}

					setChecked(configurationProvider.isFavorite(config));
				}
			}

			@Override
			public void setChecked(boolean favorite) {
				String imagePath = favorite ? "icons/star.png" : "icons/star_grey.png";
				setImageDescriptor(ImageHelper.getImageDescriptor(imagePath));
				firePropertyChange(CHECKED, !favorite, favorite);
			}
		};
		loadFavoritesMenu.setToolTipText("Manage favorites");
		loadFavoritesMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/star_grey.png"));

		linkWithEditorAction = new Action("Link with editor", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				toggleLinking(isChecked());
			}
		};
		linkWithEditorAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/synchronize.gif"));
	}

	private void createMenu() {
		IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
		IMenuManager subMgr = new MenuManager("Concealing strategies",
				ImageHelper.getImageDescriptor("icons/concealing.gif"), null);

		mgr.add(linkWithEditorAction);
		mgr.add(subMgr);
		subMgr.add(fadeConcealingAction);
		subMgr.add(visibilityConcealingAction);
	}

	private void createToolbar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		mgr.add(elementCreationMenu);
		mgr.add(new Separator());
		mgr.add(showBaseRuleAction);
		mgr.add(showConfiguredRuleAction);
		mgr.add(showFullRuleAction);
		mgr.add(new Separator());
		mgr.add(loadFavoritesMenu);
		mgr.add(new Separator());
		mgr.add(linkWithEditorAction);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private class ConfigurationListener extends ResourceSetListenerImpl {

		@Override
		public void resourceSetChanged(ResourceSetChangeEvent event) {
			Annotation annotation = findModifiedAnnotation(event.getNotifications());
			if (annotation != null) {
				String value = annotation.getValue();
				if (VariabilityHelper.isFeatureModelAnnotation(annotation)
						|| VariabilityHelper.isFeaturesAnnotation(annotation) && value != null && !value.isEmpty()) {
					featureConstraintToolbar.setVisible(true);
					updateMissingFeaturesButton(config.getRule());
					updateCNFIndicator(VariabilityHelper.INSTANCE.getFeatureConstraint(config.getRule()));
				}
			}
			
			if (observableFeatureConstraintValue.shouldUpdate()) {
				refresh();
			} else {
				viewer.refresh();
			}
		}

		private Annotation findModifiedAnnotation(List<Notification> notifications) {
			for (Notification notification : notifications) {
				if (notification.getNotifier() instanceof Annotation) {
					return (Annotation) notification.getNotifier();
				}
			}
			return null;
		}
	}

	@Override
	public void setContent(Configuration config) {
		Rule rule = config.getRule();

		TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(rule);
		domain.addResourceSetListener(new ConfigurationListener());

		viewer.setInput(config);
		ruleNameLabel.setText("Selected rule: " + rule.getName());
		writableValue.setValue(rule);
		refreshFavorites(rule);

		if (showConfiguredRuleAction.isChecked()) {
			showConfiguredRuleAction.run();
		}

		featureConstraintToolbar.setVisible(true);
		updateMissingFeaturesButton(rule);
		updateCNFIndicator(VariabilityHelper.INSTANCE.getFeatureConstraint(rule));

		add.setEnabled(true);
		delete.setEnabled(true);
		clear.setEnabled(true);
		refresh.setEnabled(true);
	}

	@Override
	public Configuration getContent() {
		return config;
	}

	public void refresh() {
		viewer.refresh();
		featureConstraintTextBindingContext.updateModels();
		featureConstraintTextBindingContext.updateTargets();
	}

	@Override
	public void editorSelectionChanged(IEditorPart activeEditor) {
		if (!this.linkingActive || !getViewSite().getPage().isPartVisible(this) || activeEditor == null) {
			return;
		}
		ISelection selection = activeEditor.getEditorSite().getSelectionProvider().getSelection();
		if (selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection) selection;
			if (structuredSelection.size() == 1 && structuredSelection.getFirstElement() instanceof RuleEditPart) {
				RuleEditPart ruleEditPart = (RuleEditPart) structuredSelection.getFirstElement();
				Rule rule = VariabilityModelHelper.getRuleForEditPart(ruleEditPart);
				config = configurationProvider.getConfiguration(rule);
				setContent(config);
				refresh();
			} else if (structuredSelection.size() == 1) {
				refresh();
			}
		}
	}

	protected void toggleLinking(boolean checked) {
		this.linkingActive = checked;
		if (checked) {
			getSite().getPage().addSelectionListener(linkWithEditorSelectionListener);
			editorSelectionChanged(getSite().getPage().getActiveEditor());
		} else {
			getSite().getPage().removeSelectionListener(linkWithEditorSelectionListener);
		}
		if (linkWithEditorAction != null) {
			linkWithEditorAction.setChecked(checked);
		}
	}

	private void refreshFavorites(Rule rule) {
		loadFavoritesMenu.clearMenu();
		Set<Favorite> favorites = configurationProvider.getFavorites(rule);
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				LoadFavoriteConfigurationAction loadConfigurationAction = new LoadFavoriteConfigurationAction(favorite,
						this);
				loadFavoritesMenu.addActionToMenu(loadConfigurationAction);
			}
		}
		selectFavorite(config);
	}

	@Override
	public void selectedRuleChanged(RuleEditPart ruleEditPart) {
		if (ruleEditPart != null) {
			Rule rule = VariabilityModelHelper.getRuleForEditPart(ruleEditPart);
			config = configurationProvider.getConfiguration(rule);
			selectedRuleEditPart = ruleEditPart;
			setContent(config);
			updateEditPolicy(ruleEditPart);
			refresh();
		}
	}

	private void runSelectedVisibilityAction() {
		if (showBaseRuleAction.isChecked()) {
			showBaseRuleAction.run();
		} else if (showConfiguredRuleAction.isChecked()) {
			showConfiguredRuleAction.run();
		}
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int sortDirection = comparator.getDirection();
				viewer.getTable().setSortDirection(sortDirection);
				viewer.getTable().setSortColumn(column);
				refresh();
			}
		};
		return selectionAdapter;
	}

	@Override
	public void tableViewerUpdated() {
		selectFavorite(config);
		if (showConfiguredRuleAction.isChecked()) {
			showConfiguredRuleAction.run();
		}
	}

	private void selectFavorite(Configuration config) {
		Favorite favorite = configurationProvider.findFavorite(config);
		if (favorite != null) {
			selectedFavorite.setText(favorite.getName());
			loadFavoritesMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/star.png"));
		} else {
			clearFavorite();
		}
	}

	private void clearFavorite() {
		selectedFavorite.setText("Configuration");
		loadFavoritesMenu.uncheckAll();
		loadFavoritesMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/star_grey.png"));
	}
	
	private boolean updateCNFIndicator(String expression) {
		boolean wasCNF = isCNF;
		
		isCNF = SatChecker.isCNF(expression);
		if (isCNF) {
			featureConstraintCNFIndicator.setImage(ImageHelper.getImage("/icons/cnf.png"));
			featureConstraintCNFIndicator.setDisabledImage(ImageHelper.getImage("/icons/cnf.png"));
			featureConstraintCNFIndicator.setToolTipText("Feature constraint is CNF");
		} else {
			featureConstraintCNFIndicator.setImage(ImageHelper.getImage("/icons/blank.png"));
			featureConstraintCNFIndicator.setDisabledImage(ImageHelper.getImage("/icons/blank.png"));
			featureConstraintCNFIndicator.setToolTipText("");
		}
		
		return wasCNF != isCNF;
	}
	
	private void updateMissingFeaturesButton(Rule rule) {
		if (VariabilityHelper.INSTANCE.hasMissingFeatures(rule)) {
			createFeatures.setImage(ImageHelper.getImage("/icons/create_features.png"));
			createFeatures.setToolTipText("Create all undefined features");
			createFeatures.setEnabled(true);
		} else {
			createFeatures.setImage(ImageHelper.getImage("/icons/blank.png"));
			createFeatures.setToolTipText("");
			createFeatures.setEnabled(false);
		}
	}
	
	private boolean updateErrorIndicators(Rule rule) {
		IStatus featureConstraintStatus;
		try {
			featureConstraintStatus = VBRuleFMValidator.validateFeatureModel(rule);
		} catch (NullPointerException e) {
			featureConstraintStatus = new Status(Status.ERROR, "TODO", "The feature constraint is invalid.");
		}
//		IStatus featuresStatus = VBRuleFeaturesValidator.validateFeatures(rule);
		
		if (!featureConstraintStatus.isOK()) {
			featureConstraintValidityIndicator.setImage(ImageHelper.getImage("/icons/error.png"));
			featureConstraintValidityIndicator.setDisabledImage(ImageHelper.getImage("/icons/error.png"));
			featureConstraintValidityIndicator.setToolTipText(featureConstraintStatus.getMessage());
		} else {
			featureConstraintValidityIndicator.setImage(ImageHelper.getImage("/icons/blank.png"));
			featureConstraintValidityIndicator.setDisabledImage(ImageHelper.getImage("/icons/blank.png"));
			featureConstraintValidityIndicator.setToolTipText("");
		}
		return featureConstraintStatus.isOK();
	}	
}
