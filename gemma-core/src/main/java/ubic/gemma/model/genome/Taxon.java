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
    private String abbreviation;
    private Integer ncbiId;
    private Boolean isSpecies;
    private Boolean isGenesUsable;
    private Integer secondaryNcbiId;
    private Long id;
    private ExternalDatabase externalDatabase;
    private Taxon parentTaxon;

    /* ********************************
     * Constructors
     * ********************************/

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public Taxon() {
    }

    /* ********************************
     * Object override methods
     * ********************************/

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

            if ( this.getSecondaryNcbiId() != null && that.getSecondaryNcbiId() != null && !this.getSecondaryNcbiId()
                    .equals( that.getSecondaryNcbiId() ) )
                return false;

            if ( this.getScientificName() != null && that.getScientificName() != null && !this.getScientificName()
                    .equals( that.getScientificName() ) )
                return false;

        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? computeHashCode() : this.getId().hashCode() );

        return hashCode;
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
        if ( this.getAbbreviation() != null ) {
            buf.append( " Abbreviation =" ).append( this.getAbbreviation() );
        }
        return buf.toString();
    }

    /* ********************************
     * Public methods
     * ********************************/

    public String getAbbreviation() {
        return this.abbreviation;
    }

    public void setAbbreviation( String abbreviation ) {
        this.abbreviation = abbreviation;
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

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Boolean getIsGenesUsable() {
        return this.isGenesUsable;
    }

    public void setIsGenesUsable( Boolean isGenesUsable ) {
        this.isGenesUsable = isGenesUsable;
    }

    public Boolean getIsSpecies() {
        return this.isSpecies;
    }

    public void setIsSpecies( Boolean isSpecies ) {
        this.isSpecies = isSpecies;
    }

    public Integer getNcbiId() {
        return this.ncbiId;
    }

    public void setNcbiId( Integer ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public Taxon getParentTaxon() {
        return this.parentTaxon;
    }

    public void setParentTaxon( Taxon parentTaxon ) {
        this.parentTaxon = parentTaxon;
    }

    public String getScientificName() {
        return this.scientificName;
    }

    public void setScientificName( String scientificName ) {
        this.scientificName = scientificName;
    }

    /**
     * Represents a "secondary" Taxon id that is used for this species. The main example where this is necessary is
     * budding yeast, which is id 4932 in GEO but genes use the (strain-specific) ID 559292.
     */
    public Integer getSecondaryNcbiId() {
        return this.secondaryNcbiId;
    }

    public void setSecondaryNcbiId( Integer secondaryNcbiId ) {
        this.secondaryNcbiId = secondaryNcbiId;
    }

    private int computeHashCode() {
        int hashCode = 0;
        if ( this.getNcbiId() != null ) {
            hashCode += this.getNcbiId().hashCode();
        } else if ( this.getScientificName() != null ) {
            hashCode += this.getScientificName().hashCode();
        } else {
            hashCode += super.hashCode();
        }

        return hashCode;
    }

    /* ********************************
     * Public classes
     * ********************************/

    /**
     * Constructs new instances of {@link Taxon}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Taxon}.
         */
        public static Taxon newInstance() {
            return new Taxon();
        }

        /**
         * (used in tests)
         */
        public static Taxon newInstance( String scientificName, String commonName, String abbreviation, Integer ncbiId,
                Boolean isSpecies, Boolean isGenesUsable ) {
            final Taxon entity = new Taxon();
            entity.setScientificName( scientificName );
            entity.setCommonName( commonName );
            entity.setAbbreviation( abbreviation );
            entity.setNcbiId( ncbiId );
            entity.setIsSpecies( isSpecies );
            entity.setIsGenesUsable( isGenesUsable );

            return entity;
        }
    }

}