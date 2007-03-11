/*
 * The Gemma project.
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

package ubic.gemma.model.association;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociationService
 * @author klc
 * @version $Id$
 */
public class Gene2GOAssociationServiceImpl extends ubic.gemma.model.association.Gene2GOAssociationServiceBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Collection<OntologyEntry>> handleCalculateGoTermOverlap( Gene masterGene, Collection geneIds )
            throws Exception {

        if ( masterGene == null ) return null;
        if ( geneIds.size() == 0 ) return null;

        Collection<OntologyEntry> masterOntos = getGOTerms( masterGene );

        // nothing to do.
        if ( ( masterOntos == null ) || ( masterOntos.isEmpty() ) ) return null;

        Map<Long, Collection<OntologyEntry>> overlap = new HashMap<Long, Collection<OntologyEntry>>();
        overlap.put( masterGene.getId(), masterOntos ); // include the master gene in the list. Clearly 100% overlap
        // with itself!

        if ( ( geneIds == null ) || ( geneIds.isEmpty() ) ) return overlap;

        Collection<Gene> genes = this.getGeneService().load( geneIds );

        for ( Object obj : genes ) {
            Gene gene = ( Gene ) obj;
            Collection<OntologyEntry> comparisonOntos = getGOTerms( gene );

            if ( ( comparisonOntos == null ) || ( comparisonOntos.isEmpty() ) ) {
                overlap.put( gene.getId(), new HashSet<OntologyEntry>() );
                continue;
            }

            overlap.put( gene.getId(), computerOverlap( masterOntos, comparisonOntos ) );
        }

        return overlap;
    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    @SuppressWarnings("unchecked")
    private Collection<OntologyEntry> getGOTerms( Gene gene ) {

        Collection<OntologyEntry> ontEntry = findByGene( gene );
        Collection<OntologyEntry> allGOTermSet = new HashSet<OntologyEntry>( ontEntry );

        if ( ( ontEntry == null ) || ontEntry.isEmpty() ) return null;

        Map<OntologyEntry, Collection<OntologyEntry>> parentMap = this.getOntologyEntryService().getAllParents(
                ontEntry );

        for ( OntologyEntry oe : parentMap.keySet() ) {
            allGOTermSet.addAll( parentMap.get( oe ) ); // add the parents
            allGOTermSet.add( oe ); // add the child
        }

        return allGOTermSet;
    }

    private Collection<OntologyEntry> computerOverlap( Collection<OntologyEntry> masterOntos,
            Collection<OntologyEntry> comparisonOntos ) {
        Collection<OntologyEntry> overlapTerms = new HashSet<OntologyEntry>( masterOntos );
        overlapTerms.retainAll( comparisonOntos );

        return overlapTerms;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationServiceBase#handleFindByGOTerm(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByGOTerm( String goID, Taxon taxon ) throws Exception {

        OntologyEntry searchOnto = this.getOntologyEntryService().findByAccession( goID );

        if ( searchOnto == null ) return new ArrayList();

        Collection searchOntologies = this.getOntologyEntryService().getAllChildren( searchOnto );
        searchOntologies.add( searchOnto );

        return this.getGene2GOAssociationDao().findByGOTerm( searchOntologies, taxon );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationServiceBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        return this.getGene2GOAssociationDao().findByGene( gene );

    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#find(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected ubic.gemma.model.association.Gene2GOAssociation handleFind(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().find( gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#create(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected ubic.gemma.model.association.Gene2GOAssociation handleCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return ( Gene2GOAssociation ) this.getGene2GOAssociationDao().create( gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findOrCreate(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected ubic.gemma.model.association.Gene2GOAssociation handleFindOrCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().findOrCreate( gene2GOAssociation );
    }

}