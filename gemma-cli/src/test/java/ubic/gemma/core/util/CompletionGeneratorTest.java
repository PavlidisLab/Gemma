package ubic.gemma.core.util;

import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ubic.gemma.core.apps.GemmaCLI;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNoException;

public class CompletionGeneratorTest {

    private Options generalOptions;
    private SortedMap<GemmaCLI.CommandGroup, SortedMap<String, CLI>> commands;

    @Before
    public void setUp() {
        generalOptions = new Options();
        generalOptions.addOption( "h", false, "Show help" );
        Options subcommandOptions = new Options();
        subcommandOptions.addOption( "h", false, "Show help" );
        subcommandOptions.addOption( "multiline", false, "Multiline\ndescription" );
        commands = new TreeMap<>();
        commands.put( GemmaCLI.CommandGroup.MISC, new TreeMap<>() );
        commands.get( GemmaCLI.CommandGroup.MISC ).put( "a", createFakeCli( "a", "test\ntest", subcommandOptions ) );
        commands.get( GemmaCLI.CommandGroup.MISC ).put( "b", createFakeCli( "b", "testb", subcommandOptions ) );
        commands.get( GemmaCLI.CommandGroup.MISC ).put( "c", createFakeCli( "c", "testc", subcommandOptions ) );
    }

    @Test
    public void testBash() throws InterruptedException, IOException {
        Process process = Runtime.getRuntime().exec( "bash", new String[] { "LANG=C" } );
        try ( PrintWriter writer = new PrintWriter( process.getOutputStream() ) ) {
            CompletionGenerator completionGenerator = new BashCompletionGenerator( generalOptions, commands );
            completionGenerator.generateCompletion( writer );
        }
        String error = IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 );
        assertEquals( error, 0, process.waitFor() );
    }

    @Test
    public void testBashCompletions() throws IOException, InterruptedException {
        assertThat( getBashCompletions( "-" ) )
                .isEqualTo( "-h\n" );
        // FIXME: I don't know how to make this work with compgen...
        // assertThat( getBashCompletions( "a -" ) )
        //         .isEqualTo( "-h\n-multiline\n" );
    }

    @Test
    public void testFish() throws InterruptedException, IOException {
        Process process;
        try {
            process = Runtime.getRuntime().exec( "fish", new String[] { "LANG=C" } );
            process.getOutputStream().close();
            assertThat( process.waitFor() ).isEqualTo( 0 );
        } catch ( IOException e ) {
            assumeNoException( "fish command was not found", e );
            return;
        }
        try ( PrintWriter writer = new PrintWriter( process.getOutputStream() ) ) {
            CompletionGenerator completionGenerator = new FishCompletionGenerator( generalOptions, commands );
            completionGenerator.generateCompletion( writer );
        }
        String error = IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 );
        assertEquals( error, 0, process.waitFor() );
    }

    @Test
    @Ignore("This test simple doesn't work on the CI.")
    public void testFishCompletions() throws IOException, InterruptedException {
        Process process;
        try {
            process = Runtime.getRuntime().exec( "fish", new String[] { "LANG=C" } );
            process.getOutputStream().close();
            assertThat( process.waitFor() ).isEqualTo( 0 );
        } catch ( IOException e ) {
            assumeNoException( "fish command was not found", e );
            return;
        }
        // not all fish versions supports 'complete -C' in non-interactive mode
        try ( PrintWriter writer = new PrintWriter( process.getOutputStream() ) ) {
            writer.println( "complete -C test" );
        }
        assumeThat( process.waitFor() ).isEqualTo( 0 );
        assertThat( getFishCompletions( "-" ) )
                .isEqualTo( "-h\tShow help\n" );
        assertThat( getFishCompletions( "a -" ) )
                .isEqualTo( "-h\tShow help\n-multiline\tMultiline\\ndescription\n" );
    }

    private String getBashCompletions( String words ) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec( "bash", new String[] { "LANG=C" } );
        try ( PrintWriter writer = new PrintWriter( process.getOutputStream() ) ) {
            StringWriter sw = new StringWriter();
            CompletionGenerator completionGenerator1 = new BashCompletionGenerator( generalOptions, commands );
            completionGenerator1.generateCompletion( new PrintWriter( sw ) );
            System.out.println( sw.getBuffer() );
            CompletionGenerator completionGenerator = new BashCompletionGenerator( generalOptions, commands );
            completionGenerator.generateCompletion( writer );
            writer.println( "export COMP_WORDS=( gemma-cli " + words + " )" );
            writer.println( "compgen -F __gemma_cli_complete -- '" + words + "'" );
        }
        return getProcessOutput( process );
    }

    private String getFishCompletions( String words ) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec( "fish", new String[] { "LANG=C" } );
        try ( PrintWriter writer = new PrintWriter( process.getOutputStream() ) ) {
            CompletionGenerator completionGenerator = new FishCompletionGenerator( generalOptions, commands );
            completionGenerator.generateCompletion( writer );
            writer.println( "complete -C 'gemma-cli " + words + "'" );
        }
        return getProcessOutput( process );
    }

    private String getProcessOutput( Process proc ) throws InterruptedException, IOException {
        if ( proc.waitFor() != 0 ) {
            throw new RuntimeException( IOUtils.toString( proc.getErrorStream(), StandardCharsets.UTF_8 ) );
        } else {
            return IOUtils.toString( proc.getInputStream(), StandardCharsets.UTF_8 );
        }
    }

    private CLI createFakeCli( String commandName, @Nullable String shortDesc, Options subcommandOptions ) {
        return new CLI() {

            @Nullable
            @Override
            public String getCommandName() {
                return commandName;
            }

            @Nullable
            @Override
            public String getShortDesc() {
                return shortDesc;
            }

            @Nullable
            public String getLongDesc() {
                return null;
            }

            @Override
            public GemmaCLI.CommandGroup getCommandGroup() {
                return GemmaCLI.CommandGroup.MISC;
            }

            @Override
            public Options getOptions() {
                return subcommandOptions;
            }

            @Override
            public boolean allowPositionalArguments() {
                return false;
            }

            @Override
            public int executeCommand( String... args ) {
                return 0;
            }
        };
    }
}