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

import ubic.gemma.model.analysis.Investigation;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneInvestigation</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.GeneInvestigation
 */
public abstract class GeneInvestigationDaoBase extends
        ubic.gemma.model.analysis.InvestigationDaoImpl<GeneInvestigation> implements
        ubic.gemma.model.genome.gene.GeneInvestigationDao {

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.gene.GeneInvestigation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#create(int transform,
     *      ubic.gemma.model.genome.gene.GeneInvestigation)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.gene.GeneInvestigation geneInvestigation ) {
        if ( geneInvestigation == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.create - 'geneInvestigation' can not be null" );
        }
        this.getHibernateTemplate().save( geneInvestigation );
        return this.transformEntity( transform, geneInvestigation );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#create(ubic.gemma.model.genome.gene.GeneInvestigation)
     */
    public GeneInvestigation create( ubic.gemma.model.genome.gene.GeneInvestigation geneInvestigation ) {
        return ( ubic.gemma.model.genome.gene.GeneInvestigation ) this.create( TRANSFORM_NONE, geneInvestigation );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#findByInvestigator(int, java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByInvestigator( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( investigator );
        argNames.add( "investigator" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#findByInvestigator(int,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByInvestigator( final int transform,
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        return this
                .findByInvestigator(
                        transform,
                        "from InvestigationImpl i inner join Contact c on c in elements(i.investigators) or c == i.owner where c == :investigator",
                        investigator );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#findByInvestigator(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByInvestigator( final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        return this.findByInvestigator( TRANSFORM_NONE, queryString, investigator );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    public java.util.Collection findByInvestigator( ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        return this.findByInvestigator( TRANSFORM_NONE, investigator );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#load(int, java.lang.Long)
     */

    public GeneInvestigation load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.gene.GeneInvestigationImpl.class, id );
        return ( GeneInvestigation ) transformEntity( transform,
                ( ubic.gemma.model.genome.gene.GeneInvestigation ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#load(java.lang.Long)
     */

    public GeneInvestigation load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.gene.GeneInvestigation ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.GeneInvestigationImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.GeneInvestigation entity = ( ubic.gemma.model.genome.gene.GeneInvestigation ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#remove(ubic.gemma.model.genome.gene.GeneInvestigation)
     */
    public void remove( ubic.gemma.model.genome.gene.GeneInvestigation geneInvestigation ) {
        if ( geneInvestigation == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.remove - 'geneInvestigation' can not be null" );
        }
        this.getHibernateTemplate().delete( geneInvestigation );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.GeneInvestigation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneInvestigationDao#update(ubic.gemma.model.genome.gene.GeneInvestigation)
     */
    public void update( ubic.gemma.model.genome.gene.GeneInvestigation geneInvestigation ) {
        if ( geneInvestigation == null ) {
            throw new IllegalArgumentException( "GeneInvestigation.update - 'geneInvestigation' can not be null" );
        }
        this.getHibernateTemplate().update( geneInvestigation );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.gene.GeneInvestigation)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.gene.GeneInvestigationDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.gene.GeneInvestigation)
     */

    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.gene.GeneInvestigationDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.gene.GeneInvestigationDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.gene.GeneInvestigation entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}