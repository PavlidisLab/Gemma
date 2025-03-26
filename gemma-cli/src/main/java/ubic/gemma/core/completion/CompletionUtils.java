package ubic.gemma.core.completion;

import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.core.apps.GemmaCLI;

/**
 * Utilities for generating completions.
 * @author poirigui
 */
public class CompletionUtils {

    /**
     * Generate a complete command suitable for {@link ubic.gemma.core.util.EnumeratedByCommandConverter}.
     * <p>
     * The command refers to the {@link ubic.gemma.core.apps.CompleteCli} tool. If you don't need a full Spring context
     * to produce completions, consider implementing  a completion command directly in {@link GemmaCLI}.
     * @param completeArgs additional arguments to pass to the completion command
     */
    public static String[] generateCompleteCommand( CompletionType completionType, String... completeArgs ) {
        return ArrayUtils.addAll( new String[] { GemmaCLI.GEMMA_CLI_EXE, "complete", completionType.name().toLowerCase() }, completeArgs );
    }
}
