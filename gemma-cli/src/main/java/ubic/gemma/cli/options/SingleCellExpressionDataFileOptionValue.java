package ubic.gemma.cli.options;

import lombok.Getter;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Rows are probes/genes, columns are samples/assays.
 *
 * @author poirigui
 */
@Getter
public class SingleCellExpressionDataFileOptionValue extends ExpressionDataFileOptionValue {

    private final boolean useEnsemblIds;

    public SingleCellExpressionDataFileOptionValue( ExpressionDataFileOptionValue dfov, boolean useEnsemblIds ) {
        this( dfov.standardLocation, dfov.standardOutput, dfov.outputFile, dfov.outputDir, dfov.excludeSampleIdentifiers, dfov.useBioAssayIds, dfov.useRawColumnNames, dfov.scaleType, useEnsemblIds, dfov.getForceOption(), dfov.isForce() );
    }

    public SingleCellExpressionDataFileOptionValue( boolean standardLocation, boolean standardOutput, @Nullable Path outputFile, @Nullable Path outputDir, boolean excludeSampleIdentifiers, boolean useBioAssayIds, boolean useRawColumnNames, @Nullable ScaleType scaleType, boolean useEnsemblIds, @Nullable String forceOption, boolean force ) {
        super( standardLocation, standardOutput, outputFile, outputDir, excludeSampleIdentifiers, useBioAssayIds, useRawColumnNames, scaleType, forceOption, force );
        this.useEnsemblIds = useEnsemblIds;
    }
}
