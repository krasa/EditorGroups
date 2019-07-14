package krasa.editorGroups.support;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.model.Link;
import krasa.editorGroups.model.RegexGroup;
import krasa.editorGroups.model.RegexGroupModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;


public class RegexFileResolver {
	private static final Logger LOG = Logger.getInstance(RegexFileResolver.class);
	private final Project project;
	protected Set<VirtualFile> links = new HashSet<>();
	protected ApplicationConfiguration config;
	                      
	public RegexFileResolver(Project project) {
		this.project = project;
		config = ApplicationConfiguration.state();
	}

	public List<Link> resolveRegexGroupLinks(@NotNull RegexGroup regexGroup, @Nullable VirtualFile currentFile) {
		LOG.debug(">resolveRegexGroupLinks");
		long start = System.currentTimeMillis();
		RegexGroupModel regexGroupModel = regexGroup.getRegexGroupModel();
		Matcher referenceMatcher = regexGroup.getReferenceMatcher();

		if (currentFile != null) {
			//always include it in case there are to many matches
			links.add(currentFile);
		}

		Matcher groupMatcher = regexGroupModel.getRegexPattern().matcher("");
		ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
		List<VirtualFile> folders = regexGroup.getScopes(project);
		try {
			for (VirtualFile dir : folders) {
				processFolders2(regexGroup, regexGroupModel, referenceMatcher, groupMatcher, projectFileIndex, dir);
			}
		} catch (TooManyFilesException e) {
			e.showNotification();
			LOG.warn("Found too many matching files, aborting. size=" + links.size() + " " + regexGroup);
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.valueOf(links));
			}
		}


		long duration = System.currentTimeMillis() - start;
		if (duration > 500) {
			LOG.warn("<resolveRegexGroup " + duration + "ms " + regexGroup + "; links=" + links);
		} else if (LOG.isDebugEnabled()) {
			LOG.debug("<resolveRegexGroup " + duration + "ms links=" + links);
		}

		return Link.fromVirtualFiles(links);
	}

	private void processFolders2(@NotNull RegexGroup regexGroup, RegexGroupModel regexGroupModel, @Nullable Matcher referenceMatcher, Matcher groupMatcher, ProjectFileIndex projectFileIndex, VirtualFile folder) {
		VfsUtilCore.visitChildrenRecursively(folder, new VirtualFileVisitor<Object>() {
			@NotNull
			@Override
			public Result visitFileEx(@NotNull VirtualFile child) {
				if (child.isDirectory()) {
					ProgressManager.checkCanceled();
					if (regexGroupModel.getScope() == RegexGroupModel.Scope.CURRENT_FOLDER) {
						if (child.equals(regexGroup.getFolder())) {
							//ok
						} else {
							return SKIP_CHILDREN;
						}
					} else {
						if (projectFileIndex.isExcluded(child)) {
							return SKIP_CHILDREN;
						}
					}
				} else {
					Matcher matcher = groupMatcher.reset(child.getName());
					if (matches(referenceMatcher, matcher)) {
						links.add(child);
						if (links.size() > config.getGroupSizeLimitInt()) {
							throw new TooManyFilesException(links.size());
						}
					}
				}
				return CONTINUE;
			}
		});


	}

	private boolean matches(@Nullable Matcher referenceMatcher, Matcher matcher) {
		if (!matcher.matches()) {
			return false;
		}
		if (referenceMatcher != null) {
			for (int j = 1; j <= matcher.groupCount(); j++) {
				String refGroup = referenceMatcher.group(j);
				String group = matcher.group(j);
				if (!refGroup.equals(group)) {
					return false;
				}
			}
		}
		return true;
	}

}
