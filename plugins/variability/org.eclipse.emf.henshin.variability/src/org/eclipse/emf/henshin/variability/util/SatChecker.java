package org.eclipse.emf.henshin.variability.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;
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

	private ISolver solver;
	private Map<Integer, String> indicesToSymbols;
	private Map<String, Integer> symbolsToIndices;

	public SatChecker(Sentence cnf) throws ContradictionException {
		indicesToSymbols = new HashMap<>();
		symbolsToIndices = new HashMap<>();
		solver = createSolver(cnf, indicesToSymbols, symbolsToIndices);
	}

	public static ISolver createSolver(Sentence cnf, Map<Integer, String> indicesToSymbols,
			Map<String, Integer> symbolsToIndices) throws ContradictionException {
		return addToSolver(cnf, indicesToSymbols, symbolsToIndices, SolverFactory.newDefault());
	}

	private static ISolver addToSolver(Sentence cnf, Map<Integer, String> indicesToSymbols,
			Map<String, Integer> symbolsToIndices, ISolver solver) throws ContradictionException {
		Set<PropositionSymbol> symbols = SymbolCollector.getSymbolsFrom(cnf);
		Set<Clause> clauses = ClauseCollector.getClausesFrom(cnf);

		Map<PropositionSymbol, Integer> indices = getSymbol2IndexMap(symbols);
		for (Entry<PropositionSymbol, Integer> entry : indices.entrySet()) {
			String symbol = entry.getKey().getSymbol();
			Integer index = entry.getValue();
			indicesToSymbols.put(index, symbol);
			symbolsToIndices.put(symbol, index);
		}

		int numberOfVariables = symbols.size();
		int numberOfClauses = clauses.size();

		solver.setDBSimplificationAllowed(true);
		solver.newVar(numberOfVariables);
		solver.setExpectedNumberOfClauses(numberOfClauses);
		for (Clause clause : clauses) {
			if (clause.isFalse()) {
				return null;
			}
			if (!clause.isTautology()) {
				int[] clauseArray = convertToArray(clause, indices);
				solver.addClause(new VecInt(clauseArray));
			}
		}
		return solver;
	}

	public static boolean isSatisfiable(Sentence expr) {
		Sentence cnf = ConvertToCNF.convert(expr);
		return isCNFSatisfiable(cnf);
	}

	public static boolean isCNFSatisfiable(Sentence cnf) {
		ISolver solver;
		try {
			solver = addToSolver(cnf, new HashMap<>(), new HashMap<>(), SolverFactory.newDefault());
		} catch (ContradictionException e) {
			return false;
		}
		if (solver == null) {
			return false;
		}
		try {
			return solver.isSatisfiable();
		} catch (TimeoutException e) {
			throw new RuntimeException("Timeout during evaluation of satisfiability.");
		}
	}

	public static boolean isContradiction(Sentence expr) {
		return !isSatisfiable(expr);
	}

	private static Map<PropositionSymbol, Integer> getSymbol2IndexMap(Set<PropositionSymbol> symbols) {
		Map<PropositionSymbol, Integer> list2Index = new HashMap<>(symbols.size());
		int counter = 1;
		for (PropositionSymbol symbol : symbols) {
			list2Index.put(symbol, Integer.valueOf(counter));
			counter++;
		}
		return list2Index;
	}

	private static int[] convertToArray(Clause clause, Map<PropositionSymbol, Integer> indices) {
		Set<Literal> literals = clause.getLiterals();
		int[] result = new int[literals.size()];
		int counter = 0;
		for (Literal literal : literals) {
			int sign = literal.isPositiveLiteral() ? 1 : -1;
			PropositionSymbol symbol = literal.getAtomicSentence();
			int index = indices.get(symbol).intValue();
			result[counter] = sign * index;
			counter++;
		}
		return result;
	}

	public static boolean isCNF(String expr) {
		if (expr == null || expr.isEmpty() || expr.contains("xor("))
			return false;

		Deque<Character> parenthesis = new LinkedList<>();

		char[] exprArr = expr.toCharArray();
		boolean foundAndOperator = false;
		boolean foundOrOperator = false;
		boolean mustOnlyContainAndOperators = false;
		boolean mustOnlyContainOrOperators = false;

		for (int i = 0; i < exprArr.length; i++) {
			switch (exprArr[i]) {
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
				parenthesis.add(exprArr[i]);
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

	private IVecInt createVec(Collection<String> initiallyTrue, Collection<String> initiallyFalse) {
		Set<String> symbols = symbolsToIndices.keySet();
		List<String> tf = initiallyTrue.parallelStream().filter(symbols::contains).collect(Collectors.toList());
		List<String> ff = initiallyFalse.parallelStream().filter(symbols::contains).collect(Collectors.toList());
		int[] vec = new int[tf.size() + ff.size()];
		int pos = 0;
		for (String t : tf) {
			Integer integer = symbolsToIndices.get(t);
			if (integer != null) {
				vec[pos++] = integer;
			}
		}
		for (String t : ff) {
			Integer integer = symbolsToIndices.get(t);
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
	public SatStatus calculateTrueAndFalseFeatures(Collection<String> initiallyTrue, Collection<String> initiallyFalse,
			List<List<String>> trueFeatureList, List<List<String>> falseFeatureList, Collection<String> features) {
		ISolver solver = new ModelIterator(this.solver);

		// Remove contained features
		List<String> unusedFeatures = getUnusedFeatures(indicesToSymbols, features);

		IVecInt assignments = createVec(initiallyTrue, initiallyFalse);
		// Line 6: iterate over all Solutions of Phi_rule
		try {
			while (solver.isSatisfiable(assignments)) {
				int[] model = solver.model();
				List<String> tmpTrueFeatures = new LinkedList<>();
				List<String> tmpFalseFeatures = new LinkedList<>();
				for (int selection : model) {
					int abs = Math.abs(selection);
					if (selection > 0) {
						tmpTrueFeatures.add(indicesToSymbols.get(abs));
					} else {
						tmpFalseFeatures.add(indicesToSymbols.get(abs));
					}
				}
				if (unusedFeatures.isEmpty()) {
					trueFeatureList.add(tmpTrueFeatures);
					falseFeatureList.add(tmpFalseFeatures);
				} else {
					int bitVector = (int) Math.pow(2, unusedFeatures.size() - 1);
					while (bitVector >= 0) {
						LinkedList<String> trueFeatures = new LinkedList<>(tmpTrueFeatures);
						LinkedList<String> falseFeatures = new LinkedList<>(tmpFalseFeatures);
						for (int i = 0; i < unusedFeatures.size(); i++) {
							if (((1 << unusedFeatures.size() - i - 1 & bitVector) != 0)) {
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
		} catch (TimeoutException e1) {
			return SatStatus.SATTimeout;
		}
		return SatStatus.OK;
	}

	private static List<String> getUnusedFeatures(Map<Integer, String> symbolsToIndices, Collection<String> features) {
		ArrayList<String> unusedFeatures = new ArrayList<>(features.size());
		for (String next : features) {
			if (!symbolsToIndices.containsValue(next)) {
				unusedFeatures.add(next);
			}
		}
		return unusedFeatures;
	}

	public boolean isSatisfiable(Collection<String> trueFeatures, Collection<String> falseFeatures)
			throws TimeoutException {
		return solver.isSatisfiable(createVec(trueFeatures, falseFeatures));
	}
}