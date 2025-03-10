package ubic.gemma.core.util.test;

import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.description.Description;
import org.assertj.core.internal.Failures;
import ubic.gemma.core.util.CLI;
import ubic.gemma.core.util.ShellUtils;
import ubic.gemma.core.util.TestCliContext;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CliAssert extends AbstractAssert<CliAssert, CLI> {

    private final Failures failures = Failures.instance();

    private final TestCliContext cliContext = new TestCliContext( null, new String[0] );
    private final ByteArrayOutputStream in = new ByteArrayOutputStream();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    public CliAssert( CLI cli ) {
        super( cli, CliAssert.class );
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream eos = new ByteArrayOutputStream();
        cliContext.setOutputStream( new PrintStream( out, true ) );
        cliContext.setErrorStream( new PrintStream( err, true ) );
        this.info.description( new Description() {
            @Override
            public String value() {
                return String.format( "Command: %s\nArguments: %s\nInteractive: %s\nEnvironment:\n%s\nStandard Input:\n%s\nStandard Output:\n%s\nStandard Error:\n%s",
                        cli.getClass().getName() + ( cliContext.getCommandNameOrAliasUsed() != null ? ( " via " + cliContext.getCommandNameOrAliasUsed() ) : "" ),
                        Arrays.stream( cliContext.getArguments() ).map( ShellUtils::quoteIfNecessary ).collect( Collectors.joining( " " ) ),
                        cliContext.getConsole() != null ? "yes" : "no",
                        cliContext.getEnvironment().entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "\n" ) ),
                        in, out, err );
            }
        } );
    }

    private String streamToString( OutputStream stream ) {
        return stream.toString().trim();
    }

    /**
     * Set the command name or alias used to execute the command.
     */
    public CliAssert withCommandNameOrAlias( String commandNameOrAlias ) {
        cliContext.setCommandNameOrAliasUsed( commandNameOrAlias );
        return myself;
    }

    /**
     * Set the arguments passed to the command.
     */
    public CliAssert withArguments( String... arguments ) {
        cliContext.setArguments( arguments );
        return myself;
    }

    /**
     * Set the environment variables to be used when executing the command.
     */
    public CliAssert withEnvironment( Map<String, String> environment ) {
        cliContext.setEnvironment( environment );
        return myself;
    }

    /**
     * Set the console to be used for interactive use.
     */
    public CliAssert withConsole( Console console ) {
        cliContext.setConsole( console );
        return myself;
    }

    /**
     * Set the input stream to be used as standard input for the command.
     */
    public CliAssert withInputStream( InputStream inputStream ) {
        cliContext.setInputStream( new TeeInputStream( inputStream, in ) );
        return myself;
    }

    public CliAssert withOutputStream( OutputStream outputStream ) {
        cliContext.setOutputStream( new PrintStream( new TeeOutputStream( outputStream, out ), true ) );
        return myself;
    }

    public CliAssert withErrorStream( OutputStream errorStream ) {
        cliContext.setErrorStream( new PrintStream( new TeeOutputStream( errorStream, err ), true ) );
        return myself;
    }

    /**
     * Asserts that the command succeeds with a zero exit status.
     */
    public void succeeds() {
        objects.assertEqual( info, actual.executeCommand( cliContext ), 0 );
    }
}
