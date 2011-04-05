/*

 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Encapsulates all the information required to identify an object Used currently for GeneSetValueObjects and
 * ExperimentSetValueObjects TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class Reference implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 426665981262835056L;

    private Long id;

    private Integer type;

    public static final int DB_GENE = 1;
    public static final int DB_EXPERIMENT = 2;
    public static final int DATABASE_BACKED_GROUP = 3;
    public static final int SESSION_BOUND_GROUP = 4;
    public static final int SESSION_UNBOUND_GROUP = 5; // session group manager doesn't know about it

    /**
     * default constructor to satisfy java bean contract
     */
    public Reference() {
        super();
    }

    public Reference( Long id, int type ) {
        this.id = id;
        this.type = type;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setType( int type ) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public String getTypeString() {
        switch ( this.type ) {
            case 1:
                return "database_backed_gene";
            case 2:
                return "database_backed_experiment";
            case 3:
                return "database_backed_group";
            case 4:
                return "session_bound_group";
        }
        return Integer.toString( this.type );
    }

    /**
     * Checks to see if both id and type are the same in both objects,
     * if so returns true
     * (non-Javadoc)
     */ 
    /* 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Reference other = ( Reference ) obj;
        if ( this.id == other.getId() && this.type == other.getType() ) return true;
        return false;
    }

    @Override
    public String toString() {
        return "{ id: " + this.id + ", type: " + this.getTypeString() + "}";
    }

    /**
     * transform the reference object into a string for easy passing to and from EXT
     * 
     * @return the code for this reference object
     */
    public String encode() {
        return this.id + "_" + this.type;
    }

    /**
     * transform the reference object into a string for easy passing to and from EXT
     * 
     * @return the code for this reference object
     */
    public static String encode( Reference ref ) {
        return ref.id + "_" + ref.type;
    }

    /**
     * After having encoded a reference object as a string for passing to and from EXT using the encode() method, decode
     * the string to create a new reference object as specified
     * 
     * @param code a specification for the fields of the reference object
     * @return a new reference object with values as specified by the code
     */
    public static Reference decode( String code ) {
        if ( code == null ) return null;
        String[] arr = code.split( "_" );
        Long id = null;
        Integer type = null;
        try {
            id = new Long( arr[0] );
            type = new Integer( arr[1] );
        } catch ( NumberFormatException e ) {
            return null;
        }
        if ( id == null || type == null ) return null;
        return new Reference( id, type );
    }

    /**
     * After having encoded a reference object as a string for passing to and from EXT using the encode() method, this
     * method decodes the string to create a new reference object as specified
     * 
     * @param a collection of codes specifying the field values for a collection of references
     * @return a collection of new reference object with values as specified by the code if no codes were of the right
     *         format, an empty collection is returned
     */
    public static Collection<Reference> decode( Collection<String> codes ) {
        Collection<Reference> refs = new ArrayList<Reference>();
        Reference newRef = null;
        for ( String code : codes ) {
            newRef = decode( code );
            if ( newRef != null ) {
                refs.add( newRef );
            }
        }
        return refs;
    }

}
