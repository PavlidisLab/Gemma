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
 * author: anton
 * date: 18/03/13
 */
public class SearchSettingsValueObject implements Serializable {

    public SearchSettingsValueObject() {
    }

    private static final long serialVersionUID = -934534534L;

    private String query;
    private String termUri;
    private Integer maxResults = Integer.valueOf( 500 );
    private Boolean searchExperiments = Boolean.valueOf( true );
    private Boolean searchGenes = Boolean.valueOf( true );
    private Boolean searchPlatforms = Boolean.valueOf( true );
    private Boolean searchExperimentSets = Boolean.valueOf( true );
    private Boolean searchPhenotypes = Boolean.valueOf( true );
    private Boolean searchProbes = Boolean.valueOf( true );
    private Boolean searchGeneSets = java.lang.Boolean.valueOf( true );
    private Boolean searchBioSequences = Boolean.valueOf( true );
    private Boolean searchBibrefs = Boolean.valueOf( true );
    private Boolean useIndices = Boolean.valueOf( true );
    private Boolean useDatabase = Boolean.valueOf( true );
    private Boolean useCharacteristics = Boolean.valueOf( true );
    private Boolean useGo = Boolean.valueOf( true );


    public String getQuery() {
        return this.query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public String getTermUri() {
        return this.termUri;
    }

    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }

    public Integer getMaxResults() {
        return this.maxResults;
    }

    public void setMaxResults( Integer maxResults ) {
        this.maxResults = maxResults;
    }

    public Boolean getSearchExperiments() {
        return this.searchExperiments;
    }

    public void setSearchExperiments( Boolean searchExperiments ) {
        this.searchExperiments = searchExperiments;
    }

    public Boolean getSearchGenes() {
        return this.searchGenes;
    }

    public void setSearchGenes( Boolean searchGenes ) {
        this.searchGenes = searchGenes;
    }

    public Boolean getSearchPlatforms() {
        return this.searchPlatforms;
    }

    public void setSearchPlatforms( Boolean searchPlatforms ) {
        this.searchPlatforms = searchPlatforms;
    }

    public Boolean getSearchExperimentSets() {
        return this.searchExperimentSets;
    }

    public void setSearchExperimentSets( Boolean searchExperimentSets ) {
        this.searchExperimentSets = searchExperimentSets;
    }

    public Boolean getSearchPhenotypes() {
        return this.searchPhenotypes;
    }

    public void setSearchPhenotypes( Boolean searchPhenotypes ) {
        this.searchPhenotypes = searchPhenotypes;
    }

    public Boolean getSearchProbes() {
        return this.searchProbes;
    }

    public void setSearchProbes( Boolean searchProbes ) {
        this.searchProbes = searchProbes;
    }

    public Boolean getSearchGeneSets() {
        return this.searchGeneSets;
    }

    public void setSearchGeneSets( Boolean searchGeneSets ) {
        this.searchGeneSets = searchGeneSets;
    }

    public Boolean getSearchBioSequences() {
        return this.searchBioSequences;
    }

    public void setSearchBioSequences( Boolean searchBioSequences ) {
        this.searchBioSequences = searchBioSequences;
    }

    public Boolean getSearchBibrefs() {
        return this.searchBibrefs;
    }

    public void setSearchBibrefs( Boolean searchBibrefs ) {
        this.searchBibrefs = searchBibrefs;
    }

    public Boolean getUseIndices() {
        return this.useIndices;
    }

    public void setUseIndices( Boolean useIndices ) {
        this.useIndices = useIndices;
    }

    public Boolean getUseDatabase() {
        return this.useDatabase;
    }

    public void setUseDatabase( Boolean useDatabase ) {
        this.useDatabase = useDatabase;
    }

    public Boolean getUseCharacteristics() {
        return this.useCharacteristics;
    }

    public void setUseCharacteristics( Boolean useCharacteristics ) {
        this.useCharacteristics = useCharacteristics;
    }

    public Boolean getUseGo() {
        return this.useGo;
    }

    public void setUseGo( Boolean useGo ) {
        this.useGo = useGo;
    }

    private Taxon taxon;
    private ArrayDesign platformConstraint;

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public ArrayDesign getPlatformConstraint() {
        return this.platformConstraint;
    }

    public void setPlatformConstraint( ArrayDesign platformConstraint ) {
        this.platformConstraint = platformConstraint;
    }

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.search.SearchSettings}.
     */
    public static final class Converter {

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

    }

}
