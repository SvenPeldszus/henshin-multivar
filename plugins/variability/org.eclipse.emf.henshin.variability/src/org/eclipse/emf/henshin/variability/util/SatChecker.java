package org.eclipse.emf.henshin.variability.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import aima.core.logic.propositional.kb.data.Clause;
import aima.core.logic.propositional.kb.data.Literal;
import aima.core.logic.propositional.parsing.ast.PropositionSymbol;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ClauseCollector;
import aima.core.logic.propositional.visitors.ConvertToCNF;
import aima.core.logic.propositional.visitors.SymbolCollector;

/**
 *
 * @author Daniel Strüber
 * @author Sven Peldszus
 *
 */
public class SatChecker {

	private final ISolver solver;
	private final Map<Integer, String> indicesToSymbols;
	private final Map<String, Integer> symbolsToIndices;

	public SatChecker(final Sentence cnf) throws ContradictionException {
		this.indicesToSymbols = new HashMap<>();
		this.symbolsToIndices = new HashMap<>();
		this.solver = createSolver(cnf, this.indicesToSymbols, this.symbolsToIndices);
	}

	public static ISolver createSolver(final Sentence cnf, final Map<Integer, String> indicesToSymbols,
			final Map<String, Integer> symbolsToIndices) throws ContradictionException {
		return addToSolver(cnf, indicesToSymbols, symbolsToIndices, SolverFactory.newDefault());
	}

	private static ISolver addToSolver(final Sentence cnf, final Map<Integer, String> indicesToSymbols,
			final Map<String, Integer> symbolsToIndices, final ISolver solver) throws ContradictionException {
		final Set<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(cnf);
		final Set<Clause> clauses = ClauseCollector.getClausesFrom(cnf);

		final Map<PropositionSymbol, Integer> indices = getSymbol2IndexMap(symbols);
		for (final Entry<PropositionSymbol, Integer> entry : indices.entrySet()) {
			final String symbol = entry.getKey().getSymbol();
			final Integer index = entry.getValue();
			indicesToSymbols.put(index, symbol);
			symbolsToIndices.put(symbol, index);
		}

		final int numberOfVariables = symbols.size();
		final int numberOfClauses = clauses.size();

		solver.setDBSimplificationAllowed(true);
		solver.newVar(numberOfVariables);
		solver.setExpectedNumberOfClauses(numberOfClauses);
		for (final Clause clause : clauses) {
			if (clause.isFalse()) {
				return null;
			}
			if (!clause.isTautology()) {
				final int[] clauseArray = convertToArray(clause, indices);
				solver.addClause(new VecInt(clauseArray));
			}
		}
		return solver;
	}

	public static boolean isSatisfiable(final Sentence expr) {
		final Sentence cnf = ConvertToCNF.convert(expr);
		return isCNFSatisfiable(cnf);
	}

	public static boolean isCNFSatisfiable(final Sentence cnf) {
		ISolver solver;
		try {
			solver = addToSolver(cnf, new HashMap<>(), new HashMap<>(), SolverFactory.newDefault());
		} catch (final ContradictionException e) {
			return false;
		}
		if (solver == null) {
			return false;
		}
		try {
			return solver.isSatisfiable();
		} catch (final TimeoutException e) {
			throw new RuntimeException("Timeout during evaluation of satisfiability.");
		}
	}

	public static boolean isContradiction(final Sentence expr) {
		return !isSatisfiable(expr);
	}

	private static Map<PropositionSymbol, Integer> getSymbol2IndexMap(final Set<PropositionSymbol> symbols) {
		final Map<PropositionSymbol, Integer> list2Index = new HashMap<>(symbols.size());
		int counter = 1;
		for (final PropositionSymbol symbol : symbols) {
			list2Index.put(symbol, counter);
			counter++;
		}
		return list2Index;
	}

	private static int[] convertToArray(final Clause clause, final Map<PropositionSymbol, Integer> indices) {
		final Set<Literal> literals = clause.getLiterals();
		final int[] result = new int[literals.size()];
		int counter = 0;
		for (final Literal literal : literals) {
			final int sign = literal.isPositiveLiteral() ? 1 : -1;
			final PropositionSymbol symbol = literal.getAtomicSentence();
			final int index = indices.get(symbol);
			result[counter] = sign * index;
			counter++;
		}
		return result;
	}

