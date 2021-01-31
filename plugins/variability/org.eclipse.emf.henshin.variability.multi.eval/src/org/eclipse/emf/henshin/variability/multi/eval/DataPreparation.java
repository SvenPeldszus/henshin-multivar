package org.eclipse.emf.henshin.variability.multi.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.variability.multi.eval.util.LoadingHelper.RuleSet;

public class DataPreparation {

	private final static RuleSet[] RULES = { RuleSet.CREATE, RuleSet.DELETE, RuleSet.MOVE };

	public static void main(final String[] args) throws IOException {
		final File folder = new File("umlrecog/output");
		final Map<RuleSet, Map<String, Long>> lifted = getCollectData(new File(folder, "lifted"));
		final Map<RuleSet, Map<String, Long>> vb = getCollectData(new File(folder, "vb"));

		final File table = new File(folder, "table-results.tex");
		if (table.exists()) {
			table.delete();
		}
		table.createNewFile();
		try (OutputStream stream = new FileOutputStream(table)) {
			stream.write(("\\begin{table*}[t]\n" + "\\centering\n" + "\\footnotesize\n"
					+ "\\begin{tabular}{l rr>{\\bfseries}rc rr>{\\bfseries}rc rr>{\\bfseries}rc rr>{\\bfseries}r}\n"
					+ "\\toprule\n"
					+ "\\vspace{-5pt} & \\multicolumn{3}{c}{\\textbf{Create/Set}} &~\\hspace{.12cm}~& \\multicolumn{3}{c}{\\textbf{Delete/Unset}} &~\\hspace{.12cm}~& \\multicolumn{3}{c}{\\textbf{Change/Move}} &~\\hspace{.12cm}~& \\multicolumn{3}{c}{\\textbf{TOTAL}} \\\\\n"
					+ " & \\multicolumn{3}{c}{\\rule{2.2cm}{.3pt}} && \\multicolumn{3}{c}{\\rule{2.2cm}{.3pt}} && \\multicolumn{3}{c}{\\rule{2.2cm}{.3pt}} && \\multicolumn{3}{c}{\\rule{2.2cm}{.35pt}}\\\\\n"
					+ " & lift & stage & \\normalfont{factor} && lift & stage & \\normalfont{factor} && lift & stage & \\normalfont{factor} && lift & stage & \\normalfont{factor} \\\\\n"
					+ "\\midrule\n").getBytes());
			final Set<String> projects = Stream.concat(lifted.values().stream(), vb.values().stream())
					.flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());
			for (final String project : projects) {
				stream.write(project.getBytes());

				appendSet(lifted, vb, stream, project, RuleSet.CREATE);
				stream.write('&');
				appendSet(lifted, vb, stream, project, RuleSet.DELETE);
				stream.write('&');
				appendSet(lifted, vb, stream, project, RuleSet.MOVE);
				stream.write("&& ".getBytes());
				final long liftValue = lifted.get(RuleSet.CREATE).get(project)+lifted.get(RuleSet.DELETE).get(project)+lifted.get(RuleSet.MOVE).get(project);
				stream.write(String.format("%.2f", liftValue / 1000d).getBytes());
				stream.write("\t& ".getBytes());
				final long vbValue = vb.get(RuleSet.CREATE).get(project)+vb.get(RuleSet.DELETE).get(project)+vb.get(RuleSet.MOVE).get(project);
				stream.write(String.format("%.2f", vbValue / 1000d).getBytes());
				stream.write("\t& ".getBytes());
				stream.write(String.format("%.2f", liftValue / (double) vbValue).getBytes());

				stream.write(" \\\\\n".getBytes());

			}
			stream.write(("\\bottomrule\n" + "\\end{tabular}\n" + "\\vspace{5pt}\n"
					+ "\\caption{Execution times (in seconds) of the lifting and the staged approach.}\n"
					+ "\\label{tab:results}\n" + "\\vspace{-20pt}\n" + "\\end{table*}").getBytes());
		}

		createCSVFiles(folder, lifted, vb);
		System.out.println("Generated CSV files and table");
	}

	public static void appendSet(final Map<RuleSet, Map<String, Long>> lifted, final Map<RuleSet, Map<String, Long>> vb,
			final OutputStream stream, final String project, final RuleSet set) throws IOException {
		stream.write("& ".getBytes());
		final long liftValue = lifted.get(set).get(project);
		stream.write(String.format("%.2f", liftValue / 1000d).getBytes());
		stream.write("\t& ".getBytes());
		final long vbValue = vb.get(set).get(project);
		stream.write(String.format("%.2f", vbValue / 1000d).getBytes());
		stream.write("\t& ".getBytes());
		stream.write(String.format("%.2f", liftValue / (double) vbValue).getBytes());
		stream.write('\t');
	}

	public static void createCSVFiles(final File folder, final Map<RuleSet, Map<String, Long>> lifted,
			final Map<RuleSet, Map<String, Long>> vb) throws IOException, FileNotFoundException {
		for (final RuleSet set : RULES) {
			final File out = new File(folder, set + ".csv");
			if (out.exists()) {
				out.delete();
			}
			out.createNewFile();

			try (FileOutputStream stream = new FileOutputStream(out)) {
				final Map<String, Long> projectResultsLifted = lifted.get(set);
				final Map<String, Long> projectResultsVB = vb.get(set);
				for (int i = 0; i <= projectResultsLifted.size(); i++) {
					stream.write("lifted,".getBytes());
				}
				for (int i = 0; i < projectResultsVB.size(); i++) {
					stream.write("vb,".getBytes());
				}
				stream.write("vb\n".getBytes());

				final String[] liftedKeys = projectResultsLifted.keySet().toArray(new String[0]);
				for (final String project : liftedKeys) {
					stream.write(project.getBytes());
					stream.write(',');
				}
				stream.write("avg,".getBytes());
				final String[] vbKeys = projectResultsVB.keySet().toArray(new String[0]);
				for (final String project : vbKeys) {
					stream.write(project.getBytes());
					stream.write(',');
				}
				stream.write("avg\n".getBytes());

				for (final String project : liftedKeys) {
					stream.write(projectResultsLifted.get(project).toString().getBytes());
					stream.write(',');
				}
				stream.write(Double.toString(projectResultsLifted.values().parallelStream().mapToLong(Long.class::cast)
						.average().getAsDouble()).getBytes());
				stream.write(',');
				for (final String project : vbKeys) {
					stream.write(projectResultsVB.get(project).toString().getBytes());
					stream.write(',');
				}
				stream.write(Double.toString(
						projectResultsVB.values().parallelStream().mapToLong(Long.class::cast).average().getAsDouble())
						.getBytes());
			}
		}
	}

	private static Map<RuleSet, Map<String, Long>> getCollectData(final File file) {
		final Map<RuleSet, Map<String, Long>> data = new HashMap<>();
		for (final File project : file.listFiles()) {
			for (final RuleSet ruleSet : RULES) {
				final Map<String, Long> projectMap = data.computeIfAbsent(ruleSet, m -> new HashMap<>());
				final File rule = new File(project, ruleSet.toString());
				if (rule.exists() && rule.isDirectory()) {
					final File log = new File(rule, "runtimes.log");
					if (log.canRead()) {
						try {
							final List<String> lines = Files.readAllLines(log.toPath());
							LongStream longStream = lines.parallelStream().mapToLong(Long::parseLong);
							if (!lines.isEmpty()) {
								if (lines.size() > 1) {
									longStream = longStream.sorted().skip((lines.size() / 2) - 1);
								}
								final long median = longStream.findFirst().getAsLong();
								projectMap.put(project.getName(), median);
							}
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return data;
	}
}
