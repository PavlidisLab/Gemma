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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sf.ehcache.Element;

import org.springframework.stereotype.Service;

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociationService
 * @author klc
 * @version $Id$
 */
@Service
public class Gene2GOAssociationServiceImpl extends ubic.gemma.model.association.Gene2GOAssociationServiceBase {

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#create(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected Gene2GOAssociation handleCreate( Gene2GOAssociation gene2GOAssociation ) {
        return this.getGene2GOAssociationDao().create( gene2GOAssociation );
    }

    /**
     * @see Gene2GOAssociationService#find(Gene2GOAssociation)
     */
    @Override
    protected Gene2GOAssociation handleFind( Gene2GOAssociation gene2GOAssociation ) {
        return this.getGene2GOAssociationDao().find( gene2GOAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationServiceBase#handleFindAssociationByGene(ubic.gemma.model.genome .Gene)
     */
    @Override
    protected Collection<Gene2GOAssociation> handleFindAssociationByGene( Gene gene ) {
        return this.getGene2GOAssociationDao().findAssociationByGene( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationServiceBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<VocabCharacteristic> handleFindByGene( Gene gene ) {

        Element element = this.gene2goCache.get( gene );

        if ( element != null ) return ( Collection<VocabCharacteristic> ) element.getValue();

        Collection<VocabCharacteristic> re = this.getGene2GOAssociationDao().findByGene( gene );

        this.gene2goCache.put( new Element( gene, re ) );

        return re;

    }

    @Override
    public Map<Gene, Collection<VocabCharacteristic>> findByGenes( Collection<Gene> genes ) {
        Map<Gene, Collection<VocabCharacteristic>> result = new HashMap<Gene, Collection<VocabCharacteristic>>();

        Collection<Gene> needToFind = new HashSet<Gene>();
        for ( Gene gene : genes ) {
            Element element = this.gene2goCache.get( gene );

            if ( element != null )
                result.put( gene, ( Collection<VocabCharacteristic> ) element.getValue() );
            else
                needToFind.add( gene );
        }

        result.putAll( this.getGene2GOAssociationDao().findByGenes( needToFind ) );

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationServiceBase#handleFindByGOTerm(java.util.Collection)
     */
    @Override
    protected Collection<Gene> handleFindByGOTerm( String goID, Taxon taxon ) {
        return this.getGene2GOAssociationDao().findByGoTerm( goID, taxon );
    }

    /**
     * @see Gene2GOAssociationService#findOrCreate(Gene2GOAssociation)
     */
    @Override
    protected Gene2GOAssociation handleFindOrCreate( Gene2GOAssociation gene2GOAssociation ) {
        return this.getGene2GOAssociationDao().findOrCreate( gene2GOAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationServiceBase#handleRemoveAll()
     */
    @Override
    protected void handleRemoveAll() {
        this.getGene2GOAssociationDao().removeAll();
    }

}