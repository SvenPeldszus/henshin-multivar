package org.eclipse.emf.henshin.variability.satchecker;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.emf.henshin.variability.util.SatChecker;
import org.junit.jupiter.api.Test;

class CNFDetectorTest {

	@Test
	void exlusiveAndOperatorsTest() {
		String expression = "a & b & c & d";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void exlusiveOrOperatorsTest() {
		String expression = "a | b | c | d";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void bracketedExpressionTest() {
		String expression = "(a | b) & (c | d)";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void partiallyBracketedAtBeginningExpressionTest() {
		String expression = "(a | b) & c & d";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void partiallyBracketedAtEndExpressionTest() {
		String expression = "a & b & (c | d)";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void partiallyBracketedInbetweenExpressionTest() {
		String expression = "a & (b | c) & d";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void fullyBracketedAndExpressionTest() {
		String expression = "(a & b & c & d)";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void partiallyBracketedAtBeginningAndExpressionTest() {
		String expression = "(a & b) & c & d";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void partiallyBracketedAtEndAndExpressionTest() {
		String expression = "a & b & (c & d)";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void partiallyBracketedInbetweenAndExpressionTest() {
		String expression = "a & (b & c) & d";
		assertTrue(SatChecker.isCNF(expression));
	}
	
	@Test
	void mixedOperatorsAndFirstTest() {
		String expression = "a & b | c";
		assertFalse(SatChecker.isCNF(expression));
	}
	
	@Test
	void mixedOperatorsOrFirstTest() {
		String expression = "a | b & c";
		assertFalse(SatChecker.isCNF(expression));
	}
	
	@Test
	void mixedOperatorsBracketedAndTest() {
		String expression = "(a & b) | c";
		assertFalse(SatChecker.isCNF(expression));
	}

}
