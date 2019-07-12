package krasa.editorGroups.support;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
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
import krasa.editorGroups.model.Link;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.substringBeforeLast;


public class FileResolver {
	private static final Logger LOG = Logger.getInstance(FileResolver.class);

	protected final Logger log = Logger.getInstance(this.getClass());

	protected final Project project;
	protected final boolean excludeEditorGroupsFiles;
	protected final Set<String> links;
	protected ApplicationConfiguration config;


	@NotNull
	public static List<Link> resolveLinks(@NotNull EditorGroupIndexValue group, @NotNull Project project) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(">resolveLinks [" + group + "], project = [" + project + "]");
		}

		return resolveLinks(project, group.getOwnerPath(), group.getRoot(), group.getRelatedPaths(), group);
	}

	@NotNull
	public static List<Link> resolveLinks(@NotNull Project project, @Nullable String ownerFilePath, String root, List<String> relatedPaths, EditorGroupIndexValue group) {
		return new FileResolver(project).resolve(ownerFilePath, root, relatedPaths, group);
	}

	public FileResolver(Project project) {
		this.project = project;
		excludeEditorGroupsFiles = ApplicationConfiguration.state().isExcludeEditorGroupsFiles();
		links = new LinkedHashSet<String>() {
			@Override
			public boolean add(String o) {
				return super.add(sanitize(o));
			}
		};
		config = ApplicationConfiguration.state();
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
		config = ApplicationConfiguration.state();
	}


	public Set<String> getLinks() {
		return links;
	}

	@NotNull
	private List<Link> resolve(@Nullable String ownerFilePath, String root, List<String> relatedPaths, EditorGroupIndexValue group) {
		try {
			return resolve2(ownerFilePath, root, relatedPaths, group);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	private List<Link> resolve2(@Nullable String ownerFilePath, String root, List<String> relatedPaths, EditorGroupIndexValue group) throws IOException {
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

			} catch (TooManyFilesException e) {
				//todo  notification?
				log.warn("TooManyFilesException filePath='" + filePath + " rootFolder=" + rootFolder + ", group = [" + group + "]");
				log.debug(e);
			} catch (ProcessCanceledException | IndexNotReadyException e) {
				//TODO what to do?
				log.warn("filePath='" + filePath + " rootFolder=" + rootFolder + ", group = [" + group + "]", new RuntimeException(e));
			} catch (Exception e) {
				log.error("filePath='" + filePath + " rootFolder=" + rootFolder + ", group = [" + group + "]", e);
			}
			long delta = System.currentTimeMillis() - t0;
			if (delta > 100) {
				if (log.isDebugEnabled()) log.debug("resolveLink " + filePath + " " + delta + "ms");
			}
		}
		if (log.isDebugEnabled())
			log.debug("<resolveLinks " + (System.currentTimeMillis() - start) + "ms links=" + links);

		return Link.from(links);
	}

	private String resolveRootFolder(@Nullable String ownerFilePath, String root, EditorGroupIndexValue group, VirtualFile ownerFile) throws IOException {
		if (ownerFilePath != null && root.startsWith("..")) {
			File file = new File(new File(ownerFilePath).getParentFile(), root);
			if (log.isDebugEnabled()) {
				log.debug("root " + file + "  exists=" + file.exists());
			}
			root = Utils.getCanonicalPath(file);
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

			String canonicalPath = file.getPath();
			if (canonicalPath != null) {
				if (substringBeforeLast(file.getPath(), ".").endsWith(sanitizedPath)) {
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

			String canonicalPath = file.getPath();
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

	public static boolean excluded(File file, boolean excludeEditorGroupsFiles) {
		if (excludeEditorGroupsFiles && EditorGroupsLanguage.isEditorGroupsLanguage(Utils.getCanonicalPath(file))) {
			return true;
		}
		if (FileUtil.isJarOrZip(file)) {
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
		//could be some shit like '/*', do not use canonical path
		String path = sanitize(file.getAbsolutePath());
		String fileName = StringUtils.substringAfterLast(path, "/");

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
		if (links.size() > config.getGroupSizeLimitInt()) {
			throw new TooManyFilesException();
		}
		if (file.isFile() && !(!definedManually && excluded(file, excludeEditorGroupsFiles))) {
			Path path = Paths.get(file.toURI());
			links.add(Utils.getCanonicalPath(file));
		}
	}

	protected String useMacros(VirtualFile virtualFile, String folder) {
		if (folder.startsWith("PROJECT")) {
			VirtualFile baseDir = project.getBaseDir();
			String canonicalPath = baseDir.getPath();
			folder = folder.replaceAll("^PROJECT", canonicalPath);
		} else if (virtualFile != null && folder.startsWith("MODULE")) {
			Module moduleForFile = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
			String moduleDirPath = ModuleUtilCore.getModuleDirPath(moduleForFile);
			folder = folder.replaceAll("^MODULE", moduleDirPath);
		}
		return folder;
	}


	private class TooManyFilesException extends RuntimeException {
	}
}
