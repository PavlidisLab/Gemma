package ubic.gemma.cli.options;

import lombok.Getter;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Options to write an experimental design file.
 * <p>
 * Rows are samples/assays and columns are factors, sample characteristics or assay characteristics.
 *
 * @author poirigui
 */
@Getter
public class DesignFileOptionValue extends DataFileOptionValue {

    private final boolean useMultipleRowsForAssays;
    private final boolean separateSampleFromAssaysIdentifiers;
    private final boolean useBioAssayIds;
    private final boolean useRawColumnNames;

    public DesignFileOptionValue( DataFileOptionValue dfov, boolean useMultipleRowsForAssays, boolean separateSampleFromAssaysIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames ) {
        this( dfov.standardLocation, dfov.standardOutput, dfov.outputFile, dfov.outputDir, useMultipleRowsForAssays, separateSampleFromAssaysIdentifiers, useBioAssayIds, useRawColumnNames, dfov.getForceOption(), dfov.isForce() );
    }

    public DesignFileOptionValue( boolean standardLocation, boolean standardOutput, @Nullable Path outputFile, @Nullable Path outputDir, boolean useMultipleRowsForAssays, boolean separateSampleFromAssaysIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames, @Nullable String forceOption, boolean force ) {
        super( standardLocation, standardOutput, outputFile, outputDir, forceOption, force );
        this.useMultipleRowsForAssays = useMultipleRowsForAssays;
        this.separateSampleFromAssaysIdentifiers = separateSampleFromAssaysIdentifiers;
        this.useBioAssayIds = useBioAssayIds;
        this.useRawColumnNames = useRawColumnNames;
    }
}
