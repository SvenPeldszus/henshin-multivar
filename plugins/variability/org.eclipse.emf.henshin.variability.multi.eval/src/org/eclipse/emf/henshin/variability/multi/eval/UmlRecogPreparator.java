package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.Change;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.AttributeChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.CompoundChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.ObjectChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.ChangeImpl.ReferenceChangeImpl;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.Lifting;
import org.eclipse.emf.henshin.variability.multi.MultiVarEGraph;
import org.eclipse.emf.henshin.variability.multi.MultiVarEngine;
import org.eclipse.emf.henshin.variability.multi.MultiVarMatch;
import org.eclipse.emf.henshin.variability.multi.SecPLUtil;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;
import org.eclipse.uml2.uml.resource.UMLResource;

import carisma.profile.umlsec.variability.Conditional_Element;
import carisma.profile.umlsec.variability.VariabilityPackage;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import symmetric.AddObject;
import symmetric.AddReference;
import symmetric.AttributeValueChange;
import symmetric.Correspondence;
import symmetric.RemoveObject;
import symmetric.RemoveReference;
import symmetric.SymmetricDifference;
import symmetric.SymmetricFactory;

public class UmlRecogPreparator {

	// Values: BCMS BMWExampleSPL Notepad-Antenna EndToEndSecurity...
	private static final String[] values = { "BMWExampleSPL", // 0
			"EndToEndSecurity", // 1
			"BCMS", // 2
			"jsse_openjdk", // 3
			"Notepad-Antenna", // 4
			"MobilePhoto07_OO", // 5
			"lampiro", // 6
			"iTrust"
	};

	static String benchmark = values[2];
	static File henshinDirectory = new File("umledit" + File.separator);
	static File umlFile = new File("models" + File.separator + benchmark + File.separator + "model.uml");
	static File fmFile = new File("models" + File.separator + benchmark + File.separator + "model.fm.xml");
	static String outputPathPart1 = "umlrecog-test" + File.separator;
	static String outputPathPart2 = "prepared-test" + File.separator + benchmark + "" + File.separator;

	Resource modelResource;
	IFeatureModel modelFM;
	Module module;

	DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");

	public static void main(final String[] args) throws IOException {
		FMCoreLibrary.getInstance().install();
		// for (int i = 3; i < values.length; i++) {

		final UmlRecogPreparator umlRecogPreparator = new UmlRecogPreparator();

		System.out.println("Processing \"" + benchmark + "\"");

		final long start = System.currentTimeMillis();

		umlRecogPreparator.run();

		System.out.println("done - " + (System.currentTimeMillis() - start) + "ms");

		Files.copy(UmlRecogPreparator.fmFile.toPath(), Paths.get(outputPathPart1 + outputPathPart2 + "model.fm.xml"),
				StandardCopyOption.REPLACE_EXISTING);
		// }

	}

	private void run() throws IOException {
		// Initialization
		loadModelsAndRules();
		final Copier oldModel = copyModel();
		final MultiVarEGraph graph = new SecPLUtil().createEGraphAndCollectPCs(this.modelResource.getContents(),
				this.modelFM);
		System.out.println("initialization done");

		// Find interesting matches, i.e., those whose Phi-Apply value is not
		// "true"
		final Map<Rule, MultiVarMatch> matches = findMatchesWherePhiApplyIsNotTrue(graph);

		// Perform rule applications and save results
		final List<Change> resultChanges = new ArrayList<>();
		final List<Rule> appliedRules = applyRules(graph, matches, resultChanges);
		final SymmetricDifference symmetric = createSymmetricDifference(oldModel, resultChanges);
		saveResults(oldModel, graph, symmetric, appliedRules);
	}

