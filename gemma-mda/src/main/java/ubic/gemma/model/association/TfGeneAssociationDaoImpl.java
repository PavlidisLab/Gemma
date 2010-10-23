/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.model.association;

import java.util.Collection;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.Gene;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
@Repository
public class TfGeneAssociationDaoImpl extends HibernateDaoSupport implements TfGeneAssociationDao {

    @Autowired
    public TfGeneAssociationDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<? extends TfGeneAssociation> create( final Collection<? extends TfGeneAssociation> entities ) {

        if ( entities == null ) {
            throw new IllegalArgumentException( "TfGeneAssociation - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.association.TfGeneAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public TfGeneAssociation create( TfGeneAssociation entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "TfGeneAssociation - 'entity' can not be null" );
        }
        this.getHibernateTemplate().save( entity );
        return entity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends TfGeneAssociation> findByTargetGene( Gene gene ) {
        return this.getHibernateTemplate().findByNamedParam( "from PazarAssociationImpl p inner join fetch p.secondGene inner join fetch p.firstGene where p.secondGene = :g", "g",
                gene );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends TfGeneAssociation> findByTf( Gene tf ) {
        return this.getHibernateTemplate().findByNamedParam( "from PazarAssociationImpl p inner join fetch p.secondGene inner join fetch p.firstGene where p.firstGene = :g", "g", tf );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends TfGeneAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from PazarAssociationImpl  p inner join fetch p.secondGene inner join fetch p.firstGene where p.id in (:ids)", "ids", ids );
    }

    @Override
    public TfGeneAssociation load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "TfGeneAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.association.PazarAssociationImpl.class,
                id );
        return ( ubic.gemma.model.association.PazarAssociation ) entity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends TfGeneAssociation> loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.association.TfGeneAssociation.class );

        return results;
    }

    @Override
    public void remove( Collection<? extends TfGeneAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "TfGeneAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );

    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.association.TfGeneAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }

    }

    @Override
    public void remove( TfGeneAssociation entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( " TfGeneAssociation.remove - 'entity' can not be null" );
        }
        this.getHibernateTemplate().delete( entity );

    }

    @Override
    public void removeAll() {
        this.getHibernateTemplate().deleteAll( loadAll() );
    }

    @Override
    public void update( Collection<? extends TfGeneAssociation> entities ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void update( TfGeneAssociation entity ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
