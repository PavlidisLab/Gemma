package ubic.gemma.cli.util.test;

import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseIntegrationTest;

/**
 * Base class for CLI integration tests.
 * @author poirigui
 */
@ActiveProfiles("cli")
public abstract class BaseCliIntegrationTest extends BaseIntegrationTest {

}
