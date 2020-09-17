package org.eclipse.emf.henshin.variability.mergein.refactoring.logic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.MappingList;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Parameter;
//import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.util.XorEncoderUtil;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import mergeSuggestion.MergeNAC;
import mergeSuggestion.MergePAC;
import mergeSuggestion.MergeRule;
import mergeSuggestion.MergeRuleElement;
import mergeSuggestion.MergeSuggestionFactory;

public class NewMerger {

	private static final String VAR = "Var";
	private static final String COMMA = ",";
	private static final String BRACKET_LEFT = "(";
	private static final String XOR = "xor";
	private static final String AC = "AC";
	private static final String BRACKET_RIGHT = ")";
	private static final String OR = " or ";

	private MergeRule mergeRule;
	private Rule masterRule;
	private List<Rule> furtherRules;
	private int numberOfRules;
	private Module henshinModule;
	private HashMap<Node, Node> nodesMap;
	private HashMap<Rule, String> rule2ConditionMap;
	private EList<MergeRuleElement> elementsLhs, elementsRhs, elementsConditions, elementsNestedRules;
	private EList<ParameterRulesMapping> parameterRulesMappings;
	private EList<MergeRuleElementRulesMapping> mergeRuleElementRulesMappings;
	private boolean unifyParameters;
	private boolean optimizePerformance;

	public NewMerger(MergeRule m) throws MergeInException {
		this(m, false, false);
	}

	public NewMerger(MergeRule m, boolean unifyParams, boolean optimizePerformance) throws MergeInException {
		this.optimizePerformance = optimizePerformance;
		this.mergeRule = m;
		this.unifyParameters = unifyParams;
		this.masterRule = mergeRule.getMasterRule();
		this.furtherRules = new ArrayList<>();
		this.furtherRules.addAll(mergeRule.getRules());
		this.furtherRules.remove(masterRule);
		this.numberOfRules = furtherRules.size() + 1;
		setModule();
		this.nodesMap = new HashMap<>();
		this.rule2ConditionMap = new LinkedHashMap<>();
		this.elementsLhs = new BasicEList<>();
		this.elementsRhs = new BasicEList<>();
		this.elementsConditions = new BasicEList<>();
		this.elementsNestedRules = new BasicEList<>();
		this.parameterRulesMappings = new BasicEList<>();
		this.mergeRuleElementRulesMappings = new BasicEList<>();
	}

	public void saveModule(String path) throws MergeInException {
		File file = new File(path);
		HenshinResourceSet resourceSet = new HenshinResourceSet(file.getParent());
		Resource resource = resourceSet.createResource(path);
		resource.getContents().add(henshinModule);
		try {
			resource.save(null);
		} catch (IOException e) {
			throw new MergeInException(MergeInException.SAVING_IMPOSSIBLE);
		}
	}

	public void merge() throws MergeInException {
		prepareMerging();
		mergeParameters();
		mergeLhsRhsGraphs();
		mergeConditions();
		// mergeNestedRules();
		completeMerging();
	}

	private void prepareMerging() {
		sortMergeRuleElements();
		setPresenceConditions();
		fillNodesMap();
		partitionMergeRuleElements();
	}

	private void mergeParameters() {
		if (unifyParameters) {
			unifyParameterNames();
		}
		mergeParametersIntoMasterRule();
	}

	private void mergeLhsRhsGraphs() {
		mergeLhsGraph();
		mergeRhsGraph();
		addLhsRhsMapping();
	}

	private void mergeConditions() {
		c: for (MergeNAC mergeNAC : mergeRule.getMergeNacs()) {
			for (Graph refNAC : mergeNAC.getReferenceNACs())
				for (NestedCondition masterNac : masterRule.getLhs().getNACs())
					if (masterNac.getConclusion() == refNAC)
						continue c; // mergeNAC already merged -> skip
			Graph nonMasterNac = mergeNAC.getReferenceNACs().get(0);
			addNestedCondition2Graph((NestedCondition) nonMasterNac.eContainer(), true);
		}

		d: for (MergePAC mergePAC : mergeRule.getMergePacs()) {
			for (Graph refPAC : mergePAC.getReferencePACs())
				for (NestedCondition masterPac : masterRule.getLhs().getPACs())
					if (masterPac.getConclusion() == refPAC)
						continue d;// mergePAC already merged -> skip
			Graph nonMasterPac = mergePAC.getReferencePACs().get(0);
			addNestedCondition2Graph((NestedCondition) nonMasterPac.eContainer(), false);
		}

		int number = 1;
		for (NestedCondition nac : masterRule.getLhs().getNACs()) {
			nac.getConclusion().setName("" + number);
			number++;
		}
		for (NestedCondition pac : masterRule.getLhs().getPACs()) {
			pac.getConclusion().setName("" + number);
			number++;
		}
	}

