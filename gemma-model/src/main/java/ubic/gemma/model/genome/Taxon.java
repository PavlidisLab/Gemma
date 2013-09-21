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

import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * 
 */
public abstract class Taxon implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.Taxon}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.Taxon}.
         */
        public static ubic.gemma.model.genome.Taxon newInstance() {
            return new ubic.gemma.model.genome.TaxonImpl();
        }

        /**
         * (used in tests)
         */
        public static ubic.gemma.model.genome.Taxon newInstance( String scientificName, String commonName,
                String abbreviation, String unigenePrefix, String swissProtSuffix, Integer ncbiId, Boolean isSpecies,
                Boolean isGenesUsable ) {
            final ubic.gemma.model.genome.Taxon entity = new ubic.gemma.model.genome.TaxonImpl();
            entity.setScientificName( scientificName );
            entity.setCommonName( commonName );
            entity.setAbbreviation( abbreviation );
            entity.setUnigenePrefix( unigenePrefix );
            entity.setSwissProtSuffix( swissProtSuffix );
            entity.setNcbiId( ncbiId );
            entity.setIsSpecies( isSpecies );
            entity.setIsGenesUsable( isGenesUsable );

            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 9219471082900615778L;
    private String scientificName;

    private String commonName;

    private String abbreviation;

    private String unigenePrefix;

    private String swissProtSuffix;

    private Integer ncbiId;

    private Boolean isSpecies;

    private Boolean isGenesUsable;

    private Integer secondaryNcbiId;

    private Long id;

    private ExternalDatabase externalDatabase;

    private Taxon parentTaxon;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Taxon() {
    }

    /**
     * Returns <code>true</code> if the argument is an Taxon instance and all identifiers for this entity equal the
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
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * e.g. E.coli
     */
    public String getAbbreviation() {
        return this.abbreviation;
    }

    /**
     * e.g. mouse, rat, human.
     */
    public String getCommonName() {
        return this.commonName;
    }

    /**
     * 
     */
    public ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public Boolean getIsGenesUsable() {
        return this.isGenesUsable;
    }

    /**
     * 
     */
    public Boolean getIsSpecies() {
        return this.isSpecies;
    }

    /**
     * Identifier in NCBI. This is here for convenience.
     */
    public Integer getNcbiId() {
        return this.ncbiId;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getParentTaxon() {
        return this.parentTaxon;
    }

    /**
     * e.g. Homo sapiens
     */
    public String getScientificName() {
        return this.scientificName;
    }

    /**
     * Represents a "secondary" Taxon id that is used for this species. The main example where this is necessary is
     * budding yeast, which is id 4932 in GEO but genes use the (strain-specific) ID 559292.
     */
    public Integer getSecondaryNcbiId() {
        return this.secondaryNcbiId;
    }

    /**
     * e.g. "_Human"
     * 
     * @deprecated
     */
    @Deprecated
    public String getSwissProtSuffix() {
        return this.swissProtSuffix;
    }

    /**
     * e.g. Hs
     * 
     * @deprecated
     */
    @Deprecated
    public String getUnigenePrefix() {
        return this.unigenePrefix;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setAbbreviation( String abbreviation ) {
        this.abbreviation = abbreviation;
    }

    public void setCommonName( String commonName ) {
        this.commonName = commonName;
    }

    public void setExternalDatabase( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setIsGenesUsable( Boolean isGenesUsable ) {
        this.isGenesUsable = isGenesUsable;
    }

    public void setIsSpecies( Boolean isSpecies ) {
        this.isSpecies = isSpecies;
    }

    public void setNcbiId( Integer ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public void setParentTaxon( ubic.gemma.model.genome.Taxon parentTaxon ) {
        this.parentTaxon = parentTaxon;
    }

    public void setScientificName( String scientificName ) {
        this.scientificName = scientificName;
    }

    public void setSecondaryNcbiId( Integer secondaryNcbiId ) {
        this.secondaryNcbiId = secondaryNcbiId;
    }

    public void setSwissProtSuffix( String swissProtSuffix ) {
        this.swissProtSuffix = swissProtSuffix;
    }

    public void setUnigenePrefix( String unigenePrefix ) {
        this.unigenePrefix = unigenePrefix;
    }

}