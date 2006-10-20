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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome.gene;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public class GeneProductDaoImpl extends ubic.gemma.model.genome.gene.GeneProductDaoBase {

    /* (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#handleGetGenesByName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByName( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select gene from GeneImpl as gene inner join gene.products where  gene.products.name like :search";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#handleGetGenesByNcbiId(java.lang.String)
     */
    @Override
    protected Collection handleGetGenesByNcbiId( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select gene from GeneImpl as gene inner join gene.products where  gene.products.ncbiId like :search";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    private static Log log = LogFactory.getLog( GeneProductDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#find(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public GeneProduct find( GeneProduct geneProduct ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( GeneProduct.class );

            BusinessKey.checkKey( geneProduct );

            BusinessKey.createQueryObject( queryObject, geneProduct );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + GeneProduct.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( GeneProduct ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public GeneProduct findOrCreate( GeneProduct geneProduct ) {
        GeneProduct existingGeneProduct = this.find( geneProduct );
        if ( existingGeneProduct != null ) {
            return existingGeneProduct;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new geneProduct: " + geneProduct.getName() );
        return ( GeneProduct ) create( geneProduct );
    }
}