/*
 * The baseCode project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.javaewah.EWAHCompressedBitmap;

/**
 * Encapsulates an array of Longs (which are actually stored as Integers...thanks to limitation of compressedbitmap I am
 * using).
 * 
 * @author paul
 * @version $Id$
 */
public class CompressedLongSet {

    private EWAHCompressedBitmap data;

    /**
     * @param longs
     */
    public CompressedLongSet( Set<Long> longs ) {
        List<Long> v = new ArrayList<>( longs );
        Collections.sort( v );
        data = new EWAHCompressedBitmap( longs.size() );

        for ( Long l : v ) {
            if ( l > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Cannot store values larger than Integer.MAX_VALUE" );
            }

            data.set( l.intValue() );
        }
    }

    /**
     * Will be in order.
     * 
     * @return
     */
    public Long[] getValues() {
        List<Integer> positions = data.getPositions();
        Long[] result = new Long[positions.size()];
        int k = 0;
        for ( Integer i : positions ) {
            result[k] = i.longValue();
            k++;
        }
        return result;
    }

    /**
     * @return set representation
     */
    public Collection<Long> toSet() {
        List<Integer> positions = data.getPositions();
        Set<Long> result = new HashSet<Long>();
        for ( Integer i : positions ) {
            result.add( i.longValue() );
        }
        return result;
    }

}
