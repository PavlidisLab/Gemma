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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.common.IdentifiableValueObject;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Data
@EqualsAndHashCode(of = { "name" }, callSuper = false)
public class ExternalDatabaseValueObject extends IdentifiableValueObject<ExternalDatabase> implements Comparable<ExternalDatabaseValueObject>, Versioned {

    private static final long serialVersionUID = -1714429166594162374L;
    private String name;
    private String description;
    private String uri;
    private String releaseVersion;
    private URL releaseUrl;
    private Date lastUpdated;
    private Set<ExternalDatabaseValueObject> externalDatabases;
    @JsonIgnore
    private boolean checked = false;

    public ExternalDatabaseValueObject() {
        super();
    }

    public ExternalDatabaseValueObject( Long id, String name, boolean checked ) {
        super( id );
        this.name = name;
        this.checked = checked;
    }

    public ExternalDatabaseValueObject( ExternalDatabase ed ) {
        super( ed );
        this.name = ed.getName();
        this.description = ed.getDescription();
        this.uri = ed.getWebUri();
        this.releaseUrl = ed.getReleaseUrl();
        this.releaseVersion = ed.getReleaseVersion();
        this.lastUpdated = ed.getLastUpdated();
        this.externalDatabases = ed.getExternalDatabases()
                .stream()
                .map( ced -> new ExternalDatabaseValueObject( ced, ed ) )
                .collect( Collectors.toSet() );
    }

    private ExternalDatabaseValueObject( ExternalDatabase ed, ExternalDatabase parentDatabase ) {
        this( ed );
        if ( ed.getReleaseVersion() == null ) {
            this.releaseVersion = parentDatabase.getReleaseVersion();
            this.releaseUrl = parentDatabase.getReleaseUrl();
        }
        if ( ed.getLastUpdated() == null ) {
            this.lastUpdated = parentDatabase.getLastUpdated();
        }
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

    @Override
    public int compareTo( ExternalDatabaseValueObject externalDatabaseValueObject ) {
        return this.getName().toLowerCase().compareTo( externalDatabaseValueObject.getName().toLowerCase() );
    }
}
