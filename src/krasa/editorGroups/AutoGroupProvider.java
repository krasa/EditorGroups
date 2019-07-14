package krasa.editorGroups;

import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import krasa.editorGroups.index.MyFileNameIndexService;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.RegexFileResolver;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AutoGroupProvider {
	private static final Logger LOG = Logger.getInstance(AutoGroupProvider.class);

	public static AutoGroupProvider getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, AutoGroupProvider.class);
	}

	@NotNull
	private Project project;

	public AutoGroupProvider(@NotNull Project project) {
		this.project = project;
	}


	public EditorGroup getFolderGroup(VirtualFile file) {
//		if (!file.isInLocalFileSystem()) {
//			return EditorGroup.EMPTY;
//		}

		VirtualFile parent = file.getParent();
		RegexGroup regexGroup = new RegexGroup(new RegexGroupModel(".*", RegexGroupModel.Scope.CURRENT_FOLDER), parent);
		List<Link> links = new RegexFileResolver(project).resolveRegexGroupLinks(regexGroup, file);
		return new FolderGroup(parent, links);
	}


	public EditorGroup getSameNameGroup(VirtualFile currentFile) {
		if (!currentFile.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}
		String nameWithoutExtension = currentFile.getNameWithoutExtension();
		long start = System.currentTimeMillis();

		Collection<VirtualFile> virtualFilesByName = null;
		List<VirtualFile> paths;
		try {
			virtualFilesByName = MyFileNameIndexService.getVirtualFilesByName(project, nameWithoutExtension, true, GlobalSearchScope.projectScope(project));

			if (LOG.isDebugEnabled()) {
				LOG.debug("getVirtualFilesByName=" + virtualFilesByName);
			}

			int size = virtualFilesByName.size();
			paths = new ArrayList<>(Math.max(size + 1, IndexCache.LIMIT_SAME_NAME));


			for (VirtualFile file : virtualFilesByName) {
				if (ProjectCoreUtil.isProjectOrWorkspaceFile(file)) {
					continue;
				}
				if (Utils.isJarOrZip(file)) {
					continue;
				}
				if (file.isDirectory()) {
					continue;
				}
				if (paths.size() == IndexCache.LIMIT_SAME_NAME) {
					LOG.warn("#getSameNameGroup: too many results for " + nameWithoutExtension + " =" + size);
					break;
				}
				paths.add(file);
			}

			if (!paths.contains(currentFile)) {
				paths.add(0, currentFile);
			}
			paths.sort(VirtualFileComparator.getInstance());
		} catch (IndexNotReadyException | ProcessCanceledException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(e);
			}
			paths = new ArrayList<>();
			paths.add(currentFile);
			paths.add(new MockVirtualFile("Indexing..."));
		}

		long t0 = System.currentTimeMillis() - start;
		if (t0 > IndexCache.LINKS_LIMIT) {
			LOG.warn("getSameNameGroup took " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());
		}
		if (LOG.isDebugEnabled())
			LOG.debug("getSameNameGroup " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());

		return new SameNameGroup(nameWithoutExtension, Link.fromVirtualFiles(paths));
	}


}
