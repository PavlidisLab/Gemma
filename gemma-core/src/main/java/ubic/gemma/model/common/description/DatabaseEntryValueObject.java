/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.model.common.description;

import java.io.Serializable;

/**
 * @author ??
 * @version $Id$
 */
public class DatabaseEntryValueObject implements Serializable {

    private static final long serialVersionUID = -527323410580090L;

    public static DatabaseEntryValueObject fromEntity( DatabaseEntry de ) {
        if ( de == null ) return null;
        DatabaseEntryValueObject vo = new DatabaseEntryValueObject();
        vo.setAccession( de.getAccession() );
        vo.setExternalDatabase( ExternalDatabaseValueObject.fromEntity( de.getExternalDatabase() ) );
        return vo;
    }

    private String accession;

    private ExternalDatabaseValueObject externalDatabase;

    public DatabaseEntryValueObject() {
    }

    public DatabaseEntryValueObject( DatabaseEntry de ) {
        if ( de == null ) throw new IllegalArgumentException( "Database entry is null" );
        this.accession = de.getAccession();
        this.externalDatabase = ExternalDatabaseValueObject.fromEntity( de.getExternalDatabase() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        DatabaseEntryValueObject other = ( DatabaseEntryValueObject ) obj;
        if ( this.accession == null ) {
            if ( other.accession != null ) return false;
        } else if ( !this.accession.equals( other.accession ) ) return false;
        if ( this.externalDatabase == null ) {
            if ( other.externalDatabase != null ) return false;
        } else if ( !this.externalDatabase.equals( other.externalDatabase ) ) return false;
        return true;
    }

    public String getAccession() {
        return this.accession;
    }

    public ExternalDatabaseValueObject getExternalDatabase() {
        return this.externalDatabase;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.accession == null ) ? 0 : this.accession.hashCode() );
        result = prime * result + ( ( this.externalDatabase == null ) ? 0 : this.externalDatabase.hashCode() );
        return result;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setExternalDatabase( ExternalDatabaseValueObject externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

}
