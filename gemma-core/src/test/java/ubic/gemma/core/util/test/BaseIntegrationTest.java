package ubic.gemma.core.util.test;

import org.junit.experimental.categories.Category;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * Base class for integration tests.
 * @author poirigui
 */
@ActiveProfiles(EnvironmentProfiles.TEST)
@Category(IntegrationTest.class)
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" })
public abstract class BaseIntegrationTest extends AbstractJUnit4SpringContextTests {

}
