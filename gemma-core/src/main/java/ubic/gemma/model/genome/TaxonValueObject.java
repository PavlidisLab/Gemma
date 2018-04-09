/*
 * The gemma project
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
package ubic.gemma.model.genome;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;

/**
 * @author Paul
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Used in frontend
public class TaxonValueObject extends IdentifiableValueObject<Taxon> {

    private String scientificName;
    private String commonName;
    private String abbreviation;
    private Integer ncbiId;
    private Boolean isSpecies;
    private Boolean isGenesUsable;
    private ExternalDatabaseValueObject externalDatabase;
    private TaxonValueObject parentTaxon;

    public TaxonValueObject( Taxon taxon ) {
        super( taxon.getId() );
        this.setScientificName( taxon.getScientificName() );
        this.setCommonName( taxon.getCommonName() );
        this.setAbbreviation( taxon.getAbbreviation() );

        this.setNcbiId( taxon.getNcbiId() );
        this.setIsGenesUsable( taxon.getIsGenesUsable() );
        this.setIsSpecies( taxon.getIsSpecies() );
        this.setParentTaxon(
                taxon.getParentTaxon() != null ? TaxonValueObject.fromEntity( taxon.getParentTaxon() ) : null );

        if ( taxon.getExternalDatabase() != null ) {
            this.setExternalDatabase( new ExternalDatabaseValueObject( taxon.getExternalDatabase() ) );
        }
    }

    public TaxonValueObject( Long id ) {
        super( id );
    }

    public static TaxonValueObject fromEntity( Taxon taxon ) {
        return new TaxonValueObject( taxon );
    }

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

    public ExternalDatabaseValueObject getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ExternalDatabaseValueObject externalDatabase ) {
        this.externalDatabase = externalDatabase;
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

    public TaxonValueObject getParentTaxon() {
        return this.parentTaxon;
    }

    public void setParentTaxon( TaxonValueObject parentTaxon ) {
        this.parentTaxon = parentTaxon;
    }

    public String getScientificName() {
        return this.scientificName;
    }

    public void setScientificName( String scientificName ) {
        this.scientificName = scientificName;
    }

}
