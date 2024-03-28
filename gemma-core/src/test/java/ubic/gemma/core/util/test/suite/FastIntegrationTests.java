package ubic.gemma.core.util.test.suite;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import ubic.gemma.core.util.test.category.IntegrationTest;
import ubic.gemma.core.util.test.category.SlowTest;

/**
 * Fast integration tests.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(IntegrationTest.class)
@Categories.ExcludeCategory(SlowTest.class)
@Suite.SuiteClasses({ AllTests.class })
public class FastIntegrationTests {
}
