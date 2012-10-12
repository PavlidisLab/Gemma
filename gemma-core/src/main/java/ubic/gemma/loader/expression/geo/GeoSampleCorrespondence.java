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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

/**
 * Holds information about GEO samples that "go together" across datasets (GDS), because they came from the same sample
 * (or so we infer)
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSampleCorrespondence implements Serializable {

    private static final long serialVersionUID = -5285504953530483114L;

    private Collection<Set<String>> sets = new LinkedHashSet<Set<String>>();

    private Map<String, String> accToTitle = new HashMap<String, String>();
    private Map<String, String> accToDataset = new HashMap<String, String>();

    /**
     * @param gsmNumberA
     * @param gsmNumberB If null, interpreted as meaning there is no correspondence to worry about, though they can be
     *        added later.
     */
    public void addCorrespondence( String gsmNumberA, String gsmNumberB ) {

        if ( StringUtils.isBlank( gsmNumberA ) )
            throw new IllegalArgumentException( "Must pass at least one GSM accession" );

        // the following is to make sets that each contain just the samples that group together.
        boolean found = false;
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumberA ) && gsmNumberB != null ) {
                if ( !sanity( gsmNumberB ) ) return;
                set.add( gsmNumberB );
                found = true;
                break;
                // gsmNumberB will be null if there is just one data set - that is, no correspondence.
            } else if ( gsmNumberB != null && set.contains( gsmNumberB ) ) {
                if ( !sanity( gsmNumberA ) ) return;
                set.add( gsmNumberA );

                found = true;
                break;
            }
        }

        if ( !found ) {
            if ( !sanity( gsmNumberA ) || !sanity( gsmNumberB ) ) {
                return;
            }
            Set<String> newSet = new LinkedHashSet<String>();
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
            Set<String> sc = new HashSet<String>();
            sc.addAll( s );
            r.sets.add( sc );
        }

        r.accToDataset.putAll( this.accToDataset );
        r.accToTitle.putAll( this.accToTitle );

        return r;
    }

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
     * Remove a sample from the maps
     * 
     * @param gsmNumber
     */
    public void removeSample( String gsmNumber ) {
        Collection<Set<String>> toRemove = new ArrayList<Set<String>>();
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
        StringBuffer buf = new StringBuffer();

        StringBuffer singletons = new StringBuffer();
        SortedSet<String> groupStrings = new TreeSet<String>();
        for ( Set<String> set : sets ) {
            String group = "";
            SortedSet<String> sortedSet = new TreeSet<String>( set );
            for ( String accession : sortedSet ) {
                group = group
                        + accession
                        + " ('"
                        + ( accToTitle != null && accToTitle.containsKey( accession ) ? accToTitle.get( accession )
                                : "[no title]" ) + "'"
                        + ( accToDataset != null ? ( " in " + accToDataset.get( accession ) ) : "" ) + ")";

                if ( sortedSet.size() == 1 ) {
                    singletons.append( group + "\n" );
                    group = group + ( " - singleton" );
                } else {
                    group = group + ( "\t<==>\t" );
                }
            }
            group = group + "\n";
            groupStrings.add( group );
        }

        for ( String string : groupStrings ) {
            buf.append( string );
        }

        return buf.toString().replaceAll( "\\t<==>\\t\\n", "\n" )
                + ( singletons.length() > 0 ? "\nSingletons:\n" + singletons.toString() : "" );
    }

    /**
     * Make sure only one set contains gsmNumberB
     * 
     * @param gsmNumberB
     */
    private boolean sanity( String gsmNumber ) {
        int count = 0;
        for ( Set<String> set : sets ) {
            if ( set.contains( gsmNumber ) ) {
                ++count;
            }
        }
        if ( count != 0 ) {
            return false;
        }
        return true;
    }

}
