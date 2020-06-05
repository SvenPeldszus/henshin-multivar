/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.FeatureExpression;
import org.eclipse.emf.validation.AbstractModelConstraint;
import org.eclipse.emf.validation.IValidationContext;
import org.eclipse.emf.validation.model.IConstraintStatus;
import org.eclipse.emf.validation.model.IModelConstraint;
import org.eclipse.emf.validation.service.IConstraintDescriptor;

import aima.core.logic.common.ParserException;
import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.SymbolCollector;

/**
 * @author speldszus
 *
 *         A validator for Henshin VB rules
 */
public abstract class AbstractVBValidator extends AbstractModelConstraint implements IModelConstraint {

	@Override
	public IConstraintStatus validate(IValidationContext ctx) {
		EObject target = ctx.getTarget();
		IStatus result = validate(target);
		return new VBConstraintStatus(result, target, this);
	}

	/**
	 * Validates the given the given targetElement
	 * 
	 * @param target An EObject to validate
	 * @return the validation status
	 */
	protected abstract IStatus validate(EObject target);

	/**
	 * Checks if a constraint can be parsed and only contains features from the
	 * features list
	 * 
	 * @param features The list of valid features
	 * @param constraint The constraint
	 * @return If the constraint is valid
	 */
	protected static IStatus checkConstraint(Set<String> features, String constraint) {
		Sentence sentence;
		try {
			sentence = FeatureExpression.getExpr(constraint);
		} catch (ParserException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
		}
		if (features == null) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No features are specified!");
		}
		Set<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(sentence);
		String missing = symbols.parallelStream().map(PropositionSymbol::getSymbol)
				.filter(symbol -> !features.contains(symbol)).collect(Collectors.joining(", "));
		if (missing.length() > 0) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"The following features are used but not contained in the list of all features: " + missing);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Returns all annotations on a rule with the given key
	 * 
	 * @param key  The key of the annotation
	 * @param rule The rule
	 * @return The annotations
	 */
	protected static List<Annotation> getAnnotations(String key, Rule rule) {
		List<Annotation> featureModels = rule.getAnnotations().parallelStream().filter(a -> {
			return key.equals(a.getKey());
		}).collect(Collectors.toList());
		return featureModels;
	}

	@Override
	public IConstraintDescriptor getDescriptor() {
		return new VBConctraintDescriptor(this);
	}

}
