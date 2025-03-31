package ubic.gemma.cli.completion;

import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.apps.CompleteCli;
import ubic.gemma.cli.main.GemmaCLI;
import ubic.gemma.cli.util.EnumeratedByCommandConverter;
import ubic.gemma.core.util.TsvUtils;

import java.io.PrintStream;

/**
 * Utilities for generating completions.
 * @author poirigui
 */
public class CompletionUtils {

    /**
     * Generate a complete command suitable for {@link EnumeratedByCommandConverter}.
     * <p>
     * The command refers to the {@link CompleteCli} tool. If you don't need a full Spring context
     * to produce completions, consider implementing  a completion command directly in {@link GemmaCLI}.
     * @param completeArgs additional arguments to pass to the completion command
     */
    public static String[] generateCompleteCommand( CompletionType completionType, String... completeArgs ) {
        return ArrayUtils.addAll( new String[] { GemmaCLI.GEMMA_CLI_EXE, "complete", completionType.name().toLowerCase() }, completeArgs );
    }

    public static void writeCompletions( CompletionSource completionSource, PrintStream out ) {
        completionSource.getCompletions().forEach( ( c ) -> {
            out.printf( "%s\t%s%n", c.getName(), TsvUtils.format( c.getDescription() ) );
        } );
    }
}
