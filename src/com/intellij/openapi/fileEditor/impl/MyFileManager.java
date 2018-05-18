package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.lang.reflect.Method;

public class MyFileManager {
	private static final Logger LOG = Logger.getInstance(MyFileManager.class);

	public static void updateTitle(Project project, VirtualFile file) {
//		System.err.println("updateTitle "+file.getName());
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);

		if (false) { //API watch
			manager.updateFileName(file);
		}

		try {
			Method updateFileName = FileEditorManagerImpl.class.getDeclaredMethod("updateFileName", VirtualFile.class);
			updateFileName.setAccessible(true);
			updateFileName.invoke(manager, file);
		} catch (Exception e) {
			LOG.error(e);
		}
	}
}
