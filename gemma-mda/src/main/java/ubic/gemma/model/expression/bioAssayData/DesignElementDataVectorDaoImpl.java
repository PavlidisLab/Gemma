/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.CommonQueries;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 * @author pavlidis
 * @version $Id$
 */
public class DesignElementDataVectorDaoImpl extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase {

    private static Log log = LogFactory.getLog( DesignElementDataVectorDaoImpl.class.getName() );

    @Override
    public Collection find( ArrayDesign arrayDesign, QuantitationType quantitationType ) {
        final String queryString = "select dev from RawExpressionDataVectorImpl dev  inner join fetch dev.bioAssayDimension bd "
                + " inner join fetch dev.designElement de inner join fetch dev.quantitationType where dev.designElement in (:desEls) "
                + "and dev.quantitationType = :quantitationType ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "quantitationType", quantitationType );
            queryObject.setParameterList( "desEls", arrayDesign.getCompositeSequences() );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector find( DesignElementDataVector designElementDataVector ) {

        BusinessKey.checkKey( designElementDataVector );

        DetachedCriteria crit = DetachedCriteria.forClass( DesignElementDataVector.class );

        crit.createCriteria( "designElement" ).add(
                Restrictions.eq( "name", designElementDataVector.getDesignElement().getName() ) ).createCriteria(
                "arrayDesign" ).add(
                Restrictions.eq( "name", designElementDataVector.getDesignElement().getArrayDesign().getName() ) );

        crit.createCriteria( "quantitationType" ).add(
                Restrictions.eq( "name", designElementDataVector.getQuantitationType().getName() ) );

        crit.createCriteria( "expressionExperiment" ).add(
                Restrictions.eq( "name", designElementDataVector.getExpressionExperiment().getName() ) );

        List results = this.getHibernateTemplate().findByCriteria( crit );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + DesignElementDataVector.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( DesignElementDataVector ) result;

    }

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector findOrCreate( DesignElementDataVector designElementDataVector ) {

        DesignElementDataVector existing = find( designElementDataVector );
        if ( existing != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing designElementDataVector: " + existing );
            return existing;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new designElementDataVector: " + designElementDataVector );
        return ( DesignElementDataVector ) create( designElementDataVector );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from DesignElementDataVectorImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleGetVectors(java.util.Collection,
     *      java.util.Collection)
     */
    @Override
    protected Map handleGetPreferredVectors( Collection ees, Collection genes ) throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();

        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        watch.stop();

        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<DesignElementDataVector, Collection<Gene>>();
        }

        log.info( "Got " + cs2gene.keySet().size() + " composite sequences for " + genes.size() + " genes in "
                + watch.getTime() + "ms" );
        watch.reset();

        // Second, get designElementDataVectors for each compositeSequence and then fill the dedv2genes
        watch.start();
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = getPreferredVectorsForProbes( ees, cs2gene );

        watch.stop();
        log.info( "Got " + dedv2genes.keySet().size() + " DEDV for " + cs2gene.keySet().size()
                + " composite sequences in " + watch.getTime() + "ms" );

        return dedv2genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleRemoveDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    protected void handleRemoveDataForCompositeSequence( final CompositeSequence compositeSequence ) throws Exception {
        // rarely used.
        String[] probeCoexpTypes = new String[] { "Mouse", "Human", "Rat", "Other" };

        for ( String type : probeCoexpTypes ) {

            final String dedvRemovalQuery = "delete dedv from RawExpressionDataVectorImpl dedv where dedv.designElement = ?";

            final String ppcRemoveFirstQuery = "delete d from " + type
                    + "ProbeCoExpressionImpl as p inner join p.firstVector d where d.designElement = ?";
            final String ppcRemoveSecondQuery = "delete d from " + type
                    + "ProbeCoExpressionImpl as p inner join p.secondVector d where d.designElement = ?";

            int deleted = getHibernateTemplate().bulkUpdate( ppcRemoveFirstQuery, compositeSequence );
            deleted += getHibernateTemplate().bulkUpdate( ppcRemoveSecondQuery, compositeSequence );
            getHibernateTemplate().bulkUpdate( dedvRemovalQuery, compositeSequence );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleRemoveDataFromQuantitationType(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected void handleRemoveDataForQuantitationType( final QuantitationType quantitationType ) throws Exception {
        final String dedvRemovalQuery = "delete from RawExpressionDataVectorImpl as dedv where dedv.quantitationType = ?";
        int deleted = getHibernateTemplate().bulkUpdate( dedvRemovalQuery, quantitationType );
        log.info( "Deleted " + deleted + " data vector elements" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleThaw(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleThaw( final Collection designElementDataVectors ) throws Exception {

        HibernateTemplate templ = this.getHibernateTemplate();
        templ.setFetchSize( 400 );
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            @SuppressWarnings("unchecked")
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {

                FlushMode oldFlushMode = session.getFlushMode();
                CacheMode oldCacheMode = session.getCacheMode();
                session.setCacheMode( CacheMode.IGNORE ); // Don't hit the secondary cache
                session.setFlushMode( FlushMode.MANUAL ); // We're READ-ONLY so this is okay.
                int count = 0;
                StopWatch timer = new StopWatch();
                timer.start();
                Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
                Collection<DesignElement> cs = new HashSet<DesignElement>();
                for ( DesignElementDataVector object : ( Collection<DesignElementDataVector> ) designElementDataVectors ) {
                    session.lock( object, LockMode.NONE );
                    Hibernate.initialize( object );
                    Hibernate.initialize( object.getExpressionExperiment() );
                    dims.add( object.getBioAssayDimension() );
                    cs.add( object.getDesignElement() );
                    session.evict( object.getQuantitationType() );
                    session.evict( object );
                }

                // thaw the bioassaydimensions we saw
                for ( BioAssayDimension bad : dims ) {
                    Hibernate.initialize( bad );
                    for ( BioAssay ba : bad.getBioAssays() ) {
                        session.lock( ba, LockMode.NONE );
                        Hibernate.initialize( ba );
                        Hibernate.initialize( ba.getArrayDesignUsed() );
                        Hibernate.initialize( ba.getDerivedDataFiles() );
                        Hibernate.initialize( ba.getSamplesUsed() );

                        for ( BioMaterial bm : ba.getSamplesUsed() ) {
                            session.lock( bm, LockMode.NONE );
                            Hibernate.initialize( bm );
                            Hibernate.initialize( bm.getBioAssaysUsedIn() );
                            Hibernate.initialize( bm.getFactorValues() );
                            session.evict( bm );
                        }
                        session.evict( ba );
                        session.clear(); // this is necessary to avoid session errors (due to multiple bioassays per
                        // biomaterial?)
                    }
                }

                // thaw the designelements we saw.
                for ( DesignElement de : cs ) {
                    BioSequence seq = ( ( CompositeSequence ) de ).getBiologicalCharacteristic();
                    if ( seq == null ) continue;
                    session.lock( seq, LockMode.NONE );
                    // Note that these steps are not done in arrayDesign.thawLite; we're assuming this information is
                    // needed if you are thawing dedvs. That might not be true in all cases.
                    Hibernate.initialize( seq );
                    ArrayDesign arrayDesign = ( ( CompositeSequence ) de ).getArrayDesign();
                    Hibernate.initialize( arrayDesign );

                    if ( ++count % 10000 == 0 ) {
                        timer.split();
                        if ( timer.getSplitTime() > 1000 ) {
                            log.info( "Thawed " + count + " vector-associated probes " + timer.getSplitTime() + " ms" );
                        }
                        timer.unsplit();
                    }
                }

                timer.stop();
                if ( designElementDataVectors.size() >= 2000 || timer.getTime() > 2000 )
                    log.info( "Done, thawed " + designElementDataVectors.size() + " vectors in " + timer.getTime()
                            + "ms" );
                session.setFlushMode( oldFlushMode );
                session.setCacheMode( oldCacheMode );
                return null;
            }

        }, false );

    }

    @Override
    protected void handleThaw( final DesignElementDataVector designElementDataVector ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                thaw( session, designElementDataVector );
                return null;
            }
        }, true );

    }

    /**
     * @param ees
     * @param cs2gene
     * @return
     */
    protected Map<DesignElementDataVector, Collection<Gene>> getPreferredVectorsForProbes( Collection ees,
            Map<CompositeSequence, Collection<Gene>> cs2gene ) {

        final String queryString;
        if ( ees == null || ees.size() == 0 ) {
            queryString = "select distinct dedv, dedv.designElement from RawExpressionDataVectorImpl dedv "
                    + " inner join fetch dedv.bioAssayDimension bd "
                    + " inner join dedv.designElement de inner join fetch dedv.quantitationType "
                    + " where dedv.designElement in ( :cs ) and dedv.quantitationType.isPreferred = true";
        } else {
            queryString = "select distinct dedv, dedv.designElement from RawExpressionDataVectorImpl dedv"
                    + " inner join fetch dedv.bioAssayDimension bd "
                    + " inner join dedv.designElement de inner join fetch dedv.quantitationType "
                    + " where dedv.designElement in (:cs ) and dedv.quantitationType.isPreferred = true"
                    + " and dedv.expressionExperiment in ( :ees )";
        }
        return getVectorsForProbesInExperiments( ees, cs2gene, queryString );
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Collection<DesignElementDataVector> getPreferredDataVectors( ExpressionExperiment ee ) {
        final String queryString = "select dedv from RawExpressionDataVectorImpl dedv inner join dedv.quantitationType q "
                + " where q.type.isPreferred = true  and dedv.expressionExperiment = :ee ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Collection<DesignElementDataVector> getMissingValueVectors( ExpressionExperiment ee ) {
        final String queryString = "select dedv from RawExpressionDataVectorImpl dedv "
                + "inner join dedv.quantitationType q where q.type = 'PRESENTABSENT'"
                + " and dedv.expressionExperiment  = :ee ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );
    }

    /**
     * @param ees
     * @param cs2gene
     * @param queryString
     * @return
     */
    protected Map<DesignElementDataVector, Collection<Gene>> getVectorsForProbesInExperiments( Collection ees,
            Map<CompositeSequence, Collection<Gene>> cs2gene, final String queryString ) {
        Session session = super.getSession( false );
        org.hibernate.Query queryObject = session.createQuery( queryString );
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = new HashMap<DesignElementDataVector, Collection<Gene>>();
        try {

            if ( ees != null && ees.size() > 0 ) {
                queryObject.setParameterList( "ees", ees );
            }
            queryObject.setParameterList( "cs", cs2gene.keySet() );

            ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

            while ( results.next() ) {
                DesignElementDataVector dedv = ( DesignElementDataVector ) results.get( 0 );
                CompositeSequence cs = ( CompositeSequence ) results.get( 1 );
                Collection<Gene> associatedGenes = cs2gene.get( cs );
                if ( !dedv2genes.containsKey( dedv ) ) {
                    dedv2genes.put( dedv, associatedGenes );
                } else {
                    Collection<Gene> mappedGenes = dedv2genes.get( dedv );
                    mappedGenes.addAll( associatedGenes );
                }
            }
            results.close();
            session.clear();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return dedv2genes;
    }

    /**
     * Thaw a single vector.
     * 
     * @param session
     * @param designElementDataVector
     */
    private void thaw( org.hibernate.Session session, DesignElementDataVector designElementDataVector ) {
        // thaw the design element.
        BioSequence seq = ( ( CompositeSequence ) designElementDataVector.getDesignElement() )
                .getBiologicalCharacteristic();
        if ( seq != null ) {
            session.lock( seq, LockMode.NONE );
            Hibernate.initialize( seq );
        }

        ArrayDesign arrayDesign = ( ( CompositeSequence ) designElementDataVector.getDesignElement() ).getArrayDesign();
        Hibernate.initialize( arrayDesign );
        arrayDesign.hashCode();

        // thaw the bioassays.
        for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
            ba = ( BioAssay ) session.get( BioAssayImpl.class, ba.getId() );
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSamplesUsed() );
            Hibernate.initialize( ba.getDerivedDataFiles() );
        }
    }

}
