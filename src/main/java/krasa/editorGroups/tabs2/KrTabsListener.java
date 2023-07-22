// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface KrTabsListener {
  default void selectionChanged(@Nullable KrTabInfo oldSelection, @Nullable KrTabInfo newSelection) {
  }

  default void beforeSelectionChanged(KrTabInfo oldSelection, KrTabInfo newSelection) {
  }

  default void tabRemoved(@NotNull KrTabInfo tabToRemove) {
  }

  default void tabsMoved() {
  }
}
