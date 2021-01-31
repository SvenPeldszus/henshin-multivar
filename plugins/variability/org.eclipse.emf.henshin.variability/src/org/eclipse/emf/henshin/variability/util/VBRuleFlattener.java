package org.eclipse.emf.henshin.variability.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.impl.ModuleImpl;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.matcher.VBRuleInfo;
import org.sat4j.specs.ContradictionException;

import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 * This class prepares a variability-based rule for variability-based
 * application and matching. If the rule is expected to be used later, it is
 * required to call the undo() method after the application has been performed.
 *
 * @author Daniel Str�ber
 *
 */
public class VBRuleFlattener {
	public VBRuleInfo rule;
	public boolean checkDangling;
	private boolean injectiveMatching;
	private boolean injectiveMatchingOriginal;
	private boolean baseRule;

	public Set<Node> removeNodes;
	public Set<Edge> removeEdges;
	public Set<Attribute> removeAttributes;
	public Map<Edge, Node> removeEdgeSources;
	public Map<Edge, Node> removeEdgeTargets;
	public Set<Mapping> removeMappings;
	public Map<EObject, EObject> removeElementContainers;
	public Set<Formula> removeFormulas;
	public Map<Mapping, EStructuralFeature> removeMappingContainingRef;
	public Map<Formula, EStructuralFeature> removeFormulaContainingRef;
	public Map<Formula, EObject> pulledUpFormulasToContainer;
	public Map<Formula, EStructuralFeature> pulledUpFormulasToContainingRef;
	public Map<Formula, EObject> pulledUpFormulasToOldContainer;
	public Map<Formula, EStructuralFeature> pulledUpFormulasToOldContainingRef;
	private final Collection<String> trueFeatures;
	private final Collection<String> falseFeatures;

	public VBRuleFlattener(final VBRuleInfo rule, final Collection<String> trueFeatures, final Collection<String> falseFeatures) {
		this.rule = rule;
		this.trueFeatures = trueFeatures;
		this.falseFeatures = falseFeatures;
		this.checkDangling = rule.getRule().isCheckDangling();
	}

	public void saveFlattenedRules(final String path) throws ContradictionException, FileNotFoundException, IOException {
		final List<List<String>> trueFeatureList = new LinkedList<>();
		final List<List<String>> falseFeatureList = new LinkedList<>();
		new SatChecker(this.rule.getFeatureModel()).calculateTrueAndFalseFeatures(this.trueFeatures, this.falseFeatures,
				trueFeatureList, falseFeatureList, this.rule.getFeatures());
		final Set<Sentence> pcs = this.rule.getAllPCs().parallelStream().map(Map.Entry::getValue).collect(Collectors.toSet());
		final Module module = new ModuleImpl();
		final String name = this.rule.getRule().getName();
		module.setName(name);
		for (int i = 0; i < trueFeatureList.size(); i++) {
			final List<String> trueFeatures = trueFeatureList.get(i);
			final List<String> falseFeatures = falseFeatureList.get(i);
			final Set<Sentence> rejected = pcs.stream().filter(pc -> !Logic.evaluate(pc, trueFeatures, falseFeatures))
					.collect(Collectors.toSet());
			prepare(rejected, this.injectiveMatching, false, true);
			final Rule copy = EcoreUtil.copy(this.rule.getRule());
			module.getUnits().add(copy);
			undo();
		}
		final Resource resource = new HenshinResourceSet(path).createResource(name+".henshin");
		resource.getContents().add(module);
		resource.save(Collections.emptyMap());
	}

	/**
	 * Prepares the rule for variability-beckersed merging and rule application:
	 * rejected elements and removed and the "injective" flag is set. Assumes that
	 * the reject() method has been invoked method before.
	 *
	 * @param rule
	 * @param ruleInfo
	 * @param rejected
	 * @param executed
	 * @return
	 */
	public BitSet prepare(final Set<Sentence> rejected, final boolean injectiveMatching, final boolean baseRule,
			final boolean includeApplicationConditions) {
		this.baseRule = baseRule;
		this.injectiveMatching = injectiveMatching;
		this.injectiveMatchingOriginal = this.rule.getRule().isInjectiveMatching();
		this.removeElementContainers = new HashMap<>();
		this.removeNodes = new HashSet<>();
		this.removeEdges = new HashSet<>();
		this.removeAttributes = new HashSet<>();
		this.removeEdgeSources = new HashMap<>();
		this.removeEdgeTargets = new HashMap<>();
		this.removeMappings = new HashSet<>();
		this.removeMappingContainingRef = new HashMap<>();
		this.removeFormulaContainingRef = new HashMap<>();
		this.removeFormulas = new HashSet<>();

		fillMaps(this.rule, rejected, includeApplicationConditions);

		final BitSet bs = getRepresentation(this.removeAttributes, this.removeNodes, this.removeEdges, this.removeFormulas,
				injectiveMatching);

		doPreparation();
		return bs;
	}

