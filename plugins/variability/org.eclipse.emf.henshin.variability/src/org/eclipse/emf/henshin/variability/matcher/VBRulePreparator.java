package org.eclipse.emf.henshin.variability.matcher;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.wrapper.VariabilityHelper;

import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 * This class prepares a variability-based rule for variability-based
 * application and matching. If the rule is expected to be used later, it is
 * required to call the undo() method after the application has been performed.
 *
 * @author Daniel Str�ber
 *
 */
public class VBRulePreparator {
	public Rule rule;
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

	public VBRulePreparator(Rule rule, Collection<String> trueFeatures, Collection<String> falseFeatures) {
		this.rule = rule;
		this.trueFeatures = trueFeatures;
		this.falseFeatures = falseFeatures;
		this.checkDangling = rule.isCheckDangling();
	}

	/**
	 * Prepares the rule for variability-based merging and rule application:
	 * rejected elements and removed and the "injective" flag is set. Assumes that
	 * the reject() method has been invoked method before.
	 *
	 * @param rule
	 * @param ruleInfo
	 * @param rejected
	 * @param executed
	 * @return
	 */
	public BitSet prepare(VBRuleInfo ruleInfo, Set<Sentence> rejected, boolean injectiveMatching, boolean baseRule,
			boolean includeApplicationConditions) {
		this.baseRule = baseRule;
		this.injectiveMatching = injectiveMatching;
		this.injectiveMatchingOriginal = ruleInfo.rule.isInjectiveMatching();
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

		fillMaps(ruleInfo, rejected, includeApplicationConditions);

		BitSet bs = getRepresentation(this.rule, this.removeAttributes, this.removeNodes, this.removeEdges,
				this.removeFormulas, injectiveMatching);

		doPreparation();
		return bs;
	}

