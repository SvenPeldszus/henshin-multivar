/*******************************************************************************
 * Copyright (c) 2010 CWI Amsterdam, Technical University Berlin, 
 * Philipps-University Marburg and others. All rights reserved. 
 * This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Technical University Berlin - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.henshin.model;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Nested Condition</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.henshin.model.NestedCondition#isNegated <em>Negated</em>}</li>
 *   <li>{@link org.eclipse.emf.henshin.model.NestedCondition#getConclusion <em>Conclusion</em>}</li>
 *   <li>{@link org.eclipse.emf.henshin.model.NestedCondition#getMappings <em>Mappings</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.henshin.model.HenshinPackage#getNestedCondition()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='mappingOriginContainedInParentCondition mappingImageContainedInCurrent'"
 * @generated
 */
public interface NestedCondition extends Formula {
	/**
	 * Returns the value of the '<em><b>Negated</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Negated</em>' attribute.
	 * @see #setNegated(boolean)
	 * @see org.eclipse.emf.henshin.model.HenshinPackage#getNestedCondition_Negated()
	 * @model
	 * @generated
	 * @deprecated Use {@link Not} to represent negative conditions.
	 */
	boolean isNegated();

	/**
	 * Sets the value of the '{@link org.eclipse.emf.henshin.model.NestedCondition#isNegated <em>Negated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Negated</em>' attribute.
	 * @see #isNegated()
	 * @generated
	 * @deprecated Use {@link Not} to represent negative conditions.
	 */
	void setNegated(boolean value);

	/**
	 * Returns the value of the '<em><b>Conclusion</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Conclusion</em>' containment reference.
	 * @see #setConclusion(Graph)
	 * @see org.eclipse.emf.henshin.model.HenshinPackage#getNestedCondition_Conclusion()
	 * @model containment="true" required="true"
	 * @generated
	 */
	Graph getConclusion();

	/**
	 * Sets the value of the '{@link org.eclipse.emf.henshin.model.NestedCondition#getConclusion <em>Conclusion</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Conclusion</em>' containment reference.
	 * @see #getConclusion()
	 * @generated
	 */
	void setConclusion(Graph value);

	/**
	 * Returns the value of the '<em><b>Mappings</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.emf.henshin.model.Mapping}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mappings</em>' containment reference list.
	 * @see org.eclipse.emf.henshin.model.HenshinPackage#getNestedCondition_Mappings()
	 * @model containment="true"
	 * @generated
	 */
	EList<Mapping> getMappings();

} // NestedCondition
