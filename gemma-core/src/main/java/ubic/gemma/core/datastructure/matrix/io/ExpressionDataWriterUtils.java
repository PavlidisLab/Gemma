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

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.MultiAssayBulkExpressionDataMatrix;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.Constants;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.TsvUtils.format;

/**
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ExpressionDataWriterUtils {

    private static final String DELIMITER_BETWEEN_BIOMATERIAL_AND_BIOASSAYS = "___";

    /**
     * Append a base header to a file.
     */
    public static void appendBaseHeader( String fileTypeStr, BuildInfo buildInfo, Writer buf ) throws IOException {
        buf.append( "# " ).append( fileTypeStr ).append( " file generated by Gemma " ).append( buildInfo.getVersion() ).append( " on " ).append( format( new Date() ) ).append( "\n" );
        buf.append( "#\n" );
        for ( String line : Constants.GEMMA_CITATION_NOTICE ) {
            buf.append( "# " ).append( line ).append( "\n" );
        }
        buf.append( "#\n" );
        buf.append( "# " ).append( Constants.GEMMA_LICENSE_NOTICE ).append( "\n" );
    }

    /**
     * Appends base header information (about the experiment) to a file.
     *
     * @param buf         buffer
     * @param fileTypeStr file type str
     * @param experiment  ee
     * @param experimentUrl an URL for the expriment, or null to ommit
     */
    public static void appendBaseHeader( ExpressionExperiment experiment, String fileTypeStr, @Nullable String experimentUrl, BuildInfo buildInfo, Writer buf ) throws IOException {
        appendBaseHeader( fileTypeStr, buildInfo, buf );
        buf.append( "#\n" );
        buf.append( "# shortName=" ).append( experiment.getShortName() ).append( "\n" );
        buf.append( "# name=" ).append( experiment.getName() ).append( "\n" );
        if ( experimentUrl != null ) {
            buf.append( "# Experiment details: " ).append( experimentUrl ).append( "\n" );
        }
    }

    /**
     * Constructs a sample name for a given column of a data matrix.
     *
     * @param matrix           matrix
     * @param assayColumnIndex The column index in the matrix.
     * @return BA name
     */
    public static String constructSampleName( BulkExpressionDataMatrix<?> matrix, int assayColumnIndex ) {
        BioMaterial bioMaterialForColumn = matrix.getBioMaterialForColumn( assayColumnIndex );
        Collection<BioAssay> bioAssaysForColumn;
        if ( matrix instanceof MultiAssayBulkExpressionDataMatrix ) {
            bioAssaysForColumn = ( ( MultiAssayBulkExpressionDataMatrix<?> ) matrix ).getBioAssaysForColumn( assayColumnIndex );
        } else {
            bioAssaysForColumn = Collections.singleton( matrix.getBioAssayForColumn( assayColumnIndex ) );
        }
        return constructSampleName( bioMaterialForColumn, bioAssaysForColumn );
    }

    /**
     * Construct a sample name in case there is only one BioAssay attached to the corresponding BioMaterial.
     */
    public static String constructSampleName( BioMaterial bm, BioAssay ba ) {
        return constructSampleName( bm, Collections.singleton( ba ) );
    }

    /**
     * Construct a BioAssay column name prefixed by the {@link BioMaterial} from which it originates.
     */
    public static String constructSampleName( BioMaterial bioMaterial, Collection<BioAssay> bioAssays ) {
        return constructRCompatibleColumnName( bioMaterial.getName()
                + DELIMITER_BETWEEN_BIOMATERIAL_AND_BIOASSAYS
                // sort for consistency
                + bioAssays.stream().map( ba -> ba.getShortName() != null ? ba.getShortName() : ba.getName() ).sorted().collect( Collectors.joining( "." ) ) );
    }

    /**
     * Produce a value for representing a factor value.
     * <p>
     * In the context of the design file, this is focusing on the value (i.e. subjects or measurement value) itself and
     * not its metadata which are instead exposed in the file header.
     * <p>
     * Replaces spaces and hyphens with underscores.
     * @param factorValue FV
     * @return replaced string
     */
    public static String constructFactorValueName( FactorValue factorValue ) {
        String v;
        if ( factorValue.getMeasurement() != null ) {
            // FIXME: have a special encoding for missing values instead of 'null'
            v = String.valueOf( factorValue.getMeasurement().getValue() );
        } else {
            String valueFromStatements = factorValue.getCharacteristics().stream()
                    .map( Statement::getSubject )
                    .collect( Collectors.joining( " | " ) );
            if ( StringUtils.isNotBlank( valueFromStatements ) ) {
                v = valueFromStatements;
            } else if ( StringUtils.isNotBlank( factorValue.getValue() ) ) {
                v = factorValue.getValue();
            } else {
                v = ""; // this is treated as NaN in most scenarios
            }
        }
        return v.replace( '-', '_' )
                .replaceAll( "\\s+", "_" );
    }

    /**
     * @param bioAssays   BAs
     * @param bioMaterial BM
     * @return string representing the external identifier of the biomaterial. This will usually be a GEO or
     * ArrayExpress accession, or {@code null} if no such identifier is available.
     */
    @Nullable
    public static String constructSampleExternalId( BioMaterial bioMaterial, Collection<BioAssay> bioAssays ) {
        if ( bioMaterial.getExternalAccession() != null ) {
            return constructRCompatibleColumnName( bioMaterial.getExternalAccession().getAccession() );
        } else if ( !bioAssays.isEmpty() ) {
            // use the external IDs of the associated bioassays
            List<String> ids = new ArrayList<>();
            for ( BioAssay ba : bioAssays ) {
                if ( ba.getAccession() != null ) {
                    ids.add( ba.getAccession().getAccession() );
                }
            }
            return !ids.isEmpty() ? constructRCompatibleColumnName( StringUtils.join( ids, "/" ) ) : null;
        } else {
            return null;
        }
    }

    private static String constructRCompatibleColumnName( String colName ) {
        Assert.isTrue( StringUtils.isNotBlank( colName ) );
        colName = StringUtils.deleteWhitespace( colName );
        colName = StringUtils.replaceChars( colName, ':', '.' );
        colName = StringUtils.replaceChars( colName, '\'', '.' );
        colName = StringUtils.replaceChars( colName, '|', '.' );
        colName = StringUtils.replaceChars( colName, '-', '.' );
        return colName;
    }
}
