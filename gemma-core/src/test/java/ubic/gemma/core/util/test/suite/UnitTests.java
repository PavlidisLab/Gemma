package ubic.gemma.core.util.test.suite;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import ubic.gemma.core.util.test.category.SpringContextTest;

@RunWith(Categories.class)
@Categories.ExcludeCategory(SpringContextTest.class)
@Suite.SuiteClasses({ AllTests.class })
public class UnitTests {
}
