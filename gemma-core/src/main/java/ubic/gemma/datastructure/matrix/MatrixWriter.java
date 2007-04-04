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
import java.text.DecimalFormat;
import java.text.NumberFormat;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MatrixWriter<T> {

    private static NumberFormat nf = DecimalFormat.getInstance();
    static {
        nf.setMaximumFractionDigits( 4 );
    }

    /**
     * @param writer
     * @param matrix
     * @return
     */
    public void write( Writer writer, ExpressionDataMatrix<T> matrix ) throws IOException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        StringBuffer buf = new StringBuffer();
        buf.append( "Probe" );
        for ( int i = 0; i < columns; i++ ) {
            buf.append( "\t" + matrix.getBioMaterialForColumn( i ).getName() + ":" );
            for ( Object ba : matrix.getBioAssaysForColumn( i ) ) {
                buf.append( ( ( BioAssay ) ba ).getName() + "," );
            }
        }
        buf.append( "\n" );

        for ( int j = 0; j < rows; j++ ) {

            buf.append( matrix.getDesignElementForRow( j ).getName() );
            BioSequence biologicalCharacteristic = ( ( CompositeSequence ) matrix.getDesignElementForRow( j ) )
                    .getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null ) buf.append( " [" + biologicalCharacteristic.getName() + "]" );

            for ( int i = 0; i < columns; i++ ) {
                T val = matrix.get( j, i );
                buf.append( "\t" + val );
            }

            buf.append( "\n" );

        }
        writer.write( buf.toString() );
    }

}
