package krasa.editorGroups.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.patterns.StringPattern;
import com.intellij.psi.search.IndexPattern;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import krasa.editorGroups.model.EditorGroupIndexValue;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainTextIndexer implements DataIndexer<String, EditorGroupIndexValue, FileContent> {
	private static final Logger LOG = Logger.getInstance(PlainTextIndexer.class);
	@SuppressWarnings("unchecked")
	final Pair<IndexPattern, Consumer>[] indexPatterns = new Pair[]{
		new Pair<IndexPattern, Consumer>(new IndexPattern("@idea.title(.*)", false), new TitleConsumer()),
		new Pair<IndexPattern, Consumer>(new IndexPattern("@idea.related(.*)", false), new RelatedFilesConsumer())
	};

	@Override
	@NotNull
	public Map<String, EditorGroupIndexValue> map(@NotNull final FileContent inputData) {
		try {
			String chars = inputData.getContentAsText().toString(); // matching strings is faster than HeapCharBuffer
			File folder = null;
			try {
				folder = new File(inputData.getFile().getParent().getCanonicalPath());
			} catch (Exception e) {
				return Collections.emptyMap();
			}

			EditorGroupIndexValue value = null;
			for (Pair<IndexPattern, Consumer> indexPattern : indexPatterns) {
				Pattern pattern = indexPattern.first.getOptimizedIndexingPattern();
				Consumer consumer = indexPattern.second;
				if (pattern != null) {
					Matcher matcher = pattern.matcher(StringPattern.newBombedCharSequence(chars));
					while (matcher.find()) {
						if (matcher.start() != matcher.end()) {
							value = consumer.consume(inputData, value, folder, matcher.group(1).trim());
						}
					}
				}
			}

			HashMap<String, EditorGroupIndexValue> map = new HashMap<>();
			String canonicalPath = inputData.getFile().getCanonicalPath();
			if (value != null) {
				value.setOwnerPath(canonicalPath);
				for (String s : value.getRelatedPaths()) {
					map.put(s, value);
				}
				map.put(canonicalPath, value);

			}
			return map;
		} catch (com.intellij.openapi.progress.ProcessCanceledException e) {
			throw e;
		} catch (Exception e) {
			LOG.error(e);
			return Collections.emptyMap();
		}
	}


	static abstract class Consumer {
		EditorGroupIndexValue init(EditorGroupIndexValue value) {
			if (value == null) {
				return new EditorGroupIndexValue();
			}
			return value;
		}

		abstract EditorGroupIndexValue consume(FileContent inputData, EditorGroupIndexValue object, File folder, String value);
	}

	static class TitleConsumer extends Consumer {
		@Override
		EditorGroupIndexValue consume(FileContent inputData, EditorGroupIndexValue object, File folder, String value) {
			return init(object).setTitle(value);
		}
	}

	static class RelatedFilesConsumer extends Consumer {

		@Override
		EditorGroupIndexValue consume(FileContent inputData, EditorGroupIndexValue object, File folder, String filePath) {
			if (StringUtils.isBlank(filePath)) {
				return object;
			}
			object = init(object);

			//TODO patterns
			try {
				if (FileUtil.isAbsolute(filePath)) {
					object.addRelated(filePath.replace("\\", "/"));
				} else {
					File file = new File(folder, filePath);
					object.addRelated(file.getCanonicalPath().replace("\\", "/"));
				}
			} catch (Exception e) {
				LOG.warn("Failed to parse: '" + filePath + "' in " + inputData.getFile().getCanonicalPath());
			}
			return object;
		}
	}
}