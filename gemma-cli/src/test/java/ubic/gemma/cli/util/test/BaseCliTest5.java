package ubic.gemma.cli.util.test;

import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseTest5;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseCliTest}.
 * <p>
 * Identical contract to the JUnit 4 base, just expressed against the Jupiter
 * non-integration base so subclasses can use {@code @Test} from
 * {@code org.junit.jupiter.api}.
 *
 * @author poirigui
 */
@ActiveProfiles("cli")
public abstract class BaseCliTest5 extends BaseTest5 {

}
