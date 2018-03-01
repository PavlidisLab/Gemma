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
package ubic.gemma.model.common.search;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

import java.io.Serializable;

public abstract class SearchSettings implements Identifiable, Serializable {

    /**
     * How many results per result type are allowed. This implies that if you search for multiple types of things, you
     * can get more than this.
     */
    private static final int DEFAULT_MAX_RESULTS_PER_RESULT_TYPE = 500;
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -982243911532743661L;
    private String query;
    private String termUri;
    private Integer maxResults = SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE;
    private Boolean searchExperiments = Boolean.TRUE;
    private Boolean searchGenes = Boolean.TRUE;
    private Boolean searchPlatforms = Boolean.TRUE;
    private Boolean searchExperimentSets = Boolean.TRUE;
    private Boolean searchPhenotypes = Boolean.TRUE;
    private Boolean searchProbes = Boolean.TRUE;
    private Boolean searchGeneSets = Boolean.TRUE;
    private Boolean searchBioSequences = Boolean.TRUE;
    private Boolean searchBibrefs = Boolean.TRUE;
    private Boolean useIndices = Boolean.TRUE;
    private Boolean useDatabase = Boolean.TRUE;
    private Boolean useCharacteristics = Boolean.TRUE;
    private Boolean useGo = Boolean.TRUE;
    private Long id;
    private Taxon taxon;
    private ArrayDesign platformConstraint;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public SearchSettings() {
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SearchSettings ) ) {
            return false;
        }
        final SearchSettings that = ( SearchSettings ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings("unused") // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    public Integer getMaxResults() {
        return this.maxResults;
    }

    public void setMaxResults( Integer maxResults ) {
        this.maxResults = maxResults;
    }

    public ubic.gemma.model.expression.arrayDesign.ArrayDesign getPlatformConstraint() {
        return this.platformConstraint;
    }

    public void setPlatformConstraint( ubic.gemma.model.expression.arrayDesign.ArrayDesign platformConstraint ) {
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

    public Boolean getSearchPhenotypes() {
        return this.searchPhenotypes;
    }

    public void setSearchPhenotypes( Boolean searchPhenotypes ) {
        this.searchPhenotypes = searchPhenotypes;
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

    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
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

    public abstract void noSearches();

    public static final class Factory {

        public static SearchSettings newInstance() {
            return new SearchSettingsImpl();
        }
    }

}