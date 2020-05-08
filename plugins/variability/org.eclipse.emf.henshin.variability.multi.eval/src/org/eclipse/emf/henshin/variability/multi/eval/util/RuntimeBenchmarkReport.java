package org.eclipse.emf.henshin.variability.multi.eval.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;

public class RuntimeBenchmarkReport {
	private int count = 0;
	String name = "";
	File logfilePath;
	File runtimefilePath;
	PrintWriter out;
	String date;

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public RuntimeBenchmarkReport(String name, String logDirectory) {
		File file = new File(logDirectory);
		if(!file.exists()){
			file.mkdirs();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		do {
			this.date = sdf.format(new Date());
			logfilePath = new File(logDirectory, date + ".log");
		}
		while(logfilePath.exists());
		runtimefilePath = new File(logDirectory, "/runtimes.log");
	}

	private boolean PRINT_TO_CONSOLE = false;

	public void start() {
		createReport();
		String start = " Starting runtime benchmark: " + name + " ";
		StringBuilder info = new StringBuilder();
		for (int i = 0; i < start.length(); i++)
			info.append('=');
		info.append('\n');
		info.append(start);
		info.append('\n');
		for (int i = 0; i < start.length(); i++)
			info.append('=');
		info.append('\n');

		addToReport(info);
	}

	private void createReport() {
		try {
			Files.write(logfilePath.toPath(), new byte[0], StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void beginNewEntry(String id) {
		count++;
		StringBuilder info = new StringBuilder();
		info.append('\n');
		info.append("Benchmark run " + count + ": input model " + id + " \n");
		addToReport(info);
	}

	public void addSubEntry(Unit unit, int before, int after, long runtime) {
		StringBuilder info = new StringBuilder();
		String unitKind = (unit instanceof Rule) ? "rule" : "unit";
		info.append("   Considered " + unitKind + " " + unit.getName() + " in " + sec(runtime) + " sec, ");
		info.append("change: " + before + " -> " + after + " nodes (delta = " + (after - before) + ")\n");
		addToReport(info);
	}

	public void finishEntry(int before, int after, long runtime, List<Rule> detectedRules) {
		StringBuilder info = new StringBuilder();
		info.append("Execution time: " + sec(runtime) + " sec\n");
		info.append("Performed change: " + before + " -> " + after + " nodes (delta = " + (after - before) + ")\n");
		info.append("\n\nDetected rules (" + detectedRules.size() + "):\n");
		for (Rule rule : detectedRules) {
			info.append("   " + rule.getName() + "\n");
		}
		System.out.println(info.toString());
		addToReport(info);
		addToRuntimelog(runtime);
	}

	private void addToRuntimelog(long runtime) {
		Path path = runtimefilePath.toPath();
		
		try {
			StandardOpenOption[] append = new StandardOpenOption[0];
			if (path.toFile().exists()) {
				append = new StandardOpenOption[] { StandardOpenOption.APPEND };
			} else {
				path.getParent().toFile().mkdirs();
			}
			Files.write(path, (runtime+"\n").getBytes(), append);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addToReport(StringBuilder info) {
		if (PRINT_TO_CONSOLE)
			System.out.print(info);

		try {
			StandardOpenOption[] append = new StandardOpenOption[0];
			if (logfilePath.exists()) {
				append = new StandardOpenOption[] { StandardOpenOption.APPEND };
			} else {
				logfilePath.getParentFile().mkdirs();
			}

			Files.write(logfilePath.toPath(), info.toString().getBytes(), append);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static double sec(long msecTime) {
		return (msecTime * 1.0) / 1000;
	}

	public void addIncorrectnessEntry() {
		StringBuilder info = new StringBuilder();
		info.append("! Found potential error: The output model is not equal to the reference output model.");
	}

}
