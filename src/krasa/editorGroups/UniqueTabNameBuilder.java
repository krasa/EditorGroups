package krasa.editorGroups;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.UniqueNameBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import krasa.editorGroups.support.Utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UniqueTabNameBuilder {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(UniqueTabNameBuilder.class);
	private String root;

	public UniqueTabNameBuilder(Project project) {
		this.root = project.getBasePath();
		this.root = this.root == null ? "" : FileUtil.toSystemIndependentName(this.root);
	}

	public Map<String, String> getNamesByPath(List<String> paths, VirtualFile currentFile) {
		UniqueNameBuilder<String> uniqueNameBuilder = new UniqueNameBuilder<>(root, "/", 25);
		Map<String, String> path_name = new THashMap<>();
		Map<String, String> name_path = new THashMap<>();
		Set<String> paths_withDuplicateName = new THashSet<>();

		for (String path: paths) {
			put(path_name, name_path, paths_withDuplicateName, path);
		}

		if (currentFile != null) {
			String currentFilePath = currentFile.getCanonicalPath();
			if (!path_name.containsKey(currentFilePath)) {
				put(path_name, name_path, paths_withDuplicateName, currentFilePath);
			}
		}

		for (String path: paths_withDuplicateName) {
			uniqueNameBuilder.addPath(path, path);
		}

		for (String path: paths_withDuplicateName) {
			String uniqueName = uniqueNameBuilder.getShortPath(path);
			path_name.put(path, uniqueName);
		}
		return path_name;
	}

	private void put(Map<String, String> path_name, Map<String, String> name_path, Set<String> paths_withDuplicateName, String currentFilePath) {
		String name = Utils.toPresentableName(currentFilePath);

		String duplicatePath = name_path.get(name);
		if (duplicatePath != null) {
			paths_withDuplicateName.add(duplicatePath);
			paths_withDuplicateName.add(currentFilePath);
		}
		path_name.put(currentFilePath, name);
		name_path.put(name, currentFilePath);
	}
}
