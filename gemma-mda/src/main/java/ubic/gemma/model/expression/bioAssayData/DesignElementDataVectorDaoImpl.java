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
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialImpl;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 * @author pavlidis
 * @version $Id$
 */
@Repository
public abstract class DesignElementDataVectorDaoImpl<T extends DesignElementDataVector> extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase<T> {

    private static Log log = LogFactory.getLog( DesignElementDataVectorDaoImpl.class.getName() );

    /**
     * @param ee
     * @param cs2gene Map of probes to genes.
     * @param queryString, which must have parameter list placeholder "cs" and may have parameter list "ees".
     * @return
     */
    protected Map<T, Collection<Long>> getVectorsForProbesInExperiments( Long ee, Map<Long, Collection<Long>> cs2gene,
            final String queryString ) {

        Session session = super.getSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        Map<T, Collection<Long>> dedv2genes = new HashMap<T, Collection<Long>>();
        StopWatch timer = new StopWatch();
        timer.start();

        queryObject.setLong( "eeid", ee );

        /*
         * Might need to adjust this. This value just seems reasonable, but it isn't uncommon for it to be much larger.
         * See bug 1866.
         */int batchSize = 100;
        for ( Collection<Long> batch : new BatchIterator<Long>( cs2gene.keySet(), batchSize ) ) {
            getVectorsBatch( cs2gene, queryObject, dedv2genes, batch );
        }

        if ( timer.getTime() > 50 ) {
            log.info( "Fetched " + dedv2genes.size() + " vectors for " + cs2gene.size() + " probes in "
                    + timer.getTime() + "ms\n" + "Vector query was: "
                    + NativeQueryUtils.toSql( this.getHibernateTemplate(), queryString ) );

        }
        return dedv2genes;
    }

    /**
     * @param cs2gene
     * @param queryString
     * @return
     */
    protected Map<T, Collection<Long>> getVectorsForProbesInExperiments( Map<Long, Collection<Long>> cs2gene,
            final String queryString ) {

        Session session = super.getSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        Map<T, Collection<Long>> dedv2genes = new HashMap<T, Collection<Long>>();
        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * Might need to adjust this. This value just seems reasonable, but it isn't uncommon for it to be much larger.
         * See bug 1866.
         */int batchSize = 100;
        for ( Collection<Long> batch : new BatchIterator<Long>( cs2gene.keySet(), batchSize ) ) {
            getVectorsBatch( cs2gene, queryObject, dedv2genes, batch );
        }

        if ( timer.getTime() > 50 ) {
            log.info( "Fetched " + dedv2genes.size() + " vectors for " + cs2gene.size() + " probes in "
                    + timer.getTime() + "ms\n" + "Vector query was: "
                    + NativeQueryUtils.toSql( this.getHibernateTemplate(), queryString ) );

        }
        return dedv2genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleThaw(java.util.Collection)
     */
    @Override
    protected void handleThaw( Collection<? extends DesignElementDataVector> designElementDataVectors ) {

        if ( designElementDataVectors == null ) return;

        Session session = this.getSessionFactory().getCurrentSession();

        Hibernate.initialize( designElementDataVectors );

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
        Collection<CompositeSequence> cs = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector vector : designElementDataVectors ) {
            session.buildLockRequest( LockOptions.NONE ).lock( vector );
            Hibernate.initialize( vector );
            Hibernate.initialize( vector.getQuantitationType() );
            dims.add( vector.getBioAssayDimension() );
            cs.add( vector.getDesignElement() );
            ees.add( vector.getExpressionExperiment() );
            session.evict( vector.getQuantitationType() );
            session.evict( vector );
        }

        if ( timer.getTime() > designElementDataVectors.size() ) {
            log.info( "Thaw phase 1, " + designElementDataVectors.size() + " vectors initialized in " + timer.getTime()
                    + "ms " );
        }
        timer.reset();
        timer.start();

        // lightly thaw the EEs we saw
        for ( ExpressionExperiment ee : ees ) {
            Hibernate.initialize( ee );
            session.evict( ee );
        }

        if ( timer.getTime() > 100 ) {
            log.info( "Thaw phase 2, " + ees.size() + " vector-associated expression experiments in " + timer.getTime()
                    + "ms " );
        }

        timer.reset();
        timer.start();
        // thaw the bioassaydimensions we saw -- This requires a lot of queries, but there usually aren't very many dims
        // to do. Usually one or more rarely two.
        for ( BioAssayDimension bad : dims ) {
            Hibernate.initialize( bad );
            for ( BioAssay ba : bad.getBioAssays() ) {
                Hibernate.initialize( ba );
                Hibernate.initialize( ba.getSampleUsed() );

                Collection<BioAssay> bioAssaysUsedIn = null;
                BioMaterial bm = ba.getSampleUsed();
                EntityUtils.attach( session, bm, BioMaterialImpl.class, bm.getId() );
                Hibernate.initialize( bm );
                bioAssaysUsedIn = bm.getBioAssaysUsedIn();
                Hibernate.initialize( bioAssaysUsedIn );
                Hibernate.initialize( bm.getFactorValues() );
                session.evict( bm );

                Hibernate.initialize( ba.getArrayDesignUsed() );
                Hibernate.initialize( ba.getDerivedDataFiles() );

                /*
                 * We have to do it this way, or we risk having the bioassay in the session already.
                 */
                if ( bioAssaysUsedIn != null ) {
                    for ( BioAssay baui : bioAssaysUsedIn ) {
                        session.evict( baui );
                    }
                }
            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw phase 3, " + dims.size() + " vector-associated bioassaydimensions in " + timer.getTime()
                    + "ms " );
        }
        timer.reset();
        timer.start();

        // thaw the designelements we saw. SLOW
        long lastTime = 0;
        int count = 0;
        for ( CompositeSequence de : cs ) {
            BioSequence seq = de.getBiologicalCharacteristic();
            if ( seq == null ) continue;
            session.buildLockRequest( LockOptions.NONE ).lock( seq );
            Hibernate.initialize( seq );

            // is this really necessary?
            ArrayDesign arrayDesign = de.getArrayDesign();
            Hibernate.initialize( arrayDesign );

            if ( ++count % 10000 == 0 ) {
                if ( timer.getTime() - lastTime > 1000 ) {
                    log.info( "Thawed " + count + " vector-associated probes " + timer.getTime() + " ms" );
                }
                lastTime = timer.getTime();
            }
        }

        timer.stop();
        if ( designElementDataVectors.size() >= 2000 || timer.getTime() > 200 ) {
            log.info( "Thaw phase 4 " + cs.size() + " vector-associated probes thawed in " + timer.getTime() + "ms" );
        }

    }

