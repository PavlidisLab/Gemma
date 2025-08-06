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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationScaleConversionException;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.core.datastructure.matrix.MultiAssayBulkExpressionDataMatrix;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
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
import static ubic.gemma.core.util.TsvUtils.SUB_DELIMITER;
import static ubic.gemma.core.util.TsvUtils.format;

/**
 * Writes {@link BulkExpressionDataMatrix} to various tabular formats.
 * @author pavlidis
 */
@CommonsLog
@ParametersAreNonnullByDefault
public class MatrixWriter implements BulkExpressionDataMatrixWriter {

    private final EntityUrlBuilder entityUrlBuilder;
    private final BuildInfo buildInfo;

    private boolean autoFlush = false;

    @Nullable
    private ScaleType scaleType;

    public MatrixWriter( EntityUrlBuilder entityUrlBuilder, BuildInfo buildInfo ) {
        this.entityUrlBuilder = entityUrlBuilder;
        this.buildInfo = buildInfo;
    }

    @Override
    public void setAutoFlush( boolean autoFlush ) {
        this.autoFlush = autoFlush;
    }

    @Override
    public void setScaleType( @Nullable ScaleType scaleType ) {
        this.scaleType = scaleType;
    }

    @Override
    public int write( BulkExpressionDataMatrix<?> matrix, Writer writer ) throws IOException {
        return this.write( matrix, null, writer );
    }

    /**
     * @param writer          the writer to use
     * @param matrix          the matrix
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @throws IOException when the write failed
     */
    public int write( BulkExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Collection<Gene>> geneAnnotations, Writer writer ) throws IOException {
        QuantitationType qt = matrix.getQuantitationType();

        List<BioMaterial> bioMaterials = this.getBioMaterialsInRequestedOrder( matrix, false );
        this.writeHeader( bioMaterials, matrix, geneAnnotations, writer );

        int rows = matrix.rows();
        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            writer.append( format( probeForRow.getName() ) );
            this.writeSequence( probeForRow, writer );
            if ( geneAnnotations != null ) {
                this.writeGeneInfo( probeForRow, geneAnnotations, writer );
            }
            // print the data.
            for ( BioMaterial bioMaterial : bioMaterials ) {
                int i = matrix.getColumnIndex( bioMaterial );
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
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @param matrix          the matrix to write
     * @param writer          the writer to use
     * @throws IOException when the write failed
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFile(ArrayDesign)
     */
    public int writeWithStringifiedGeneAnnotations( Writer writer, BulkExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, String[]> geneAnnotations ) throws IOException {
        QuantitationType qt = matrix.getQuantitationType();

        List<BioMaterial> orderedBioMaterials = this.getBioMaterialsInRequestedOrder( matrix, true );

        this.writeHeader( orderedBioMaterials, matrix, geneAnnotations, writer );

        int rows = matrix.rows();
        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            writer.append( format( probeForRow.getName() ) );
            this.writeSequence( probeForRow, writer );
            if ( geneAnnotations != null ) {
                this.writeStringifiedGeneInfo( probeForRow, geneAnnotations, writer );
            }

            for ( BioMaterial bioMaterial : orderedBioMaterials ) {
                int i = matrix.getColumnIndex( bioMaterial );
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

    public int writeJSON( Writer writer, BulkExpressionDataMatrix<?> matrix ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        StringBuilder buf = new StringBuilder();

        buf.append( "{ 'numRows' : " ).append( matrix.rows() ).append( ", 'rows': " );

        buf.append( "[" );

        for ( int j = 0; j < rows; j++ ) {

            if ( j > 0 )
                buf.append( "," );
            buf.append( "{" );
            buf.append( "'id' : \"" ).append( matrix.getDesignElementForRow( j ).getName() ).append( "\"" );
            BioSequence biologicalCharacteristic = matrix.getDesignElementForRow( j ).getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null )
                buf.append( ", 'sequence' : \"" ).append( biologicalCharacteristic.getName() ).append( "\"" );

            buf.append( ", 'data' : [" );
            for ( int i = 0; i < columns; i++ ) {
                Object val = matrix.get( j, i );
                if ( i > 0 )
                    buf.append( "," );
                buf.append( val );
            }

            buf.append( "]}\n" );
        }
        buf.append( "\n]}" );
        writer.write( buf.toString() );
        return rows;
    }

    private List<BioMaterial> getBioMaterialsInRequestedOrder( BulkExpressionDataMatrix<?> matrix, boolean orderByDesign ) {
        List<BioMaterial> bioMaterials = new ArrayList<>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            bioMaterials.add( matrix.getBioMaterialForColumn( i ) );
        }
        if ( orderByDesign ) {
            return ExpressionDataMatrixColumnSort.orderByExperimentalDesign( bioMaterials, null, null );
        } else {
            return bioMaterials;
        }
    }

    /**
     * @param geneAnnotations just passed in to check it is there.
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFile(ArrayDesign)
     */
    private void writeHeader( List<BioMaterial> orderedBioMaterials, BulkExpressionDataMatrix<?> matrix,
            @Nullable Map<CompositeSequence, ?> geneAnnotations, Writer writer ) throws IOException {

        ExpressionExperiment experiment = matrix.getExpressionExperiment();
        if ( experiment != null ) {
            String experimentUrl = entityUrlBuilder.fromHostUrl().entity( experiment ).web().toUriString();
            appendBaseHeader( experiment, "Expression data", experimentUrl, buildInfo, writer );
        } else {
            log.warn( "Provided matrix does not have an ExpressionExperiment, omitting it from the base header." );
            appendBaseHeader( "Expression data", buildInfo, writer );
        }

        Collection<QuantitationType> qts;
        if ( matrix instanceof MultiAssayBulkExpressionDataMatrix ) {
            qts = ( ( MultiAssayBulkExpressionDataMatrix<?> ) matrix ).getQuantitationTypes();
        } else {
            qts = Collections.singleton( matrix.getQuantitationType() );
        }
        for ( QuantitationType qt : qts ) {
            writer.append( "# Quantitation type: " ).append( qt.toString() ).append( "\n" );
        }

        if ( autoFlush ) {
            writer.flush();
        }

        writer.append( "Probe\tSequence" );

        if ( geneAnnotations != null ) {
            writer.append( "\tGeneSymbol\tGeneName\tGemmaId\tNCBIid" );
        }

        for ( BioMaterial bioMaterial : orderedBioMaterials ) {
            int i = matrix.getColumnIndex( bioMaterial );
            String colName = ExpressionDataWriterUtils.constructSampleName( matrix, i );
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
                    .append( "\t" ).append( StringUtils.join( gs, SUB_DELIMITER ) )
                    .append( "\t" ).append( StringUtils.join( gn, SUB_DELIMITER ) )
                    .append( "\t" ).append( StringUtils.join( ids, SUB_DELIMITER ) )
                    .append( "\t" ).append( StringUtils.join( ncbiIds, SUB_DELIMITER ) );
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
