package ubic.gemma.cli.options;

import lombok.Getter;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Options to write cell metadata file.
 * <p>
 * Rows are cells and columns are cell metadata.
 *
 * @author poirigui
 */
@Getter
public class CellMetadataFileOptionValue extends DataFileOptionValue {

    private final boolean separateSampleFromAssaysIdentifiers;
    private final boolean useBioAssayIds;
    private final boolean useRawColumnNames;

    public CellMetadataFileOptionValue( DataFileOptionValue dfov, boolean separateSampleFromAssaysIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames ) {
        this( dfov.standardLocation, dfov.standardOutput, dfov.outputFile, dfov.outputDir, separateSampleFromAssaysIdentifiers, useBioAssayIds, useRawColumnNames, dfov.getForceOption(), dfov.isForce() );
    }

    public CellMetadataFileOptionValue( boolean standardLocation, boolean standardOutput, @Nullable Path outputFile, @Nullable Path outputDir, boolean separateSampleFromAssaysIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames, @Nullable String forceOption, boolean force ) {
        super( standardLocation, standardOutput, outputFile, outputDir, forceOption, force );
        this.separateSampleFromAssaysIdentifiers = separateSampleFromAssaysIdentifiers;
        this.useBioAssayIds = useBioAssayIds;
        this.useRawColumnNames = useRawColumnNames;
    }
}
