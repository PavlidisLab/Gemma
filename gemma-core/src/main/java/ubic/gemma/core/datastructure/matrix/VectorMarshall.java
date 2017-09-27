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

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pavlidis
 */
public class VectorMarshall {

    // ByteArrayConverter is stateless.
    private static ByteArrayConverter converter = new ByteArrayConverter();

    /**
     * Convert the data in a DataVector into a List of Objects of the appropriate type for the representation
     * (Boolean,Double,String,Integer,Long,Character);
     *
     * @param vector
     * @return objects
     */
    public static List<Object> marshall( DataVector vector ) {

        byte[] rawDat = vector.getData();
        QuantitationType q = vector.getQuantitationType();
        PrimitiveType representation = q.getRepresentation();
        List<Object> data = new ArrayList<Object>();

        if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
            boolean[] convertedDat = converter.byteArrayToBooleans( rawDat );
            for ( boolean b : convertedDat ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.CHAR ) ) {
            char[] convertedDat = converter.byteArrayToChars( rawDat );
            for ( char b : convertedDat ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.DOUBLE ) ) {
            double[] convertedDat = converter.byteArrayToDoubles( rawDat );
            for ( double b : convertedDat ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.INT ) ) {
            int[] convertedDat = converter.byteArrayToInts( rawDat );
            for ( int b : convertedDat ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.LONG ) ) {
            long[] convertedDat = converter.byteArrayToLongs( rawDat );
            for ( long b : convertedDat ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.STRING ) ) {
            String[] convertedDat = converter.byteArrayToStrings( rawDat );
            for ( String b : convertedDat ) {
                data.add( b );
            }
        } else {
            throw new UnsupportedOperationException( "Don't know how to handle " + representation );
        }

        return data;
    }

}
