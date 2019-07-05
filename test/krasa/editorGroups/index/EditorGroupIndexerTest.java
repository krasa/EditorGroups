package krasa.editorGroups.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.search.IndexPattern;
import org.junit.Test;

import java.util.regex.Matcher;

/* @group.disable */
public class EditorGroupIndexerTest {
	private static final Logger LOG = Logger.getInstance(EditorGroupIndexerTest.class);

	@Test
	public void name() {
		String input = "@group.related */Kuk.java\n" +
			"@group.related B2\n" +
			"@group.color red-26+40\n" +
			"\n";

		IndexPattern indexPattern = new IndexPattern("@group\\.(excluded|title|color|related)\\s.*", false);
		Matcher matcher = indexPattern.getOptimizedIndexingPattern().matcher(input);
		while (matcher.find()) {
			if (matcher.start() != matcher.end()) {
				String trim = matcher.group(0).trim();
				if (LOG.isDebugEnabled()) LOG.debug(trim);
			}
		}


	}
}