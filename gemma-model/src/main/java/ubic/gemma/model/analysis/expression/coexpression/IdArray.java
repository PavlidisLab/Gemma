/*
 * The gemma-model project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.googlecode.javaewah.EWAHCompressedBitmap;

/**
 * Represents a set of IDs for entities (e.g., genes or experiments), stored in a bitset.
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class IdArray implements Serializable {

    static byte[] pack( EWAHCompressedBitmap bitmap ) {
        ByteArrayDataOutput os = ByteStreams.newDataOutput();
        try {
            bitmap.serialize( os );
            return os.toByteArray();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Internal use.
     */
    static EWAHCompressedBitmap unpack( byte[] bitmap ) {
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        try {
            b.deserialize( ByteStreams.newDataInput( bitmap ) );
            return b;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    // keep visible to subclasses.
    EWAHCompressedBitmap data = new EWAHCompressedBitmap();

    /**
     * @param ids
     */
    public synchronized void addEntities( Collection<Long> ids ) {
        List<Long> idl = new ArrayList<>( ids );
        Collections.sort( idl );

        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        for ( Long id : idl ) {
            if ( id.intValue() > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Cannot store values larger than " + Integer.MAX_VALUE );
            }
            b.set( id.intValue() );
        }
        data = data.or( b );
    }

    /**
     * Add the data set to the list of those which are in the array. If it is already included, nothing will change.
     * 
     * @param ds this is cast to an int
     * @throws IllegalArgumentException if the value is too larger to be stored as an integer.
     */
    public synchronized void addEntity( Long ds ) {
        assert ds > 0L;

        if ( ds > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Cannot store values larger than " + Integer.MAX_VALUE );
        }

        /*
         * Can't do "set" on this.data - it must be done in order, so we make a new bitmap and OR it with this.
         */

        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        b.set( ds.intValue() );
        data = data.or( b );

        assert data.get( ds.intValue() );
    }

    /**
     * @param other
     * @return datasets IDs it has in common with this.
     */
    public Collection<Long> and( IdArray other ) {
        List<Long> result = new ArrayList<>();
        EWAHCompressedBitmap aab = data.and( other.data );
        for ( int i : aab.toArray() ) {
            result.add( new Long( i ) );
        }
        return result;
    }

    /**
     * @return
     */
    public byte[] getBytes() {
        return pack( this.data );
    }

    /**
     * @return datasets IDs
     */
    public Collection<Long> getIds() {

        List<Long> result = new ArrayList<>();

        int[] array = data.toArray();
        for ( int i : array ) {
            result.add( new Long( i ) );
        }
        return result;

    }

    /**
     * @return how many datasets there are
     */
    public int getNumIds() {
        assert this.data.cardinality() == this.data.getPositions().size();
        return this.data.cardinality();
    }

    /**
     * Use 'and' instead if possible. FIXME this might be a bit slow.
     * 
     * @param g
     * @return
     */
    public boolean isIncluded( Long g ) {
        return data.get( g.intValue() );
    }

    /**
     * @param ds ID of dataset to remove. If it isn't here, has no effect.
     */
    public synchronized void removeEntity( Long ds ) {
        // don't remove if not there...
        if ( !data.get( ds.intValue() ) ) return;

        // debug code
        int old = this.data.cardinality();

        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        b.set( ds.intValue() );
        assert b.get( ds.intValue() );

        this.data = data.xor( b );

        assert old - 1 == this.data.cardinality();
        assert !this.isIncluded( ds );
    }

    /**
     * @param bytes
     */
    public void setBytes( byte[] bytes ) {
        this.data = unpack( bytes );
    }

    @Override
    public String toString() {
        return data.toDebugString();
    }
}
