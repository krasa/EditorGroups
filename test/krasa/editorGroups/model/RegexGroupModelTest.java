package krasa.editorGroups.model;

import junit.framework.TestCase;

public class RegexGroupModelTest extends TestCase {

	public static final String CURRENT_FOLDER_1_2 = "v1|CURRENT_FOLDER|1,2|.*";
	public static final RegexGroupModel REGEX_GROUP_MODEL = new RegexGroupModel(".*", RegexGroupModel.Scope.CURRENT_FOLDER, "1,2");

	public void test() {
		assertTrue(REGEX_GROUP_MODEL.isComparingGroup(3));
		assertFalse(REGEX_GROUP_MODEL.isComparingGroup(2));
		assertFalse(REGEX_GROUP_MODEL.isComparingGroup(1));
	}

	public void testSerialize() {
		assertEquals(CURRENT_FOLDER_1_2, REGEX_GROUP_MODEL.serialize());
	}

	public void testDeserialize() {
		assertEquals(REGEX_GROUP_MODEL, RegexGroupModel.deserialize(CURRENT_FOLDER_1_2));
	}
}