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
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.arrayDesign.ArrayDesign</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
public abstract class ArrayDesignDaoBase extends HibernateDaoSupport implements ArrayDesignDao {

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#arrayDesignValueObjectToEntity(ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void arrayDesignValueObjectToEntity( ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject source,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign target, boolean copyIfNull ) {
        if ( copyIfNull || source.getShortName() != null ) {
            target.setShortName( source.getShortName() );
        }
        if ( copyIfNull || source.getTechnologyType() != null ) {
            target.setTechnologyType( TechnologyType.fromString( source.getTechnologyType() ) );
        }
        if ( copyIfNull || source.getName() != null ) {
            target.setName( source.getName() );
        }
        if ( copyIfNull || source.getDescription() != null ) {
            target.setDescription( source.getDescription() );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutBioSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutGenes(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.countAll()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends ArrayDesign> create(
            final java.util.Collection<? extends ArrayDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ArrayDesign.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<ArrayDesign>() {
                    public ArrayDesign doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ArrayDesign> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#create(int transform,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ArrayDesign create( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.create - 'arrayDesign' can not be null" );
        }
        this.getHibernateTemplate().save( arrayDesign );
        return arrayDesign;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void deleteAlignmentData( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleDeleteAlignmentData( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void deleteGeneProductAssociations( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleDeleteGeneProductAssociations( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    public ArrayDesign find( final java.lang.String queryString,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( arrayDesign );
        argNames.add( "arrayDesign" );
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.arrayDesign.ArrayDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( ArrayDesign ) result;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#find(int,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ArrayDesign find( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this
                .find( "from ubic.gemma.model.expression.arrayDesign.ArrayDesign as arrayDesign where arrayDesign.arrayDesign = :arrayDesign",
                        arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByAlternateName(java.lang.String)
     */
    public java.util.Collection<ArrayDesign> findByAlternateName( final java.lang.String queryString ) {
        try {
            return this.handleFindByAlternateName( queryString );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.findByAlternateName(java.lang.String queryString)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByName(int, java.lang.String)
     */
    public ArrayDesign findByName( final java.lang.String name ) {
        return this.findByName( "from ArrayDesignImpl a where a.name=:name", name );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByName(int, java.lang.String, java.lang.String)
     */

    public ArrayDesign findByName( final java.lang.String queryString, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ArrayDesign result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.arrayDesign.ArrayDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByShortName(int, java.lang.String)
     */
    public ArrayDesign findByShortName( final java.lang.String shortName ) {
        return this.findByShortName( "from ArrayDesignImpl a where a.shortName=:shortName", shortName );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByShortName(int, java.lang.String,
     *      java.lang.String)
     */

    public ArrayDesign findByShortName( final java.lang.String queryString, final java.lang.String shortName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( shortName );
        argNames.add( "shortName" );
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ArrayDesign result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.arrayDesign.ArrayDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */

    public ArrayDesign findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( arrayDesign );
        argNames.add( "arrayDesign" );
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ArrayDesign result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.arrayDesign.ArrayDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(int,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ArrayDesign findOrCreate( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this
                .findOrCreate(
                        "from ubic.gemma.model.expression.arrayDesign.ArrayDesign as arrayDesign where arrayDesign.arrayDesign = :arrayDesign",
                        arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getAllAssociatedBioAssays(java.lang.Long)
     */
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( final java.lang.Long id ) {
        try {
            return this.handleGetAllAssociatedBioAssays( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.getAllAssociatedBioAssays(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getAuditEvents(java.util.Collection)
     */
    public java.util.Map<Long, Collection<AuditEvent>> getAuditEvents( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetAuditEvents( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.getAuditEvents(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleGetExpressionExperiments( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getTaxa(java.lang.Long)
     */
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( final java.lang.Long id ) {
        try {
            return this.handleGetTaxa( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.getTaxa(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getTaxon(java.lang.Long)
     */
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long id ) {
        try {
            return this.handleGetTaxon( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.getTaxon(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isMerged(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isMerged( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsMerged( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.isMerged(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isMergee(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isMergee( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsMergee( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.isMergee(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isSubsumed(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isSubsumed( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsSubsumed( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.isSubsumed(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isSubsumer(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isSubsumer( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsSubsumer( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.isSubsumer(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#load(int, java.lang.Long)
     */
    public ArrayDesign load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ArrayDesign.load - 'id' can not be null" );
        }
        final ArrayDesign entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl.class, id );
        return entity;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#load(java.util.Collection)
     */
    public java.util.Collection<ArrayDesign> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadAll(int)
     */
    public java.util.Collection<? extends ArrayDesign> loadAll() {
        return this.getHibernateTemplate().loadAll( ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl.class );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadAllValueObjects()
     */
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects() {
        try {
            return this.handleLoadAllValueObjects();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.loadAllValueObjects()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadCompositeSequences(java.lang.Long)
     */
    public java.util.Collection<CompositeSequence> loadCompositeSequences( final java.lang.Long id ) {
        try {
            return this.handleLoadCompositeSequences( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.loadCompositeSequences(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadFully(java.lang.Long)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign loadFully( final java.lang.Long id ) {
        try {
            return this.handleLoadFully( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.loadFully(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadValueObjects(java.util.Collection)
     */
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadValueObjects( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.loadValueObjects(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBioSequences()
     */
    public long numAllCompositeSequenceWithBioSequences() {
        try {
            return this.handleNumAllCompositeSequenceWithBioSequences();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllCompositeSequenceWithBioSequences()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    public long numAllCompositeSequenceWithBioSequences( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllCompositeSequenceWithBioSequences( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllCompositeSequenceWithBioSequences(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBlatResults()
     */
    public long numAllCompositeSequenceWithBlatResults() {
        try {
            return this.handleNumAllCompositeSequenceWithBlatResults();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllCompositeSequenceWithBlatResults()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    public long numAllCompositeSequenceWithBlatResults( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllCompositeSequenceWithBlatResults( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllCompositeSequenceWithBlatResults(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithGenes()
     */
    public long numAllCompositeSequenceWithGenes() {
        try {
            return this.handleNumAllCompositeSequenceWithGenes();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllCompositeSequenceWithGenes()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithGenes(java.util.Collection)
     */
    public long numAllCompositeSequenceWithGenes( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllCompositeSequenceWithGenes( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllCompositeSequenceWithGenes(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllGenes()
     */
    public long numAllGenes() {
        try {
            return this.handleNumAllGenes();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllGenes()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllGenes(java.util.Collection)
     */
    public long numAllGenes( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllGenes( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numAllGenes(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numBioSequences( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumBioSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numBlatResults( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequences(java.lang.Long)
     */
    public java.lang.Integer numCompositeSequences( final java.lang.Long id ) {
        try {
            return this.handleNumCompositeSequences( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numCompositeSequences(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithBioSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithPredictedGene(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithPredictedGene(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithPredictedGene( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numCompositeSequenceWithPredictedGene(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithProbeAlignedRegion(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithProbeAlignedRegion(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithProbeAlignedRegion( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numCompositeSequenceWithProbeAlignedRegion(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.arrayDesign.ArrayDesign entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection<? extends ArrayDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void remove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'arrayDesign' can not be null" );
        }
        this.getHibernateTemplate().delete( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void removeBiologicalCharacteristics( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleRemoveBiologicalCharacteristics( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ArrayDesign thaw( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleThaw( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection<? extends ArrayDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ArrayDesign.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<ArrayDesign>() {
                    public ArrayDesign doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ArrayDesign> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#update(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void update( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.update - 'arrayDesign' can not be null" );
        }
        this.getHibernateTemplate().update( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.lang.Boolean updateSubsumingStatus(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee ) {
        try {
            return this.handleUpdateSubsumingStatus( candidateSubsumer, candidateSubsumee );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer, ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleDeleteAlignmentData( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleDeleteGeneProductAssociations(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByAlternateName(java.lang.String)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleFindByAlternateName( java.lang.String queryString )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAllAssociatedBioAssays(java.lang.Long)}
     */
    protected abstract java.util.Collection<BioAssay> handleGetAllAssociatedBioAssays( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAuditEvents(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Collection<AuditEvent>> handleGetAuditEvents( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetExpressionExperiments(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getTaxa(java.lang.Long)}
     */
    protected abstract java.util.Collection<ubic.gemma.model.genome.Taxon> handleGetTaxa( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getTaxon(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isMerged(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsMerged( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isMergee(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsMergee( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isSubsumed(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsSubsumed( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isSubsumer(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsSubsumer( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAllValueObjects()}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadAllValueObjects()
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadCompositeSequences(java.lang.Long)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoadCompositeSequences( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadFully(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleLoadFully( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadValueObjects(java.util.Collection)}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadValueObjects(
            java.util.Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes()}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllGenes()}
     */
    protected abstract long handleNumAllGenes() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllGenes(java.util.Collection)}
     */
    protected abstract long handleNumAllGenes( java.util.Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numCompositeSequences(java.lang.Long)}
     */
    protected abstract java.lang.Integer handleNumCompositeSequences( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithPredictedGene(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithPredictedGene(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithProbeAlignedRegion(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithProbeAlignedRegion(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleRemoveBiologicalCharacteristics(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ArrayDesign handleThaw( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.lang.Boolean handleUpdateSubsumingStatus(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee ) throws java.lang.Exception;

}