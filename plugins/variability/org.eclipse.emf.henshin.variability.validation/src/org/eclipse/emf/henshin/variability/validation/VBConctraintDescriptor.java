/**
 * 
 */
package org.eclipse.emf.henshin.variability.validation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.validation.model.ConstraintSeverity;
import org.eclipse.emf.validation.model.EvaluationMode;
import org.eclipse.emf.validation.service.AbstractConstraintDescriptor;

/**
 * Describes a VB constraint based on its registration at the extension point
 * "org.eclipse.emf.validation.constraintProviders" in the category
 * "org.eclipse.emf.henshin.validation/vb"
 * 
 * @author speldszus
 *
 */
public class VBConctraintDescriptor extends AbstractConstraintDescriptor {

	private final String name;
	private final String id;
	private final String description;
	private final int statusCode;
	private final EvaluationMode<Object> mode;
	private final ConstraintSeverity severity;
	private final String messagePattern;
	private final Set<EClassifier> targets;

	/**
	 * Creates a new descriptor based on the data gathered from the Eclipse
	 * extension registry
	 * 
	 * @param constraint The validator for which a description should be generated
	 */
	public VBConctraintDescriptor(AbstractVBValidator constraint) {
		IConfigurationElement constraintElement = getConfigurationElementFromRegistry(constraint);
		this.id = constraintElement.getAttribute("id");
		this.mode = EvaluationMode.getInstance(constraintElement.getAttribute("mode"));
		this.name = constraintElement.getAttribute("name");
		this.severity = ConstraintSeverity.getInstance(constraintElement.getAttribute("severity"));
		int statusCode;
		try {
			statusCode = Integer.parseInt(constraintElement.getAttribute("statusCode"));
		} catch (NumberFormatException e) {
			statusCode = -1;
		}
		this.statusCode = statusCode;
		IConfigurationElement[] messages = constraintElement.getChildren("message");
		if (messages.length > 0) {
			this.messagePattern = messages[0].getValue();
		} else {
			this.messagePattern = "";
		}
		IConfigurationElement[] descriptions = constraintElement.getChildren("description");
		if (descriptions.length > 0) {
			this.description = descriptions[0].getValue();
		} else {
			this.description = "";
		}
		this.targets = new HashSet<>();
		for (IConfigurationElement targetElement : constraintElement.getChildren("target")) {
			EClassifier eClassifier = getEClassifier(targetElement.getAttribute("class"));
			targets.add(eClassifier);
		}
	}

	/**
	 * Searches the EClassifier from the Henshin meta model corresponding with the
	 * class name
	 * 
	 * @param name The name of the class
	 * @return The Henshin EClassifier
	 */
	protected EClassifier getEClassifier(String name) {
		String simpleName;
		int index = name.indexOf('.');
		if (index >= 0) {
			simpleName = name.substring(index + 1);
		} else {
			simpleName = name;
		}
		EClassifier eClassifier = HenshinPackage.eINSTANCE.getEClassifier(simpleName);
		return eClassifier;
	}

	/**
	 * Searches the configuration element corresponding with the given validator
	 * 
	 * @param constraint A VB validator
	 * @return The configuration element describing the validator
	 */
	private IConfigurationElement getConfigurationElementFromRegistry(AbstractVBValidator constraint) {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		IExtension extension = extensionRegistry
				.getExtension("org.eclipse.emf.henshin.variability.validation.constraints");
		Stream<IConfigurationElement> constraints = Stream.empty();
		for (IConfigurationElement element : extension.getConfigurationElements()) {
			if ("constraintProvider".equals(element.getName())) {
				constraints = Stream.concat(constraints,
						Stream.of(element.getChildren("constraints")).parallel()
								.filter(constraintsList -> "org.eclipse.emf.henshin.validation/vb"
										.equals(constraintsList.getAttribute("categories"))));
			}
		}
		Optional<IConfigurationElement> relevantElement = constraints
				.flatMap(constraintList -> Stream.of(constraintList.getChildren("constraint")))
				.filter(constraintElement -> {
					String className = constraintElement.getAttribute("class");
					return constraint.getClass().getName().equals(className);
				}).findAny();
		if (!relevantElement.isPresent()) {
			throw new IllegalStateException("Cannot read extension");
		}
		return relevantElement.get();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getPluginId() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public ConstraintSeverity getSeverity() {
		return this.severity;
	}

	@Override
	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public EvaluationMode<?> getEvaluationMode() {
		return this.mode;
	}

	@Override
	public boolean targetsTypeOf(EObject eObject) {
		return targets.contains(eObject.eClass());
	}

	@Override
	public boolean targetsEvent(Notification notification) {
		Object feature = notification.getFeature();
		if (feature instanceof EObject) {
			return targetsTypeOf((EObject) feature);
		}
		return false;
	}

	@Override
	public String getMessagePattern() {
		return this.messagePattern;
	}

	@Override
	public String getBody() {
		// We don't have a body
		return null;
	}

}
