package ubic.gemma.cli.util.test;

import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseTest;

/**
 * Base class for CLI tests.
 * <p>
 * Use {@link BaseCliIntegrationTest} for integration tests.
 */
@ActiveProfiles("cli")
public abstract class BaseCliTest extends BaseTest {

}
