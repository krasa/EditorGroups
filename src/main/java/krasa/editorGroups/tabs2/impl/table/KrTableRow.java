// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl.table;

import krasa.editorGroups.tabs2.KrTabInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
class KrTableRow {

  private final KrTablePassInfo myData;
  final List<KrTabInfo> myColumns = new ArrayList<>();
  int width;

  KrTableRow(final KrTablePassInfo data) {
    myData = data;
  }

  void add(KrTabInfo info, int width) {
    myColumns.add(info);
    this.width += width;
    myData.myInfo2Row.put(info, this);
  }

}
