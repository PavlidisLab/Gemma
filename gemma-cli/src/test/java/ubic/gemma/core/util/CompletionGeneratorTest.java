package ubic.gemma.core.util;

import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import ubic.gemma.core.completion.BashCompletionGenerator;
import ubic.gemma.core.completion.CompletionGenerator;
import ubic.gemma.core.completion.FishCompletionGenerator;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNoException;

public class CompletionGeneratorTest {

    @Test
    public void testBash() throws InterruptedException, IOException {
        Process process = Runtime.getRuntime().exec( "bash", new String[] { "LANG=C" } );
        try ( PrintWriter writer = new PrintWriter( new OutputStreamWriter( process.getOutputStream() ) ) ) {
            writeCompletionScript( new BashCompletionGenerator( "gemma-cli", new HashSet<>( Arrays.asList( "a", "b", "c" ) ) ), writer );
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
        try ( PrintWriter writer = new PrintWriter( new OutputStreamWriter( process.getOutputStream() ) ) ) {
            writeCompletionScript( new FishCompletionGenerator( "gemma-cli", new HashSet<>( Arrays.asList( "a", "b", "c" ) ) ), writer );
        }
        String error = IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 );
        assertEquals( error, 0, process.waitFor() );
    }

    @Test
    @Ignore("This test simple does't work on the CI.")
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
        try ( PrintWriter writer = new PrintWriter( new OutputStreamWriter( process.getOutputStream() ) ) ) {
            writeCompletionScript( new BashCompletionGenerator( "gemma-cli", new HashSet<>( Arrays.asList( "a", "b", "c" ) ) ), writer );
            writer.println( "export COMP_WORDS=( gemma-cli " + words + " )" );
            writer.println( "compgen -F __gemma_cli_complete -- '" + words + "'" );
        }
        return getProcessOutput( process );
    }

    private String getFishCompletions( String words ) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec( "fish", new String[] { "LANG=C" } );
        try ( PrintWriter writer = new PrintWriter( new OutputStreamWriter( process.getOutputStream() ) ) ) {
            writeCompletionScript( new FishCompletionGenerator( "gemma-cli", new HashSet<>( Arrays.asList( "a", "b", "c" ) ) ), writer );
            writer.println( "complete -C 'gemma-cli " + words + "'" );
        }
        return getProcessOutput( process );
    }

    private void writeCompletionScript( CompletionGenerator completionGenerator, PrintWriter writer ) {
        Options generalOptions = new Options();
        Options subcommandOptions = new Options();
        generalOptions.addOption( "h", false, "Show help" );
        subcommandOptions.addOption( "h", false, "Show help" );
        subcommandOptions.addOption( "multiline", false, "Multiline\ndescription" );
        completionGenerator.beforeCompletion( writer );
        completionGenerator.generateCompletion( generalOptions, writer );
        completionGenerator.generateSubcommandCompletion( "a", subcommandOptions, "test\ntest", false, writer );
        completionGenerator.afterCompletion( writer );
    }

    private String getProcessOutput( Process proc ) throws InterruptedException, IOException {
        if ( proc.waitFor() != 0 ) {
            throw new RuntimeException( IOUtils.toString( proc.getErrorStream(), StandardCharsets.UTF_8 ) );
        } else {
            return IOUtils.toString( proc.getInputStream(), StandardCharsets.UTF_8 );
        }
    }
}