	private Map<Node, List<EObject>> findElementsWithPC(final List<Conditional_Element> pcs, final Rule rule,
			final Map<EClass, LinkedList<EObject>> mapping) {
		final Map<EClass, List<EClass>> subclasses = getInheritanceTree(UMLPackage.eINSTANCE);

		final Map<Node, List<EObject>> result = new HashMap<>();
		for (final Node node : rule.getLhs().getNodes()) {
			final LinkedList<EObject> nodeResult = new LinkedList<>();
			final Stack<EClass> todo = new Stack<>();
			todo.add(node.getType());
			while (!todo.isEmpty()) {
				final EClass type = todo.pop();
				nodeResult.addAll(findType(mapping, pcs, type));
				if (subclasses.containsKey(type)) {
					todo.addAll(subclasses.get(type));
				}
			}
			result.put(node, nodeResult);
		}
		return result;
	}

	public Map<EClass, List<EClass>> getInheritanceTree(final EPackage ePackage) {
		final Map<EClass, List<EClass>> subclasses = new HashMap<>();
		for (final EClassifier classifier : ePackage.getEClassifiers()) {
			if (classifier instanceof EClass) {
				final EClass eclass = (EClass) classifier;
				for (final EClass e : eclass.getEAllSuperTypes()) {
					if (e.getEPackage() instanceof UMLPackage) {

						List<EClass> sub;
						if (subclasses.containsKey(e)) {
							sub = subclasses.get(e);
						} else {
							sub = new LinkedList<>();
							subclasses.put(e, sub);
						}
						sub.add(eclass);
					}
				}
			}
		}
		return subclasses;
	}

	public List<EObject> findType(final Map<EClass, LinkedList<EObject>> mapping, final List<Conditional_Element> pcs,
			final EClass type) {
		if (mapping.containsKey(type)) {
			return mapping.get(type);
		} else {
			final LinkedList<EObject> matches = new LinkedList<>();
			for (int i = pcs.size() - 1; i > 0; i--) {
				final Conditional_Element pc = pcs.get(i);
				if (type.equals(pc.getBase_Element().eClass())) {
					matches.add(pc.getBase_Element());
					pcs.remove(i);
				}
			}
			mapping.put(type, matches);
			return matches;
		}
	}

	private void loadModelsAndRules() throws IOException {
		final HenshinResourceSet rs = new HenshinResourceSet(henshinDirectory.getPath());

		UMLPackage.eINSTANCE.eClass();
		rs.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		VariabilityPackage.eINSTANCE.eClass();
		rs.getPackageRegistry().put(VariabilityPackage.eINSTANCE.getNsURI(), VariabilityPackage.eINSTANCE);
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		this.modelFM = FeatureModelManager.getInstance(fmFile.toPath()).getObject();
		this.modelResource = rs.createResource(umlFile.getName());
		this.modelResource.load(new FileInputStream(umlFile), Collections.EMPTY_MAP);

		this.module = LoadingHelper.loadAllRulesAsOneModule(rs, henshinDirectory.getPath(), "rules", RuleSet.NOFILTER);
	}

	private Copier copyModel() {
		final Copier oldModel = new Copier();
		oldModel.copyAll(this.modelResource.getContents());
		oldModel.copyReferences();
		return oldModel;
	}

	/**
	 * Sets parameter values to be filled into attributes of newly created objects.
	 *
	 * @param match
	 * @param rule
	 */
	private void prepareMatch(final Match match, final Rule rule) {
		for (final Parameter parameter : rule.getParameters()) {
			if (match.getParameterValue(parameter) == null) {
				final EClassifier type = parameter.getType();
				Object value = null;
				if (type.getName().contentEquals("String")) {
					value = ("RandString" + (int) (Math.random() * 1000));
				}
				if (type.getName().contentEquals("Boolean")) {
					value = Boolean.TRUE;
				}
				if (type.getName().contentEquals("Integer")) {
					value = 42;
				}
				if (type.getName().contentEquals("Double")) {
					value = Math.random();
				}
				if (type.getName().contentEquals("VisibilityKind")) {
					value = VisibilityKind.PUBLIC_LITERAL;
				}
				match.setParameterValue(parameter, value);
			}
		}
	}

