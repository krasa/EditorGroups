package krasa.editorGroups.support;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.Link;
import krasa.editorGroups.model.RegexGroup;
import krasa.editorGroups.model.RegexGroupModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Matcher;


public class RegexFileResolver extends FileResolver {

	public RegexFileResolver(Project project) {
		super(project);
	}

	public List<Link> resolveRegexGroupLinks(@NotNull RegexGroup regexGroup) {
		try {
			long start = System.currentTimeMillis();
			String folderPath = regexGroup.getFolderPath();
			if (regexGroup.getRegexGroupModel().getScope() == RegexGroupModel.Scope.WHOLE_PROJECT) {
				folderPath = project.getBasePath();
			}
			if (folderPath == null) {
				throw new IllegalStateException("folderPath is null " + regexGroup);
			}
			RegexGroupModel regexGroupModel = regexGroup.getRegexGroupModel();
			String finalFolderPath = folderPath;

			Matcher groupMatcher = regexGroupModel.getRegexPattern().matcher("");

			Files.walkFileTree(Paths.get(folderPath), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (regexGroupModel.getScope() == RegexGroupModel.Scope.CURRENT_FOLDER) {
						if (dir.equals(Paths.get(finalFolderPath))) {
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.SKIP_SUBTREE;
						}
					} else {
						return super.preVisitDirectory(dir, attrs);
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String string = file.getFileName().toString();
					Matcher matcher = groupMatcher.reset(string);
					if (matcher.matches()) {
						links.add(file.toAbsolutePath().toString());
						if (links.size() > config.getGroupSizeLimitInt()) {
							log.warn("Found too many matching files, aborting. size=" + links.size() + " " + regexGroup);
							return FileVisitResult.TERMINATE;
						}
					}

					return super.visitFile(file, attrs);
				}
			});
			long duration = System.currentTimeMillis() - start;
			if (duration > 500) {
				log.warn("<resolveRegexGroup " + duration + "ms " + regexGroup + "; links=" + links);
			} else if (log.isDebugEnabled()) {
				log.debug("<resolveRegexGroup " + duration + "ms links=" + links);
			}

			return Link.from(links);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Link> resolveRegexGroupLinks(@NotNull RegexGroup regexGroup, @NotNull VirtualFile currentFile) {
		try {
			long start = System.currentTimeMillis();
			String folderPath = regexGroup.getFolderPath();
			if (regexGroup.getRegexGroupModel().getScope() == RegexGroupModel.Scope.WHOLE_PROJECT) {
				folderPath = project.getBasePath();
			}
			if (folderPath == null) {
				throw new IllegalStateException("folderPath is null " + regexGroup);
			}
			String fileName = regexGroup.getFileName();
			RegexGroupModel regexGroupModel = regexGroup.getRegexGroupModel();
			Matcher referenceMatcher = regexGroupModel.getRegexPattern().matcher(fileName);
			boolean matches = referenceMatcher.matches();
			if (!matches) {
				throw new RuntimeException(fileName + " does not match " + regexGroup.getRegexGroupModel());
			}
			//always include it in case there are to many matches
			links.add(currentFile.getPath());

			String finalFolderPath = folderPath;

			Matcher groupMatcher = regexGroupModel.getRegexPattern().matcher("");

			Files.walkFileTree(Paths.get(folderPath), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (regexGroupModel.getScope() == RegexGroupModel.Scope.CURRENT_FOLDER) {
						if (dir.equals(Paths.get(finalFolderPath))) {
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.SKIP_SUBTREE;
						}
					} else {
						return super.preVisitDirectory(dir, attrs);
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String string = file.getFileName().toString();
					Matcher matcher = groupMatcher.reset(string);
					if (matcher.matches()) {
						for (int j = 1; j <= matcher.groupCount(); j++) {
							String refGroup = referenceMatcher.group(j);
							String group = matcher.group(j);
							if (!refGroup.equals(group)) {
								return FileVisitResult.CONTINUE;
							}
						}
						links.add(file.toAbsolutePath().toString());
						if (links.size() > config.getGroupSizeLimitInt()) {
							log.warn("Found too many matching files, aborting. size=" + links.size() + " " + regexGroup);
							return FileVisitResult.TERMINATE;
						}
					}

					return super.visitFile(file, attrs);
				}
			});
			long duration = System.currentTimeMillis() - start;
			if (duration > 500) {
				log.warn("<resolveRegexGroup " + duration + "ms " + regexGroup + "; links=" + links);
			} else if (log.isDebugEnabled()) {
				log.debug("<resolveRegexGroup " + duration + "ms links=" + links);
			}

			return Link.from(links);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
