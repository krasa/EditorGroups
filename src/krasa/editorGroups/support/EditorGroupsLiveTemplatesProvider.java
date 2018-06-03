package krasa.editorGroups.support;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;
import org.jetbrains.annotations.Nullable;

public class EditorGroupsLiveTemplatesProvider implements DefaultLiveTemplatesProvider {
	@Override
	public String[] getDefaultLiveTemplateFiles() {
		return new String[]{"liveTemplates/EditorGroups"};
	}

	@Nullable
	@Override
	public String[] getHiddenLiveTemplateFiles() {
		return new String[0];
	}
}