    @Override
    protected void handleThaw( T designElementDataVector ) {

        Session session = this.getHibernateTemplate().getSessionFactory().getCurrentSession();

        this.thaw( session, designElementDataVector );
    }

    /**
     * Thaw a single vector.
     * 
     * @param session
     * @param designElementDataVector
     */
    void thaw( org.hibernate.Session session, T designElementDataVector ) {
        // thaw the design element.
        BioSequence seq = designElementDataVector.getDesignElement().getBiologicalCharacteristic();
        if ( seq != null ) {
            session.buildLockRequest( LockOptions.NONE ).lock( seq );
            Hibernate.initialize( seq );
        }

        ArrayDesign arrayDesign = designElementDataVector.getDesignElement().getArrayDesign();
        Hibernate.initialize( arrayDesign );
        arrayDesign.hashCode();

        // thaw the bioassays.
        for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
            ba = ( BioAssay ) session.get( BioAssayImpl.class, ba.getId() );
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
            Hibernate.initialize( ba.getDerivedDataFiles() );
        }
    }

    /**
     * Fetch vectors for a batch of probes.
     * 
     * @param cs2gene (by ID)
     * @param queryObject
     * @param dedv2genes
     * @param batch
     */
    private void getVectorsBatch( Map<Long, Collection<Long>> cs2gene, org.hibernate.Query queryObject,
            Map<T, Collection<Long>> dedv2genes, Collection<Long> batch ) {
        queryObject.setParameterList( "cs", batch );
        queryObject.setFlushMode( FlushMode.MANUAL );
        queryObject.setReadOnly( true );
        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            T dedv = ( T ) results.get( 0 );
            Long cs = ( Long ) results.get( 1 );
            Collection<Long> associatedGenes = cs2gene.get( cs );
            if ( !dedv2genes.containsKey( dedv ) ) {
                dedv2genes.put( dedv, associatedGenes );
            } else {
                Collection<Long> mappedGenes = dedv2genes.get( dedv );
                mappedGenes.addAll( associatedGenes );
            }
        }

        results.close();
    }

}
