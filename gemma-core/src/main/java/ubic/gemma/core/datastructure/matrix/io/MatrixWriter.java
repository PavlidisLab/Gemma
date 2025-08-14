/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationScaleConversionException;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.MultiAssayBulkExpressionDataMatrix;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.clearScalarConversionThreadLocalStorage;
import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.convertScalar;
import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.appendBaseHeader;
import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.formatQuantitationType;
import static ubic.gemma.core.util.TsvUtils.format;

/**
 * Writes {@link BulkExpressionDataMatrix} to a tabular format.
 * @author pavlidis
 */
@Setter
@CommonsLog
@ParametersAreNonnullByDefault
public class MatrixWriter implements BulkExpressionDataMatrixWriter {

    private final EntityUrlBuilder entityUrlBuilder;
    private final BuildInfo buildInfo;
    /**
     * Only include bioassays in the formed column name instead of prepending it with the sample name.
     */
    private boolean onlyIncludeBioAssayIdentifiers = false;
    /**
     * Use BioAssay IDs (and BioMaterial IDs) in the column names instead of names (or short names).
     */
    private boolean useBioAssayIds = false;
    /**
     * Do not make the column names R-friendly.
     * @see ubic.basecode.util.StringUtil#makeNames(String)
     */
    private boolean useRawColumnNames = false;
    private boolean autoFlush = false;
    @Nullable
    private ScaleType scaleType;

    public MatrixWriter( EntityUrlBuilder entityUrlBuilder, BuildInfo buildInfo ) {
        this.entityUrlBuilder = entityUrlBuilder;
        this.buildInfo = buildInfo;
    }

    @Override
    public int write( BulkExpressionDataMatrix<?> matrix, Class<? extends BulkExpressionDataVector> vectorType, Writer writer ) throws IOException {
        return this.write( matrix, vectorType, null, writer );
    }

