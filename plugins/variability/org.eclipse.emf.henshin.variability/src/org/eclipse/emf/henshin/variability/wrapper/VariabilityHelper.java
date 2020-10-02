package org.eclipse.emf.henshin.variability.wrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.util.FeatureExpression;

import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.SymbolCollector;

public class VariabilityHelper {

	public static final VariabilityHelper INSTANCE = new VariabilityHelper();


	Annotation addAnnotation(ModelElement modelElement, VariabilityConstants annotationKey, String value) {
		Annotation anno = HenshinFactory.eINSTANCE.createAnnotation();

		anno.setKey(annotationKey.toString());
		setAnnotationValue(anno, value);

		modelElement.getAnnotations().add(anno);
		return anno;
	}

	/**
	 * @param value
	 * @param annotation
	 */
	void setAnnotationValue(Annotation annotation, String value) {
		annotation.setValue(value);
	}

	public Annotation getAnnotation(ModelElement modelElement, VariabilityConstants annotationKey) {
		EList<Annotation> annotations = modelElement.getAnnotations();
		if (annotations != null) {
			for (Annotation anno : annotations) {
				if (anno.getKey().equals(annotationKey.toString())) {
					return anno;
				}
			}
		}
		return null;
	}

	public String getPresenceCondition(ModelElement element) {
		Annotation annotation = getAnnotation(element, VariabilityConstants.PRESENCE_CONDITION);
		if (annotation != null) {
			return annotation.getValue();
		}
		return "";
	}

	public void setPresenceCondition(ModelElement element, String condition) {
		setAnnotation(element, VariabilityConstants.PRESENCE_CONDITION, condition);
	}

	/**
	 * @param element
	 * @param presenceCondition
	 * @param value
	 */
	void setAnnotation(ModelElement element, VariabilityConstants presenceCondition, String value) {
		Annotation annotation = getAnnotation(element, presenceCondition);
		if (annotation == null) {
			addAnnotation(element, presenceCondition, value);
		} else {
			setAnnotationValue(annotation, value);
		}
	}

	public String getPresenceConditionIfModelElement(GraphElement elem) {
		if (elem instanceof ModelElement) {
			return getPresenceCondition((ModelElement) elem);
		}
		throw new IllegalStateException();
	}

	public void setFeatureConstraint(Rule rule, String featureModel) {
		setAnnotation(rule, VariabilityConstants.FEATURE_CONSTRAINT, featureModel);
	}

	public String getFeatureConstraint(Rule rule) {
		Annotation annotation = getAnnotation(rule, VariabilityConstants.FEATURE_CONSTRAINT);
		if (annotation != null) {
			return annotation.getValue();
		}
		return "";
	}

	public void setFeatures(Rule rule, Set<String> features) {
		setAnnotation(rule, VariabilityConstants.FEATURES, String.join(", ", features));
	}

	public void addFeature(Rule rule, String name) {
		Annotation annotation = getAnnotation(rule, VariabilityConstants.FEATURES);
		if (annotation == null) {
			addAnnotation(rule, VariabilityConstants.FEATURES, name);
		} else {
			setAnnotationValue(annotation, name);
		}
	}

	public Set<String> getFeatures(Rule rule) {
		Annotation annotation = getAnnotation(rule, VariabilityConstants.FEATURES);
		if (annotation != null) {
			return getFeatures(annotation.getValue());
		}
		return Collections.emptySet();
	}

	private static final Pattern featureSeparatorPattern = Pattern.compile("\\s*,\\s*");

	/**
	 * Converts a comma separated string of features into a set
	 *
	 * @param featureString The feature names
	 * @return A set of feature names
	 */
	private Set<String> getFeatures(String featureString) {
		return Stream.of(featureSeparatorPattern.split(featureString)).collect(Collectors.toSet());
	}

	/**
	 * Returns the injective matching presence condition of this Rule.
	 *
	 * @return the injective matching presence condition of this Rule.
	 */
	public String getInjectiveMatchingPresenceCondition(Rule rule) {
		Annotation annotation = getAnnotation(rule, VariabilityConstants.INJECTIVE_MATCHING_PC);
		if (annotation != null) {
			return annotation.getValue();
		}
		return "";
	}

	public void setInjectiveMatchingPresenceCondition(Rule rule, String pc) {
		setAnnotation(rule, VariabilityConstants.INJECTIVE_MATCHING_PC, pc);

	}

	public static boolean isVariabilityRule(Rule rule) {
		return rule.getAnnotations().parallelStream().filter(annotation -> {
			String key = annotation.getKey();
			return (VariabilityConstants.FEATURES.toString().equals(key)
					|| VariabilityConstants.FEATURE_CONSTRAINT.toString().equals(key)
					|| VariabilityConstants.INJECTIVE_MATCHING_PC.toString().equals(key));
		}).anyMatch(annotation -> {
			String value = annotation.getValue();
			return value != null && !value.isEmpty();
		});
	}

	public boolean hasMissingFeatures(Rule rule) {
		return !calculateMissingFeatureNames(rule).isEmpty();
	}

	public String[] getMissingFeatures(Rule rule) {
		return calculateMissingFeatureNames(rule).toArray(new String[0]);
	}

	private Set<String> calculateMissingFeatureNames(Rule rule) {
		String currentModel = getFeatureConstraint(rule);
		Sentence sentence = FeatureExpression.getExpr(currentModel);
		Set<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(sentence);
		Set<String> missingFeatures = new HashSet<>();
		for (PropositionSymbol symbol : symbols) {
			String symbolName = symbol.getSymbol().trim();
			if (!getFeatures(rule).contains(symbolName)) {
				missingFeatures.add(symbolName);
			}
		}
		return missingFeatures;
	}

	private static boolean isAnnotation(Annotation annotation, VariabilityConstants id) {
		return id.toString().equals(annotation.getKey());
	}

	public static boolean isFeaturesAnnotation(Annotation annotation) {
		return isAnnotation(annotation, VariabilityConstants.FEATURES);
	}

	public static boolean isFeatureModelAnnotation(Annotation annotation) {
		return isAnnotation(annotation, VariabilityConstants.FEATURE_CONSTRAINT);
	}

	public static boolean isPresenceConditionAnnotation(Annotation annotation) {
		return isAnnotation(annotation, VariabilityConstants.PRESENCE_CONDITION);
	}

	public boolean isFeatureConstraintCNF(Rule rule) {
		Annotation constraintAnnotation = getAnnotation(rule, VariabilityConstants.FEATURE_CONSTRAINT_CNF);
		if (constraintAnnotation == null) {
			return false;
		}
		String value = constraintAnnotation.getValue();
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}

	public void setFeatureConstraintIsCNF(Rule rule, boolean value) {
		setAnnotation(rule, VariabilityConstants.FEATURE_CONSTRAINT_CNF, Boolean.toString(value));
	}
}
