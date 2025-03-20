package ubic.gemma.core.util;

import javax.annotation.Nullable;
import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

public interface CliContext {

    /**
     * Command name or alias used to invoke the command, or {@code null} if this was invoked using the fully
     * qualified class name.
     */
    @Nullable
    String getCommandNameOrAliasUsed();

    /**
     * Obtain the command line arguments for the command.
     */
    String[] getArguments();

    /**
     * Obtain the environment variables for the command.
     */
    Map<String, String> getEnvironment();

    /**
     * Obtain the console, if available.
     */
    @Nullable
    Console getConsole();

    InputStream getInputStream();

    PrintStream getOutputStream();

    PrintStream getErrorStream();
}
