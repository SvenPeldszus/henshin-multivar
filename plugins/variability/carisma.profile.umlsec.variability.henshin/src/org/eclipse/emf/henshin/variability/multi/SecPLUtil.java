package org.eclipse.emf.henshin.variability.multi;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.UsageCrossReferencer;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.UMLPackage;

import carisma.profile.umlsec.variability.Conditional_Element;
import carisma.profile.umlsec.variability.ElementHelper.Logic;
import carisma.profile.umlsec.variability.VariabilityFactory;

public class SecPLUtil implements MultiVarProcessor {

	@Override
	public MultiVarEGraph createEGraphAndCollectPCs(List<EObject> roots, Map<EObject, String> pcs, String fm) {
		MultiVarEGraph graphP = new MultiVarEGraph();
		for (EObject e : roots) {
			if (e instanceof Conditional_Element) {
				Conditional_Element cond = (Conditional_Element) e;
				String pc = cond.getPresence_condition().get(0);
				EObject elemet = cond.getBase_Element();
				pcs.put(elemet, pc);
			} else {
				graphP.addTree(e);
			}
		}
		graphP.addTree(UMLPackage.eINSTANCE);
		graphP.setPCS(pcs);
		graphP.setFM(fm);
		return graphP;
	}

	public void createNewVariabilityAnnotations(Map<EObject, String> pcsP) {
		Set<EObject> markAsDelete = new HashSet<>(); // Discard presence conditions of diffs
		for (Entry<EObject, String> pair : pcsP.entrySet()) {
			EObject element = pair.getKey();
			String pc = pair.getValue();

			// TODO: What is the symmetric package? -> Tries to add PC to SiLift element ->
			// Check if is instance of Element, do we really want to delete everything else?
			// if (element.eClass().getEPackage() != SymmetricPackage.eINSTANCE) {
			if (element instanceof Element) {
				if (!pc.trim().equals(Logic.TRUE.trim())) {
					Conditional_Element e = VariabilityFactory.eINSTANCE.createConditional_Element();
					e.setBase_Element((Element) element);
					e.getPresence_condition().add(pc);
					element.eResource().getContents().add(e);
				}
			} else {
				markAsDelete.add(element);
			}
		}

		markAsDelete.forEach(pcsP::remove);

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
					// Set pc of element to value from map
					List<String> pc = cond.getPresence_condition();
					pc.clear();
					pc.add(pcsP.remove(baseElement));
				}
			}
		}
		deleteAll(delete);
	}

	private static void deleteAll(Collection<EObject> eObjects) {
		Set<Resource> resources = eObjects.stream().map(EObject::eResource).collect(Collectors.toSet());
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

	@Override
	public void writePCsToModel(MultiVarEGraph graph) {
		Map<EObject, String> mutablePCs = new HashMap<>(graph.getPCS());
		deleteObsoleteVariabilityAnnotations(graph.getRoots(), mutablePCs);
		createNewVariabilityAnnotations(mutablePCs);
	}
}
