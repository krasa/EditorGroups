package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import krasa.editorGroups.index.MyFileNameIndexService;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FolderGroup;
import krasa.editorGroups.model.SameNameGroup;
import krasa.editorGroups.support.FileResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AutoGroupProvider {
	private static final Logger LOG = Logger.getInstance(AutoGroupProvider.class);

	public static AutoGroupProvider getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, AutoGroupProvider.class);
	}

	@NotNull
	private Project project;
	private FileResolver fileResolver;

	public AutoGroupProvider(@NotNull Project project) {
		this.project = project;
		fileResolver = new FileResolver();
	}


	public EditorGroup getFolderGroup(VirtualFile file) {
		if (!file.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}

		VirtualFile parent = file.getParent();
		String folder = parent.getCanonicalPath();
		List<String> links = fileResolver.resolveLinks(project, null, folder, Collections.singletonList("./"));
		return new FolderGroup(folder, links);
	}


	public EditorGroup getSameNameGroup(VirtualFile currentFile) {
		if (!currentFile.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}
		String nameWithoutExtension = currentFile.getNameWithoutExtension();
		long start = System.currentTimeMillis();

		Collection<VirtualFile> virtualFilesByName = MyFileNameIndexService.getVirtualFilesByName(project, nameWithoutExtension, true, GlobalSearchScope.projectScope(project));


		boolean containsCurrent = virtualFilesByName.contains(currentFile);
		int size = virtualFilesByName.size();
		List<String> paths = new ArrayList<>(Math.max(containsCurrent ? size : size + 1, IndexCache.LIMIT_SAME_NAME));

		if (!containsCurrent) {
			paths.add(currentFile.getCanonicalPath());
		}
		for (VirtualFile file : virtualFilesByName) {
			if (ProjectCoreUtil.isProjectOrWorkspaceFile(file)) {
				continue;
			}
			if (file.isDirectory()) {
				continue;
			}
			if (paths.size() == IndexCache.LIMIT_SAME_NAME) {
				LOG.warn("#getSameNameGroup: too many results for " + nameWithoutExtension + " =" + size);
				break;
			}
			paths.add(file.getCanonicalPath());
		}

		Collections.sort(paths);

		long t0 = System.currentTimeMillis() - start;
		if (t0 > IndexCache.LINKS_LIMIT) {
			LOG.warn("getSameNameGroup took " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());
		}
		if (LOG.isDebugEnabled())
			LOG.debug("getSameNameGroup " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());

		return new SameNameGroup(nameWithoutExtension, paths);
	}


}
