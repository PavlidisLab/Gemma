package ubic.gemma.cli.util.test;

import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseIntegrationTest5;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseCliIntegrationTest}.
 * <p>
 * Identical contract to the JUnit 4 base, just expressed against the Jupiter
 * integration base so subclasses can use {@code @Test} from
 * {@code org.junit.jupiter.api}.
 *
 * @author poirigui
 */
@ActiveProfiles("cli")
public abstract class BaseCliIntegrationTest5 extends BaseIntegrationTest5 {

}
