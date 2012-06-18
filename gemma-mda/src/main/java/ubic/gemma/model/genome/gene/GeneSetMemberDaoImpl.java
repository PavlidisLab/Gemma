/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

/**
 * @author kelsey
 * @version $ID
 */
@Repository
public class GeneSetMemberDaoImpl extends HibernateDaoSupport implements GeneSetMemberDao {

    @Autowired
    public GeneSetMemberDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<? extends GeneSetMember> create( final Collection<? extends GeneSetMember> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSetMember.create - 'genesetmembers' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends GeneSetMember> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public GeneSetMember create( GeneSetMember entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSetMember.create - 'genesetmember' can not be null" );
        }
        this.getHibernateTemplate().save( entity );

        return entity;

    }

    @Override
    public Collection<? extends GeneSetMember> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneSetMemberImpl where id in (:ids)", "ids", ids );

    }

    @Override
    public GeneSetMember load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneSetMember.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( GeneSetMember.class, id );
    }

    @Override
    public Collection<? extends GeneSetMember> loadAll() {
        final java.util.Collection<GeneSetMemberImpl> results = this.getHibernateTemplate().loadAll(
                GeneSetMemberImpl.class );
        return results;
    }

    @Override
    public void remove( Collection<? extends GeneSetMember> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSetMember.remove - 'Collection of geneSetmembers' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneSetMember.remove - 'id' can not be null" );
        }
        GeneSetMember entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }

    }

    @Override
    public void remove( GeneSetMember entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSetMember.remove - 'geneset entity' can not be null" );
        }
        this.getHibernateTemplate().delete( entity );

    }

    @Override
    public void update( final Collection<? extends GeneSetMember> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "GeneSetMember.update - 'Collection of geneSetsMembers' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends GeneSetMember> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );

    }

    @Override
    public void update( GeneSetMember entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSetMember.update - 'geneSetmember' can not be null" );
        }
        this.getHibernateTemplate().update( entity );

    }

}
