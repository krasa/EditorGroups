// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl.singleRow;

import com.intellij.icons.AllIcons;
import krasa.editorGroups.tabs2.impl.JBTabsImpl;
import krasa.editorGroups.tabs2.impl.ShapeTransform;
import krasa.editorGroups.tabs2.impl.TabLabel;
import krasa.editorGroups.tabs2.impl.TabLayout;

import javax.swing.*;
import java.awt.*;

public abstract class SingleRowLayoutStrategy {

	private static final int MIN_TAB_WIDTH = 50;
	final krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout myLayout;
	final JBTabsImpl myTabs;

	protected SingleRowLayoutStrategy(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout layout) {
		myLayout = layout;
		myTabs = myLayout.myTabs;
	}

	abstract int getMoreRectAxisSize();

	public abstract int getStartPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data);

	public abstract int getToFitLength(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data);

	public abstract int getLengthIncrement(final Dimension dimension);

	public abstract int getMinPosition(final Rectangle bounds);

	public abstract int getMaxPosition(final Rectangle bounds);

	protected abstract int getFixedFitLength(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data);

	public Rectangle getLayoutRect(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data, final int position, final int length) {
		return getLayoutRec(position, getFixedPosition(data), length, getFixedFitLength(data));
	}

	protected abstract Rectangle getLayoutRec(final int position, final int fixedPos, final int length, final int fixedFitLength);

	protected abstract int getFixedPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data);

	public abstract Rectangle getMoreRect(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data);

	public abstract boolean isToCenterTextWhenStretched();

	public abstract ShapeTransform createShapeTransform(Rectangle rectangle);

	public abstract boolean canBeStretched();

	public abstract void layoutComp(krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data);

	public boolean isSideComponentOnTabs() {
		return false;
	}

	public abstract boolean isDragOut(TabLabel tabLabel, int deltaX, int deltaY);

	/**
	 * Whether a tab that didn't fit completely on the right/bottom side in scrollable layout should be clipped or hidden altogether.
	 *
	 * @return true if the tab should be clipped, false if hidden.
	 */
	public abstract boolean drawPartialOverflowTabs();

	/**
	 * Return the change of scroll offset for every unit of mouse wheel scrolling.
	 *
	 * @param label the first visible tab label
	 * @return the scroll amount
	 */
	public abstract int getScrollUnitIncrement(TabLabel label);

	abstract static class Horizontal extends SingleRowLayoutStrategy {
		protected Horizontal(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout layout) {
			super(layout);
		}

		@Override
		public boolean isToCenterTextWhenStretched() {
			return true;
		}

		@Override
		public boolean canBeStretched() {
			return true;
		}

		@Override
		public boolean isDragOut(TabLabel tabLabel, int deltaX, int deltaY) {
			return Math.abs(deltaY) > tabLabel.getHeight() * TabLayout.getDragOutMultiplier();
		}

		@Override
		public int getMoreRectAxisSize() {
			return AllIcons.General.MoreTabs.getIconWidth() + 15;
		}

		@Override
		public int getToFitLength(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			JComponent hToolbar = data.hToolbar.get();
			if (hToolbar != null) {
				return myTabs.getWidth() - data.insets.left - data.insets.right - hToolbar.getMinimumSize().width;
			} else {
				return myTabs.getWidth() - data.insets.left - data.insets.right;
			}
		}

		@Override
		public int getLengthIncrement(final Dimension labelPrefSize) {
			return myTabs.isEditorTabs() ? labelPrefSize.width < MIN_TAB_WIDTH ? MIN_TAB_WIDTH : labelPrefSize.width : labelPrefSize.width;
		}

		@Override
		public int getMinPosition(Rectangle bounds) {
			return (int) bounds.getX();
		}

		@Override
		public int getMaxPosition(final Rectangle bounds) {
			return (int) bounds.getMaxX();
		}

		@Override
		public int getFixedFitLength(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return myTabs.myHeaderFitSize.height;
		}

		@Override
		public Rectangle getLayoutRec(final int position, final int fixedPos, final int length, final int fixedFitLength) {
			return new Rectangle(position, fixedPos, length, fixedFitLength);
		}

		@Override
		public int getStartPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return data.insets.left;
		}

		@Override
		public boolean drawPartialOverflowTabs() {
			return true;
		}

		@Override
		public int getScrollUnitIncrement(TabLabel label) {
			return 10;
		}
	}

	static class Top extends Horizontal {

		Top(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout layout) {
			super(layout);
		}

		@Override
		public boolean isSideComponentOnTabs() {
			return !myTabs.isSideComponentVertical() && myTabs.isSideComponentOnTabs();
		}

		@Override
		public ShapeTransform createShapeTransform(Rectangle labelRec) {
			return new ShapeTransform.Top(labelRec);
		}

		@Override
		public int getFixedPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return data.insets.top;
		}

		@Override
		public Rectangle getMoreRect(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			int x;
			if (myTabs.isEditorTabs()) {
				x = data.layoutSize.width - data.moreRectAxisSize - 1;
			} else {
				x = data.position + (data.lastGhostVisible ? data.lastGhost.width : 0);
			}
			return new Rectangle(x, data.insets.top + JBTabsImpl.getSelectionTabVShift(),
				data.moreRectAxisSize - 1, myTabs.myHeaderFitSize.height);
		}


		@Override
		public void layoutComp(krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			if (myTabs.isHideTabs()) {
				myTabs.layoutComp(data, 0, 0, 0, 0);
			} else {
				JComponent vToolbar = data.vToolbar.get();
				final int vToolbarWidth = vToolbar != null ? vToolbar.getPreferredSize().width : 0;
				final int vSeparatorWidth = vToolbarWidth > 0 ? 1 : 0;
				final int x = vToolbarWidth > 0 ? vToolbarWidth + vSeparatorWidth : 0;
				JComponent hToolbar = data.hToolbar.get();
				final int hToolbarHeight = !myTabs.isSideComponentOnTabs() && hToolbar != null ? hToolbar.getPreferredSize().height : 0;
				final int y = myTabs.myHeaderFitSize.height +
					(Math.max(hToolbarHeight, 0));

				JComponent comp = data.comp.get();
				if (hToolbar != null) {
					final Rectangle compBounds = myTabs.layoutComp(x, y, comp, 0, 0);
					if (myTabs.isSideComponentOnTabs()) {
						int toolbarX = (data.moreRect != null ? (int) data.moreRect.getMaxX() : data.position) + myTabs.getToolbarInset();
						final Rectangle rec =
							new Rectangle(toolbarX, data.insets.top, myTabs.getSize().width - data.insets.left - toolbarX, myTabs.myHeaderFitSize.height);
						myTabs.layout(hToolbar, rec);
					} else {
						final int toolbarHeight = hToolbar.getPreferredSize().height;
						myTabs.layout(hToolbar, compBounds.x, compBounds.y - toolbarHeight, compBounds.width, toolbarHeight);
					}
				} else if (vToolbar != null) {
					if (myTabs.isSideComponentBefore()) {
						final Rectangle compBounds = myTabs.layoutComp(x, y, comp, 0, 0);
						myTabs.layout(vToolbar, compBounds.x - vToolbarWidth - vSeparatorWidth, compBounds.y, vToolbarWidth, compBounds.height);
					} else {
						int width = vToolbarWidth > 0 ? myTabs.getWidth() - vToolbarWidth - vSeparatorWidth : myTabs.getWidth();
						final Rectangle compBounds = myTabs.layoutComp(new Rectangle(0, y, width, myTabs.getHeight()), comp, 0, 0);
						myTabs.layout(vToolbar, compBounds.x + compBounds.width + vSeparatorWidth, compBounds.y, vToolbarWidth, compBounds.height);
					}
				} else {
					myTabs.layoutComp(x, y, comp, 0, 0);
				}
			}
		}
	}

	static class Bottom extends Horizontal {
		Bottom(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout layout) {
			super(layout);
		}

		@Override
		public void layoutComp(krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			if (myTabs.isHideTabs()) {
				myTabs.layoutComp(data, 0, 0, 0, 0);
			} else {
				myTabs.layoutComp(data, 0, 0, 0, -(myTabs.myHeaderFitSize.height));
			}
		}

		@Override
		public int getFixedPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return myTabs.getSize().height - data.insets.bottom - myTabs.myHeaderFitSize.height;
		}

		@Override
		public Rectangle getMoreRect(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return new Rectangle(myTabs.getWidth() - data.insets.right - data.moreRectAxisSize + 2, getFixedPosition(data),
				data.moreRectAxisSize - 1, myTabs.myHeaderFitSize.height);
		}

		@Override
		public ShapeTransform createShapeTransform(Rectangle labelRec) {
			return new ShapeTransform.Bottom(labelRec);
		}
	}

	abstract static class Vertical extends SingleRowLayoutStrategy {
		protected Vertical(krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout layout) {
			super(layout);
		}

		@Override
		public boolean isDragOut(TabLabel tabLabel, int deltaX, int deltaY) {
			return Math.abs(deltaX) > tabLabel.getHeight() * TabLayout.getDragOutMultiplier();
		}

		@Override
		public boolean isToCenterTextWhenStretched() {
			return false;
		}

		@Override
		int getMoreRectAxisSize() {
			return AllIcons.General.MoreTabs.getIconHeight() + 4;
		}

		@Override
		public boolean canBeStretched() {
			return false;
		}

		@Override
		public int getStartPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return data.insets.top;
		}

		@Override
		public int getToFitLength(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return myTabs.getHeight() - data.insets.top - data.insets.bottom;
		}

		@Override
		public int getLengthIncrement(final Dimension labelPrefSize) {
			return labelPrefSize.height;
		}

		@Override
		public int getMinPosition(Rectangle bounds) {
			return (int) bounds.getMinY();
		}

		@Override
		public int getMaxPosition(final Rectangle bounds) {
			return (int) bounds.getMaxY();
		}

		@Override
		public int getFixedFitLength(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return myTabs.myHeaderFitSize.width;
		}

		@Override
		public boolean drawPartialOverflowTabs() {
			return false;
		}

		@Override
		public int getScrollUnitIncrement(TabLabel label) {
			return label.getPreferredSize().height;
		}
	}

	static class Left extends Vertical {
		Left(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout layout) {
			super(layout);
		}


		@Override
		public void layoutComp(krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			if (myTabs.isHideTabs()) {
				myTabs.layoutComp(data, 0, 0, 0, 0);
			} else {
				myTabs.layoutComp(data, myTabs.myHeaderFitSize.width, 0, 0, 0);
			}
		}

		@Override
		public ShapeTransform createShapeTransform(Rectangle labelRec) {
			return new ShapeTransform.Left(labelRec);
		}

		@Override
		public Rectangle getLayoutRec(final int position, final int fixedPos, final int length, final int fixedFitLength) {
			return new Rectangle(fixedPos, position, fixedFitLength, length);
		}

		@Override
		public int getFixedPosition(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return data.insets.left;
		}

		@Override
		public Rectangle getMoreRect(final krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return new Rectangle(data.insets.left + JBTabsImpl.getSelectionTabVShift(),
				myTabs.getHeight() - data.insets.bottom - data.moreRectAxisSize - 1,
				myTabs.myHeaderFitSize.width,
				data.moreRectAxisSize - 1);
		}

	}

	static class Right extends Vertical {
		Right(SingleRowLayout layout) {
			super(layout);
		}

		@Override
		public void layoutComp(krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			if (myTabs.isHideTabs()) {
				myTabs.layoutComp(data, 0, 0, 0, 0);
			} else {
				myTabs.layoutComp(data, 0, 0, -(myTabs.myHeaderFitSize.width), 0);
			}
		}

		@Override
		public ShapeTransform createShapeTransform(Rectangle labelRec) {
			return new ShapeTransform.Right(labelRec);
		}

		@Override
		public Rectangle getLayoutRec(int position, int fixedPos, int length, int fixedFitLength) {
			return new Rectangle(fixedPos, position, fixedFitLength - 1, length);
		}

		@Override
		public int getFixedPosition(krasa.editorGroups.tabs2.impl.singleRow.SingleRowPassInfo data) {
			return data.layoutSize.width - myTabs.myHeaderFitSize.width - data.insets.right;
		}

		@Override
		public Rectangle getMoreRect(SingleRowPassInfo data) {
			return new Rectangle(data.layoutSize.width - myTabs.myHeaderFitSize.width,
				myTabs.getHeight() - data.insets.bottom - data.moreRectAxisSize - 1,
				myTabs.myHeaderFitSize.width,
				data.moreRectAxisSize - 1);
		}
	}

}
