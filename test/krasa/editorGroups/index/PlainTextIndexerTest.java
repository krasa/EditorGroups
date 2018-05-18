//package krasa.editorGroups.index;
//
//import org.apache.commons.io.filefilter.RegexFileFilter;
//import org.apache.commons.io.filefilter.WildcardFileFilter;
//import org.apache.tools.ant.DirectoryScanner;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.FileFilter;
//import java.io.FilenameFilter;
//import java.io.IOException;
//import java.net.FileNameMap;
//import java.nio.file.*;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.Arrays;
//import java.util.List;
//import java.util.function.BiPredicate;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//
//public class PlainTextIndexerTest {
//	@Test
//	public void name() throws IOException {
//		String pattern = "../**";
//		String dir = "F:\\workspace\\_projekty\\Github\\EditorGroups\\src\\krasa\\editorGroups\\";
//		
//		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
//		Stream<Path> paths = Files.find(Paths.get(dir), Integer.MAX_VALUE, (path, f)->pathMatcher.matches(path));
//		
//		List<Path> pathsList = paths.collect(Collectors.toList());
//
//		System.out.println(pathsList);
//		
//		
//		
//		
//		DirectoryScanner scanner = new DirectoryScanner();
//		scanner.setIncludes(new String[]{"**/*"});
//		scanner.setBasedir(dir);
//		scanner.setCaseSensitive(false);
//		scanner.scan();
//		String[] files = scanner.getIncludedFiles();
//		
//		System.out.println(Arrays.toString(files));
//		    
//		
//		
//		
////		Path start = Paths.get("");
////		Files.find(start,
////			Integer.MAX_VALUE,
////			new BiPredicate<Path, BasicFileAttributes>() {
////				@Override
////				public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
////					return path.toFile().getName().matches(".*.pom");
////				}
////			}
////		);
//	}
//}