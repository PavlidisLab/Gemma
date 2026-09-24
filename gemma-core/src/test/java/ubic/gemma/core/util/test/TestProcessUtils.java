package ubic.gemma.core.util.test;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public class TestProcessUtils {

    /**
     * A shell command that writes this many bytes to standard error: more than a pipe buffer holds (64 KiB on Linux,
     * up to 64 KiB on macOS), so a child running it blocks until the parent reads its standard error.
     */
    public static final String FILL_STDERR = "head -c 300000 /dev/zero | tr '\\0' 'e' >&2";

    /**
     * Write an executable POSIX shell script to stand in for an external program.
     */
    public static Path writeShellScript( Path dir, String name, String body ) throws IOException {
        Path script = dir.resolve( name );
        Files.writeString( script, "#!/bin/sh\n" + body + "\n" );
        Files.setPosixFilePermissions( script, PosixFilePermissions.fromString( "rwxr-xr-x" ) );
        return script;
    }

    /**
     * Start a Java process that inherits the current process's environment and classpath.
     */
    public static Process startJavaProcess( Class<?> mainClass, String... argv ) throws IOException {
        String javaHome = System.getProperty( "java.home" );
        String classpath = System.getProperty( "java.class.path" );
        return new ProcessBuilder()
                .command( ArrayUtils.addAll( new String[] { javaHome + "/bin/java", "-cp", classpath, mainClass.getName() }, argv ) )
                .inheritIO()
                .start();
    }
}
