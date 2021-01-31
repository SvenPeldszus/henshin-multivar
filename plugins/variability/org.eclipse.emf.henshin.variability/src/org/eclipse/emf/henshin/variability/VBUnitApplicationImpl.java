package org.eclipse.emf.henshin.variability;

import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.impl.AssignmentImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.ParameterMapping;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
/**
 * Variability-aware {@link org.eclipse.emf.henshin.interpreter.UnitApplication UnitApplication} implementation.
 *
 * @author Daniel Str�ber
 */
public class VBUnitApplicationImpl extends UnitApplicationImpl {

	public VBUnitApplicationImpl(Engine engine) {
		super(engine);
	}

	public VBUnitApplicationImpl(Engine engine, EGraph graph, Unit unit,
			Assignment assignment) {
		super(engine, graph, unit, assignment);
	}

	@Override
	protected boolean executeRule(ApplicationMonitor monitor) {
		Rule rule = (Rule) this.unit;
		RuleApplication ruleApp = new VBRuleApplicationImpl(this.engine, this.graph, rule, this.resultAssignment);
		if (ruleApp.execute(monitor)) {
			this.resultAssignment = new AssignmentImpl(ruleApp.getResultMatch(), true);
			this.appliedRules.push(ruleApp);
			return true;  // notification is done in the rule application
		} else {
			return false;
		}
	}

	/*
	 * Create an UnitApplication for a given Unit.
	 */
	@Override
	protected UnitApplicationImpl createApplicationFor(Unit subUnit) {
		if (this.resultAssignment==null) {
			this.resultAssignment = new AssignmentImpl(this.unit);
		}
		Assignment assign = new AssignmentImpl(subUnit);
		for (ParameterMapping mapping : this.unit.getParameterMappings()) {
			Parameter source = mapping.getSource();
			Parameter target = mapping.getTarget();
			if (target.getUnit()==subUnit) {
				assign.setParameterValue(target, this.resultAssignment.getParameterValue(source));
			}
		}
		return new VBUnitApplicationImpl(this.engine, this.graph, subUnit, assign);
	}
}
