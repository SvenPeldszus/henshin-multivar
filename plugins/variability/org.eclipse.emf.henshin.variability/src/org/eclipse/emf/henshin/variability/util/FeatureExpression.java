package org.eclipse.emf.henshin.variability.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import aima.core.logic.common.ParserException;
import aima.core.logic.propositional.parsing.PLParser;
import aima.core.logic.propositional.parsing.ast.ComplexSentence;
import aima.core.logic.propositional.parsing.ast.Connective;
import aima.core.logic.propositional.parsing.ast.Sentence;

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
	public static boolean implies(Sentence expr1, Sentence expr2) {
		if (implies.containsKey(expr1)) {
			if (implies.get(expr1).containsKey(expr2)) {
				return implies.get(expr1).get(expr2);
			} else {
				boolean val = new SatChecker().isContradiction(andNot(expr1, expr2).toString());
				implies.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			implies.put(expr1, new HashMap<>());
			return implies(expr1, expr2);
		}
	}

	public static Sentence and(Sentence expr1, Sentence expr2) {
		if (and.containsKey(expr1)) {
			if (and.get(expr1).containsKey(expr2)) {
				return and.get(expr1).get(expr2);
			} else {
				Sentence val = Sentence.newConjunction(expr1, expr2);
				and.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			and.put(expr1, new HashMap<>());
			return and(expr1, expr2);
		}
	}

	public static Sentence andNot(Sentence expr1, Sentence expr2) {
		if (andNot.containsKey(expr1)) {
			if (andNot.get(expr1).containsKey(expr2)) {
				return andNot.get(expr1).get(expr2);
			} else {
				Sentence val = Sentence.newConjunction(expr1, new ComplexSentence(Connective.NOT, expr2));
				andNot.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			andNot.put(expr1, new HashMap<>());
			return andNot(expr1, expr2);
		}
	}

	public static Sentence getExpr(String condition) {
		Sentence result = null;		
		try {
			condition = XorEncoderUtil.encodeXor(condition);
			result = parser.parse(condition);
		} catch (ParserException ex) {
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
	public static boolean contradicts(Sentence expr1, Sentence expr2) {
		if (contradicts.containsKey(expr1)) {
			if (contradicts.get(expr1).containsKey(expr2)) {
				return contradicts.get(expr1).get(expr2);
			} else {
				boolean val = new SatChecker().isContradiction(Sentence.newConjunction(expr1, expr2));
				contradicts.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			contradicts.put(expr1, new HashMap<Sentence, Boolean>());
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
			Sentence trueSentence = super.parse(Logic.TRUE);
			this.exprToSentence = new HashMap<>();
			this.exprToSentence.put(Logic.TRUE, trueSentence);
			this.exprToSentence.put("", trueSentence);
		}

		@Override
		public Sentence parse(String input) {
			String trimmed = input.trim();
			if (this.exprToSentence.containsKey(trimmed)) {
				return this.exprToSentence.get(trimmed);
			}
			String resolved = resolveSynonyms(trimmed);
			Sentence sentence = super.parse(resolved);
			this.exprToSentence.put(trimmed, sentence);
			this.exprToSentence.put(resolved, sentence);
			return sentence;
		}

		private static final Pattern and = Pattern.compile(" *(\\b(and|AND)\\b)|(\\&\\&) *");
		private static final Pattern or = Pattern.compile(" *(\\b(or|OR)\\b)|(\\|\\|) *");
		private static final Pattern not = Pattern.compile(" *(\\b(not|NOT)\\b)|\\! *");

		public static String resolveSynonyms(String expression) {
			expression = and.matcher(expression).replaceAll(' ' + Connective.AND.getSymbol() + ' ');
			expression = or.matcher(expression).replaceAll(' ' + Connective.OR.getSymbol() + ' ');
			expression = not.matcher(expression).replaceAll(' ' + Connective.NOT.getSymbol() + ' ');
			return expression;
		}
	}

}