	private void fillMaps(final VBRuleInfo ruleInfo, final Set<Sentence> rejected, final boolean includeApplicationConditions) {
		for (final Sentence expr : rejected) {
			final Set<ModelElement> elements = ruleInfo.getElementsWithPC(expr);
			if (elements == null) {
				continue;
			}

			for (final ModelElement ge : elements) {
				if (ge instanceof Node) {
					this.removeNodes.add((Node) ge);
					final Set<Mapping> mappings = ruleInfo.getMappings(ge);
					if (mappings != null) {
						this.removeMappings.addAll(mappings);
					}
					((Node) ge).getAllEdges().forEach(this.removeEdges::add);
				} else if (ge instanceof Edge) {
					this.removeEdges.add((Edge) ge);
				} else if (ge instanceof Attribute) {
					this.removeAttributes.add((Attribute) ge);
				}

			}
		}

		if (this.baseRule && (this.rule.getRule().getLhs().getFormula() != null)) {
			this.removeFormulas.add(this.rule.getRule().getLhs().getFormula());
			this.removeElementContainers.put(this.rule.getRule().getLhs().getFormula(), this.rule.getRule().getLhs());
			this.removeFormulaContainingRef.put(this.rule.getRule().getLhs().getFormula(),
					this.rule.getRule().getLhs().getFormula().eContainingFeature());

		} else {
			for (final NestedCondition ac : this.rule.getRule().getLhs().getNestedConditions()) {
				final Sentence acPC = ruleInfo.getPC(ac);
				if (!includeApplicationConditions || rejected.contains(acPC)) {
					Formula removeFormula = null;
					if (ac.isNAC()) {
						removeFormula = (Formula) ac.eContainer();
					} else if (ac.isPAC()) {
						removeFormula = ac;
					} else {
						throw new IllegalStateException("Unsupported formula: " + ac);
					}

					this.removeFormulas.add(removeFormula);
					this.removeElementContainers.put(removeFormula, removeFormula.eContainer());
					this.removeFormulaContainingRef.put(removeFormula, removeFormula.eContainingFeature());
				}
			}
		}
	}

	/**
	 * Prepares the rule for variability-based merging and rule application:
	 * rejected elements are removed and the "injective" flag is set. Assumes that
	 * the reject() method has been invoked method before.
	 */
	public void doPreparation() {
		if (this.removeNodes == null) {
			throw new IllegalStateException("This method may only be invoked after reject() has been invoked.");
		}

		removeFormulas();

		for (final Mapping m : this.removeMappings) {
			final EStructuralFeature feature = m.eContainingFeature();
			this.removeMappingContainingRef.put(m, feature);
			final EObject eContainer = m.eContainer();
			this.removeElementContainers.put(m, eContainer);
			((EList<EObject>) eContainer.eGet(feature)).remove(m);
		}
		for (final Attribute a : this.removeAttributes) {
			this.removeElementContainers.put(a, a.getNode());
			a.getNode().getAttributes().remove(a);
		}
		for (final Edge e : this.removeEdges) {
			this.removeElementContainers.put(e, e.getGraph());
			this.removeEdgeSources.put(e, e.getSource());
			this.removeEdgeTargets.put(e, e.getTarget());
			e.getGraph().getEdges().remove(e);
			e.getSource().getOutgoing().remove(e);
			e.getTarget().getIncoming().remove(e);
		}

		for (final Node n : this.removeNodes) {
			this.removeElementContainers.put(n, n.getGraph());
			n.getGraph().getNodes().remove(n);
		}
		this.rule.getRule().setInjectiveMatching(this.injectiveMatching);
		this.rule.getRule().setCheckDangling(false); // Dangling edges are allowed in a partial
		// match.

	}

	/**
	 * Removes the formulas in the previously determined order.
	 */
	private void removeFormulas() {
		for (final Formula formula : this.removeFormulas) {
			this.removeElementContainers.get(formula).eUnset(this.removeFormulaContainingRef.get(formula));
			this.removeElementContainers.get(formula).eSet(this.removeFormulaContainingRef.get(formula),
					HenshinFactory.eINSTANCE.createTrue());
		}
	}

