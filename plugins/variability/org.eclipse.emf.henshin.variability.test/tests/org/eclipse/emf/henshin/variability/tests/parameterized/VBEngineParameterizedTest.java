/**
 *
 */
package org.eclipse.emf.henshin.variability.tests.parameterized;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.InconsistentRuleException;
import org.eclipse.emf.henshin.variability.VBRuleApplicationImpl;
import org.eclipse.emf.henshin.variability.matcher.VBMatch;
import org.eclipse.emf.henshin.variability.matcher.VBMatcher;
import org.eclipse.emf.henshin.variability.tests.parameterized.create.TestCreator;
import org.eclipse.emf.henshin.variability.tests.parameterized.data.TestResult;
import org.eclipse.emf.henshin.variability.tests.parameterized.data.VBTestData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author speldszus
 *
 *         A test for checking the Henshin engine. The tests are specified in
 *         JSON files described by inner classes.
 *
 */
@RunWith(Parameterized.class)
public class VBEngineParameterizedTest {

	private static final File dataFile = new File("data");
	private static final boolean DEBUG = false;
	private final VBTestData data;

	public VBEngineParameterizedTest(VBTestData data) {
		this.data = data;
	}

	@Parameters(name = "{0}")
	public static Collection<VBTestData> collectTests() {
		Collection<VBTestData> tests = new LinkedList<>();
		for (File folder : dataFile.listFiles()) {
			if (folder.isDirectory()) {
				List<File> expectFiles = new LinkedList<>();
				List<File> metaModelFiles = new LinkedList<>();
				for (File file : folder.listFiles()) {
					if (file.isFile()) {
						if (file.getName().endsWith(".json")) {
							expectFiles.add(file);
						} else if (file.getName().endsWith(".ecore")) {
							metaModelFiles.add(file);
						}
					}
				}
				for (File expectFile : expectFiles) {
					tests.addAll(TestCreator.createTests(metaModelFiles, expectFile));
				}
			}
		}
		return tests;
	}

	/**
	 * Executes the specified test
	 *
	 * @param dataFile The test specification
	 * @throws InconsistentRuleException If a inconsistent rule should be executed
	 */
	@Test
	public void testVBEngine() throws InconsistentRuleException {
		EngineImpl engine = new EngineImpl();
		EGraphImpl graph = new EGraphImpl(this.data.getResource());
		Map<String, Boolean> configuration = this.data.getConfiguration();
		Rule rule = this.data.getRule();

		VBMatcher matcher = new VBMatcher(rule, graph, configuration);
		Set<? extends VBMatch> allMatches = matcher.findMatches();


		for(VBMatch match : allMatches) {
			RuleApplication vbRuleApp = new VBRuleApplicationImpl(engine, graph, configuration, match);
			vbRuleApp.execute(null);
		}

		if (DEBUG) {
			save();
		}

		int modelSize = getModelSize(this.data);
		for (TestResult check : this.data.getExpect()) {
			switch (check.getKind()) {
			case MATCHES:
				assertEquals("Number of matches not as expected!", ((Number) check.getValue()).intValue(), allMatches.size());
				break;
			case MODEL_SIZE:
				assertEquals("Model size not as expected!", ((Number) check.getValue()).intValue(), modelSize);
				break;
			}
		}
	}

	/**
	 *
	 */
	private void save() {
		Path path = Paths.get("debug/" + this.data.getResource().getURI().lastSegment());
		path.getParent().toFile().mkdirs();
		try (OutputStream outputStream = Files.newOutputStream(path)) {
			this.data.getResource().save(outputStream, Collections.emptyMap());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculates the size of the test model in terms of model elements
	 *
	 * @param data The test data
	 * @return The size of the model
	 */
	protected int getModelSize(VBTestData data) {
		int modelSize = 0;
		EcoreUtil.resolveAll(data.getResource());
		TreeIterator<EObject> contents = data.getResource().getAllContents();
		while (contents.hasNext()) {
			EObject next = contents.next();
			modelSize++;
		}
		return modelSize;
	}
}
