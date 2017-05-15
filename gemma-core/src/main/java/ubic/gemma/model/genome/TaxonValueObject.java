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

import ubic.gemma.model.common.description.ExternalDatabaseValueObject;

/**
 * @author Paul
 */
public class TaxonValueObject {

    private String scientificName;
    private String commonName;
    private String abbreviation;
    private String unigenePrefix;
    private String swissProtSuffix;
    private Integer ncbiId;
    private Boolean isSpecies;
    private Boolean isGenesUsable;
    private Long id;
    private ExternalDatabaseValueObject externalDatabase;
    private TaxonValueObject parentTaxon;

    public static TaxonValueObject fromEntity( Taxon taxon ) {
        TaxonValueObject vo = new TaxonValueObject();
        vo.setScientificName( taxon.getScientificName() );
        vo.setId( taxon.getId() );
        vo.setCommonName( taxon.getCommonName() );

        if ( taxon.getExternalDatabase() != null ) {
            vo.setExternalDatabase( ExternalDatabaseValueObject.fromEntity( taxon.getExternalDatabase() ) );
        }

        return vo;
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

    public String getSwissProtSuffix() {
        return this.swissProtSuffix;
    }

    public void setSwissProtSuffix( String swissProtSuffix ) {
        this.swissProtSuffix = swissProtSuffix;
    }

    public String getUnigenePrefix() {
        return this.unigenePrefix;
    }

    public void setUnigenePrefix( String unigenePrefix ) {
        this.unigenePrefix = unigenePrefix;
    }

}
