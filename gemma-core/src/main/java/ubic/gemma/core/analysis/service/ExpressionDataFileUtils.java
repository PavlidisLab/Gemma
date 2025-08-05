package ubic.gemma.core.analysis.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generate filenames for various types of data files.
 * @author poirigui
 */
public class ExpressionDataFileUtils {

    // for single-cell vectors
    private static final String SC_DATA_SUFFIX = ".scdata";
    public static final String MEX_SC_DATA_SUFFIX = SC_DATA_SUFFIX + ".mex";
    public static final String TABULAR_SC_DATA_SUFFIX = SC_DATA_SUFFIX + ".tsv.gz";
    public static final String CELL_BROWSER_SC_DATA_SUFFIX = SC_DATA_SUFFIX + ".cellbrowser.tsv.gz";

    // for single-cell metadata
    private static final String SC_METADATA_SUFFIX = ".scmetadata";
    private static final String TABULAR_SC_METADATA_FILE_SUFFIX = SC_METADATA_SUFFIX + ".tsv.gz";
    public static final String CELL_BROWSER_SC_METADATA_SUFFIX = SC_METADATA_SUFFIX + ".cellbrowser.tsv.gz";

    // for bulk (raw or processed vectors)
    private static final String BULK_DATA_SUFFIX = ".data";
    public static final String TABULAR_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".txt.gz";
    public static final String JSON_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".json.gz";

