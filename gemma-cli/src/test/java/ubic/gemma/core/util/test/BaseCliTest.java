package ubic.gemma.core.util.test;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.persistence.util.EnvironmentProfiles;

/**
 * Minimal setup
 */
@ActiveProfiles({ "cli", EnvironmentProfiles.TEST })
public abstract class BaseCliTest extends AbstractJUnit4SpringContextTests {

}
