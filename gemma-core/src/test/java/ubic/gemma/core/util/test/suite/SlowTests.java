package ubic.gemma.core.util.test.suite;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import ubic.gemma.core.util.test.category.SlowTest;

/**
 * Run all the slow tests.
 * <p>
 * If you run this and realize some tests are actually fast, remove their {@link SlowTest} annotation.
 * @author poirigui
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(SlowTest.class)
@Suite.SuiteClasses(AllTests.class)
public class SlowTests {
}
