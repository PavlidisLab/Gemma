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
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is a transient entity
     * 
     * @param goId
     * @param taxon
     * @return
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
     * transient entity
     * 
     * @param goTermName
     * @param taxon
     * @return
     */
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon ) {
        Collection<OntologyTerm> matches = this.geneOntologyService.findTerm( StringUtils.strip( goTermName ) );

        Collection<GeneSet> results = new HashSet<GeneSet>();

        for ( OntologyTerm t : matches ) {
            results.add( goTermToGeneSet( t, taxon ) );
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
    private GeneSet goTermToGeneSet( OntologyTerm term, Taxon taxon ) {

        Collection<OntologyTerm> allMatches = this.geneOntologyService.getAllChildren( term );
        allMatches.add( term );

        Collection<Gene> genes = new HashSet<Gene>();

        for ( OntologyTerm t : allMatches ) {
            String goId = uri2goid( t );
            genes.addAll( this.gene2GoService.findByGOTerm( goId, taxon ) );
        }

        GeneSet transientGeneSet = GeneSet.Factory.newInstance();
        transientGeneSet.setName( uri2goid( term ) );
        transientGeneSet.setDescription( term.getLabel() );

        for ( Gene gene : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( gene );
            transientGeneSet.getMembers().add( gmember );
        }
        return transientGeneSet;
    }

    private String uri2goid( OntologyTerm t ) {
        return t.getUri().replaceFirst( ".*/GO#", "" );
    }

}
