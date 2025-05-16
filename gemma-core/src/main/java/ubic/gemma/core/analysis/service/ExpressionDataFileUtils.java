package ubic.gemma.core.analysis.service;

import org.apache.commons.lang3.StringUtils;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.List;
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

    // for bulk (raw or processed vectors)
    private static final String BULK_DATA_SUFFIX = ".data";
    public static final String TABULAR_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".txt.gz";
    public static final String JSON_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".json.gz";

    /**
     * Obtain a filename for writing the processed data.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, boolean filtered, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    public static String getDataOutputFilename( ExpressionExperiment ee, List<BioAssay> assays, boolean filtered, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_" + formatAssays( assays ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, QuantitationType type, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_" + type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + "_expmat.unfilt" + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, List<BioAssay> assays, QuantitationType type, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_" + type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + "_" + formatAssays( assays ) + "_expmat.unfilt" + suffix;
    }

    private static String formatAssays( List<BioAssay> assays ) {
        return assays.stream()
                .map( ba -> ba.getShortName() != null ? ba.getId() + "_" + FileTools.cleanForFileName( ba.getShortName() ) : ba.getId() + "_" + FileTools.cleanForFileName( ba.getName() ) )
                .collect( Collectors.joining( "___" ) );
    }

    /**
     * Obtain the filename for writing coexpression data.
     */
    public static String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_coExp" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getDesignFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getDiffExArchiveFileName( DifferentialExpressionAnalysis diff ) {
        BioAssaySet experimentAnalyzed = diff.getExperimentAnalyzed();

        ExpressionExperiment ee;
        if ( experimentAnalyzed instanceof ExpressionExperiment ) {
            ee = ( ExpressionExperiment ) experimentAnalyzed;
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) experimentAnalyzed;
            ee = subset.getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Don't know about " + experimentAnalyzed.getClass().getName() );
        }

        return experimentAnalyzed.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_diffExpAnalysis"
                // might be for a non-persistent diff ex. analysis
                + ( diff.getId() != null ? "_" + diff.getId() : "" )
                + ".zip";
    }

    public static String getMeanVarianceRelationFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_mvr" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getEigenGenesFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_eigengenes" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    /**
     * Forms a folder name where the given experiments metadata will be located (within the {@link #metadataDir} directory).
     *
     * @param ee the experiment to get the folder name for.
     * @return folder name based on the given experiments properties. Usually this will be the experiments short name,
     * without any splitting suffixes (e.g. for GSE123.1 the folder name would be GSE123). If the short name is empty for
     * any reason, the experiments ID will be used.
     */
    public static String getEEFolderName( ExpressionExperiment ee ) {
        String sName = ee.getShortName();
        if ( StringUtils.isBlank( sName ) ) {
            return ee.getId().toString();
        }
        return sName.replaceAll( "\\.\\d+$", "" );
    }
}
