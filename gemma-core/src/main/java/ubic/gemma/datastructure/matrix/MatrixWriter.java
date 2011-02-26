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

    /**
     * @param writer
     * @param matrix
     * @param geneAnnotations
     * @param writeHeader
     * @param orderByDesign
     * @throws IOException
     */
    public void write( Writer writer, ExpressionDataMatrix<T> matrix, Map<Long, Collection<Gene>> geneAnnotations,
            boolean writeHeader, boolean orderByDesign ) throws IOException {
        this.write( writer, matrix, geneAnnotations, writeHeader, true, true, orderByDesign );
    }

    /**
     * Alternate method that uses annotations in string form (e.g., read from another file).
     * 
     * @param writer
     * @param matrix
     * @param geneAnnotations Map of composite sequence ids to an array of delimited strings: [probe name,genes symbol,
     *        gene Name]
     * @param writeHeader
     * @throws IOException
     */
    public void writeWithStringifiedGeneAnnotations( Writer writer, ExpressionDataMatrix<T> matrix,
            Map<Long, String[]> geneAnnotations, boolean writeHeader ) throws IOException {
        this.writeWithStringifiedGeneAnnotations( writer, matrix, geneAnnotations, writeHeader, true, true, true );
    }

    /**
     * @param writer
     * @param matrix
     * @param geneAnnotations Map of composite sequence ids to an array of delimited strings: [probe name,genes symbol,
     *        gene Name]
     * @param writeHeader
     * @param writeSequence
     * @param writeGeneInfo
     * @apram orderByDesign
     * @throws IOException
     */
    public void writeWithStringifiedGeneAnnotations( Writer writer, ExpressionDataMatrix<T> matrix,
            Map<Long, String[]> geneAnnotations, boolean writeHeader, boolean writeSequence, boolean writeGeneInfo,
            boolean orderByDesign ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        List<BioMaterial> orderedBioMaterials = getBioMaterialsInRequestedOrder( matrix, orderByDesign );

        StringBuffer buf = new StringBuffer();
        if ( writeHeader ) {
            writeHeader( orderedBioMaterials, matrix, geneAnnotations, writeSequence, writeGeneInfo, columns, buf );
        }

        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            buf.append( probeForRow.getName() + "\t" );
            if ( writeSequence ) {
                BioSequence biologicalCharacteristic = probeForRow.getBiologicalCharacteristic();
                if ( biologicalCharacteristic != null ) buf.append( biologicalCharacteristic.getName() + "\t" );
            }

            if ( writeGeneInfo ) {
                addGeneInfoFromStrings( buf, probeForRow, geneAnnotations );
            }

            int orderedBioMLastIndex = orderedBioMaterials.size() - 1;

            for ( BioMaterial bioMaterial : orderedBioMaterials ) {
                int i = matrix.getColumnIndex( bioMaterial );
                T val = matrix.get( j, i );

                // Don't want line to contain a trailing unnecessary tab
                if ( orderedBioMaterials.indexOf( bioMaterial ) == orderedBioMLastIndex )
                    buf.append( val );
                else
                    buf.append( val + "\t" );

            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
    }

    /**
     * @param writer
     * @param matrix
     * @param geneAnnotations
     * @param writeHeader
     * @param writeSequence
     * @param writeGeneInfo
     * @param orderByDesign if true, the columns are in the order defined by
     *        ExpressionDataMatrixColumnSort.orderByExperimentalDesign
     * @throws IOException
     */
    public void write( Writer writer, ExpressionDataMatrix<T> matrix, Map<Long, Collection<Gene>> geneAnnotations,
            boolean writeHeader, boolean writeSequence, boolean writeGeneInfo, boolean orderByDesign )
            throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        List<BioMaterial> bioMaterials = getBioMaterialsInRequestedOrder( matrix, orderByDesign );

        StringBuffer buf = new StringBuffer();
        if ( writeHeader ) {
            writeHeader( bioMaterials, matrix, geneAnnotations, writeSequence, writeGeneInfo, columns, buf );
        }

        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            buf.append( probeForRow.getName() + "\t" );
            if ( writeSequence ) {
                BioSequence biologicalCharacteristic = probeForRow.getBiologicalCharacteristic();
                if ( biologicalCharacteristic != null ) buf.append( biologicalCharacteristic.getName() + "\t" );
            }

            if ( writeGeneInfo ) {
                addGeneInfo( buf, probeForRow, geneAnnotations );
            }

            int orderedBioMLastIndex = bioMaterials.size() - 1;

            for ( BioMaterial bioMaterial : bioMaterials ) {
                int i = matrix.getColumnIndex( bioMaterial );
                T val = matrix.get( j, i );

                // Don't want line to contain a trailing unnecessary tab
                if ( bioMaterials.indexOf( bioMaterial ) == orderedBioMLastIndex )
                    buf.append( val );
                else
                    buf.append( val + "\t" );

            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
    }

    private List<BioMaterial> getBioMaterialsInRequestedOrder( ExpressionDataMatrix<T> matrix, boolean orderByDesign ) {
        List<BioMaterial> bioMaterials = new ArrayList<BioMaterial>();
        if ( orderByDesign ) {
            bioMaterials = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( matrix );
        } else {
            for ( int i = 0; i < matrix.columns(); i++ ) {
                bioMaterials.add( matrix.getBioMaterialForColumn( i ) );
            }
        }
        return bioMaterials;
    }

    /**
     * @param orderedBiomaterials
     * @param matrix
     * @param geneAnnotations just passed into check it is there.
     * @param writeSequence
     * @param writeGeneInfo
     * @param columns
     * @param buf
     */
    private void writeHeader( List<BioMaterial> orderedBioMaterials, ExpressionDataMatrix<T> matrix,
            Map<Long, ? extends Object> geneAnnotations, boolean writeSequence, boolean writeGeneInfo, int columns,
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
     * @param geneAnnotations Map of composite sequence ids to an array of strings. If null, nothing will be added to
     *        the text. If there are no genes for the probe, then blanks will be added. In each array, the first string
     *        is expected to respresent the gene symbols, the second the names. Any other array elements are ignored.
     */
    private void addGeneInfoFromStrings( StringBuffer buf, CompositeSequence probe, Map<Long, String[]> geneAnnotations ) {
        if ( geneAnnotations == null || geneAnnotations.isEmpty() ) return;
        Long probeid = probe.getId();
        if ( geneAnnotations.containsKey( probeid ) ) {

            String[] geneStrings = geneAnnotations.get( probeid );

            if ( geneStrings.length == 0 ) {
                buf.append( "\t\t" );
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

            // Improve compatibility with third-party programs like R. See bug 1851. Annotation file should already be
            // cleaned, this is just to make sure.
            names = names.replaceAll( "#", "_" );

            // initial tab has already been added before
            buf.append( symbols + "\t" + names + "\t" );
        } else {
            buf.append( "\t\t" );
        }
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
            // tab has already been added before FIXME make sure this format is consistent with other files!!
            buf.append( StringUtils.join( gs.toArray(), '|' ) );
            buf.append( "\t" );
            buf.append( StringUtils.join( gn.toArray(), '|' ) );
            buf.append( "\t" );
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
            BioSequence biologicalCharacteristic = matrix.getDesignElementForRow( j ).getBiologicalCharacteristic();
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
