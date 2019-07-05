package krasa.editorGroups;

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
import krasa.editorGroups.support.FileResolver;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

	public AutoGroupProvider(@NotNull Project project) {
		this.project = project;
	}

	public EditorGroup findFirstMatchingRegexGroup(VirtualFile file) {
		if (LOG.isDebugEnabled())
			LOG.debug("findFirstMatchingRegexGroup: " + file);


		long start = System.currentTimeMillis();
		String fileName = file.getName();
		RegexGroupModel matching = ApplicationConfiguration.state().getRegExpGroupModels().findFirstMatching(fileName);
		if (LOG.isDebugEnabled()) LOG.debug("findMatchingRegexGroups: " + (System.currentTimeMillis() - start) + "ms");

		if (matching == null) {
			return EditorGroup.EMPTY;
		}

		return new RegexGroup(matching, file.getParent().getPath(), Collections.emptyList(), fileName);
	}

	public List<RegexGroup> findMatchingRegexGroups(VirtualFile file) {
		if (LOG.isDebugEnabled())
			LOG.debug("findMatchingRegexGroups: " + file);


		long start = System.currentTimeMillis();
		String fileName = file.getName();
		List<RegexGroupModel> matching = ApplicationConfiguration.state().getRegExpGroupModels().findMatching(fileName);
		if (LOG.isDebugEnabled()) LOG.debug("findMatchingRegexGroups: " + (System.currentTimeMillis() - start) + "ms");


		return toRegexGroups(file, fileName, matching);
	}

	@NotNull
	public List<RegexGroup> toRegexGroups(VirtualFile file, String fileName, List<RegexGroupModel> matching) {
		List<RegexGroup> result = new ArrayList<>(matching.size());
		for (RegexGroupModel regexGroupModel : matching) {
			result.add(new RegexGroup(regexGroupModel, file.getParent().getPath(), Collections.emptyList(), fileName));
		}
		return result;
	}

	public RegexGroup getRegexGroup(RegexGroup result, Project project, VirtualFile currentFile) {
		List<Link> links = new FileResolver(project).resolveRegexGroupLinks(result, currentFile);
		if (links.isEmpty()) {
			LOG.error("should contain the current file at least: " + result);
		}
		return new RegexGroup(result.getRegexGroupModel(), result.getFolderPath(), links, result.getFileName());
	}

	public EditorGroup findRegexGroup(String filePath, String substring) {
		RegExpGroupModels regExpGroupModels = ApplicationConfiguration.state().getRegExpGroupModels();
		RegexGroupModel regexGroupModel = regExpGroupModels.find(substring);
		if (regexGroupModel == null) {
			return EditorGroup.EMPTY;
		}

		File file = new File(filePath);
		return new RegexGroup(regexGroupModel, Utils.getCanonicalPath(file.getParentFile()), Collections.emptyList(), file.getName());
	}

	public EditorGroup getFolderGroup(VirtualFile file) {
		if (!file.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}

		VirtualFile parent = file.getParent();
		String folder = parent.getPath();
		List<Link> links = FileResolver.resolveLinks(project, null, folder, Collections.singletonList("./"), null);
		return new FolderGroup(folder, links);
	}


	public EditorGroup getSameNameGroup(VirtualFile currentFile) {
		if (!currentFile.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}
		String nameWithoutExtension = currentFile.getNameWithoutExtension();
		long start = System.currentTimeMillis();

		Collection<VirtualFile> virtualFilesByName = null;
		List<String> paths;
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
				paths.add(file.getPath());
			}

			if (!paths.contains(currentFile.getPath())) {
				paths.add(0, currentFile.getPath());
			}
			Collections.sort(paths);
		} catch (IndexNotReadyException | ProcessCanceledException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(e);
			}
			paths = new ArrayList<>();
			paths.add(currentFile.getPath());
			paths.add("Indexing...");
		}

		long t0 = System.currentTimeMillis() - start;
		if (t0 > IndexCache.LINKS_LIMIT) {
			LOG.warn("getSameNameGroup took " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());
		}
		if (LOG.isDebugEnabled())
			LOG.debug("getSameNameGroup " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());

		return new SameNameGroup(nameWithoutExtension, Link.from(paths));
	}


}
