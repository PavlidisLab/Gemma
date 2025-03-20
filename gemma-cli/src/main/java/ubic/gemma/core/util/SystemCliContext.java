package ubic.gemma.core.util;

import javax.annotation.Nullable;
import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

/**
 * A context for the CLI based on {@link System}.
 * @author poirigui
 */
public class SystemCliContext implements CliContext {

    @Nullable
    private final String commandNameOrAliasUsed;

    private final String[] arguments;

    public SystemCliContext( @Nullable String commandNameOrAliasUsed, String[] arguments ) {
        this.commandNameOrAliasUsed = commandNameOrAliasUsed;
        this.arguments = Arrays.copyOf( arguments, arguments.length );
    }

    @Nullable
    @Override
    public String getCommandNameOrAliasUsed() {
        return commandNameOrAliasUsed;
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return System.getenv();
    }

    @Nullable
    @Override
    public Console getConsole() {
        return System.console();
    }

    @Override
    public InputStream getInputStream() {
        return System.in;
    }

    @Override
    public PrintStream getOutputStream() {
        return System.out;
    }

    @Override
    public PrintStream getErrorStream() {
        return System.err;
    }
}
