/*
 
 *The Gemma project.
 * 
 * Copyright (c) 2007 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.PersistentCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
@Repository
public class ArrayDesignDaoImpl extends HibernateDaoSupport implements ArrayDesignDao {

    static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );
    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;

    @Autowired
    public ArrayDesignDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#addProbes(ubic.gemma.model.expression.arrayDesign.ArrayDesign
     * , java.util.Collection)
     */
    @Override
    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newprobes ) {
        for ( CompositeSequence compositeSequence : newprobes ) {
            compositeSequence.setArrayDesign( arrayDesign );
            this.getSession().update( compositeSequence );
        }
        this.update( arrayDesign );
    }

    public ArrayDesign arrayDesignValueObjectToEntity( ArrayDesignValueObject arrayDesignValueObject ) {
        Long id = arrayDesignValueObject.getId();
        return this.load( id );
    }

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
    @Override
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleCompositeSequenceWithoutBioSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.compositeSequenceWithoutBlatResults"
                            + "(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutGenes(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDao.compositeSequenceWithoutGenes"
                            + "(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#countAll()
     */
    @Override
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
    @Override
    public java.util.Collection<? extends ArrayDesign> create(
            final java.util.Collection<? extends ArrayDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ArrayDesign.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<ArrayDesign>() {
                    @Override
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
    @Override
    public ArrayDesign create( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.create - 'arrayDesign' can not be null" );
        }
        this.getHibernateTemplate().saveOrUpdate( arrayDesign );
        return arrayDesign;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void deleteAlignmentData( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleDeleteAlignmentData( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void deleteGeneProductAssociations( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleDeleteGeneProductAssociations( arrayDesign );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign find( ArrayDesign arrayDesign ) {
        StopWatch timer = new StopWatch();
        timer.start();
        BusinessKey.checkValidKey( arrayDesign );
        Criteria queryObject = super.getSession().createCriteria( ArrayDesign.class );
        BusinessKey.addRestrictions( queryObject, arrayDesign );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                debug( results );
                throw new InvalidDataAccessResourceUsageException( results.size() + " " + ArrayDesign.class.getName()
                        + "s were found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }

        if ( timer.getTime() > 200 ) {
            log.warn( "Slow find: " + timer.getTime() + "ms" );
        }
        return ( ArrayDesign ) result;

    }

    public ArrayDesign find( final java.lang.String queryString,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( arrayDesign );
        argNames.add( "arrayDesign" );
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet<ArrayDesign>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByAlternateName(java.lang.String)
     */
    @Override
    public java.util.Collection<ArrayDesign> findByAlternateName( final java.lang.String queryString ) {

        return this.handleFindByAlternateName( queryString );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByManufacturer(java.lang.String)
     */
    @Override
    public Collection<ArrayDesign> findByManufacturer( String queryString ) {
        if ( StringUtils.isBlank( queryString ) ) {
            return new HashSet<ArrayDesign>();
        }
        return this.getHibernateTemplate().find(
                "select ad from ArrayDesignImpl ad inner join ad.designProvider n where n.name like ?",
                queryString + "%" );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByName(int, java.lang.String)
     */
    @Override
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
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet<ArrayDesign>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
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
    @Override
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
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet<ArrayDesign>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
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

    @Override
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from ArrayDesignImpl a where a.primaryTaxon = :t", "t", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(ubic.gemma.model.expression.arrayDesign.
     * ArrayDesign)
     */
    @Override
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign ) {
        ArrayDesign existingArrayDesign = this.find( arrayDesign );
        if ( existingArrayDesign != null ) {
            assert existingArrayDesign.getId() != null;
            return existingArrayDesign;
        }
        log.debug( "Creating new arrayDesign: " + arrayDesign.getName() );
        return create( arrayDesign );
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
        java.util.Set<ArrayDesign> results = new java.util.LinkedHashSet<ArrayDesign>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getAllAssociatedBioAssays(java.lang.Long)
     */
    @Override
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( final java.lang.Long id ) {

        return this.handleGetAllAssociatedBioAssays( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getAuditEvents(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Collection<AuditEvent>> getAuditEvents( final java.util.Collection<Long> ids ) {

        return this.handleGetAuditEvents( ids );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getBioSequences(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign)
     */
    @Override
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {

        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException( "Cannot fetch sequences for a non-persistent array design" );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        // have to include ad in the select to be able to use fetch join
        List<?> r = getHibernateTemplate().findByNamedParam(
                "select ad from ArrayDesignImpl ad inner join fetch ad.compositeSequences cs "
                        + "left outer join fetch cs.biologicalCharacteristic bs where ad = :ad", "ad", arrayDesign );
        Map<CompositeSequence, BioSequence> result = new HashMap<CompositeSequence, BioSequence>();

        for ( CompositeSequence cs : ( ( ArrayDesign ) r.get( 0 ) ).getCompositeSequences() ) {
            result.put( cs, cs.getBiologicalCharacteristic() );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Fetch sequences: " + timer.getTime() + "ms" );
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleGetExpressionExperiments( arrayDesign );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getPerTaxonCount()
     */
    @Override
    public Map<Taxon, Integer> getPerTaxonCount() {
        Map<Taxon, Integer> result = new HashMap<Taxon, Integer>();

        final String csString = "select t, count(ad) from ArrayDesignImpl ad inner join ad.primaryTaxon t group by t ";
        org.hibernate.Query csQueryObject = super.getSession().createQuery( csString );
        csQueryObject.setReadOnly( true );
        csQueryObject.setCacheable( true );

        List<?> csList = csQueryObject.list();

        Taxon t = null;
        for ( Object object : csList ) {
            Object[] oa = ( Object[] ) object;
            t = ( Taxon ) oa[0];
            Long count = ( Long ) oa[1];

            result.put( t, count.intValue() );

        }

        return result;

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getTaxa(java.lang.Long)
     */
    @Override
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( final java.lang.Long id ) {

        return this.handleGetTaxa( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getTaxon(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long id ) {

        return this.handleGetTaxon( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isMerged(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isMerged( final java.util.Collection<Long> ids ) {

        return this.handleIsMerged( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isMergee(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isMergee( final java.util.Collection<Long> ids ) {

        return this.handleIsMergee( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isSubsumed(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isSubsumed( final java.util.Collection<Long> ids ) {

        return this.handleIsSubsumed( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#isSubsumer(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isSubsumer( final java.util.Collection<Long> ids ) {

        return this.handleIsSubsumer( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#load(int, java.lang.Long)
     */
    @Override
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
    @Override
    public java.util.Collection<ArrayDesign> load( final java.util.Collection<Long> ids ) {

        return this.handleLoadMultiple( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends ArrayDesign> loadAll() {
        return this.getHibernateTemplate().loadAll( ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl.class );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadAllValueObjects()
     */
    @Override
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects() {

        return this.handleLoadAllValueObjects();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadCompositeSequences(java.lang.Long)
     */
    @Override
    public java.util.Collection<CompositeSequence> loadCompositeSequences( final java.lang.Long id ) {

        return this.handleLoadCompositeSequences( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#loadValueObjects(java.util.Collection)
     */
    @Override
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( final java.util.Collection<Long> ids ) {

        return this.handleLoadValueObjects( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBioSequences()
     */
    @Override
    public long numAllCompositeSequenceWithBioSequences() {

        return this.handleNumAllCompositeSequenceWithBioSequences();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    @Override
    public long numAllCompositeSequenceWithBioSequences( final java.util.Collection<Long> ids ) {

        return this.handleNumAllCompositeSequenceWithBioSequences( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBlatResults()
     */
    @Override
    public long numAllCompositeSequenceWithBlatResults() {

        return this.handleNumAllCompositeSequenceWithBlatResults();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    @Override
    public long numAllCompositeSequenceWithBlatResults( final java.util.Collection<Long> ids ) {

        return this.handleNumAllCompositeSequenceWithBlatResults( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithGenes()
     */
    @Override
    public long numAllCompositeSequenceWithGenes() {

        return this.handleNumAllCompositeSequenceWithGenes();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllCompositeSequenceWithGenes(java.util.Collection)
     */
    @Override
    public long numAllCompositeSequenceWithGenes( final java.util.Collection<Long> ids ) {

        return this.handleNumAllCompositeSequenceWithGenes( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllGenes()
     */
    @Override
    public long numAllGenes() {

        return this.handleNumAllGenes();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numAllGenes(java.util.Collection)
     */
    @Override
    public long numAllGenes( final java.util.Collection<Long> ids ) {

        return this.handleNumAllGenes( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numBioSequences( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumBioSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numBlatResults( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumBlatResults( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequences(java.lang.Long)
     */
    @Override
    public java.lang.Integer numCompositeSequences( final java.lang.Long id ) {

        return this.handleNumCompositeSequences( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithBioSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithBlatResults( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithGenes( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumGenes( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#remove(java.lang.Long)
     */
    @Override
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
    @Override
    public void remove( java.util.Collection<? extends ArrayDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'arrayDesign' can not be null" );
        }

        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( arrayDesign );
                Hibernate.initialize( arrayDesign.getMergees() );
                Hibernate.initialize( arrayDesign.getSubsumedArrayDesigns() );
                arrayDesign.getMergees().clear();
                arrayDesign.getSubsumedArrayDesigns().clear();
                return null;
            }
        } );

        this.getHibernateTemplate().delete( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void removeBiologicalCharacteristics( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleRemoveBiologicalCharacteristics( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign thaw( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleThaw( arrayDesign );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign
     * )
     */
    @Override
    public ArrayDesign thawLite( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "array design cannot be null" );
        }
        List<?> res = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from ArrayDesignImpl a left join fetch a.subsumedArrayDesigns "
                                + " left join fetch a.mergees left join fetch a.designProvider left join fetch a.primaryTaxon "
                                + " join fetch a.auditTrail trail join fetch trail.events join fetch a.status left join fetch a.externalReferences"
                                + " left join fetch a.subsumingArrayDesign left join fetch a.mergedInto where a.id=:adid",
                        "adid", arrayDesign.getId() );

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No array design with id=" + arrayDesign.getId() + " could be loaded." );
        }
        ArrayDesign result = ( ArrayDesign ) res.get( 0 );

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#thawLite(java.util.Collection)
     */
    @Override
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        if ( arrayDesigns.isEmpty() ) return arrayDesigns;
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from ArrayDesignImpl a "
                                + "left join fetch a.subsumedArrayDesigns "
                                + " left join fetch a.mergees left join fetch a.designProvider left join fetch a.primaryTaxon "
                                + " join fetch a.auditTrail trail join fetch trail.events join fetch a.status left join fetch a.externalReferences"
                                + " left join fetch a.subsumedArrayDesigns left join fetch a.subsumingArrayDesign "
                                + " left join fetch a.mergedInto where a.id in (:adids)", "adids",
                        EntityUtils.getIds( arrayDesigns ) );

    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends ArrayDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ArrayDesign.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<ArrayDesign>() {
                    @Override
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
    @Override
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
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBioSequences(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) {
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic bs where ar = :ar and bs IS NULL";
        return getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBlatResults(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String nativeQueryString = "SELECT distinct cs.id from "
                + "COMPOSITE_SEQUENCE cs left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on bs2gp.BIO_SEQUENCE_FK=cs.BIOLOGICAL_CHARACTERISTIC_FK "
                + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssResult on bs2gp.BLAT_RESULT_FK=ssResult.ID "
                + "WHERE ssResult.ID is NULL AND ARRAY_DESIGN_FK = :id ";

        // final String queryString = "select distinct cs id from CompositeSequenceImpl cs, BlatAssociationImpl bs2gp
        // inner join bs2gp.blatResult";

        return ( Collection<CompositeSequence> ) NativeQueryUtils.findByNamedParam( this.getHibernateTemplate(),
                nativeQueryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutGenes(ubic.gemma.model
     * .expression.arrayDesign.ArrayDesign)
     */
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();

        final String nativeQueryString = "SELECT distinct cs.id from "
                + "COMPOSITE_SEQUENCE cs left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
                + "left join CHROMOSOME_FEATURE geneProduct on (geneProduct.ID=bs2gp.GENE_PRODUCT_FK AND geneProduct.class='GeneProductImpl') "
                + "left join CHROMOSOME_FEATURE gene on geneProduct.GENE_FK=gene.ID  "
                + "WHERE gene.ID IS NULL AND ARRAY_DESIGN_FK = :id";
        return ( Collection<CompositeSequence> ) NativeQueryUtils.findByNamedParam( this.getHibernateTemplate(),
                nativeQueryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCountAll()
     */
    protected Integer handleCountAll() {
        final String queryString = "select count(*) from ArrayDesignImpl";
        return ( ( Long ) getHibernateTemplate().find( queryString ).iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleDeleteAlignmentData(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign)
     */
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) {
        // First have to delete all blatAssociations, because they are referred to by the alignments
        deleteGeneProductAssociations( arrayDesign );

        // Note attempts to do this with bulk updates were unsuccessful due to the need for joins.
        final String queryString = "select br from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResultImpl br "
                + "where br.querySequence = bs and ad=:arrayDesign";
        getHibernateTemplate().deleteAll(
                getHibernateTemplate().findByNamedParam( queryString, "arrayDesign", arrayDesign ) );

        log.info( "Done deleting  BlatResults for " + arrayDesign );

    }

    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        final String queryString = "select ba from CompositeSequenceImpl  cs "
                + "inner join cs.biologicalCharacteristic bs, BioSequence2GeneProductImpl ba "
                + "where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";
        List<?> blatAssociations = getHibernateTemplate().findByNamedParam( queryString, "arrayDesign", arrayDesign );
        if ( !blatAssociations.isEmpty() ) {
            getHibernateTemplate().deleteAll( blatAssociations );
            log.info( "Done deleting " + blatAssociations.size() + "  BlatAssociations for " + arrayDesign );
        }

        final String annotationAssociationQueryString = "select ba from CompositeSequenceImpl cs "
                + " inner join cs.biologicalCharacteristic bs, AnnotationAssociationImpl ba "
                + " where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";
        List<?> annotAssociations = getHibernateTemplate().findByNamedParam( annotationAssociationQueryString,
                "arrayDesign", arrayDesign );

        if ( !annotAssociations.isEmpty() ) {
            getHibernateTemplate().deleteAll( annotAssociations );
            log.info( "Done deleting " + annotAssociations.size() + " AnnotationAssociations for " + arrayDesign );
        }
    }

    protected Collection<ArrayDesign> handleFindByAlternateName( String queryString ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ad from ArrayDesignImpl ad inner join ad.alternateNames n where n.name = :q", "q", queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    protected Collection<BioAssay> handleGetAllAssociatedBioAssays( Long id ) {
        final String queryString = "select b from BioAssayImpl as b inner join b.arrayDesignUsed a where a.id = :id";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetAuditEvents(java.util.Collection)
     */
    protected Map<Long, Collection<AuditEvent>> handleGetAuditEvents( Collection<Long> ids ) {
        final String queryString = "select ad.id, auditEvent from ArrayDesignImpl ad"
                + " join ad.auditTrail as auditTrail join auditTrail.events as auditEvent join fetch auditEvent.performer "
                + " where ad.id in (:ids) ";

        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<Long, Collection<AuditEvent>>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            AuditEvent event = ( AuditEvent ) o[1];

            if ( eventMap.containsKey( id ) ) {
                Collection<AuditEvent> events = eventMap.get( id );
                events.add( event );
            } else {
                Collection<AuditEvent> events = new ArrayList<AuditEvent>();
                events.add( event );
                eventMap.put( id, events );
            }
        }
        // add in the array design ids that do not have events. Set their values to null.
        for ( Object object : ids ) {
            Long id = ( Long ) object;
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetExpressionExperimentsById(long)
     */
    protected Collection<ExpressionExperiment> handleGetExpressionExperiments( ArrayDesign arrayDesign ) {
        final String queryString = "select distinct ee from ArrayDesignImpl ad, "
                + "BioAssayImpl ba, ExpressionExperimentImpl ee inner join ee.bioAssays eeba where"
                + " ba.arrayDesignUsed=ad and eeba=ba and ad = :ad";
        return getHibernateTemplate().findByNamedParam( queryString, "ad", arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetTaxon(java.lang.Long)
     */
    protected Collection<Taxon> handleGetTaxa( Long id ) {

        final String queryString = "select distinct t from ArrayDesignImpl as arrayD "
                + "inner join arrayD.compositeSequences as cs inner join " + "cs.biologicalCharacteristic as bioC"
                + " inner join bioC.taxon t where arrayD.id = :id";

        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetTaxon(java.lang.Long)
     */
    protected Taxon handleGetTaxon( Long id ) {
        Collection<Taxon> taxon = handleGetTaxa( id );
        if ( taxon.size() == 0 ) {
            log.warn( "No taxon found for array " + id );
            return null; // printwarning
        }

        if ( taxon.size() > 1 ) {
            log.warn( taxon.size() + " taxon found for array " + id );
        }
        return taxon.iterator().next();
    }

    protected Map<Long, Boolean> handleIsMerged( Collection<Long> ids ) {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad left join ad.mergees subs where ad.id in (:ids) group by ad";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long mergeeCount = ( Long ) o[1];
            if ( mergeeCount != null && mergeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    protected Map<Long, Boolean> handleIsMergee( final Collection<Long> ids ) {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, ad.mergedInto from ArrayDesignImpl as ad where ad.id in (:ids) ";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign merger = ( ArrayDesign ) o[1];
            if ( merger != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    protected Map<Long, Boolean> handleIsSubsumed( final Collection<Long> ids ) {
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, ad.subsumingArrayDesign from ArrayDesignImpl as ad where ad.id in (:ids) ";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign subsumer = ( ArrayDesign ) o[1];
            if ( subsumer != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    protected Map<Long, Boolean> handleIsSubsumer( Collection<Long> ids ) {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long subsumeeCount = ( Long ) o[1];
            if ( subsumeeCount != null && subsumeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    protected Collection<ArrayDesignValueObject> handleLoadAllValueObjects() {

        Map<Long, Integer> eeCounts = this.getExpressionExperimentCountMap();

        final String queryString = "select ad.id, ad.name, ad.shortName, "
                + "ad.technologyType, ad.description,s.createDate, m, s.troubled, s.validated, t.commonName"
                + " from ArrayDesignImpl ad join ad.status s join ad.primaryTaxon t  left join ad.mergedInto m";

        Query queryObject = super.getSession().createQuery( queryString );

        Collection<ArrayDesignValueObject> result = processADValueObjectQueryResults( eeCounts, queryObject );

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    protected Collection<CompositeSequence> handleLoadCompositeSequences( Long id ) {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadMultiple(java.util.Collection)
     */
    protected Collection<ArrayDesign> handleLoadMultiple( Collection<Long> ids ) {
        if ( ids == null || ids.isEmpty() ) return new HashSet<ArrayDesign>();
        final String queryString = "select ad from ArrayDesignImpl as ad where ad.id in (:ids) ";
        return getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadValueObjects(java.util.Collection)
     */
    protected Collection<ArrayDesignValueObject> handleLoadValueObjects( Collection<Long> ids ) {
        // sanity check
        if ( ids == null || ids.size() == 0 ) {
            return new ArrayList<ArrayDesignValueObject>();
        }

        Map<Long, Integer> eeCounts = this.getExpressionExperimentCountMap( ids );

        final String queryString = "select ad.id, ad.name, ad.shortName, "
                + "ad.technologyType, ad.description,s.createDate, m, s.troubled, s.validated, t.commonName"
                + " from ArrayDesignImpl ad join ad.status s join ad.primaryTaxon t left join ad.mergedInto m where ad.id in (:ids)  ";

        Query queryObject = super.getSession().createQuery( queryString );
        queryObject.setParameterList( "ids", ids );

        return processADValueObjectQueryResults( eeCounts, queryObject );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences()
     */
    protected long handleNumAllCompositeSequenceWithBioSequences() {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences(java
     * .util.Collection)
     */
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar.id in (:ids) and cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults()
     */
    protected long handleNumAllCompositeSequenceWithBlatResults() {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults(java.
     * util.Collection)
     */
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BlatResultImpl as blat where blat.querySequence= and ar.id in (:ids)";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes()
     */
    protected long handleNumAllCompositeSequenceWithGenes() {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and bs2gp.geneProduct=gp";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes(java.util.
     * Collection)
     */
    protected long handleNumAllCompositeSequenceWithGenes( Collection<Long> ids ) {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp"
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct=gp and ar.id in (:ids)";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllGenes()
     */
    protected long handleNumAllGenes() {
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and  bs2gp.geneProduct=gp";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    protected long handleNumAllGenes( Collection<Long> ids ) {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct=gp  and ar.id in (:ids)";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    protected long handleNumBioSequences( ArrayDesign arrayDesign ) {
        final String queryString = "select count (distinct cs.biologicalCharacteristic) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    protected long handleNumBlatResults( ArrayDesign arrayDesign ) {
        final String queryString = "select count (distinct bs2gp) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl as bs2gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    protected Integer handleNumCompositeSequences( Long id ) {
        final String queryString = "select count (*) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return ( ( Long ) getHibernateTemplate().findByNamedParam( queryString, "id", id ).iterator().next() )
                .intValue();
    }

    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) {
        final String queryString = "select count (distinct cs) from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    protected long handleNumGenes( ArrayDesign arrayDesign ) {
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    protected void handleRemoveBiologicalCharacteristics( final ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Array design cannot be null" );
        }
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( arrayDesign );
                int count = 0;
                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                    cs.setBiologicalCharacteristic( null );
                    session.update( cs );
                    session.evict( cs );
                    if ( ++count % LOGGING_UPDATE_EVENT_COUNT == 0 ) {
                        log.info( "Cleared sequence association for " + count + " composite sequences" );
                    }
                }

                return null;
            }
        } );
    }

    protected ArrayDesign handleThaw( final ArrayDesign arrayDesign ) {
        return this.doThaw( arrayDesign );
    }

    protected Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {

        // Size does not automatically disqualify, because we only consider BioSequences that actually have
        // sequences in them.
        if ( candidateSubsumee.getCompositeSequences().size() > candidateSubsumer.getCompositeSequences().size() ) {
            log.info( "Subsumee has more sequences than subsumer so probably cannot be subsumed ... checking anyway" );
        }

        Collection<BioSequence> subsumerSeqs = new HashSet<BioSequence>();
        Collection<BioSequence> subsumeeSeqs = new HashSet<BioSequence>();

        for ( CompositeSequence cs : candidateSubsumee.getCompositeSequences() ) {
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq == null ) continue;
            subsumeeSeqs.add( seq );
        }

        for ( CompositeSequence cs : candidateSubsumer.getCompositeSequences() ) {
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq == null ) continue;
            subsumerSeqs.add( seq );
        }

        if ( subsumeeSeqs.size() > subsumerSeqs.size() ) {
            log.info( "Subsumee has more sequences than subsumer so probably cannot be subsumed, checking overlap" );
        }

        int overlap = 0;
        List<BioSequence> missing = new ArrayList<BioSequence>();
        for ( BioSequence sequence : subsumeeSeqs ) {
            if ( subsumerSeqs.contains( sequence ) ) {
                overlap++;
            } else {
                missing.add( sequence );
            }
        }

        log.info( "Subsumer " + candidateSubsumer + " contains " + overlap + "/" + subsumeeSeqs.size()
                + " biosequences from the subsumee " + candidateSubsumee );

        if ( overlap != subsumeeSeqs.size() ) {
            int n = 50;
            System.err.println( "Up to " + n + " missing sequences will be listed." );
            for ( int i = 0; i < Math.min( n, missing.size() ); i++ ) {
                System.err.println( missing.get( i ) );
            }
            return false;
        }

        // if we got this far, then we definitely have a subsuming situtation.
        if ( candidateSubsumee.getCompositeSequences().size() == candidateSubsumer.getCompositeSequences().size() ) {
            log.info( candidateSubsumee + " and " + candidateSubsumer + " are apparently exactly equivalent" );
        } else {
            log.info( candidateSubsumer + " subsumes " + candidateSubsumee );
        }
        candidateSubsumer.getSubsumedArrayDesigns().add( candidateSubsumee );
        candidateSubsumee.setSubsumingArrayDesign( candidateSubsumer );
        this.update( candidateSubsumer );
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();
        this.update( candidateSubsumee );
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();

        return true;
    }

    // /**
    // * @see
    // ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
    // */
    // public void remove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
    // if ( arrayDesign == null ) {
    // throw new IllegalArgumentException( "ArrayDesign.remove - 'arrayDesign' can not be null" );
    // }
    // this.getHibernateTemplate().delete( arrayDesign );
    // }

    /**
     * 
     */
    private void debug( List<? extends Object> results ) {
        for ( Object ad : results ) {
            log.error( ad );
        }
    }

    /**
     * @param arrayDesign @
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign doThaw( ArrayDesign arrayDesign ) {

        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException( "Cannot thaw a non-persistent array design" );
        }

        /*
         * Thaw basic stuff
         */
        StopWatch timer = new StopWatch();
        timer.start();

        ArrayDesign result = thawLite( arrayDesign );

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw array design stage 1: " + timer.getTime() + "ms" );
        }

        timer.stop();
        timer.reset();
        timer.start();

        /*
         * Thaw the composite sequences.
         */
        log.info( "Start initialize composite sequences" );

        Hibernate.initialize( result.getCompositeSequences() );

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw array design stage 2: " + timer.getTime() + "ms" );
        }
        timer.stop();
        timer.reset();
        timer.start();

        /*
         * Thaw the biosequences in batches
         */
        Collection<CompositeSequence> thawed = new HashSet<CompositeSequence>();
        Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
        long lastTime = timer.getTime();
        for ( CompositeSequence cs : result.getCompositeSequences() ) {
            batch.add( cs );
            if ( batch.size() == 1000 ) {
                long t = timer.getTime();
                if ( t > 10000 && t - lastTime > 1000 ) {
                    log.info( "Thaw Batch : " + t );
                }
                List<?> bb = thawBatchOfProbes( batch );
                thawed.addAll( ( Collection<? extends CompositeSequence> ) bb );
                lastTime = timer.getTime();
                batch.clear();
            }
            this.getSession().evict( cs );
        }

        if ( !batch.isEmpty() ) { // tail end
            List<?> bb = thawBatchOfProbes( batch );
            thawed.addAll( ( Collection<? extends CompositeSequence> ) bb );
        }

        result.getCompositeSequences().clear();
        result.getCompositeSequences().addAll( thawed );

        /*
         * This is a bit ugly, but necessary to avoid 'dirty collection' errors later.
         */
        if ( result.getCompositeSequences() instanceof PersistentCollection )
            ( ( PersistentCollection ) result.getCompositeSequences() ).clearDirty();

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw array design stage 3: " + timer.getTime() );
        }

        return result;
    }

    /**
     * Gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    private Map<Long, Integer> getExpressionExperimentCountMap() {
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperimentImpl ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad group by ad";

        Map<Long, Integer> eeCount = new HashMap<Long, Integer>();
        List<Object[]> list = getHibernateTemplate().find( queryString );

        // Bug 1549: for unknown reasons, this method sometimes returns only a single record (or no records). Obviously
        // if we only have 1 array design this warning is spurious.
        if ( list.size() < 2 ) log.warn( list.size() + " rows from getExpressionExperimentCountMap query" );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Integer count = ( ( Long ) o[1] ).intValue();
            eeCount.put( id, count );
        }

        return eeCount;
    }

    /**
     * Gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    private Map<Long, Integer> getExpressionExperimentCountMap( Collection<Long> arrayDesignIds ) {

        Map<Long, Integer> result = new HashMap<Long, Integer>();

        if ( arrayDesignIds == null || arrayDesignIds.isEmpty() ) {
            return result;
        }
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperimentImpl ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad  where ad.id in (:ids) group by ad ";

        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", arrayDesignIds );

        // Bug 1549: for unknown reasons, this method sometimes returns only a single record (or no records)
        if ( arrayDesignIds.size() > 1 && list.size() != arrayDesignIds.size() ) {
            log.info( list.size() + " rows from getExpressionExperimentCountMap query for " + arrayDesignIds.size()
                    + " ids" );
        }

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Integer count = ( ( Long ) o[1] ).intValue();
            result.put( id, count );
        }

        return result;
    }

    /**
     * Process query results for handleLoadAllValueObjects or handleLoadValueObjects
     * 
     * @param eeCounts
     * @param queryString
     * @param arrayToTaxon
     * @return
     */
    private Collection<ArrayDesignValueObject> processADValueObjectQueryResults( Map<Long, Integer> eeCounts,
            final Query queryObject ) {
        Collection<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        queryObject.setCacheable( true );
        ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( list != null ) {
            while ( list.next() ) {
                ArrayDesignValueObject v = new ArrayDesignValueObject();
                v.setId( list.getLong( 0 ) );
                v.setName( list.getString( 1 ) );
                v.setShortName( list.getString( 2 ) );

                TechnologyType color = ( TechnologyType ) list.get( 3 );
                if ( color != null ) {
                    v.setTechnologyType( color.toString() );
                    v.setColor( color.getValue() );
                }

                v.setDescription( list.getString( 4 ) );

                v.setDateCreated( list.getDate( 5 ) );

                v.setIsMergee( list.get( 6 ) != null );

                v.setTroubled( list.getBoolean( 7 ) );

                v.setValidated( list.getBoolean( 8 ) );

                v.setTaxon( list.getString( 9 ) );

                if ( !eeCounts.containsKey( v.getId() ) ) {
                    v.setExpressionExperimentCount( 0L );
                } else {
                    v.setExpressionExperimentCount( eeCounts.get( v.getId() ).longValue() );
                }

                result.add( v );
            }
        }
        return result;
    }

    private List<?> thawBatchOfProbes( Collection<CompositeSequence> batch ) {
        List<?> bb = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select cs from CompositeSequenceImpl cs left join fetch cs.biologicalCharacteristic where cs in (:batch)",
                        "batch", batch );
        return bb;
    }

}