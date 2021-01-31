package org.eclipse.emf.henshin.variability.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aima.core.logic.common.ParserException;
import aima.core.logic.propositional.parsing.PLParser;
import aima.core.logic.propositional.parsing.ast.ComplexSentence;
import aima.core.logic.propositional.parsing.ast.Connective;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToCNF;

/**
 * This class serves as a cache for SAT evaluation results, helping to avoid
 * performing the same computations repeatedly.
 *
 * @author Daniel Strüber
 *
 */
public class FeatureExpression {
	private static PropositionalParser parser = new PropositionalParser();

	public static final Sentence TRUE = parser.parse(Logic.TRUE);
	public static final Sentence FALSE = parser.parse(Logic.FALSE);

	static Map<Sentence, Map<Sentence, Boolean>> implies = new HashMap<>();
	static Map<Sentence, Map<Sentence, Boolean>> contradicts = new HashMap<>();
	static Map<Sentence, Map<Sentence, Sentence>> and = new HashMap<>();
	static Map<Sentence, Map<Sentence, Sentence>> andNot = new HashMap<>();

	/**
	 * Does expression 1 imply expression 2?
	 *
	 * @param expr1
	 * @param expr2
	 * @return
	 */
	public static boolean implies(final Sentence expr1, final Sentence expr2) {
		if (implies.containsKey(expr1)) {
			if (implies.get(expr1).containsKey(expr2)) {
				return implies.get(expr1).get(expr2);
			} else {
				final boolean val = SatChecker.isContradiction(andNot(expr1, expr2));
				implies.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			implies.put(expr1, new HashMap<>());
			return implies(expr1, expr2);
		}
	}

	public static Sentence and(final Sentence expr1, final Sentence expr2) {
		if (and.containsKey(expr1)) {
			if (and.get(expr1).containsKey(expr2)) {
				return and.get(expr1).get(expr2);
			} else {
				final Sentence val = Sentence.newConjunction(expr1, expr2);
				and.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			and.put(expr1, new HashMap<>());
			return and(expr1, expr2);
		}
	}

	public static Sentence andNot(final Sentence expr1, final Sentence expr2) {
		if (andNot.containsKey(expr1)) {
			if (andNot.get(expr1).containsKey(expr2)) {
				return andNot.get(expr1).get(expr2);
			} else {
				final Sentence val = Sentence.newConjunction(expr1, new ComplexSentence(Connective.NOT, expr2));
				andNot.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			andNot.put(expr1, new HashMap<>());
			return andNot(expr1, expr2);
		}
	}

	public static Sentence and(final Sentence... sentences) {
		return and(Arrays.asList(sentences));
	}

	public static Sentence and(final Collection<Sentence> values) {
		final Set<Sentence> set = new HashSet<>(values);
		if (set.contains(FALSE)) {
			return FALSE;
		}
		set.remove(TRUE);
		return Sentence.newConjunction(new ArrayList<>(set));
	}

	public static Sentence or(final Sentence... sentences) {
		return or(Arrays.asList(sentences));
	}

	public static Sentence or(final Collection<Sentence> sentences) {
		final Set<Sentence> set = new HashSet<>(sentences);
		if(set.contains(TRUE)) {
			return TRUE;
		}
		set.remove(FALSE);
		return Sentence.newDisjunction(new ArrayList<>(set));
	}

	public static Sentence negate(final Sentence sentence) {
		if (FeatureExpression.TRUE.equals(sentence)) {
			return FeatureExpression.FALSE;
		} else if (FeatureExpression.FALSE.equals(sentence)) {
			return FeatureExpression.TRUE;
		}
		return new ComplexSentence(Connective.NOT, sentence);
	}

	public static Sentence getExpr(final String condition) {
		if (condition.isEmpty()) {
			return TRUE;
		}
		Sentence result = null;
		try {
			result = ConvertToCNF.convert(parser.parse(condition));
		} catch (final ParserException ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Does expression 1 contradict expression 2?
	 *
	 * @param expr1
	 * @param expr2
	 * @return
	 */
	public static boolean contradicts(final Sentence expr1, final Sentence expr2) {
		if (contradicts.containsKey(expr1)) {
			if (contradicts.get(expr1).containsKey(expr2)) {
				return contradicts.get(expr1).get(expr2);
			} else {
				final boolean val = SatChecker.isContradiction(Sentence.newConjunction(expr1, expr2));
				contradicts.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			contradicts.put(expr1, new HashMap<>());
			return contradicts(expr1, expr2);
		}
	}

	/**
	 *
	 * @author speldszus
	 */
	private static class PropositionalParser extends PLParser {

		private final HashMap<String, Sentence> exprToSentence;

		/**
		 * Creates a new parser and initializes a map with already parsed expressions
		 */
		public PropositionalParser() {
			final Sentence trueSentence = super.parse(Logic.TRUE);
			this.exprToSentence = new HashMap<>();
			this.exprToSentence.put(Logic.TRUE, trueSentence);
			this.exprToSentence.put("", trueSentence);
		}

		@Override
		public Sentence parse(final String input) {
			final String trimmed = input.trim();
			if (this.exprToSentence.containsKey(trimmed)) {
				return this.exprToSentence.get(trimmed);
			}
			final String resolved = Logic.resolveSynonyms4Aima(XorEncoderUtil.encodeXor(trimmed));
			final Sentence sentence = super.parse(resolved);
			this.exprToSentence.put(trimmed, sentence);
			this.exprToSentence.put(resolved, sentence);
			return sentence;
		}
	}
}
