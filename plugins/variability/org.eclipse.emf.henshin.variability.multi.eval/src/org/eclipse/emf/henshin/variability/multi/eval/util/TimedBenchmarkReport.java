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
public class TimedBenchmarkReport implements IBenchmarkReport {

	private String current;

	@Override
	public void start() {
		
	}
	
	@Override
	public void beginNewEntry(String exampleID) {
		current = exampleID;
	}

	@Override
	public void addSubEntry(Unit unit, int graphInitial, int graphChanged, long runtime) {
		
	}

	@Override
	public void finishEntry(int graphInitially, int graphChanged, long runtime, List<Rule> detectedRules, RuleSet set) {
		System.out.println(current+"\t\t\t"+runtime);
	}

}
