package ubic.gemma.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SentinelPropertyValidator}. Constructs the validator directly against a
 * synthetic {@link MutablePropertySources} so we don't pay the cost of a Spring context.
 */
class SentinelPropertyValidatorTest {

    @Test
    void allSentinelsOverridden_passes() {
        MutablePropertySources ps = baseDefaults();
        Properties env = new Properties();
        env.setProperty( "gemma.db.password", "real-secret" );
        env.setProperty( "gemma.runas.password", "another-secret" );
        env.setProperty( "gemma.anonymousAuth.key", "anon-key" );
        env.setProperty( "gemma.agent.password", "agent-secret" );
        env.setProperty( "mail.username", "gemma@example.org" );
        ps.addFirst( new PropertiesPropertySource( "env", env ) );

        SentinelPropertyValidator v = new SentinelPropertyValidator( ps );
        // default: ignoreSentinels=false; afterPropertiesSet should not throw
        v.afterPropertiesSet();
    }

    @Test
    void unresolvedSentinel_throwsAndNamesTheKey() {
        MutablePropertySources ps = baseDefaults();
        // gemma.db.password is left at XXXXXX in defaults; everything else overridden
        Properties env = new Properties();
        env.setProperty( "gemma.runas.password", "another-secret" );
        env.setProperty( "gemma.anonymousAuth.key", "anon-key" );
        env.setProperty( "gemma.agent.password", "agent-secret" );
        env.setProperty( "mail.username", "gemma@example.org" );
        ps.addFirst( new PropertiesPropertySource( "env", env ) );

        SentinelPropertyValidator v = new SentinelPropertyValidator( ps );

        assertThatThrownBy( v::afterPropertiesSet )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "gemma.db.password" )
                .hasMessageContaining( "GEMMA_DB_PASSWORD" )
                .hasMessageContaining( "XXXXXX" );
    }

    @Test
    void ignoreSentinels_bypassesCheck() {
        MutablePropertySources ps = baseDefaults();
        // nothing overridden — all five keys still XXXXXX

        SentinelPropertyValidator v = new SentinelPropertyValidator( ps );
        // flip the @Value-bound field via reflection to simulate gemma.sentinels.ignore=true
        new DirectFieldAccessor( v ).setPropertyValue( "ignoreSentinels", true );

        v.afterPropertiesSet(); // does not throw
    }

    @Test
    void toEnvVar_translatesDotToUnderscore() {
        assertThat( SentinelPropertyValidator.toEnvVar( "gemma.db.password" ) )
                .isEqualTo( "GEMMA_DB_PASSWORD" );
        assertThat( SentinelPropertyValidator.toEnvVar( "mail.username" ) )
                .isEqualTo( "MAIL_USERNAME" );
    }

    /**
     * A {@link MutablePropertySources} containing only the XXXXXX-sentinel defaults shipped by
     * {@code default.properties} (no overrides). Tests layer overrides on top via
     * {@link MutablePropertySources#addFirst}.
     */
    private static MutablePropertySources baseDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty( "mail.username", "XXXXXX" );
        defaults.setProperty( "gemma.db.password", "XXXXXX" );
        defaults.setProperty( "gemma.runas.password", "XXXXXXX" );
        defaults.setProperty( "gemma.anonymousAuth.key", "XXXXXXXX" );
        defaults.setProperty( "gemma.agent.password", "XXXXXXXX" );
        MutablePropertySources ps = new MutablePropertySources();
        ps.addLast( new PropertiesPropertySource( "defaults", defaults ) );
        return ps;
    }
}
