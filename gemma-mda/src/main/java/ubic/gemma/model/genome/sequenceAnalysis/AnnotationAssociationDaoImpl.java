/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.util.BusinessKey;

/**
 * @author paul
 * @version $Id$
 */
@Repository
public class AnnotationAssociationDaoImpl extends HibernateDaoSupport implements AnnotationAssociationDao {

    @Autowired
    public AnnotationAssociationDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#create(ubic.gemma.model.genome.sequenceAnalysis
     * .AnnotationAssociation)
     */
    @Override
    public AnnotationAssociation create( AnnotationAssociation annotationAssociation ) {
        if ( annotationAssociation == null ) {
            throw new IllegalArgumentException(
                    "AnnotationAssociation.create - 'annotationAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( annotationAssociation );
        return annotationAssociation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#create(java.util.Collection)
     */
    @Override
    public Collection<AnnotationAssociation> create( final Collection<AnnotationAssociation> anCollection ) {

        if ( anCollection == null ) {
            throw new IllegalArgumentException( "AnnotationAssociation.create - 'anCollection' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<AnnotationAssociation> entityIterator = anCollection.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return anCollection;

    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#find(ubic.gemma.model.genome.biosequence.
     * BioSequence)
     */
    @Override
    public Collection<AnnotationAssociation> find( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( AnnotationAssociation.class );

        BusinessKey.attachCriteria( queryObject, bioSequence, "bioSequence" );

        return queryObject.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#find(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<AnnotationAssociation> find( Gene gene ) {
        if ( gene.getProducts().size() == 0 ) {
            throw new IllegalArgumentException( "Gene has no products" );
        }

        Collection<AnnotationAssociation> result = new HashSet<AnnotationAssociation>();

        for ( GeneProduct geneProduct : gene.getProducts() ) {

            BusinessKey.checkValidKey( geneProduct );

            Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( AnnotationAssociation.class );
            Criteria innerQuery = queryObject.createCriteria( "geneProduct" );

            if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) ) {
                innerQuery.add( Restrictions.eq( "ncbiGi", geneProduct.getNcbiGi() ) );
            }

            if ( StringUtils.isNotBlank( geneProduct.getName() ) ) {
                innerQuery.add( Restrictions.eq( "name", geneProduct.getName() ) );
            }

            result.addAll( queryObject.list() );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#load(java.util.Collection)
     */
    @Override
    public Collection<AnnotationAssociation> load( Collection<Long> ids ) {
        if ( ids.size() == 0 ) {
            return new HashSet<AnnotationAssociation>();
        }
        int BATCH_SIZE = 2000;

        final String queryString = "select a from AnnotationAssociationImpl a where a.id in (:ids)";
        Collection<Long> batch = new HashSet<Long>();
        Collection<AnnotationAssociation> results = new HashSet<AnnotationAssociation>();

        for ( Long id : ids ) {
            batch.add( id );
            if ( batch.size() == BATCH_SIZE ) {
                results.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            results.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#load(java.lang.Long)
     */
    @Override
    public AnnotationAssociation load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AnnotationAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation.class, id );
        return ( ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation ) entity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#remove(ubic.gemma.model.genome.sequenceAnalysis
     * .AnnotationAssociation)
     */
    @Override
    public void remove( AnnotationAssociation annotationAssociation ) {
        if ( annotationAssociation == null ) {
            throw new IllegalArgumentException(
                    "AnnotationAssociation.remove - 'annotationAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( annotationAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#remove(java.util.Collection)
     */
    @Override
    public void remove( Collection<AnnotationAssociation> anCollection ) {
        if ( anCollection == null ) {
            throw new IllegalArgumentException( "AnnotationAssociation.remove - 'anCollection' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( anCollection );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#thaw(ubic.gemma.model.genome.sequenceAnalysis
     * .AnnotationAssociation)
     */
    @Override
    public void thaw( final AnnotationAssociation annotationAssociation ) {
        if ( annotationAssociation == null ) return;
        if ( annotationAssociation.getId() == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                thawAssociation( session, annotationAssociation );
                return null;
            }
        } );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final Collection<AnnotationAssociation> anCollection ) {
        if ( anCollection == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Object object : anCollection ) {
                    AnnotationAssociation blatAssociation = ( AnnotationAssociation ) object;
                    if ( blatAssociation.getId() == null ) continue;
                    thawAssociation( session, blatAssociation );
                }

                return null;
            }

        } );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#update(ubic.gemma.model.genome.sequenceAnalysis
     * .AnnotationAssociation)
     */
    @Override
    public void update( AnnotationAssociation annotationAssociation ) {
        if ( annotationAssociation == null ) {
            throw new IllegalArgumentException(
                    "AnnotationAssociation.update - 'annotationAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( annotationAssociation );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao#update(java.util.Collection)
     */
    @Override
    public void update( final Collection<AnnotationAssociation> anCollection ) {
        if ( anCollection == null ) {
            throw new IllegalArgumentException( "AnnotationAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<AnnotationAssociation> entityIterator = anCollection.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    private void thawAssociation( org.hibernate.Session session, AnnotationAssociation association ) {
        session.update( association );
        session.update( association.getBioSequence() );
        session.update( association.getGeneProduct() );
        session.update( association.getGeneProduct().getGene() );
        session.update( association.getGeneProduct().getGene().getPhysicalLocation() );
        association.getGeneProduct().getGene().getProducts().size();
        session.update( association.getBioSequence() );
        association.getBioSequence().getSequenceDatabaseEntry();
    }

    @Override
    public Collection<AnnotationAssociation> find( Collection<GeneProduct> gps ) {
        if ( gps.isEmpty() ) return new HashSet<AnnotationAssociation>();
        return this.getHibernateTemplate().findByNamedParam(
                "select b from AnnotationAssociationImpl b join b.geneProduct gp where gp in (:gps)", "gps", gps );
    }

}
