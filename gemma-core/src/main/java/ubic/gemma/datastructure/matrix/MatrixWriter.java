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
package ubic.gemma.datastructure.matrix;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MatrixWriter<T> {

    private static NumberFormat nf = NumberFormat.getInstance();
    static {
        nf.setMaximumFractionDigits( 4 );
    }

    public void write( Writer writer, ExpressionDataMatrix<T> matrix, Map<Long, Collection<Gene>> geneAnnotations,
            boolean writeHeader ) throws IOException {
        this.write( writer, matrix, geneAnnotations, writeHeader, true, true );
    }

    /**
     * @param writer
     * @param matrix
     * @param geneAnnotations
     * @param writeHeader
     * @param writeSequence
     * @param writeGeneInfo
     * @throws IOException
     */
    public void write( Writer writer, ExpressionDataMatrix<T> matrix, Map<Long, Collection<Gene>> geneAnnotations,
            boolean writeHeader, boolean writeSequence, boolean writeGeneInfo ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        List<BioMaterial> orderedBioMaterials = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( matrix );

        StringBuffer buf = new StringBuffer();
        if ( writeHeader ) {
            writeHeader( orderedBioMaterials, matrix, geneAnnotations, writeSequence, writeGeneInfo, columns, buf );
        }

        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = ( CompositeSequence ) matrix.getDesignElementForRow( j );
            buf.append( probeForRow.getName() );
            if ( writeSequence ) {
                BioSequence biologicalCharacteristic = probeForRow.getBiologicalCharacteristic();
                if ( biologicalCharacteristic != null ) buf.append( "\t" + biologicalCharacteristic.getName() );
            }

            if ( writeGeneInfo ) {
                addGeneInfo( buf, probeForRow, geneAnnotations );
            }

            for ( BioMaterial bioMaterial : orderedBioMaterials ) {
                int i = matrix.getColumnIndex( bioMaterial );
                T val = matrix.get( j, i );
                buf.append( "\t" + val );
            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
    }

    /**
     * @param orderedBiomaterials
     * @param matrix
     * @param geneAnnotations
     * @param writeSequence
     * @param writeGeneInfo
     * @param columns
     * @param buf
     */
    private void writeHeader( List<BioMaterial> orderedBioMaterials, ExpressionDataMatrix<T> matrix,
            Map<Long, Collection<Gene>> geneAnnotations, boolean writeSequence, boolean writeGeneInfo, int columns,
            StringBuffer buf ) {

        ExpressionDataWriterUtils.appendBaseHeader( matrix.getExpressionExperiment(), false, buf );
        buf.append( "Probe" );
        if ( writeSequence ) buf.append( "\tSequence" );

        if ( writeGeneInfo && geneAnnotations != null && !geneAnnotations.isEmpty() ) {
            buf.append( "\tGeneSymbol\tGeneName" );
        }

        for ( BioMaterial bioMaterial : orderedBioMaterials ) {
            int i = matrix.getColumnIndex( bioMaterial );
            buf.append( "\t" );
            String colName = ExpressionDataWriterUtils.constructBioAssayName( matrix, i );
            buf.append( colName );
        }
        buf.append( "\n" );
    }

    /**
     * @param buf
     * @param probe The probe to add genes for.
     * @param geneAnnotations Map of composite sequence ids to genes. If null, nothing will be added to the text. If
     *        there are no genes for the probe, then blanks will be added.
     */
    private void addGeneInfo( StringBuffer buf, CompositeSequence probe, Map<Long, Collection<Gene>> geneAnnotations ) {
        if ( geneAnnotations == null || geneAnnotations.isEmpty() ) return;
        Collection<Gene> genes = geneAnnotations.get( probe.getId() );
        if ( genes != null ) {
            List<String> gs = new ArrayList<String>();
            List<String> gn = new ArrayList<String>();
            for ( Gene gene : genes ) {
                gs.add( gene.getOfficialSymbol() );
                gn.add( gene.getOfficialName() );
            }

            buf.append( StringUtils.join( gs.toArray(), '|' ) );
            buf.append( "\t" );
            buf.append( StringUtils.join( gn.toArray(), '|' ) );
        } else {
            buf.append( "\t\t" );
        }
    }

    /**
     * @param writer
     * @param matrix
     * @param writeHeader
     * @throws IOException
     */
    public void writeJSON( Writer writer, ExpressionDataMatrix<T> matrix, boolean writeHeader ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        StringBuffer buf = new StringBuffer();

        buf.append( "{ 'numRows' : " + matrix.rows() + ", 'rows': " );

        buf.append( "[" );

        for ( int j = 0; j < rows; j++ ) {

            if ( j > 0 ) buf.append( "," );
            buf.append( "{" );
            buf.append( "'id' : \"" + matrix.getDesignElementForRow( j ).getName() + "\"" );
            BioSequence biologicalCharacteristic = ( ( CompositeSequence ) matrix.getDesignElementForRow( j ) )
                    .getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null )
                buf.append( ", 'sequence' : \"" + biologicalCharacteristic.getName() + "\"" );

            buf.append( ", 'data' : [" );
            for ( int i = 0; i < columns; i++ ) {
                T val = matrix.get( j, i );
                if ( i > 0 ) buf.append( "," );
                buf.append( val );
            }

            buf.append( "]}\n" );

        }
        buf.append( "\n]}" );
        writer.write( buf.toString() );
    }

}