	@SuppressWarnings("unused")
	private Map<Rule, MultiVarMatch> findMatchesWherePhiApplyIsNotTrue(final MultiVarEGraph graph) {
		int countSuccess = 0;
		int countFail = 0;
		int countBaseMatchRules = 0;
		int countRules = 0;

		// Caches for Elements with PC
		final HashMap<EClass, LinkedList<EObject>> mapping = new HashMap<>();
		final List<Conditional_Element> pcs = getPCs(graph);

		final Map<Rule, MultiVarMatch> matches = new LinkedHashMap<>();

		final int engineReinitializations = 0;
		final MultiVarEngine engine = new MultiVarEngine();

		for (final Unit unit : this.module.getUnits()) {
			// Skip the following rules which seem to be defective
			if (unit.getName().startsWith("createConstraint_IN_Transition")
					|| unit.getName().startsWith("createLiteralInteger")
					|| unit.getName().startsWith("CREATE_Association")
					|| unit.getName().equals("setParameterableElement_templateParameter_TO_TemplateParameter") // produces
					// changes
					// which
					// violate
					// UML
					// constraints
					|| unit.getName().equals("removeFromAssociation_memberEnd_Property") // change not allowed in UML
					|| unit.getName().equals("setTypedElement_type_TO_Type") // reason extremely long execution
					|| unit.getName().equals("createAssociation_IN_Class") // reason extremely long execution
					|| unit.getName().equals("moveOperation_FROM_Class_ownedOperation_TO_Class_Class") // reason
					// extremely
					// long
					// execution
					|| unit.getName().equals(
							"moveLiteralInteger_FROM_MultiplicityElement_lowerValue_TO_MultiplicityElement_MultiplicityElement") // reason
					// extremely
					// long
					// execution
					|| unit.getName().equals("moveComment_FROM_Element_ownedComment_TO_Element_Element") // reason
					// extremely
					// long
					// execution
					|| unit.getName().equals("moveOperation_FROM_Class_ownedOperation_TO_Class_Class0") // reason
					// extremely
					// long
					// execution
					|| unit.getName().equals("setOperation_bodyCondition_TO_Constraint") // reason extremely long
					// execution
					|| unit.getName().equals("moveConstraint_FROM_Namespace_ownedRule_TO_Namespace_Namespace") // reason
					// extremely
					// long
					// execution
					) {
				continue;
			}
			System.out.println(this.dateFormat.format(new Date(System.currentTimeMillis())) + " - Process rule: "
					+ unit.getName());
			final Rule rule = (Rule) unit;
			countRules++;

			final Map<Node, List<EObject>> elements = findElementsWithPC(pcs, rule, mapping);
			int size = 0;
			for (final List<EObject> val : elements.values()) {
				size += val.size();
			}
			if (size == 0) {
				continue;
			}

			final long startRule = System.currentTimeMillis();
			countBaseMatchRules++;

			for (final Entry<Node, List<EObject>> entry : elements.entrySet()) {
				final Node node = entry.getKey();
				for (final EObject target : entry.getValue()) {
					final Match baseMatch = new MatchImpl(rule);
					baseMatch.setNodeTarget(node, target);

					final Lifting lifting = new Lifting(graph);

					MultiVarMatch usedMatch = null;

					// boolean deep = true;
					// if (deep) {
					// List<Match> allMatches = InterpreterUtil.findAllMatches(engine, rule, graph,
					// baseMatch);
					// for (Match m : allMatches) {
					// String phiApply = lifting.computePhiApply(m).trim();
					// if (!phiApply.equals("true")) {
					// usedMatch = m;
					// break;
					// }
					// }
					// } else {
					Iterator<? extends MultiVarMatch> it;
					it = engine.findMatches(rule, graph, baseMatch).iterator();
					if (it.hasNext()) {
						final MultiVarMatch first = it.next();
						final String phiApply = lifting.computePhiApply(first).toString().trim();
						if (!phiApply.equalsIgnoreCase("true")) {
							usedMatch = first;
						}
					}

					// }
					if (usedMatch != null) {
						prepareMatch(usedMatch, rule);
						matches.put(rule, usedMatch);
						countSuccess++;
					} else {
						countFail++;
					}

				}
			}
			final String message = this.dateFormat.format(new Date(System.currentTimeMillis())) + " - " + countRules
					+ "/" + this.module.getUnits().size() + " rule, " + countBaseMatchRules
					+ " baseMatchRules, duration: " + (System.currentTimeMillis() - startRule) + "ms";
			System.out.println(message);
		}
		System.out.println("Rules with base match: " + countBaseMatchRules);
		return matches;
	}