	private void fillMaps(VBRuleInfo ruleInfo, Set<Sentence> rejected, boolean includeApplicationConditions) {
		for (Sentence expr : rejected) {
			Set<GraphElement> elements = ruleInfo.getPc2Elem().get(expr);
			if (elements == null) {
				continue;
			}

			for (GraphElement ge : elements) {
				if (ge instanceof Node) {
					this.removeNodes.add((Node) ge);
					Set<Mapping> mappings = ruleInfo.getNode2Mapping().get(ge);
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

		if (this.baseRule && this.rule.getLhs().getFormula() != null) {
			this.removeFormulas.add(this.rule.getLhs().getFormula());
			this.removeElementContainers.put(this.rule.getLhs().getFormula(), this.rule.getLhs());
			this.removeFormulaContainingRef.put(this.rule.getLhs().getFormula(),
					this.rule.getLhs().getFormula().eContainingFeature());

		} else {
			for (NestedCondition ac : this.rule.getLhs().getNestedConditions()) {
				Sentence acPC = ruleInfo.getExpressions().get(VariabilityHelper.INSTANCE.getPresenceCondition(ac));
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

		for (Mapping m : this.removeMappings) {
			EStructuralFeature feature = m.eContainingFeature();
			this.removeMappingContainingRef.put(m, feature);
			EObject eContainer = m.eContainer();
			this.removeElementContainers.put(m, eContainer);
			((EList<EObject>) eContainer.eGet(feature)).remove(m);
		}
		for (Attribute a : this.removeAttributes) {
			this.removeElementContainers.put(a, a.getNode());
			a.getNode().getAttributes().remove(a);
		}
		for (Edge e : this.removeEdges) {
			this.removeElementContainers.put(e, e.getGraph());
			this.removeEdgeSources.put(e, e.getSource());
			this.removeEdgeTargets.put(e, e.getTarget());
			e.getGraph().getEdges().remove(e);
			e.getSource().getOutgoing().remove(e);
			e.getTarget().getIncoming().remove(e);
		}
		for (Node n : this.removeNodes) {
			this.removeElementContainers.put(n, n.getGraph());
			n.getGraph().getNodes().remove(n);
		}
		this.rule.setInjectiveMatching(this.injectiveMatching);
		this.rule.setCheckDangling(false); // Dangling edges are allowed in a partial
		// match.

	}

	/**
	 * Removes the formulas in the previously determined order.
	 */
	private void removeFormulas() {
		for (Formula formula : this.removeFormulas) {
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
		this.rule.setCheckDangling(this.checkDangling);
		this.rule.setInjectiveMatching(this.injectiveMatchingOriginal);

		for (Node n : this.removeNodes) {
			((Graph) this.removeElementContainers.get(n)).getNodes().add(n);
		}
		for (Edge e : this.removeEdges) {
			((Graph) this.removeElementContainers.get(e)).getEdges().add(e);
			this.removeEdgeSources.get(e).getOutgoing().add(e);
			this.removeEdgeTargets.get(e).getIncoming().add(e);
		}

		for (Attribute a : this.removeAttributes) {
			((Node) this.removeElementContainers.get(a)).getAttributes().add(a);
		}

		for (Mapping m : this.removeMappings) {
			EStructuralFeature feature = this.removeMappingContainingRef.get(m);
			@SuppressWarnings("unchecked")
			EList<Mapping> list = (EList<Mapping>) this.removeElementContainers.get(m).eGet(feature);
			list.add(m);
		}

		restoreFormulas();

	}

	/**
	 * Restores the formulas in the previously determined order.
	 */
	private void restoreFormulas() {
		for (Formula formula : this.removeFormulas) {
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
	private void determineRemoveOrder(Set<Formula> formulas) {
		Formula outer = this.rule.getLhs().getFormula(); //
		if (outer instanceof Not || outer instanceof NestedCondition) {
			Formula formula = formulas.iterator().next();
			if (formula == outer) {
				this.removeElementContainers.put(formula, this.rule.getLhs());
				this.removeFormulaContainingRef.put(formula, HenshinPackage.Literals.GRAPH__FORMULA);
			}
		} else if (outer instanceof And) {
			determineRemoverOrder((And) outer, formulas, this.rule.getLhs(), HenshinPackage.Literals.GRAPH__FORMULA);
		} else {
			throw new IllegalArgumentException(
					"TODO: Only AND-based nesting of applications conditions supported yet.");
		}
	}

	private void determineRemoverOrder(And and, Set<Formula> formulas, EObject container, EReference feature) {
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

	private void designatePullupChild(Formula formula, And oldContainer, EReference oldFeature, EObject newContainer,
			EReference newFeature) {
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
	private BitSet getRepresentation(Rule rule, Set<Attribute> deleteAttributes, Set<Node> deleteNodes,
			Set<Edge> deleteEdges, Set<Formula> deleteFormula, boolean injectiveMatching) {
		BitSet result = new BitSet(rule.getLhs().getNodes().size() + rule.getLhs().getEdges().size()
				+ rule.getLhs().getNestedConditions().size() + 1);

		result.set(0, injectiveMatching);
		int i = 1;

		for (NestedCondition nc : rule.getLhs().getNestedConditions()) {
			result.set(i, !deleteFormula.contains(nc));
			i++;
		}

		for (Node n : rule.getLhs().getNodes()) {
			result.set(i, !deleteNodes.contains(n));
			i++;
			for (Attribute a : n.getAttributes()) {
				result.set(i, !deleteAttributes.contains(a));
				i++;
			}
		}
		for (Edge e : rule.getLhs().getEdges()) {
			result.set(i, !deleteEdges.contains(e));
			i++;
		}

		for (Node n : rule.getRhs().getNodes()) {
			result.set(i, !deleteNodes.contains(n));
			i++;
			for (Attribute a : n.getAttributes()) {
				result.set(i, !deleteAttributes.contains(a));
				i++;
			}
		}
		for (Edge e : rule.getRhs().getEdges()) {
			result.set(i, !deleteEdges.contains(e));
			i++;
		}

		return result;
	}

	public VBRulePreparator getSnapShot() {
		VBRulePreparator result = new VBRulePreparator(this.rule, getTrueFeatures(), getFalseFeatures());
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