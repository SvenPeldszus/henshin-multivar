package org.eclipse.emf.henshin.variability.multi.eval.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;

public class LoadingHelper {

	public static enum RuleSet {
		CREATE, DELETE, MOVE, ALL, NOFILTER
	}

	public static List<String> getRuleLocations(String filePath, String filePathRules, int nestingLevel, RuleSet set) {
		Path folder = Paths.get(filePath + "/" + filePathRules);
		Stream<Path> paths;
		try {
			paths = Files.walk(folder).filter(Files::isRegularFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		Stream<String> result = paths.map(path -> {
			if (nestingLevel == 2) {
				return filePathRules + "/" + path.getParent().getParent().getFileName() + "/"
						+ path.getParent().getFileName() + "/" + path.getFileName();
			}
			if (nestingLevel == 1) {
				return filePathRules + "/" + path.getParent().getFileName() + "/" + path.getFileName();
			}
			if (nestingLevel == 0) {
				return filePathRules + "/" + path.getFileName();
			}
			throw new RuntimeException();
		}).filter(s -> s.endsWith(".henshin"));

		switch (set) {
		case CREATE:
			return result.filter(s -> isCREATE(s)).collect(Collectors.toList());
		case DELETE:
			return result.filter(s -> isDELETE(s)).collect(Collectors.toList());
		case MOVE:
			return result.filter(s -> isMOVE(s)).collect(Collectors.toList());
		case ALL:
			return result.filter(s -> isCREATE(s) && isMOVE(s) && isMOVE(s)).collect(Collectors.toList());
		case NOFILTER:
			return result.collect(Collectors.toList());
		}
		return null;

	}

	public static boolean isMOVE(String s) {
		return s.contains("_MOVE_") || s.contains("rrmove") || s.contains("_CHANGE_") || s.contains("rrchange")
				|| s.contains("-var");
	}

	public static boolean isDELETE(String s) {
		return s.contains("_DELETE_") || s.contains("rrdelete") || s.contains("_REMOVE_") || s.contains("rrremove")
				|| s.contains("_UNSET_") || s.contains("rrunset") || s.contains("-var");
	}

	public static boolean isCREATE(String s) {
		return s.contains("_CREATE_") || s.contains("rrcreate") || s.contains("_ADD_") || s.contains("rradd")
				|| s.contains("_SET_") || s.contains("rrset") || s.contains("-var");
	}

	public static List<String> getModelLocations(String filePath, String filePathInstances, String fileNameInstance) {
		List<String> result = new ArrayList<String>();
		try {
			Files.walk(Paths.get(filePath + "/" + filePathInstances)).filter(Files::isRegularFile)
					.filter(f -> f.getFileName().toString().equals(fileNameInstance)).forEach(f -> result
							.add(f.getParent().getParent().getFileName() + "/" + f.getParent().getFileName()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static List<String> getModelLocations(String filePath, String filePathInstances,
			String filePathInstancesCore, String fileNameInstance) {
		List<String> result = new ArrayList<String>();
		try {
			Files.walk(Paths.get(filePath + "/" + filePathInstances)).filter(Files::isRegularFile)
					.filter(f -> f.getFileName().toString().equals(fileNameInstance))
					.filter(f -> !f.getParent().toString().equals(filePathInstancesCore))
					.forEach(f -> result.add(filePathInstances + "/" + f.getParent().getParent().getFileName() + "/"
							+ f.getParent().getFileName()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static Module loadAllRulesAsOneModule(HenshinResourceSet rs, String filePath, String filePathRules,
			int depth, RuleSet set) {
		Module module = null;
		for (String location : getRuleLocations(filePath, filePathRules, depth, set)) {
			if (module == null) {
				module = rs.getModule(location, false);
			} else {
				Module mod = rs.getModule(location, false);
				module.getUnits().addAll(mod.getUnits());
			}
		}
		if(module == null) {
			throw new IllegalStateException("No rule loaded!");
		}
		
		module.getUnits()
				.retainAll(module.getUnits().stream().filter(u -> u instanceof Rule).collect(Collectors.toSet()));
		RuleSetNormalizer.prepareRules(module);
		return module;
	}

	public static Module loadAllRulesAsOneModule(HenshinResourceSet rs, String filePath, String filePathRules,
			RuleSet set) {
		return loadAllRulesAsOneModule(rs, filePath, filePathRules, 2, set);
	}
}
