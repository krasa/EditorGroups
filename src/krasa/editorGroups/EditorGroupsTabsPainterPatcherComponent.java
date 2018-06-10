package krasa.editorGroups;

import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.tabs.impl.DefaultEditorTabsPainter;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.ui.tabs.impl.JBEditorTabsPainter;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.WeakHashMap;


public final class EditorGroupsTabsPainterPatcherComponent implements ApplicationComponent {
	private static final Logger LOG = Logger.getInstance(EditorGroupsTabsPainterPatcherComponent.class);

	private final ApplicationConfigurationComponent config;

	private WeakHashMap<EditorGroupsTabsPainter, String> map = new WeakHashMap<>();

	public EditorGroupsTabsPainterPatcherComponent(ApplicationConfigurationComponent config) {
		this.config = config;
	}


	public static EditorGroupsTabsPainterPatcherComponent getInstance() {
		return ServiceManager.getService(EditorGroupsTabsPainterPatcherComponent.class);
	}

	public static void onColorsChanged(ApplicationConfiguration applicationConfiguration) {
		ApplicationConfiguration.Tabs tabs = applicationConfiguration.getTabs();

		EditorGroupsTabsPainterPatcherComponent instance = getInstance();
		for (EditorGroupsTabsPainter editorGroupsTabsPainter : instance.map.keySet()) {
			setColors(tabs, editorGroupsTabsPainter);
		}
	}

	@SuppressWarnings("UseJBColor")
	private static void setColors(ApplicationConfiguration.Tabs tabs, EditorGroupsTabsPainter editorGroupsTabsPainter) {
		if (editorGroupsTabsPainter != null) {
			if (editorGroupsTabsPainter instanceof DarculaEditorGroupsTabsPainter) {
				editorGroupsTabsPainter.setOpacity(tabs.getDarcula_opacity());
				editorGroupsTabsPainter.setMask(new Color(tabs.getDarcula_mask()));
			} else {
				editorGroupsTabsPainter.setOpacity(tabs.getOpacity());
				editorGroupsTabsPainter.setMask(new Color(tabs.getMask()));
			}
			JBEditorTabs painterTabs = editorGroupsTabsPainter.getTabs();
			if (!painterTabs.isDisposed()) {
				painterTabs.repaint();
			}
		}
	}


	@Override
	public void initComponent() {
		final MessageBus bus = ApplicationManagerEx.getApplicationEx().getMessageBus();

		final MessageBusConnection connect = bus.connect();
		connect.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			@Override
			public void selectionChanged(@NotNull final FileEditorManagerEvent event) {
				if (!config.getState().getTabs().isPatchPainter()) {
					return;
				}

				final FileEditor editor = event.getNewEditor();
				if (editor != null) {
					Component component = editor.getComponent();
					while (component != null) {
						if (component instanceof JBEditorTabs) {
							patchPainter((JBEditorTabs) component);
							return;
						}
						component = component.getParent();
					}
				}
			}
		});

	}

	private void patchPainter(final JBEditorTabs component) {
		if (alreadyPatched(component)) return;

		final EditorGroupsTabsPainter tabsPainter = new EditorGroupsTabsPainter(component);
		init(tabsPainter);

		final EditorGroupsTabsPainter darculaTabsPainter = new DarculaEditorGroupsTabsPainter(component);
		init(darculaTabsPainter);


		LOG.info("HACK: Overriding JBEditorTabsPainters");
		ReflectionUtil.setField(JBEditorTabs.class, component, JBEditorTabsPainter.class, "myDefaultPainter", tabsPainter);
		ReflectionUtil.setField(JBEditorTabs.class, component, JBEditorTabsPainter.class, "myDarkPainter", darculaTabsPainter);
	}

	private boolean alreadyPatched(JBEditorTabs component) {
		if (UIUtil.isUnderDarcula()) {
			JBEditorTabsPainter painter = ReflectionUtil.getField(JBEditorTabs.class, component, JBEditorTabsPainter.class, "myDarkPainter");
			if (painter instanceof EditorGroupsTabsPainter) {
				return true;
			}
			if (!painter.getClass().getPackage().getName().startsWith("com.intellij")) { //some other plugin
				return true;
			}
		} else {
			JBEditorTabsPainter painter = ReflectionUtil.getField(JBEditorTabs.class, component, JBEditorTabsPainter.class, "myDefaultPainter");
			if (painter instanceof EditorGroupsTabsPainter) {
				return true;
			}
			if (!painter.getClass().getPackage().getName().startsWith("com.intellij")) { //some other plugin
				return true;
			}
		}
		return false;
	}


	public void init(EditorGroupsTabsPainter tabsPainter) {
		setColors(config.getState().getTabs(), tabsPainter);
		map.put(tabsPainter, null);
	}


	public static class EditorGroupsTabsPainter extends DefaultEditorTabsPainter {

		private JBEditorTabs tabs;

		protected Color mask;
		protected int opacity;

		/**
		 * @see DefaultEditorTabsPainter#getInactiveMaskColor()
		 */
		public EditorGroupsTabsPainter(final JBEditorTabs tabs) {
			super(tabs);
			this.tabs = tabs;
			mask = ApplicationConfiguration.Tabs.DEFAULT_MASK;
			opacity = ApplicationConfiguration.Tabs.DEFAULT_OPACITY;
		}

		public JBEditorTabs getTabs() {
			return tabs;
		}


		protected Color getInactiveMaskColor() {
			return ColorUtil.withAlpha(mask, (opacity / 100.0));
		}

		public void setMask(Color mask) {
			this.mask = mask;
		}

		public void setOpacity(int opacity) {
			this.opacity = opacity;
		}
	}


	/**
	 * @see com.intellij.ui.tabs.impl.DarculaEditorTabsPainter
	 */
	public static class DarculaEditorGroupsTabsPainter extends EditorGroupsTabsPainter {


		public DarculaEditorGroupsTabsPainter(JBEditorTabs component) {
			super(component);
			mask = ApplicationConfiguration.Tabs.DEFAULT_DARCULA_MASK;
			opacity = ApplicationConfiguration.Tabs.DEFAULT_DARCULA_OPACITY;
			myDefaultTabColor = ApplicationConfiguration.Tabs.DEFAULT_DARCULA_TAB_COLOR;
		}

		@Override
		protected Color getDefaultTabColor() {
			if (myDefaultTabColor != null) {
				return myDefaultTabColor;
			}
			return new Color(0x515658);
		}
		

	}
}

