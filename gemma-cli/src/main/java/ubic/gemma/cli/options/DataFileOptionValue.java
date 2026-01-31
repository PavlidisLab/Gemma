package ubic.gemma.cli.options;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared option values for commands that write data files.
 *
 * @author poirigui
 */
@Getter
@AllArgsConstructor
public class DataFileOptionValue {

    /**
     * Write the file to the standard location in the {@code ${gemma.appdata.home}/dataFiles} directory.
     */
    protected final boolean standardLocation;
    /**
     * Write the file to standard output.
     */
    protected final boolean standardOutput;
    /**
     * Write the file to the given output file.
     */
    @Nullable
    protected final Path outputFile;
    /**
     * Write the file to the given output directory.
     */
    @Nullable
    protected final Path outputDir;
    /**
     * Name of the force option.
     */
    @Nullable
    private final String forceOption;
    /**
     * Indicate if existing files should be overwritten.
     */
    private final boolean force;

    /**
     *
     * @param filenameToUseIfDirectory if the output directory is set, this filename will be used to create the
     *                                 output file. Use utilities in {@link ubic.gemma.core.analysis.service.ExpressionDataFileUtils}
     *                                 to generate the filename.
     */
    public Path getOutputFile( String filenameToUseIfDirectory ) {
        if ( outputFile != null ) {
            return checkIfExists( outputFile );
        } else if ( outputDir != null ) {
            return checkIfExists( outputDir.resolve( filenameToUseIfDirectory ) );
        } else {
            throw new IllegalStateException( "This result does not have an output file or directory set." );
        }
    }

    private Path checkIfExists( Path o ) {
        if ( !force && Files.exists( o ) ) {
            throw new RuntimeException( o + " already exists" + ( forceOption != null ? ", use -" + forceOption + " to overwrite it." : "." ) );
        }
        return o;
    }
}
