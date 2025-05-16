package ubic.gemma.cli.completion;

import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;

public abstract class AbstractCompletionGenerator implements CompletionGenerator {

    /**
     * List of keywords suggesting an option might allow a file as argument.
     */
    private static final String[] FILE_KEYWORDS = { "file", "directory", "folder" };

    protected boolean isFileOption( Option o ) {
        return File.class.equals( o.getType() )
                || Path.class.equals( o.getType() )
                // FIXME: remove all these heuristics, all options should be either File or Path
                || ( String.class.equals( o.getType() ) && o.getConverter() == null && (
                StringUtils.containsAnyIgnoreCase( o.getOpt(), FILE_KEYWORDS )
                        || StringUtils.containsAnyIgnoreCase( o.getLongOpt(), FILE_KEYWORDS )
                        || StringUtils.containsAnyIgnoreCase( o.getArgName(), FILE_KEYWORDS ) ) );
    }
}
