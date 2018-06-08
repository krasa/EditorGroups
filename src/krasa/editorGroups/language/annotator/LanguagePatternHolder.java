package krasa.editorGroups.language.annotator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import krasa.editorGroups.support.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

public enum LanguagePatternHolder {
	INSTANCE;

	public final Collection<String> keywords = Arrays.asList(
		"related",
		"color",
		"disable",
		"title"
	);

	public final Collection<String> colors = Utils.colorMap.keySet();


	public final Collection<String> metadata = Arrays.asList(
		"group"

	);


	public final Pattern keywordsPattern = createPattern(keywords, "", true);
	public final Pattern colorPattern = createPattern(colors, "", false);
	public final Pattern metadataPattern = createPattern(metadata, "[@]", true);

	private Pattern createPattern(Collection<String> tokens, final String patternPrefix, boolean caseSensitive) {
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
