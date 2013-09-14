/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.genome.gene.ncbi.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the information from the "gene_history" file from NCBI (for one gene's history).
 * 
 * @author paul
 * @version $Id$
 */
public class NcbiGeneHistory {

    LinkedList<String> history = new LinkedList<String>();

    public NcbiGeneHistory( String startingId ) {
        history = new LinkedList<String>();
        history.add( startingId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj instanceof NcbiGeneHistory ) return false;
        return ( ( NcbiGeneHistory ) obj ).getCurrentId().equals( this.getCurrentId() );
    }

    public String getCurrentId() {
        return history.getLast();
    }

    /**
     * If the id was ever changed, give the <em>previous</em> id from the current. Otherwise return null.
     * 
     * @return
     */
    public String getPreviousId() {
        if ( history.size() == 1 ) {
            return null;
        }
        return history.get( history.size() - 2 );

    }

    /**
     * @return
     */
    public List<String> getPreviousIds() {
        return Collections.unmodifiableList( history );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getCurrentId().hashCode();
    }

    @Override
    public String toString() {
        return StringUtils.join( history, "->" );
    }

    /**
     * @param newId
     */
    public void update( String oldId, String newId ) {
        if ( history.contains( newId ) ) {
            throw new IllegalArgumentException( "History already contains " + newId );
        }
        if ( !history.contains( oldId ) ) {
            throw new IllegalArgumentException( "History doesn't contain " + oldId );
        }
        this.history.add( history.indexOf( oldId ) + 1, newId );
    }

    public boolean usedToBe( String oldId ) {
        return history.contains( oldId );
    }

}