	private void completeMerging() throws MergeInException {
		setInjectiveMatching();
		setFeatureModel();
		setFeatures();
		setAnnotation();
		if (furtherRules.isEmpty()) {
			masterRule.setName(masterRule.getName() + "-var");
		} else {
			for (Rule rule : furtherRules) {
				masterRule.setName(masterRule.getName() + "," + rule.getName());
			}
		}
		removeNonMasterRules();
	}

	private void setAnnotation() {
		Annotation anno = HenshinFactory.eINSTANCE.createAnnotation();
		StringBuilder sb = new StringBuilder();
		for (Entry<Rule, String> entry : rule2ConditionMap.entrySet()) {
			sb.append(entry.getValue());
			sb.append('=');
			sb.append(entry.getKey());
			sb.append(", ");
		}
		String value = sb.toString();
		value = value.substring(0, value.length() - 2);
		anno.setKey("originalRules");
		anno.setValue(value);
		masterRule.getAnnotations().add(anno);
	}

	private void removeNonMasterRules() throws MergeInException {
		for (Rule rule : furtherRules) {
			if (rule.getModule() == null) {
				throw new MergeInException(MergeInException.NO_MODULE_2);
			}
		}
		henshinModule.getUnits().removeAll(furtherRules);
	}

	private void setFeatureModel() {
		List<String> features = new ArrayList<>(furtherRules.size() + 1);
		String masterRuleFeature = createFeature(masterRule);
		features.add(masterRuleFeature);
		VariabilityHelper.INSTANCE.addFeature(masterRule, masterRuleFeature);
		for(Rule rule : furtherRules) {
			String feature = createFeature(rule);
			VariabilityHelper.INSTANCE.addFeature(masterRule, feature);
			features.add(feature);
		}
		
		String featureModel;
		if (optimizePerformance) {
			// Already translate the fm to a CNF
			featureModel = XorEncoderUtil.createXorEncodingCNF(features);
			VariabilityHelper.INSTANCE.setFeatureConstraintIsCNF(masterRule, true);
		} else {
			featureModel = XOR;
			featureModel += BRACKET_LEFT;
			featureModel += masterRuleFeature;
			for (String feature : features) {
				featureModel += COMMA;
				featureModel += feature;
			}
			featureModel += BRACKET_RIGHT;
		}
		VariabilityHelper.INSTANCE.setFeatureConstraint(masterRule, featureModel);
	}

	private void setFeatures() {
		Set<String> sb = new HashSet<>();
		for (Entry<Rule, String> entry : rule2ConditionMap.entrySet()) {
			sb.add(createFeature(entry.getKey()));
		}
		VariabilityHelper.INSTANCE.setFeatures(masterRule, sb);
	}

	private void addNestedCondition2Graph(NestedCondition condition, boolean nac) {
		Formula formula = condition;
		if (nac) {
			formula = HenshinFactory.eINSTANCE.createNot();
			((Not) formula).setChild(condition);
		}

		if (masterRule.getLhs().getFormula() == null) {
			masterRule.getLhs().setFormula(formula);
		} else {
			And and = HenshinFactory.eINSTANCE.createAnd();
			and.setLeft(masterRule.getLhs().getFormula());
			and.setRight(formula);
			masterRule.getLhs().setFormula(and);
		}
		adjustMappings(formula);
	}

	private void adjustMappings(Formula formula) {
		if (formula instanceof NestedCondition) {
			NestedCondition nestedCondition = (NestedCondition) formula;
			Iterator<Mapping> iter = nestedCondition.getMappings().iterator();
			while (iter.hasNext()) {
				Mapping mapping = iter.next();
				Node oldOrigin = mapping.getOrigin();
				mapping.setOrigin(nodesMap.get(oldOrigin));
			}
		}
		if (formula instanceof Not) {
			Not not = (Not) formula;
			adjustMappings(not.getChild());
		}
	}

