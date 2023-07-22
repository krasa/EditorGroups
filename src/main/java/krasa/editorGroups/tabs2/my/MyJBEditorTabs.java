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
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.SwitchRequest;
import krasa.editorGroups.tabs2.KrTabInfo;
import krasa.editorGroups.tabs2.KrTabPainter;
import krasa.editorGroups.tabs2.impl.KrEditorTabs;
import krasa.editorGroups.tabs2.impl.KrTabLabel;
import krasa.editorGroups.tabs2.impl.singleRow.KrScrollableSingleRowLayout;
import krasa.editorGroups.tabs2.impl.singleRow.KrSingleRowLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class MyJBEditorTabs extends KrEditorTabs {
  private static final Logger LOG = Logger.getInstance(MyJBEditorTabs.class);

  private final Project project;
  private final VirtualFile file;
  private final KrSingleRowLayout mySingleRowLayout = createSingleRowLayout();

  public MyJBEditorTabs(Project project, @NotNull ActionManager actionManager, IdeFocusManager focusManager, @NotNull Disposable parent, VirtualFile file) {
    super(project, actionManager, focusManager, parent);
    this.project = project;
    this.file = file;
    patchMouseListener(this);
  }

  protected @NotNull KrTabLabel createTabLabel(@NotNull KrTabInfo info) {
    KrTabLabel tabLabel = new KrTabLabel(this, info);
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
  public void revalidateAndRepaint(boolean layoutNow) {
    if (bulkUpdate) {        //performance optimization
      return;
    }
    super.revalidateAndRepaint(layoutNow);
  }


  protected void updateSideComponent(KrTabInfo tabInfo) {
    //performance optimization
  }

  @Nullable
  @Override
  public KrTabInfo getSelectedInfo() {
    KrTabInfo selectedInfo = super.getSelectedInfo();
    if (selectedInfo instanceof EditorGroupPanel.MyTabInfo) {
      boolean selectable = ((EditorGroupPanel.MyTabInfo) selectedInfo).selectable;
      if (!selectable) {
        return null;
      }
    }
    return selectedInfo;
  }

  protected KrTabPainter createTabPainter() {
    return new EditorGroupsJBDefaultTabPainter();
  }


  /**
   * com.intellij.util.IncorrectOperationException: Sorry but parent: EditorGroups.MyJBEditorTabs visible=[] selected=null has already been disposed (see the cause for stacktrace) so the child: Animator 'KrTabs Attractions' @1845106519 (stopped) will never be disposed
   * at com.intellij.openapi.util.objectTree.ObjectTree.register(ObjectTree.java:61)
   * at com.intellij.openapi.util.Disposer.register(Disposer.java:92)
   * at krasa.editorGroups.tabs2.impl.JBTabsImpl$7.initialize(JBTabsImpl.java:340)
   * at krasa.editorGroups.tabs2.impl.JBTabsImpl$7.initialize(JBTabsImpl.java:333)
   */

  @Override
  protected @NotNull KrSingleRowLayout createSingleRowLayout() {
    return new KrScrollableSingleRowLayout(this);
  }

  @Override
  public boolean isActiveTabs(KrTabInfo info) {
    return true;
  }


  public boolean isSelectionClick(final MouseEvent e, boolean canBeQuick) {
    if (e.getClickCount() == 1 || canBeQuick) {
      if (!e.isPopupTrigger()) {
        return e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON2 && BitUtil.isSet(e.getModifiersEx(), InputEvent.ALT_DOWN_MASK);
      }
    }

    return false;
  }

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
    if (mySingleRowLayout.lastSingleRowLayout != null) {
      int relativeScroll = myScrollOffset - getMyScrollOffset();
      mySingleRowLayout.scroll(relativeScroll);
      revalidateAndRepaint(false);
    }
  }


  @Override
  public @NotNull String toString() {
    return "EditorGroups.MyJBEditorTabs visible=" + getVisibleInfos() + " selected=" + getSelectedInfo();
  }


  public void setMyPopupInfo(KrTabInfo myPopupInfo) {
    this.setPopupInfo(myPopupInfo);
  }


  public int getMyScrollOffset() {
    if (mySingleRowLayout instanceof KrScrollableSingleRowLayout) {
      KrScrollableSingleRowLayout mySingleRowLayout = (KrScrollableSingleRowLayout) this.mySingleRowLayout;
      return mySingleRowLayout.getScrollOffset();
    }
    return 0;
  }

  @Override
  public boolean isCycleRoot() {
    return super.isCycleRoot();
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
