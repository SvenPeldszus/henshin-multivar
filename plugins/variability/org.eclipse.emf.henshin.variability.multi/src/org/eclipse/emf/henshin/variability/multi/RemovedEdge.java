package org.eclipse.emf.henshin.variability.multi;

import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Node;

public class RemovedEdge {

	private final Edge edge;
	private final Node source;
	private final Node target;

	public RemovedEdge(final Edge edge, final Node source, final Node target) {
		this.edge = edge;
		this.source = source;
		this.target = target;
	}

	public Edge restore() {
		this.getEdge().setSource(this.getSource());
		this.getEdge().setTarget(this.getTarget());
		return this.getEdge();
	}

	public Edge getEdge() {
		return edge;
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}
}
