package krasa.editorGroups;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ApplicationConfiguration {

	private Tabs tabs = new Tabs();

	private boolean autoFolders = true;
	private boolean autoSameName = true;
	private boolean forceSwitch = true;
	private boolean hideEmpty = true;
	private boolean showSize = false;
	private boolean continuousScrolling;
	private boolean preferLatencyOverFlicker = true;
	private boolean indexOnlyEditorGroupsFiles;
	private boolean excludeEditorGroupsFiles;
	private Integer tabBgColor;
	private boolean tabBgColorEnabled;
	private Integer tabFgColor;
	private boolean tabFgColorEnabled;

	public static ApplicationConfiguration state() {
		return ApplicationConfigurationComponent.getInstance().getState();
	}

	public Tabs getTabs() {
		return tabs;
	}

	public void setTabs(Tabs tabs) {
		this.tabs = tabs;
	}

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


	public static class Tabs {
		public static final Color DEFAULT_MASK = new Color(0x262626);
		public static final int DEFAULT_OPACITY = 20;
		public static final Color DEFAULT_TAB_COLOR = Color.WHITE;

		public static final Color DEFAULT_DARCULA_MASK = new Color(0x262626);
		public static final int DEFAULT_DARCULA_OPACITY = 50;
		public static final Color DEFAULT_DARCULA_TAB_COLOR = new Color(0x515658);


		private boolean patchPainter;

		private Integer mask = DEFAULT_MASK.getRGB();
		private int opacity = DEFAULT_OPACITY;
		private Integer defaultTabColor = DEFAULT_TAB_COLOR.getRGB();

		private Integer darcula_mask = DEFAULT_DARCULA_MASK.getRGB();
		private int darcula_opacity = DEFAULT_DARCULA_OPACITY;
		private Integer darcula_defaultTabColor = DEFAULT_DARCULA_TAB_COLOR.getRGB();

		public Integer getMask() {
			return mask;
		}

		public void setMask(Integer mask) {
			this.mask = mask;
		}

		public int getOpacity() {
			return opacity;
		}

		public void setOpacity(int opacity) {
			this.opacity = opacity;
		}

		public Integer getDefaultTabColor() {
			return defaultTabColor;
		}

		public void setDefaultTabColor(Integer defaultTabColor) {
			this.defaultTabColor = defaultTabColor;
		}

		public Integer getDarcula_mask() {
			return darcula_mask;
		}

		public void setDarcula_mask(Integer darcula_mask) {
			this.darcula_mask = darcula_mask;
		}

		public int getDarcula_opacity() {
			return darcula_opacity;
		}

		public void setDarcula_opacity(int darcula_opacity) {
			this.darcula_opacity = darcula_opacity;
		}

		public Integer getDarcula_defaultTabColor() {
			return darcula_defaultTabColor;
		}

		public void setDarcula_defaultTabColor(Integer darcula_defaultTabColor) {
			this.darcula_defaultTabColor = darcula_defaultTabColor;
		}

		public boolean isPatchPainter() {
			return patchPainter;
		}

		public void setPatchPainter(boolean patchPainter) {
			this.patchPainter = patchPainter;
		}

		@Transient
		public void setOpacity(String text) {
			try {
				opacity = Integer.parseInt(text);
				if (opacity > 100) {
					opacity = 100;
				} else if (opacity < 0) {
					opacity = 0;
				}
			} catch (Exception e) {
				opacity = DEFAULT_OPACITY;
			}
		}

		@Transient
		public void setDarcula_opacity(String text) {
			try {
				darcula_opacity = Integer.parseInt(text);
				if (darcula_opacity > 100) {
					darcula_opacity = 100;
				} else if (darcula_opacity < 0) {
					darcula_opacity = 0;
				}
			} catch (Exception e) {
				darcula_opacity = DEFAULT_DARCULA_OPACITY;
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
