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
package ubic.gemma.model.genome;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.ExpressionQtl</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.ExpressionQtl
 */
public abstract class ExpressionQtlDaoBase extends BaseQtlDaoImpl<ExpressionQtl> {

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#create(int, java.util.Collection)
     */

    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.ExpressionQtl ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#create(int transform, ubic.gemma.model.genome.ExpressionQtl)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.ExpressionQtl expressionQtl ) {
        if ( expressionQtl == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.create - 'expressionQtl' can not be null" );
        }
        this.getHibernateTemplate().save( expressionQtl );
        return this.transformEntity( transform, expressionQtl );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#create(ubic.gemma.model.genome.ExpressionQtl)
     */
    public ExpressionQtl create( ubic.gemma.model.genome.ExpressionQtl expressionQtl ) {
        return ( ubic.gemma.model.genome.ExpressionQtl ) this.create( TRANSFORM_NONE, expressionQtl );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#findByPhysicalMarkers(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalMarker, ubic.gemma.model.genome.PhysicalMarker)
     */

    @Override
    public java.util.Collection findByPhysicalMarkers( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalMarker startMarker,
            final ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( startMarker );
        argNames.add( "startMarker" );
        args.add( endMarker );
        argNames.add( "endMarker" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#findByPhysicalMarkers(int, ubic.gemma.model.genome.PhysicalMarker,
     *      ubic.gemma.model.genome.PhysicalMarker)
     */

    @Override
    public java.util.Collection findByPhysicalMarkers( final int transform,
            final ubic.gemma.model.genome.PhysicalMarker startMarker,
            final ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        return this
                .findByPhysicalMarkers(
                        transform,
                        "from QtlImpl qtl where (qtl.startMaker.physicalLocation.chromosome = :n.physicalLocation.chromosome and qtl.startMaker.physicalLocation.nucleotide > :n.physicalLocation.nucleotide and qtl.endMarker.physicalLocation.nucleotide < (:n.physicalLocation.nucleotide + :n.physicalLocation.nucleotide.nucleotideLength)",
                        startMarker, endMarker );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#findByPhysicalMarkers(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalMarker, ubic.gemma.model.genome.PhysicalMarker)
     */

    @Override
    public java.util.Collection findByPhysicalMarkers( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalMarker startMarker,
            final ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        return this.findByPhysicalMarkers( TRANSFORM_NONE, queryString, startMarker, endMarker );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#findByPhysicalMarkers(ubic.gemma.model.genome.PhysicalMarker,
     *      ubic.gemma.model.genome.PhysicalMarker)
     */

    @Override
    public java.util.Collection findByPhysicalMarkers( ubic.gemma.model.genome.PhysicalMarker startMarker,
            ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        return this.findByPhysicalMarkers( TRANSFORM_NONE, startMarker, endMarker );
    }

    public Collection<? extends ExpressionQtl> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionQtlImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.ExpressionQtlImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.ExpressionQtl ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#load(java.lang.Long)
     */

    public ExpressionQtl load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.ExpressionQtl ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.ExpressionQtlImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.ExpressionQtl entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#remove(ubic.gemma.model.genome.ExpressionQtl)
     */
    public void remove( ubic.gemma.model.genome.ExpressionQtl expressionQtl ) {
        if ( expressionQtl == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.remove - 'expressionQtl' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionQtl );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.ExpressionQtl ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.ExpressionQtlDao#update(ubic.gemma.model.genome.ExpressionQtl)
     */
    public void update( ubic.gemma.model.genome.ExpressionQtl expressionQtl ) {
        if ( expressionQtl == null ) {
            throw new IllegalArgumentException( "ExpressionQtl.update - 'expressionQtl' can not be null" );
        }
        this.getHibernateTemplate().update( expressionQtl );
    }

}