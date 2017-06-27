/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.association;

import org.hibernate.SessionFactory;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.Gene2GOAssociation</code>.
 *
 * @see ubic.gemma.model.association.Gene2GOAssociation
 */
public abstract class Gene2GOAssociationDaoBase extends AbstractDao<Gene2GOAssociation>
        implements Gene2GOAssociationDao {



    protected Gene2GOAssociationDaoBase( SessionFactory sessionFactory ) {
        super( Gene2GOAssociation.class, sessionFactory );
    }



    /**
     * @see Gene2GOAssociationDao#findAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<Gene2GOAssociation> findAssociationByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindAssociationByGene( gene );
    }

    /**
     * @see Gene2GOAssociationDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<VocabCharacteristic> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindByGene( gene );
    }

    @Override
    public Collection<Gene> findByGoTerm( final java.lang.String goId ) {
        return this.handleFindByGoTerm( goId );
    }

    /**
     * @see Gene2GOAssociationDao#findByGoTerm(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Gene> findByGoTerm( final java.lang.String goId, final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByGoTerm( goId, taxon );
    }

    /**
     * @see Gene2GOAssociationDao#findByGOTerm(Collection, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Gene> findByGOTerm( final Collection<String> goTerms,
            final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByGOTerm( goTerms, taxon );
    }

    /**
     * @see Gene2GOAssociationDao#removeAll()
     */
    @Override
    public void removeAll() {
        try {
            this.handleRemoveAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException( "Error performing 'Gene2GOAssociationDao.removeAll()' --> " + th,
                    th );
        }
    }

    @Override
    public void update( Collection<Gene2GOAssociation> entities ) {
        throw new UnsupportedOperationException( "Immutable, update not supported" );
    }

    @Override
    public void update( Gene2GOAssociation gene2GOAssociation ) {
        throw new UnsupportedOperationException( "Immutable, update not supported" );
    }



    /**
     * Performs the core logic for {@link #findAssociationByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<Gene2GOAssociation> handleFindAssociationByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<VocabCharacteristic> handleFindByGene( ubic.gemma.model.genome.Gene gene );

    protected abstract Collection<Gene> handleFindByGoTerm( java.lang.String goId );

    /**
     * Performs the core logic for {@link #findByGoTerm(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<Gene> handleFindByGoTerm( java.lang.String goId,
            ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #findByGOTerm(Collection, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<Gene> handleFindByGOTerm( Collection<String> goTerms,
            ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #removeAll()}
     */
    protected abstract void handleRemoveAll();

}