package org.eclipse.emf.henshin.variability.tests.utils;

import static org.junit.Assert.assertEquals;

import org.eclipse.emf.henshin.variability.util.XorEncoderUtil;
import org.junit.Test;

public class LogicTests {

	@Test
	public void testXOR0() {
		assertEquals("((A) | (B)) & ( ~(A) | ~(B))", XorEncoderUtil.encodeXor("((A) | (B)) & ( ~(A) | ~(B))"));
	}

	@Test
	public void testXOR1() {
		assertEquals("((A) | (B)) & ( ~(A) | ~(B))", XorEncoderUtil.encodeXor("xor(A, B)"));
	}

	@Test
	public void testXOR2() {
		assertEquals("((A) | (B) | (C)) & ( ~(A) | ~(B)) & ( ~(A) | ~(C)) & ( ~(B) | ~(C))",
				XorEncoderUtil.encodeXor("xor(A, B, C)"));
	}

	@Test
	public void testXOR3() {
		assertEquals(
				"((a) | (b) | (c) | (d)) & ( ~(a) | ~(b)) & ( ~(a) | ~(c)) & ( ~(a) | ~(d)) & ( ~(b) | ~(c)) & ( ~(b) | ~(d)) & ( ~(c) | ~(d))",
				XorEncoderUtil.encodeXor("xor(a, b, c, d)"));
	}

	@Test
	public void testXOR4() {
		assertEquals("((A) | (!B) | (D)) & ( ~(A) | ~(!B)) & ( ~(A) | ~(D)) & ( ~(!B) | ~(D))",
				XorEncoderUtil.encodeXor("xor(A, !B, D)"));
	}

	@Test
	public void testXOR5() {
		assertEquals("((A) | (((B) | (C)) & ( ~(B) | ~(C)))) & ( ~(A) | ~(((B) | (C)) & ( ~(B) | ~(C))))",
				XorEncoderUtil.encodeXor("xor(A, xor(B,C))"));
	}

	@Test
	public void testXOR6() {
		assertEquals("((A) | (B)) & ( ~(A) | ~(B)) and ((A) | (B)) & ( ~(A) | ~(B))", XorEncoderUtil.encodeXor("xor(A, B) and xor(A, B)"));
	}

	@Test
	public void testXOR7() {
		assertEquals("((A) | (B & C)) & ( ~(A) | ~(B & C))", XorEncoderUtil.encodeXor("xor(A, B & C)"));
	}

	@Test
	public void testXOR8() {
		assertEquals("((A) | (((B) | (C)) & ( ~(B) | ~(C)))) & ( ~(A) | ~(((B) | (C)) & ( ~(B) | ~(C))))", XorEncoderUtil.encodeXor("xor(A, xor(B,C))"));
	}
}
