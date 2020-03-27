package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Hashtable;
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
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.multi.Lifting;
import org.eclipse.emf.henshin.variability.multi.RuleToProductLineEngine;
import org.eclipse.emf.henshin.variability.multi.SecPLUtil;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl;

import carisma.profile.umlsec.variability.Conditional_Element;
import carisma.profile.umlsec.variability.VariabilityPackage;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
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
			"lampiro" // 6
	};

	static String benchmark = values[0];
	static File henshinDirectory = new File("umledit" + File.separator);
	static File umlFile = new File("models" + File.separator + benchmark + File.separator + "model.uml");
	static File fmFile = new File("models" + File.separator + benchmark + File.separator + "model.fm.xml");
	static String outputPathPart1 = "umlrecog" + File.separator;
	static String outputPathPart2 = "prepared" + File.separator + benchmark + "" + File.separator;

	Resource modelResource;
	IFeatureModel modelFM;
	Module module;

	DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// for (int i = 3; i < values.length; i++) {

		UmlRecogPreparator umlRecogPreparator = new UmlRecogPreparator();

		System.out.println("Processing \"" + benchmark + "\"");

		long start = System.currentTimeMillis();

		umlRecogPreparator.run();

		System.out.println("done - " + (System.currentTimeMillis() - start) + "ms");

		Files.copy(umlRecogPreparator.fmFile.toPath(), Paths.get(outputPathPart1 + outputPathPart2 + "model.fm.xml"),
				StandardCopyOption.REPLACE_EXISTING);
		// }

	}

	private void run() throws FileNotFoundException, IOException {
		// Initialization
		loadModelsAndRules();
		Copier oldModel = copyModel();
		Map<EObject, String> presenceConditions = new HashMap<>();
		EGraphImpl graph = new SecPLUtil().createEGraphAndCollectPCs(modelResource.getContents(), presenceConditions);
		System.out.println("initialization done");
		
		// Find interesting matches, i.e., those whose Phi-Apply value is not
		// "true"
		Map<Rule, Match> matches = findMatchesWherePhiApplyIsNotTrue(presenceConditions, graph);

		// Perform rule applications and save results
		List<Change> resultChanges = new ArrayList<Change>();
		List<Rule> appliedRules = applyRules(graph, presenceConditions, matches, resultChanges);
		SymmetricDifference symmetric = createSymmetricDifference(oldModel, resultChanges);
		saveResults(oldModel, graph, symmetric, appliedRules);
	}

	private Hashtable<Node, List<EObject>> findElementsWithPC(List<Conditional_Element> pcs, final Rule rule,
			Hashtable<EClass, LinkedList<EObject>> mapping) {
		Hashtable<EClass, List<EClass>> subclasses = getInheritanceTree(UMLPackage.eINSTANCE);

		Hashtable<Node, List<EObject>> result = new Hashtable<Node, List<EObject>>();
		for (Node node : rule.getLhs().getNodes()) {
			LinkedList<EObject> nodeResult = new LinkedList<EObject>();
			Stack<EClass> todo = new Stack<>();
			todo.add(node.getType());
			while (!todo.isEmpty()) {
				EClass type = todo.pop();
				nodeResult.addAll(findType(mapping, pcs, type));
				if (subclasses.containsKey(type)) {
					todo.addAll(subclasses.get(type));
				}
			}
			result.put(node, nodeResult);
		}
		return result;
	}

	public Hashtable<EClass, List<EClass>> getInheritanceTree(EPackage ePackage) {
		Hashtable<EClass, List<EClass>> subclasses = new Hashtable<>();
		for (EClassifier classifier : ePackage.getEClassifiers()) {
			if (classifier instanceof EClass) {
				EClass eclass = (EClass) classifier;
				for (EClass e : eclass.getEAllSuperTypes()) {
					if (e.getEPackage() instanceof UMLPackage) {

						List<EClass> sub;
						if (subclasses.containsKey(e)) {
							sub = subclasses.get(e);
						} else {
							sub = new LinkedList<EClass>();
							subclasses.put(e, sub);
						}
						sub.add(eclass);
					}
				}
			}
		}
		return subclasses;
	}

	public List<EObject> findType(Hashtable<EClass, LinkedList<EObject>> mapping, List<Conditional_Element> pcs,
			EClass type) {
		if (mapping.containsKey(type)) {
			return mapping.get(type);
		} 
		else {
			LinkedList<EObject> matches = new LinkedList<>();
			for (int i = pcs.size() - 1; i > 0; i--) {
				Conditional_Element pc = pcs.get(i);
				if (type.equals(pc.getBase_Element().eClass())) {
					matches.add(pc.getBase_Element());
					pcs.remove(i);
				}
			}
			mapping.put(type, matches);
			return matches;
		}
	}

	private void loadModelsAndRules() throws IOException, FileNotFoundException {
		HenshinResourceSet rs = new HenshinResourceSet(henshinDirectory.getPath());

		UMLPackage.eINSTANCE.eClass();
		rs.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		VariabilityPackage.eINSTANCE.eClass();
		rs.getPackageRegistry().put(VariabilityPackage.eINSTANCE.getNsURI(), VariabilityPackage.eINSTANCE);
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("uml", new UMLResourceFactoryImpl());

		modelFM = FeatureModelManager.getInstance(fmFile.toPath()).getObject();
		modelResource = rs.createResource(umlFile.getName());
		modelResource.load(new FileInputStream(umlFile), Collections.EMPTY_MAP);

		module = LoadingHelper.loadAllRulesAsOneModule(rs, henshinDirectory.getPath(), "rules", RuleSet.NOFILTER);
	}

	private Copier copyModel() {
		Copier oldModel = new Copier();
		oldModel.copyAll(modelResource.getContents());
		oldModel.copyReferences();
		return oldModel;
	}

	/**
	 * Sets parameter values to be filled into attributes of newly created
	 * objects.
	 * 
	 * @param match
	 * @param rule
	 */
	private void prepareMatch(Match match, Rule rule) {
		for (Parameter parameter : rule.getParameters()) {
			if (match.getParameterValue(parameter) == null) {
				EClassifier type = parameter.getType();
				Object value = null;
				if (type.getName().contentEquals("String")) {
					value = new String("RandString" + (int) (Math.random() * 1000));
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
	private Map<Rule, Match> findMatchesWherePhiApplyIsNotTrue(Map<EObject, String> presenceConditions,
			EGraphImpl graph) {
		int countSuccess = 0;
		int countFail = 0;
		int countBaseMatchRules = 0;
		int countRules = 0;

		// Caches for Elements with PC
		Hashtable<EClass, LinkedList<EObject>> mapping = new Hashtable<EClass, LinkedList<EObject>>();
		List<Conditional_Element> pcs = getPCs(graph);

		Map<Rule, Match> matches = new LinkedHashMap<Rule, Match>();

		int engineReinitializations = 0;
		RuleToProductLineEngine engine = new RuleToProductLineEngine();
		
		for (Unit unit : module.getUnits()) {
			// Skip the following rules which seem to be defective
			if (unit.getName().startsWith("createConstraint_IN_Transition")
					|| unit.getName().startsWith("createLiteralInteger")
					|| unit.getName().startsWith("CREATE_Association")
					|| unit.getName().equals("setParameterableElement_templateParameter_TO_TemplateParameter") //produces changes which violate UML constraints
					|| unit.getName().equals("setTypedElement_type_TO_Type") //reason extremely long execution
					|| unit.getName().equals("createAssociation_IN_Class")  //reason extremely long execution
					|| unit.getName().equals("removeFromAssociation_memberEnd_Property") // change not allowed in UML
					)
				continue;
			Rule rule = (Rule) unit;
			countRules++;
			
			Hashtable<Node, List<EObject>> elements = findElementsWithPC(pcs, rule, mapping);
			int size = 0;
			for (List<EObject> val : elements.values()) {
				size += val.size();
			}
			if (size == 0) {
				continue;
			}
			
			long startRule = System.currentTimeMillis();
			countBaseMatchRules++;
			
			for (Entry<Node, List<EObject>> entry : elements.entrySet()) {
				Node node = entry.getKey();
				for (EObject target : entry.getValue()) {
					Match baseMatch = new MatchImpl(rule);
					baseMatch.setNodeTarget(node, target);

					Lifting lifting = new Lifting(engine, graph, modelFM, presenceConditions);

					Match usedMatch = null;

					boolean deep = true;
					if (deep) {
						List<Match> allMatches = InterpreterUtil.findAllMatches(engine, rule, graph, baseMatch);
						for (Match m : allMatches) {
							String phiApply = lifting.computePhiApply(m).trim();
							if (!phiApply.equals("true")) {
								usedMatch = m;
								break;
							}
						}
					} else {
						Iterator<Match> it = engine.findMatches(rule, graph, baseMatch).iterator();
						if (it.hasNext()) {
							Match first = it.next();
							String phiApply = lifting.computePhiApply(first).trim();
							if (!phiApply.equals("true")) {
								usedMatch = first;
							}
						}
					}
					if (usedMatch != null) {
						prepareMatch(usedMatch, rule);
						matches.put(rule, usedMatch);
						countSuccess++;
					} else
						countFail++;
					
					
				}
			}
			String message = dateFormat.format(new Date(System.currentTimeMillis()))+" - "+countRules+"/"+module.getUnits().size()+" rule, "+countBaseMatchRules+" baseMatchRules, duration: "+(System.currentTimeMillis()-startRule)+"ms";
			System.out.println(message);
		}
		System.out.println("Rules with base match: "+countBaseMatchRules);
		return matches;
	}

	private List<Conditional_Element> getPCs(EGraphImpl graph) {
		List<Conditional_Element> pcs = new LinkedList<>();
		HashSet<Resource> seen = new HashSet<>();
		for (EObject root : graph.getRoots()) {
			Resource resource = root.eResource();
			if (resource != null && !seen.contains(resource)) {
				seen.add(resource);
				for (EObject eObject : resource.getContents()) {
					if (eObject instanceof Conditional_Element) {
						pcs.add((Conditional_Element) eObject);
					}
				}
			}
		}
		return pcs;
	}

	private List<Rule> applyRules(EGraphImpl graph, Map<EObject, String> presenceConditions, Map<Rule, Match> matches,
			List<Change> resultChanges) {
		List<Rule> rules = new ArrayList<Rule>(matches.keySet());
		List<Rule> appliedRules = new ArrayList<Rule>();
		// for (int i = 0; i < 5; i++) {
		// int index = (int) (rules.size() * Math.random());
		// Rule rule = rules.get(index);

		for (Rule rule : rules) {
			Match match = matches.get(rule);

			if (rule.getName().startsWith("move")) {
				boolean applicible = checkForCycleInContainmentHieracy(rule, match);
				if (!applicible) {
					continue;
				}
			}

			System.out.println("Applying rule \"" + rule + "\" to match:\n" + match);
			RuleToProductLineEngine engine = new RuleToProductLineEngine();
			Lifting lifting = new Lifting(engine, graph, modelFM, presenceConditions);
			Change change = lifting.liftAndApplyRule(match, rule);
			if (change != null) {
				CompoundChangeImpl ch = (CompoundChangeImpl) change;
				boolean isDetectableChange = true;
				for (Change subChange : ch.getChanges()) {
					if (subChange instanceof AttributeChangeImpl
							&& ((AttributeChangeImpl) subChange).getAttribute() == null)
						isDetectableChange = false;
					// else if (subChange instanceof ReferenceChangeImpl
					// && ((ReferenceChangeImpl) subChange).getReference() ==
					// null)
					// isDetectableChange = false;
				}

				for (EObject e : match.getNodeTargets()) {
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

	private boolean checkForCycleInContainmentHieracy(Rule rule, Match match) {
		boolean applicible = true;
		for (Edge edge : rule.getRhs().getEdges()) {
			Action action = edge.getAction();
			if (action.getType() == Type.CREATE) {
				Node src = edge.getSource();
				EObject nodeTarget = match.getNodeTarget(src.getActionNode());
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

	private SymmetricDifference createSymmetricDifference(Copier new2old, List<Change> resultChanges) {
		SymmetricFactory factory = SymmetricFactory.eINSTANCE;
		SymmetricDifference result = factory.createSymmetricDifference();
		Map<EObject, EObject> new2cor = new HashMap<EObject, EObject>();
		Map<EObject, EObject> old2new = new HashMap<EObject, EObject>();

		// Init lists
		for (Entry<EObject, EObject> entry : new2old.entrySet()) {
			Correspondence cor = factory.createCorrespondence();
			cor.setObjA(entry.getValue());
			cor.setObjB(entry.getKey());
			cor.setReliability(1.0F);
			result.getCorrespondences().add(cor);
			new2cor.put(entry.getKey(), cor);
			old2new.put(entry.getValue(), entry.getKey());
		}

		// Create changes
		List<symmetric.Change> changes = new ArrayList<symmetric.Change>();
		for (Change compound : resultChanges) {
			for (Change change : ((CompoundChangeImpl) compound).getChanges()) {
				if (change instanceof ObjectChangeImpl) {
					ObjectChangeImpl ch = (ObjectChangeImpl) change;
					createObjectChanges(new2old, old2new, factory, result, new2cor, changes, ch);
				} else if (change instanceof ReferenceChangeImpl) {
					ReferenceChangeImpl ch = (ReferenceChangeImpl) change;
					createReferenceChanges(new2old, old2new, factory, changes, ch);
				} else if (change instanceof AttributeChangeImpl) {
					AttributeChangeImpl ch = (AttributeChangeImpl) change;
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

	private void createAttributeChanges(Copier new2old, SymmetricFactory factory, List<symmetric.Change> changes,
			AttributeChangeImpl ch) {
		AttributeValueChange attrValueChange = factory.createAttributeValueChange();
		EAttribute attribute = ch.getAttribute();
		attrValueChange.setObjB(ch.getObject());
		attrValueChange.setObjA(new2old.get(ch.getObject()));
		attrValueChange.setType(attribute);
		changes.add(attrValueChange);
	}

	private void createReferenceChanges(Copier new2old, Map<EObject, EObject> old2new, SymmetricFactory factory,
			List<symmetric.Change> changes, ReferenceChangeImpl ch) {
		EReference reference = ch.getReference();

		if (reference.isMany()) {
			if (!ch.isCreate()) { // -> created (REVERSED!)
				AddReference addReference = factory.createAddReference();
				addReference.setTgt(ch.getTarget());
				addReference.setSrc(ch.getSource());
				addReference.setType(reference);
				changes.add(addReference);

				if (reference.isContainment() && new2old.get(ch.getTarget()) != null) {
					RemoveReference removeReference = factory.createRemoveReference();
					EObject oldTarget = new2old.get(ch.getTarget());
					removeReference.setSrc(oldTarget.eContainer());
					removeReference.setTgt(oldTarget);
					removeReference.setType(oldTarget.eContainmentFeature());
					changes.add(removeReference);
				}
			} else if (ch.isCreate()) { // -> deleted (REVERSED)
				RemoveReference removeReference = factory.createRemoveReference();
				EObject oldSource = new2old.get(ch.getSource());
				EObject oldTarget = new2old.get(ch.getTarget());
				removeReference.setSrc(oldSource);
				removeReference.setTgt(oldTarget);
				removeReference.setType(reference);
				changes.add(removeReference);
			}
		} else if (!reference.isMany()) {
			if (ch.isCreate()) { // -> created (NOT REVERSED!)
				AddReference addReference = factory.createAddReference();
				addReference.setTgt(ch.getTarget());
				addReference.setSrc(ch.getSource());
				addReference.setType(reference);
				changes.add(addReference);

				if (ch.getTarget() != null) {
					RemoveReference removeReference = factory.createRemoveReference();
					EObject oldSource = new2old.get(ch.getSource());
					EObject oldTarget = new2old.get(ch.getTarget());
					removeReference.setSrc(oldSource);
					removeReference.setTgt(oldTarget);
					removeReference.setType(reference);
					changes.add(removeReference);
				}
			} else if (!ch.isCreate()) { // > deleted (NOT REVERSED!)
				RemoveReference removeReference = factory.createRemoveReference();
				EObject oldSource = new2old.get(ch.getSource());
				EObject oldTarget = new2old.get(ch.getTarget()); // REVERSED
				removeReference.setSrc(oldSource);
				removeReference.setTgt(oldTarget);
				removeReference.setType(reference);
				changes.add(removeReference);
			}
		}

	}

	private void createObjectChanges(Copier new2old, Map<EObject, EObject> old2new, SymmetricFactory factory,
			SymmetricDifference result, Map<EObject, EObject> new2cor, List<symmetric.Change> changes,
			ObjectChangeImpl ch) {
		if (!ch.isCreate()) { // negated because object changes are
								// reversed
			AddObject addObject = factory.createAddObject();
			addObject.setObj(ch.getObject());
			changes.add(addObject);

			AddReference addReference = factory.createAddReference();
			addReference.setSrc(ch.getObject().eContainer());
			addReference.setTgt(ch.getObject());
			addReference.setType(ch.getObject().eContainmentFeature());
			changes.add(addReference);
		} else {
			RemoveObject removeObject = factory.createRemoveObject();
			EObject oldObject = new2old.get(ch.getObject());
			removeObject.setObj(oldObject);
			changes.add(removeObject);
			result.getCorrespondences().remove(new2cor.get(ch.getObject()));

			RemoveReference removeReference = factory.createRemoveReference();
			removeReference.setSrc(oldObject.eContainer());
			removeReference.setTgt(oldObject);
			removeReference.setType(oldObject.eContainmentFeature());
			changes.add(removeReference);
		}
	}

	private void saveResults(Copier oldModel, EGraphImpl graph, SymmetricDifference symmetric, List<Rule> appliedRules)
			throws IOException {
		HenshinResourceSet resourceSet = new HenshinResourceSet(new Path(outputPathPart1).toOSString());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

		File file = new File(outputPathPart1 + outputPathPart2);
		file.mkdirs();
		FileWriter writer = new FileWriter(new File(file, "appliedRules"));
		for (Rule rule : appliedRules) {
			writer.write(rule.getName());
			writer.write('\n');
		}
		writer.close();

		Resource res1 = resourceSet.createResource(outputPathPart2 + "1.uml");
		Resource res2 = resourceSet.createResource(outputPathPart2 + "2.uml");
		for (EObject root : graph.getRoots()) {
			if (!(root instanceof EPackage) && !(root instanceof EFactory) && !(root instanceof PrimitiveType)
					&& !(root instanceof EFactory) && !(root instanceof EFactory) && !(root instanceof Profile)) {
				res2.getContents().add(root);
			}
		}
		for (EObject old : oldModel.values()) {
			if (old.eContainer() == null)
				res1.getContents().add(old);
		}

		res1.save(null);
		res2.save(null);
		resourceSet.saveEObject(symmetric, outputPathPart2 + "1-to-2.symmetric");
		resourceSet.saveEObject(symmetric, "1-to-2.symmetric"); // for easier
																// debugging
	}

}
