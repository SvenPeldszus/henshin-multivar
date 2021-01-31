/**
 * <copyright>
 * Copyright (c) 2010-2014 Henshin developers. All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.model.impl;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.actions.ActionElementFinder;
import org.eclipse.emf.henshin.model.actions.AttributeActionHelper;

/**
 * <!-- begin-user-doc --> An implementation of the model object
 * '<em><b>Attribute</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 * <li>{@link org.eclipse.emf.henshin.model.impl.AttributeImpl#getAction
 * <em>Action</em>}</li>
 * <li>{@link org.eclipse.emf.henshin.model.impl.AttributeImpl#getType
 * <em>Type</em>}</li>
 * <li>{@link org.eclipse.emf.henshin.model.impl.AttributeImpl#getValue
 * <em>Value</em>}</li>
 * <li>{@link org.eclipse.emf.henshin.model.impl.AttributeImpl#getNode
 * <em>Node</em>}</li>
 * <li>{@link org.eclipse.emf.henshin.model.impl.AttributeImpl#getConstant
 * <em>Constant</em>}</li>
 * <li>{@link org.eclipse.emf.henshin.model.impl.AttributeImpl#isNull
 * <em>Null</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AttributeImpl extends ModelElementImpl implements Attribute {

	/**
	 * The default value of the '{@link #getAction() <em>Action</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #getAction()
	 * @generated
	 * @ordered
	 */
	protected static final Action ACTION_EDEFAULT = null;

	/**
	 * An adapter that automatically updates the derived fields {@link #constant}
	 * and {@link #null_}.
	 *
	 * @generated NOT
	 */
	private final Adapter derivedFieldsUpdater = new AdapterImpl() {
		@Override
		public void notifyChanged(final Notification msg) {
			final int featureID = msg.getFeatureID(Attribute.class);
			if ((featureID == HenshinPackage.ATTRIBUTE__TYPE) || (featureID == HenshinPackage.ATTRIBUTE__VALUE)) {
				updateDerivedFields();
			}
		}
	};

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' reference. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected EAttribute type;

	/**
	 * The default value of the '{@link #getValue() <em>Value</em>}' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #getValue()
	 * @generated
	 * @ordered
	 */
	protected static final String VALUE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getValue() <em>Value</em>}' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #getValue()
	 * @generated
	 * @ordered
	 */
	protected String value = VALUE_EDEFAULT;

	/**
	 * The default value of the '{@link #getConstant() <em>Constant</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #getConstant()
	 * @generated NOT
	 * @ordered
	 */
	protected static final Object CONSTANT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getConstant() <em>Constant</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #getConstant()
	 * @generated
	 * @ordered
	 */
	protected Object constant = CONSTANT_EDEFAULT;

	/**
	 * The default value of the '{@link #isNull() <em>Null</em>}' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #isNull()
	 * @generated
	 * @ordered
	 */
	protected static final boolean NULL_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isNull() <em>Null</em>}' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @see #isNull()
	 * @generated
	 * @ordered
	 */
	protected boolean null_ = NULL_EDEFAULT;

	/**
	 * <!-- begin-user-doc --> Default and only constructor. <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	protected AttributeImpl() {
		// Add the constant updater to the adapters list:
		eAdapters().add(this.derivedFieldsUpdater);
	}

	/**
	 * Update the derived {@link #constant} field of this attribute.
	 *
	 * @generated NOT
	 */
	protected void updateDerivedFields() {

		// Reset constant:
		this.constant = null;

		// Supposed to be null?
		if (isNull(this.value)) {
			this.null_ = true;
			return;
		}

		// Otherwise it is not null:
		this.null_ = false;

		// We need the value and the type:
		if ((this.value == null) || (this.type == null)) {
			return;
		}

		// Need to resolve the data type first?
		if (this.type.getEType() == null) {
			EcoreUtil.resolveAll(this);
		}

		// Package and factory must be known:
		if ((this.type.getEType() == null) || (this.type.getEType().getEPackage() == null)
				|| (this.type.getEType().getEPackage().getEFactoryInstance() == null)) {
			return;
		}

		// Get a trimmed version of the value:
		String v = this.value.trim();

		// Multiple values?
		if (this.type.isMany()) {
			if (v.startsWith("[") && v.endsWith("]")) {
				v = v.substring(1, v.length() - 1);
				EList<Object> list = new BasicEList<>();
				for (final String e : v.split(",")) {
					if (isNull(e)) {
						list.add(null);
					} else {
						final Object c = getConstant(e, this.type);
						if (c != null) {
							list.add(c);
						} else {
							list = null;
							break;
						}
					}
				}
				this.constant = list;
			}
		} else {
			this.constant = getConstant(v, this.type);
		}

	}

	/**
	 * Check whether a string value is "null".
	 *
	 * @param value String value.
	 * @return <code>true</code> if it is null.
	 */
	private static boolean isNull(final String value) {
		return (value != null) && (value.trim().equalsIgnoreCase("null"));
	}

	/**
	 * Try to cast a string value into a constant.
	 *
	 * @param value String value.
	 * @param type  Attribute type.
	 * @return The constant or <code>null</code>.
	 */
	private static Object getConstant(final String value, final EAttribute type) {

		// Enum?
		if (type.getEType() instanceof EEnum) {
			try {
				final EEnumLiteral literal = ((EEnum) type.getEType()).getEEnumLiteral(value);
				final Enumerator instance = literal.getInstance();
				if (instance == null) {
					return literal;
				}
				return instance;
			} catch (final Throwable t) {
			}
		}

		// Special treatment for strings:
		if (type.getEType().getInstanceClass() == String.class) {
			String v = value.trim();
			if (v.startsWith("\"") && v.endsWith("\"")) { // double quotes
				v = v.substring(1, v.length() - 1);
				if (v.indexOf("\"") < 0) {
					return v;
				}
			}
			if (v.startsWith("'") && v.endsWith("'")) { // single quotes
				v = v.substring(1, v.length() - 1);
				if (v.indexOf("'") < 0) {
					return v;
				}
			}
			return null;
		}

		// Last chance: try to load it using the factory:
		try {
			final EFactory factory = type.getEType().getEPackage().getEFactoryInstance();
			return factory.createFromString(type.getEAttributeType(), value);
		} catch (final Throwable t) {
		}

		// Seems not to be a constant:
		return null;

	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return HenshinPackage.Literals.ATTRIBUTE;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	@Override
	public Action getAction() {
		return AttributeActionHelper.INSTANCE.getAction(this);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	@Override
	public void setAction(final Action action) {
		AttributeActionHelper.INSTANCE.setAction(this, action);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public EAttribute getType() {
		if ((this.type != null) && this.type.eIsProxy()) {
			final InternalEObject oldType = (InternalEObject) this.type;
			this.type = (EAttribute) eResolveProxy(oldType);
			if ((this.type != oldType) && eNotificationRequired()) {
				eNotify(new ENotificationImpl(this, Notification.RESOLVE, HenshinPackage.ATTRIBUTE__TYPE, oldType,
						this.type));
			}
		}
		return this.type;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	public EAttribute basicGetType() {
		return this.type;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setType(final EAttribute newType) {
		final EAttribute oldType = this.type;
		this.type = newType;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET, HenshinPackage.ATTRIBUTE__TYPE, oldType, this.type));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public String getValue() {
		return this.value;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setValue(final String newValue) {
		final String oldValue = this.value;
		this.value = newValue;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET, HenshinPackage.ATTRIBUTE__VALUE, oldValue, this.value));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public Node getNode() {
		if (eContainerFeatureID() != HenshinPackage.ATTRIBUTE__NODE) {
			return null;
		}
		return (Node) eContainer();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	public Node basicGetNode() {
		if (eContainerFeatureID() != HenshinPackage.ATTRIBUTE__NODE) {
			return null;
		}
		return (Node) eInternalContainer();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	public NotificationChain basicSetNode(final Node newNode, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject) newNode, HenshinPackage.ATTRIBUTE__NODE, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setNode(final Node newNode) {
		if ((newNode != eInternalContainer())
				|| ((eContainerFeatureID() != HenshinPackage.ATTRIBUTE__NODE) && (newNode != null))) {
			if (EcoreUtil.isAncestor(this, newNode)) {
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			}
			NotificationChain msgs = null;
			if (eInternalContainer() != null) {
				msgs = eBasicRemoveFromContainer(msgs);
			}
			if (newNode != null) {
				msgs = ((InternalEObject) newNode).eInverseAdd(this, HenshinPackage.NODE__ATTRIBUTES, Node.class, msgs);
			}
			msgs = basicSetNode(newNode, msgs);
			if (msgs != null) {
				msgs.dispatch();
			}
		} else if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET, HenshinPackage.ATTRIBUTE__NODE, newNode, newNode));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public Object getConstant() {
		return this.constant;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public boolean isNull() {
		return this.null_;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	@Override
	public Attribute getActionAttribute() {
		return ActionElementFinder.getActionElement(this, AttributeActionHelper.INSTANCE);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	@Override
	public Graph getGraph() {
		final Node node = getNode();
		return (node != null) ? node.getGraph() : null;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public NotificationChain eInverseAdd(final InternalEObject otherEnd, final int featureID, NotificationChain msgs) {
		switch (featureID) {
		case HenshinPackage.ATTRIBUTE__NODE:
			if (eInternalContainer() != null) {
				msgs = eBasicRemoveFromContainer(msgs);
			}
			return basicSetNode((Node) otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(final InternalEObject otherEnd, final int featureID, final NotificationChain msgs) {
		switch (featureID) {
		case HenshinPackage.ATTRIBUTE__NODE:
			return basicSetNode(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public NotificationChain eBasicRemoveFromContainerFeature(final NotificationChain msgs) {
		switch (eContainerFeatureID()) {
		case HenshinPackage.ATTRIBUTE__NODE:
			return eInternalContainer().eInverseRemove(this, HenshinPackage.NODE__ATTRIBUTES, Node.class, msgs);
		}
		return super.eBasicRemoveFromContainerFeature(msgs);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public Object eGet(final int featureID, final boolean resolve, final boolean coreType) {
		switch (featureID) {
		case HenshinPackage.ATTRIBUTE__ACTION:
			return getAction();
		case HenshinPackage.ATTRIBUTE__TYPE:
			if (resolve) {
				return getType();
			}
			return basicGetType();
		case HenshinPackage.ATTRIBUTE__VALUE:
			return getValue();
		case HenshinPackage.ATTRIBUTE__NODE:
			if (resolve) {
				return getNode();
			}
			return basicGetNode();
		case HenshinPackage.ATTRIBUTE__CONSTANT:
			return getConstant();
		case HenshinPackage.ATTRIBUTE__NULL:
			return isNull();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void eSet(final int featureID, final Object newValue) {
		switch (featureID) {
		case HenshinPackage.ATTRIBUTE__ACTION:
			setAction((Action) newValue);
			return;
		case HenshinPackage.ATTRIBUTE__TYPE:
			setType((EAttribute) newValue);
			return;
		case HenshinPackage.ATTRIBUTE__VALUE:
			setValue((String) newValue);
			return;
		case HenshinPackage.ATTRIBUTE__NODE:
			setNode((Node) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void eUnset(final int featureID) {
		switch (featureID) {
		case HenshinPackage.ATTRIBUTE__ACTION:
			setAction(ACTION_EDEFAULT);
			return;
		case HenshinPackage.ATTRIBUTE__TYPE:
			setType((EAttribute) null);
			return;
		case HenshinPackage.ATTRIBUTE__VALUE:
			setValue(VALUE_EDEFAULT);
			return;
		case HenshinPackage.ATTRIBUTE__NODE:
			setNode((Node) null);
			return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	public boolean eIsSetGen(final int featureID) {
		switch (featureID) {
		case HenshinPackage.ATTRIBUTE__ACTION:
			return ACTION_EDEFAULT == null ? getAction() != null : !ACTION_EDEFAULT.equals(getAction());
		case HenshinPackage.ATTRIBUTE__TYPE:
			return this.type != null;
		case HenshinPackage.ATTRIBUTE__VALUE:
			return VALUE_EDEFAULT == null ? this.value != null : !VALUE_EDEFAULT.equals(this.value);
		case HenshinPackage.ATTRIBUTE__NODE:
			return basicGetNode() != null;
		case HenshinPackage.ATTRIBUTE__CONSTANT:
			return CONSTANT_EDEFAULT == null ? this.constant != null : !CONSTANT_EDEFAULT.equals(this.constant);
		case HenshinPackage.ATTRIBUTE__NULL:
			return this.null_ != NULL_EDEFAULT;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	@Override
	public boolean eIsSet(final int featureID) {
		if (featureID == HenshinPackage.ATTRIBUTE__ACTION) {
			return false;
		}
		return eIsSetGen(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public int eBaseStructuralFeatureID(final int derivedFeatureID, final Class<?> baseClass) {
		if (baseClass == GraphElement.class) {
			switch (derivedFeatureID) {
			case HenshinPackage.ATTRIBUTE__ACTION:
				return HenshinPackage.GRAPH_ELEMENT__ACTION;
			default:
				return -1;
			}
		}
		return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public int eDerivedStructuralFeatureID(final int baseFeatureID, final Class<?> baseClass) {
		if (baseClass == GraphElement.class) {
			switch (baseFeatureID) {
			case HenshinPackage.GRAPH_ELEMENT__ACTION:
				return HenshinPackage.ATTRIBUTE__ACTION;
			default:
				return -1;
			}
		}
		return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated NOT
	 */
	@Override
	public String toString() {
		final String type = (this.type != null) ? this.type.getName() : null;
		return "Attribute (type: " + type + ", value: " + this.value + ")";
	}

} // AttributeImpl
