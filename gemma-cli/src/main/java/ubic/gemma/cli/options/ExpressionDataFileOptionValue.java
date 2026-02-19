package ubic.gemma.cli.options;

import lombok.Getter;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Options to write an expression data file.
 * <p>
 * Rows are probes/genes, columns are samples/assays.
 *
 * @author poirigui
 */
@Getter
public class ExpressionDataFileOptionValue extends DataFileOptionValue {

    protected final boolean excludeSampleIdentifiers;
    protected final boolean useBioAssayIds;
    protected final boolean useRawColumnNames;
    @Nullable
    protected final ScaleType scaleType;

    public ExpressionDataFileOptionValue( DataFileOptionValue dfov, boolean excludeSampleIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames, @Nullable ScaleType scaleType ) {
        this( dfov.standardLocation, dfov.standardOutput, dfov.outputFile, dfov.outputDir, excludeSampleIdentifiers, useBioAssayIds, useRawColumnNames, scaleType, dfov.getForceOption(), dfov.isForce() );
    }

    public ExpressionDataFileOptionValue( boolean standardLocation, boolean standardOutput, @Nullable Path outputFile, @Nullable Path outputDir, boolean excludeSampleIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames, @Nullable ScaleType scaleType, @Nullable String forceOption, boolean force ) {
        super( standardLocation, standardOutput, outputFile, outputDir, forceOption, force );
        this.excludeSampleIdentifiers = excludeSampleIdentifiers;
        this.useBioAssayIds = useBioAssayIds;
        this.useRawColumnNames = useRawColumnNames;
        this.scaleType = scaleType;
    }
}