	public static boolean isCNF(final String expr) {
		if ((expr == null) || expr.isEmpty() || expr.contains("xor(")) {
			return false;
		}

		final Deque<Character> parenthesis = new LinkedList<>();

		final char[] exprArr = expr.toCharArray();
		boolean foundAndOperator = false;
		boolean foundOrOperator = false;
		boolean mustOnlyContainAndOperators = false;
		boolean mustOnlyContainOrOperators = false;

		for (final char element : exprArr) {
			switch (element) {
			case '&':
				if (!parenthesis.isEmpty() && foundOrOperator) {
					if (mustOnlyContainOrOperators) {
						return false;
					} else {
						mustOnlyContainAndOperators = true;
					}
				} else if (mustOnlyContainOrOperators) {
					return false;
				}
				foundAndOperator = true;
				break;
			case '|':
				if (parenthesis.isEmpty()) {
					if (foundAndOperator || mustOnlyContainAndOperators) {
						return false;
					} else {
						mustOnlyContainOrOperators = true;
					}
				}
				foundOrOperator = true;
				break;
			case '(':
				parenthesis.add(element);
				break;
			case ')':
				if (parenthesis.isEmpty()) {
					return false;
				} else {
					parenthesis.pop();
				}
				break;
			default:
				break;
			}
		}
		return true;

	}

	private IVecInt createVec(final Collection<String> initiallyTrue, final Collection<String> initiallyFalse) {
		final Set<String> symbols = this.symbolsToIndices.keySet();
		final List<String> tf = initiallyTrue.parallelStream().filter(symbols::contains).collect(Collectors.toList());
		final List<String> ff = initiallyFalse.parallelStream().filter(symbols::contains).collect(Collectors.toList());
		final int[] vec = new int[tf.size() + ff.size()];
		int pos = 0;
		for (final String t : tf) {
			final Integer integer = this.symbolsToIndices.get(t);
			if (integer != null) {
				vec[pos++] = integer;
			}
		}
		for (final String t : ff) {
			final Integer integer = this.symbolsToIndices.get(t);
			if (integer != null) {
				vec[pos++] = integer * -1;
			}
		}
		return new VecInt(vec);
	}

	/**
	 * Calculates all possible rule products and their feature configurations
	 *
	 * @param initiallyFalseFeatures2
	 * @param initiallyTrueFeatures2
	 *
	 * @param rule
	 * @param ruleInfo
	 * @param trueFeatureList
	 * @param falseFeatureList
	 * @param features
	 * @return
	 */
	public SatStatus calculateTrueAndFalseFeatures(final Collection<String> initiallyTrue, final Collection<String> initiallyFalse,
			final List<List<String>> trueFeatureList, final List<List<String>> falseFeatureList, final Collection<String> features) {
		final ISolver solver = new ModelIterator(this.solver);

		// Remove contained features
		final List<String> unusedFeatures = getUnusedFeatures(this.indicesToSymbols, features);

		final IVecInt assignments = createVec(initiallyTrue, initiallyFalse);
		// Line 6: iterate over all Solutions of Phi_rule
		try {
			while (solver.isSatisfiable(assignments)) {
				final int[] model = solver.model();
				final List<String> tmpTrueFeatures = new LinkedList<>();
				final List<String> tmpFalseFeatures = new LinkedList<>();
				for (final int selection : model) {
					final int abs = Math.abs(selection);
					if (selection > 0) {
						tmpTrueFeatures.add(this.indicesToSymbols.get(abs));
					} else {
						tmpFalseFeatures.add(this.indicesToSymbols.get(abs));
					}
				}
				if (unusedFeatures.isEmpty()) {
					trueFeatureList.add(tmpTrueFeatures);
					falseFeatureList.add(tmpFalseFeatures);
				} else {
					int bitVector = (int) Math.pow(2, unusedFeatures.size() - 1);
					while (bitVector >= 0) {
						final LinkedList<String> trueFeatures = new LinkedList<>(tmpTrueFeatures);
						final LinkedList<String> falseFeatures = new LinkedList<>(tmpFalseFeatures);
						for (int i = 0; i < unusedFeatures.size(); i++) {
							if ((((1 << (unusedFeatures.size() - i - 1)) & bitVector) != 0)) {
								trueFeatures.add(unusedFeatures.get(i));
							} else {
								falseFeatures.add(unusedFeatures.get(i));
							}
						}
						trueFeatureList.add(trueFeatures);
						falseFeatureList.add(falseFeatures);
						bitVector--;
					}
				}

			}
		} catch (final TimeoutException e1) {
			return SatStatus.SATTimeout;
		}
		return SatStatus.OK;
	}

	private static List<String> getUnusedFeatures(final Map<Integer, String> symbolsToIndices, final Collection<String> features) {
		final ArrayList<String> unusedFeatures = new ArrayList<>(features.size());
		for (final String next : features) {
			if (!symbolsToIndices.containsValue(next)) {
				unusedFeatures.add(next);
			}
		}
		return unusedFeatures;
	}

	public boolean isSatisfiable(final Collection<String> trueFeatures, final Collection<String> falseFeatures)
			throws TimeoutException {
		return this.solver.isSatisfiable(createVec(trueFeatures, falseFeatures));
	}
}