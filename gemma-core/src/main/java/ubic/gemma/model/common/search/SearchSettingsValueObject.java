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
@SuppressWarnings({ "WeakerAccess", "unused" }) // Used in frontend
public class SearchSettingsValueObject implements Serializable {

    private static final long serialVersionUID = -934534534L;
    private Integer maxResults = SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE;
    private ArrayDesign platformConstraint;
    private String query;
    private Boolean searchBibrefs = Boolean.TRUE;
    private Boolean searchBioSequences = Boolean.TRUE;
    private Boolean searchExperiments = Boolean.TRUE;
    private Boolean searchExperimentSets = Boolean.TRUE;
    private Boolean searchGenes = Boolean.TRUE;
    private Boolean searchGeneSets = Boolean.TRUE;
    private Boolean searchPlatforms = Boolean.TRUE;
    private Boolean searchProbes = Boolean.TRUE;
    private Taxon taxon;
    private String termUri;
    private Boolean useCharacteristics = Boolean.TRUE;
    private Boolean useDatabase = Boolean.TRUE;
    private Boolean useGo = Boolean.TRUE;
    private Boolean useIndices = Boolean.TRUE;

    public SearchSettingsValueObject() {
    }

    public Integer getMaxResults() {
        return this.maxResults;
    }

    public void setMaxResults( Integer maxResults ) {
        this.maxResults = maxResults;
    }

    public ArrayDesign getPlatformConstraint() {
        return this.platformConstraint;
    }

    public void setPlatformConstraint( ArrayDesign platformConstraint ) {
        this.platformConstraint = platformConstraint;
    }

    public String getQuery() {
        return this.query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public Boolean getSearchBibrefs() {
        return this.searchBibrefs;
    }

    public void setSearchBibrefs( Boolean searchBibrefs ) {
        this.searchBibrefs = searchBibrefs;
    }

    public Boolean getSearchBioSequences() {
        return this.searchBioSequences;
    }

    public void setSearchBioSequences( Boolean searchBioSequences ) {
        this.searchBioSequences = searchBioSequences;
    }

    public Boolean getSearchExperiments() {
        return this.searchExperiments;
    }

    public void setSearchExperiments( Boolean searchExperiments ) {
        this.searchExperiments = searchExperiments;
    }

    public Boolean getSearchExperimentSets() {
        return this.searchExperimentSets;
    }

    public void setSearchExperimentSets( Boolean searchExperimentSets ) {
        this.searchExperimentSets = searchExperimentSets;
    }

    public Boolean getSearchGenes() {
        return this.searchGenes;
    }

    public void setSearchGenes( Boolean searchGenes ) {
        this.searchGenes = searchGenes;
    }

    public Boolean getSearchGeneSets() {
        return this.searchGeneSets;
    }

    public void setSearchGeneSets( Boolean searchGeneSets ) {
        this.searchGeneSets = searchGeneSets;
    }

    public Boolean getSearchPlatforms() {
        return this.searchPlatforms;
    }

    public void setSearchPlatforms( Boolean searchPlatforms ) {
        this.searchPlatforms = searchPlatforms;
    }

    public Boolean getSearchProbes() {
        return this.searchProbes;
    }

    public void setSearchProbes( Boolean searchProbes ) {
        this.searchProbes = searchProbes;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public String getTermUri() {
        return this.termUri;
    }

    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }

    public Boolean getUseCharacteristics() {
        return this.useCharacteristics;
    }

    public void setUseCharacteristics( Boolean useCharacteristics ) {
        this.useCharacteristics = useCharacteristics;
    }

    public Boolean getUseDatabase() {
        return this.useDatabase;
    }

    public void setUseDatabase( Boolean useDatabase ) {
        this.useDatabase = useDatabase;
    }

    public Boolean getUseGo() {
        return this.useGo;
    }

    public void setUseGo( Boolean useGo ) {
        this.useGo = useGo;
    }

    public Boolean getUseIndices() {
        return this.useIndices;
    }

    public void setUseIndices( Boolean useIndices ) {
        this.useIndices = useIndices;
    }
}
