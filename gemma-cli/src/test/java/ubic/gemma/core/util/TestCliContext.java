package ubic.gemma.core.util;

import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of {@link CliContext} for testing purposes.
 * @author poirigui
 */
public class TestCliContext implements CliContext {

    @Nullable
    private String commandNameOrAliasUsed;
    private String[] arguments;
    private Map<String, String> environment = new HashMap<>();
    private Console console = null;
    private InputStream inputStream = new ByteArrayInputStream( new byte[0] );
    private PrintStream outputStream = new PrintStream( new ByteArrayOutputStream() );
    private PrintStream errorStream = new PrintStream( new ByteArrayOutputStream() );

    public TestCliContext( @Nullable String commandNameOrAliasUsed, String[] arguments ) {
        this.commandNameOrAliasUsed = commandNameOrAliasUsed;
        this.arguments = arguments;
    }

    @Nullable
    @Override
    public String getCommandNameOrAliasUsed() {
        return commandNameOrAliasUsed;
    }

    public void setCommandNameOrAliasUsed( @Nullable String commandNameOrAliasUsed ) {
        this.commandNameOrAliasUsed = commandNameOrAliasUsed;
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

    public void setArguments( String[] arguments ) {
        this.arguments = arguments;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    @Nullable
    @Override
    public Console getConsole() {
        return console;
    }

    public void setConsole( Console console ) {
        this.console = console;
    }

    public void setEnvironment( Map<String, String> environment ) {
        this.environment = environment;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream( InputStream inputStream ) {
        this.inputStream = inputStream;
    }

    @Override
    public PrintStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream( PrintStream outputStream ) {
        this.outputStream = outputStream;
    }

    @Override
    public PrintStream getErrorStream() {
        return errorStream;
    }

    public void setErrorStream( PrintStream errorStream ) {
        this.errorStream = errorStream;
    }
}
