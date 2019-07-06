// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl;

import com.intellij.util.ui.JBUI;
import krasa.editorGroups.tabs2.JBTabsPosition;
import krasa.editorGroups.tabs2.JBTabsPresentation;

import java.awt.*;

public class TabsBorder {

	private Insets myBorderSize;
	private int myTabBorderSize;

	private final krasa.editorGroups.tabs2.impl.JBTabsImpl myTabs;

	private JBTabsPosition myPosition;

	private Insets myEffectiveBorder;

	public TabsBorder(krasa.editorGroups.tabs2.impl.JBTabsImpl tabs) {
		myTabs = tabs;
		myBorderSize = new Insets(krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(-1), krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(-1), krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(-1), krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(-1));
		myTabBorderSize = krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(-1);
	}

	public JBTabsPresentation setPaintBorder(int top, int left, int right, int bottom) {
		final Insets newBorder = new Insets(
			krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(top), krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(left), krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(bottom), krasa.editorGroups.tabs2.impl.JBTabsImpl.getBorder(right));
		if (newBorder.equals(myBorderSize)) return myTabs;

		myBorderSize = newBorder;

		myEffectiveBorder = null;

		myTabs.relayout(true, false);

		return myTabs;
	}

	public JBTabsPresentation setTabSidePaintBorder(int size) {
		final int newSize = JBTabsImpl.getBorder(size);
		if (myTabBorderSize == newSize) return myTabs;

		myTabBorderSize = newSize;
		myEffectiveBorder = null;

		myTabs.revalidateAndRepaint(false);

		return myTabs;
	}

	public int getTabBorderSize() {
		return myTabBorderSize;
	}

	public Insets getEffectiveBorder() {
		if (myEffectiveBorder != null && myTabs.getTabsPosition() == myPosition)
			return (Insets) myEffectiveBorder.clone();

		myPosition = myTabs.getTabsPosition();

		if (myTabs.isEditorTabs()) {
			myEffectiveBorder = JBUI.emptyInsets();
		} else {
			myEffectiveBorder = new Insets(
				myPosition == JBTabsPosition.top ? myTabBorderSize : myBorderSize.top,
				myPosition == JBTabsPosition.left ? myTabBorderSize : myBorderSize.left,
				myPosition == JBTabsPosition.bottom ? myTabBorderSize : myBorderSize.bottom,
				myPosition == JBTabsPosition.right ? myTabBorderSize : myBorderSize.right
			);
		}


		return (Insets) myEffectiveBorder.clone();
	}
}
