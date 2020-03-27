package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.UsageCrossReferencer;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.UMLPackage;
import carisma.profile.umlsec.variability.Conditional_Element;
import carisma.profile.umlsec.variability.ElementHelper.Logic;
import carisma.profile.umlsec.variability.VariabilityFactory;

public class SecPLUtil extends VBProcessor {

	public EGraphImpl createEGraphAndCollectPCs(List<EObject> roots, Map<EObject, String> pcsP) {
		EGraphImpl graphP = new EGraphImpl();
		for (EObject e : roots) {
			if (e instanceof Conditional_Element) {
				Conditional_Element cond = (Conditional_Element) e;
				String pc = cond.getPresence_condition().get(0);
				EObject elemet = cond.getBase_Element();
				pcsP.put(elemet, pc);
			} else {
				graphP.addTree(e);
			}
		}
		graphP.addTree(UMLPackage.eINSTANCE);
		return graphP;
	}

	public void createNewVariabilityAnnotations(List<EObject> roots, Map<EObject, String> pcsP) {
		Set<EObject> markAsDelete = new HashSet<EObject>(); // Discard presence conditions of diffs
		for (Entry<EObject, String> pair : pcsP.entrySet()) {
			EObject element = pair.getKey();
			String pc = pair.getValue();

			//TODO: What is the symmetric package?
//			if (element.eClass().getEPackage() != SymmetricPackage.eINSTANCE) {
				if (!pc.trim().equals(Logic.TRUE.trim())) {
					Conditional_Element e = VariabilityFactory.eINSTANCE.createConditional_Element();
					e.setBase_Element((Element) element);
					e.getPresence_condition().add(pc);
					element.eResource().getContents().add(e);
				}
//			} else {
//				markAsDelete.add(element);
//			}
		}

		markAsDelete.forEach(o -> pcsP.remove(o));
	}

	public void deleteObsoleteVariabilityAnnotations(List<EObject> roots, Map<EObject, String> pcsP) {
		LinkedList<EObject> delete = new LinkedList<>();
		for (EObject e : roots) {
			if (e instanceof Conditional_Element) {
				Conditional_Element cond = (Conditional_Element) e;
				Element baseElement = cond.getBase_Element();
				if (baseElement == null) {
					delete.add(cond);
				} else {
					EList<String> pc = cond.getPresence_condition();
					pc.clear();
					pc.add(pcsP.get(baseElement));
					pcsP.remove(baseElement);
				}
			}
		}
		deleteAll(delete);
	}

	private static void deleteAll(Collection<EObject> eObjects) {
		Set<Resource> resources = eObjects.stream().map(x -> x.eResource()).collect(Collectors.toSet());
		for (Resource res : resources) {
			Map<EObject, Collection<Setting>> usages = UsageCrossReferencer.findAll(eObjects, res);
			for (EObject eObject : eObjects) {
				if (eObject != null && !usages.containsKey(eObject)) {
					EcoreUtil.delete(eObject);
				}
			}
			for (Entry<EObject, Collection<Setting>> entry : usages.entrySet()) {
				EObject eObject = entry.getKey();
				for (EStructuralFeature.Setting setting : entry.getValue()) {
					if (setting.getEStructuralFeature().isChangeable()) {
						EcoreUtil.remove(setting, eObject);
					}
				}
				EcoreUtil.remove(eObject);
			}

		}
	}
}
