/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

/**
 * Dao implementation for gene2geneproteinassociations.
 * 
 * @author ldonnison
 * @version $Id$
 */

@Repository
public class Gene2GeneProteinAssociationDaoImpl extends Gene2GeneProteinAssociationDaoBase {

    private static Log log = LogFactory.getLog( Gene2GeneProteinAssociationDaoImpl.class.getName() );

    @Autowired
    public Gene2GeneProteinAssociationDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends Gene2GeneProteinAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from Gene2GeneProteinAssociationImpl where id in (:ids)",
                "ids", ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.util.Collection)
     */
    public Collection create( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GeneProteinAssociation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( Gene2GeneProteinAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    public Gene2GeneProteinAssociation create( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        if ( gene2GeneProteinAssociation == null ) {
            throw new IllegalArgumentException(
                    "Gene2GeneProteinAssociation.create - 'Gene2GeneProteinAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( gene2GeneProteinAssociation );

        return gene2GeneProteinAssociation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#createOrUpdate(java.lang.Object)
     */
    public Gene2GeneProteinAssociation createOrUpdate( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        if ( gene2GeneProteinAssociation == null ) {
            throw new IllegalArgumentException(
                    "Gene2GeneProteinAssociation.createOrUpdate - 'Gene2GeneProteinAssociation' can not be null" );
        }
        Gene2GeneProteinAssociation gene2GeneProteinAssociationExisting = this.find( gene2GeneProteinAssociation );
        // does not exist in db there are no two genes with interaction stored
        if ( gene2GeneProteinAssociationExisting == null ) {
            this.create( gene2GeneProteinAssociation );
        } else {
            // check if this is really an update such that the confidence score, evidence vector or the url has changed
            // as such update other wise just
            // return the record from the db.
            gene2GeneProteinAssociationExisting.setConfidenceScore( gene2GeneProteinAssociation.getConfidenceScore() );
            gene2GeneProteinAssociationExisting.getDatabaseEntry().setAccession(
                    gene2GeneProteinAssociation.getDatabaseEntry().getAccession() );
            gene2GeneProteinAssociationExisting.getDatabaseEntry().setUri(
                    gene2GeneProteinAssociation.getDatabaseEntry().getUri() );
            gene2GeneProteinAssociationExisting.setEvidenceVector( gene2GeneProteinAssociation.getEvidenceVector() );
            this.update( gene2GeneProteinAssociationExisting );
            log.debug( "Existing record updating with id " + gene2GeneProteinAssociationExisting.getId() );
        }

        return gene2GeneProteinAssociation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.lang.Long)
     */
    public Gene2GeneProteinAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GeneProteinAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( Gene2GeneProteinAssociationImpl.class, id );
        return ( Gene2GeneProteinAssociation ) entity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#loadAll()
     */
    public Collection loadAll() {
        final Collection results = this.getHibernateTemplate().loadAll( Gene2GeneProteinAssociationImpl.class );
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GeneProteinAssociation.remove - 'id' can not be null" );
        }
        Gene2GeneProteinAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.util.Collection)
     */
    public void remove( Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GeneProteinAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Object)
     */
    public void remove( Gene2GeneProteinAssociation Gene2GeneProteinAssociation ) {
        if ( Gene2GeneProteinAssociation == null ) {
            throw new IllegalArgumentException(
                    "Gene2GeneProteinAssociation.remove - 'Gene2GeneProteinAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( Gene2GeneProteinAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.util.Collection)
     */
    public void update( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GeneProteinAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( Gene2GeneProteinAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.lang.Object)
     */
    public void update( Gene2GeneProteinAssociation Gene2GeneProteinAssociation ) {
        if ( Gene2GeneProteinAssociation == null ) {
            throw new IllegalArgumentException(
                    "Gene2GeneProteinAssociation.update - 'Gene2GeneProteinAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( Gene2GeneProteinAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.association.Gene2GeneProteinAssociationDao#find(ubic.gemma.model.association.
     * Gene2GeneProteinAssociation)
     */
    public Gene2GeneProteinAssociation find( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {

        try {
            Criteria queryObject = super.getSession().createCriteria( Gene2GeneProteinAssociation.class );
            // have to have gene 1 and gene 2 there
            BusinessKey.checkKey( gene2GeneProteinAssociation );

            BusinessKey.createQueryObject( queryObject, gene2GeneProteinAssociation );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() == 1 ) {
                    result = results.iterator().next();
                } else if ( results.size() > 1 ) {
                    log.error( "Multiple interactions  found for " + gene2GeneProteinAssociation + ":" );

                    Collections.sort( results, new Comparator<Gene2GeneProteinAssociation>() {
                        public int compare( Gene2GeneProteinAssociation arg0, Gene2GeneProteinAssociation arg1 ) {
                            return arg0.getId().compareTo( arg1.getId() );
                        }
                    } );
                    result = results.iterator().next();
                    log.error( "Returning arbitrary gene2GeneProteinAssociation: " + result );
                }
            }
            return ( Gene2GeneProteinAssociation ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.association.Gene2GeneProteinAssociationDao#findProteinInteractionsForGene(ubic.gemma.model.
     * association.Gene2GeneProteinAssociation)
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene ) {

        try {

            String queryStr = "from Gene2GeneProteinAssociationImpl where :gene = firstGene.id or :gene = secondGene.id";
            Query queryObject = super.getSession().createQuery( queryStr ).setLong( "gene", gene.getId() );
            java.util.List results = queryObject.list();

            if ( results != null ) {
                return results;
            }
            log.debug( "No interactions found for gene " + gene.getId() );
            return null;

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.association.Gene2GeneProteinAssociationDao#thaw(ubic.gemma.model.association.
     * Gene2GeneProteinAssociation)
     */
    @Override
    public void thaw( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) throws Exception {
        if ( gene2GeneProteinAssociation == null ) return;
        if ( gene2GeneProteinAssociation.getId() == null ) return;

        Session session = this.getSession();

        EntityUtils.attach( session, gene2GeneProteinAssociation, Gene2GeneProteinAssociationImpl.class,
                gene2GeneProteinAssociation.getId() );
        Hibernate.initialize( gene2GeneProteinAssociation );
        Hibernate.initialize( gene2GeneProteinAssociation.getFirstGene() );
        Hibernate.initialize( gene2GeneProteinAssociation.getSecondGene() );

        if ( gene2GeneProteinAssociation.getSecondGene().getTaxon() != null
                && gene2GeneProteinAssociation.getSecondGene().getTaxon().getId() != null ) {
            Hibernate.initialize( gene2GeneProteinAssociation.getSecondGene().getTaxon() );
            Hibernate.initialize( gene2GeneProteinAssociation.getFirstGene().getTaxon() );
        }

        session.evict( gene2GeneProteinAssociation );

    }

}