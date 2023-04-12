package krasa.editorGroups.support;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import krasa.editorGroups.EditorGroupPanel;

import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;

public class HackedJBScrollPane extends JBScrollPane {
	private static final Logger LOG = Logger.getInstance(JBScrollPane.class);

	public HackedJBScrollPane(EditorGroupPanel panel) {
		super(panel);
	}

	@Override
	public void setUI(ScrollPaneUI ui) {
		super.setUI(ui);
		if (ui instanceof BasicScrollPaneUI) {
			try {
				Field field = BasicScrollPaneUI.class.getDeclaredField("mouseScrollListener");
				field.setAccessible(true);
				Object value = field.get(ui);
				if (value instanceof MouseWheelListener) {
					MouseWheelListener oldListener = (MouseWheelListener) value;
					MouseWheelListener newListener = event -> {
						if (isScrollEvent(event)) {
							try {
								//WE HAVE THE BEST HACKS !!!
								Field modifiers = InputEvent.class.getDeclaredField("modifiers");
								modifiers.setAccessible(true);
								modifiers.setInt(event, modifiers.getModifiers() | InputEvent.SHIFT_MASK);
							} catch (Exception e) {
								LOG.error(e);
							}

							oldListener.mouseWheelMoved(event);
						}
					};
					field.set(ui, newListener);
					// replace listener if field updated successfully
					removeMouseWheelListener(oldListener);
					addMouseWheelListener(newListener);
				}
			} catch (Exception exception) {
				LOG.warn(exception);
			}
		}
	}
}
