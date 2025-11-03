package ubic.gemma.cli.util;

import lombok.Getter;

import javax.annotation.Nullable;
import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

/**
 * A context for the CLI based on {@link System}.
 *
 * @author poirigui
 */
public class SystemCLIContext implements CLIContext {

    @Nullable
    private final String commandNameOrAliasUsed;

    private final String[] arguments;

    private int exitStatus = 0;
    @Getter
    @Nullable
    private Exception exitCause = null;

    public SystemCLIContext( @Nullable String commandNameOrAliasUsed, String[] arguments ) {
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

    @Override
    public int getExitStatus() {
        return exitStatus;
    }

    @Override
    public void setExitStatus( int exitStatus, @Nullable Exception exitCause ) {
        this.exitStatus = exitStatus;
        this.exitCause = exitCause;
    }
}
