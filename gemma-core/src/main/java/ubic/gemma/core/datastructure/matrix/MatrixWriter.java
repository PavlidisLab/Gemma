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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.StringUtils;
import org.jfree.util.Log;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author pavlidis
 */
public class MatrixWriter {

    public void write( Writer writer, ExpressionDataMatrix<?> matrix,
            Map<CompositeSequence, Collection<Gene>> geneAnnotations, boolean writeHeader, boolean orderByDesign )
            throws IOException {
        this.write( writer, matrix, geneAnnotations, writeHeader, true, true, orderByDesign );
    }

    /**
     * Alternate method that uses annotations in string form (e.g., read from another file).
     *
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @param matrix          the matrix to write
     * @param writeHeader     the writer header
     * @param writer          the writer to use
     * @throws IOException when the write failed
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFileAsString(ArrayDesign)
     */
    public void writeWithStringifiedGeneAnnotations( Writer writer, ExpressionDataMatrix<?> matrix,
            Map<CompositeSequence, String[]> geneAnnotations, boolean writeHeader ) throws IOException {
        this.writeWithStringifiedGeneAnnotations( writer, matrix, geneAnnotations, writeHeader, true, true, true );
    }

    /**
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @param writeHeader     the writer header
     * @param matrix          the matrix
     * @param orderByDesign   if true, the columns are in the order defined by
     *                        ExpressionDataMatrixColumnSort.orderByExperimentalDesign
     * @param writeGeneInfo   whether to write gene info
     * @param writer          the writer to use
     * @param writeSequence   whether to write sequence
     * @throws IOException when the write failed
     */
    @SuppressWarnings("WeakerAccess") // possible external use
    public void writeWithStringifiedGeneAnnotations( Writer writer, ExpressionDataMatrix<?> matrix,
            Map<CompositeSequence, String[]> geneAnnotations, boolean writeHeader, boolean writeSequence,
            boolean writeGeneInfo, boolean orderByDesign ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        List<BioMaterial> orderedBioMaterials = getBioMaterialsInRequestedOrder( matrix, orderByDesign );

        StringBuffer buf = new StringBuffer();
        if ( writeHeader ) {
            writeHeader( orderedBioMaterials, matrix, geneAnnotations, writeSequence, writeGeneInfo, columns, buf );
        }

        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            buf.append( probeForRow.getName() ).append( "\t" );
            writeSequence( writeSequence, buf, probeForRow );

            if ( writeGeneInfo ) {
                addGeneInfoFromStrings( buf, probeForRow, geneAnnotations );
            }

            int orderedBioMLastIndex = orderedBioMaterials.size() - 1;

            for ( BioMaterial bioMaterial : orderedBioMaterials ) {
                int i = matrix.getColumnIndex( bioMaterial );
                Object val = matrix.get( j, i );

                // Don't want line to contain a trailing unnecessary tab
                if ( orderedBioMaterials.indexOf( bioMaterial ) == orderedBioMLastIndex ) {
                    buf.append( val );
                } else {
                    buf.append( val ).append( "\t" );
                }
            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
        writer.flush();
        Log.debug( "Done writing" );
    }

    /**
     * @param orderByDesign   if true, the columns are in the order defined by
     *                        ExpressionDataMatrixColumnSort.orderByExperimentalDesign
     * @param writeSequence   whether to write sequence
     * @param writer          the writer to use
     * @param writeGeneInfo   whether to write gene info
     * @param matrix          the matrix
     * @param writeHeader     the writer header
     * @param geneAnnotations Map of composite sequences to an array of delimited strings: [probe name,genes symbol,
     *                        gene Name] -- these include the "|" to indicate multiple genes, and originate in the platform annotation
     *                        files.
     * @throws IOException when the write failed
     */
    public void write( Writer writer, ExpressionDataMatrix<?> matrix,
            Map<CompositeSequence, Collection<Gene>> geneAnnotations, boolean writeHeader, boolean writeSequence,
            boolean writeGeneInfo, boolean orderByDesign ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        List<BioMaterial> bioMaterials = getBioMaterialsInRequestedOrder( matrix, orderByDesign );

        StringBuffer buf = new StringBuffer();
        if ( writeHeader ) {
            writeHeader( bioMaterials, matrix, geneAnnotations, writeSequence, writeGeneInfo, columns, buf );
        }

        for ( int j = 0; j < rows; j++ ) {
            CompositeSequence probeForRow = matrix.getDesignElementForRow( j );
            buf.append( probeForRow.getName() ).append( "\t" );
            writeSequence( writeSequence, buf, probeForRow );

            if ( writeGeneInfo ) {
                addGeneInfo( buf, probeForRow, geneAnnotations );
            }

            // print the data.
            for ( BioMaterial bioMaterial : bioMaterials ) {
                buf.append( "\t" );

                int i = matrix.getColumnIndex( bioMaterial );
                Object val = matrix.get( j, i );
                if ( val == null || ( val instanceof Double && Double.isNaN( ( Double ) val ) ) ) {
                    buf.append( "" );
                } else if ( val instanceof Double ) {
                    buf.append( String.format( "%.3g", ( Double ) val ) );
                } else {
                    buf.append( val );
                }
            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
        writer.flush();
        Log.debug( "Done writing" );
    }

    private List<BioMaterial> getBioMaterialsInRequestedOrder( ExpressionDataMatrix<?> matrix, boolean orderByDesign ) {
        List<BioMaterial> bioMaterials = new ArrayList<>();
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
     * @param geneAnnotations just passed in to check it is there.
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFileAsString(ArrayDesign)
     */
    private void writeHeader( List<BioMaterial> orderedBioMaterials, ExpressionDataMatrix<?> matrix,
            Map<CompositeSequence, ?> geneAnnotations, boolean writeSequence, boolean writeGeneInfo, int columns,
            StringBuffer buf ) {

        ExpressionDataWriterUtils.appendBaseHeader( matrix.getExpressionExperiment(), false, buf );
        buf.append( "Probe" );
        if ( writeSequence )
            buf.append( "\tSequence" );

        if ( writeGeneInfo && geneAnnotations != null && !geneAnnotations.isEmpty() ) {
            buf.append( "\tGeneSymbol\tGeneName" );
            Object o = geneAnnotations.values().iterator().next();
            if ( o instanceof Collection /* genes */ || ( ( String[] ) o ).length > 4 ) {
                buf.append( "\tGemmaId\tNCBIid" );
            }
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
     * @param probe           The probe to add genes for.
     * @param geneAnnotations Map of composite sequence ids to an array of strings. If null, nothing will be added to
     *                        the text. If there are no genes for the probe, then blanks will be added. In each array, the first string
     *                        is expected to represent the gene symbols, the second the names. Any other array elements are ignored.
     *                        The array of annotations is like this: [probe name,genes symbol, gene Name, gemma gene id, ncbi id]
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl#readAnnotationFileAsString(ArrayDesign)
     */
    private void addGeneInfoFromStrings( StringBuffer buf, CompositeSequence probe,
            Map<CompositeSequence, String[]> geneAnnotations ) {
        if ( geneAnnotations == null || geneAnnotations.isEmpty() )
            return;
        if ( geneAnnotations.containsKey( probe ) ) {

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

            // initial tab has already been added before
            buf.append( symbols ).append( "\t" ).append( names ).append( "\t" ).append( gemmaID ).append( "\t" )
                    .append( ncbiID ).append( "\t" );
        } else {
            buf.append( "\t\t\t\t" );
        }
    }

    /**
     * @param probe           The probe to add genes for.
     * @param geneAnnotations Map of composite sequence ids to genes. If null, nothing will be added to the text. If
     *                        there are no genes for the probe, then blanks will be added.
     */
    private void addGeneInfo( StringBuffer buf, CompositeSequence probe,
            Map<CompositeSequence, Collection<Gene>> geneAnnotations ) {
        if ( geneAnnotations == null || geneAnnotations.isEmpty() )
            return;
        Collection<Gene> genes = geneAnnotations.get( probe );
        if ( genes != null && !genes.isEmpty() ) {

            if ( genes.size() == 1 ) {
                // simple case, avoid some overhead.
                Gene g = genes.iterator().next();
                buf.append( g.getOfficialSymbol() ).append( "\t" ).append( g.getOfficialName() ).append( "\t" )
                        .append( g.getId() ).append( "\t" )
                        .append( g.getNcbiGeneId() == null ? "" : g.getNcbiGeneId().toString() );
            } else {
                List<String> gs = new ArrayList<>();
                List<String> gn = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                List<String> ncbiIds = new ArrayList<>();
                for ( Gene gene : genes ) {
                    gs.add( gene.getOfficialSymbol() );
                    gn.add( gene.getOfficialName() );
                    ids.add( gene.getId() );
                    ncbiIds.add( gene.getNcbiGeneId() == null ? "" : gene.getNcbiGeneId().toString() );
                }

                buf.append( StringUtils.join( gs.toArray(), '|' ) );
                buf.append( "\t" );
                buf.append( StringUtils.join( gn.toArray(), '|' ) );
                buf.append( "\t" );
                buf.append( StringUtils.join( ids.toArray(), '|' ) );
                buf.append( "\t" );
                buf.append( StringUtils.join( ncbiIds.toArray(), '|' ) );
            }
        } else {
            buf.append( "\t\t\t" );
        }
    }

    public void writeJSON( Writer writer, ExpressionDataMatrix<?> matrix, boolean writeHeader ) throws IOException {
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
    }

    private void writeSequence( boolean writeSequence, StringBuffer buf, CompositeSequence probeForRow ) {
        if ( writeSequence ) {
            BioSequence biologicalCharacteristic = probeForRow.getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null )
                buf.append( biologicalCharacteristic.getName() );

            buf.append( "\t" );
        }
    }

}
