package ubic.gemma.core.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards that the Cellosaurus/MGI load flags live in {@code default.properties} (not
 * {@code basecode.properties}), so they honour the standard {@code -Dgemma.load.*} / {@code GEMMA_LOAD_*}
 * override convention rather than the legacy {@code -Dbasecode.load.*} one.
 * <p>
 * {@link SettingsConfig#filterSystemProperties} only passes through a {@code gemma.<key>} system property
 * when {@code <key>} is declared in {@code default.properties}/{@code project.properties}.
 */
class LexicalOntologyConfigOverrideTest {

    @Test
    void gemmaPrefixedSystemPropertyEnablesLexicalOntologies() throws Exception {
        Properties sys = new Properties();
        sys.setProperty( "gemma.load.cellosaurus", "true" );
        sys.setProperty( "gemma.load.mgiStrain", "true" );
        sys.setProperty( "gemma.url.cellosaurus", "file:///tmp/override.obo" );

        Properties resolved = SettingsConfig.filterSystemProperties( sys );

        assertEquals( "true", resolved.getProperty( "load.cellosaurus" ),
                "-Dgemma.load.cellosaurus must resolve to load.cellosaurus (key must be in default.properties)" );
        assertEquals( "true", resolved.getProperty( "load.mgiStrain" ),
                "-Dgemma.load.mgiStrain must resolve to load.mgiStrain" );
        assertEquals( "file:///tmp/override.obo", resolved.getProperty( "url.cellosaurus" ),
                "-Dgemma.url.cellosaurus must be overridable too" );
    }
}
