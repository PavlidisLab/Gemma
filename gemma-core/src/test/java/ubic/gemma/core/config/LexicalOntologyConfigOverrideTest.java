package ubic.gemma.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the config path the Cellosaurus/MGI providers actually use: {@link Configuration}, which reads
 * defaults from {@code basecode.properties} and honours system-property overrides via the {@code basecode.}
 * prefix or the bare key — NOT the {@code gemma.} prefix used by {@code SettingsConfig}. The provider
 * ctors read {@code Configuration.getBoolean("load.<name>")} / {@code getString("url.<name>")}, so these
 * keys must live in {@code basecode.properties} and enable via {@code -Dload.<name>} / {@code -Dbasecode.load.<name>}.
 */
class LexicalOntologyConfigOverrideTest {

    @Test
    void defaultsAreReadableAndDisabled() {
        assertFalse( Boolean.TRUE.equals( Configuration.getBoolean( "load.cellosaurus" ) ) );
        assertFalse( Boolean.TRUE.equals( Configuration.getBoolean( "load.mgiStrain" ) ) );
        // the URL default must resolve (was the regression: moving it out of basecode.properties null'd it)
        assertNotNull( Configuration.getString( "url.cellosaurus" ), "url.cellosaurus must be in basecode.properties" );
        assertNotNull( Configuration.getString( "url.mgiStrain" ), "url.mgiStrain must be in basecode.properties" );
    }

    @Test
    void bareSystemPropertyEnables() {
        try {
            System.setProperty( "load.cellosaurus", "true" );
            assertTrue( Configuration.getBoolean( "load.cellosaurus" ), "-Dload.cellosaurus=true must enable" );
        } finally {
            System.clearProperty( "load.cellosaurus" );
        }
    }

    @Test
    void basecodePrefixedSystemPropertyEnables() {
        try {
            System.setProperty( "basecode.load.mgiStrain", "true" );
            assertTrue( Configuration.getBoolean( "load.mgiStrain" ), "-Dbasecode.load.mgiStrain=true must enable" );
        } finally {
            System.clearProperty( "basecode.load.mgiStrain" );
        }
    }

    @Test
    void gemmaPrefixDoesNotReachConfiguration() {
        // documents the trap: the gemma. prefix is for SettingsConfig/@Value, not the Configuration facade
        try {
            System.setProperty( "gemma.load.cellosaurus", "true" );
            assertFalse( Boolean.TRUE.equals( Configuration.getBoolean( "load.cellosaurus" ) ),
                    "-Dgemma.load.cellosaurus must NOT enable (wrong facade)" );
        } finally {
            System.clearProperty( "gemma.load.cellosaurus" );
        }
    }
}
