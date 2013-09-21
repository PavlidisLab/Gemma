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

/**
 * 
 */
public abstract class SearchSettings implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.search.SearchSettings}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.search.SearchSettings}.
         */
        public static ubic.gemma.model.common.search.SearchSettings newInstance() {
            return new ubic.gemma.model.common.search.SearchSettingsImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -982243911532743661L;
    private String query;

    private String termUri;

    private Integer maxResults = Integer.valueOf( 500 );

    private Boolean searchExperiments = Boolean.valueOf( true );

    private Boolean searchGenes = Boolean.valueOf( true );

    private Boolean searchPlatforms = Boolean.valueOf( true );

    private Boolean searchExperimentSets = Boolean.valueOf( true );

    private Boolean searchPhenotypes = Boolean.valueOf( true );

    private Boolean searchProbes = Boolean.valueOf( true );

    private Boolean searchGeneSets = Boolean.valueOf( true );

    private Boolean searchBioSequences = Boolean.valueOf( true );

    private Boolean searchBibrefs = Boolean.valueOf( true );

    private Boolean useIndices = Boolean.valueOf( true );

    private Boolean useDatabase = Boolean.valueOf( true );

    private Boolean useCharacteristics = Boolean.valueOf( true );

    private Boolean useGo = Boolean.valueOf( true );

    private Long id;

    private ubic.gemma.model.genome.Taxon taxon;

    private ubic.gemma.model.expression.arrayDesign.ArrayDesign platformConstraint;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public SearchSettings() {
    }

    /**
     * Returns <code>true</code> if the argument is an SearchSettings instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SearchSettings ) ) {
            return false;
        }
        final SearchSettings that = ( SearchSettings ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
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
    public Integer getMaxResults() {
        return this.maxResults;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign getPlatformConstraint() {
        return this.platformConstraint;
    }

    /**
     * 
     */
    public String getQuery() {
        return this.query;
    }

    /**
     * 
     */
    public Boolean getSearchBibrefs() {
        return this.searchBibrefs;
    }

    /**
     * 
     */
    public Boolean getSearchBioSequences() {
        return this.searchBioSequences;
    }

    /**
     * 
     */
    public Boolean getSearchExperiments() {
        return this.searchExperiments;
    }

    /**
     * 
     */
    public Boolean getSearchExperimentSets() {
        return this.searchExperimentSets;
    }

    /**
     * 
     */
    public Boolean getSearchGenes() {
        return this.searchGenes;
    }

    /**
     * 
     */
    public Boolean getSearchGeneSets() {
        return this.searchGeneSets;
    }

    /**
     * 
     */
    public Boolean getSearchPhenotypes() {
        return this.searchPhenotypes;
    }

    /**
     * 
     */
    public Boolean getSearchPlatforms() {
        return this.searchPlatforms;
    }

    /**
     * 
     */
    public Boolean getSearchProbes() {
        return this.searchProbes;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    /**
     * 
     */
    public String getTermUri() {
        return this.termUri;
    }

    /**
     * 
     */
    public Boolean getUseCharacteristics() {
        return this.useCharacteristics;
    }

    /**
     * 
     */
    public Boolean getUseDatabase() {
        return this.useDatabase;
    }

    /**
     * 
     */
    public Boolean getUseGo() {
        return this.useGo;
    }

    /**
     * 
     */
    public Boolean getUseIndices() {
        return this.useIndices;
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

    /**
     * 
     */
    public abstract void noSearches();

    public void setId( Long id ) {
        this.id = id;
    }

    public void setMaxResults( Integer maxResults ) {
        this.maxResults = maxResults;
    }

    public void setPlatformConstraint( ubic.gemma.model.expression.arrayDesign.ArrayDesign platformConstraint ) {
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

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
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