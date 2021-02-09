package org.eclipse.emf.henshin.variability.multi;

import java.nio.file.Paths;
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
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.UMLPackage;
import org.prop4j.Node;
import org.prop4j.NodeWriter;

import carisma.profile.umlsec.variability.Conditional_Element;
import carisma.profile.umlsec.variability.ElementHelper.Logic;
import carisma.profile.umlsec.variability.VariabilityFactory;
import carisma.profile.umlsec.variability.VariabilityPackage;
import de.ovgu.featureide.fm.core.analysis.cnf.CNFCreator;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

public class SecPLUtil implements MultiVarProcessor<VariabilityPackage, IFeatureModel> {

	@Override
	public MultiVarEGraph createEGraphAndCollectPCs(final List<EObject> roots,
			final IFeatureModel fm) {
		final Map<EObject, String> pcs = new HashMap<>();
		final MultiVarEGraph graphP = new MultiVarEGraph();
		for (final EObject e : roots) {
			if (e instanceof Conditional_Element) {
				final Conditional_Element cond = (Conditional_Element) e;
				final String pc = cond.getPresence_condition().get(0);
				final EObject elemet = cond.getBase_Element();
				pcs.put(elemet, pc);
			} else {
				graphP.addTree(e);
			}
		}
		graphP.addTree(UMLPackage.eINSTANCE);
		graphP.setAndConvertPCS(pcs);
		graphP.setFM(getFMExpressionAsCNF(fm));
		return graphP;
	}

	public void createNewVariabilityAnnotations(final Map<EObject, String> pcsP) {
		final Set<EObject> markAsDelete = new HashSet<>(); // Discard presence conditions of diffs
		for (final Entry<EObject, String> pair : pcsP.entrySet()) {
			final EObject element = pair.getKey();
			final String pc = pair.getValue();

			// TODO: What is the symmetric package? -> Tries to add PC to SiLift element ->
			// Check if is instance of Element, do we really want to delete everything else?
			// if (element.eClass().getEPackage() != SymmetricPackage.eINSTANCE) {
			if (element instanceof Element) {
				if (!pc.trim().equals(Logic.TRUE.trim())) {
					final Conditional_Element e = VariabilityFactory.eINSTANCE.createConditional_Element();
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

	public void deleteObsoleteVariabilityAnnotations(final List<EObject> roots, final Map<EObject, String> pcsP) {
		final LinkedList<EObject> delete = new LinkedList<>();
		for (final EObject e : roots) {
			if (e instanceof Conditional_Element) {
				final Conditional_Element cond = (Conditional_Element) e;
				final Element baseElement = cond.getBase_Element();
				if (baseElement == null) {
					delete.add(cond);
				} else {
					// Set pc of element to value from map
					final List<String> pc = cond.getPresence_condition();
					pc.clear();
					pc.add(pcsP.remove(baseElement));
				}
			}
		}
		deleteAll(delete);
	}

	private static void deleteAll(final Collection<EObject> eObjects) {
		final Set<Resource> resources = eObjects.stream().map(EObject::eResource).collect(Collectors.toSet());
		for (final Resource res : resources) {
			final Map<EObject, Collection<Setting>> usages = UsageCrossReferencer.findAll(eObjects, res);
			for (final EObject eObject : eObjects) {
				if ((eObject != null) && !usages.containsKey(eObject)) {
					EcoreUtil.delete(eObject);
				}
			}
			for (final Entry<EObject, Collection<Setting>> entry : usages.entrySet()) {
				final EObject eObject = entry.getKey();
				for (final EStructuralFeature.Setting setting : entry.getValue()) {
					if (setting.getEStructuralFeature().isChangeable()) {
						EcoreUtil.remove(setting, eObject);
					}
				}
				EcoreUtil.remove(eObject);
			}

		}
	}

	@Override
	public MultiVarEGraph createEGraphAndCollectPCs(final HenshinResourceSet set, final String modelLocation,
			final String featureModelLocation) {
		final Resource resource = set.getResource(modelLocation);
		final IFeatureModel fm = FeatureModelManager.load(Paths.get(featureModelLocation));
		return createEGraphAndCollectPCs(resource.getContents(), fm);
	}

	@Override
	public void writePCsToModel(final MultiVarEGraph graph) {
		final Map<EObject, String> mutablePCs = new HashMap<>(graph.getPCsAsStrings());
		deleteObsoleteVariabilityAnnotations(graph.getRoots(), mutablePCs);
		createNewVariabilityAnnotations(mutablePCs);
	}


	public static String getFMExpressionAsCNF(final IFeatureModel featureModel) {
		final Node nodes = Nodes.convert(CNFCreator.createNodes(featureModel));
		return nodes.toString(NodeWriter.shortSymbols);
	}

	@Override
	public VariabilityPackage getEPackage() {
		return VariabilityPackage.eINSTANCE;
	}
}
