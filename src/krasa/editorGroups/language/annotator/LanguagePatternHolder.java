package krasa.editorGroups.language.annotator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import krasa.editorGroups.support.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public enum LanguagePatternHolder {
	INSTANCE;
	/**
	 * @see krasa.editorGroups.index.EditorGroupIndexer
	 */
	public static final Collection<String> keywords = Arrays.asList(
		"id",
		"root",
		"related",
		"color",
		"disable",
		"title"
	);

	public static final Map<String, String> keywordsWithDescription = new HashMap<String, String>() {{
		put("@group.id ", "ID - optional for one group per file");
		put("@group.root ", "Directory root, this by default");
		put("@group.related ", "Related files");
		put("@group.color ", "Tab background color");
		put("@group.disable ", "Disable indexing of this file");
		put("@group.title ", "Group title");

	}};


	public static final Collection<String> colors = Utils.colorSet;


	public static final Collection<String> metadata = Arrays.asList(
		"group"
	);


	public static final Map<String, String> macrosWithDescription = new HashMap<String, String>() {{
		put("PROJECT/", "Project macro");
		put("MODULE/", "Current module macro");
		put("*/", "Anywhere in the project");
	}};

	public static final Map<String, String> allWithDescription = new HashMap<String, String>() {{
		putAll(keywordsWithDescription);
		putAll(macrosWithDescription);
	}};


	public static final Collection<String> macros = Arrays.asList(
		"PROJECT",
		"MODULE"
	);

	public static final Pattern keywordsPattern = createPattern(keywords, "", true);
	public static final Pattern colorPattern = createPattern(colors, "", false);
	public static final Pattern metadataPattern = createPattern(metadata, "[@]", true);
	public static final Pattern macrosPattern = createPattern(macros, "", true);

	private static Pattern createPattern(Collection<String> tokens, final String patternPrefix, boolean caseSensitive) {
		Collection<String> tokensAsWords = Collections2.transform(tokens, new Function<String, String>() {
			@Override
			public String apply(String s) {
				return "\\b" + s + "\\b";
			}
		});

		if (caseSensitive) {
			return Pattern.compile("(" + patternPrefix + Joiner.on("|").join(tokensAsWords) + ")");
		} else {
			return Pattern.compile("(?i:" + patternPrefix + Joiner.on("|").join(tokensAsWords) + ")");
		}
	}


}