	private EList<Rule> getRulesWithCondition() {
		EList<Rule> rulesWithCondition = new BasicEList<>();
		for (MergeRuleElementRulesMapping mapping : mergeRuleElementRulesMappings) {
			Rule rule = mapping.getRules().get(0);
			if (rule != masterRule) {
				rulesWithCondition.add(rule);
			}
		}
		return rulesWithCondition;
	}

	private void prepareRuleSets() {
		for (MergeRuleElement mre : elementsConditions) {
			MergeRuleElementRulesMapping mapping = new MergeRuleElementRulesMapping(this, mre);
			for (GraphElement elem : mre.getReferenceElements()) {
				mapping.addRule(elem.getGraph().getRule());
			}
			if (!mappingContainsRuleList(mapping)) {
				mergeRuleElementRulesMappings.add(mapping);
			}
			setConditionName(mre);
		}
	}

	private void setConditionName(MergeRuleElement mre) {
		for (GraphElement elem : mre.getReferenceElements()) {
			if (elem.getGraph().isNestedCondition()) {
				EList<Rule> affectedRules = getRulesFromMergeRuleElement(mre);
				String str = "";
				for (Rule rule : affectedRules) {
					str += rule.getName();
				}
				elem.getGraph().setName(AC + str);
			}
		}
	}

	private boolean mappingContainsRuleList(MergeRuleElementRulesMapping mapping) {
		for (MergeRuleElementRulesMapping m : mergeRuleElementRulesMappings) {
			if (m.hasSameRuleList(mapping)) {
				return true;
			}
		}
		return false;
	}

	private void addLhsRhsMapping() {
		for (Rule rule : furtherRules) {
			MappingList mappingList = rule.getMappings();
			addMappings2Rule(masterRule, mappingList);
		}
	}

	private void addMappings2Rule(Rule rule, MappingList mappingList) {
		Iterator<Mapping> iter = mappingList.iterator();
		while (iter.hasNext()) {
			Mapping mapping = iter.next();
			Node origin = nodesMap.get(mapping.getOrigin());
			Node image = nodesMap.get(mapping.getImage());
			if (rule.getMappings().get(origin, image) == null) {
				rule.getMappings().add(HenshinFactory.eINSTANCE.createMapping(origin, image));
			}
		}
	}

	private void mergeRhsGraph() {
		List<Node> nodesRhs = new ArrayList<>();
		List<Edge> edgesRhs = new ArrayList<>();
		List<Attribute> attributesRhs = new ArrayList<>();
		fillLists(nodesRhs, edgesRhs, attributesRhs, elementsRhs);
		addNodes2GraphOfMasterRule(masterRule.getRhs(), nodesRhs);
		addAttributes2Nodes(attributesRhs);
		addEdges2Graph(masterRule.getRhs(), edgesRhs);
	}

	private void mergeLhsGraph() {
		List<Node> nodesLhs = new ArrayList<>();
		List<Edge> edgesLhs = new ArrayList<>();
		List<Attribute> attributesLhs = new ArrayList<>();
		fillLists(nodesLhs, edgesLhs, attributesLhs, elementsLhs);
		addNodes2GraphOfMasterRule(masterRule.getLhs(), nodesLhs);
		addAttributes2Nodes(attributesLhs);
		addEdges2Graph(masterRule.getLhs(), edgesLhs);
	}

	private void addEdges2Graph(Graph graph, List<Edge> edges) {
		for (Edge edge : edges) {
			Node sourceNew = nodesMap.get(edge.getSource());
			Node targetNew = nodesMap.get(edge.getTarget());
			Edge e = sourceNew.getOutgoing(edge.getType(), targetNew);
			if (e == null) {
				edge.setSource(sourceNew);
				edge.setTarget(targetNew);
				graph.getEdges().add(edge);
			} else {
				if (e == edge && !graph.getEdges().contains(edge)) {
					graph.getEdges().add(edge);
				}
			}
		}
	}

	private void addAttributes2Nodes(List<Attribute> attributes) {
		for (Attribute attribute : attributes) {
			Node newOwningNode = nodesMap.get(attribute.getNode());
			// if (newOwningNode.getAttribute(attribute.getType()) == null) {
			newOwningNode.getAttributes().add(attribute);
			// }
		}
	}

