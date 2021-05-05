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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.databinding.EMFProperties;
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
import org.eclipse.emf.henshin.variability.util.FeatureExpression;
import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.eclipse.emf.henshin.variability.validation.validators.VBRuleFMValidator;
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

import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.visitors.SymbolCollector;
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
	private final LinkWithEditorSelectionListener linkWithEditorSelectionListener = new LinkWithEditorSelectionListener(this);
	private boolean linkingActive;
	private Text featureConstraintText;
	private DataBindingContext featureConstraintTextBindingContext;
	private ObservableFeatureConstraintValue<?> observableFeatureConstraintValue;
	private FeatureViewerComparator comparator;
	private final ConfigurationProvider configurationProvider = ConfigurationProvider.getInstance();
	private WritableValue<Rule> writableValue;
	private CreationMode creationMode = CreationMode.SELECTION;
	private Configuration config;

	private RuleEditPart selectedRuleEditPart;

	private Label ruleNameLabel;

	private ToolItem createFeatures, featureConstraintCNFIndicator, featureConstraintValidityIndicator, add, delete, clear, refresh, selectedFavorite, deleteFavorite;
	private ToolBar favoriteToolBar, featureConstraintToolbar;

	private boolean isCNF;

	public RuleEditPart getSelectedRuleEditPart() {
		return this.selectedRuleEditPart;
	}

	public void setSelectedRuleEditPart(final RuleEditPart selectedRuleEditPart) {
		this.selectedRuleEditPart = selectedRuleEditPart;
	}

	public VariabilityView() {
	}

	/**
	 * @see IViewPart.init(IViewSite)
	 */
	@Override
	public void init(final IViewSite site) throws PartInitException {
		super.init(site);
	}

	private Composite createViewer(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout grid = new GridLayout(2, false);
		grid.marginLeft = -5;
		grid.marginRight = -5;
		grid.marginTop = -5;
		grid.marginBottom = -5;
		grid.horizontalSpacing = 0;

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		composite.setLayout(grid);

		this.favoriteToolBar = new ToolBar(composite, SWT.FLAT);
		this.favoriteToolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		//		deleteFavorite = new ToolItem(favoriteToolBar, SWT.PUSH);
		//		deleteFavorite.setImage(ImageHelper.getImage("/icons/trash.png"));
		//		deleteFavorite.setToolTipText("Create feature");
		this.selectedFavorite = new ToolItem(this.favoriteToolBar, SWT.FLAT);
		this.selectedFavorite.setText("Configuration");

		final ToolBar buttonToolBar = new ToolBar(composite, SWT.FLAT);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(buttonToolBar);
		buttonToolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

		new ToolItem(buttonToolBar, SWT.SEPARATOR);
		this.add = new ToolItem(buttonToolBar, SWT.PUSH);
		this.add.setImage(ImageHelper.getImage("/icons/add.png"));
		this.add.setToolTipText("Create feature");
		this.add.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				Rule rule;
				if (VariabilityView.this.selectedRuleEditPart == null) {
					rule = VariabilityView.this.writableValue.doGetValue();
				} else {
					rule = VariabilityModelHelper.getRuleForEditPart(VariabilityView.this.selectedRuleEditPart);
				}

				final NameDialog dialog = new NameDialog(getViewSite().getShell(), "Feature",
						VariabilityHelper.INSTANCE.getFeatures(rule));

				if (dialog.open() == Window.OK) {
					final String featureName = dialog.getName().trim();
					final Feature feature = ConfigurationFactory.eINSTANCE.createFeature();
					feature.setName(featureName);
					VariabilityView.this.config.addFeature(feature);
					refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		this.add.setEnabled(false);

		this.delete = new ToolItem(buttonToolBar, SWT.PUSH);
		this.delete.setImage(ImageHelper.getImage("/icons/delete.png"));
		this.delete.setToolTipText("Delete selected features");
		this.delete.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Rule rule = VariabilityModelHelper.getRuleForEditPart(VariabilityView.this.selectedRuleEditPart);
				final StructuredSelection selection = (StructuredSelection) VariabilityView.this.viewer.getSelection();
				final ArrayList<Feature> selectedFeatures = new ArrayList<>();
				final Iterator<?> it = selection.iterator();

				while (it.hasNext()) {
					final Object obj = it.next();

					if (obj instanceof Feature) {
						selectedFeatures.add((Feature) obj);
					}
				}

				final MessageDialog messageDialog = new MessageDialog(getViewSite().getShell(), "Delete features", null,
						"Do you really want to delete the selected features?\nDoing so may render the rule's feature model invalid.",
						MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);

				if (messageDialog.open() == 0) {
					for (final Feature feature : selectedFeatures) {
						VariabilityView.this.config.removeFeature(feature);
					}
					VariabilityView.this.configurationProvider.clearFavorites(VariabilityView.this.config);
					refreshFavorites(rule);
					refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		this.delete.setEnabled(false);

		this.clear = new ToolItem(buttonToolBar, SWT.PUSH);
		this.clear.setImage(ImageHelper.getImage("/icons/clear.png"));
		this.clear.setToolTipText("Clear feature bindings");
		this.clear.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (VariabilityView.this.config != null) {
					clearFavorite();
					for (final Feature feature : VariabilityView.this.config.getFeatures()) {
						feature.setBinding(FeatureBinding.UNBOUND);
					}
					refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		this.clear.setEnabled(false);

		this.refresh = new ToolItem(buttonToolBar, SWT.PUSH);
		this.refresh.setImage(ImageHelper.getImage("/icons/refresh.png"));
		this.refresh.setToolTipText("Refresh");
		this.refresh.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				refresh();
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		this.clear.setEnabled(false);

		final Composite tableComposite = new Composite(parent, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
		final TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);

		this.viewer = new SynchronizedTableViewer(tableComposite,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER, this);
		createColumns(tableComposite, tableColumnLayout, this.viewer);
		final Table table = this.viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		this.viewer.setContentProvider(new FeatureViewerContentProvider());
		this.viewer.setInput(this.config);

		getSite().setSelectionProvider(this.viewer);

		final GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		this.viewer.getControl().setLayoutData(gridData);

		this.comparator = new FeatureViewerComparator();
		this.viewer.setComparator(this.comparator);

		return tableComposite;
	}

	private void createColumns(final Composite parent, final TableColumnLayout tableColumnLayout,
			final TableViewer viewer) {
		final String[] titles = { "Feature", "Binding" };

		TableViewerColumn col = createTableViewerColumn(titles[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final Feature vp = (Feature) element;
				return vp.getName();
			}

			@Override
			public Image getImage(final Object element) {
				return ImageHelper.getImage("/icons/table_default.png");
			}
		});
		col.setEditingSupport(new FeatureViewerNameEditingSupport(viewer));
		tableColumnLayout.setColumnData(col.getColumn(), new ColumnWeightData(60, false));

		col = createTableViewerColumn(titles[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final Feature vp = (Feature) element;
				return vp.getBinding().getName();
			}

			@Override
			public Image getImage(final Object element) {
				return ImageHelper.getImage("/icons/table_default.png");
				// return ImageHelper.getImage("/icons/" + ((Feature)
				// element).getBinding().getName().toLowerCase() + ".png");
			}
		});
		col.setEditingSupport(new FeatureViewerBindingEditingSupport(viewer));
		tableColumnLayout.setColumnData(col.getColumn(), new ColumnWeightData(40, false));
	}

	private TableViewerColumn createTableViewerColumn(final String title, final int index) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(this.viewer, SWT.NONE, index);
		final TableColumn column = viewerColumn.getColumn();

		column.setText(title);
		column.setResizable(false);
		column.setMoveable(false);

		column.addSelectionListener(getSelectionAdapter(column, index));

		return viewerColumn;
	}

	@Override
	public void createPartControl(final Composite parent) {
		final GridLayout gl_parent = new GridLayout(1, false);
		gl_parent.verticalSpacing = 0;
		parent.setLayout(gl_parent);

		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		composite.setLayout(new GridLayout(2, false));

		this.ruleNameLabel = new Label(composite, SWT.NONE);
		this.ruleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		this.ruleNameLabel.setText("No rule selected");

		final Label separatorName = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separatorName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		final Label featureConstraintLabel = new Label(composite, SWT.NONE);
		featureConstraintLabel.setImage(ImageHelper.getImage("/icons/variability.gif"));
		featureConstraintLabel.setText("Feature constraint");
		featureConstraintLabel.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, false, false, 1, 1));
		this.featureConstraintToolbar = new ToolBar(composite, SWT.FLAT);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(this.featureConstraintToolbar);
		this.featureConstraintToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

		this.featureConstraintCNFIndicator = new ToolItem(this.featureConstraintToolbar, SWT.NONE);
		this.featureConstraintCNFIndicator.setEnabled(false);

		this.featureConstraintValidityIndicator = new ToolItem(this.featureConstraintToolbar, SWT.NONE);
		this.featureConstraintValidityIndicator.setEnabled(false);

		this.createFeatures = new ToolItem(this.featureConstraintToolbar, SWT.PUSH);
		this.createFeatures.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Rule rule = VariabilityModelHelper.getRuleForEditPart(VariabilityView.this.selectedRuleEditPart);
				final String[] missingFeatures = getMissingFeatures(VariabilityView.this.config.getRule());
				for (final String featureName : missingFeatures) {
					final Feature feature = ConfigurationFactory.eINSTANCE.createFeature();
					feature.setName(featureName);
					VariabilityView.this.config.addFeature(feature);
				}
				VariabilityView.this.createFeatures.setEnabled(false);
				refresh();
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {

			}
		});
		this.createFeatures.setEnabled(false);
		this.featureConstraintToolbar.setVisible(false);

		this.featureConstraintText = new Text(composite, SWT.BORDER);
		this.featureConstraintText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		final IValueProperty<Widget, String> property = WidgetProperties.text(SWT.Modify);
		final IObservableValue<String> target = property.observe(this.featureConstraintText);
		this.featureConstraintTextBindingContext = new DataBindingContext();
		this.writableValue = new WritableValue<>();
		final IObservableValue<String> model = EMFProperties.value(HenshinPackage.Literals.MODEL_ELEMENT__ANNOTATIONS)
				.observeDetail(this.writableValue);
		this.observableFeatureConstraintValue = new ObservableFeatureConstraintValue<>(model);
		this.featureConstraintTextBindingContext.bindValue(target, this.observableFeatureConstraintValue);
		this.featureConstraintText.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(final KeyEvent e) {
				final boolean isValid = updateErrorIndicators(VariabilityView.this.config.getRule());
				final boolean hasChanged = updateCNFIndicator(VariabilityView.this.featureConstraintText.getText());
				updateMissingFeaturesButton(VariabilityView.this.config.getRule());

				if (isValid && hasChanged) {
					VariabilityTransactionHelper.INSTANCE.setFeatureConstraintIsCNF(VariabilityView.this.config.getRule(), VariabilityView.this.isCNF);
				} else {
					toggleRuleValidation(true);
					VariabilityTransactionHelper.INSTANCE.setFeatureConstraintIsCNF(VariabilityView.this.config.getRule(), VariabilityView.this.isCNF);
					toggleRuleValidation(false);
				}
				VariabilityView.this.featureConstraintToolbar.redraw();
			}

			@Override
			public void keyPressed(final KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});
		final Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		final GridData tableCompositeGridData = new GridData();
		tableCompositeGridData.grabExcessHorizontalSpace = true;
		tableCompositeGridData.grabExcessVerticalSpace = true;
		tableCompositeGridData.horizontalAlignment = GridData.FILL;
		tableCompositeGridData.verticalAlignment = GridData.FILL;
		tableCompositeGridData.horizontalSpan = 2;
		final Composite tableComposite = createViewer(parent);
		tableComposite.setLayoutData(tableCompositeGridData);

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this.viewer.getControl(),
				"org.eclipse.emf.henshin.variability.ui.viewer");
		createActions(parent);
		createMenu();
		createToolbar();
		toggleLinking(true);
	}

	private void toggleRuleValidation(final boolean disabled) {
		//TODO: Read validator extension points for rules
		final String featureModel = "org.eclipse.emf.henshin.variability.validation.featureModel";
		final String injectiveMatchingPC = "org.eclipse.emf.henshin.variability.validation.injectiveMatchingPC";
		final String featureList = "org.eclipse.emf.henshin.variability.validation.featuresList";
		EMFModelValidationPreferences.setConstraintDisabled(featureModel, disabled);
		EMFModelValidationPreferences.setConstraintDisabled(injectiveMatchingPC, disabled);
		EMFModelValidationPreferences.setConstraintDisabled(featureList, disabled);
	}

	private void updateEditPolicy(final RuleEditPart ruleEditPart) {
		if (ruleEditPart == null) {
			return;
		}

		final AbstractEditPart parent = (AbstractEditPart) ruleEditPart.getChildren().get(1);

		if ((this.creationMode == CreationMode.CONFIGURATION)
				|| ((this.creationMode == CreationMode.SELECTION) && !this.showBaseRuleAction.isChecked())) {
			installConfigurationEditPolicy(parent);
		} else {
			installBasePolicy(parent);
		}
	}

	protected void installBasePolicy(final AbstractEditPart editPart) {
		editPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new RuleCompartmentItemSemanticEditPolicy());

		for (final Object child : editPart.getChildren()) {
			if (child instanceof NodeEditPart) {
				final NodeEditPart nodeEditPart = (NodeEditPart) child;
				nodeEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new NodeItemSemanticEditPolicy());
				final NodeCompartmentEditPart nodeCompartmentEditPart = (NodeCompartmentEditPart) nodeEditPart.getChildren()
						.get(2);
				nodeCompartmentEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
						new NodeCompartmentItemSemanticEditPolicy());

			}
		}
	}

	private void installConfigurationEditPolicy(final AbstractEditPart editPart) {
		editPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new RuleVariabilityEditPolicy(this.config));

		for (final Object child : editPart.getChildren()) {
			if (child instanceof NodeEditPart) {
				final NodeEditPart nodeEditPart = (NodeEditPart) child;
				nodeEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
						new NodeVariabilityItemSemanticEditPolicy(this.config));
				final NodeCompartmentEditPart nodeCompartmentEditPart = (NodeCompartmentEditPart) nodeEditPart.getChildren()
						.get(2);
				nodeCompartmentEditPart.installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
						new NodeVariabilityEditPolicy(this.config));
			}
		}
	}

	private void createActions(final Composite parent) {
		this.elementCreationMenu = new DropDownMenuAction("Element creation mode", parent);
		this.elementCreationMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/creation_mode.gif"));

		this.linkToViewingMode = new Action("Link to viewing mode", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				VariabilityView.this.creationMode = CreationMode.SELECTION;
				updateEditPolicy(VariabilityView.this.selectedRuleEditPart);
			}
		};
		this.linkToViewingMode.setImageDescriptor(ImageHelper.getImageDescriptor("icons/add_to_selection.gif"));

		this.createInBase = new Action("Create in base rule", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				VariabilityView.this.creationMode = CreationMode.BASE;
				updateEditPolicy(VariabilityView.this.selectedRuleEditPart);
			}
		};
		this.createInBase.setImageDescriptor(ImageHelper.getImageDescriptor("icons/add_to_base.gif"));

		this.createInConfiguration = new Action("Create in configuration", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				VariabilityView.this.creationMode = CreationMode.CONFIGURATION;
				updateEditPolicy(VariabilityView.this.selectedRuleEditPart);
			}
		};
		this.createInConfiguration.setImageDescriptor(ImageHelper.getImageDescriptor("icons/add_to_configuration.gif"));

		this.elementCreationMenu.addActionToMenu(this.linkToViewingMode);
		this.elementCreationMenu.addActionToMenu(this.createInBase);
		this.elementCreationMenu.addActionToMenu(this.createInConfiguration);

		this.visibilityConcealingAction = new Action("Visibility", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				RuleEditPartVisibilityHelper.showFullRule(VariabilityView.this.selectedRuleEditPart);
				RuleEditPartVisibilityHelper.setFadingStrategy(new FigureVisibilityConcealingStrategy());
				runSelectedVisibilityAction();
			}
		};
		this.visibilityConcealingAction.setChecked(true);
		this.fadeConcealingAction = new Action("Fading", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				RuleEditPartVisibilityHelper.showFullRule(VariabilityView.this.selectedRuleEditPart);
				RuleEditPartVisibilityHelper.setFadingStrategy(new ShapeAlphaConcealingStrategy());
				runSelectedVisibilityAction();
			}
		};

		this.showBaseRuleAction = new Action("Show base rule", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					super.run();
					RuleEditPartVisibilityHelper.showBaseRule(VariabilityView.this.selectedRuleEditPart);
					if (VariabilityView.this.creationMode == CreationMode.SELECTION) {
						updateEditPolicy(VariabilityView.this.selectedRuleEditPart);
					}
				}
			}
		};
		this.showBaseRuleAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/rule_base.gif"));

		this.showConfiguredRuleAction = new Action("Show current configuration", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked() && (VariabilityView.this.selectedRuleEditPart != null)) {
					super.run();
					RuleEditPartVisibilityHelper.showConfiguredRule(VariabilityView.this.selectedRuleEditPart, VariabilityView.this.config,
							VariabilityHelper.INSTANCE.getFeatureConstraint(VariabilityView.this.config.getRule()));
					if (VariabilityView.this.creationMode == CreationMode.SELECTION) {
						updateEditPolicy(VariabilityView.this.selectedRuleEditPart);
					}
				}
			}
		};
		this.showConfiguredRuleAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/rule_configured.gif"));

		this.showFullRuleAction = new Action("Show full rule", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					super.run();
					RuleEditPartVisibilityHelper.showFullRule(VariabilityView.this.selectedRuleEditPart);
				}
			}
		};
		this.showFullRuleAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/rule_full.gif"));
		this.showFullRuleAction.setChecked(true);

		this.loadFavoritesMenu = new DropDownMenuAction("Manage favorites", parent) {
			@Override
			public void runWithEvent(final Event event) {
				if (VariabilityView.this.config == null) {
					return;
				}

				// Star button was clicked
				if (event.detail == 0) {
					if (!VariabilityView.this.configurationProvider.isFavorite(VariabilityView.this.config)) {
						final Rule rule = VariabilityModelHelper.getRuleForEditPart(VariabilityView.this.selectedRuleEditPart);
						final Set<String> favoriteNames = new HashSet<>();
						final Set<Favorite> favorites = VariabilityView.this.configurationProvider.getFavorites(rule);

						if (favorites != null) {
							for (final Favorite fav : favorites) {
								favoriteNames.add(fav.getName());
							}
						}
						final NameDialog dialog = new NameDialog(getViewSite().getShell(), "Favorite", favoriteNames);
						if (dialog.open() == Window.OK) {
							final String name = dialog.getName();
							final Favorite favorite = VariabilityView.this.configurationProvider.addConfigurationToFavorites(rule, name, VariabilityView.this.config);
							final LoadFavoriteConfigurationAction loadConfigurationAction = new LoadFavoriteConfigurationAction(
									favorite, VariabilityView.this);
							addActionToMenu(loadConfigurationAction);
							uncheckAll();
							selectFavorite(VariabilityView.this.config);
							loadConfigurationAction.setChecked(true);
						} else {
							return;
						}
					} else {
						VariabilityView.this.configurationProvider.removeConfigurationFromFavorites(VariabilityView.this.config);
						refreshFavorites(VariabilityView.this.config.getRule());
					}

					setChecked(VariabilityView.this.configurationProvider.isFavorite(VariabilityView.this.config));
				}
			}

			@Override
			public void setChecked(final boolean favorite) {
				final String imagePath = favorite ? "icons/star.png" : "icons/star_grey.png";
				setImageDescriptor(ImageHelper.getImageDescriptor(imagePath));
				firePropertyChange(CHECKED, !favorite, favorite);
			}
		};
		this.loadFavoritesMenu.setToolTipText("Manage favorites");
		this.loadFavoritesMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/star_grey.png"));

		this.linkWithEditorAction = new Action("Link with editor", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				toggleLinking(isChecked());
			}
		};
		this.linkWithEditorAction.setImageDescriptor(ImageHelper.getImageDescriptor("icons/synchronize.gif"));
	}

	private void createMenu() {
		final IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
		final IMenuManager subMgr = new MenuManager("Concealing strategies",
				ImageHelper.getImageDescriptor("icons/concealing.gif"), null);

		mgr.add(this.linkWithEditorAction);
		mgr.add(subMgr);
		subMgr.add(this.fadeConcealingAction);
		subMgr.add(this.visibilityConcealingAction);
	}

	private void createToolbar() {
		final IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		mgr.add(this.elementCreationMenu);
		mgr.add(new Separator());
		mgr.add(this.showBaseRuleAction);
		mgr.add(this.showConfiguredRuleAction);
		mgr.add(this.showFullRuleAction);
		mgr.add(new Separator());
		mgr.add(this.loadFavoritesMenu);
		mgr.add(new Separator());
		mgr.add(this.linkWithEditorAction);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		this.viewer.getControl().setFocus();
	}

	private class ConfigurationListener extends ResourceSetListenerImpl {

		@Override
		public void resourceSetChanged(final ResourceSetChangeEvent event) {
			final Annotation annotation = findModifiedAnnotation(event.getNotifications());
			if (annotation != null) {
				final String value = annotation.getValue();
				if (VariabilityHelper.isFeatureModelAnnotation(annotation)
						|| (VariabilityHelper.isFeaturesAnnotation(annotation) && (value != null) && !value.isEmpty())) {
					VariabilityView.this.featureConstraintToolbar.setVisible(true);
					updateMissingFeaturesButton(VariabilityView.this.config.getRule());
					updateCNFIndicator(VariabilityHelper.INSTANCE.getFeatureConstraint(VariabilityView.this.config.getRule()));
				}
			}

			if (VariabilityView.this.observableFeatureConstraintValue.shouldUpdate()) {
				refresh();
			} else {
				VariabilityView.this.viewer.refresh();
			}
		}

		private Annotation findModifiedAnnotation(final List<Notification> notifications) {
			for (final Notification notification : notifications) {
				if (notification.getNotifier() instanceof Annotation) {
					return (Annotation) notification.getNotifier();
				}
			}
			return null;
		}
	}

	@Override
	public void setContent(final Configuration config) {
		final Rule rule = config.getRule();

		final TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(rule);
		domain.addResourceSetListener(new ConfigurationListener());

		this.viewer.setInput(config);
		this.ruleNameLabel.setText("Selected rule: " + rule.getName());
		this.writableValue.setValue(rule);
		refreshFavorites(rule);

		if (this.showConfiguredRuleAction.isChecked()) {
			this.showConfiguredRuleAction.run();
		}

		this.featureConstraintToolbar.setVisible(true);
		updateMissingFeaturesButton(rule);
		updateCNFIndicator(VariabilityHelper.INSTANCE.getFeatureConstraint(rule));

		this.add.setEnabled(true);
		this.delete.setEnabled(true);
		this.clear.setEnabled(true);
		this.refresh.setEnabled(true);
	}

	@Override
	public Configuration getContent() {
		return this.config;
	}

	public void refresh() {
		this.viewer.refresh();
		this.featureConstraintTextBindingContext.updateModels();
		this.featureConstraintTextBindingContext.updateTargets();
	}

	@Override
	public void editorSelectionChanged(final IEditorPart activeEditor) {
		if (!this.linkingActive || !getViewSite().getPage().isPartVisible(this) || (activeEditor == null)) {
			return;
		}
		final ISelection selection = activeEditor.getEditorSite().getSelectionProvider().getSelection();
		if (selection instanceof StructuredSelection) {
			final StructuredSelection structuredSelection = (StructuredSelection) selection;
			if ((structuredSelection.size() == 1) && (structuredSelection.getFirstElement() instanceof RuleEditPart)) {
				final RuleEditPart ruleEditPart = (RuleEditPart) structuredSelection.getFirstElement();
				final Rule rule = VariabilityModelHelper.getRuleForEditPart(ruleEditPart);
				this.config = this.configurationProvider.getConfiguration(rule);
				setContent(this.config);
				refresh();
			} else if (structuredSelection.size() == 1) {
				refresh();
			}
		}
	}

	protected void toggleLinking(final boolean checked) {
		this.linkingActive = checked;
		if (checked) {
			getSite().getPage().addSelectionListener(this.linkWithEditorSelectionListener);
			editorSelectionChanged(getSite().getPage().getActiveEditor());
		} else {
			getSite().getPage().removeSelectionListener(this.linkWithEditorSelectionListener);
		}
		if (this.linkWithEditorAction != null) {
			this.linkWithEditorAction.setChecked(checked);
		}
	}

	private void refreshFavorites(final Rule rule) {
		this.loadFavoritesMenu.clearMenu();
		final Set<Favorite> favorites = this.configurationProvider.getFavorites(rule);
		if (favorites != null) {
			for (final Favorite favorite : favorites) {
				final LoadFavoriteConfigurationAction loadConfigurationAction = new LoadFavoriteConfigurationAction(favorite,
						this);
				this.loadFavoritesMenu.addActionToMenu(loadConfigurationAction);
			}
		}
		selectFavorite(this.config);
	}

	@Override
	public void selectedRuleChanged(final RuleEditPart ruleEditPart) {
		if (ruleEditPart != null) {
			final Rule rule = VariabilityModelHelper.getRuleForEditPart(ruleEditPart);
			this.config = this.configurationProvider.getConfiguration(rule);
			this.selectedRuleEditPart = ruleEditPart;
			setContent(this.config);
			updateEditPolicy(ruleEditPart);
			refresh();
		}
	}

	private void runSelectedVisibilityAction() {
		if (this.showBaseRuleAction.isChecked()) {
			this.showBaseRuleAction.run();
		} else if (this.showConfiguredRuleAction.isChecked()) {
			this.showConfiguredRuleAction.run();
		}
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
		final SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				VariabilityView.this.comparator.setColumn(index);
				final int sortDirection = VariabilityView.this.comparator.getDirection();
				VariabilityView.this.viewer.getTable().setSortDirection(sortDirection);
				VariabilityView.this.viewer.getTable().setSortColumn(column);
				refresh();
			}
		};
		return selectionAdapter;
	}

	@Override
	public void tableViewerUpdated() {
		selectFavorite(this.config);
		if (this.showConfiguredRuleAction.isChecked()) {
			this.showConfiguredRuleAction.run();
		}
	}

	private void selectFavorite(final Configuration config) {
		final Favorite favorite = this.configurationProvider.findFavorite(config);
		if (favorite != null) {
			this.selectedFavorite.setText(favorite.getName());
			this.loadFavoritesMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/star.png"));
		} else {
			clearFavorite();
		}
	}

	private void clearFavorite() {
		this.selectedFavorite.setText("Configuration");
		this.loadFavoritesMenu.uncheckAll();
		this.loadFavoritesMenu.setImageDescriptor(ImageHelper.getImageDescriptor("icons/star_grey.png"));
	}

	private boolean updateCNFIndicator(final String expression) {
		final boolean wasCNF = this.isCNF;

		this.isCNF = SatChecker.isCNF(expression);
		if (this.isCNF) {
			this.featureConstraintCNFIndicator.setImage(ImageHelper.getImage("/icons/cnf.png"));
			this.featureConstraintCNFIndicator.setDisabledImage(ImageHelper.getImage("/icons/cnf.png"));
			this.featureConstraintCNFIndicator.setToolTipText("Feature constraint is CNF");
		} else {
			this.featureConstraintCNFIndicator.setImage(ImageHelper.getImage("/icons/blank.png"));
			this.featureConstraintCNFIndicator.setDisabledImage(ImageHelper.getImage("/icons/blank.png"));
			this.featureConstraintCNFIndicator.setToolTipText("");
		}

		return wasCNF != this.isCNF;
	}

	private void updateMissingFeaturesButton(final Rule rule) {
		if (hasMissingFeatures(rule)) {
			this.createFeatures.setImage(ImageHelper.getImage("/icons/create_features.png"));
			this.createFeatures.setToolTipText("Create all undefined features");
			this.createFeatures.setEnabled(true);
		} else {
			this.createFeatures.setImage(ImageHelper.getImage("/icons/blank.png"));
			this.createFeatures.setToolTipText("");
			this.createFeatures.setEnabled(false);
		}
	}

	private boolean updateErrorIndicators(final Rule rule) {
		IStatus featureConstraintStatus;
		try {
			featureConstraintStatus = VBRuleFMValidator.validateFeatureModel(rule);
		} catch (final NullPointerException e) {
			featureConstraintStatus = new Status(IStatus.ERROR, "TODO", "The feature constraint is invalid.");
		}
		//		IStatus featuresStatus = VBRuleFeaturesValidator.validateFeatures(rule);

		if (!featureConstraintStatus.isOK()) {
			this.featureConstraintValidityIndicator.setImage(ImageHelper.getImage("/icons/error.png"));
			this.featureConstraintValidityIndicator.setDisabledImage(ImageHelper.getImage("/icons/error.png"));
			this.featureConstraintValidityIndicator.setToolTipText(featureConstraintStatus.getMessage());
		} else {
			this.featureConstraintValidityIndicator.setImage(ImageHelper.getImage("/icons/blank.png"));
			this.featureConstraintValidityIndicator.setDisabledImage(ImageHelper.getImage("/icons/blank.png"));
			this.featureConstraintValidityIndicator.setToolTipText("");
		}
		return featureConstraintStatus.isOK();
	}


	public boolean hasMissingFeatures(final Rule rule) {
		return !calculateMissingFeatureNames(rule).isEmpty();
	}

	public String[] getMissingFeatures(final Rule rule) {
		return calculateMissingFeatureNames(rule).toArray(new String[0]);
	}

	private Set<String> calculateMissingFeatureNames(final Rule rule) {
		final String sentence = VariabilityHelper.INSTANCE.getFeatureConstraint(rule);
		final Set<String> missingFeatures = new HashSet<>();
		final Set<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(FeatureExpression.getExpr(sentence));
		for (final PropositionSymbol symbol : symbols) {
			final String symbolName = symbol.getSymbol().trim();
			final Set<String> features = VariabilityHelper.INSTANCE.getFeatures(rule);
			if ((features == null) || !features.contains(symbolName)) {
				missingFeatures.add(symbolName);
			}
		}
		return missingFeatures;
	}
}
