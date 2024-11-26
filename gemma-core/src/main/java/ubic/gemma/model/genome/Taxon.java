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
package ubic.gemma.model.genome;

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.description.ExternalDatabase;

import javax.annotation.Nullable;
import java.util.Objects;

public class Taxon extends AbstractIdentifiable {

    @Nullable
    private String scientificName;
    @Nullable
    private String commonName;
    @Nullable
    private Integer ncbiId;
    private boolean isGenesUsable;
    @Nullable
    private Integer secondaryNcbiId;
    @Nullable
    private ExternalDatabase externalDatabase;

    @Override
    public int hashCode() {
        return Objects.hash( scientificName, commonName, ncbiId, isGenesUsable, secondaryNcbiId, externalDatabase );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Taxon ) ) {
            return false;
        }
        final Taxon that = ( Taxon ) object;

        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }

        return Objects.equals( this.getCommonName(), that.getCommonName() )
                && Objects.equals( this.getIsGenesUsable(), that.getIsGenesUsable() )
                && Objects.equals( this.getExternalDatabase(), that.getExternalDatabase() )
                && Objects.equals( this.getSecondaryNcbiId(), that.getSecondaryNcbiId() )
                && Objects.equals( this.getNcbiId(), that.getNcbiId() )
                && Objects.equals( this.getScientificName(), that.getScientificName() );
    }

    /**
     * @see Taxon#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( "Taxon:" );
        if ( this.getId() != null ) {
            buf.append( " Id = " ).append( this.getId() );
        }
        if ( this.getScientificName() != null ) {
            buf.append( " " ).append( this.getScientificName() );
        }
        if ( this.getCommonName() != null ) {
            buf.append( " (" ).append( this.getCommonName() ).append( ")" );
        }
        if ( this.getNcbiId() != null ) {
            buf.append( " NCBI id=" ).append( this.getNcbiId() );
        }
        return buf.toString();
    }

    @Nullable
    public String getCommonName() {
        return this.commonName;
    }

    public void setCommonName( @Nullable String commonName ) {
        this.commonName = commonName;
    }

    @Nullable
    public ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( @Nullable ExternalDatabase externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public boolean getIsGenesUsable() {
        return this.isGenesUsable;
    }

    public void setIsGenesUsable( boolean isGenesUsable ) {
        this.isGenesUsable = isGenesUsable;
    }

    @Nullable
    public Integer getNcbiId() {
        return this.ncbiId;
    }

    public void setNcbiId( @Nullable Integer ncbiId ) {
        this.ncbiId = ncbiId;
    }

    @Nullable
    public String getScientificName() {
        return this.scientificName;
    }

    public void setScientificName( @Nullable String scientificName ) {
        this.scientificName = scientificName;
    }

    /**
     * @return Represents a "secondary" Taxon id that is used for this species. The main example where this is necessary
     *         is
     *         budding yeast, which is id 4932 in GEO but genes use the (strain-specific) ID 559292.
     */
    @Nullable
    public Integer getSecondaryNcbiId() {
        return this.secondaryNcbiId;
    }

    @SuppressWarnings("unused")
    public void setSecondaryNcbiId( @Nullable Integer secondaryNcbiId ) {
        this.secondaryNcbiId = secondaryNcbiId;
    }

    public static final class Factory {

        public static Taxon newInstance() {
            return new Taxon();
        }

        public static Taxon newInstance( String scientificName, String commonName, Integer ncbiId, boolean isGenesUsable ) {
            final Taxon entity = new Taxon();
            entity.setScientificName( scientificName );
            entity.setCommonName( commonName );
            entity.setNcbiId( ncbiId );
            entity.setIsGenesUsable( isGenesUsable );
            return entity;
        }

        public static Taxon newInstance( String commonName ) {
            Taxon entity = new Taxon();
            entity.setCommonName( commonName );
            return entity;
        }
    }
}