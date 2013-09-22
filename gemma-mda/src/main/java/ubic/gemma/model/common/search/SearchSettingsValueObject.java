/*
 * The gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.model.common.search;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

import java.io.Serializable;

/**
 * author: anton date: 18/03/13
 */
public class SearchSettingsValueObject implements Serializable {

    private static final long serialVersionUID = -934534534L;

    public static SearchSettings toEntity( SearchSettingsValueObject valueObject ) {
        final SearchSettings entity = new SearchSettingsImpl();
        entity.setQuery( valueObject.getQuery() );
        entity.setPlatformConstraint( valueObject.getPlatformConstraint() );
        entity.setTermUri( valueObject.getTermUri() );
        entity.setTaxon( valueObject.getTaxon() );
        entity.setMaxResults( valueObject.getMaxResults() );
        entity.setSearchExperiments( valueObject.getSearchExperiments() );
        entity.setSearchGenes( valueObject.getSearchGenes() );
        entity.setSearchPlatforms( valueObject.getSearchPlatforms() );
        entity.setSearchExperimentSets( valueObject.getSearchExperimentSets() );
        entity.setSearchPhenotypes( valueObject.getSearchPhenotypes() );
        entity.setSearchProbes( valueObject.getSearchProbes() );
        entity.setSearchGeneSets( valueObject.getSearchGeneSets() );
        entity.setSearchBioSequences( valueObject.getSearchBioSequences() );
        entity.setSearchBibrefs( valueObject.getSearchBibrefs() );
        entity.setUseIndices( valueObject.getUseIndices() );
        entity.setUseDatabase( valueObject.getUseDatabase() );
        entity.setUseCharacteristics( valueObject.getUseCharacteristics() );
        entity.setUseGo( valueObject.getUseGo() );
        return entity;
    }

    private Integer maxResults = Integer.valueOf( 500 );
    private ArrayDesign platformConstraint;
    private String query;
    private Boolean searchBibrefs = Boolean.valueOf( true );
    private Boolean searchBioSequences = Boolean.valueOf( true );
    private Boolean searchExperiments = Boolean.valueOf( true );
    private Boolean searchExperimentSets = Boolean.valueOf( true );
    private Boolean searchGenes = Boolean.valueOf( true );
    private Boolean searchGeneSets = java.lang.Boolean.valueOf( true );
    private Boolean searchPhenotypes = Boolean.valueOf( true );
    private Boolean searchPlatforms = Boolean.valueOf( true );
    private Boolean searchProbes = Boolean.valueOf( true );
    private Taxon taxon;
    private String termUri;
    private Boolean useCharacteristics = Boolean.valueOf( true );
    private Boolean useDatabase = Boolean.valueOf( true );

    private Boolean useGo = Boolean.valueOf( true );

    private Boolean useIndices = Boolean.valueOf( true );

    public SearchSettingsValueObject() {
    }

    public Integer getMaxResults() {
        return this.maxResults;
    }

    public ArrayDesign getPlatformConstraint() {
        return this.platformConstraint;
    }

    public String getQuery() {
        return this.query;
    }

    public Boolean getSearchBibrefs() {
        return this.searchBibrefs;
    }

    public Boolean getSearchBioSequences() {
        return this.searchBioSequences;
    }

    public Boolean getSearchExperiments() {
        return this.searchExperiments;
    }

    public Boolean getSearchExperimentSets() {
        return this.searchExperimentSets;
    }

    public Boolean getSearchGenes() {
        return this.searchGenes;
    }

    public Boolean getSearchGeneSets() {
        return this.searchGeneSets;
    }

    public Boolean getSearchPhenotypes() {
        return this.searchPhenotypes;
    }

    public Boolean getSearchPlatforms() {
        return this.searchPlatforms;
    }

    public Boolean getSearchProbes() {
        return this.searchProbes;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public String getTermUri() {
        return this.termUri;
    }

    public Boolean getUseCharacteristics() {
        return this.useCharacteristics;
    }

    public Boolean getUseDatabase() {
        return this.useDatabase;
    }

    public Boolean getUseGo() {
        return this.useGo;
    }

    public Boolean getUseIndices() {
        return this.useIndices;
    }

    public void setMaxResults( Integer maxResults ) {
        this.maxResults = maxResults;
    }

    public void setPlatformConstraint( ArrayDesign platformConstraint ) {
        this.platformConstraint = platformConstraint;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public void setSearchBibrefs( Boolean searchBibrefs ) {
        this.searchBibrefs = searchBibrefs;
    }

    public void setSearchBioSequences( Boolean searchBioSequences ) {
        this.searchBioSequences = searchBioSequences;
    }

    public void setSearchExperiments( Boolean searchExperiments ) {
        this.searchExperiments = searchExperiments;
    }

    public void setSearchExperimentSets( Boolean searchExperimentSets ) {
        this.searchExperimentSets = searchExperimentSets;
    }

    public void setSearchGenes( Boolean searchGenes ) {
        this.searchGenes = searchGenes;
    }

    public void setSearchGeneSets( Boolean searchGeneSets ) {
        this.searchGeneSets = searchGeneSets;
    }

    public void setSearchPhenotypes( Boolean searchPhenotypes ) {
        this.searchPhenotypes = searchPhenotypes;
    }

    public void setSearchPlatforms( Boolean searchPlatforms ) {
        this.searchPlatforms = searchPlatforms;
    }

    public void setSearchProbes( Boolean searchProbes ) {
        this.searchProbes = searchProbes;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }

    public void setUseCharacteristics( Boolean useCharacteristics ) {
        this.useCharacteristics = useCharacteristics;
    }

    public void setUseDatabase( Boolean useDatabase ) {
        this.useDatabase = useDatabase;
    }

    public void setUseGo( Boolean useGo ) {
        this.useGo = useGo;
    }

    public void setUseIndices( Boolean useIndices ) {
        this.useIndices = useIndices;
    }

}
