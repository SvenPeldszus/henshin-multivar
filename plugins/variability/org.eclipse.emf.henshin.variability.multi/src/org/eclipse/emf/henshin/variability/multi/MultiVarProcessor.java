package org.eclipse.emf.henshin.variability.multi;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;

/**
 * A processor for loading and updating model productlines for the usage in Henshin MultiVar
 *
 * @author speldszus
 *
 * @param <D> The EPackage defining the variability stereotypes the processor can process
 * @param <F> The type of the supported feature model
 */
public interface MultiVarProcessor<D extends EPackage, F> {

	/**
	 * Loads a model and it's feature model from the file system and created a MultiVarEGraph from it.
	 *
	 * @param set The resource set the EGraph should be created in.
	 * @param modelLocation The location of the model to load.
	 * @param featureModelLocation The location of the feature model or null, if the feature model is part of the model.
	 * @return The EGraph containing the model and feature model.
	 */
	MultiVarEGraph createEGraphAndCollectPCs(HenshinResourceSet set, String modelLocation, String featureModelLocation);

	/**
	 * Creates an MultiVarEGraph from the given root elements and feature model
	 *
	 * @param roots The roots of the EGraph.
	 * @param featureModel The feature model or null, iff the feature model is encoded in the roots.
	 * @return The EGraph containing the model and feature model.
	 * @throws IllegalStateException if the roots are not in the same resource set
	 */
	MultiVarEGraph createEGraphAndCollectPCs(List<EObject> roots, F featureModel);

	/**
	 * Persists the updated PCs in th the model
	 *
	 * @param graph The modified EGraph
	 */
	void writePCsToModel(MultiVarEGraph graph);

	/**
	 * A getter for the EPackage containing the variability annotations
	 *
	 * @return The EPackage
	 */
	D getEPackage();

	MultiVarEGraph createEGraphAndCollectPCs(Resource resource, String featureModelPath);
}
