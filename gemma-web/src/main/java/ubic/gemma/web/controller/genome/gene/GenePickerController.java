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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.web.controller.BaseController;

/**
 * For 'live searches' from the web interface.
 * 
 * @author luke
 * @version $Id$
 */
@Controller
public class GenePickerController extends BaseController {

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SearchService searchService = null;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private static final int MAX_GENES_PER_QUERY = 20;

    private static Comparator<Taxon> TAXON_COMPARATOR = new Comparator<Taxon>() {
        public int compare( Taxon o1, Taxon o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };

    /**
     * @param geneIds
     * @return
     */
    public Collection<Gene> getGenes( Collection<Long> geneIds ) {
        return geneService.loadMultiple( geneIds );
    }

    public Collection<Taxon> getTaxa() {
        SortedSet<Taxon> taxa = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            taxa.add( taxon );
        }
        return taxa;
    }

    /**
     * @return Taxon that are species.
     */
    public Collection<Taxon> getTaxaSpecies() {
        SortedSet<Taxon> taxaSpecies = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( taxon.getIsSpecies() ) {
                taxaSpecies.add( taxon );
            }
        }
        return taxaSpecies;
    }

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    public Collection<Taxon> getTaxaWithGenes() {
        SortedSet<Taxon> taxaWithGenes = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( taxon );
            }
        }
        return taxaWithGenes;
    }

    /**
     * @return collection of taxa that have expression experiments available.
     */
    public Collection<Taxon> getTaxaWithDatasets() {
        Set<Taxon> taxaWithDatasets = new TreeSet<Taxon>( TAXON_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( perTaxonCount.containsKey( taxon ) && perTaxonCount.get( taxon ) > 0 ) {
                taxaWithDatasets.add( taxon );
            }
        }
        return taxaWithDatasets;
    }

    /**
     * @return
     */
    public Collection<Taxon> getTaxaWithArrays() {
        Set<Taxon> taxaWithDatasets = new TreeSet<Taxon>( TAXON_COMPARATOR );

        taxaWithDatasets.addAll( arrayDesignService.getPerTaxonCount().keySet() );

        return taxaWithDatasets;
    }

    /**
     * @param query
     * @param taxonId
     * @return
     */
    public Collection<Gene> searchGenes( String query, Long taxonId ) {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.GeneSearch( query, taxon );
        List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }
        log.info( "Gene search: " + query + " taxon=" + taxonId + ", " + genes.size() + " found" );
        return genes;
    }

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return
     * @throws IOException
     */
    public Collection<Gene> searchMultipleGenes( String query, Long taxonId ) throws IOException {
        Taxon taxon = taxonService.load( taxonId );

        BufferedReader reader = new BufferedReader( new StringReader( query ) );
        Collection<Gene> genes = new HashSet<Gene>();
        String line = null;

        while ( ( line = reader.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) ) continue;
            if ( genes.size() >= MAX_GENES_PER_QUERY ) {
                log.warn( "Too many genes, stopping" );
                break;
            }
            line = StringUtils.strip( line );
            SearchSettings settings = SearchSettings.GeneSearch( line, taxon );
            List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class );

            // FIXME try not to add more than one gene per query.

            // FIXME try to inform the user if there are some that don't have
            // results.
            for ( SearchResult sr : geneSearchResults ) {
                genes.add( ( Gene ) sr.getResultObject() );
            }

        }
        return genes;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param searchService The searchService to set.
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * @param taxonService The taxonService to set.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

}