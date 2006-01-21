/*
 * The Gemma project
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
package edu.columbia.gemma.loader.expression.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds information about GEO samples that "go together".
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSampleCorrespondence {

    Collection<Set<String>> sets = new HashSet<Set<String>>();

    /**
     * @param gsmNumber
     * @return Collection of sample accession values that correspond to the argument.
     */
    public Collection<String> getCorrespondingSamples( String gsmNumber ) {
        // return this.map.get( gsmNumber );
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumber ) ) {
                return set;
            }
        }
        return null; // not found!
    }

    /**
     * @param gsmNumberA
     * @param gsmNumberB
     */
    public void addCorrespondence( String gsmNumberA, String gsmNumberB ) {
        // if ( !map.containsKey( gsmNumberA ) ) map.put( gsmNumberA, new HashSet<String>() );
        // if ( !map.containsKey( gsmNumberB ) ) map.put( gsmNumberB, new HashSet<String>() );
        // map.get( gsmNumberA ).add( gsmNumberB );
        // map.get( gsmNumberB ).add( gsmNumberA );

        // the following is to make sets that each contain just the samples that group together.
        boolean found = false;
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumberA ) ) {
                set.add( gsmNumberB );
                found = true;
                break;
            } else if ( set.contains( gsmNumberB ) ) {
                set.add( gsmNumberA );
                found = true;
                break;
            }
        }

        if ( !found ) {
            Set<String> newSet = new HashSet<String>();
            newSet.add( gsmNumberA );
            newSet.add( gsmNumberB );
            sets.add( newSet );
        }

    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        List<String> groupStrings = new ArrayList<String>();
        for ( Set<String> set : sets ) {
            String group = "";
            List<String> sortedSet = new ArrayList<String>( set );
            Collections.sort( sortedSet );
            for ( String string : sortedSet ) {
                group = group + string + " <==> ";
            }
            group = group + "\n";
            groupStrings.add( group );
        }

        Collections.sort( groupStrings );
        for ( String string : groupStrings ) {
            buf.append( string );
        }

        return buf.toString().replaceAll( " <==> \\n", "\n" );
    }
}
