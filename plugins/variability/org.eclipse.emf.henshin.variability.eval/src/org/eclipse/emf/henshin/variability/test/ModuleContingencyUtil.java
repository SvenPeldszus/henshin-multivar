package org.eclipse.emf.henshin.variability.test;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;

public class ModuleContingencyUtil {

	public static void checkModule(Module module) {
		for (EPackage pack : module.getImports()) {
			if (pack == null || pack.getName() == null) {
				throw new IllegalArgumentException("Error in module "
						+ module.getName()
						+ ": A package import could not be resolved!");
			}
		}

		TreeIterator<EObject> it = module.eAllContents();
		while (it.hasNext()) {
			EObject object = it.next();

			if (object instanceof Node) {
				Node node = (Node) object;
				if (node.getType() == null || node.getType().getName() == null) {
					throw new IllegalArgumentException("Error in module "
							+ module.getName()
							+ ": A type could not be resolved!");
				}
			}

			if (object instanceof Edge) {
				Edge edge = (Edge) object;
				if (edge.getType() == null || edge.getType().getName() == null) {
					throw new IllegalArgumentException("Error in module "
							+ module.getName()
							+ ": A type could not be resolved!");
				}
			}
			
			if (object instanceof Attribute) {
				Attribute attr = (Attribute) object;
				if (attr.getType() == null || attr.getType().getName() == null) {
					throw new IllegalArgumentException("Error in module "
							+ module.getName()
							+ ": A type could not be resolved!");
				}
			}

		}

	}
}
