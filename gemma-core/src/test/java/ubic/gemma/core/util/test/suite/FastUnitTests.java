package ubic.gemma.core.util.test.suite;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import ubic.gemma.core.util.test.category.IntegrationTest;
import ubic.gemma.core.util.test.category.SlowTest;

/**
 * Fast unit tests.
 */
@RunWith(Categories.class)
@Categories.ExcludeCategory({ IntegrationTest.class, SlowTest.class })
@Suite.SuiteClasses(AllTests.class)
public class FastUnitTests {
}
