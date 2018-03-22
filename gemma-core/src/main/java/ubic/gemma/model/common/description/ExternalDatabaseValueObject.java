/*
 * The Gemma project
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
package ubic.gemma.model.common.description;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ExternalDatabaseValueObject implements Serializable, Comparable<ExternalDatabaseValueObject> {

    private static final long serialVersionUID = -1714429166594162374L;
    private String name;
    private Long id;
    private boolean checked = false;

    public ExternalDatabaseValueObject() {
        super();
    }

    public ExternalDatabaseValueObject( Long id, String name, boolean checked ) {
        super();
        this.name = name;
        this.id = id;
        this.checked = checked;
    }

    public ExternalDatabaseValueObject( ExternalDatabase ed ) {
        super();
        this.id = ed.getId();
        this.name = ed.getName();
    }

    public static Collection<ExternalDatabaseValueObject> fromEntity( Collection<ExternalDatabase> eds ) {
        if ( eds == null )
            return null;

        Collection<ExternalDatabaseValueObject> vos = new TreeSet<>();
        for ( ExternalDatabase ed : eds ) {
            if ( ed != null )
                vos.add( new ExternalDatabaseValueObject( ed ) );
        }

        return vos;
    }

    @Deprecated // use constructor instead
    public static ExternalDatabaseValueObject fromEntity( ExternalDatabase ed ) {
        if ( ed == null )
            return null;
        ExternalDatabaseValueObject vo = new ExternalDatabaseValueObject();
        vo.setName( ed.getName() );
        vo.setId( ed.getId() );
        return vo;
    }

    @Override
    public int compareTo( ExternalDatabaseValueObject externalDatabaseValueObject ) {
        return this.getName().toLowerCase().compareTo( externalDatabaseValueObject.getName().toLowerCase() );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        ExternalDatabaseValueObject other = ( ExternalDatabaseValueObject ) obj;
        if ( name == null ) {
            return other.name == null;
        } else
            return name.equals( other.name );
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked( boolean checked ) {
        this.checked = checked;
    }
}
