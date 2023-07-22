package krasa.editorGroups.language;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class EditorGroupsTypeFactory extends FileTypeFactory {

  @Override
  public void createFileTypes(@NotNull FileTypeConsumer consumer) {
    consumer.consume(
      EditorGroupsFileType.EDITOR_GROUPS_FILE_TYPE,
      EditorGroupsFileType.EXTENSION);
  }
}