package krasa.editorGroups;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

@State(
	name = "EditorGroups",
	storages = {
		@Storage(value = "EditorGroups.xml")
	}
)
public class ApplicationConfiguration implements PersistentStateComponent<ApplicationConfiguration.State> {
	private State state = new State();

	public static ApplicationConfiguration getInstance() {
		return ServiceManager.getService(ApplicationConfiguration.class);
	}

	public static State state() {
		return getInstance().getState();
	}

	@NotNull
	@Override
	public State getState() {
		return state;
	}

	@Override
	public void loadState(@NotNull State state) {
		this.state = state;
	}


	public static class State {
		public boolean autoFolders = true;
		public boolean autoSameName = true;
		public boolean forceSwitch = true;
		public boolean hideEmpty = true;
		public boolean showSize = false;
		private boolean continuousScrolling;
		private boolean preferLatencyOverFlicker = true;
		private boolean indexOnlyEditorGroupsFiles;
		private boolean excludeEditorGroupsFiles;
		private Integer tabBgColor;
		private boolean tabBgColorEnabled;
		private Integer tabFgColor;
		private boolean tabFgColorEnabled;


		public boolean isAutoFolders() {
			return autoFolders;
		}

		public void setAutoFolders(boolean autoFolders) {
			this.autoFolders = autoFolders;
		}

		public boolean isAutoSameName() {
			return autoSameName;
		}

		public void setAutoSameName(boolean autoSameName) {
			this.autoSameName = autoSameName;
		}

		public boolean isForceSwitch() {
			return forceSwitch;
		}

		public void setForceSwitch(boolean forceSwitch) {
			this.forceSwitch = forceSwitch;
		}

		public boolean isHideEmpty() {
			return hideEmpty;
		}

		public void setHideEmpty(boolean hideEmpty) {
			this.hideEmpty = hideEmpty;
		}

		public boolean isShowSize() {
			return showSize;
		}

		public void setShowSize(boolean showSize) {
			this.showSize = showSize;
		}


		public boolean isContinuousScrolling() {
			return continuousScrolling;
		}

		public void setContinuousScrolling(final boolean continuousScrolling) {
			this.continuousScrolling = continuousScrolling;
		}

		public boolean isPreferLatencyOverFlicker() {
			return preferLatencyOverFlicker;
		}

		public void setPreferLatencyOverFlicker(final boolean preferLatencyOverFlicker) {
			this.preferLatencyOverFlicker = preferLatencyOverFlicker;
		}

		public boolean isIndexOnlyEditorGroupsFiles() {
			return indexOnlyEditorGroupsFiles;
		}

		public void setIndexOnlyEditorGroupsFiles(final boolean indexOnlyEditorGroupsFiles) {
			this.indexOnlyEditorGroupsFiles = indexOnlyEditorGroupsFiles;
		}

		public boolean isExcludeEditorGroupsFiles() {
			return excludeEditorGroupsFiles;
		}

		public void setExcludeEditorGroupsFiles(final boolean excludeEditorGroupsFiles) {
			this.excludeEditorGroupsFiles = excludeEditorGroupsFiles;
		}

		public void setTabBgColor(Integer tabBgColor) {
			this.tabBgColor = tabBgColor;
		}

		public void setTabFgColor(Integer tabFgColor) {
			this.tabFgColor = tabFgColor;
		}

		public Integer getTabBgColor() {
			return tabBgColor;
		}

		public Integer getTabFgColor() {
			return tabFgColor;
		}


		public boolean isTabBgColorEnabled() {
			return tabBgColorEnabled;
		}

		public void setTabBgColorEnabled(boolean tabBgColorEnabled) {
			this.tabBgColorEnabled = tabBgColorEnabled;
		}

		public boolean isTabFgColorEnabled() {
			return tabFgColorEnabled;
		}

		public void setTabFgColorEnabled(boolean tabFgColorEnabled) {
			this.tabFgColorEnabled = tabFgColorEnabled;
		}

		@Transient
		public Color getTabBgColorAsAWT() {
			return asAWT(tabBgColor);
		}

		@Transient
		public void setTabBgColorAWT(Color color) {
			if (color != null) {
				this.tabBgColor = color.getRGB();
			}
		}

		@Transient
		public Color getTabFgColorAsAWT() {
			return asAWT(tabFgColor);
		}

		@Transient
		public void setTabFgColorAWT(Color color) {
			if (color != null) {
				this.tabFgColor = color.getRGB();
			}
		}


	}


	@Nullable
	private static Color asAWT(Integer color) {
		if (color == null) {
			return null;
		}
		return new Color(color);
	}
}
