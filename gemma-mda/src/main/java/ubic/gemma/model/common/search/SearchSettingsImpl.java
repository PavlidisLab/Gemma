/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public class SearchSettingsImpl extends SearchSettings {

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings arrayDesignSearch( String query ) {
        SearchSettingsImpl s = new SearchSettingsImpl( query );
        s.noSearches();
        s.setSearchPlatforms( true );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings bibliographicReferenceSearch( String query ) {
        SearchSettings s = new SearchSettingsImpl( query );
        s.noSearches();
        s.setSearchBibrefs( true );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings compositeSequenceSearch( String query, ArrayDesign arrayDesign ) {
        SearchSettings s = new SearchSettingsImpl( query );
        s.noSearches();
        s.setSearchProbes( true );
        s.setPlatformConstraint( arrayDesign );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings expressionExperimentSearch( String query ) {
        SearchSettingsImpl s = new SearchSettingsImpl( query );
        s.setSearchGenes( false );
        s.noSearches();
        s.setSearchExperiments( true );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettingsImpl expressionExperimentSetSearch( String query ) {
        SearchSettingsImpl s = new SearchSettingsImpl( query );
        s.noSearches();
        s.setSearchExperimentSets( true );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings geneSearch( String query, Taxon taxon ) {
        SearchSettings s = new SearchSettingsImpl( query );
        s.noSearches();
        s.setSearchGenes( true );
        s.setTaxon( taxon );
        return s;
    }

    private boolean doHighlighting = false;

    public SearchSettingsImpl() {
    }

    /**
     * NOTE the query is trim()'ed, no need to do that later.
     * 
     * @param query
     */
    public SearchSettingsImpl( String query ) {
        this.setQuery( query.trim() );
    }

    public boolean getDoHighlighting() {
        return this.doHighlighting;
    }

    /**
     * Reset all search criteria to false.
     */
    @Override
    public void noSearches() {
        this.setSearchPlatforms( false );
        this.setSearchBibrefs( false );
        this.setSearchBioSequences( false );
        this.setSearchGenes( false );
        this.setUseGo( false );
        this.setSearchPhenotypes( false );
        this.setSearchExperiments( false );
        this.setSearchProbes( false );
        this.setSearchGeneSets( false );
        this.setSearchExperimentSets( false );
    }

    /**
     * Set to false to reduce overhead when highlighting isn't needed.
     * 
     * @param doHighlighting
     */
    public void setDoHighlighting( boolean doHighlighting ) {
        this.doHighlighting = doHighlighting;
    }

    @Override
    public String toString() {
        if ( !StringUtils.isBlank( this.getTermUri() ) ) {
            return this.getTermUri();
        }
        return this.getQuery();
    }

}
