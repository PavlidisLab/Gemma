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
package ubic.gemma.model.genome.gene;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneAlias</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.GeneAlias
 */
public abstract class GeneAliasDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport implements
        ubic.gemma.model.genome.gene.GeneAliasDao {

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection create( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneAlias.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<?> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.genome.gene.GeneAlias ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#create(int transform, ubic.gemma.model.genome.gene.GeneAlias)
     */
    @Override
    public GeneAlias create( final ubic.gemma.model.genome.gene.GeneAlias geneAlias ) {
        if ( geneAlias == null ) {
            throw new IllegalArgumentException( "GeneAlias.create - 'geneAlias' can not be null" );
        }
        this.getHibernateTemplate().save( geneAlias );
        return geneAlias;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#load(int, java.lang.Long)
     */
    @Override
    public GeneAlias load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneAlias.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.gene.GeneAliasImpl.class, id );
        return ( ubic.gemma.model.genome.gene.GeneAlias ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#loadAll(int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<GeneAlias> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.GeneAliasImpl.class );
        return ( Collection<GeneAlias> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneAlias.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.GeneAlias entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneAlias.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#remove(ubic.gemma.model.genome.gene.GeneAlias)
     */
    @Override
    public void remove( ubic.gemma.model.genome.gene.GeneAlias geneAlias ) {
        if ( geneAlias == null ) {
            throw new IllegalArgumentException( "GeneAlias.remove - 'geneAlias' can not be null" );
        }
        this.getHibernateTemplate().delete( geneAlias );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneAlias.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<?> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.GeneAlias ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneAliasDao#update(ubic.gemma.model.genome.gene.GeneAlias)
     */
    @Override
    public void update( ubic.gemma.model.genome.gene.GeneAlias geneAlias ) {
        if ( geneAlias == null ) {
            throw new IllegalArgumentException( "GeneAlias.update - 'geneAlias' can not be null" );
        }
        this.getHibernateTemplate().update( geneAlias );
    }

}