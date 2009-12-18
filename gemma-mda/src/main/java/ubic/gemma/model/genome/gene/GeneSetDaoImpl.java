/*
 * The Gemma project.
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
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneSet</code>.
 * 
 * @author kelsey
 * @see ubic.gemma.model.genome.gene.GeneSet
 * @version $ID
 */
@Repository
public class GeneSetDaoImpl extends HibernateDaoSupport implements GeneSetDao {


    @Autowired
    public GeneSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }
    
    @Override
    public Collection<? extends GeneSet> create( final Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.create - 'genesets' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( GeneSet ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public GeneSet create( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.create - 'geneset' can not be null" );
        }
        this.getHibernateTemplate().save( entity );

        return entity;

    }

    @Override
    public Collection<? extends GeneSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneSetImpl where id in (:ids)", "ids", ids );
    }

    @Override
    public GeneSet load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.load - 'id' can not be null" );
        }
        return ( GeneSet ) this.getHibernateTemplate().get( GeneSet.class, id );

    }

    @Override
    public Collection<? extends GeneSet> loadAll() {
        final java.util.Collection<GeneSetImpl> results = this.getHibernateTemplate().loadAll( GeneSetImpl.class );
        return results;
    }

    @Override
    public void remove( Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'Collection of geneSet' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'id' can not be null" );
        }
        GeneSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }

    }

    @Override
    public void remove( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'geneset entity' can not be null" );
        }
        this.getHibernateTemplate().delete( entity );

    }

    @Override
    public void update( final Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.update - 'Collection of geneSets' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( GeneSet ) entityIterator.next() );
                        }
                        return null;
                    }
                } );

    }

    @Override
    public void update( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.update - 'geneSet' can not be null" );
        }
        this.getHibernateTemplate().update( entity );

    }

}