    /**
     * Obtain a filename for writing the processed data.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, boolean filtered, String suffix ) {
        return formatExpressionExperimentFilename( ee ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    public static String getDataOutputFilename( ExpressionExperiment ee, List<BioAssay> assays, boolean filtered, String suffix ) {
        return formatExpressionExperimentFilename( ee ) + "_" + formatBioAssaysFilename( assays ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, QuantitationType type, String suffix ) {
        return formatExpressionExperimentFilename( ee ) + "_" + formatIdentifiableFilename( type, QuantitationType::getName ) + "_expmat.unfilt" + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, List<BioAssay> assays, QuantitationType type, String suffix ) {
        return formatExpressionExperimentFilename( ee ) + "_" + formatIdentifiableFilename( type, QuantitationType::getName ) + "_" + formatBioAssaysFilename( assays ) + "_expmat.unfilt" + suffix;
    }

    public static String getMetadataOutputFilename( ExpressionExperiment ee, QuantitationType qt, String suffix ) {
        return formatExpressionExperimentFilename( ee ) + "_" + formatIdentifiableFilename( qt, QuantitationType::getName ) + suffix;
    }

    /**
     * Obtain the filename for writing coexpression data.
     */
    public static String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return formatExpressionExperimentFilename( ee ) + "_coExp" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getDesignFileName( ExpressionExperiment ee ) {
        return formatExpressionExperimentFilename( ee ) + "_expdesign" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getDiffExArchiveFileName( DifferentialExpressionAnalysis diff ) {
        BioAssaySet experimentAnalyzed = diff.getExperimentAnalyzed();
        return formatExperimentAnalyzedFilename( experimentAnalyzed ) + "_diffExpAnalysis"
                // might be for a non-persistent diff ex. analysis
                // the name of a diff. ex. analysis is not really meaningful
                + ( diff.getId() != null ? "_" + diff.getId() : "" )
                + ".zip";
    }

    public static String getMeanVarianceRelationFilename( ExpressionExperiment ee ) {
        return formatIdentifiableFilename( ee, ExpressionExperiment::getShortName ) + "_mvr" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getEigenGenesFilename( ExpressionExperiment ee ) {
        return formatIdentifiableFilename( ee, ExpressionExperiment::getShortName ) + "_eigengenes" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getSingleCellMetadataFilename( ExpressionExperiment ee, QuantitationType qt ) {
        return formatExpressionExperimentFilename( ee ) + "_" + formatIdentifiableFilename( qt, QuantitationType::getName ) + TABULAR_SC_METADATA_FILE_SUFFIX;
    }

    public static String getSingleCellMetadataFilename( ExpressionExperiment ee, QuantitationType qt, CellTypeAssignment cellTypeAssignment ) {
        return String.format( "%s_%s_%s%s", formatExpressionExperimentFilename( ee ),
                formatIdentifiableFilename( qt, QuantitationType::getName ),
                formatIdentifiableFilename( cellTypeAssignment, ExpressionDataFileUtils::getCellLevelCharacteristicsName ),
                TABULAR_SC_METADATA_FILE_SUFFIX );
    }

    public static String getSingleCellMetadataFilename( ExpressionExperiment ee, QuantitationType qt, Collection<CellLevelCharacteristics> cellLevelCharacteristics ) {
        return String.format( "%s_%s_%s%s", formatExpressionExperimentFilename( ee ),
                formatIdentifiableFilename( qt, QuantitationType::getName ),
                cellLevelCharacteristics.stream()
                        .map( clc -> formatIdentifiableFilename( clc, ExpressionDataFileUtils::getCellLevelCharacteristicsName ) )
                        .collect( Collectors.joining( "_" ) ),
                TABULAR_SC_METADATA_FILE_SUFFIX );
    }

    public static String getSingleCellMetadataFilename( ExpressionExperiment ee, QuantitationType qt, CellLevelCharacteristics cellLevelCharacteristics ) {
        return String.format( "%s_%s_%s%s", formatExpressionExperimentFilename( ee ),
                formatIdentifiableFilename( qt, QuantitationType::getName ),
                formatIdentifiableFilename( cellLevelCharacteristics, ExpressionDataFileUtils::getCellLevelCharacteristicsName ),
                TABULAR_SC_METADATA_FILE_SUFFIX );
    }

    private static String getCellLevelCharacteristicsName( CellLevelCharacteristics clc ) {
        if ( clc.getName() != null ) {
            return clc.getName();
        } else if ( clc instanceof CellTypeAssignment ) {
            return "cell_type";
        } else {
            return clc.getCharacteristics().iterator().next().getCategory();
        }
    }

    /**
     * Forms a folder name where the given experiments metadata will be located (within the {@code ${gemma.appdata.home}/metadata} directory).
     */
    public static String getExpressionExperimentMetadataDirname( ExpressionExperiment ee ) {
        Assert.isTrue( StringUtils.isNotBlank( ee.getShortName() ), "Cannot resolve a directory name for an experiment lacking a shortname." );
        return FileTools.cleanForFileName( ee.getShortName() );
    }

    private static String formatExperimentAnalyzedFilename( BioAssaySet experimentAnalyzed ) {
        ExpressionExperiment ee;
        if ( experimentAnalyzed instanceof ExpressionExperiment ) {
            ee = ( ExpressionExperiment ) experimentAnalyzed;
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) experimentAnalyzed;
            ee = subset.getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Don't know about " + experimentAnalyzed.getClass().getName() );
        }
        return formatIdentifiableFilename( experimentAnalyzed, ignored -> ee.getShortName() );

    }

    private static String formatExpressionExperimentFilename( ExpressionExperiment ee ) {
        return formatIdentifiableFilename( ee, ExpressionExperiment::getShortName );
    }

    private static String formatBioAssaysFilename( List<BioAssay> assays ) {
        return assays.stream()
                .map( ExpressionDataFileUtils::formatBioAssayFilename )
                .collect( Collectors.joining( "___" ) );
    }

    public static String formatBioAssayFilename( BioAssay ba ) {
        return formatIdentifiableFilename( ba, ba2 -> ba2.getShortName() != null ? ba2.getShortName() : ba2.getName() );
    }

    private static <T extends Identifiable> String formatIdentifiableFilename( T identifiable, Function<T, String> nameGetter ) {
        return ( identifiable.getId() != null ? identifiable.getId() + "_" : "" ) + FileTools.cleanForFileName( nameGetter.apply( identifiable ) );
    }
}
