package ubic.gemma.core.util.test;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;

public class TestProcessUtils {

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
