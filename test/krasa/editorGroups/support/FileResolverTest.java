package krasa.editorGroups.support;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileResolverTest {

	@Test
	public void resolve_inDir() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_wildcard() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF\\*"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_wildcard2() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF\\plug*"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_wildcard3() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF\\plugin.*"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_withoutExtension() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF\\plugin"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF\\plugin.xml"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(1, objects.length);
		assertTrue(objects[0].endsWith("plugin.xml"));
	}

	@Test
	public void resolve_notFound() throws IOException {
		LinkedHashSet<String> links = new LinkedHashSet<>();
		new FileResolver().resolve(links, new File("resources\\META-INF\\foo"));
		String[] objects = links.toArray(new String[0]);
		assertEquals(0, objects.length);
	}

	@Test
	public void macros() {
	}
}