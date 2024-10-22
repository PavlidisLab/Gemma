package ubic.gemma.core.util.test;

import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for CLI tests.
 * <p>
 * Use {@link BaseCliIntegrationTest} for integration tests.
 */
@ActiveProfiles("cli")
public abstract class BaseCliTest extends BaseTest {

}
