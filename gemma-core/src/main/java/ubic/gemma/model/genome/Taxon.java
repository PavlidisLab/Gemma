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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.ExternalDatabase;

public class Taxon implements Identifiable, java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 9219471082900615778L;
    private String scientificName;
    private String commonName;
    private Integer ncbiId;
    private boolean isGenesUsable;
    private Integer secondaryNcbiId;
    private Long id;
    private ExternalDatabase externalDatabase;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public Taxon() {
    }

    @Override
    public int hashCode() {
        if ( this.getNcbiId() != null ) {
            return this.getNcbiId().hashCode();
        } else if ( this.getScientificName() != null ) {
            return this.getScientificName().hashCode();
        } else {
            return super.hashCode();
        }
    }

    /**
     * Returns <code>true</code> if the argument is a Taxon instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Taxon ) ) {
            return false;
        }
        final Taxon that = ( Taxon ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            // use ncbi id OR scientific name.

            if ( this.getNcbiId() != null && that.getNcbiId() != null && !this.getNcbiId().equals( that.getNcbiId() ) )
                return false;

            //noinspection SimplifiableIfStatement // Better readability
            if ( this.getSecondaryNcbiId() != null && that.getSecondaryNcbiId() != null && !this.getSecondaryNcbiId()
                    .equals( that.getSecondaryNcbiId() ) )
                return false;

            return this.getScientificName() == null || that.getScientificName() == null || this.getScientificName()
                    .equals( that.getScientificName() );

        }
        return true;
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

    public String getCommonName() {
        return this.commonName;
    }

    public void setCommonName( String commonName ) {
        this.commonName = commonName;
    }

    public ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public boolean getIsGenesUsable() {
        return this.isGenesUsable;
    }

    public void setIsGenesUsable( boolean isGenesUsable ) {
        this.isGenesUsable = isGenesUsable;
    }

    public Integer getNcbiId() {
        return this.ncbiId;
    }

    public void setNcbiId( Integer ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public String getScientificName() {
        return this.scientificName;
    }

    public void setScientificName( String scientificName ) {
        this.scientificName = scientificName;
    }

    /**
     * @return Represents a "secondary" Taxon id that is used for this species. The main example where this is necessary
     *         is
     *         budding yeast, which is id 4932 in GEO but genes use the (strain-specific) ID 559292.
     */
    public Integer getSecondaryNcbiId() {
        return this.secondaryNcbiId;
    }

    public void setSecondaryNcbiId( Integer secondaryNcbiId ) {
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