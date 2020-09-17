/**
 * <copyright>
 * Copyright (c) 2010-2014 Henshin developers. All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.interpreter.matching.conditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;

import org.eclipse.emf.henshin.model.util.ScriptEngineWrapper;

/**
 * Condition handler.
 *
 * @author Enrico Biermann, Christian Krause
 */
public class ConditionHandler {

	// Attribute conditions:
	final Collection<AttributeCondition> attributeConditions;

	// Involved conditions:
	final Map<String, Collection<AttributeCondition>> involvedConditions;

	// Assigned parameters:
	final Collection<String> assignedParameters;

	// Used script engine:
	final ScriptEngineWrapper scriptEngine;

	/**
	 * Default constructor.
	 * @param conditionParameters Condition parameters.
	 * @param scriptEngine Script engine,
	 * @param localJavaImports
	 * @param javaImports
	 */
	public ConditionHandler(
			Map<String, Collection<String>> conditionParameters,
			ScriptEngineWrapper scriptEngine, List<String> localJavaImports) {

		this.attributeConditions = new ArrayList<>();
		this.involvedConditions = new HashMap<>();
		this.assignedParameters = new HashSet<>();
		this.scriptEngine = scriptEngine;

		for (String condition : conditionParameters.keySet()) {
			Collection<String> usedParameters = conditionParameters.get(condition);
			AttributeCondition attCondition = new AttributeCondition(condition, usedParameters, scriptEngine, localJavaImports);
			this.attributeConditions.add(attCondition);

			// Create a map for easy lookup of conditions a parameter is involved in:
			for (String usedParameter : usedParameters) {
				Collection<AttributeCondition> conditionList = this.involvedConditions.get(usedParameter);
				if (conditionList == null) {
					conditionList = new ArrayList<>();
					this.involvedConditions.put(usedParameter, conditionList);
				}
				conditionList.add(attCondition);
			}
		}
	}

	public ConditionHandler(Map<String, Collection<String>> conditionParameters, ScriptEngine engine) {
		this(conditionParameters, new ScriptEngineWrapper(engine, new String[0]), Collections.emptyList());
	}

	/**
	 * Set the value for a parameter.
	 * @param paramName Parameter name.
	 * @param value Value.
	 * @return <code>true</code> if it was set.
	 */
	public boolean setParameter(String paramName, Object value) {
		boolean result = true;
		if (this.assignedParameters.add(paramName)) {
			this.scriptEngine.put(paramName, value);
			Collection<AttributeCondition> conditionList = this.involvedConditions.get(paramName);
			if (conditionList != null) {
				for (AttributeCondition condition : conditionList) {
					condition.parameters.remove(paramName);
					result = result && condition.eval();
				}
			}
		}
		return result;
	}

	/**
	 * Unset a parameter value.
	 * @param paramName Parameter name.
	 */
	public void unsetParameter(String paramName) {
		if (this.assignedParameters.remove(paramName)) {
			Collection<AttributeCondition> conditionList = this.involvedConditions.get(paramName);
			if (conditionList != null) {
				for (AttributeCondition condition : this.involvedConditions.get(paramName)) {
					condition.parameters.add(paramName);
				}
			}
		}
	}

	/**
	 * Check whether a parameter is set.
	 * @param paramName Parameter name.
	 * @return <code>true</code> if it is set.
	 */
	public boolean isSet(String paramName) {
		return this.assignedParameters.contains(paramName);
	}

	/**
	 * Get the value for a parameter.
	 * @param paramName Name of the parameter.
	 * @return The value.
	 */
	public Object getParameter(String paramName) {
		return this.scriptEngine.get(paramName);
	}

	/**
	 * Get all parameter values.
	 * @return Map with all parameter values.
	 */
	public Map<String, Object> getParameterValues() {
		Map<String, Object> paramValues = new HashMap<>();
		for (String paramName : this.assignedParameters) {
			paramValues.put(paramName, this.scriptEngine.get(paramName));
		}
		return paramValues;
	}
}
