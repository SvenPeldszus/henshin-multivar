/*******************************************************************************
 * Copyright (c) 2010 CWI Amsterdam, Technical University of Berlin, 
 * University of Marburg and others. All rights reserved. 
 * This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Technical University of Berlin - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.henshin.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.NamedElement;
import org.eclipse.emf.henshin.model.Port;
import org.eclipse.emf.henshin.model.PortMapping;
import org.eclipse.emf.henshin.model.SequentialUnit;
import org.eclipse.emf.henshin.model.TransformationUnit;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Sequential Unit</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.emf.henshin.model.impl.SequentialUnitImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.emf.henshin.model.impl.SequentialUnitImpl#isActivated <em>Activated</em>}</li>
 *   <li>{@link org.eclipse.emf.henshin.model.impl.SequentialUnitImpl#getPorts <em>Ports</em>}</li>
 *   <li>{@link org.eclipse.emf.henshin.model.impl.SequentialUnitImpl#getPortMappings <em>Port Mappings</em>}</li>
 *   <li>{@link org.eclipse.emf.henshin.model.impl.SequentialUnitImpl#getSubUnits <em>Sub Units</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class SequentialUnitImpl extends DescribedElementImpl implements SequentialUnit {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;
	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;
	/**
	 * The default value of the '{@link #isActivated() <em>Activated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isActivated()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ACTIVATED_EDEFAULT = false;
	/**
	 * The cached value of the '{@link #isActivated() <em>Activated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isActivated()
	 * @generated
	 * @ordered
	 */
	protected boolean activated = ACTIVATED_EDEFAULT;
	/**
	 * The cached value of the '{@link #getPorts() <em>Ports</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPorts()
	 * @generated
	 * @ordered
	 */
	protected EList<Port> ports;
	/**
	 * The cached value of the '{@link #getPortMappings() <em>Port Mappings</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPortMappings()
	 * @generated
	 * @ordered
	 */
	protected EList<PortMapping> portMappings;
	/**
	 * The cached value of the '{@link #getSubUnits() <em>Sub Units</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSubUnits()
	 * @generated
	 * @ordered
	 */
	protected EList<TransformationUnit> subUnits;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SequentialUnitImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return HenshinPackage.Literals.SEQUENTIAL_UNIT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, HenshinPackage.SEQUENTIAL_UNIT__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isActivated() {
		return activated;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setActivated(boolean newActivated) {
		boolean oldActivated = activated;
		activated = newActivated;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, HenshinPackage.SEQUENTIAL_UNIT__ACTIVATED, oldActivated, activated));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Port> getPorts() {
		if (ports == null) {
			ports = new EObjectContainmentWithInverseEList<Port>(Port.class, this, HenshinPackage.SEQUENTIAL_UNIT__PORTS, HenshinPackage.PORT__UNIT);
		}
		return ports;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<PortMapping> getPortMappings() {
		if (portMappings == null) {
			portMappings = new EObjectContainmentEList<PortMapping>(PortMapping.class, this, HenshinPackage.SEQUENTIAL_UNIT__PORT_MAPPINGS);
		}
		return portMappings;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<TransformationUnit> getSubUnits() {
		if (subUnits == null) {
			subUnits = new EObjectResolvingEList<TransformationUnit>(TransformationUnit.class, this, HenshinPackage.SEQUENTIAL_UNIT__SUB_UNITS);
		}
		return subUnits;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public EList<TransformationUnit> getAllSubUnits() {
		List<TransformationUnit> allunits = new ArrayList<TransformationUnit>();

		for (TransformationUnit unit : this.subUnits) {
			allunits.addAll(unit.getAllSubUnits());
		}// for
		return new BasicEList<TransformationUnit>(allunits);
	}// getAllSubUnits

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case HenshinPackage.SEQUENTIAL_UNIT__PORTS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getPorts()).basicAdd(otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case HenshinPackage.SEQUENTIAL_UNIT__PORTS:
				return ((InternalEList<?>)getPorts()).basicRemove(otherEnd, msgs);
			case HenshinPackage.SEQUENTIAL_UNIT__PORT_MAPPINGS:
				return ((InternalEList<?>)getPortMappings()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case HenshinPackage.SEQUENTIAL_UNIT__NAME:
				return getName();
			case HenshinPackage.SEQUENTIAL_UNIT__ACTIVATED:
				return isActivated();
			case HenshinPackage.SEQUENTIAL_UNIT__PORTS:
				return getPorts();
			case HenshinPackage.SEQUENTIAL_UNIT__PORT_MAPPINGS:
				return getPortMappings();
			case HenshinPackage.SEQUENTIAL_UNIT__SUB_UNITS:
				return getSubUnits();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case HenshinPackage.SEQUENTIAL_UNIT__NAME:
				setName((String)newValue);
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__ACTIVATED:
				setActivated((Boolean)newValue);
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__PORTS:
				getPorts().clear();
				getPorts().addAll((Collection<? extends Port>)newValue);
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__PORT_MAPPINGS:
				getPortMappings().clear();
				getPortMappings().addAll((Collection<? extends PortMapping>)newValue);
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__SUB_UNITS:
				getSubUnits().clear();
				getSubUnits().addAll((Collection<? extends TransformationUnit>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case HenshinPackage.SEQUENTIAL_UNIT__NAME:
				setName(NAME_EDEFAULT);
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__ACTIVATED:
				setActivated(ACTIVATED_EDEFAULT);
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__PORTS:
				getPorts().clear();
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__PORT_MAPPINGS:
				getPortMappings().clear();
				return;
			case HenshinPackage.SEQUENTIAL_UNIT__SUB_UNITS:
				getSubUnits().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case HenshinPackage.SEQUENTIAL_UNIT__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case HenshinPackage.SEQUENTIAL_UNIT__ACTIVATED:
				return activated != ACTIVATED_EDEFAULT;
			case HenshinPackage.SEQUENTIAL_UNIT__PORTS:
				return ports != null && !ports.isEmpty();
			case HenshinPackage.SEQUENTIAL_UNIT__PORT_MAPPINGS:
				return portMappings != null && !portMappings.isEmpty();
			case HenshinPackage.SEQUENTIAL_UNIT__SUB_UNITS:
				return subUnits != null && !subUnits.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
		if (baseClass == NamedElement.class) {
			switch (derivedFeatureID) {
				case HenshinPackage.SEQUENTIAL_UNIT__NAME: return HenshinPackage.NAMED_ELEMENT__NAME;
				default: return -1;
			}
		}
		return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int eDerivedStructuralFeatureID(int baseFeatureID, Class<?> baseClass) {
		if (baseClass == NamedElement.class) {
			switch (baseFeatureID) {
				case HenshinPackage.NAMED_ELEMENT__NAME: return HenshinPackage.SEQUENTIAL_UNIT__NAME;
				default: return -1;
			}
		}
		return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(", activated: ");
		result.append(activated);
		result.append(')');
		return result.toString();
	}

} //SequentialUnitImpl
