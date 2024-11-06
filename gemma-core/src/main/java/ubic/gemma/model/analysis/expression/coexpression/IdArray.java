/*
 * The gemma project
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

import com.googlecode.javaewah.EWAHCompressedBitmap;

import java.io.*;
import java.util.*;

/**
 * Represents a set of IDs for entities (e.g., genes or experiments), stored in a bitSet.
 *
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class IdArray {

    // keep visible to subclasses.
    protected EWAHCompressedBitmap data = new EWAHCompressedBitmap();

    private static byte[] pack( EWAHCompressedBitmap bitmap ) {
        try ( ByteArrayOutputStream bos = new ByteArrayOutputStream( bitmap.serializedSizeInBytes() ) ) {
            DataOutput os = new DataOutputStream( bos );
            bitmap.serialize( os );
            return bos.toByteArray();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private static EWAHCompressedBitmap unpack( byte[] bitmap ) {
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        try ( ByteArrayInputStream bis = new ByteArrayInputStream( bitmap ) ) {
            b.deserialize( new DataInputStream( bis ) );
            return b;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public synchronized void addEntities( Collection<Long> ids ) {
        List<Long> idl = new ArrayList<>( ids );
        Collections.sort( idl );

        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        for ( Long id : idl ) {
            if ( id > Integer.MAX_VALUE ) {
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
     * @param other the other idArray to compare with.
     * @return datasets IDs it has in common with this.
     */
    public Collection<Long> and( IdArray other ) {
        List<Long> result = new ArrayList<>();
        EWAHCompressedBitmap aab = data.and( other.data );
        for ( int i : aab.toArray() ) {
            result.add( ( long ) i );
        }
        return result;
    }

    /**
     * @param other the other idArray to compare with.
     * @return datasets IDs it has in common with this, as a set
     */
    public Set<Long> andSet( IdArray other ) {
        Set<Long> result = new HashSet<>();
        EWAHCompressedBitmap aab = data.and( other.data );
        for ( int i : aab.toArray() ) {
            result.add( ( long ) i );
        }
        return result;
    }

    public byte[] getBytes() {
        return IdArray.pack( this.data );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setBytes( byte[] bytes ) {
        this.data = IdArray.unpack( bytes );
    }

    /**
     * @return datasets IDs
     */
    public Collection<Long> getIds() {

        List<Long> result = new ArrayList<>();

        int[] array = data.toArray();
        for ( int i : array ) {
            result.add( ( long ) i );
        }
        return result;

    }

    /**
     * @return set representation
     */
    public Set<Long> getIdsSet() {

        Set<Long> result = new HashSet<>();

        int[] array = data.toArray();
        for ( int i : array ) {
            result.add( ( long ) i );
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
     * Use 'and' instead if possible.
     *
     * @param g the id to check for
     * @return true if the given id is in this array
     */
    public boolean isIncluded( Long g ) {
        return data.get( g.intValue() );
    }

    /**
     * @param ds ID of dataset to remove. If it isn't here, has no effect.
     */
    public synchronized void removeEntity( Long ds ) {
        // don't remove if not there...
        if ( !data.get( ds.intValue() ) )
            return;

        // debug code
        int old = this.data.cardinality();

        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        b.set( ds.intValue() );
        assert b.get( ds.intValue() );

        this.data = data.xor( b );

        assert old - 1 == this.data.cardinality();
        assert !this.isIncluded( ds );
    }

    @Override
    public String toString() {
        return data.toDebugString();
    }
}
