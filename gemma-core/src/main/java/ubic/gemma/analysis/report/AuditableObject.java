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
package ubic.gemma.analysis.report;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jsantos
 * @version $Id$
 */
public class AuditableObject implements Serializable {
    // small holder struct to ease serialization of the list

    private static final long serialVersionUID = -7862129089784691035L;
    public String type = null;
    public Long id = null;
    public Date date = null;

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final AuditableObject other = ( AuditableObject ) obj;
        if ( date == null ) {
            if ( other.date != null ) return false;
        } else if ( !date.equals( other.date ) ) return false;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        if ( type == null ) {
            if ( other.type != null ) return false;
        } else if ( !type.equals( other.type ) ) return false;
        return true;
    }

    public Date getDate() {
        return date;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( date == null ) ? 0 : date.hashCode() );
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setType( String type ) {
        this.type = type;
    }

}
