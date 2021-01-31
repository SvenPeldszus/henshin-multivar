package org.eclipse.emf.henshin.variability.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import aima.core.logic.propositional.parsing.ast.Connective;
import aima.core.logic.propositional.parsing.ast.Sentence;

/**
 *
 * @author Daniel Strüber
 * @author Sven Peldszus
 *
 */
public class Logic {

	private Logic() {
		// This class shouldn't be instantiated
	}

	public static final String TRUE = Boolean.toString(true);
	private static final String TRUE_SPACE = ' ' + TRUE + ' ';
	public static final String FALSE = Boolean.toString(false);
	private static final String FALSE_SPACE = ' ' + FALSE + ' ';
	private static final String NOT = " ~ ";
	private static final String AND = " & ";
	private static final String OR = " | ";
	private static final String LB = " ( ";
	private static final String RB = " ) ";
	private static String limitForCombinations = "0123456789";
	private static volatile ScriptEngine engine;

	protected static synchronized String choice(final List<String> expressions, final int min, final int max,
			final boolean forceFalse) {
		final List<String> combinationsIndexes = new ArrayList<>();
		if (expressions.size() > Logic.limitForCombinations.length()) {
			return TRUE_SPACE;
		}
		final String limit = Logic.limitForCombinations.substring(0, expressions.size());
		combinations(min, max, "", limit, combinationsIndexes);
		final List<String> orInput = new ArrayList<>();
		if (forceFalse) {
			for (int i = 0; i < combinationsIndexes.size(); ++i) {
				final String comb = combinationsIndexes.get(i);
				final List<String> andInput = new ArrayList<>();
				for (int j = 0; j < expressions.size(); ++j) {
					String andElement = "";
					if (comb.lastIndexOf(String.valueOf(j)) == -1) {
						andElement = negate(expressions.get(j));
					} else {
						andElement = expressions.get(j);
					}
					andInput.add(andElement);
				}
				orInput.add(and(andInput));
			}
		} else {
			for (int i = 0; i < combinationsIndexes.size(); ++i) {
				final String comb = combinationsIndexes.get(i);
				final List<String> andInput = new ArrayList<>();
				for (int j = 0; j < comb.length(); ++j) {
					andInput.add(expressions.get(Integer.parseInt(String.valueOf(comb.charAt(j)))));
				}
				orInput.add(and(andInput));
			}
		}
		return or(orInput);
	}

	protected static synchronized void combinations(final int min, final int max, final String prefix,
			final String elements, final List<String> combinationsIndexes) {
		if (prefix.length() >= min) {
			combinationsIndexes.add(prefix);
		}
		for (int i = 0; i < elements.length(); ++i) {
			if (prefix.length() < max) {
				combinations(min, max, String.valueOf(prefix) + elements.charAt(i), elements.substring(i + 1),
						combinationsIndexes);
			}
		}
	}

	protected static synchronized String claused(final String expression) {
		if ((expression == null) || (expression.length() == 0)) {
			return "";
		}
		if (isClaused(expression)) {
			return expression;
		}
		return Logic.LB + expression + Logic.RB;
	}

	private static boolean isClaused(final String expression) {
		if (!expression.startsWith("(") || !expression.endsWith(")")) {
			return false;
		}
		final Map<Integer, Integer> openClose = new HashMap<>();
		final Deque<Integer> open = new LinkedList<>();
		int index = 0;
		while (index < expression.length()) {
			final char c = expression.charAt(index);
			if (c == '(') {
				open.push(index);
			} else if (c == ')') {
				openClose.put(open.pop(), index);
			}
			index++;
		}
		return openClose.get(Integer.valueOf(0)).intValue() == (expression.length() - 1);
	}

	protected static synchronized String implies(final String lhs, final String rhs) {
		if ((lhs == null) || (lhs.length() == 0) || (rhs == null) || (rhs.length() == 0)) {
			throw new IllegalArgumentException();
		}
		final String lhsTrimmed = lhs.trim();
		final String rhsTrimmed = rhs.trim();
		if (FALSE.equals(lhsTrimmed) || TRUE.equals(rhsTrimmed) || lhsTrimmed.equals(rhsTrimmed)) {
			return TRUE_SPACE;
		}
		if (FALSE.equals(rhsTrimmed)) {
			return negate(lhs);
		}
		if (TRUE.equals(lhsTrimmed)) {
			return rhs;
		}
		return or(negate(lhs), rhs);
	}

