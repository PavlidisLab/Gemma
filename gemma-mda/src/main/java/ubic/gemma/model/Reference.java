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

    private String type;

    /*IMPORTANT: when adding a type value, be sure to add it to isSession() or isDatabase() as appropriate*/
    public static final String DB_GENE = "databaseBackedGene";
    public static final String DB_EXPERIMENT = "databaseBackedExperiment";
    public static final String DATABASE_BACKED_GROUP = "databaseBackedGroup";
    public static final String SESSION_BOUND_GROUP = "SessionBoundGroup";
    public static final String MODIFIED_SESSION_BOUND_GROUP = "activeSessionBoundGroup";
    // session group created just to store a recent search selection like a GO group
    public static final String UNMODIFIED_SESSION_BOUND_GROUP = "recentSearchsessionBoundGroup"; 
    
    public boolean isSessionBound(){
        if(this.type.equals( SESSION_BOUND_GROUP ) || this.type.equals( MODIFIED_SESSION_BOUND_GROUP ) || this.type.equals( UNMODIFIED_SESSION_BOUND_GROUP )){
            return true;
        }
        return false;
    }
    
    public boolean isDatabaseBacked(){
        if(this.type.equals( DB_EXPERIMENT )|| this.type.equals( DB_GENE ) || this.type.equals( DATABASE_BACKED_GROUP )){
            return true;
        }
        return false;
    }
    
    public boolean isNotGroup(){
        if(this.type.equals( DB_EXPERIMENT )|| this.type.equals( DB_GENE ) ){
            return true;
        }
        return false;
    }
    /**
     * default constructor to satisfy java bean contract
     */
    public Reference() {
        super();
    }

    public Reference( Long id, String type ) {
        this.id = id;
        this.type = type;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType( String type ) {
        this.type = type;
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
        if ( (this.id.equals( other.getId())) && (this.type.equals( other.getType()) )) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{ id: " + this.id + ", type: " + this.getType() + "}";
    }

}
