package ubic.gemma.core.config;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Shared configuration helpers.
 * <p>
 * Implements the {@code *_FILE} indirection pattern used by Postgres, Redis, Mongo containers and by
 * systemd's {@code LoadCredential=} mechanism: for any env var {@code FOO}, if a sibling {@code FOO_FILE}
 * is set, its value is treated as a path whose contents (trimmed) become the resolved value. If
 * {@code FOO_FILE} is unset, fall back to {@code FOO} directly.
 * <p>
 * This lets a podman Quadlet on Rocky Linux do:
 * <pre>
 *     [Service]
 *     LoadCredential=db.password:/etc/gemma/db.password
 *     Environment=GEMMA_DB_PASSWORD_FILE=%d/db.password
 * </pre>
 * and have the Java side pick the secret up without it ever appearing in process env or
 * {@code systemctl show} output.
 * <p>
 * Failure is fail-fast: a {@code _FILE} pointer to a missing or unreadable file throws
 * {@link IllegalStateException} at startup rather than silently falling through to the bare env var.
 */
public final class ConfigUtils {

    /**
     * Suffix appended to an env-var name to indicate "value lives in this file on disk".
     */
    public static final String FILE_INDIRECTION_SUFFIX = "_FILE";

    private ConfigUtils() {
    }

    /**
     * Resolve an environment variable, honouring the {@code *_FILE} indirection pattern.
     *
     * @param name env-var name, e.g. {@code GEMMA_DB_PASSWORD}
     * @return the resolved value (from {@code <name>_FILE} contents if set, else from {@code <name>}),
     * or {@code null} if neither is set
     * @throws IllegalStateException if {@code <name>_FILE} is set but the file cannot be read
     */
    @Nullable
    public static String resolveEnvVar( String name ) {
        return resolveEnvVar( name, System::getenv );
    }

    /**
     * Test-friendly form: resolve against an arbitrary env source instead of {@link System#getenv()}.
     * <p>
     * Useful for unit tests since {@code System.setenv()} is not supported on most JVMs.
     */
    @Nullable
    public static String resolveEnvVar( String name, Function<String, String> envSource ) {
        String filePath = envSource.apply( name + FILE_INDIRECTION_SUFFIX );
        if ( filePath != null && !filePath.isBlank() ) {
            return readCredentialFile( name, filePath );
        }
        return envSource.apply( name );
    }

    /**
     * Read a credential file, trimming leading/trailing whitespace (systemd-creds + {@code echo > file}
     * commonly append a newline). Fails fast on IO error.
     */
    private static String readCredentialFile( String envVarName, String filePath ) {
        try {
            return Files.readString( Path.of( filePath ) ).trim();
        } catch ( IOException e ) {
            throw new IllegalStateException( "Could not read credential file " + filePath
                    + " for environment variable " + envVarName, e );
        }
    }
}
