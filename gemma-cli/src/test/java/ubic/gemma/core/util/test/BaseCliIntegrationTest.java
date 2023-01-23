package ubic.gemma.core.util.test;

import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for CLI integration tests.
 */
@ActiveProfiles({ "cli" })
public class BaseCliIntegrationTest extends BaseSpringContextTest {

}