    /**
     * @param matrix          the matrix
     * @param vectorType      the type of vector to write
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @param writer          the writer to use
     * @throws IOException when the write failed
     */
    public int write( BulkExpressionDataMatrix<?> matrix, Class<? extends BulkExpressionDataVector> vectorType, @Nullable Map<CompositeSequence, Collection<Gene>> geneAnnotations, Writer writer ) throws IOException {
        QuantitationType qt = matrix.getQuantitationType();

        this.writeHeader( matrix, vectorType, geneAnnotations, writer );

        int rows = matrix.rows();
        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            writer.append( format( probeForRow.getName() ) );
            this.writeSequence( probeForRow, writer );
            if ( geneAnnotations != null ) {
                this.writeGeneInfo( probeForRow, geneAnnotations, writer );
            }
            // print the data.
            for ( int i = 0; i < matrix.columns(); i++ ) {
                writer.append( "\t" );
                writeValue( matrix.get( j, i ), qt, writer );
            }
            writer.append( "\n" );
            if ( autoFlush ) {
                writer.flush();
            }
        }
        if ( scaleType != null ) {
            // avoid leakage when using convertScalar()
            clearScalarConversionThreadLocalStorage();
        }
        log.debug( "Done writing" );
        return rows;
    }

    /**
     * Alternate method that uses annotations in string form (e.g., read from another file).
     *
     * @param writer          the writer to use
     * @param matrix          the matrix to write
     * @param vectorType      the type of vector to write
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @throws IOException when the write failed
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFile(ArrayDesign)
     */
    public int writeWithStringifiedGeneAnnotations( Writer writer, BulkExpressionDataMatrix<?> matrix, Class<? extends BulkExpressionDataVector> vectorType, @Nullable Map<CompositeSequence, String[]> geneAnnotations ) throws IOException {
        QuantitationType qt = matrix.getQuantitationType();

        this.writeHeader( matrix, vectorType, geneAnnotations, writer );

        int rows = matrix.rows();
        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            writer.append( format( probeForRow.getName() ) );
            this.writeSequence( probeForRow, writer );
            if ( geneAnnotations != null ) {
                this.writeStringifiedGeneInfo( probeForRow, geneAnnotations, writer );
            }

            for ( int i = 0; i < matrix.columns(); i++ ) {
                Object val = matrix.get( j, i );
                writer.append( "\t" );
                writeValue( val, qt, writer );
            }

            writer.append( "\n" );
            if ( autoFlush ) {
                writer.flush();
            }
        }
        log.debug( "Done writing" );
        return rows;
    }

    private void writeHeader( BulkExpressionDataMatrix<?> matrix, Class<? extends BulkExpressionDataVector> vectorType,
            @Nullable Map<CompositeSequence, ?> geneAnnotations, Writer writer ) throws IOException {

        ExpressionExperiment experiment = matrix.getExpressionExperiment();
        if ( experiment != null ) {
            String experimentUrl = entityUrlBuilder.fromHostUrl().entity( experiment ).web().toUriString();
            appendBaseHeader( experiment, "Expression data", experimentUrl, buildInfo, new Date(), writer );
        } else {
            log.warn( "Provided matrix does not have an ExpressionExperiment, omitting it from the base header." );
            appendBaseHeader( "Expression data", buildInfo, new Date(), writer );
        }

        if ( matrix instanceof MultiAssayBulkExpressionDataMatrix ) {
            Collection<QuantitationType> qts = ( ( MultiAssayBulkExpressionDataMatrix<?> ) matrix ).getQuantitationTypes();
            for ( QuantitationType qt : qts ) {
                writer.append( "# Quantitation type: " ).append( formatQuantitationType( qt, vectorType ) ).append( "\n" );
            }
        } else {
            writer.append( "# Quantitation type: " ).append( formatQuantitationType( matrix.getQuantitationType(), vectorType ) ).append( "\n" );
        }

        if ( autoFlush ) {
            writer.flush();
        }

        writer.append( "Probe\tSequence" );

        if ( geneAnnotations != null ) {
            writer.append( "\tGeneSymbol\tGeneName\tGemmaId\tNCBIid" );
        }

        for ( int i = 0; i < matrix.columns(); i++ ) {
            String colName;
            if ( onlyIncludeBioAssayIdentifiers ) {
                colName = ExpressionDataWriterUtils.constructAssayName( matrix.getBioAssayForColumn( i ), useBioAssayIds, useRawColumnNames );
            } else {
                BioMaterial bioMaterialForColumn = matrix.getBioMaterialForColumn( i );
                Collection<BioAssay> bioAssaysForColumn;
                if ( matrix instanceof MultiAssayBulkExpressionDataMatrix ) {
                    bioAssaysForColumn = ( ( MultiAssayBulkExpressionDataMatrix<?> ) matrix ).getBioAssaysForColumn( i );
                } else {
                    bioAssaysForColumn = Collections.singleton( matrix.getBioAssayForColumn( i ) );
                }
                colName = ExpressionDataWriterUtils.constructSampleName( bioMaterialForColumn, bioAssaysForColumn, useBioAssayIds, useRawColumnNames, '.' );
            }
            writer.append( "\t" ).append( format( colName ) );
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }

    /**
     * @param probe           The probe to add genes for.
     * @param geneAnnotations Map of composite sequence ids to an array of strings. If null, nothing will be added to
     *                        the text. If there are no genes for the probe, then blanks will be added. In each array, the first string
     *                        is expected to represent the gene symbols, the second the names. Any other array elements are ignored.
     *                        The array of annotations is like this: [probe name,genes symbol, gene Name, gemma gene id, ncbi id]
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFile(ArrayDesign)
     */
    private void writeStringifiedGeneInfo( CompositeSequence probe, Map<CompositeSequence, String[]> geneAnnotations, Writer buf ) throws IOException {
        if ( !geneAnnotations.containsKey( probe ) ) {
            buf.append( "\t\t\t\t" );
            return;
        }

        String[] geneStrings = geneAnnotations.get( probe );

        if ( geneStrings.length == 0 ) {
            buf.append( "\t\t\t\t" );
            return;
        }

        String symbols = "";

        if ( geneStrings.length > 1 && geneStrings[1] != null ) {
            symbols = geneStrings[1];
        }

        String names = "";
        if ( geneStrings.length > 2 && geneStrings[2] != null ) {
            names = geneStrings[2];
        }

        String gemmaID = "";
        if ( geneStrings.length > 3 && geneStrings[3] != null ) {
            gemmaID = geneStrings[3];
        }

        String ncbiID = "";
        if ( geneStrings.length > 4 && geneStrings[4] != null ) {
            ncbiID = geneStrings[4];
        }

        // Improve compatibility with third-party programs like R. See bug 1851. Annotation file should already be
        // cleaned, this is just to make sure.
        names = names.replaceAll( "#", "_" );

        buf.append( "\t" ).append( format( symbols ) )
                .append( "\t" ).append( format( names ) )
                .append( "\t" ).append( format( gemmaID ) )
                .append( "\t" ).append( format( ncbiID ) );
    }

    /**
     * @param probe           The probe to add genes for.
     * @param geneAnnotations Map of composite sequence ids to genes. If null, nothing will be added to the text. If
     *                        there are no genes for the probe, then blanks will be added.
     */
    private void writeGeneInfo( CompositeSequence probe, Map<CompositeSequence, Collection<Gene>> geneAnnotations, Writer writer ) throws IOException {
        Collection<Gene> genes = geneAnnotations.get( probe );
        if ( genes == null || genes.isEmpty() ) {
            writer.append( "\t\t\t" );
        } else if ( genes.size() == 1 ) {
            // simple case, avoid some overhead.
            Gene g = genes.iterator().next();
            writer.append( "\t" ).append( format( g.getOfficialSymbol() ) )
                    .append( "\t" ).append( format( g.getOfficialName() ) )
                    .append( "\t" ).append( format( g.getId() ) )
                    .append( "\t" ).append( format( g.getNcbiGeneId() ) );
        } else {
            List<String> gs = new ArrayList<>();
            List<String> gn = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            List<String> ncbiIds = new ArrayList<>();
            for ( Gene gene : genes ) {
                gs.add( format( gene.getOfficialSymbol() ) );
                gn.add( format( gene.getOfficialName() ) );
                ids.add( format( gene.getId() ) );
                ncbiIds.add( format( gene.getNcbiGeneId() ) );
            }
            writer
                    .append( "\t" ).append( format( gs ) )
                    .append( "\t" ).append( format( gn ) )
                    .append( "\t" ).append( format( ids ) )
                    .append( "\t" ).append( format( ncbiIds ) );
        }
    }

    private void writeSequence( CompositeSequence probeForRow, Writer buf ) throws IOException {
        BioSequence biologicalCharacteristic = probeForRow.getBiologicalCharacteristic();
        buf.append( "\t" );
        if ( biologicalCharacteristic != null ) {
            buf.append( format( biologicalCharacteristic.getName() ) );
        }
    }

    private void writeValue( Object val, QuantitationType qt, Writer buf ) throws IOException {
        if ( scaleType != null ) {
            try {
                buf.write( format( convertScalar( ( Number ) val, qt, scaleType ) ) );
            } catch ( UnsupportedQuantitationScaleConversionException e ) {
                throw new RuntimeException( e );
            }
        } else {
            buf.write( format( val ) );
        }
    }
}
