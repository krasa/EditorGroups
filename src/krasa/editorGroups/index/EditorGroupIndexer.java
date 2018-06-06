package krasa.editorGroups.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.StringPattern;
import com.intellij.psi.search.IndexPattern;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import krasa.editorGroups.IndexCache;
import krasa.editorGroups.PanelRefresher;
import krasa.editorGroups.model.EditorGroupIndexValue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.*;

public class EditorGroupIndexer implements DataIndexer<String, EditorGroupIndexValue, FileContent> {
	private static final Logger LOG = Logger.getInstance(EditorGroupIndexer.class);
	public static final IndexPattern MAIN_PATTERN = new IndexPattern("@idea\\.(disable|title|color|related)\\s.*", false);
	@SuppressWarnings("unchecked")
	final Pair<IndexPattern, Consumer>[] indexPatterns = new Pair[]{
		new Pair<IndexPattern, Consumer>(new IndexPattern("^@idea\\.title\\s(.*)", false), new TitleConsumer()),
		new Pair<IndexPattern, Consumer>(new IndexPattern("^@idea\\.color\\s(.*)", false), new ColorConsumer()),
		new Pair<IndexPattern, Consumer>(new IndexPattern("^@idea\\.related\\s(.*)", false), new RelatedFilesConsumer()),
		new Pair<IndexPattern, Consumer>(new IndexPattern("(^@idea\\.disable\\s.*)", false), new DisableConsumer())
	};

	@Override
	@NotNull
	public Map<String, EditorGroupIndexValue> map(@NotNull final FileContent inputData) {
		String ownerPath = inputData.getFile().getCanonicalPath();
		try {
			String chars = inputData.getContentAsText().toString(); // matching strings is faster than HeapCharBuffer
			File folder = null;
			try {
				folder = new File(inputData.getFile().getParent().getCanonicalPath());
			} catch (Exception e) {
				return Collections.emptyMap();
			}

			EditorGroupIndexValue currentGroup = null;
			EditorGroupIndexValue lastGroup = null;
			int index = 0;
			HashMap<String, EditorGroupIndexValue> map = new HashMap<>();

			CharSequence input = StringPattern.newBombedCharSequence(chars);
			Pattern optimizedIndexingPattern = MAIN_PATTERN.getOptimizedIndexingPattern();
			Matcher matcher = optimizedIndexingPattern.matcher(input);
			while (matcher.find()) {
				if (matcher.start() != matcher.end()) {
					String trim = matcher.group(0).trim();
					currentGroup = processPatterns(inputData, folder, currentGroup, trim);

					if (lastGroup != null && lastGroup != currentGroup) {
						index = add(inputData, ownerPath, lastGroup, index, map);
					}

					lastGroup = currentGroup;
				}
			}

			if (currentGroup != null && isEmpty(currentGroup.getId())) {
				add(inputData, ownerPath, currentGroup, index, map);
			}
			return map;
		} catch (DisableException e) {
			IndexCache.getInstance(inputData.getProject()).removeGroup(ownerPath);
			return Collections.emptyMap();
		} catch (com.intellij.openapi.progress.ProcessCanceledException e) {
			throw e;
		} catch (Exception e) {
			LOG.error(e);
			return Collections.emptyMap();
		}
	}

	public int add(@NotNull FileContent inputData, String ownerPath, EditorGroupIndexValue lastGroup, int index, HashMap<String, EditorGroupIndexValue> map) {
		lastGroup.setId(ownerPath, index++);
		lastGroup = PanelRefresher.getInstance(inputData.getProject()).onIndexingDone(ownerPath, lastGroup);
		map.put(lastGroup.getId(), lastGroup);
		return index;
	}

	public EditorGroupIndexValue processPatterns(@NotNull FileContent inputData, File folder, EditorGroupIndexValue value, CharSequence trim) {
		for (Pair<IndexPattern, Consumer> indexPattern : indexPatterns) {
			Pattern pattern = indexPattern.first.getOptimizedIndexingPattern();
			Consumer consumer = indexPattern.second;
			if (pattern != null) {
				Matcher subMatcher = pattern.matcher(trim);
				while (subMatcher.find()) {
					if (subMatcher.start() != subMatcher.end()) {
						value = consumer.consume(inputData, value, folder, subMatcher.group(1).trim());
					}
				}
			}
		}
		return value;
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
			EditorGroupIndexValue group = init(object);
			if (isNotEmpty(group.getTitle())) {
				group = new EditorGroupIndexValue();
			}
			return group.setTitle(value);
		}
	}

	static class ColorConsumer extends Consumer {
		@Override
		EditorGroupIndexValue consume(FileContent inputData, EditorGroupIndexValue object, File folder, String value) {
			return init(object).setColor(value);
		}
	}

	static class RelatedFilesConsumer extends Consumer {

		@Override
		EditorGroupIndexValue consume(FileContent inputData, EditorGroupIndexValue object, File folder, String filePath) {
			if (isBlank(filePath)) {
				return object;
			}
			object = init(object);
			object.addRelated(filePath);
			return object;
		}
	}

	private class DisableConsumer extends Consumer {
		@Override
		EditorGroupIndexValue consume(FileContent inputData, EditorGroupIndexValue object, File folder, String value) {
			throw new DisableException();
		}
	}

	class DisableException extends RuntimeException {
	}
}