	private List<Conditional_Element> getPCs(final EGraphImpl graph) {
		final List<Conditional_Element> pcs = new LinkedList<>();
		final HashSet<Resource> seen = new HashSet<>();
		for (final EObject root : graph.getRoots()) {
			final Resource resource = root.eResource();
			if ((resource != null) && !seen.contains(resource)) {
				seen.add(resource);
				for (final EObject eObject : resource.getContents()) {
					if (eObject instanceof Conditional_Element) {
						pcs.add((Conditional_Element) eObject);
					}
				}
			}
		}
		return pcs;
	}

	private List<Rule> applyRules(final MultiVarEGraph graph, final Map<Rule, MultiVarMatch> matches,
			final List<Change> resultChanges) {
		final List<Rule> rules = new ArrayList<>(matches.keySet());
		final List<Rule> appliedRules = new ArrayList<>();
		// for (int i = 0; i < 5; i++) {
		// int index = (int) (rules.size() * Math.random());
		// Rule rule = rules.get(index);

		for (final Rule rule : rules) {
			final MultiVarMatch match = matches.get(rule);

			if (rule.getName().startsWith("move")) {
				final boolean applicible = checkForCycleInContainmentHieracy(rule, match);
				if (!applicible) {
					continue;
				}
			}

			System.out.println("Applying rule \"" + rule + "\" to match:\n" + match);
			final Lifting lifting = new Lifting(graph);
			final MultiVarMatch liftedMatch = lifting.liftMatch(match);

			if (liftedMatch != null) {
				final CompoundChangeImpl change = (CompoundChangeImpl) new MultiVarEngine().createChange(rule, graph,
						liftedMatch, null);
				change.applyAndReverse();
				boolean isDetectableChange = true;
				for (final Change subChange : change.getChanges()) {
					if ((subChange instanceof AttributeChangeImpl)
							&& (((AttributeChangeImpl) subChange).getAttribute() == null)) {
						isDetectableChange = false;
						// else if (subChange instanceof ReferenceChangeImpl
						// && ((ReferenceChangeImpl) subChange).getReference() ==
						// null)
						// isDetectableChange = false;
					}
				}

				for (final EObject e : match.getNodeTargets()) {
					// Drop changes which remove a matched EObject from a
					// resource
					if (e.eResource() == null) {
						isDetectableChange = false;
						change.applyAndReverse();
						break;
					}
				}
				// Only record the detectable changes (i.e, those which can be
				// reconstructed
				// via low-level diffing) -- all others are a "lost cause"
				// anyways
				if (isDetectableChange) {
					resultChanges.add(change);
					appliedRules.add(rule);
				}
			}
		}
		return appliedRules;
	}

	private boolean checkForCycleInContainmentHieracy(final Rule rule, final Match match) {
		boolean applicible = true;
		for (final Edge edge : rule.getRhs().getEdges()) {
			final Action action = edge.getAction();
			if (action.getType() == Type.CREATE) {
				final Node src = edge.getSource();
				final EObject nodeTarget = match.getNodeTarget(src.getActionNode());
				EObject next = nodeTarget.eContainer();
				while (next != null) {
					if (match.getNodeTargets().contains(next)) {
						applicible = false;
						break;
					}
					next = next.eContainer();
				}
			}
			if (!applicible) {
				break;
			}
		}
		return applicible;
	}