	public static synchronized String negate(final String expression) {
		final String trimmed = expression.trim();
		if (trimmed.contentEquals(TRUE)) {
			return FALSE_SPACE;
		} else if (trimmed.equals(FALSE)) {
			return TRUE_SPACE;
		}
		return Logic.NOT + claused(expression);
	}

	private static synchronized String op(final List<String> expressions, final String op) {
		if ((expressions == null) || expressions.isEmpty()) {
			return "";
		}
		if (expressions.size() == 1) {
			return expressions.get(0);
		}
		String expression = "";
		String part = null;
		boolean first = true;
		for (final String expression2 : expressions) {
			part = expression2;
			if ((part != null) && (part.length() > 0)) {
				if (first) {
					expression = String.valueOf(expression) + claused(part);
					first = false;
				} else {
					expression = String.valueOf(expression) + op + claused(part);
				}
			}
		}
		return expression;
	}

	public static synchronized String or(final List<String> expressions) {
		final List<String> reduced = new ArrayList<>(expressions.size());
		for (final String expr : expressions) {
			final String trimmedExpr = expr.trim();
			if (TRUE.equals(trimmedExpr)) {
				return TRUE_SPACE;
			} else if (FALSE.equals(trimmedExpr)) {
				// Skip "| false" as id doesn't change the expression
			} else {
				reduced.add(expr);
			}
		}
		if (reduced.isEmpty()) {
			return Logic.FALSE_SPACE; // All expressions are FALSE
		}
		if (reduced.size() == 1) {
			return reduced.get(0);
		}
		String v1 = reduced.get(0);
		for (int i = 1; i < reduced.size(); i++) {
			v1 = or(v1, reduced.get(i));
		}
		return v1;
	}

	public static synchronized String or(final String s1, final String s2) {
		final String s1Trimmed = s1.trim();
		final String s2Trimmed = s2.trim();
		if (TRUE.equals(s1Trimmed) || TRUE.equals(s2Trimmed)) {
			return Logic.TRUE_SPACE;
		}
		if (Logic.negate(s1Trimmed).trim().equals(s2Trimmed) || Logic.negate(s2Trimmed).trim().equals(s1Trimmed)) {
			return Logic.TRUE_SPACE;
		}
		if (s1Trimmed.equals(s2Trimmed)) {
			return s1;
		}
		if (FALSE.equals(s1Trimmed)) {
			return s2;
		}
		if (FALSE.contentEquals(s2Trimmed)) {
			return s1;
		}
		return claused(s1) + Logic.OR + claused(s2);
	}

	public static synchronized String and(final Collection<String> conditions) {
		final List<String> reduced = new ArrayList<>(conditions.size());
		for (final String expr : conditions) {
			final String trimmed = expr.trim();
			if (FALSE.equals(trimmed)) {
				return FALSE_SPACE;
			} else if (TRUE.equals(trimmed)) {
				// Skip "& true" as id doesn't change the expression
			} else {
				reduced.add(expr);
			}
		}
		if (reduced.isEmpty()) {
			return Logic.TRUE_SPACE; // all elements are TRUE
		}
		if (reduced.size() == 1) {
			return reduced.get(0);
		}
		String v1 = reduced.get(0);
		for (int i = 1; i < reduced.size(); i++) {
			v1 = and(v1, reduced.get(i));
		}
		return v1;
	}

	public static synchronized String and(final String... conditions) {
		final List<String> reduced = new ArrayList<>(conditions.length);
		for (final String expr : conditions) {
			final String trimmed = expr.trim();
			if (FALSE.equals(trimmed)) {
				return FALSE_SPACE;
			} else if (TRUE.equals(trimmed)) {
				// Skip "& true" as id doesn't change the expression
			} else {
				reduced.add(expr);
			}
		}
		if (reduced.isEmpty()) {
			return Logic.TRUE_SPACE; // all elements are TRUE
		}
		String v1 = reduced.get(0);
		for (int i = 1; i < reduced.size(); i++) {
			v1 = and(v1, reduced.get(i));
		}
		return v1;
	}

