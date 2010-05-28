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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.ontology.providers.GeneOntologyService;

/**
 * TODO Document Me
 * 
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
     * finds gene sets by goTermId eg: GO:0000002 Note: the gene set returned is a transient entity
     * 
     * @param goId
     * @param taxon
     * @return
     */
    public GeneSet findByGoId( String goId, Taxon taxon ) {

        String ontologyName = geneOntologyService.getTermName( goId );
        Collection<Gene> genes = this.gene2GoService.findByGOTerm( goId, taxon );
        GeneSet transientGeneSet = GeneSet.Factory.newInstance();
        if ( ontologyName == null || ontologyName.isEmpty() )
            transientGeneSet.setName( goId );
        else
            transientGeneSet.setName( ontologyName + ": " + goId );

        if ( genes == null ) return transientGeneSet;

        Collection<GeneSetMember> members = new HashSet<GeneSetMember>();
        for ( Gene gene : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( gene );
            members.add( gmember );
        }

        transientGeneSet.setMembers( members );
        return transientGeneSet;
    }

    /**
     * finds genesets by go term name eg: "trans-hexaprenyltranstransferase activity" Note: the gene set returned is a
     * transient entity
     * 
     * @param goTermName
     * @param taxon
     * @return
     */
    public GeneSet findByGoTermName( String goTermName, Taxon taxon ) {
        throw new UnsupportedOperationException();

    }

    /**
     * @param name
     * @return
     * @see ubic.gemma.model.genome.gene.GeneSetService#findByName(java.lang.String)
     */
    public Collection<GeneSet> findByName( String name ) {
        return geneSetService.findByName( name );
    }

    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return geneSetService.findByName( name, taxon );
    }

}
