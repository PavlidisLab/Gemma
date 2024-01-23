package ubic.gemma.core.util.test.suite;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import ubic.gemma.core.util.test.category.SlowTest;

/**
 * Test suite that excludes all the slow tests (i.e. marked with {@link SlowTest}).
 * @author poirigui
 */
@RunWith(Categories.class)
@Categories.ExcludeCategory(SlowTest.class)
@Suite.SuiteClasses(AllTests.class)
public class FastTests {
}
