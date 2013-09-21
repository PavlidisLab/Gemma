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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.genome.gene.GOGroupValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.genome.gene.service.GeneCoreService;
import ubic.gemma.genome.gene.service.GeneSearchService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.web.persistence.SessionListManager;

/**
 * For 'live searches' from the web interface.
 * 
 * @author luke
 * @version $Id$
 */
@Controller
public class GenePickerController {

    // private static Log log = LogFactory.getLog( GenePickerController.class );

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private GeneSearchService geneSearchService;

    @Autowired
    private GeneCoreService geneCoreService;

    @Autowired
    private SessionListManager sessionListManager;

    /**
     * AJAX
     * 
     * @param collection of <long> geneIds
     * @return collection of gene entity objects
     */
    public Collection<GeneValueObject> getGenes( Collection<Long> geneIds ) {
        if ( geneIds == null || geneIds.isEmpty() ) {
            return new HashSet<GeneValueObject>();
        }
        return geneService.loadValueObjects( geneIds );
    }

    /**
     * for AJAX get all genes in the given taxon that are annotated with the given go id, including its child terms in
     * the hierarchy
     * 
     * @param goId GO id that must be in the format "GO_#######"
     * @param taxonId must not be null and must correspond to a taxon
     * @return Collection<GeneSet> empty if goId was blank or taxonId didn't correspond to a taxon
     */
    public Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId ) {

        if ( !StringUtils.isBlank( goId ) && goId.toUpperCase().startsWith( "GO" ) ) {

            return geneSearchService.getGenesByGOId( goId, taxonId );
        }

        return new HashSet<GeneValueObject>();
    }

    /**
     * for AJAX get a gene set with all genes in the given taxon that are annotated with the given go id, including its
     * child terms in the hierarchy
     * 
     * @param goId GO id that must be in the format "GO_#######"
     * @param taxonId must not be null and must correspond to a taxon
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
     * @return Taxon that are species. (only returns usable taxa)
     */
    public Collection<TaxonValueObject> getTaxaSpecies() {

        return taxonService.getTaxaSpecies();
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
     * @return Taxon that are on NeuroCarta evidence
     */
    public Collection<TaxonValueObject> getTaxaWithEvidence() {

        return this.taxonService.getTaxaWithEvidence();
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
     * @param query
     * @param taxonId
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) {

        return geneCoreService.searchGenes( query, taxonId );
    }

    /**
     * AJAX (used by GeneAndGeneGroupCombo.js)
     * 
     * @param query
     * @param taxonId can be null
     * @return Collection of SearchResultDisplayObject
     */
    public Collection<SearchResultDisplayObject> searchGenesAndGeneGroups( String query, Long taxonId ) {

        // get any session-bound groups

        Collection<SessionBoundGeneSetValueObject> sessionResult = ( taxonId != null ) ? sessionListManager
                .getModifiedGeneSets( taxonId ) : sessionListManager.getModifiedGeneSets();

        List<SearchResultDisplayObject> sessionSets = new ArrayList<SearchResultDisplayObject>();

        // create SearchResultDisplayObjects
        if ( sessionResult != null && sessionResult.size() > 0 ) {
            for ( SessionBoundGeneSetValueObject gvo : sessionResult ) {
                SearchResultDisplayObject srdo = new SearchResultDisplayObject( gvo );
                srdo.setUserOwned( true );
                sessionSets.add( srdo );
            }
        }

        Collections.sort( sessionSets );

        // maintain order: session sets first
        Collection<SearchResultDisplayObject> results = new ArrayList<SearchResultDisplayObject>();
        results.addAll( sessionSets );
        results.addAll( geneSearchService.searchGenesAndGeneGroups( query, taxonId ) );
        return results;

    }

    /**
     * AJAX (used by neurocarta)
     * 
     * @param query
     * @param taxonId
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenesWithNCBIId( String query, Long taxonId ) {

        Collection<GeneValueObject> geneValueObjects = this.geneCoreService.searchGenes( query, taxonId );

        Collection<GeneValueObject> geneValueObjectWithNCBIId = new HashSet<GeneValueObject>();

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
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return colleciton of gene value objects
     * @throws IOException
     */
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException {
        return geneSearchService.searchMultipleGenes( query, taxonId );
    }

    /**
     * AJAX Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return map with each gene-query as a key and a collection of the search-results as the value
     * @throws IOException
     */
    public Map<String, Collection<GeneValueObject>> searchMultipleGenesGetMap( String query, Long taxonId )
            throws IOException {
        return geneSearchService.searchMultipleGenesGetMap( query, taxonId );
    }

}