	/**
	 * Puts the rule back in its original state: rejected elements and
	 * "injectiveMatching" flag are restored.
	 */
	public void undo() {
		this.rule.getRule().setCheckDangling(this.checkDangling);
		this.rule.getRule().setInjectiveMatching(this.injectiveMatchingOriginal);

		for (final Node n : this.removeNodes) {
			((Graph) this.removeElementContainers.get(n)).getNodes().add(n);
		}
		for (final Edge e : this.removeEdges) {
			((Graph) this.removeElementContainers.get(e)).getEdges().add(e);
			this.removeEdgeSources.get(e).getOutgoing().add(e);
			this.removeEdgeTargets.get(e).getIncoming().add(e);
		}

		for (final Attribute a : this.removeAttributes) {
			((Node) this.removeElementContainers.get(a)).getAttributes().add(a);
		}

		for (final Mapping m : this.removeMappings) {
			final EStructuralFeature feature = this.removeMappingContainingRef.get(m);
			@SuppressWarnings("unchecked")
			final
			EList<Mapping> list = (EList<Mapping>) this.removeElementContainers.get(m).eGet(feature);
			list.add(m);
		}

		restoreFormulas();

	}

	/**
	 * Restores the formulas in the previously determined order.
	 */
	private void restoreFormulas() {
		for (final Formula formula : this.removeFormulas) {
			this.removeElementContainers.get(formula).eSet(this.removeFormulaContainingRef.get(formula), formula);
		}
		// fix
		// for (Formula f:pulledUpFormulasToOldContainingRef.keySet()) {
		// EObject container = pulledUpFormulasToOldContainer.get(f);
		// container.eSet(pulledUpFormulasToOldContainingRef.get(f), f);
		// }
		// for (Formula f:removeFormulaContainingRef.keySet()) {
		// EObject container = removeElementContainers.get(f);
		// container.eSet(removeFormulaContainingRef.get(f), f);
		// }
	}

	/**
	 * Calling this method ensures that the elements to be removed can later be
	 * added in the correct order to produce the original rule.
	 *
	 * @param formulas All instances must be either a NestedCondition (i.e., a
	 *                 Graph) or a NOT
	 */
	private void determineRemoveOrder(final Set<Formula> formulas) {
		final Formula outer = this.rule.getRule().getLhs().getFormula(); //
		if ((outer instanceof Not) || (outer instanceof NestedCondition)) {
			final Formula formula = formulas.iterator().next();
			if (formula == outer) {
				this.removeElementContainers.put(formula, this.rule.getRule().getLhs());
				this.removeFormulaContainingRef.put(formula, HenshinPackage.Literals.GRAPH__FORMULA);
			}
		} else if (outer instanceof And) {
			determineRemoverOrder((And) outer, formulas, this.rule.getRule().getLhs(),
					HenshinPackage.Literals.GRAPH__FORMULA);
		} else {
			throw new IllegalArgumentException(
					"TODO: Only AND-based nesting of applications conditions supported yet.");
		}
	}

	private void determineRemoverOrder(final And and, final Set<Formula> formulas, final EObject container, final EReference feature) {
		if (formulas.contains(and.getLeft()) && formulas.contains(and.getRight())) {
			this.removeFormulaContainingRef.put(and, feature);
			this.removeElementContainers.put(and, container);
		}
		if (!formulas.contains(and.getLeft()) && formulas.contains(and.getRight())) {
			designatePullupChild(and.getLeft(), and, HenshinPackage.Literals.BINARY_FORMULA__LEFT, container, feature);
		}
		if (formulas.contains(and.getLeft()) && !formulas.contains(and.getRight())) {
			designatePullupChild(and.getRight(), and, HenshinPackage.Literals.BINARY_FORMULA__RIGHT, container,
					feature);

		}
		if (!formulas.contains(and.getLeft()) && !formulas.contains(and.getRight())) {
			if (and.getLeft() instanceof And) {
				determineRemoverOrder((And) and.getLeft(), formulas, and, HenshinPackage.Literals.BINARY_FORMULA__LEFT);
			}
			if (and.getRight() instanceof And) {
				determineRemoverOrder((And) and.getRight(), formulas, and,
						HenshinPackage.Literals.BINARY_FORMULA__RIGHT);
			}
		}
	}

