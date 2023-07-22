// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ExperimentalUI;
import krasa.editorGroups.tabs2.KrEditorTabsBase;
import krasa.editorGroups.tabs2.KrTabsPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author pegov
 */
public class KrEditorTabs extends KrTabsImpl implements KrEditorTabsBase {
  public static final Key<Boolean> MARK_MODIFIED_KEY = Key.create("EDITOR_TABS_MARK_MODIFIED");
  @Deprecated(forRemoval = true)
  protected KrEditorTabsPainter myDefaultPainter = new KrDefaultEditorTabsPainter(this);

  private boolean myAlphabeticalModeChanged = false;

  public KrEditorTabs(@Nullable Project project, @SuppressWarnings("unused") @Nullable IdeFocusManager focusManager, @NotNull Disposable parentDisposable) {
    super(project, parentDisposable);
    setSupportsCompression(true);
  }

  public KrEditorTabs(@Nullable Project project, @NotNull Disposable parentDisposable) {
    super(project, parentDisposable);
    setSupportsCompression(true);
  }

  @Override
  public void uiSettingsChanged(@NotNull UISettings uiSettings) {
    resetTabsCache();
    relayout(true, false);

    super.uiSettingsChanged(uiSettings);
  }

  /**
   * @deprecated Use {@link #KrEditorTabs(Project, Disposable)}
   */
  @Deprecated
  public KrEditorTabs(@Nullable Project project,
                      @SuppressWarnings("unused") @NotNull ActionManager actionManager,
                      @Nullable IdeFocusManager focusManager,
                      @NotNull Disposable parent) {
    this(project, parent);
  }

  @Override
  public boolean isEditorTabs() {
    return true;
  }

  @Override
  public boolean useSmallLabels() {
    return UISettings.getInstance().getUseSmallLabelsOnTabs() && !ExperimentalUI.isNewUI();
  }

  @Override
  public boolean isAlphabeticalMode() {
    if (myAlphabeticalModeChanged) {
      return super.isAlphabeticalMode();
    }
    return UISettings.getInstance().getSortTabsAlphabetically();
  }

  @Override
  public @NotNull KrTabsPresentation setAlphabeticalMode(boolean alphabeticalMode) {
    myAlphabeticalModeChanged = true;
    return super.setAlphabeticalMode(alphabeticalMode);
  }

  public boolean shouldPaintBottomBorder() {
    return true;
  }
}
