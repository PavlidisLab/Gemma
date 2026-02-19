/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.description;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import ubic.gemma.model.common.AbstractIdentifiable;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;

/**
 * <p>
 * A reference to a record in a database.
 * </p>
 */
@Indexed
public class DatabaseEntry extends AbstractIdentifiable {

    /**
     * Compares {@link DatabaseEntry} by version.
     */
    public static Comparator<DatabaseEntry> getComparator() {
        return Comparator
                // we always prefer integer versions, so we put them last
                .comparing( ( DatabaseEntry databaseEntry ) -> {
                    try {
                        return databaseEntry.accessionVersion != null ? Integer.valueOf( databaseEntry.accessionVersion ) : null;
                    } catch ( NumberFormatException e ) {
                        return null;
                    }
                }, Comparator.nullsFirst( Comparator.naturalOrder() ) )
                // sort the remaining accessions lexicographically
                .thenComparing( DatabaseEntry::getAccessionVersion )
                // for ties, simply use the latest ID
                .thenComparing( DatabaseEntry::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );
    }

    private String accession;
    private String accessionVersion;
    /**
     * If present, overrides the default URI construction for this database entry which is usually based on
     * {@link ExternalDatabase#getWebUri()}.
     */
    @Nullable
    private String uri;
    private ExternalDatabase externalDatabase;

    @Field(analyze = Analyze.NO)
    public String getAccession() {
        return this.accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public String getAccessionVersion() {
        return this.accessionVersion;
    }

    public void setAccessionVersion( String accessionVersion ) {
        this.accessionVersion = accessionVersion;
    }

    public ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ExternalDatabase externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Nullable
    public String getUri() {
        return this.uri;
    }

    public void setUri( @Nullable String uri ) {
        this.uri = uri;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getAccession(), getAccessionVersion(), getExternalDatabase() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof DatabaseEntry ) ) {
            return false;
        }
        final DatabaseEntry that = ( DatabaseEntry ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( getAccession(), that.getAccession() )
                    && Objects.equals( getAccessionVersion(), that.getAccessionVersion() )
                    && Objects.equals( getExternalDatabase(), that.getExternalDatabase() );
        }
    }

    @Override
    public String toString() {
        return super.toString()
                + ( this.getAccession() + " " )
                + ( this.getExternalDatabase() == null ? "[no external database]" : this.getExternalDatabase().getName() );
    }

    public static final class Factory {

        public static DatabaseEntry newInstance() {
            return new DatabaseEntry();
        }

        public static DatabaseEntry newInstance( ExternalDatabase externalDatabase ) {
            final DatabaseEntry entity = newInstance();
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }

        public static DatabaseEntry newInstance( String accession, ExternalDatabase externalDatabase ) {
            final DatabaseEntry entity = newInstance( externalDatabase );
            entity.setAccession( accession );
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }
    }
}