	public static synchronized String and(final String s1, final String s2) {
		if (s1 == null) {
			return s2;
		}
		if (s2 == null) {
			return s1;
		}
		final String s1Trimmed = s1.trim();
		final String s2Trimmed = s2.trim();
		if (FALSE.equals(s1Trimmed) || FALSE.equals(s2Trimmed)) {
			return Logic.FALSE_SPACE;
		}
		if (Logic.negate(s1Trimmed).trim().equals(s2Trimmed) || Logic.negate(s2Trimmed).trim().equals(s1Trimmed)) {
			return Logic.FALSE_SPACE;
		}
		final List<String> list = new ArrayList<>();
		if (!"".equals(s1Trimmed) && !TRUE.equals(s1Trimmed)) {
			list.add(s1);
		}
		if (!"".equals(s2Trimmed) && !TRUE.equals(s2Trimmed) && !s1Trimmed.equals(s2Trimmed)) {
			list.add(s2);
		}
		if (list.isEmpty()) {
			return Logic.TRUE_SPACE;
		}
		if (list.size() == 1) {
			return list.get(0);
		}
		return op(list, Logic.AND);
	}

	protected static synchronized List<String> andMerge(final List<String> memberEnds,
			final HashMap<String, String> memberEndPCs, final HashMap<String, String> memberEndsTypesPCs) {
		final List<String> mergedList = new ArrayList<>();
		for (int i = 0; i < memberEnds.size(); ++i) {
			mergedList.add(and(memberEndPCs.get(memberEnds.get(i)), memberEndsTypesPCs.get(memberEnds.get(i))));
		}
		return mergedList;
	}

	protected static String simplifyDNF(final String dnf, final List<String> variables) {
		String expression = dnf;
		int i = 0;
		boolean delete = false;
		String newExpression = "";
		while (expression.lastIndexOf(" | ") != -1) {
			i = expression.lastIndexOf(" | ") + " | ".length();
			String clause = expression.substring(i, expression.length());
			delete = false;
			for (final String variable : variables) {
				if (((clause.contains(String.valueOf(Logic.AND) + variable) || clause.startsWith(variable))
						&& clause.contains(String.valueOf(Logic.NOT) + variable)) || clause.contains(Logic.FALSE)) {
					delete = true;
					break;
				}
			}
			expression = expression.substring(0, expression.lastIndexOf(" | "));
			if (!delete && (newExpression.indexOf(String.valueOf(Logic.LB) + clause + Logic.RB) == -1)
					&& (clause.indexOf(Logic.FALSE) == -1)) {
				for (final String variable : variables) {
					if (clause.indexOf(variable) != clause.lastIndexOf(variable)) {
						clause = clause.replaceAll(Logic.AND + Logic.TRUE + Logic.AND, Logic.AND)
								.replaceAll(String.valueOf(Logic.AND) + Logic.TRUE, "")
								.replaceAll(Logic.TRUE + Logic.AND, "");
						final String part1 = clause.substring(0, clause.indexOf(variable) + variable.length());
						String part2 = clause.substring(clause.indexOf(variable) + variable.length(), clause.length());
						part2 = part2.replaceAll(String.valueOf(Logic.AND) + Logic.NOT + variable, "");
						part2 = part2.replaceAll(String.valueOf(Logic.AND) + variable, "");
						clause = String.valueOf(part1) + part2;
					}
				}
				if ((clause.replace(" ", "").length() <= 0)
						|| newExpression.contains(String.valueOf(Logic.LB) + clause + Logic.RB)
						|| clause.contains(Logic.FALSE_SPACE)) {
					continue;
				}
				newExpression = String.valueOf(newExpression) + Logic.LB + clause + Logic.RB + Logic.OR;
			}
		}
		newExpression = newExpression.substring(0, newExpression.lastIndexOf(Logic.OR));
		return newExpression;
	}

