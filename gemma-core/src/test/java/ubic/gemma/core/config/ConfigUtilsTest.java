package ubic.gemma.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the {@code *_FILE} indirection in {@link ConfigUtils#resolveEnvVar(String, java.util.function.Function)}.
 * Uses the injectable env-source overload because {@link System#setenv} is not supported on the JVMs Gemma runs on.
 */
public class ConfigUtilsTest {

    @Test
    public void testResolvesFromFileWhenFileSuffixIsSet( @TempDir Path tmp ) throws IOException {
        Path secret = tmp.resolve( "db.password" );
        Files.writeString( secret, "s3cret-from-disk\n" ); // trailing newline simulates `echo > file`

        Map<String, String> env = new HashMap<>();
        env.put( "GEMMA_DB_PASSWORD_FILE", secret.toString() );
        env.put( "GEMMA_DB_PASSWORD", "ignored-direct-value" );

        assertThat( ConfigUtils.resolveEnvVar( "GEMMA_DB_PASSWORD", env::get ) )
                .isEqualTo( "s3cret-from-disk" );
    }

    @Test
    public void testTrimsLeadingAndTrailingWhitespace( @TempDir Path tmp ) throws IOException {
        Path secret = tmp.resolve( "db.password" );
        Files.writeString( secret, "  padded-secret\t\n\n" );

        Map<String, String> env = Map.of( "GEMMA_DB_PASSWORD_FILE", secret.toString() );

        assertThat( ConfigUtils.resolveEnvVar( "GEMMA_DB_PASSWORD", env::get ) )
                .isEqualTo( "padded-secret" );
    }

    @Test
    public void testFallsBackToDirectEnvVarWhenFileSuffixUnset() {
        Map<String, String> env = Map.of( "GEMMA_DB_PASSWORD", "direct-value" );

        assertThat( ConfigUtils.resolveEnvVar( "GEMMA_DB_PASSWORD", env::get ) )
                .isEqualTo( "direct-value" );
    }

    @Test
    public void testFallsBackToDirectEnvVarWhenFileSuffixIsBlank() {
        Map<String, String> env = new HashMap<>();
        env.put( "GEMMA_DB_PASSWORD_FILE", "   " );
        env.put( "GEMMA_DB_PASSWORD", "direct-value" );

        // Blank _FILE should be treated as unset, not as a pointer to a file at path "   ".
        assertThat( ConfigUtils.resolveEnvVar( "GEMMA_DB_PASSWORD", env::get ) )
                .isEqualTo( "direct-value" );
    }

    @Test
    public void testReturnsNullWhenNeitherIsSet() {
        Map<String, String> env = Map.of();

        assertThat( ConfigUtils.resolveEnvVar( "GEMMA_DB_PASSWORD", env::get ) )
                .isNull();
    }

    @Test
    public void testFailsFastWhenFileSuffixPointsToMissingFile( @TempDir Path tmp ) {
        Path missing = tmp.resolve( "does-not-exist" );
        Map<String, String> env = Map.of( "GEMMA_DB_PASSWORD_FILE", missing.toString() );

        assertThatThrownBy( () -> ConfigUtils.resolveEnvVar( "GEMMA_DB_PASSWORD", env::get ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( missing.toString() )
                .hasMessageContaining( "GEMMA_DB_PASSWORD" );
    }

    @Test
    public void testProductionOverloadReadsSystemEnv() {
        // Sanity check: the no-arg overload routes through System.getenv. We cannot set env vars at runtime,
        // so we just confirm that an unset var returns null without throwing.
        assertThat( ConfigUtils.resolveEnvVar( "GEMMA_DEFINITELY_UNSET_TEST_VAR_" + System.nanoTime() ) )
                .isNull();
    }

    @Test
    public void testIntegrationWithFilterEnvironmentVariables( @TempDir Path tmp ) throws IOException {
        // End-to-end via SettingsConfig: a GEMMA_DB_PASSWORD_FILE in the env map should land as
        // gemma.db.password in the filtered Properties (gemma.db.password is declared in default.properties).
        Path secret = tmp.resolve( "db.password" );
        Files.writeString( secret, "from-credential-file\n" );

        Map<String, String> env = new HashMap<>();
        env.put( "GEMMA_DB_PASSWORD_FILE", secret.toString() );

        java.util.Properties filtered = SettingsConfig.filterEnvironmentVariables( env );
        assertThat( filtered ).containsEntry( "gemma.db.password", "from-credential-file" );
    }
}
