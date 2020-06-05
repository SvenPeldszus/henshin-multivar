package org.eclipse.emf.henshin.variability.wrapper;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.henshin.model.Annotation;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.ModelElement;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;

/**
 * This class offers an interface to allow UI elements to modify the underlying
 * models.
 * 
 * @author Stefan Schulz
 *
 */
public class VariabilityTransactionHelper extends VariabilityHelper {
	
	public static final VariabilityTransactionHelper INSTANCE = new VariabilityTransactionHelper();

	private VariabilityTransactionHelper() {
		// This class should not be instantiated
	}

	private static TransactionalEditingDomain getOrCreateEditingDomain(EObject modelElement) {
		TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(modelElement);
		if (domain == null) {
			ResourceSet resSet = new ResourceSetImpl();
			resSet.createResource(modelElement.eResource().getURI());
			domain = TransactionalEditingDomain.Factory.INSTANCE.createEditingDomain(resSet);
		}
		return domain;
	}

	@Override
	Annotation addAnnotation(ModelElement modelElement, VariabilityConstants key, String value) {
		Annotation anno = HenshinFactory.eINSTANCE.createAnnotation();
		anno.setKey(key.toString());
		anno.setValue(value);

		try {
			EditingDomain domain = getOrCreateEditingDomain(modelElement);
			Command command = AddCommand.create(domain, modelElement, HenshinPackage.Literals.MODEL_ELEMENT__ANNOTATIONS, anno);
			CommandStack stack = domain.getCommandStack();
			stack.execute(command);
		} catch (IllegalStateException e) {
		}
		return anno;
	}

	@Override
	void setAnnotationValue(Annotation anno, String value) {
		EditingDomain domain = getOrCreateEditingDomain(anno.eContainer());
		Command command = SetCommand.create(domain, anno, HenshinPackage.Literals.ANNOTATION__VALUE, value);
		CommandStack stack = domain.getCommandStack();
		try {
			stack.execute(command);
		} catch (IllegalStateException e) {
		}
	}

}
