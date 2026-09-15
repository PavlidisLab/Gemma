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
package ubic.gemma.core.util.matrix.datafilter;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubic.gemma.core.util.matrix.Matrix2D;

/**
 * Base implementation of the filter interface. Subclasses must implement the filter() method.
 * 
 * @author Paul Pavlidis
 * 
 */
public abstract class AbstractFilter<M extends Matrix2D<R, C, V>, R, C, V> implements Filter<M, R, C, V> {

    protected static final Logger log = LoggerFactory.getLogger( AbstractFilter.class );

    @SuppressWarnings("unchecked")
    protected M getOutputMatrix( M data, int numRows, int numCols ) {
        Matrix2D<R, C, V> returnval = null;

        try {
            Constructor<? extends Matrix2D<R, C, V>> cr = ( Constructor<? extends Matrix2D<R, C, V>> ) data.getClass()
                    .getConstructor( new Class[] { int.class, int.class } );
            returnval = cr.newInstance( new Object[] { numRows, numCols } );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return ( M ) returnval;
    }

}