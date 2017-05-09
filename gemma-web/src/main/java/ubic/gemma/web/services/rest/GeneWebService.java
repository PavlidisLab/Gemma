/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.web.services.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneCoreService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;

import com.sun.jersey.api.NotFoundException;

/**
 * RESTful web services for gene
 * 
 * @author frances
 * @version $Id $
 */

@Component
@Path("/gene")
public class GeneWebService {

    @Autowired
    private GeneCoreService geneCoreService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ChromosomeService chromosomeService;

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    @GET
    @Path("/find-gene-details")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<GeneValueObject> findGeneDetails( @QueryParam("geneId") Long geneId ) {
        ArrayList<GeneValueObject> valueObjects = new ArrayList<>( 1 ); // Contain only 1 element.
        valueObjects.add( geneCoreService.loadGeneDetails( geneId ) );
        return valueObjects;
    }

    @GET
    @Path("/find-genes-by-ncbi")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, GeneValueObject> findGenesByNcbiId( @QueryParam("ncbiIds") String ncbiIdsQuery ) {
        if ( ncbiIdsQuery == null )
            throw new NotFoundException( "Requires: ncbiIds = comma separated list of NCBI Ids" );

        Collection<Integer> ncbiIds = Lists.newArrayList();
        for ( String ncbiId : ncbiIdsQuery.split( "," ) ) {
            try {
                ncbiIds.add( Integer.valueOf( ncbiId ) );
            } catch ( NumberFormatException e ) {
                throw new NotFoundException( "Cannot convert given NCBI Id to integer: " + ncbiId );
            }
        }

        Map<Integer, GeneValueObject> result = geneService.findByNcbiIds( ncbiIds );
        for ( Integer ncbiId : ncbiIds ) {
            if ( !result.containsKey( ncbiId ) ) {
                result.put( ncbiId, null );
            }
        }
        return result;

    }

    @GET
    @Path("/find-genes-by-symbol")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, GeneValueObject> findGenesBySymbol( @QueryParam("taxonId") Long taxonId,
            @QueryParam("symbols") String symbolsQuery ) {
        if ( symbolsQuery == null )
            throw new NotFoundException( "Requires: symbols = comma separated list of Gene Symbols" );

        if ( taxonId == null )
            throw new NotFoundException( "Requires: taxonId" );

        Collection<String> symbols = Arrays.asList( symbolsQuery.split( "," ) );

        Map<String, GeneValueObject> result = geneService.findByOfficialSymbols( symbols, taxonId );
        for ( String symbol : symbols ) {
            if ( !result.containsKey( symbol.toLowerCase() ) ) {
                result.put( symbol, null );
            }
        }
        return result;

    }

    /**
     * Find genes located in a given region. Genes that overlap the query region are returned.
     * 
     * @param chromosomeName - eg: 2, 3, X
     * @param strand - eg: +, - (currently disabled)
     * @param start - start of the region
     * @param size - size of the region
     * @return GeneValue objects of the genes in the region.
     */
    @GET
    @Path("/find-genes-by-genomic-location")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<GeneValueObject> findGeneDetails( @QueryParam("chromosome") String chromosomeName,
            @QueryParam("strand") String strand, @QueryParam("start") Long start, @QueryParam("size") Integer size ) {

        // Construct query object
        PhysicalLocation region = new PhysicalLocationImpl();
        Taxon taxon = taxonService.findByCommonName( "human" );

        Collection<Chromosome> chromosomes = chromosomeService.find( chromosomeName, taxon );
        if ( chromosomes.isEmpty() ) throw new NotFoundException( "Chromosome " + chromosomeName + " not found." );

        Chromosome chromosome = chromosomes.iterator().next();
        region.setChromosome( chromosome );
        region.setNucleotide( start );
        region.setNucleotideLength( size );
        // region.setStrand( strand );

        // Do the search
        Collection<Gene> genes = geneService.find( region );

        genes = geneService.thawLite( genes );
        // Convert to value objects
        Collection<GeneValueObject> valueObjects = GeneValueObject.convert2ValueObjects( genes );

        return valueObjects;
    }

    @GET
    @Path("/find-genes-with-evidence")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<GeneEvidenceValueObject> findGenesWithEvidence( @QueryParam("geneSymbol") String geneSymbol ) {
        return phenotypeAssociationManagerService.findGenesWithEvidence( geneSymbol, null );
    }

}