package ubic.gemma.core.util.test;

import org.junit.Rule;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.EnvironmentProfiles;

/**
 * Base class for all tests.
 * <p>
 * This sets the {@link EnvironmentProfiles#TEST} profile.
 */
@ActiveProfiles(EnvironmentProfiles.TEST)
public abstract class BaseTest extends AbstractJUnit4SpringContextTests {

}
