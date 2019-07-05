package krasa.editorGroups.gui;

import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.ErrorLabel;
import com.intellij.ui.components.JBTextField;
import krasa.editorGroups.model.RegexGroupModel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelEditor extends DialogWrapper {
	private JTextField regex;
	private JComboBox<RegexGroupModel.Scope> scopeCombo;
	private JPanel root;
	private ErrorLabel error;
	//	private FileTextField fileName;
	private JPanel testResult;
	private JPanel fileNamePanel;
	private EditorImpl myEditor;
	//	private TextFieldWithBrowseButton textFieldWithBrowseButton;
	private JBTextField fileNameField;
	private static String _fileName = "";

	private void createUIComponents() {
		myEditor = (EditorImpl) createEditorPreview();
		testResult = (JPanel) myEditor.getComponent();
		testResult.setPreferredSize(new Dimension(400, 200));
	}

	public ModelEditor(String title, String regex, RegexGroupModel.Scope scope) {
		super(true);
//		FileChooserDescriptor singleFileDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
//		Project currentContextProject = ProjectUtils.getCurrentContextProject();
//		if (currentContextProject != null) {
//			singleFileDescriptor.setRoots(ProjectRootManager.getInstance(currentContextProject).getContentRoots());
//		}
//		
//		fileName = FileChooserFactoryImpl.getInstance().createFileTextField(singleFileDescriptor, null);
//                                                              

//		textFieldWithBrowseButton = new TextFieldWithBrowseButton(fileName.getField());
//
//		fileName.getField().setText("");
//
//
//		textFieldWithBrowseButton.addBrowseFolderListener(new TextBrowseFolderListener(singleFileDescriptor, currentContextProject) {
//															  @NotNull
//															  @Override
//															  protected String chosenFileToResultingText(@NotNull VirtualFile chosenFile) {
//																  return chosenFile.getName();
//															  }
//														  }
//		);


//		fileNamePanel.add(textFieldWithBrowseButton, BorderLayout.CENTER);
//		fileNamePanel.add(fileName.getField(), BorderLayout.CENTER);
		fileNameField = new JBTextField(_fileName);
		fileNamePanel.add(fileNameField, BorderLayout.CENTER);


		error.setForeground(DialogWrapper.ERROR_FOREGROUND_COLOR);
		setTitle(title);
		this.regex.setNextFocusableComponent(this.regex);
		scopeCombo.setModel(new EnumComboBoxModel<>(RegexGroupModel.Scope.class));

		this.regex.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			public void textChanged(@NotNull DocumentEvent event) {
				updateControls();
				updateTest();
			}
		});
//		this.fileName.getField().getDocument().addDocumentListener(new DocumentAdapter() {
//			@Override
//			public void textChanged(@NotNull DocumentEvent event) {
//				updateTest();
//			}
//		});
		fileNameField.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			public void textChanged(@NotNull DocumentEvent event) {
				updateTest();
			}
		});

		this.regex.setText(regex);
		this.scopeCombo.setSelectedItem(scope);
		init();
		updateControls();
	}


	private void updateTest() {
		StringBuilder sb = new StringBuilder();
		try {
			String text = regex.getText();
			String fileName = getFileName();
			if (StringUtils.isNotBlank(text) && StringUtils.isNotBlank(fileName)) {
				Pattern compile = Pattern.compile(text);
				Matcher matcher = compile.matcher(fileName);
				sb.append("Matches: ").append(matcher.matches()).append("\n");
				int groups = matcher.groupCount();
				if (matcher.matches()) {
					sb.append("Groups: ").append(groups).append("\n");
					for (int j = 1; j <= groups; j++) {
						sb.append("Group ").append(j).append(": ").append(matcher.group(j)).append("\n");
					}
				}
			}
		} catch (Throwable e) {
			sb.append(e.toString());
		}

		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			@Override
			public void run() {
				myEditor.getDocument().setText(sb.toString());
				testResult.validate();
				testResult.repaint();
			}
		});

	}

	@Nullable
	private String getFileName() {
//		String fileName = null;
//		VirtualFile selectedFile1 = this.fileName.getSelectedFile();
//		if (selectedFile1 != null) {
//			fileName = selectedFile1.getName();
//		}
//		return fileName;
		_fileName = fileNameField.getText();
		return _fileName;
	}

	@NotNull
	private static Editor createEditorPreview() {
		EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
		ColorAndFontOptions options = new ColorAndFontOptions();
		options.reset();
		options.selectScheme(scheme.getName());
		return createPreviewEditor("", scheme, false);
	}

	static Editor createPreviewEditor(String text, EditorColorsScheme scheme, boolean editable) {
		EditorFactory editorFactory = EditorFactory.getInstance();
		Document editorDocument = editorFactory.createDocument(text);
		EditorEx editor = (EditorEx) (editable ? editorFactory.createEditor(editorDocument) : editorFactory.createViewer(editorDocument));
		editor.setColorsScheme(scheme);
		EditorSettings settings = editor.getSettings();
		settings.setLineNumbersShown(false);
		settings.setWhitespacesShown(false);
		settings.setLineMarkerAreaShown(false);
		settings.setIndentGuidesShown(false);
		settings.setFoldingOutlineShown(false);
		settings.setAdditionalColumnsCount(0);
		settings.setAdditionalLinesCount(1);
		settings.setRightMarginShown(false);

		return editor;
	}

	private void updateControls() {
		getOKAction().setEnabled(isRegexOK(getRegex()));
	}

	private boolean isRegexOK(String regex) {
		error.setText("");
		if (regex.isEmpty()) {
			return false;
		}
		try {
			Pattern compile = Pattern.compile(regex);
			return true;
		} catch (Exception e) {
			root.revalidate();
			root.repaint();
			error.setText("Regex not valid");
			return false;
		}
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		return regex;
	}

	@Override
	protected String getHelpId() {
		return null;
	}

	@Override
	protected void doOKAction() {
		if (!isRegexOK(getRegex())) return;
		super.doOKAction();
	}

	public String getRegex() {
		return regex.getText().trim();
	}

	public RegexGroupModel.Scope getScopeCombo() {
		return (RegexGroupModel.Scope) scopeCombo.getSelectedItem();
	}

	@Override
	protected JComponent createCenterPanel() {
		return root;
	}

	@Nullable
	protected String getDimensionServiceKey() {
//		return null; 	
		return "krasa.editorGroups.ModelEditor3";
	}


}