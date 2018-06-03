package krasa.editorGroups.support;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"UseJBColor"})
public class Utils {

	public static Map<String, Color> colorMap;

	/**
	 * not good enough for UI forms sometimes
	 */
	@Nullable
	public static VirtualFile getFileFromTextEditor(Project project, FileEditor textEditor) {
		VirtualFile file = textEditor.getFile();
		if (file != null) {
			return file;
		}
		return FileEditorManagerEx.getInstanceEx(project).getFile(textEditor);
	}

	@Nullable
	public static VirtualFile getFileByUrl(String url) {
		return VirtualFileManagerEx.getInstance().findFileByUrl(url);
	}

	public static String getFileContent(String ownerPath) {
		VirtualFile fileByPath = getFileByPath(ownerPath);
		if (fileByPath == null) {
			return null;
		}
		return getFileContent(fileByPath);
	}

	@Nullable
	public static String getFileContent(VirtualFile url) {
		Document document = FileDocumentManager.getInstance().getDocument(url);
		if (document != null) {
			return document.getText();
		}
		return null;
	}

	@Nullable
	public static VirtualFile getVirtualFileByAbsolutePath(@NotNull String s) {
		VirtualFile fileByPath = null;
		if (new File(s).exists()) {
			fileByPath = getFileByPath(s);
		}
		return fileByPath;
	}


	@Nullable
	public static VirtualFile getFileByPath(@NotNull String path) {
		return getFileByPath(path, (VirtualFile) null);
	}

	@Nullable
	public static VirtualFile getFileByPath(@NotNull String path, @Nullable VirtualFile currentFile) {
		VirtualFile file = null;
		if (FileUtil.isUnixAbsolutePath(path) || FileUtil.isWindowsAbsolutePath(path)) {
			file = VirtualFileManagerEx.getInstance().findFileByUrl("file://" + path);
		} else if (path.startsWith("file://")) {
			file = VirtualFileManagerEx.getInstance().findFileByUrl(path);
		} else if (currentFile != null) {
			VirtualFile parent = currentFile.getParent();
			if (parent != null) {
				file = parent.findFileByRelativePath(path);
			}
		}

		return file;
	}

	@Nullable
	public static VirtualFile getFileByPath(@NotNull String path, @NotNull EditorGroup group) {
		String ownerPath = group.getOwnerPath();
		VirtualFile virtualFile = getFileByPath(ownerPath);

		return getFileByPath(path, virtualFile);
	}

	public static boolean isTheSameFile(@NotNull String path, @NotNull VirtualFile file) {
		if (file != null) {
			return path.equals(file.getCanonicalPath());
		}
		return false;
	}

	@NotNull
	public static String toPresentableName(String path) {
		String name = path;
		int i = StringUtil.lastIndexOfAny(path, "\\/");
		if (i > 0) {
			name = path.substring(i + 1);
		}
		return name;
	}

