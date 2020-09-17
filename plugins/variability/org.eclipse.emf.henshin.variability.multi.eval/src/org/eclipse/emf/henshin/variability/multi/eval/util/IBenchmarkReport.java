/**
 * 
 */
package org.eclipse.emf.henshin.variability.multi.eval.util;

import java.util.List;

import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;

/**
 * @author speldszus
 *
 */
public interface IBenchmarkReport {

	/**
	 * @param exampleID
	 */
	void beginNewEntry(String exampleID);

	/**
	 * @param unit
	 * @param graphInitial
	 * @param graphChanged
	 * @param runtime
	 */
	void addSubEntry(Unit unit, int graphInitial, int graphChanged, long runtime);

	/**
	 * @param graphInitially
	 * @param graphChanged
	 * @param runtime
	 * @param detectedRules
	 */
	void finishEntry(int graphInitially, int graphChanged, long runtime, List<Rule> detectedRules, RuleSet set);

	/**
	 * 
	 */
	void start();

}
