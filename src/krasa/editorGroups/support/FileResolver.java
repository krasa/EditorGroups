package krasa.editorGroups.support;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.index.MyFileNameIndexService;
import krasa.editorGroups.language.EditorGroupsLanguage;
import krasa.editorGroups.model.EditorGroupIndexValue;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.substringBeforeLast;


public class FileResolver {
	protected static final Logger LOG = Logger.getInstance(FileResolver.class);

	private final Project project;
	private final boolean excludeEditorGroupsFiles;
	private final Set<String> links;


	@NotNull
	public static List<String> resolveLinks(@NotNull EditorGroupIndexValue group, @NotNull Project project) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(">resolveLinks [" + group + "], project = [" + project + "]");
		}

		return resolveLinks(project, group.getOwnerPath(), group.getRoot(), group.getRelatedPaths(), group);
	}

	@NotNull
	public static List<String> resolveLinks(@NotNull Project project, @Nullable String ownerFilePath, String root, List<String> relatedPaths, EditorGroupIndexValue group) {
		return new FileResolver(project).resolve(ownerFilePath, root, relatedPaths, group);
	}

	protected FileResolver(Project project) {
		this.project = project;
		excludeEditorGroupsFiles = ApplicationConfiguration.state().isExcludeEditorGroupsFiles();
		links = new LinkedHashSet<String>() {
			@Override
			public boolean add(String o) {
				return super.add(sanitize(o));
			}
		};
	}

	protected FileResolver() {
		this.project = null;
		excludeEditorGroupsFiles = false;
		links = new LinkedHashSet<String>() {
			@Override
			public boolean add(String o) {
				return super.add(sanitize(o));
			}
		};
	}

	public Set<String> getLinks() {
		return links;
	}

	@NotNull
	private List<String> resolve(@Nullable String ownerFilePath, String root, List<String> relatedPaths, EditorGroupIndexValue group) {
		try {
			return resolve2(ownerFilePath, root, relatedPaths, group);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	private List<String> resolve2(@Nullable String ownerFilePath, String root, List<String> relatedPaths, EditorGroupIndexValue group) throws IOException {
		long start = System.currentTimeMillis();


		VirtualFile ownerFile = Utils.getNullableFileByPath(ownerFilePath);

		String rootFolder = resolveRootFolder(ownerFilePath, root, group, ownerFile);


		if (ownerFilePath != null) {
			add(new File(ownerFilePath), false);
		}


		for (String filePath : relatedPaths) {
			long t0 = System.currentTimeMillis();
			try {
				filePath = useMacros(ownerFile, filePath);
				filePath = sanitize(filePath);


				if (filePath.startsWith("*/") && filePath.endsWith(".*")) {
					resolveSameNameProjectFiles(filePath);
				} else if (filePath.startsWith("*/")) {
					resolveProjectFiles(filePath);
				} else if (FileUtil.isAbsolute(filePath)) {
					File file = new File(filePath);
					resolve(file);
				} else {
					File file = new File(rootFolder, filePath);
					resolve(file);
				}

			} catch (Exception e) {
				LOG.error("filePath='" + filePath + " rootFolder=" + rootFolder + ", group = [" + group + "]", e);
			}
			long delta = System.currentTimeMillis() - t0;
			if (delta > 100) {
				if (LOG.isDebugEnabled()) LOG.debug("resolveLink " + filePath + " " + delta + "ms");
			}
		}
		if (LOG.isDebugEnabled())
			LOG.debug("<resolveLinks " + (System.currentTimeMillis() - start) + "ms links=" + links);

		return new ArrayList<>(links);
	}

	private String resolveRootFolder(@Nullable String ownerFilePath, String root, EditorGroupIndexValue group, VirtualFile ownerFile) throws IOException {
		if (ownerFilePath != null && root.startsWith("..")) {
			File file = new File(new File(ownerFilePath).getParentFile(), root);
			if (LOG.isDebugEnabled()) {
				LOG.debug("root " + file + "  exists=" + file.exists());
			}
			root = file.getCanonicalPath();
		}

		root = useMacros(ownerFile, root);


		File rootFile = new File(root);
		if (!rootFile.exists()) {
			Notifications.warning("Root does not exist [" + root + "] in " + Notifications.href(ownerFile) + "<br\\>" + group, new NotificationListener.Adapter() {
				@Override
				protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
					OpenFileAction.openFile(ownerFile, project);
					notification.expire();
				}
			});
		}
		return rootFile.isFile() ? rootFile.getParent() : root;
	}

	private void resolveSameNameProjectFiles(String filePath) throws IOException {
		String sanitizedPath = filePath.substring("*/".length());
		sanitizedPath = StringUtils.substringBefore(sanitizedPath, ".*");
		String fileName = sanitizedPath;
		if (fileName.contains("/")) {
			fileName = StringUtils.substringAfterLast(fileName, "/");
		}


		Collection<VirtualFile> virtualFilesByName = MyFileNameIndexService.getVirtualFilesByName(project, fileName, !SystemInfo.isWindows, GlobalSearchScope.allScope(project));
		for (VirtualFile file : virtualFilesByName) {

			String canonicalPath = file.getCanonicalPath();
			if (canonicalPath != null) {
				if (substringBeforeLast(file.getCanonicalPath(), ".").endsWith(sanitizedPath)) {
					add(canonicalPath);
				}
			}
		}
	}

	protected void resolveProjectFiles(String filePath) throws IOException {
		String sanitizedPath = filePath.substring("*/".length());
		String fileName = sanitizedPath;
		if (fileName.contains("/")) {
			fileName = StringUtils.substringAfterLast(fileName, "/");
		}


		Collection<VirtualFile> virtualFilesByName = FilenameIndex.getVirtualFilesByName(project, fileName, !SystemInfo.isWindows, GlobalSearchScope.allScope(project));
		for (VirtualFile file : virtualFilesByName) {

			String canonicalPath = file.getCanonicalPath();
			if (canonicalPath != null) {
				if (canonicalPath.endsWith(sanitizedPath)) {
					add(canonicalPath);
				}
			}
		}
	}

	protected static String sanitize(String filePath) {
		String replace = filePath.replace("\\", "/");
		//file path starting with // causes major delays for some reason
		replace = replace.replaceFirst("/+", "/");
		return replace;
	}

	protected void resolve(File file) throws IOException {
		if (file.isFile()) {
			add(file, true);
		} else if (file.isDirectory()) {
			addChilds(file);
		} else {
			addMatching(file);
		}
	}

	private boolean excluded(File file) throws IOException {
		if (excludeEditorGroupsFiles && EditorGroupsLanguage.isEditorGroupsLanguage(file.getCanonicalPath())) {
			return true;
		}
		return false;
	}

	protected void addChilds(File parentDir) throws IOException {
		File[] foundFiles = parentDir.listFiles((FileFilter) FileFileFilter.FILE);
		for (File foundFile : foundFiles) {
			add(foundFile, false);
		}
	}

	protected void addMatching(File file) throws IOException {
		File parentDir = file.getParentFile();
		String canonicalPath = sanitize(file.getAbsolutePath());
		String fileName = StringUtils.substringAfterLast(canonicalPath, "/");

		if (fileName.length() > 0 && !file.isDirectory() && parentDir.isDirectory()) {
			FileFilter filter = new WildcardFileFilter(fileName, IOCase.SYSTEM);
			File[] foundFiles = parentDir.listFiles(filter);
			for (File f : foundFiles) {
				if (f.isFile()) {
					add(f, false);
				}
			}

			if (foundFiles.length == 0) {
				foundFiles = parentDir.listFiles((FilenameFilter) new PrefixFileFilter(fileName + ".", IOCase.SYSTEM));
				for (File f : foundFiles) {
					add(f, false);
				}
			}
		}
	}

	protected void add(String canonicalPath) throws IOException {
		add(new File(canonicalPath), false);
	}

	protected void add(File file, boolean definedManually) throws IOException {
		if (file.isFile() && !(!definedManually && excluded(file))) {
			links.add(file.getCanonicalPath());
		}
	}

	protected String useMacros(VirtualFile virtualFile, String folder) {
		if (folder.startsWith("PROJECT")) {
			VirtualFile baseDir = project.getBaseDir();
			String canonicalPath = baseDir.getCanonicalPath();
			folder = folder.replaceAll("^PROJECT", canonicalPath);
		} else if (virtualFile != null && folder.startsWith("MODULE")) {
			Module moduleForFile = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
			String moduleDirPath = ModuleUtilCore.getModuleDirPath(moduleForFile);
			folder = folder.replaceAll("^MODULE", moduleDirPath);
		}
		return folder;
	}
}
