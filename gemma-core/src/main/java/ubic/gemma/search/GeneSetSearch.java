/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.search;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.genome.gene.GOGroupValueObject;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class GeneSetSearch {

    private static Log log = LogFactory.getLog( GeneSetSearch.class );
    
    @Autowired
    private Gene2GOAssociationService gene2GoService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneSetService geneSetService;

    @Autowired
    private TaxonService taxonService;
    
    /**
     * @param gene
     * @return
     * @see ubic.gemma.genome.gene.service.GeneSetService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public Collection<GeneSet> findByGene( Gene gene ) {
        return geneSetService.findByGene( gene );
    }

    /**
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is not persistent.
     * 
     * @param goId
     * @param taxon
     * @return a GeneSet or null if nothing is found
     */
    public GOGroupValueObject findGeneSetValueObjectByGoId( String goId, Long taxonId ) {


        // shouldn't need to set the taxon here, should be taken care of when creating the value object
        Taxon taxon = null;
        
        if ( taxonId != null ) {
           taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                log.warn( "No such taxon with id=" + taxonId );
            } else {
                GeneSet result = findByGoId( goId, taxonService.load(taxonId) );
                if ( result == null ) {
                    log.warn( "No matching gene set found for: " + goId );
                    return null;
                }
                GOGroupValueObject ggvo = new GOGroupValueObject( result, goId, goId );
                
                ggvo.setTaxonId( taxon.getId() );
                ggvo.setTaxonName( taxon.getCommonName() );
            
                return ggvo;
            } 
        }
        return null;
    }

    /**
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is not persistent.
     * 
     * @param goId
     * @param taxon
     * @return a GeneSet or null if nothing is found
     */
    public GeneSet findByGoId( String goId, Taxon taxon ) {
        OntologyTerm goTerm = GeneOntologyServiceImpl.getTermForId( StringUtils.strip( goId ) );

        if ( goTerm == null ) {
            return null;
        }

        return goTermToGeneSet( goTerm, taxon );
    }

    /**
     * finds genesets by go term name eg: "trans-hexaprenyltranstransferase activity" Note: the gene sets returned are
     * not persistent
     * 
     * @param goTermName
     * @param taxon
     * @return a collection with the hits
     */
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon ) {
        return findByGoTermName( goTermName, taxon, null );
    }

    /**
     * finds genesets by go term name eg: "trans-hexaprenyltranstransferase activity" Note: the gene sets returned are
     * not persistent
     * 
     * @param goTermName
     * @param taxon
     * @param maxGoTermsProcessed
     * @return a collection with the hits
     */
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon, Integer maxGoTermsProcessed ) {
        Collection<? extends OntologyResource> matches = this.geneOntologyService.findTerm( StringUtils
                .strip( goTermName ) );

        Collection<GeneSet> results = new HashSet<GeneSet>();

        Integer termsProcessed = 0;

        for ( OntologyResource t : matches ) {
            GeneSet converted = goTermToGeneSet( t, taxon );
            if ( converted != null ) results.add( converted );

            if ( maxGoTermsProcessed != null ) {
                termsProcessed++;
                if ( termsProcessed > maxGoTermsProcessed ) {
                    return results;
                }
            }
        }

        return results;

    }

    /**
     * @param name
     * @return
     * @see ubic.gemma.genome.gene.service.GeneSetService#findByName(java.lang.String)
     */
    public Collection<GeneSet> findByName( String name ) {
        return geneSetService.findByName( StringUtils.strip( name ) );
    }

    /**
     * @param name
     * @param taxon
     * @return
     */
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return geneSetService.findByName( StringUtils.strip( name ), taxon );
    }

    /**
     * Convert a GO term to a 'GeneSet', including genes from all child terms.
     */
    private GeneSet goTermToGeneSet( OntologyResource term, Taxon taxon ) {
        if ( term == null ) return null;
        if ( term.getUri() == null ) return null;

        Collection<OntologyResource> allMatches = new HashSet<OntologyResource>();

        if ( term instanceof OntologyTerm ) {
            allMatches.addAll( this.geneOntologyService.getAllChildren( ( OntologyTerm ) term ) );
        }
        allMatches.add( term );

        Collection<Gene> genes = new HashSet<Gene>();

        for ( OntologyResource t : allMatches ) {
            String goId = uri2goid( t );
            /*
             * This is a slow step. We might want to defer it. Getting a count would be faster
             */
            genes.addAll( this.gene2GoService.findByGOTerm( goId, taxon ) );
        }

        GeneSet transientGeneSet = GeneSet.Factory.newInstance();
        transientGeneSet.setName( uri2goid( term ) );

        if ( term.getLabel().toUpperCase().startsWith( "GO_" ) ) {
            // hm, this is an individual or a 'resource', not a 'class', but it's a real GO term. How to get the text.
        }

        transientGeneSet.setDescription( term.getLabel() );

        for ( Gene gene : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( gene );
            transientGeneSet.getMembers().add( gmember );
        }
        return transientGeneSet;
    }

    private String uri2goid( OntologyResource t ) {
        return t.getUri().replaceFirst( ".*/GO#", "" );
    }
    

    /**
     * Similar to method of same name in GeneSetController.java but here: - no taxon needed - GO groups always searched
     * - GeneSet objects returned instead of GeneSetValueObjects
     * 
     * @param query string to match to a gene set.
     * @param taxonId
     * @return collection of GeneSet
     */
    public Collection<GeneSet> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSet>();
        }
        Collection<GeneSet> foundGeneSets = null;
        Taxon tax = null;
        tax = taxonService.load( taxonId );

        if ( tax == null ) {
            // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
            foundGeneSets = findByName( query );
        } else {
            foundGeneSets = findByName( query, tax );
        }

        foundGeneSets.clear(); // for testing general search

        /*
         * SEARCH GENE ONTOLOGY
         */

        if ( query.toUpperCase().startsWith( "GO" ) ) {
            GeneSet goSet = findByGoId( query, tax );
            if ( goSet != null ) foundGeneSets.add( goSet );
        } else {
            foundGeneSets.addAll( findByGoTermName( query, tax ) );
        }

        return foundGeneSets;
    }

}