	private void addNodes2GraphOfMasterRule(Graph graph, List<Node> nodes) {
		for (Node node : nodes) {
			if (nodesMap.get(node) == node && node.getGraph().getRule() != masterRule) {
				graph.getNodes().add(node);
			}
		}
	}

	private void fillLists(List<Node> nodes, List<Edge> edges, List<Attribute> attributes,
			EList<MergeRuleElement> elements) {
		for (MergeRuleElement mre : elements) {
			for (GraphElement elem : mre.getReferenceElements()) {
				if (elem instanceof Node) {
					nodes.add((Node) elem);
				}
				if (elem instanceof Edge) {
					edges.add((Edge) elem);
				}
				if (elem instanceof Attribute) {
					attributes.add((Attribute) elem);
				}
			}
		}
	}

	private void mergeParametersIntoMasterRule() {
		for (Rule rule : furtherRules) {
			for (Parameter param : rule.getParameters()) {
				if (!masterRuleContainsParameter(param)) {
					Parameter paramCopy = HenshinFactory.eINSTANCE.createParameter();
					paramCopy.setName(param.getName());
					if (param.getType() != null) {
						paramCopy.setType(param.getType());
					}
					masterRule.getParameters().add(paramCopy);
				}
			}
		}
	}

	private boolean masterRuleContainsParameter(Parameter param) {
		for (Parameter p : masterRule.getParameters()) {
			if (p.getName().equals(param.getName())) {
				if (p.getType() == null && param.getType() == null) {
					return true;
				}
				if (p.getType() != null && param.getType() != null && p.getType() == param.getType()) {
					return true;
				}
			}
		}
		return false;
	}

	private void unifyParameterNames() {
		for (Parameter param : masterRule.getParameters()) {
			ParameterRulesMapping parameterRulesMapping = new ParameterRulesMapping(param, this);
			assignRules2Parameter(parameterRulesMapping);
			parameterRulesMappings.add(parameterRulesMapping);
		}
		for (Rule rule : furtherRules) {
			for (Parameter param : rule.getParameters()) {
				ParameterRulesMapping parameterRulesMapping = new ParameterRulesMapping(param, this);
				assignRules2Parameter(parameterRulesMapping);
				parameterRulesMappings.add(parameterRulesMapping);
			}
		}
		for (ParameterRulesMapping mapping : parameterRulesMappings) {
			Parameter param = mapping.getParameter();
			String oldName = param.getName();
			String newName = "";
			for (Rule rule : mapping.getRules()) {
				if (rule.getName() != null && !rule.getName().isEmpty()) {
					newName += firstToUpper(rule.getName().toLowerCase());
				}
			}
			newName += firstToUpper(oldName);
			param.setName(newName);
			updateReferences2Parameter(((Rule) param.getUnit()), oldName, newName);
		}
	}

	private void updateReferences2Parameter(Rule rule, String oldName, String newName) {
		for (MergeRuleElement mre : mergeRule.getElements()) {
			for (GraphElement ge : mre.getReferenceElements()) {
				if (ge instanceof Node && ((Node) ge).getName() != null && ((Node) ge).getName().equals(oldName)) {
					if (ge.getGraph().getRule().getKernelRule() != null) {
						if (ge.getGraph().getRule().getKernelRule() == rule) {
							((Node) ge).setName(newName);
						}
					} else {
						if (ge.getGraph().getRule() == rule) {
							((Node) ge).setName(newName);
						}
					}
				}
				if (ge instanceof Attribute && ((Attribute) ge).getValue().equals(oldName)) {
					if (ge.getGraph().getRule().getKernelRule() != null) {
						if (ge.getGraph().getRule().getKernelRule() == rule) {
							((Attribute) ge).setValue(newName);
						}
					} else {
						if (ge.getGraph().getRule() == rule) {
							((Attribute) ge).setValue(newName);
						}
					}
				}
			}
		}
	}

