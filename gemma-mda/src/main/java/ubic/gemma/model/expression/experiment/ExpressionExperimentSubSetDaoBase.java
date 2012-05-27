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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
public abstract class ExpressionExperimentSubSetDaoBase extends
        ubic.gemma.model.expression.experiment.BioAssaySetDaoImpl<ExpressionExperimentSubSet> implements
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao {

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#create(int, java.util.Collection)
     */
    public java.util.Collection<ExpressionExperimentSubSet> create( final int transform,
            final java.util.Collection<ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create(
                                    transform,
                                    ( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet ) entityIterator
                                            .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#create(int transform,
     *      ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    public Object create( final int transform,
            final ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.create - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSubSet );
        return this.transformEntity( transform, expressionExperimentSubSet );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#create(java.util.Collection)
     */

    @Override
    public java.util.Collection<ExpressionExperimentSubSet> create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#create(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public ExpressionExperimentSubSet create(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        return ( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet ) this.create( TRANSFORM_NONE,
                expressionExperimentSubSet );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#findByInvestigator(int,
     *      java.lang.String, ubic.gemma.model.common.auditAndSecurity.Contact)
     */

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
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#findByInvestigator(int,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    public java.util.Collection findByInvestigator( final int transform,
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        return this
                .findByInvestigator(
                        transform,
                        "from InvestigationImpl i inner join Contact c on c in elements(i.investigators) or c == i.owner where c == :investigator",
                        investigator );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#findByInvestigator(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    public java.util.Collection findByInvestigator( final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        return this.findByInvestigator( TRANSFORM_NONE, queryString, investigator );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    @Override
    public java.util.Collection findByInvestigator( ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        return this.findByInvestigator( TRANSFORM_NONE, investigator );
    }

    @Override
    public Collection<? extends ExpressionExperimentSubSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSubSetImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#load(int, java.lang.Long)
     */

    public ExpressionExperimentSubSet load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl.class, id );
        return ( ExpressionExperimentSubSet ) transformEntity( transform,
                ( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet ) entity );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#load(java.lang.Long)
     */

    @Override
    public ExpressionExperimentSubSet load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#loadAll()
     */

    @Override
    public java.util.Collection<ExpressionExperimentSubSet> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#loadAll(int)
     */

    public java.util.Collection<ExpressionExperimentSubSet> loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#remove(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public void remove( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.remove - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSubSet );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#update(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public void update( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.update - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSubSet );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)} method. This
     * method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
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
     * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet entity ) {
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