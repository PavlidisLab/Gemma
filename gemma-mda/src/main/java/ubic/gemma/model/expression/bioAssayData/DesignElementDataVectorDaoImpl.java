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
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

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
     * @param ees
     * @param cs2gene Map of probes to genes.
     * @param queryString, which must have parameter list placeholder "cs" and may have parameter list "ees".
     * @return
     */
    protected Map<T, Collection<Gene>> getVectorsForProbesInExperiments( Collection<ExpressionExperiment> ees,
            Map<CompositeSequence, Collection<Gene>> cs2gene, final String queryString ) {
        Session session = super.getSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        Map<T, Collection<Gene>> dedv2genes = new HashMap<T, Collection<Gene>>();
        StopWatch timer = new StopWatch();
        timer.start();
        try {

            if ( ees != null && ees.size() > 0 ) {
                queryObject.setParameterList( "ees", ees );
            }

            /*
             * Might need to adjust this. This value just seems reasonable, but it isn't uncommon for it to be much
             * larger. See bug 1866.
             */
            int batchSize = 100;
            Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
            for ( CompositeSequence cs : cs2gene.keySet() ) {
                batch.add( cs );
                if ( batch.size() == batchSize ) {
                    getVectorsBatch( cs2gene, queryObject, dedv2genes, batch );
                    batch.clear();
                }
            }

            if ( batch.size() > 0 ) {
                getVectorsBatch( cs2gene, queryObject, dedv2genes, batch );
            }

            session.clear();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        timer.stop();
        if ( timer.getTime() > 50 ) {
            log.info( "Fetched " + dedv2genes.size() + " vectors for " + cs2gene.size() + " probes in "
                    + ( ees == null ? "(?)" : ees.size() ) + " ees : " + timer.getTime() + "ms" );
        }
        return dedv2genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleThaw(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleThaw( Collection designElementDataVectors ) throws Exception {

        if ( designElementDataVectors == null ) return;

        Session session = this.getSessionFactory().getCurrentSession();

        Hibernate.initialize( designElementDataVectors );

        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
        Collection<CompositeSequence> cs = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector vector : ( Collection<DesignElementDataVector> ) designElementDataVectors ) {
            session.lock( vector, LockMode.NONE );
            Hibernate.initialize( vector );
            Hibernate.initialize( vector.getQuantitationType() );
            BioAssayDimension bioAssayDimension = vector.getBioAssayDimension();

            if ( !dims.contains( bioAssayDimension ) ) {
                Hibernate.initialize( bioAssayDimension );
                Hibernate.initialize( bioAssayDimension.getBioAssays() );
                for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
                    session.lock( ba, LockMode.NONE );
                    Hibernate.initialize( ba );
                    Hibernate.initialize( ba.getSamplesUsed() );
                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
                        session.lock( bm, LockMode.NONE );
                        Hibernate.initialize( bm );
                        Collection<BioAssay> bioAssaysUsedIn = bm.getBioAssaysUsedIn();
                        Hibernate.initialize( bioAssaysUsedIn );
                        Hibernate.initialize( bm.getFactorValues() );
                        session.evict( bm );
                        if ( bioAssaysUsedIn != null ) {
                            for ( BioAssay baui : bioAssaysUsedIn ) {
                                session.evict( baui );
                            }
                        }
                    }
                    session.evict( ba );
                }
                dims.add( bioAssayDimension );
            }

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

        if ( timer.getTime() > 100 ) {
            log.info( "Thaw phase 3, " + dims.size() + " vector-associated bioassaydimensions in " + timer.getTime()
                    + "ms " );
        }
        timer.reset();
        timer.start();

        // thaw the designelements we saw.
        long lastTime = 0;
        for ( CompositeSequence de : cs ) {
            BioSequence seq = de.getBiologicalCharacteristic();
            if ( seq == null ) continue;
            session.lock( seq, LockMode.NONE );
            // Note that these steps are not done in arrayDesign.thawLite; we're assuming this information is
            // needed if you are thawing dedvs. That might not be true in all cases.
            Hibernate.initialize( seq );
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
    protected void handleThaw( T designElementDataVector ) throws Exception {

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
            session.lock( seq, LockMode.NONE );
            Hibernate.initialize( seq );
        }

        ArrayDesign arrayDesign = designElementDataVector.getDesignElement().getArrayDesign();
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

    /**
     * Fetch vectors for a batch of probes.
     * 
     * @param cs2gene
     * @param queryObject
     * @param dedv2genes
     * @param batch
     */
    @SuppressWarnings("unchecked")
    private void getVectorsBatch( Map<CompositeSequence, Collection<Gene>> cs2gene, org.hibernate.Query queryObject,
            Map<T, Collection<Gene>> dedv2genes, Collection<CompositeSequence> batch ) {
        queryObject.setParameterList( "cs", batch );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            T dedv = ( T ) results.get( 0 );
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
    }

}
