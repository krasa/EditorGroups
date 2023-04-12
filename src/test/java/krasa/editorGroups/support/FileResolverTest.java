package krasa.editorGroups.support;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileResolverTest {

	private FileResolver fileResolver;

	@Before
	public void setUp() throws Exception {
		fileResolver = new FileResolver();
	}

	@Test
	public void resolve_inDir() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_wildcard() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF\\*"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_wildcard2() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF\\plug*"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_wildcard3() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF\\plugin.*"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_withoutExtension() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF\\plugin"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF\\plugin.xml"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_notFound() throws IOException {
		fileResolver.resolve(new File("resources\\META-INF\\foo"));
		String[] objects = fileResolver.getLinks().toArray(new String[0]);
		assertEquals(0, objects.length);
	}

	@Test
	public void macros() {
	}
}