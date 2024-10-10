package ubic.gemma.core.util.test;

import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for CLI tests.
 * <p>
 * Use {@link BaseCLIIntegrationTest} for integration tests.
 */
@ActiveProfiles("cli")
public class BaseCLITest extends BaseTest {
}
