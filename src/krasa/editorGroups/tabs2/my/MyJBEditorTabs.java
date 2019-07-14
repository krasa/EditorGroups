package krasa.editorGroups.tabs2.my;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.BitUtil;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.SwitchRequest;
import krasa.editorGroups.tabs2.JBTabPainter;
import krasa.editorGroups.tabs2.TabInfo;
import krasa.editorGroups.tabs2.impl.JBEditorTabs;
import krasa.editorGroups.tabs2.impl.TabLabel;
import krasa.editorGroups.tabs2.impl.singleRow.ScrollableSingleRowLayout;
import krasa.editorGroups.tabs2.impl.singleRow.SingleRowLayout;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class MyJBEditorTabs extends JBEditorTabs {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(MyJBEditorTabs.class);

	private final Project project;
	private final VirtualFile file;

	public MyJBEditorTabs(Project project, @NotNull ActionManager actionManager, IdeFocusManager focusManager, @NotNull Disposable parent, VirtualFile file) {
		super(project, actionManager, focusManager, parent);
		this.project = project;
		this.file = file;
		patchMouseListener(this);
	}

	protected krasa.editorGroups.tabs2.impl.TabLabel createTabLabel(TabInfo info) {
		TabLabel tabLabel = new TabLabel(this, info);
		patchMouseListener(tabLabel);

		return tabLabel;
	}

	private void patchMouseListener(Component tabLabel) {
		MouseListener[] mouseListeners = tabLabel.getMouseListeners();
		for (MouseListener mouseListener : mouseListeners) {
			tabLabel.removeMouseListener(mouseListener);
		}

		for (MouseListener mouseListener : mouseListeners) {
			tabLabel.addMouseListener(new MyMouseAdapter(mouseListener));

		}
	}

	@Override
	public void remove(Component comp) {
		if (comp == null) {   //because of #updateSideComponent
			return;
		}
		super.remove(comp);
	}

	public boolean bulkUpdate;

	@Override
	protected void revalidateAndRepaint(boolean layoutNow) {
		if (bulkUpdate) {        //performance optimization
			return;
		}
		super.revalidateAndRepaint(layoutNow);
	}

//	@Override
//	protected void updateAttraction(TabInfo tabInfo, boolean start) {
//	}

	@Override
	protected void updateSideComponent(TabInfo tabInfo) {
		//performance optimization
	}

	@Override
	protected JBTabPainter createTabPainter() {
		return new EditorGroupsJBDefaultTabPainter();
	}

//	@Override
//	protected Animator createAnimator() {
//		return null;
//	}


	/**
	 * com.intellij.util.IncorrectOperationException: Sorry but parent: EditorGroups.MyJBEditorTabs visible=[] selected=null has already been disposed (see the cause for stacktrace) so the child: Animator 'JBTabs Attractions' @1845106519 (stopped) will never be disposed
	 * at com.intellij.openapi.util.objectTree.ObjectTree.register(ObjectTree.java:61)
	 * at com.intellij.openapi.util.Disposer.register(Disposer.java:92)
	 * at krasa.editorGroups.tabs2.impl.JBTabsImpl$7.initialize(JBTabsImpl.java:340)
	 * at krasa.editorGroups.tabs2.impl.JBTabsImpl$7.initialize(JBTabsImpl.java:333)
	 */

//	@Override
//	protected void createLazyUiDisposable(@NotNull Disposable parent) {
//		super.createLazyUiDisposable(parent);
//	}
	@Override
	protected SingleRowLayout createSingleRowLayout() {
		return new ScrollableSingleRowLayout(this);
	}

	@Override
	protected boolean isActiveTabs(TabInfo info) {
		return true;
	}


	@Override
	public boolean isSelectionClick(final MouseEvent e, boolean canBeQuick) {
		if (e.getClickCount() == 1 || canBeQuick) {
			if (!e.isPopupTrigger()) {
				return e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON2 && BitUtil.isSet(e.getModifiersEx(), InputEvent.ALT_DOWN_MASK);
			}
		}

		return false;
	}

	@Override
	public boolean isCloseClick(MouseEvent e) {
		return false;
	}

	@Override
	public void doLayout() {
		adjustScroll();
		super.doLayout();
	}

	/**
	 * fixes flicker when switching to an already opened tab, #scroll is too late, but is necessary anyway for some reason
	 */
	public void adjustScroll() {
		if (project.isDisposed()) {
			return;
		}
		SwitchRequest switchingRequest = EditorGroupManager.getInstance(project).getSwitchingRequest(file);
		if (switchingRequest != null) {
			int myScrollOffset = switchingRequest.myScrollOffset;
			int relativeScroll = myScrollOffset - getMyScrollOffset();
			mySingleRowLayout.scroll(relativeScroll);
		}
	}

	public void scroll(int myScrollOffset) {
		if (mySingleRowLayout.myLastSingRowLayout != null) {
			int relativeScroll = myScrollOffset - getMyScrollOffset();
			mySingleRowLayout.scroll(relativeScroll);
			revalidateAndRepaint(false);
		}
	}


	@Override
	public String toString() {
		return "EditorGroups.MyJBEditorTabs visible=" + myVisibleInfos + " selected=" + mySelectedInfo;
	}


	public void setMyPopupInfo(TabInfo myPopupInfo) {
		this.myPopupInfo = myPopupInfo;
	}


	public void setMySelectedInfo(krasa.editorGroups.tabs2.TabInfo mySelectedInfo) {
		this.mySelectedInfo = mySelectedInfo;
	}

	public int getMyScrollOffset() {
		if (mySingleRowLayout instanceof ScrollableSingleRowLayout) {
			ScrollableSingleRowLayout mySingleRowLayout = (ScrollableSingleRowLayout) this.mySingleRowLayout;
			return mySingleRowLayout.getMyScrollOffset();
		}
		return 0;
	}


	private static class MyMouseAdapter extends MouseAdapter {
		private final MouseListener mouseListener;

		public MyMouseAdapter(MouseListener mouseListener) {
			this.mouseListener = mouseListener;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			//fix for - Ctrl + Mouse Click events are also consumed by the editor
			IdeEventQueue.getInstance().blockNextEvents(e, IdeEventQueue.BlockMode.ACTIONS);
			mouseListener.mouseClicked(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			IdeEventQueue.getInstance().blockNextEvents(e, IdeEventQueue.BlockMode.ACTIONS);
			mouseListener.mousePressed(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			IdeEventQueue.getInstance().blockNextEvents(e, IdeEventQueue.BlockMode.ACTIONS);
			mouseListener.mouseReleased(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			super.mouseEntered(e);
			mouseListener.mouseEntered(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			super.mouseExited(e);
			mouseListener.mouseExited(e);
		}

	}
}
