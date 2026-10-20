package ubic.gemma.core.util.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ubic.gemma.core.context.EnvironmentProfiles;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseTest}.
 * <p>
 * Where {@link BaseTest} inherits {@code AbstractJUnit4SpringContextTests} to get
 * the legacy Spring JUnit 4 runner, this base uses Jupiter's
 * {@link SpringExtension} directly. The JUnit 5 idiom is annotation-driven, not
 * inheritance-driven, but we keep a base class to centralise the
 * {@link ActiveProfiles} declaration and to provide a stable extension point
 * for downstream JUnit 5 sub-bases (see {@link BaseIntegrationTest5} and
 * {@link BaseSpringContextTest5}).
 * <p>
 * Phase 3 JUnit 5 migration: introduced in parallel with {@link BaseTest} so
 * downstream test classes can migrate one-at-a-time rather than as a flag day.
 * Once every former {@code extends BaseTest} has flipped, delete
 * {@link BaseTest} and rename this class.
 */
@ActiveProfiles(EnvironmentProfiles.TEST)
@ExtendWith(SpringExtension.class)
public abstract class BaseTest5 {

}
