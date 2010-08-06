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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.ontology.providers.GeneOntologyService;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class GeneSetSearch {

    @Autowired
    private Gene2GOAssociationService gene2GoService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneSetService geneSetService;

    /**
     * @param gene
     * @return
     * @see ubic.gemma.model.genome.gene.GeneSetService#findByGene(ubic.gemma.model.genome.Gene)
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
    public GeneSet findByGoId( String goId, Taxon taxon ) {
        OntologyTerm goTerm = GeneOntologyService.getTermForId( StringUtils.strip( goId ) );

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
        Collection<? extends OntologyResource> matches = this.geneOntologyService.findTerm( StringUtils
                .strip( goTermName ) );

        Collection<GeneSet> results = new HashSet<GeneSet>();

        for ( OntologyResource t : matches ) {
            GeneSet converted = goTermToGeneSet( t, taxon );
            if ( converted != null ) results.add( converted );
        }

        return results;

    }

    /**
     * @param name
     * @return
     * @see ubic.gemma.model.genome.gene.GeneSetService#findByName(java.lang.String)
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

}
