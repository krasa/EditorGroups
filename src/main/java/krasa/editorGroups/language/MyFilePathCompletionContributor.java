package krasa.editorGroups.language;

import com.google.common.collect.ObjectArrays;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelperRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileSystemItemUtil;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.intellij.openapi.diagnostic.Logger.getInstance;
import static com.intellij.patterns.PlatformPatterns.psiElement;

/* @group.disable */
public class MyFilePathCompletionContributor extends CompletionContributor {
  private static final Logger LOG = getInstance(MyFilePathCompletionContributor.class);

  public MyFilePathCompletionContributor() {
    CompletionProvider<CompletionParameters> provider = new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull final CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull final CompletionResultSet _result) {

        @NotNull final CompletionResultSet result = _result.caseInsensitive();
        final PsiElement e = parameters.getPosition();
        final Project project = e.getProject();

        String text = parameters.getOriginalFile().getText();
        int offset = Math.min(text.length(), parameters.getOffset());


        int from = text.lastIndexOf('\n', offset - 1) + 1;
        String substring = text.substring(from, offset);
        boolean root = false;
        int keywordEndIndex = StringUtil.indexOfSubstringEnd(substring, "@group.related ");
        if (keywordEndIndex < 0) {
          keywordEndIndex = StringUtil.indexOfSubstringEnd(substring, "@group.root ");
          root = true;
        }
        if (keywordEndIndex < 0) {
          return;
        }

        int j = text.lastIndexOf('\n', offset - 1) + 1;
        String prefix = text.substring(Math.max(from + keywordEndIndex, j), offset);
        prefix = prefix.trim();
        String macro = null;
        if (prefix.startsWith("MODULE/")) {
          macro = "MODULE";
          prefix = StringUtil.substringAfter(prefix, "MODULE/");
        } else if (prefix.startsWith("PROJECT/")) {
          macro = "PROJECT";
          prefix = StringUtil.substringAfter(prefix, "PROJECT/");
        } else if (!root && prefix.startsWith("*/")) {
          macro = "*";
          prefix = StringUtil.substringAfter(prefix, "*/");
        }
        Module moduleForFile = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(parameters.getOriginalFile().getVirtualFile());

        List<String> pathPrefixParts = null;
        String pathPrefix;
        int lastSlashIndex;
        if ((lastSlashIndex = Objects.requireNonNull(prefix).lastIndexOf('/')) != -1) {
          pathPrefix = prefix.substring(0, lastSlashIndex);
          pathPrefixParts = StringUtil.split(pathPrefix, "/");
          prefix = prefix.substring(lastSlashIndex + 1);
        }

        final CompletionResultSet __result = result.withPrefixMatcher(prefix).caseInsensitive();

        final PsiFile originalFile = parameters.getOriginalFile();
        final VirtualFile contextFile = originalFile.getVirtualFile();
        if (contextFile != null) {
          final String[] fileNames = getAllNames(project);
          final Set<String> resultNames = new TreeSet<>();
          for (String fileName : fileNames) {
            if (filenameMatchesPrefixOrType(fileName, prefix, parameters.getInvocationCount())) {
              resultNames.add(fileName);
            }
          }

          final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();

          final Module contextModule = index.getModuleForFile(contextFile);
          if (contextModule != null) {
            final List<FileReferenceHelper> helpers = FileReferenceHelperRegistrar.getHelpers(originalFile);

            final GlobalSearchScope scope = ProjectScope.getProjectScope(project);
            for (final String name : resultNames) {
              ProgressManager.checkCanceled();

              PsiFileSystemItem[] folders = FilenameIndex.getFilesByName(project, name, scope, true);
              PsiFileSystemItem[] files = FilenameIndex.getFilesByName(project, name, scope, false);
              PsiFileSystemItem[] concat = ObjectArrays.concat(folders, files, PsiFileSystemItem.class);

              for (final PsiFileSystemItem file : concat) {
                ProgressManager.checkCanceled();

                final VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile == null || !virtualFile.isValid() || Comparing.equal(virtualFile, contextFile)) {
                  continue;
                }
                List<FileReferenceHelper> helperList = new ArrayList<>();
                for (FileReferenceHelper contextHelper : helpers) {
                  ProgressManager.checkCanceled();

                  if (contextHelper.isMine(project, virtualFile)) {
                    if (pathPrefixParts == null ||
                      fileMatchesPathPrefix(contextHelper.getPsiFileSystemItem(project, virtualFile), pathPrefixParts)) {
                      helperList.add(contextHelper);
                    }
                  }
                }
                if (!helperList.isEmpty()) {
                  __result.addElement(new FilePathLookupItem(parameters.getOriginalFile(), file, macro, moduleForFile, helperList));
                }
              }
            }
          }
        }


      }
    };
    extend(CompletionType.BASIC, psiElement(), provider);
  }

  private static boolean filenameMatchesPrefixOrType(final String fileName, final String prefix, final int invocationCount) {
    final boolean prefixMatched = prefix.isEmpty() || StringUtil.startsWithIgnoreCase(fileName, prefix);
    if (prefixMatched && (FileType.EMPTY_ARRAY.length == 0 || invocationCount > 2)) return true;

    if (prefixMatched) {
      final String extension = FileUtilRt.getExtension(fileName);
      if (extension.isEmpty()) return false;

      for (final FileType fileType : FileType.EMPTY_ARRAY) {
        for (final FileNameMatcher matcher : FileTypeManager.getInstance().getAssociations(fileType)) {
          if (matcher.acceptsCharSequence(fileName)) return true;
        }
      }
    }

    return false;
  }

  private static boolean fileMatchesPathPrefix(@Nullable final PsiFileSystemItem file, @NotNull final List<String> pathPrefix) {
    if (file == null) return false;

    final List<String> contextParts = new ArrayList<>();
    PsiFileSystemItem parentFile = file;
    PsiFileSystemItem parent;
    while ((parent = parentFile.getParent()) != null) {
      if (!parent.getName().isEmpty()) contextParts.add(0, parent.getName().toLowerCase());
      parentFile = parent;
    }

    final String path = StringUtil.join(contextParts, "/");

    int nextIndex = 0;
    for (@NonNls final String s : pathPrefix) {
      if (s.equals("..")) {
        continue;
      }
      if ((nextIndex = path.indexOf(s.toLowerCase(), nextIndex)) == -1) return false;
    }

    return true;
  }

  private static String[] getAllNames(@NotNull final Project project) {
    Set<String> names = new HashSet<>();
    final ChooseByNameContributor[] nameContributors = ChooseByNameContributor.FILE_EP_NAME.getExtensions();
    for (final ChooseByNameContributor contributor : nameContributors) {
      try {
        ContainerUtil.addAll(names, contributor.getNames(project, false));
      } catch (ProcessCanceledException ex) {
        // index corruption detected, ignore
      } catch (Exception ex) {
        LOG.error(ex);
      }
    }

    return ArrayUtil.toStringArray(names);
  }


  public static class FilePathLookupItem extends LookupElement {
    private final PsiFile originalFile;
    private final String myName;
    private final String myPath;
    private final String myInfo;
    private final Icon myIcon;
    private final PsiFileSystemItem myFile;
    private final String macro;
    private final Module moduleForFile;
    private final List<FileReferenceHelper> myHelpers;

    public FilePathLookupItem(PsiFile originalFile, @NotNull final PsiFileSystemItem file, String macro, Module moduleForFile, @NotNull final List<FileReferenceHelper> helpers) {
      this.originalFile = originalFile;
      myName = file.getName();
      myPath = file.getVirtualFile().getPath();
      this.macro = macro;
      this.moduleForFile = moduleForFile;

      myHelpers = helpers;

      myInfo = FileInfoManager.getFileAdditionalInfo(file);
      myIcon = file.getIcon(0);

      myFile = file;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    @Override
    public String toString() {
      return String.format("%s%s", myName, myInfo == null ? "" : " (" + myInfo + ")");
    }

    @NotNull
    @Override
    public Object getObject() {
      return myFile;
    }

    @Override
    @NotNull
    public String getLookupString() {
      return myName;
    }

    @Override
    public void handleInsert(InsertionContext context) {
      Editor editor = context.getEditor();

      final Document document = editor.getDocument();
      final int startOffset = context.getStartOffset();
      String text = document.getText();

      int from = text.lastIndexOf('\n', startOffset - 1) + 1;
      String substring = text.substring(from, startOffset);
      int keywordEndIndex = StringUtil.indexOfSubstringEnd(substring, "@group.related ");
      if (keywordEndIndex < 0) {
        keywordEndIndex = StringUtil.indexOfSubstringEnd(substring, "@group.root ");
      }
      if (keywordEndIndex < 0) {
        return;
      }

      int to = text.indexOf('\n', startOffset - 1);
      if (to < 0) {
        to = text.length();
      }


//			String prefix = text.substring(from+keywordEndIndex, Math.min(to, text.length());

      String relativePath = getRelativePath();
      document.replaceString(from + keywordEndIndex, to, Objects.requireNonNull(relativePath));


      editor.getCaretModel().moveToOffset(from + keywordEndIndex + relativePath.length());
      final Project project = context.getProject();
      PsiDocumentManager.getInstance(project).commitDocument(document);
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
      final String relativePath = getRelativePath();

      final StringBuilder sb = new StringBuilder();
      if (myInfo != null) {
        sb.append(" (").append(myInfo);
      }

      if (relativePath != null && !relativePath.equals(myName)) {
        if (myInfo != null) {
          sb.append(", ");
        } else {
          sb.append(" (");
        }

        sb.append(relativePath);
      }

      if (!sb.isEmpty()) {
        sb.append(')');
      }

      presentation.setItemText(myName);

      if (!sb.isEmpty()) {
        presentation.setTailText(sb.toString(), true);
      }

      presentation.setIcon(myIcon);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    private String getRelativePath() {
      final VirtualFile virtualFile = myFile.getVirtualFile();
      LOG.assertTrue(virtualFile != null);
      for (FileReferenceHelper helper : myHelpers) {
        PsiFileSystemItem psiFileSystemItem = helper.getPsiFileSystemItem(myFile.getProject(), virtualFile);
        VirtualFile projectBaseDir;
        String path;
        projectBaseDir = myFile.getProject().getBaseDir();
        VirtualFile moduleFile = moduleForFile.getModuleFile();
        if ("*".equals(macro)) {
          PsiFileSystemItem root = helper.findRoot(myFile.getProject(), virtualFile);
          if (root != null) {
            path = "*/" + PsiFileSystemItemUtil.findRelativePath(root, helper.getPsiFileSystemItem(myFile.getProject(), virtualFile));
          } else {
            path = "*/" + virtualFile.getName();
          }
        } else if ("PROJECT".equals(macro)) {
          path = "PROJECT/" + VfsUtilCore.findRelativePath(projectBaseDir, virtualFile, VfsUtilCore.VFS_SEPARATOR_CHAR);
        } else if ("MODULE".equals(macro)) {
          if (moduleFile != null) {
            path = "MODULE/" + VfsUtilCore.findRelativePath(moduleFile, virtualFile, VfsUtilCore.VFS_SEPARATOR_CHAR);
          } else {
            path = "PROJECT/" + VfsUtilCore.findRelativePath(projectBaseDir, virtualFile, VfsUtilCore.VFS_SEPARATOR_CHAR);
          }
        } else {
          path = PsiFileSystemItemUtil.findRelativePath(originalFile, psiFileSystemItem);
          if (path == null || path.isEmpty()) {
            path = "PROJECT/" + VfsUtilCore.findRelativePath(projectBaseDir, virtualFile, VfsUtilCore.VFS_SEPARATOR_CHAR);
          }
        }
        return path;
      }
      return null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      FilePathLookupItem that = (FilePathLookupItem) o;

      if (!myName.equals(that.myName)) return false;
      return myPath.equals(that.myPath);
    }

    @Override
    public int hashCode() {
      int result = myName.hashCode();
      result = 31 * result + myPath.hashCode();
      return result;
    }
  }
}