	static {
		colorMap = new HashMap<>();
		// https://www.w3schools.com/colors/colors_names.asp
		colorMap.put("black", new Color(0x000000));
		colorMap.put("deepskyblue", new Color(0x00bfff));
		colorMap.put("mediumblue", new Color(0x0000cd));
		colorMap.put("darkturquoise", new Color(0x00ced1));
		colorMap.put("mediumspringgreen", new Color(0x00fa9a));
		colorMap.put("blue", new Color(0x0000ff));
		colorMap.put("lime", new Color(0x00ff00));
		colorMap.put("springgreen", new Color(0x00ff7f));
		colorMap.put("aqua", new Color(0x00ffff));
		colorMap.put("cyan", new Color(0x00ffff));
		colorMap.put("dodgerblue", new Color(0x1e90ff));
		colorMap.put("seagreen", new Color(0x2e8b57));
		colorMap.put("darkslategray", new Color(0x2f4f4f));
		colorMap.put("darkslategrey", new Color(0x2f4f4f));
		colorMap.put("mediumseagreen", new Color(0x3cb371));
		colorMap.put("indigo", new Color(0x4b0082));
		colorMap.put("cadetblue", new Color(0x5f9ea0));
		colorMap.put("slateblue", new Color(0x6a5acd));
		colorMap.put("olivedrab", new Color(0x6b8e23));
		colorMap.put("mediumslateblue", new Color(0x7b68ee));
		colorMap.put("lawngreen", new Color(0x7cfc00));
		colorMap.put("chartreuse", new Color(0x7fff00));
		colorMap.put("aquamarine", new Color(0x7fffd4));
		colorMap.put("blueviolet", new Color(0x8a2be2));
		colorMap.put("darkblue", new Color(0x00008b));
		colorMap.put("darkred", new Color(0x8b0000));
		colorMap.put("darkcyan", new Color(0x008b8b));
		colorMap.put("darkmagenta", new Color(0x8b008b));
		colorMap.put("saddlebrown", new Color(0x8b4513));
		colorMap.put("darkseagreen", new Color(0x8fbc8f));
		colorMap.put("yellowgreen", new Color(0x9acd32));
		colorMap.put("lightseagreen", new Color(0x20b2aa));
		colorMap.put("limegreen", new Color(0x32cd32));
		colorMap.put("turquoise", new Color(0x40e0d0));
		colorMap.put("mediumturquoise", new Color(0x48d1cc));
		colorMap.put("mediumaquamarine", new Color(0x66cdaa));
		colorMap.put("navy", new Color(0x000080));
		colorMap.put("skyblue", new Color(0x87ceeb));
		colorMap.put("lightskyblue", new Color(0x87cefa));
		colorMap.put("lightgreen", new Color(0x90ee90));
		colorMap.put("palegreen", new Color(0x98fb98));
		colorMap.put("forestgreen", new Color(0x228b22));
		colorMap.put("darkslateblue", new Color(0x483d8b));
		colorMap.put("darkolivegreen", new Color(0x556b2f));
		colorMap.put("royalblue", new Color(0x4169e1));
		colorMap.put("steelblue", new Color(0x4682b4));
		colorMap.put("darkgreen", new Color(0x006400));
		colorMap.put("cornflowerblue", new Color(0x6495ed));
		colorMap.put("green", new Color(0x008000));
		colorMap.put("teal", new Color(0x008080));
		colorMap.put("mediumpurple", new Color(0x9370d8));
		colorMap.put("darkviolet", new Color(0x9400d3));
		colorMap.put("darkorchid", new Color(0x9932cc));
		colorMap.put("midnightblue", new Color(0x191970));
		colorMap.put("dimgray", new Color(0x696969));
		colorMap.put("dimgrey", new Color(0x696969));
		colorMap.put("slategray", new Color(0x708090));
		colorMap.put("slategrey", new Color(0x708090));
		colorMap.put("lightslategray", new Color(0x778899));
		colorMap.put("lightslategrey", new Color(0x778899));
		colorMap.put("maroon", new Color(0x800000));
		colorMap.put("purple", new Color(0x800080));
		colorMap.put("olive", new Color(0x808000));
		colorMap.put("gray", new Color(0x808080));
		colorMap.put("grey", new Color(0x808080));
		colorMap.put("darkgray", new Color(0xa9a9a9));
		colorMap.put("darkgrey", new Color(0xa9a9a9));
		colorMap.put("brown", new Color(0xa52a2a));
		colorMap.put("sienna", new Color(0xa0522d));
		colorMap.put("lightblue", new Color(0xadd8e6));
		colorMap.put("greenyellow", new Color(0xadff2f));
		colorMap.put("paleturquoise", new Color(0xafeeee));
		colorMap.put("lightsteelblue", new Color(0xb0c4de));
		colorMap.put("powderblue", new Color(0xb0e0e6));
		colorMap.put("darkgoldenrod", new Color(0xb8860b));
		colorMap.put("firebrick", new Color(0xb22222));
		colorMap.put("mediumorchid", new Color(0xba55d3));
		colorMap.put("rosybrown", new Color(0xbc8f8f));
		colorMap.put("darkkhaki", new Color(0xbdb76b));
		colorMap.put("silver", new Color(0xc0c0c0));
		colorMap.put("mediumvioletred", new Color(0xc71585));
		colorMap.put("indianred", new Color(0xcd5c5c));
		colorMap.put("peru", new Color(0xcd853f));
		colorMap.put("tan", new Color(0xd2b48c));
		colorMap.put("lightgray", new Color(0xd3d3d3));
		colorMap.put("lightgrey", new Color(0xd3d3d3));
		colorMap.put("thistle", new Color(0xd8bfd8));
		colorMap.put("chocolate", new Color(0xd2691e));
		colorMap.put("palevioletred", new Color(0xd87093));
		colorMap.put("orchid", new Color(0xda70d6));
		colorMap.put("goldenrod", new Color(0xdaa520));
		colorMap.put("crimson", new Color(0xdc143c));
		colorMap.put("gainsboro", new Color(0xdcdcdc));
		colorMap.put("plum", new Color(0xdda0dd));
		colorMap.put("burlywood", new Color(0xdeb887));
		colorMap.put("lightcyan", new Color(0xe0ffff));
		colorMap.put("lavender", new Color(0xe6e6fa));
		colorMap.put("darksalmon", new Color(0xe9967a));
		colorMap.put("violet", new Color(0xee82ee));
		colorMap.put("palegoldenrod", new Color(0xeee8aa));
		colorMap.put("khaki", new Color(0xf0e68c));
		colorMap.put("aliceblue", new Color(0xf0f8ff));
		colorMap.put("honeydew", new Color(0xf0fff0));
		colorMap.put("azure", new Color(0xf0ffff));
		colorMap.put("sandybrown", new Color(0xf4a460));
		colorMap.put("wheat", new Color(0xf5deb3));
		colorMap.put("beige", new Color(0xf5f5dc));
		colorMap.put("whitesmoke", new Color(0xf5f5f5));
		colorMap.put("mintcream", new Color(0xf5fffa));
		colorMap.put("ghostwhite", new Color(0xf8f8ff));
		colorMap.put("lightcoral", new Color(0xf08080));
		colorMap.put("salmon", new Color(0xfa8072));
		colorMap.put("antiquewhite", new Color(0xfaebd7));
		colorMap.put("linen", new Color(0xfaf0e6));
		colorMap.put("lightgoldenrodyellow", new Color(0xfafad2));
		colorMap.put("oldlace", new Color(0xfdf5e6));
		colorMap.put("red", new Color(0xff0000));
		colorMap.put("fuchsia", new Color(0xff00ff));
		colorMap.put("magenta", new Color(0xff00ff));
		colorMap.put("coral", new Color(0xff7f50));
		colorMap.put("darkorange", new Color(0xff8c00));
		colorMap.put("hotpink", new Color(0xff69b4));
		colorMap.put("deeppink", new Color(0xff1493));
		colorMap.put("orangered", new Color(0xff4500));
		colorMap.put("tomato", new Color(0xff6347));
		colorMap.put("lightsalmon", new Color(0xffa07a));
		colorMap.put("orange", new Color(0xffa500));
		colorMap.put("lightpink", new Color(0xffb6c1));
		colorMap.put("pink", new Color(0xffc0cb));
		colorMap.put("gold", new Color(0xffd700));
		colorMap.put("peachpuff", new Color(0xffdab9));
		colorMap.put("navajowhite", new Color(0xffdead));
		colorMap.put("moccasin", new Color(0xffe4b5));
		colorMap.put("bisque", new Color(0xffe4c4));
		colorMap.put("mistyrose", new Color(0xffe4e1));
		colorMap.put("blanchedalmond", new Color(0xffebcd));
		colorMap.put("papayawhip", new Color(0xffefd5));
		colorMap.put("lavenderblush", new Color(0xfff0f5));
		colorMap.put("seashell", new Color(0xfff5ee));
		colorMap.put("cornsilk", new Color(0xfff8dc));
		colorMap.put("lemonchiffon", new Color(0xfffacd));
		colorMap.put("floralwhite", new Color(0xfffaf0));
		colorMap.put("snow", new Color(0xfffafa));
		colorMap.put("yellow", new Color(0xffff00));
		colorMap.put("lightyellow", new Color(0xffffe0));
		colorMap.put("ivory", new Color(0xfffff0));
		colorMap.put("white", new Color(0xffffff));
	}

}
