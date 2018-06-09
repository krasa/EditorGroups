package krasa.editorGroups.language;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextCompletionValueDescriptor;
import com.intellij.util.textCompletion.ValuesCompletionProvider;
import com.intellij.xml.util.ColorIconCache;
import krasa.editorGroups.language.annotator.LanguagePatternHolder;
import krasa.editorGroups.support.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MyCompletionContributor extends CompletionContributor {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(MyCompletionContributor.class);

	public MyCompletionContributor() {
		extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {

			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
				PsiFile file = parameters.getOriginalFile();

				//		if (parameters.getInvocationCount() == 0 &&
				//			!Boolean.TRUE.equals(file.getUserData(TextCompletionUtil.AUTO_POPUP_KEY))) {
				//			return;
				//		}

				String text = file.getText();
				int offset = Math.min(text.length(), parameters.getOffset());
				int j = text.lastIndexOf('\n', offset - 1) + 1;
				String line = text.substring(j, offset);
				line = line.substring(Math.max(line.indexOf("@"), 0));
				int i = line.indexOf(' ') + 1;
				String prefix = line.substring(i).trim();


				ArrayList<String> values = new ArrayList<>();

				if (line.contains("@group.color")) {
					values.addAll(LanguagePatternHolder.colors);
				}
				if (isPathLine(line) && !containsMacro(line)) {
					values.add("MODULE/");
					values.add("PROJECT/");
					values.add("*/");
				} else if (prefix.contains("@") || StringUtils.isBlank(line)) {
					values.addAll(LanguagePatternHolder.keywordsWithDescription.keySet());
				}


				TextCompletionProvider provider = new MyCompletionProvider(new MyStringValueDescriptor(), values, Arrays.asList(' '));
				CompletionResultSet activeResult = provider.applyPrefixMatcher(result, prefix);

				provider.fillCompletionVariants(parameters, prefix, activeResult);

				result.runRemainingContributors(parameters, true);
				result.stopHere();
			}

			private boolean containsMacro(String line) {
				return line.contains("MODULE/") || line.contains("PROJECT/") || line.contains("*/");
			}

			private boolean isPathLine(String line) {
				return line.contains("@group.related") || line.contains("@group.root");
			}
		});


	}

	@Override
	public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
		super.fillCompletionVariants(parameters, result);
	}


	private static class MyCompletionProvider extends ValuesCompletionProvider.ValuesCompletionProviderDumbAware<String> {

		MyCompletionProvider(TextCompletionValueDescriptor descriptor, @NotNull Collection<String> values, List<Character> ts) {
			super(descriptor, ts, values, true);
		}


		@NotNull
		@Override
		protected LookupElement installInsertHandler(@NotNull LookupElementBuilder builder) {
			LookupElement lookupElement = super.installInsertHandler(builder);
			return PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE);
		}

		@Nullable
		@Override
		public String getPrefix(@NotNull String text, int offset) {
			return MyCompletionContributor.getPrefix(text, offset);
		}
	}

	@NotNull
	private static String getPrefix(@NotNull String text, int offset) {
		int i = text.lastIndexOf(' ', offset - 1) + 1;
		int j = text.lastIndexOf('\n', offset - 1) + 1;
		return text.substring(Math.max(i, j), offset);
	}

	private static class MyStringValueDescriptor extends DefaultTextCompletionValueDescriptor.StringValueDescriptor {

		@Nullable
		@Override
		protected Icon getIcon(@NotNull String item) {
			Color colorInstance = Utils.getColorInstance(item.toLowerCase());
			if (colorInstance == null) {
				return null;
			}
			return ColorIconCache.getIconCache().getIcon(colorInstance, 13);
		}


		@Nullable
		@Override
		protected String getTailText(@NotNull String item) {
			return null;
		}

		@Nullable
		@Override
		protected String getTypeText(@NotNull String item) {
			return LanguagePatternHolder.allWithDescription.get(item);
		}


	}
}