	private SymmetricDifference createSymmetricDifference(final Copier new2old, final List<Change> resultChanges) {
		final SymmetricFactory factory = SymmetricFactory.eINSTANCE;
		final SymmetricDifference result = factory.createSymmetricDifference();
		final Map<EObject, EObject> new2cor = new HashMap<>();
		final Map<EObject, EObject> old2new = new HashMap<>();

		// Init lists
		for (final Entry<EObject, EObject> entry : new2old.entrySet()) {
			final Correspondence cor = factory.createCorrespondence();
			cor.setObjA(entry.getValue());
			cor.setObjB(entry.getKey());
			cor.setReliability(1.0F);
			result.getCorrespondences().add(cor);
			new2cor.put(entry.getKey(), cor);
			old2new.put(entry.getValue(), entry.getKey());
		}

		// Create changes
		final List<symmetric.Change> changes = new ArrayList<>();
		for (final Change compound : resultChanges) {
			for (final Change change : ((CompoundChangeImpl) compound).getChanges()) {
				if (change instanceof ObjectChangeImpl) {
					final ObjectChangeImpl ch = (ObjectChangeImpl) change;
					createObjectChanges(new2old, old2new, factory, result, new2cor, changes, ch);
				} else if (change instanceof ReferenceChangeImpl) {
					final ReferenceChangeImpl ch = (ReferenceChangeImpl) change;
					createReferenceChanges(new2old, old2new, factory, changes, ch);
				} else if (change instanceof AttributeChangeImpl) {
					final AttributeChangeImpl ch = (AttributeChangeImpl) change;
					createAttributeChanges(new2old, factory, changes, ch);
				} else {
					throw new RuntimeException("Unsupported change type!");
				}
			}

		}
		// Add changes stepwise to enforce proper ordering.
		result.getChanges().addAll(changes.stream().filter(c -> c instanceof AddObject).collect(Collectors.toList()));
		result.getChanges()
		.addAll(changes.stream().filter(c -> c instanceof AddReference).collect(Collectors.toList()));
		result.getChanges()
		.addAll(changes.stream().filter(c -> c instanceof RemoveObject).collect(Collectors.toList()));
		result.getChanges()
		.addAll(changes.stream().filter(c -> c instanceof RemoveReference).collect(Collectors.toList()));
		result.getChanges()
		.addAll(changes.stream().filter(c -> c instanceof AttributeValueChange).collect(Collectors.toList()));
		return result;
	}

	private void createAttributeChanges(final Copier new2old, final SymmetricFactory factory,
			final List<symmetric.Change> changes, final AttributeChangeImpl ch) {
		final AttributeValueChange attrValueChange = factory.createAttributeValueChange();
		final EAttribute attribute = ch.getAttribute();
		attrValueChange.setObjB(ch.getObject());
		attrValueChange.setObjA(new2old.get(ch.getObject()));
		attrValueChange.setType(attribute);
		changes.add(attrValueChange);
	}

