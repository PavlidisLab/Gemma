/*
 * The baseCode project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.util.matrix;

/**
 * Use this class when fast iteration over the matrix is of primary interest. The primary change is that getQuick is
 * faster, and toArray returns the actual elements, not a copy.
 * 
 * @author pavlidis
 * 
 */
public class DenseDoubleMatrix1D extends cern.colt.matrix.impl.DenseDoubleMatrix1D {

    /**
     * 
     */
    private static final long serialVersionUID = -5196826500179433512L;

    /**
     * @param values
     */
    public DenseDoubleMatrix1D( double[] values ) {
        super( values );
    }

    /**
     * @param size
     */
    public DenseDoubleMatrix1D( int size ) {
        super( size );
    }

    /**
     * @param size
     * @param elements
     * @param zero
     * @param stride
     */
    protected DenseDoubleMatrix1D( int size, double[] elements, int zero, int stride ) {
        super( size, elements, zero, stride );
    }

    /**
     * This is an optimized version of getQuick. Implementation note: the superclass uses an add and a multiply. This is
     * faster, but there might be unforseen consequences...
     * 
     * @see cern.colt.matrix.DoubleMatrix1D#getQuick(int)
     */
    @Override
    public double getQuick( int i ) {
        return elements[i];
    }

    /**
     * WARNING unlike the superclass, this returns the actual underlying array, not a copy.
     * 
     * @return the elements
     * @see cern.colt.matrix.DoubleMatrix1D#toArray()
     */
    @Override
    public double[] toArray() {
        return elements;
    }

}
