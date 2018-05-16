package krasa.editorGroups.support;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {
	private static final int PARSING_LIMIT = 300;
	private static final String IDEA_RELATED = "@idea.related";
	public static final String IDEA_TITLE = "@idea.title";


	@NotNull
	public EditorGroup parse(String fileContent, String ownerPath, Cache cache) {
		long t0 = System.currentTimeMillis();
		File parentFile = new File(ownerPath).getParentFile();

		if (fileContent == null) {
			System.out.println("Parsing " + ownerPath + " fileContent=null, aborting");
			return EditorGroupImpl.EMPTY;
		}
		System.out.println("Parsing " + ownerPath);

		List<String> paths = new ArrayList<>();
		// paths.add(owner.getCanonicalPath());
		int start = StringUtil.indexOf(fileContent, IDEA_TITLE, 0, PARSING_LIMIT);
		int end = StringUtil.indexOf(fileContent, "\n", start + IDEA_RELATED.length());
		if (end < 0) {
			end = fileContent.length();
		}
		String title = null;
		if (start > 0 && end > 0) {
			title = fileContent.substring(start + IDEA_TITLE.length() + 1, end).trim();
		}

		start = StringUtil.indexOf(fileContent, IDEA_RELATED, 0, PARSING_LIMIT);
		while (start >= 0) {
			end = StringUtil.indexOf(fileContent, "\n", start + IDEA_RELATED.length());
			if (end < 0) {
				end = fileContent.length();
			}
			String filePath = fileContent.substring(start + IDEA_RELATED.length() + 1, end).trim();
			if (filePath.length() > 0) {
				if (FileUtil.isAbsolute(filePath)) {
					paths.add(filePath.replace("\\", "/"));
				} else {
					try {
						File file = new File(parentFile, filePath);
						paths.add(file.getCanonicalPath().replace("\\", "/"));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

				}
			}

			start = StringUtil.indexOf(fileContent, IDEA_RELATED, end, end + PARSING_LIMIT);
		}

		if (!paths.contains(ownerPath)) {
			paths.add(0, ownerPath);
		}
		EditorGroup editorGroup = new EditorGroupImpl(paths, ownerPath, title);
		cache.updateCaches(editorGroup, ownerPath);
		System.out.println("parse " + (System.currentTimeMillis() - t0));

		return editorGroup;
	}
}