	private String firstToUpper(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private void assignRules2Parameter(ParameterRulesMapping mapping) {
		Parameter param = mapping.getParameter();
		for (MergeRuleElement mre : mergeRule.getElements()) {
			if (getRulesFromMergeRuleElement(mre).contains((Rule) param.getUnit())) {
				for (GraphElement ge : mre.getReferenceElements()) {
					if (ge instanceof Node && ((Node) ge).getName() != null
							&& ((Node) ge).getName().equals(param.getName())) {
						if (ge.getGraph().getRule().getKernelRule() != null) {
							mapping.addRule(ge.getGraph().getRule().getKernelRule());
						} else {
							mapping.addRule(ge.getGraph().getRule());
						}
					}
					if (ge instanceof Attribute && ((Attribute) ge).getValue().equals(param.getName())) {
						if (ge.getGraph().getRule().getKernelRule() != null) {
							mapping.addRule(ge.getGraph().getRule().getKernelRule());
						} else {
							mapping.addRule(ge.getGraph().getRule());
						}
					}
				}
			}
		}
	}

	private EList<Rule> getRulesFromMergeRuleElement(MergeRuleElement mre) {
		EList<Rule> rules = new BasicEList<Rule>();
		for (GraphElement ge : mre.getReferenceElements()) {
			if (ge.getGraph().getRule().getKernelRule() != null) {
				rules.add(ge.getGraph().getRule().getKernelRule());
			} else {
				rules.add(ge.getGraph().getRule());
			}
		}
		return rules;
	}

	private void partitionMergeRuleElements() {
		for (MergeRuleElement mre : mergeRule.getElements()) {
			GraphElement elem = mre.getReferenceElements().get(0);
			if (elem.getGraph().getRule().getKernelRule() != null) {
				elementsNestedRules.add(mre);
				continue;
			}
			if (elem.getGraph().isLhs()) {
				elementsLhs.add(mre);
				continue;
			}
			if (elem.getGraph().isRhs()) {
				elementsRhs.add(mre);
				continue;
			}
			if (elem.getGraph().isNestedCondition()) {
				elementsConditions.add(mre);
				setPresenceCondition(mre);
				continue;
			}
		}
	}

	private void setPresenceCondition(MergeRuleElement mre) {
		for (GraphElement elem : mre.getReferenceElements()) {
			String presenceCondition = VariabilityHelper.INSTANCE.getPresenceConditionIfModelElement(elem);
			if (elem.getGraph().isNestedCondition()) {
				NestedCondition condition = (NestedCondition) elem.getGraph().eContainer();
				VariabilityHelper.INSTANCE.setPresenceCondition(condition, presenceCondition);
			}
		}
	}

	private void fillNodesMap() {
		for (MergeRuleElement mre : mergeRule.getElements()) {
			if (mre.getReferenceElements().get(0) instanceof Node) {
				Node baseNode = (Node) mre.getReferenceElements().get(0);
				for (GraphElement elem : mre.getReferenceElements()) {
					Node node = (Node) elem;
					nodesMap.put(node, baseNode);
				}
			}
		}
	}

	private void setPresenceConditions() {
		int i = 0;
		for (Rule rule : mergeRule.getRules()) {
			rule2ConditionMap.put(rule, str(i));
			i++;
		}

		for (MergeRuleElement mre : mergeRule.getElements()) {
			if (mre.getReferenceElements().size() != numberOfRules) {
				for (GraphElement elem : mre.getReferenceElements()) {
					String condition = getCondition(mre);
					VariabilityHelper.INSTANCE.setPresenceCondition((ModelElement) elem, condition);
				}
			}
		}

		for (MergeNAC nac : mergeRule.getMergeNacs()) {
			if (nac.getReferenceNACs().size() != numberOfRules) {
				for (Graph g : nac.getReferenceNACs()) {
					setPresenceCondition(nac, g);
				}
			}
		}
	}

	static String str(int i) {
		return i < 0 ? "" : str((i / 26) - 1) + (char) (65 + i % 26);
	}

	private void setPresenceCondition(MergeNAC nac, Graph g) {
		String condition = getCondition(nac);
		VariabilityHelper.INSTANCE.setPresenceCondition(((NestedCondition) g.eContainer()), condition);
		for (Node node : g.getNodes()) {
			if (node.getActionNode() == node)
				VariabilityHelper.INSTANCE.setPresenceCondition(node, condition);

			for (Attribute attribute : node.getAttributes()) {
				if (attribute.getActionAttribute() == attribute)
					VariabilityHelper.INSTANCE.setPresenceCondition(attribute, condition);
			}
		}
		for (Edge edge : g.getEdges()) {
			if (edge.getActionEdge() == edge)
				VariabilityHelper.INSTANCE.setPresenceCondition(edge, condition);
		}
	}

	private String getCondition(MergeNAC nac) {
		List<Rule> affectedRules = new ArrayList<>();
		for (Graph el : nac.getReferenceNACs()) {
			affectedRules.add(el.getRule());
		}
		return getCondition(affectedRules);
	}

	private String getCondition(MergeRuleElement mre) {
		List<Rule> affectedRules = new ArrayList<>();
		for (GraphElement elem : mre.getReferenceElements()) {
			affectedRules.add(elem.getGraph().getRule());
		}
		return getCondition(affectedRules);
	}

	private String getCondition(List<Rule> affectedRules) {
		String condition = "";
		if (!affectedRules.isEmpty()) {
			condition = createFeature(affectedRules.get(0));
		}
		for (int i = 1; i < affectedRules.size(); i++) {
			condition += OR + createFeature(affectedRules.get(i));
		}
		return condition;
	}

	private String createFeature(Rule rule) {
		String ruleName = rule.getName();
		ruleName = ruleName.replaceAll("\\W", "");
		ruleName = ruleName.replace("_", "");
		return ruleName.toLowerCase();
	}

	private void sortMergeRuleElements() {
		EList<MergeRuleElement> elements = new BasicEList<MergeRuleElement>();
		for (MergeRuleElement mre : mergeRule.getElements()) {
			elements.add(getSortedElement(mre));
		}
		mergeRule.getElements().clear();
		mergeRule.getElements().addAll(elements);
	}

	private MergeRuleElement getSortedElement(MergeRuleElement mre) {
		int oldSize = mre.getReferenceElements().size();
		MergeRuleElement elem = MergeSuggestionFactory.eINSTANCE.createMergeRuleElement();
		for (GraphElement ge : mre.getReferenceElements()) {
			if (ge.getGraph().getRule() == masterRule) {
				elem.getReferenceElements().add(ge);
				break;
			}
		}
		for (int i = 0; i < furtherRules.size(); i++) {
			Rule rule = furtherRules.get(i);
			for (GraphElement ge : mre.getReferenceElements()) {
				if (ge.getGraph().getRule() == rule) {
					elem.getReferenceElements().add(ge);
					break;
				}
			}
		}
		if (elem.getReferenceElements().size() != oldSize)
			throw new RuntimeException("Error during sorting!");
		return elem;
	}

	private void setModule() throws MergeInException {
		if (mergeRule != null) {
			if (masterRule != null) {
				if (masterRule.getModule() != null) {
					henshinModule = masterRule.getModule();
				} else {
					throw new MergeInException(MergeInException.NO_MODULE);
				}
			} else {
				throw new MergeInException(MergeInException.NO_MASTERRULE);
			}
		} else {
			throw new MergeInException(MergeInException.NO_MERGERULE);
		}
	}

	private void setInjectiveMatching() {
		List<Rule> injective = new ArrayList<Rule>();
		for (Rule rule : mergeRule.getRules()) {
			if (rule.isInjectiveMatching())
				injective.add(rule);
		}
		if (injective.isEmpty()) {
			VariabilityHelper.INSTANCE.setInjectiveMatchingPresenceCondition(masterRule, "false");
		} else if (injective.size() == mergeRule.getRules().size()) {
			// NOOP, is default.
		} else {
			String condition = createFeature(injective.get(0));
			for (int i = 1; i < injective.size(); i++) {
				condition += OR + createFeature(injective.get(i));
			}
			VariabilityHelper.INSTANCE.setInjectiveMatchingPresenceCondition(masterRule, condition);
		}
	}

	MergeRule getMergeRule() {
		return mergeRule;
	}

	HashMap<Node, Node> getNodesMap() {
		return nodesMap;
	}

	EList<MergeRuleElement> getElementsLhs() {
		return elementsLhs;
	}

	EList<MergeRuleElement> getElementsRhs() {
		return elementsRhs;
	}

	EList<MergeRuleElement> getElementsConditions() {
		return elementsConditions;
	}

	EList<MergeRuleElement> getElementsNestedRules() {
		return elementsNestedRules;
	}

	Rule getMasterRule() {
		return masterRule;
	}

	List<Rule> getFurtherRules() {
		return furtherRules;
	}

	EList<MergeRuleElementRulesMapping> getMergeRuleElementRulesMappings() {
		return mergeRuleElementRulesMappings;
	}

	Module getModule() {
		return henshinModule;
	}
}
