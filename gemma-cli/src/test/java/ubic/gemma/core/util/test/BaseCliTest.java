package ubic.gemma.core.util.test;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.persistence.util.SpringProfiles;

/**
 * Minimal setup
 */
@ActiveProfiles({ "cli", SpringProfiles.TEST })
public abstract class BaseCliTest extends AbstractJUnit4SpringContextTests {

}
