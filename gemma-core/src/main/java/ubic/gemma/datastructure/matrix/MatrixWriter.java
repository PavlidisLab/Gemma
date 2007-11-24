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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
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

    public void write( Writer writer, ExpressionDataMatrix<T> matrix, boolean writeHeader ) throws IOException {
        this.write( writer, matrix, writeHeader, true );
    }

    /**
     * @param writer
     * @param matrix
     * @return
     */
    public void write( Writer writer, ExpressionDataMatrix<T> matrix, boolean writeHeader, boolean writeSequence )
            throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        StringBuffer buf = new StringBuffer();
        if ( writeHeader ) {
            // TO do get gene.
            buf.append( "Probe" );
            if ( writeSequence ) buf.append( "\tSequence" );
            for ( int i = 0; i < columns; i++ ) {
                buf.append( "\t" + matrix.getBioMaterialForColumn( i ).getName() + ":" );
                for ( Object ba : matrix.getBioAssaysForColumn( i ) ) {
                    buf.append( ( ( BioAssay ) ba ).getName() + "," );
                }
            }
            buf.append( "\n" );
        }

        for ( int j = 0; j < rows; j++ ) {

            buf.append( matrix.getDesignElementForRow( j ).getName() );
            if ( writeSequence ) {
                BioSequence biologicalCharacteristic = ( ( CompositeSequence ) matrix.getDesignElementForRow( j ) )
                        .getBiologicalCharacteristic();
                if ( biologicalCharacteristic != null ) buf.append( "\t" + biologicalCharacteristic.getName() );
            }

            for ( int i = 0; i < columns; i++ ) {
                T val = matrix.get( j, i );
                buf.append( "\t" + val );
            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
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
        // if ( writeHeader ) {
        // // TO do get gene.
        // buf.append( "{ 'id' : \"Probe\",\"Sequence\"" );
        // for ( int i = 0; i < columns; i++ ) {
        // buf.append( ",\"" + matrix.getBioMaterialForColumn( i ).getName() + "." );
        // for ( Object ba : matrix.getBioAssaysForColumn( i ) ) {
        // buf.append( ( ( BioAssay ) ba ).getName() + "." );
        // }
        // buf.append("\"");
        // }
        // buf.append( "}\n" );
        // }

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
