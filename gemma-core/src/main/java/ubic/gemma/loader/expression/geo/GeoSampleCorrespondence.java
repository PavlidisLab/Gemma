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
package ubic.gemma.loader.expression.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds information about GEO samples that "go together".
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSampleCorrespondence {

    Collection<Set<String>> sets = new LinkedHashSet<Set<String>>();
    private Map<String, String> accToTitle;
    private Map<String, String> accToDataset;

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
     * 
     */
    public Iterator<Set<String>> iterator() {
        return sets.iterator();
    }

    /**
     * @return the number of sets (groups of matching samples)
     */
    public int size() {
        return sets.size();
    }

    /**
     * @param gsmNumberA
     * @param gsmNumberB If null, interpreted as meaning there is no correspondence to worry about
     */
    public void addCorrespondence( String gsmNumberA, String gsmNumberB ) {

        assert gsmNumberA != null : "Must pass at least one GSM accession";

        // the following is to make sets that each contain just the samples that group together.
        boolean found = false;
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumberA ) ) {
                set.add( gsmNumberB );
                found = true;
                break;
                // gsmNumberB will be null if there is just one data set - that is, no correspondence.
            } else if ( gsmNumberB != null && set.contains( gsmNumberB ) ) {
                set.add( gsmNumberA );
                found = true;
                break;
            }
        }

        if ( !found ) {
            Set<String> newSet = new LinkedHashSet<String>();
            newSet.add( gsmNumberA );
            if ( gsmNumberB != null ) newSet.add( gsmNumberB );
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
            for ( String accession : sortedSet ) {
                group = group + accession + " ('" + accToTitle.get( accession ) + "' in "
                        + accToDataset.get( accession ) + ")";

                if ( sortedSet.size() == 1 ) {
                    group = group + ( " - singleton" );
                } else {
                    group = group + ( " <==> " );
                }
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

    public void setAccToTitleMap( Map<String, String> accToTitle ) {
        this.accToTitle = accToTitle;
    }

    public void setAccToDatasetMap( Map<String, String> accToDataset ) {
        this.accToDataset = accToDataset;
    }
}
