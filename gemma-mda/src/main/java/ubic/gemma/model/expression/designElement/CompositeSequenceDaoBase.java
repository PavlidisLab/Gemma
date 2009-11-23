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
package ubic.gemma.model.expression.designElement;

import java.util.Collection;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.designElement.CompositeSequence</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.designElement.CompositeSequence
 */
public abstract class CompositeSequenceDaoBase extends
        ubic.gemma.model.expression.designElement.DesignElementDaoImpl<CompositeSequence> implements
        ubic.gemma.model.expression.designElement.CompositeSequenceDao {

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.countAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends CompositeSequence> create( final int transform,
            final java.util.Collection<? extends CompositeSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CompositeSequence.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends CompositeSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#create(int transform,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public CompositeSequence create( final int transform,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        if ( compositeSequence == null ) {
            throw new IllegalArgumentException( "CompositeSequence.create - 'compositeSequence' can not be null" );
        }
        this.getHibernateTemplate().save( compositeSequence );
        return ( CompositeSequence ) this.transformEntity( transform, compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#create(java.util.Collection)
     */
    public java.util.Collection<? extends CompositeSequence> create(
            final java.util.Collection<? extends CompositeSequence> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#create(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public CompositeSequence create( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this.create( TRANSFORM_NONE, compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    
    public CompositeSequence find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( compositeSequence );
        argNames.add( "compositeSequence" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.designElement.CompositeSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.designElement.CompositeSequence ) result );
        return ( CompositeSequence ) result;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#find(int,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public CompositeSequence find( final int transform,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.compositeSequence = :compositeSequence",
                        compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#find(java.lang.String,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence find( final java.lang.String queryString,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this.find( TRANSFORM_NONE, queryString, compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#find(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence find(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this.find( TRANSFORM_NONE, compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public java.util.Collection<CompositeSequence> findByBioSequence(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleFindByBioSequence( bioSequence );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByBioSequenceName(java.lang.String)
     */
    public java.util.Collection<CompositeSequence> findByBioSequenceName( final java.lang.String name ) {
        try {
            return this.handleFindByBioSequenceName( name );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.findByBioSequenceName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(int, java.lang.String,
     *      ubic.gemma.model.genome.Gene)
     */
    
    public java.util.Collection<CompositeSequence> findByGene( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene );
        argNames.add( "gene" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(int, java.lang.String,
     *      ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    
    public java.util.Collection<CompositeSequence> findByGene( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene );
        argNames.add( "gene" );
        args.add( arrayDesign );
        argNames.add( "arrayDesign" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(int, ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<CompositeSequence> findByGene( final int transform,
            final ubic.gemma.model.genome.Gene gene ) {
        return this
                .findByGene(
                        transform,
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.gene = :gene",
                        gene );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(int, ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> findByGene( final int transform,
            final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this
                .findByGene(
                        transform,
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.gene = :gene and compositeSequence.arrayDesign = :arrayDesign",
                        gene, arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(java.lang.String,
     *      ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<CompositeSequence> findByGene( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return this.findByGene( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(java.lang.String,
     *      ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> findByGene( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.findByGene( TRANSFORM_NONE, queryString, gene, arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<CompositeSequence> findByGene( ubic.gemma.model.genome.Gene gene ) {
        return this.findByGene( TRANSFORM_NONE, gene );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByGene(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> findByGene( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.findByGene( TRANSFORM_NONE, gene, arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(int, java.lang.String)
     */
    public java.util.Collection<CompositeSequence> findByName( final int transform, final java.lang.String name ) {
        return this
                .findByName(
                        transform,
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.name = :name",
                        name );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(int, java.lang.String,
     *      java.lang.String)
     */
    
    public java.util.Collection<CompositeSequence> findByName( final int transform, final java.lang.String queryString,
            final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(int, java.lang.String,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)
     */
    
    public CompositeSequence findByName( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( arrayDesign );
        argNames.add( "arrayDesign" );
        args.add( name );
        argNames.add( "name" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.designElement.CompositeSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.designElement.CompositeSequence ) result );
        return ( CompositeSequence ) result;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(int,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)
     */
    public CompositeSequence findByName( final int transform,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.String name ) {
        return this
                .findByName(
                        transform,
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.arrayDesign = :arrayDesign and compositeSequence.name = :name",
                        arrayDesign, name );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(java.lang.String)
     */
    public java.util.Collection<CompositeSequence> findByName( java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection<CompositeSequence> findByName( final java.lang.String queryString,
            final java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, name );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(java.lang.String,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence findByName( final java.lang.String queryString,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, arrayDesign, name );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.String)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence findByName(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, arrayDesign, name );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    
    public CompositeSequence findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( compositeSequence );
        argNames.add( "compositeSequence" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.designElement.CompositeSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.designElement.CompositeSequence ) result );
        return ( CompositeSequence ) result;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findOrCreate(int,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public CompositeSequence findOrCreate( final int transform,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.compositeSequence = :compositeSequence",
                        compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence findOrCreate(
            final java.lang.String queryString,
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence findOrCreate(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this.findOrCreate( TRANSFORM_NONE, compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#getGenes(java.util.Collection)
     */
    public java.util.Map<CompositeSequence, Collection<Gene>> getGenes(
            final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            return this.handleGetGenes( compositeSequences );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.getGenes(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public java.util.Collection<Gene> getGenes(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            return this.handleGetGenes( compositeSequence );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.getGenes(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#getGenesWithSpecificity(java.util.Collection)
     */
    public java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            return this.handleGetGenesWithSpecificity( compositeSequences );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.getGenesWithSpecificity(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#getRawSummary(java.util.Collection,
     *      java.lang.Integer)
     */
    public java.util.Collection<Object[]> getRawSummary(
            final java.util.Collection<CompositeSequence> compositeSequences, final java.lang.Integer numResults ) {
        try {
            return this.handleGetRawSummary( compositeSequences, numResults );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.getRawSummary(java.util.Collection compositeSequences, java.lang.Integer numResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.Integer)
     */
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.Integer numResults ) {
        try {
            return this.handleGetRawSummary( arrayDesign, numResults );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence,
     *      java.lang.Integer)
     */
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence,
            final java.lang.Integer numResults ) {
        try {
            return this.handleGetRawSummary( compositeSequence, numResults );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#load(int, java.lang.Long)
     */

    public CompositeSequence load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CompositeSequence.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.designElement.CompositeSequenceImpl.class, id );
        return ( CompositeSequence ) transformEntity( transform,
                ( ubic.gemma.model.expression.designElement.CompositeSequence ) entity );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#load(java.lang.Long)
     */

    public CompositeSequence load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#load(java.util.Collection)
     */
    public java.util.Collection<CompositeSequence> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#loadAll()
     */
    public java.util.Collection<CompositeSequence> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#loadAll(int)
     */

    
    public java.util.Collection<CompositeSequence> loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.designElement.CompositeSequenceImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CompositeSequence.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.designElement.CompositeSequence entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends CompositeSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CompositeSequence.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#remove(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public void remove( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        if ( compositeSequence == null ) {
            throw new IllegalArgumentException( "CompositeSequence.remove - 'compositeSequence' can not be null" );
        }
        this.getHibernateTemplate().delete( compositeSequence );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#thaw(java.util.Collection)
     */
    public void thaw( final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            this.handleThaw( compositeSequences );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceDao.thaw(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends CompositeSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CompositeSequence.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends CompositeSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDao#update(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public void update( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        if ( compositeSequence == null ) {
            throw new IllegalArgumentException( "CompositeSequence.update - 'compositeSequence' can not be null" );
        }
        this.getHibernateTemplate().update( compositeSequence );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioSequenceName(java.lang.String)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequenceName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<Gene>> handleGetGenes(
            java.util.Collection<CompositeSequence> compositeSequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenes(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesWithSpecificity(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            java.util.Collection<CompositeSequence> compositeSequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getRawSummary(java.util.Collection, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            java.util.Collection<CompositeSequence> compositeSequences, java.lang.Integer numResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoad( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<CompositeSequence> compositeSequences )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.expression.designElement.CompositeSequence)} method. This method
     * does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.expression.designElement.CompositeSequenceDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.expression.designElement.CompositeSequence)
     */

    protected void transformEntities( final int transform, final java.util.Collection<CompositeSequence> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.expression.designElement.CompositeSequenceDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.expression.designElement.CompositeSequenceDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.expression.designElement.CompositeSequence entity ) {
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