/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.genome.gene;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.model.genome.gene.GOGroupValueObject;
import ubic.gemma.model.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneSearchService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.ParseSearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResultDisplayObject;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.controller.persistence.SessionListManager;

import java.io.IOException;
import java.util.*;

/**
 * For 'live searches' from the web interface.
 *
 * @author luke
 */
@SuppressWarnings("unused") // Used in front end
@Controller
public class GenePickerController {

    @Autowired
    private GeneSearchService geneSearchService;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private SessionListManager sessionListManager;

    @Autowired
    private TaxonService taxonService = null;

    /**
     * AJAX
     *
     * @param  geneIds gene ids
     * @return collection of gene entity objects; duplicates will be resolved.
     */
    public Collection<GeneValueObject> getGenes( Collection<Long> geneIds ) {
        if ( geneIds == null || geneIds.isEmpty() ) {
            return new HashSet<>();
        }

        return geneService.loadValueObjectsByIds( new HashSet<>( geneIds ) );
    }

    /**
     * for AJAX get all genes in the given taxon that are annotated with the given go id, including its child terms in
     * the hierarchy
     *
     * @param  goId    GO id that must be in the format "GO_#######"
     * @param  taxonId must not be null and must correspond to a taxon
     * @return Collection empty if goId was blank or taxonId didn't correspond to a taxon
     */
    public Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId ) {

        if ( !StringUtils.isBlank( goId ) && goId.toUpperCase().startsWith( "GO" ) ) {

            return geneSearchService.getGenesByGOId( goId, taxonId );
        }

        return new HashSet<>();
    }

    /**
     * for AJAX get a gene set with all genes in the given taxon that are annotated with the given go id, including its
     * child terms in the hierarchy
     *
     * @param  goId    GO id that must be in the format "GO_#######"
     * @param  taxonId must not be null and must correspond to a taxon
     * @return GOGroupValueObject empty if goId was blank or taxonId didn't correspond to a taxon
     */
    public GOGroupValueObject getGeneSetByGOId( String goId, Long taxonId ) {

        return geneSetSearch.findGeneSetValueObjectByGoId( goId, taxonId );

    }

    /**
     * AJAX
     *
     * @return a collection of the taxa in gemma (whether usable or not)
     */
    public Collection<TaxonValueObject> getTaxa() {

        return taxonService.loadAllValueObjects();
    }

    /**
     * AJAX
     *
     * @return List of taxa with array designs in gemma
     */
    public Collection<TaxonValueObject> getTaxaWithArrays() {

        return taxonService.getTaxaWithArrays();
    }

    /**
     * AJAX
     *
     * @return collection of taxa that have expression experiments available.
     */
    public Collection<TaxonValueObject> getTaxaWithDatasets() {

        return taxonService.getTaxaWithDatasets();
    }

    /**
     * AJAX
     *
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    public Collection<TaxonValueObject> getTaxaWithGenes() {
        return taxonService.getTaxaWithGenes();
    }

    /**
     * AJAX (used by GeneCombo.js)
     *
     * @param  query   query
     * @param  taxonId taxon id
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) {
        try {
            return geneService.searchGenes( query, taxonId );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }
    }

    /**
     * AJAX (used by GeneAndGeneGroupCombo.js)
     *
     * @param  query   query
     * @param  taxonId can be null
     * @return Collection of SearchResultDisplayObject
     */
    public Collection<SearchResultDisplayObject> searchGenesAndGeneGroups( String query, Long taxonId ) {

        // get any session-bound groups

        Collection<SessionBoundGeneSetValueObject> sessionResult = ( taxonId != null ) ? sessionListManager.getModifiedGeneSets( taxonId )
                : sessionListManager.getModifiedGeneSets();

        List<SearchResultDisplayObject> sessionSets = new ArrayList<>();

        // create SearchResultDisplayObjects
        if ( sessionResult != null && sessionResult.size() > 0 ) {
            for ( SessionBoundGeneSetValueObject gvo : sessionResult ) {
                SearchResultDisplayObject srDo = new SearchResultDisplayObject( gvo );
                srDo.setUserOwned( true );
                sessionSets.add( srDo );
            }
        }

        Collections.sort( sessionSets );

        // maintain order: session sets first
        Collection<SearchResultDisplayObject> results = new ArrayList<>();
        results.addAll( sessionSets );
        try {
            results.addAll( geneSearchService.searchGenesAndGeneGroups( query, taxonId ) );
        } catch ( ParseSearchException e ) {
            throw new IllegalArgumentException( "Invalid search query: " + query, e );
        } catch ( SearchException e ) {
            throw new RuntimeException( e );
        }

        for ( SearchResultDisplayObject r : results ) {
            r.setOriginalQuery( query );
        }
        return results;

    }

    /**
     * AJAX (used by Phenocarta)
     *
     * @param  query   query
     * @param  taxonId taxon id
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenesWithNCBIId( String query, Long taxonId ) {

        Collection<GeneValueObject> geneValueObjects = null;
        try {
            geneValueObjects = this.geneService.searchGenes( query, taxonId );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }

        Collection<GeneValueObject> geneValueObjectWithNCBIId = new HashSet<>();

        for ( GeneValueObject geneValueObject : geneValueObjects ) {
            if ( geneValueObject.getNcbiId() != null ) {
                geneValueObjectWithNCBIId.add( geneValueObject );
            }
        }

        return geneValueObjectWithNCBIId;
    }

    /**
     * AJAX Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     *
     * @param  query   A list of gene names (symbols), one per line.
     * @param  taxonId taxon id
     * @return collection of gene value objects
     */
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException {
        try {
            return geneSearchService.searchMultipleGenes( query, taxonId );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }
    }

    /**
     * AJAX Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     *
     * @param  query   A list of gene names (symbols), one per line.
     * @param  taxonId taxon id
     * @return map with each gene-query as a key and a collection of the search-results as the value
     */
    public Map<String, GeneValueObject> searchMultipleGenesGetMap( Collection<String> query, Long taxonId ) {
        try {
            return geneSearchService.searchMultipleGenesGetMap( query, taxonId );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }
    }

}