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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.CandidateGeneList</code>.
 * 
 * @see ubic.gemma.model.genome.gene.CandidateGeneList
 */
public abstract class CandidateGeneListDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.gene.CandidateGeneListDao {

    private ubic.gemma.model.genome.gene.CandidateGeneDao candidateGeneDao;

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.gene.CandidateGeneList ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#create(int transform,
     *      ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) {
        if ( candidateGeneList == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.create - 'candidateGeneList' can not be null" );
        }
        this.getHibernateTemplate().save( candidateGeneList );
        return this.transformEntity( transform, candidateGeneList );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#create(ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public CandidateGeneList create( ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) {
        return ( ubic.gemma.model.genome.gene.CandidateGeneList ) this.create( TRANSFORM_NONE, candidateGeneList );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByContributer(int, java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */

    public java.util.Collection findByContributer( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( owner );
        argNames.add( "owner" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByContributer(int,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */

    public java.util.Collection findByContributer( final int transform,
            final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        return this
                .findByContributer(
                        transform,
                        "from edu.columbia.gemma.genome.gene.CandidateGeneListImpl list inner join list.candidates c where c.owner=:owner order by list.name",
                        owner );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByContributer(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */

    public java.util.Collection findByContributer( final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        return this.findByContributer( TRANSFORM_NONE, queryString, owner );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByContributer(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public java.util.Collection findByContributer( ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        return this.findByContributer( TRANSFORM_NONE, owner );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByGeneOfficialName(int, java.lang.String)
     */

    public java.util.Collection findByGeneOfficialName( final int transform, final java.lang.String officialName ) {
        return this
                .findByGeneOfficialName(
                        transform,
                        "from edu.columbia.gemma.genome.gene.CandidateGeneListImpl list inner join list.candidates as c inner join c.gene as g where g.officialName=:officialName order by list.name",
                        officialName );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByGeneOfficialName(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection findByGeneOfficialName( final int transform, final java.lang.String queryString,
            final java.lang.String officialName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialName );
        argNames.add( "officialName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByGeneOfficialName(java.lang.String)
     */
    public java.util.Collection findByGeneOfficialName( java.lang.String officialName ) {
        return this.findByGeneOfficialName( TRANSFORM_NONE, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByGeneOfficialName(java.lang.String, java.lang.String)
     */

    public java.util.Collection findByGeneOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        return this.findByGeneOfficialName( TRANSFORM_NONE, queryString, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByID(int, java.lang.Long)
     */

    public Object findByID( final int transform, final java.lang.Long id ) {
        return this.findByID( transform,
                "from edu.columbia.gemma.genome.gene.CandidateGeneListImpl list where list.id=:id order by list.name",
                id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByID(int, java.lang.String, java.lang.Long)
     */

    public Object findByID( final int transform, final java.lang.String queryString, final java.lang.Long id ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( id );
        argNames.add( "id" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.gene.CandidateGeneList"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.genome.gene.CandidateGeneList ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByID(java.lang.Long)
     */
    public ubic.gemma.model.genome.gene.CandidateGeneList findByID( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.gene.CandidateGeneList ) this.findByID( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByID(java.lang.String, java.lang.Long)
     */
    public ubic.gemma.model.genome.gene.CandidateGeneList findByID( final java.lang.String queryString,
            final java.lang.Long id ) {
        return ( ubic.gemma.model.genome.gene.CandidateGeneList ) this.findByID( TRANSFORM_NONE, queryString, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByListOwner(int, java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */

    public java.util.Collection findByListOwner( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( owner );
        argNames.add( "owner" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByListOwner(int,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */

    public java.util.Collection findByListOwner( final int transform,
            final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        return this
                .findByListOwner(
                        transform,
                        "from edu.columbia.gemma.genome.gene.CandidateGeneListImpl list where list.owner=:owner order by list.name",
                        owner );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByListOwner(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */

    public java.util.Collection findByListOwner( final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        return this.findByListOwner( TRANSFORM_NONE, queryString, owner );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#findByListOwner(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public java.util.Collection findByListOwner( ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        return this.findByListOwner( TRANSFORM_NONE, owner );
    }

    public Collection<? extends CandidateGeneList> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from CandidateGeneListImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.gene.CandidateGeneListImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.gene.CandidateGeneList ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#load(java.lang.Long)
     */

    public CandidateGeneList load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.gene.CandidateGeneList ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.CandidateGeneListImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.CandidateGeneList entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#remove(ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public void remove( ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) {
        if ( candidateGeneList == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.remove - 'candidateGeneList' can not be null" );
        }
        this.getHibernateTemplate().delete( candidateGeneList );
    }

    /**
     * Sets the reference to <code>candidateGeneDao</code>.
     */
    public void setCandidateGeneDao( ubic.gemma.model.genome.gene.CandidateGeneDao candidateGeneDao ) {
        this.candidateGeneDao = candidateGeneDao;
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.CandidateGeneList ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListDao#update(ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public void update( ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) {
        if ( candidateGeneList == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.update - 'candidateGeneList' can not be null" );
        }
        this.getHibernateTemplate().update( candidateGeneList );
    }

    /**
     * Gets the reference to <code>candidateGeneDao</code>.
     */
    protected ubic.gemma.model.genome.gene.CandidateGeneDao getCandidateGeneDao() {
        return this.candidateGeneDao;
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.gene.CandidateGeneList)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.gene.CandidateGeneListDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.gene.CandidateGeneList)
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
     * <code>ubic.gemma.model.genome.gene.CandidateGeneListDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.gene.CandidateGeneListDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.gene.CandidateGeneList entity ) {
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