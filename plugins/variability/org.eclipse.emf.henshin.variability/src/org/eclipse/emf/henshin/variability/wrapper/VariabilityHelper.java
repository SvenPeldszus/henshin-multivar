package org.eclipse.emf.henshin.variability.wrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.eclipse.emf.henshin.variability.matcher.FeatureExpression;

import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.SymbolCollector;

public class VariabilityHelper {
	
	public static final VariabilityHelper INSTANCE = new VariabilityHelper();
	
	private HashMap<ModelElement, Annotation> fmCache;
	private HashMap<ModelElement, Annotation> featureCache;
	private HashMap<ModelElement, Annotation> injCache;
	private HashMap<ModelElement, Annotation> pcCache;
	
	public VariabilityHelper() {
		fmCache = new HashMap<>();
		featureCache = new HashMap<>();
		injCache = new HashMap<>();
		pcCache = new HashMap<>();
	}

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

	Annotation getAnnotation(ModelElement modelElement, VariabilityConstants annotationKey) {
		Map<ModelElement, Annotation> map;
		switch(annotationKey) {
			case FEATURE_MODEL: map = fmCache;break;
			case FEATURES: map = featureCache;break;
			case INJECTIVE_MATCHING_PC: map = injCache;break;
			case PRESENCE_CONDITION: map = pcCache;break;
			default: throw new IllegalStateException();
		}
		Annotation annotation = map.get(modelElement);
		if(annotation != null) {
			return annotation;
		}
		EList<Annotation> annotations = modelElement.getAnnotations();
		if (annotations != null) {
			for (Annotation anno : annotations) {
				if (anno.getKey().equals(annotationKey.toString())) {
					map.put(modelElement, anno);
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
		}
		else {
			setAnnotationValue(annotation, value);
		}
	}

	public String getPresenceConditionIfModelElement(GraphElement elem) {
		if (elem instanceof ModelElement) {
			return getPresenceCondition((ModelElement) elem);
		}
		throw new IllegalStateException();
	}

	public void setFeatureModel(Rule rule, String featureModel) {
		setAnnotation(rule, VariabilityConstants.FEATURE_MODEL, featureModel);
	}

	public String getFeatureModel(Rule rule) {
		Annotation annotation = getAnnotation(rule, VariabilityConstants.FEATURE_MODEL);
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
		if(annotation == null) {
			addAnnotation(rule, VariabilityConstants.FEATURES, name);
		}
		else {
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
		return rule.getAnnotations().parallelStream().map(Annotation::getKey)
				.anyMatch(key -> (VariabilityConstants.FEATURES.toString().equals(key)
						|| VariabilityConstants.FEATURE_MODEL.toString().equals(key)
						|| VariabilityConstants.INJECTIVE_MATCHING_PC.toString().equals(key)));
	}

	public boolean hasMissingFeatures(Rule rule) {
		return !calculateMissingFeatureNames(rule).isEmpty();
	}

	public String[] getMissingFeatures(Rule rule) {
		return calculateMissingFeatureNames(rule).toArray(new String[0]);
	}

	private Set<String> calculateMissingFeatureNames(Rule rule) {
		String currentModel = getFeatureModel(rule);
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
		return isAnnotation(annotation, VariabilityConstants.FEATURE_MODEL);
	}

	public static boolean isPresenceConditionAnnotation(Annotation annotation) {
		return isAnnotation(annotation, VariabilityConstants.PRESENCE_CONDITION);
	}
}
