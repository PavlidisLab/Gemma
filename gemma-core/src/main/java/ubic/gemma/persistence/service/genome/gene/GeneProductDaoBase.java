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
package ubic.gemma.persistence.service.genome.gene;

import org.hibernate.SessionFactory;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneProduct</code>.
 *
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public abstract class GeneProductDaoBase extends VoEnabledDao<GeneProduct, GeneProductValueObject>
        implements GeneProductDao {

    public GeneProductDaoBase( SessionFactory sessionFactory ) {
        super( GeneProduct.class, sessionFactory );
    }

    @Override
    public GeneProduct findByNcbiId( String ncbiId ) {
        return ( GeneProduct ) this.getSessionFactory().getCurrentSession().createQuery( "from GeneProductImpl g where g.ncbiGi = :ncbiId" )
                .setParameter( "ncbiId", ncbiId ).uniqueResult();
    }

    /**
     * @see GeneProductDao#getGenesByName(String)
     */
    @Override
    public Collection<Gene> getGenesByName( final String search ) {
        try {
            return this.handleGetGenesByName( search );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'GeneProductDao.getGenesByName(String search)' --> " + th,
                    th );
        }
    }

    /**
     * @see GeneProductDao#getGenesByNcbiId(String)
     */
    @Override
    public Collection<Gene> getGenesByNcbiId( final String search ) {
        try {
            return this.handleGetGenesByNcbiId( search );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'GeneProductDao.getGenesByNcbiId(String search)' --> " + th,
                    th );
        }
    }

    /**
     * Performs the core logic for {@link #getGenesByName(String)}
     */
    protected abstract Collection<Gene> handleGetGenesByName( String search ) throws Exception;

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(String)}
     */
    protected abstract Collection<Gene> handleGetGenesByNcbiId( String search ) throws Exception;

}