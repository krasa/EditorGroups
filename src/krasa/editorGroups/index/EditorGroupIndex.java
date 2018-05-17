package krasa.editorGroups.index;

import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class EditorGroupIndex extends FileBasedIndexExtension<String, EditorGroupIndexValue> {
	@NonNls
	public static final ID<String, EditorGroupIndexValue> NAME = ID.create("EditorGroupIndex");

	private final DataExternalizer<EditorGroupIndexValue> myValueExternalizer = new DataExternalizer<EditorGroupIndexValue>() {

		@Override
		public void save(@NotNull DataOutput out, EditorGroupIndexValue value) throws IOException {
			String title = value.getTitle();
			out.writeUTF(value.getOwnerPath());
			out.writeUTF(title == null ? "" : title);
			out.writeInt(value.getRelatedPaths().size());
			List<String> related = value.getRelatedPaths();
			for (String s : related) {
				out.writeUTF(s);
			}

		}

		@Override
		public EditorGroupIndexValue read(@NotNull DataInput in) throws IOException {
			EditorGroupIndexValue value = new EditorGroupIndexValue();
			value.setOwnerPath(in.readUTF());
			value.setTitle(in.readUTF());
			int i = in.readInt();
			for (int j = 0; j < i; j++) {
				value.addRelated(in.readUTF());
			}
			return value;
		}

	};

	private final DataIndexer<String, EditorGroupIndexValue, FileContent> myIndexer = new PlainTextIndexer();


	@Override
	public int getVersion() {
		int version = 10;
		int versionFromIndexer = myIndexer instanceof PlainTextIndexer ? (((PlainTextIndexer) myIndexer).getVersion()) : 0xFF;
		version = version * 31 + (versionFromIndexer ^ myIndexer.getClass().getName().hashCode());
		return version;
	}

	@Override
	public boolean dependsOnFileContent() {
		return true;
	}

	@NotNull
	@Override
	public ID<String, EditorGroupIndexValue> getName() {
		return NAME;
	}

	@NotNull
	@Override
	public DataIndexer<String, EditorGroupIndexValue, FileContent> getIndexer() {
		return myIndexer;
	}

	@NotNull
	@Override
	public KeyDescriptor<String> getKeyDescriptor() {
		return EnumeratorStringDescriptor.INSTANCE;
	}

	@NotNull
	@Override
	public DataExternalizer<EditorGroupIndexValue> getValueExternalizer() {
		return myValueExternalizer;
	}

	private final FileBasedIndex.InputFilter myInputFilter = file -> {
		if (!file.isInLocalFileSystem()  // skip library sources
			|| file.getFileType().isBinary()
			) {
			return false;
		}
		return true;
	};

	@NotNull
	@Override
	public FileBasedIndex.InputFilter getInputFilter() {
		return myInputFilter;
	}

	@Override
	public boolean hasSnapshotMapping() {
		return true;
	}
}
