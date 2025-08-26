/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.datastructure.matrix.io;

import org.springframework.util.Assert;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.TsvUtils.format;

/**
 * Utilities for writing expression data files.
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ExpressionDataWriterUtils {

    private static final String DELIMITER_BETWEEN_BIOMATERIAL_AND_BIOASSAYS = "___";

    public static void appendBaseHeader( String fileTypeStr, BuildInfo buildInfo, Date timestamp, Writer buf ) throws IOException {
        TsvUtils.appendBaseHeader( fileTypeStr, buildInfo, timestamp, buf );
    }

    /**
     * Appends base header information (about the experiment) to a file.
     *
     * @param experiment    ee
     * @param fileTypeStr   file type str
     * @param experimentUrl an URL for the expriment, or null to omit
     * @param timestamp     timestamp to include in the header that reflect the time of file generation
     * @param buf           buffer
     */
    public static void appendBaseHeader( ExpressionExperiment experiment, String fileTypeStr, @Nullable String experimentUrl, BuildInfo buildInfo, Date timestamp, Writer buf ) throws IOException {
        TsvUtils.appendBaseHeader( fileTypeStr, buildInfo, timestamp, buf );
        buf.append( "#\n" );
        buf.append( "# Short name: " ).append( format( experiment.getShortName() ) ).append( "\n" );
        buf.append( "# Name: " ).append( format( experiment.getName() ) ).append( "\n" );
        if ( experimentUrl != null ) {
            buf.append( "# Experiment details: " ).append( experimentUrl ).append( "\n" );
        }
    }

    /**
     * Append base header information (about the experiment) to a file with some information about the quantitation type.
     */
    public static void appendBaseHeader( ExpressionExperiment experiment, QuantitationType quantitationType, Class<? extends DataVector> dataVectorType, String fileTypeStr, @Nullable String experimentUrl, BuildInfo buildInfo, Date timestamp, Writer buf ) throws IOException {
        appendBaseHeader( experiment, fileTypeStr, experimentUrl, buildInfo, timestamp, buf );
        buf.append( "# Quantitation type: " ).append( formatQuantitationType( quantitationType, dataVectorType ) ).append( "\n" );
    }

    public static String formatQuantitationType( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType ) {
        StringBuilder sb = new StringBuilder();
        sb.append( quantitationType.getName() );
        if ( ProcessedExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            sb.append( " [Data Type=Processed]" );
            sb.append( " [Preferred]" );
        } else if ( RawExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            sb.append( " [Data Type=Raw]" );
            if ( quantitationType.getIsPreferred() ) {
                sb.append( " [Preferred]" );
            }
        } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            sb.append( " [Data Type=Single-cell]" );
            if ( quantitationType.getIsSingleCellPreferred() ) {
                sb.append( " [Preferred]" );
            }
        } else {
            throw new UnsupportedOperationException( "Unsupported data vector type: " + dataVectorType.getName() );
        }
        sb.append( " [General Type=" ).append( quantitationType.getGeneralType() ).append( "]" );
        sb.append( " [Type=" ).append( quantitationType.getType() ).append( "]" );
        sb.append( " [Scale=" ).append( quantitationType.getScale() ).append( "]" );
        sb.append( " [Representation=" ).append( quantitationType.getRepresentation() ).append( "]" );
        if ( quantitationType.getIsRatio() ) {
            sb.append( " [Ratio]" );
        }
        if ( quantitationType.getIsBackground() ) {
            sb.append( " [Background]" );
        }
        if ( quantitationType.getIsBackgroundSubtracted() ) {
            sb.append( " [Background-subtracted]" );
        }
        if ( ProcessedExpressionDataVector.class.isAssignableFrom( dataVectorType ) && quantitationType.getIsNormalized() ) {
            sb.append( " [Quantile-normalized]" );
        } else if ( quantitationType.getIsNormalized() ) {
            sb.append( " [Normalized]" );
        }
        if ( quantitationType.getIsBatchCorrected() ) {
            sb.append( " [Batch-corrected]" );
        }
        return sb.toString();
    }

    /**
     * Construct a sample name in case there is only one BioAssay attached to the corresponding BioMaterial.
     * @see #constructSampleName(BioMaterial, Collection, boolean, boolean, char)
     */
    public static String constructSampleName( BioMaterial bm, BioAssay ba, boolean useIds, boolean useRawColumnNames ) {
        return constructSampleName( bm, Collections.singleton( ba ), useIds, useRawColumnNames, '.' );
    }

    public static String constructSampleName( BioMaterial bioMaterial, boolean useIds, boolean useRawColumnNames ) {
        String colName = ( useIds ? String.valueOf( bioMaterial.getId() ) : bioMaterial.getName() );
        return useRawColumnNames ? colName : StringUtil.makeNames( colName );
    }

    public static String constructAssaysName( Collection<BioAssay> bas, boolean useIds, boolean useRawColumnNames, char assayDelimiter ) {
        String colName = bas.stream().map( ba -> getBioAssayName( ba, useIds ).replace( assayDelimiter, '_' ) ).sorted().collect( Collectors.joining( String.valueOf( assayDelimiter ) ) );
        return useRawColumnNames ? colName : StringUtil.makeNames( colName );
    }

    /**
     * Construct a BioAssay column name prefixed by the {@link BioMaterial} from which it originates.
     * @param bioMaterial       the biomaterial
     * @param bioAssays         the bioassay(s) associated to the biomaterial
     * @param useIds            use biomaterial and bioassay IDs instead of names (or short names)
     * @param useRawColumnNames do not clean up the names with {@link StringUtil#makeNames(String)} to make them
     *                          R-friendly
     * @param assayDelimiter    the delimiter to use between bioassays, if that delimiter appears in the bioassay name,
     *                          it will be replaced with a '_'. For this reason, this function does not allow '_' as a
     *                          delimiter.
     */
    public static String constructSampleName( BioMaterial bioMaterial, Collection<BioAssay> bioAssays, boolean useIds, boolean useRawColumnNames, char assayDelimiter ) {
        Assert.isTrue( assayDelimiter != '_', "Cannot use '_' as assay delimiter, it is reserved." );
        String colName = ( useIds ? bioMaterial.getId() : bioMaterial.getName() )
                + DELIMITER_BETWEEN_BIOMATERIAL_AND_BIOASSAYS
                // use raw here, we'll clean it up afterward
                + constructAssaysName( bioAssays, useIds, true, assayDelimiter );
        return useRawColumnNames ? colName : StringUtil.makeNames( colName );
    }

    /**
     * Construct a BioAssay column name, unprefixed by the {@link BioMaterial} from which it originates.
     */
    public static String constructAssayName( BioAssay ba, boolean useIds, boolean useRawColumnNames ) {
        String colName = getBioAssayName( ba, useIds );
        return useRawColumnNames ? colName : StringUtil.makeNames( colName );
    }

    /**
     * Construct a BioAssay column name, unprefixed by the {@link BioMaterial} from which it originates.
     */
    public static String constructCellIdName( BioAssay ba, String cellId, boolean useBioAssayIds, boolean useRawColumnNames ) {
        String colName = getBioAssayName( ba, useBioAssayIds ) + "_" + cellId;
        return useRawColumnNames ? colName : StringUtil.makeNames( colName );
    }

    private static String getBioAssayName( BioAssay ba, boolean useIds ) {
        return useIds ? String.valueOf( ba.getId() ) : ( ba.getShortName() != null ? ba.getShortName() : ba.getName() );
    }

    /**
     * Construct an ExperimentalFactor column name.
     * @see StringUtil#makeNames(String)
     */
    public static String constructExperimentalFactorName( ExperimentalFactor ef ) {
        return StringUtil.makeNames( ef.getName() );
    }

    /**
     * Construct an ExperimentalFactor column names for a list of factors.
     * @see StringUtil#makeNames(String[], boolean)
     */
    public static String[] constructExperimentalFactorNames( List<ExperimentalFactor> factors ) {
        String[] colNames = factors.stream()
                .map( ExperimentalFactor::getName )
                .toArray( String[]::new );
        return StringUtil.makeNames( colNames, true );
    }
}
