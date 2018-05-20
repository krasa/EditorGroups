package krasa.editorGroups.support;

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
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class FileResolver {
	protected static final Logger LOG = Logger.getInstance(FileResolver.class);

	@NotNull
	List<String> resolveLinks(Project project, String ownerPath, List<String> relatedPaths) {
		long start = System.currentTimeMillis();
		File file1 = new File(ownerPath);
		String folder = file1.isFile() ? file1.getParent() : ownerPath;
		
		VirtualFile virtualFile = Utils.getFileByPath(ownerPath);

		Set<String> links = new LinkedHashSet<String>() {
			@Override
			public boolean add(String o) {
				return super.add(sanitize(o));
			}
		};


		try {
			add(links, ownerPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (String filePath : relatedPaths) {
			try {
				filePath = useMacros(project, virtualFile, filePath);
				filePath = sanitize(filePath);


				if (filePath.startsWith("*/")) {
					resolveProjectFiles(project, links, filePath);
				} else if (FileUtil.isAbsolute(filePath)) {
					File file = new File(filePath);
					resolve(links, file);
				} else {
					File file = new File(folder, filePath);
					resolve(links, file);
				}

			} catch (Exception e) {
				LOG.warn("filePath='" + filePath + "'; owner=" + ownerPath, e);
			}
		}
		System.err.println("resolveLinks " + (System.currentTimeMillis() - start) + "ms ownerPath=" + ownerPath);
		return new ArrayList<>(links);
	}

	protected static void resolveProjectFiles(Project project, Set<String> links, String filePath) throws IOException {
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
					add(links, canonicalPath);
				}
			}
		}
	}

	protected static String sanitize(String filePath) {
		return filePath.replace("\\", "/");
	}

	protected static void resolve(Set<String> links, File file) throws IOException {
		if (file.isFile()) {
			add(links, file);
		} else if (file.isDirectory()) {
			addChilds(links, file);
		} else {
			addMatching(links, file);
		}
	}

	protected static void addChilds(Set<String> links, File parentDir) throws IOException {
		File[] foundFiles = parentDir.listFiles((FileFilter) FileFileFilter.FILE);
		for (File foundFile : foundFiles) {
			add(links, foundFile);
		}
	}

	protected static void addMatching(Set<String> links, File file) throws IOException {
		File parentDir = file.getParentFile();
		String canonicalPath = sanitize(file.getAbsolutePath());
		String fileName = StringUtils.substringAfterLast(canonicalPath, "/");

		if (fileName.length() > 0 && !file.isDirectory() && parentDir.isDirectory()) {
			FileFilter filter = new WildcardFileFilter(fileName, IOCase.SYSTEM);
			File[] foundFiles = parentDir.listFiles(filter);
			for (File f : foundFiles) {
				if (f.isFile()) {
					add(links, f);
				}
			}

			if (foundFiles.length == 0) {
				foundFiles = parentDir.listFiles((FilenameFilter) new PrefixFileFilter(fileName + ".", IOCase.SYSTEM));
				for (File f : foundFiles) {
					add(links, f);
				}
			}
		}
	}

	protected static void add(Set<String> links, File file) throws IOException {
		if (file.isFile()) {
			links.add(file.getCanonicalPath());
		}
	}

	protected static void add(Set<String> links, String canonicalPath) throws IOException {
		add(links, new File(canonicalPath));
	}

	protected static String useMacros(Project project, VirtualFile virtualFile, String folder) {
		if (folder.startsWith("PROJECT")) {
			VirtualFile baseDir = project.getBaseDir();
			String canonicalPath = baseDir.getCanonicalPath();
			folder = folder.replaceAll("^PROJECT", canonicalPath);
		} else if (folder.startsWith("MODULE")) {
			Module moduleForFile = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
			String moduleDirPath = ModuleUtilCore.getModuleDirPath(moduleForFile);
			folder = folder.replaceAll("^MODULE", moduleDirPath);
		}
		return folder;
	}
}
