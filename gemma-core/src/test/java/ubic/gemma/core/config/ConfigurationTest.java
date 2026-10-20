package ubic.gemma.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationTest {

    private static final String BARE_KEY = "gemma.test.barekey.unique";
    private static final String PREFIXED_KEY = "basecode." + BARE_KEY;

    @AfterEach
    void cleanup() {
        Configuration.reset();
        System.clearProperty( BARE_KEY );
        System.clearProperty( PREFIXED_KEY );
    }

    @Test
    void prefixedSystemPropertyResolves() {
        System.setProperty( PREFIXED_KEY, "prefixed" );
        assertEquals( "prefixed", Configuration.getString( BARE_KEY ) );
    }

    @Test
    void bareSystemPropertyResolves() {
        // -Dload.chebiOntology=true style override without the legacy basecode. prefix
        System.setProperty( BARE_KEY, "bare" );
        assertEquals( "bare", Configuration.getString( BARE_KEY ) );
    }

    @Test
    void prefixedTakesPrecedenceOverBare() {
        System.setProperty( PREFIXED_KEY, "prefixed" );
        System.setProperty( BARE_KEY, "bare" );
        assertEquals( "prefixed", Configuration.getString( BARE_KEY ) );
    }

    @Test
    void getBooleanHonoursBareSystemProperty() {
        System.setProperty( "load.chebiOntologyTestOnly", "true" );
        try {
            assertTrue( Boolean.TRUE.equals( Configuration.getBoolean( "load.chebiOntologyTestOnly" ) ) );
        } finally {
            System.clearProperty( "load.chebiOntologyTestOnly" );
        }
    }

    @Test
    void unknownKeyReturnsNull() {
        assertNull( Configuration.getString( "definitely.not.a.real.key.zzz" ) );
    }
}
