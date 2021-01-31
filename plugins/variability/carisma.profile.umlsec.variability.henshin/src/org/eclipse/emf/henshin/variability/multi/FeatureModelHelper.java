package org.eclipse.emf.henshin.variability.multi;

import org.prop4j.Node;
import org.prop4j.NodeWriter;

import de.ovgu.featureide.fm.core.analysis.cnf.CNFCreator;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.base.IFeatureModel;

public class FeatureModelHelper {

	public static String getFMExpressionAsCNF(IFeatureModel featureModel) {
		final Node nodes = Nodes.convert(CNFCreator.createNodes(featureModel));
		return nodes.toString(NodeWriter.shortSymbols);
	}
}
