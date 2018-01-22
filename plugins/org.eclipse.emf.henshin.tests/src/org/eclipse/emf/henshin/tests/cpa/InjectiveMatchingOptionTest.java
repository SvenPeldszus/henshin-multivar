/**
 * <copyright>
 * Copyright (c) 2010-2016 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.tests.cpa;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.henshin.cpa.CPAOptions;
import org.eclipse.emf.henshin.cpa.CPAUtility;
import org.eclipse.emf.henshin.cpa.CpaByAGG;
import org.eclipse.emf.henshin.cpa.ICriticalPairAnalysis;
import org.eclipse.emf.henshin.cpa.UnsupportedRuleException;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the support of rules with injective matching option on and off as well as the 'check' for sets of rules with
 * inconsistent injective matching option.
 * 
 * @author Kristopher Born
 *
 */
public class InjectiveMatchingOptionTest {

	final String PATH = "basic/testModelsCPA/injectiveMatchingOptionTest/";
	final String henshinFileName = "checkInjectiveMatching.henshin";

	Module module;
	private ICriticalPairAnalysis cpaByAgg;
	CPAOptions cpaOptions;

	@Before
	public void setUp() throws Exception {
		cpaByAgg = new CpaByAGG();
		cpaOptions = new CPAOptions();
	}

	/**
	 * Test for the 'check'-function of the passed rule set. Check function shall deny the inconsistent rule set
	 * regarding the injective matching property.
	 * 
	 * @throws UnsupportedRuleException when attempting to analyze rules which are not supported
	 */
	@Test
	public void checkInjectiveMatchingInconsistent() {
		String firstRuleName = "Rule1InjectiveMatchingOff";
		String secondRuleName = "Rule2InjectiveMatchingOn";

		List<Rule> firstRule = new LinkedList<Rule>();
		List<Rule> secondRule = new LinkedList<Rule>();

		HenshinResourceSet resourceSet = new HenshinResourceSet(PATH);
		module = resourceSet.getModule(henshinFileName, false);

		CPAUtility.extractSingleRules(module, firstRule, firstRuleName, secondRule, secondRuleName);

		boolean exceptionThrown = false;

		try {
			List<org.eclipse.emf.henshin.model.Rule> rulesToBeChecked = new ArrayList<org.eclipse.emf.henshin.model.Rule>(
					firstRule);
			rulesToBeChecked.addAll(secondRule);
			cpaByAgg.check(rulesToBeChecked);
		} catch (UnsupportedRuleException e) {
			if (e.getMessage().equals(UnsupportedRuleException.inconsistentInjectiveMatchingOptions))
				exceptionThrown = true;
		}
		Assert.assertTrue(exceptionThrown);
	}

}