	private void designatePullupChild(final Formula formula, final And oldContainer, final EReference oldFeature, final EObject newContainer,
			final EReference newFeature) {
		this.removeFormulaContainingRef.put(oldContainer, newFeature);
		this.removeElementContainers.put(oldContainer, newContainer);
		this.pulledUpFormulasToContainingRef.put(formula, newFeature);
		this.pulledUpFormulasToContainer.put(formula, newContainer);
		this.pulledUpFormulasToOldContainingRef.put(formula, oldFeature);
		this.pulledUpFormulasToOldContainer.put(formula, oldContainer);
	}

	/**
	 * A representation of the removed elements in a rule as a bit set. Aims at
	 * avoiding to match the same sub-rule on the same input twice.
	 *
	 * @param rule
	 * @param deleteAttributes
	 * @param deleteNodes
	 * @param deleteEdges
	 * @param deleteFormula
	 * @param injectiveMatching
	 * @return
	 */
	private BitSet getRepresentation(final Set<Attribute> deleteAttributes, final Set<Node> deleteNodes, final Set<Edge> deleteEdges,
			final Set<Formula> deleteFormula, final boolean injectiveMatching) {
		final BitSet result = new BitSet(this.rule.getRule().getLhs().getNodes().size() + this.rule.getRule().getLhs().getEdges().size()
				+ this.rule.getRule().getLhs().getNestedConditions().size() + 1);

		result.set(0, injectiveMatching);
		int i = 1;

		for (final NestedCondition nc : this.rule.getRule().getLhs().getNestedConditions()) {
			result.set(i, !deleteFormula.contains(nc));
			i++;
		}

		for (final Node n : this.rule.getRule().getLhs().getNodes()) {
			result.set(i, !deleteNodes.contains(n));
			i++;
			for (final Attribute a : n.getAttributes()) {
				result.set(i, !deleteAttributes.contains(a));
				i++;
			}
		}
		for (final Edge e : this.rule.getRule().getLhs().getEdges()) {
			result.set(i, !deleteEdges.contains(e));
			i++;
		}

		for (final Node n : this.rule.getRule().getRhs().getNodes()) {
			result.set(i, !deleteNodes.contains(n));
			i++;
			for (final Attribute a : n.getAttributes()) {
				result.set(i, !deleteAttributes.contains(a));
				i++;
			}
		}
		for (final Edge e : this.rule.getRule().getRhs().getEdges()) {
			result.set(i, !deleteEdges.contains(e));
			i++;
		}

		return result;
	}

	public VBRuleFlattener getSnapShot() {
		final VBRuleFlattener result = new VBRuleFlattener(this.rule, getTrueFeatures(), getFalseFeatures());
		result.removeNodes = new HashSet<>(this.removeNodes);
		result.removeEdges = new HashSet<>(this.removeEdges);
		result.removeAttributes = new HashSet<>(this.removeAttributes);
		result.removeEdgeSources = new HashMap<>(this.removeEdgeSources);
		result.removeEdgeTargets = new HashMap<>(this.removeEdgeTargets);
		result.removeElementContainers = new HashMap<>(this.removeElementContainers);
		result.removeFormulas = new HashSet<>(this.removeFormulas);
		result.removeFormulaContainingRef = new HashMap<>(this.removeFormulaContainingRef);
		// result.pulledUpFormulasToContainer = new HashMap<Formula, EObject>(
		// pulledUpFormulasToContainer);
		// result.pulledUpFormulasToContainingRef = new HashMap<Formula,
		// EStructuralFeature>(
		// pulledUpFormulasToContainingRef);
		result.removeMappings = new HashSet<>(this.removeMappings);
		result.removeMappingContainingRef = new HashMap<>(this.removeMappingContainingRef);
		// result.pulledUpFormulasToOldContainingRef = new HashMap<Formula,
		// EStructuralFeature>();
		// result.pulledUpFormulasToOldContainer = new HashMap<Formula, EObject>();

		return result;

	}

	public List<NestedCondition> getApplicationConditions() {
		return this.removeFormulas.parallelStream().filter(NestedCondition.class::isInstance)
				.map(NestedCondition.class::cast).filter(cond -> cond.isNAC() || cond.isPAC())
				.collect(Collectors.toList());
	}

	/**
	 * @return the trueFeatures
	 */
	public Collection<String> getTrueFeatures() {
		return this.trueFeatures;
	}

	/**
	 * @return the falseFeatures
	 */
	public Collection<String> getFalseFeatures() {
		return this.falseFeatures;
	}

}