	private void createReferenceChanges(final Copier new2old, final Map<EObject, EObject> old2new,
			final SymmetricFactory factory, final List<symmetric.Change> changes, final ReferenceChangeImpl ch) {
		final EReference reference = ch.getReference();

		if (reference.isMany()) {
			if (!ch.isCreate()) { // -> created (REVERSED!)
				final AddReference addReference = factory.createAddReference();
				addReference.setTgt(ch.getTarget());
				addReference.setSrc(ch.getSource());
				addReference.setType(reference);
				changes.add(addReference);

				if (reference.isContainment() && (new2old.get(ch.getTarget()) != null)) {
					final RemoveReference removeReference = factory.createRemoveReference();
					final EObject oldTarget = new2old.get(ch.getTarget());
					removeReference.setSrc(oldTarget.eContainer());
					removeReference.setTgt(oldTarget);
					removeReference.setType(oldTarget.eContainmentFeature());
					changes.add(removeReference);
				}
			} else if (ch.isCreate()) { // -> deleted (REVERSED)
				final RemoveReference removeReference = factory.createRemoveReference();
				final EObject oldSource = new2old.get(ch.getSource());
				final EObject oldTarget = new2old.get(ch.getTarget());
				removeReference.setSrc(oldSource);
				removeReference.setTgt(oldTarget);
				removeReference.setType(reference);
				changes.add(removeReference);
			}
		} else if (!reference.isMany()) {
			if (ch.isCreate()) { // -> created (NOT REVERSED!)
				final AddReference addReference = factory.createAddReference();
				addReference.setTgt(ch.getTarget());
				addReference.setSrc(ch.getSource());
				addReference.setType(reference);
				changes.add(addReference);

				if (ch.getTarget() != null) {
					final RemoveReference removeReference = factory.createRemoveReference();
					final EObject oldSource = new2old.get(ch.getSource());
					final EObject oldTarget = new2old.get(ch.getTarget());
					removeReference.setSrc(oldSource);
					removeReference.setTgt(oldTarget);
					removeReference.setType(reference);
					changes.add(removeReference);
				}
			} else if (!ch.isCreate()) { // > deleted (NOT REVERSED!)
				final RemoveReference removeReference = factory.createRemoveReference();
				final EObject oldSource = new2old.get(ch.getSource());
				final EObject oldTarget = new2old.get(ch.getTarget()); // REVERSED
				removeReference.setSrc(oldSource);
				removeReference.setTgt(oldTarget);
				removeReference.setType(reference);
				changes.add(removeReference);
			}
		}

	}

	private void createObjectChanges(final Copier new2old, final Map<EObject, EObject> old2new,
			final SymmetricFactory factory, final SymmetricDifference result, final Map<EObject, EObject> new2cor,
			final List<symmetric.Change> changes, final ObjectChangeImpl ch) {
		if (!ch.isCreate()) { // negated because object changes are
			// reversed
			final AddObject addObject = factory.createAddObject();
			addObject.setObj(ch.getObject());
			changes.add(addObject);

			final AddReference addReference = factory.createAddReference();
			addReference.setSrc(ch.getObject().eContainer());
			addReference.setTgt(ch.getObject());
			addReference.setType(ch.getObject().eContainmentFeature());
			changes.add(addReference);
		} else {
			final RemoveObject removeObject = factory.createRemoveObject();
			final EObject oldObject = new2old.get(ch.getObject());
			removeObject.setObj(oldObject);
			changes.add(removeObject);
			result.getCorrespondences().remove(new2cor.get(ch.getObject()));

			final RemoveReference removeReference = factory.createRemoveReference();
			removeReference.setSrc(oldObject.eContainer());
			removeReference.setTgt(oldObject);
			removeReference.setType(oldObject.eContainmentFeature());
			changes.add(removeReference);
		}
	}

	private void saveResults(final Copier oldModel, final EGraphImpl graph, final SymmetricDifference symmetric,
			final List<Rule> appliedRules) throws IOException {
		final HenshinResourceSet resourceSet = new HenshinResourceSet(new Path(outputPathPart1).toOSString());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

		final File file = new File(outputPathPart1 + outputPathPart2);
		file.mkdirs();
		final FileWriter writer = new FileWriter(new File(file, "appliedRules"));
		for (final Rule rule : appliedRules) {
			writer.write(rule.getName());
			writer.write('\n');
		}
		writer.close();

		final Resource res1 = resourceSet.createResource(outputPathPart2 + "1.uml");
		final Resource res2 = resourceSet.createResource(outputPathPart2 + "2.uml");
		for (final EObject root : graph.getRoots()) {
			if (!(root instanceof EPackage) && !(root instanceof EFactory) && !(root instanceof PrimitiveType)
					&& !(root instanceof EFactory) && !(root instanceof EFactory) && !(root instanceof Profile)) {
				res2.getContents().add(root);
			}
		}
		for (final EObject old : oldModel.values()) {
			if (old.eContainer() == null) {
				res1.getContents().add(old);
			}
		}

		res1.save(null);
		res2.save(null);
		resourceSet.saveEObject(symmetric, outputPathPart2 + "1-to-2.symmetric");
		resourceSet.saveEObject(symmetric, "1-to-2.symmetric"); // for easier
		// debugging
	}

}