	/**
	 * Evaluates a constraint with the given feature assignments
	 *
	 * @param pc            The constraint to evaluate
	 * @param trueFeatures  The variables assigned to true
	 * @param falseFeatures The variables assigned to false
	 * @return the evaluation result or null if not all variables are assigned to a
	 *         value
	 * @throws ScriptException
	 */
	public static boolean evaluate(final Sentence pc, final Collection<String> trueFeatures, final Collection<String> falseFeatures) {
		if (FeatureExpression.TRUE.equals(pc)) {
			return true;
		}
		try {
			final Map<String, Integer> symbolsToIndices = new HashMap<>();
			final ISolver solver = SatChecker.createSolver(pc, new HashMap<>(), symbolsToIndices);

			final Set<String> symbols = symbolsToIndices.keySet();
			final List<String> tf = trueFeatures.parallelStream().filter(symbols::contains).collect(Collectors.toList());
			final List<String> ff = falseFeatures.parallelStream().filter(symbols::contains).collect(Collectors.toList());
			final int[] vec = new int[tf.size() + ff.size()];
			int pos = 0;
			for (final String t : tf) {
				final Integer integer = symbolsToIndices.get(t);
				if (integer != null) {
					vec[pos++] = integer;
				}
			}
			for (final String t : ff) {
				final Integer integer = symbolsToIndices.get(t);
				if (integer != null) {
					vec[pos++] = integer * -1;
				}
			}
			final IVecInt assignments = new VecInt(vec);
			return solver.isSatisfiable(assignments);
		} catch (ContradictionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	public static Boolean evaluateJS(final Sentence pc, final Collection<String> trueFeatures, final Collection<String> falseFeatures)
			throws ScriptException {
		if (FeatureExpression.TRUE.equals(pc)) {
			return Boolean.TRUE;
		}
		if (engine == null) {
			engine = new ScriptEngineManager().getEngineByName("JavaScript");
		}
		final Bindings bindings = new SimpleBindings();
		for (final String trueFeature : trueFeatures) {
			bindings.put(trueFeature, Boolean.TRUE);
		}
		for (final String falseFeature : falseFeatures) {
			bindings.put(falseFeature, Boolean.FALSE);
		}

		Object result = null;
		try {
			result = engine.eval(getJavaScriptCondition(pc), bindings);
			if (result instanceof Boolean) {
				return (Boolean) result;
			} else if (result instanceof Integer) {
				final int number = (int) result;
				switch (number) {
				case 0:
					return false;
				case 1:
					return true;
				case -1:
					return false;
				default:
					break;
				}
			} else if (result == null) {
				return null;
			}
		} catch (final ScriptException e) {
			final Pattern pattern = Pattern.compile("ReferenceError: \\\".+\\\" is not defined");
			if (pattern.matcher(e.getMessage()).find()) {
				return null;
			}
		}

		throw new IllegalStateException("Unknown evaluation result:\n" + "Constraint: " + pc + "\n" + "True features: "
				+ String.join(", ", trueFeatures) + "\n" + "False features: " + String.join(", ", falseFeatures) + "\n"
				+ "Result: " + result);
	}

	private static String getJavaScriptCondition(final Sentence pc) {
		return pc.toString().replace('~', '!');
	}

	private static final Pattern and = Pattern.compile(" *(\\b(and|AND)\\b)|(\\&\\&) *");
	private static final Pattern or = Pattern.compile(" *(\\b(or|OR)\\b)|(\\|\\|) *");
	private static final Pattern not = Pattern.compile(" *(\\b(not|NOT)\\b)|\\!|-|~ *");

	public static String resolveSynonyms4Aima(String expression) {
		expression = and.matcher(expression).replaceAll(' ' + Connective.AND.getSymbol() + ' ');
		expression = or.matcher(expression).replaceAll(' ' + Connective.OR.getSymbol() + ' ');
		expression = not.matcher(expression).replaceAll(' ' + Connective.NOT.getSymbol() + ' ');
		return expression;
	}
}