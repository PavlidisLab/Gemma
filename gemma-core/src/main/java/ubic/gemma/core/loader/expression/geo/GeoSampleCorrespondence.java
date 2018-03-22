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
package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Holds information about GEO samples that "go together" across datasets (GDS), because they came from the same sample
 * (or so we infer)
 *
 * @author pavlidis
 */
public class GeoSampleCorrespondence implements Serializable {

    private static final long serialVersionUID = -5285504953530483114L;

    private final Collection<Set<String>> sets = new LinkedHashSet<>();

    private Map<String, String> accToTitle = new HashMap<>();
    private Map<String, String> accToDataset = new HashMap<>();

    /**
     * @param gsmNumberA number A
     * @param gsmNumberB If null, interpreted as meaning there is no correspondence to worry about, though they can be
     *                   added later.
     */
    public void addCorrespondence( String gsmNumberA, String gsmNumberB ) {

        if ( StringUtils.isBlank( gsmNumberA ) )
            throw new IllegalArgumentException( "Must pass at least one GSM accession" );

        // the following is to make sets that each contain just the samples that group together.
        boolean found = false;
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumberA ) && gsmNumberB != null ) {
                if ( this.insanity( gsmNumberB ) )
                    return;
                set.add( gsmNumberB );
                found = true;
                break;
                // gsmNumberB will be null if there is just one data set - that is, no correspondence.
            } else if ( gsmNumberB != null && set.contains( gsmNumberB ) ) {
                if ( this.insanity( gsmNumberA ) )
                    return;
                set.add( gsmNumberA );

                found = true;
                break;
            }
        }

        if ( !found ) {
            if ( this.insanity( gsmNumberA ) || this.insanity( gsmNumberB ) ) {
                return;
            }
            Set<String> newSet = new LinkedHashSet<>();
            newSet.add( gsmNumberA );
            if ( gsmNumberB != null ) {
                newSet.add( gsmNumberB );
            }
            sets.add( newSet );

        }

    }

    public GeoSampleCorrespondence copy() {
        GeoSampleCorrespondence r = new GeoSampleCorrespondence();

        for ( Set<String> s : sets ) {
            Set<String> sc = new HashSet<>( s );
            r.sets.add( sc );
        }

        r.accToDataset.putAll( this.accToDataset );
        r.accToTitle.putAll( this.accToTitle );

        return r;
    }

    /**
     * @param gsmNumber gsm number
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

    public Iterator<Set<String>> iterator() {
        return sets.iterator();
    }

    /**
     * Remove a sample from the maps
     *
     * @param gsmNumber gsm number
     */
    public void removeSample( String gsmNumber ) {
        Collection<Set<String>> toRemove = new ArrayList<>();
        for ( Set<String> set : sets ) {
            set.remove( gsmNumber );
            if ( set.isEmpty() ) {
                toRemove.add( set );
            }
        }
        sets.removeAll( toRemove );
    }

    public void setAccToDatasetOrPlatformMap( Map<String, String> accToDataset ) {
        this.accToDataset = accToDataset;
    }

    public void setAccToTitleMap( Map<String, String> accToTitle ) {
        this.accToTitle = accToTitle;
    }

    /**
     * @return the number of sets (groups of matching samples)
     */
    public int size() {
        return sets.size();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        StringBuilder singletons = new StringBuilder();
        SortedSet<String> groupStrings = new TreeSet<>();
        for ( Set<String> set : sets ) {
            StringBuilder group = new StringBuilder();
            SortedSet<String> sortedSet = new TreeSet<>( set );
            for ( String accession : sortedSet ) {
                group.append( accession ).append( " ('" ).append(
                        accToTitle != null && accToTitle.containsKey( accession ) ?
                                accToTitle.get( accession ) :
                                "[no title]" ).append( "'" )
                        .append( accToDataset != null ? ( " in " + accToDataset.get( accession ) ) : "" ).append( ")" );

                if ( sortedSet.size() == 1 ) {
                    singletons.append( group ).append( "\n" );
                    group.append( " - singleton" );
                } else {
                    group.append( "\t<==>\t" );
                }
            }
            group.append( "\n" );
            groupStrings.add( group.toString() );
        }

        for ( String string : groupStrings ) {
            buf.append( string );
        }

        return buf.toString().replaceAll( "\\t<==>\\t\\n", "\n" ) + ( singletons.length() > 0 ?
                "\nSingletons:\n" + singletons.toString() :
                "" );
    }

    /**
     * Make sure only one set contains gsmNumberB
     *
     * @param gsmNumber gsm number
     */
    private boolean insanity( String gsmNumber ) {
        int count = 0;
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumber ) ) {
                ++count;
            }
        }
        return count != 0;
    }

}
