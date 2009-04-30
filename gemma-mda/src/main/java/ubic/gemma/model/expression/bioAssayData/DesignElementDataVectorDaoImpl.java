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
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 * @author pavlidis
 * @version $Id$
 */
public abstract class DesignElementDataVectorDaoImpl<T extends DesignElementDataVector> extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase<T> {

    private static Log log = LogFactory.getLog( DesignElementDataVectorDaoImpl.class.getName() );

    /**
     * @param ees
     * @param cs2gene Map of probes to genes.
     * @param queryString, which must have parameter list placeholder "cs" and may have parameter list "ees".
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Map<T, Collection<Gene>> getVectorsForProbesInExperiments( Collection<ExpressionExperiment> ees,
            Map<CompositeSequence, Collection<Gene>> cs2gene, final String queryString ) {
        Session session = super.getSession( false );
        org.hibernate.Query queryObject = session.createQuery( queryString );
        Map<T, Collection<Gene>> dedv2genes = new HashMap<T, Collection<Gene>>();
        StopWatch timer = new StopWatch();
        timer.start();
        try {

            if ( ees != null && ees.size() > 0 ) {
                queryObject.setParameterList( "ees", ees );
            }
            queryObject.setParameterList( "cs", cs2gene.keySet() );

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
            session.clear();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        timer.stop();
        if ( timer.getTime() > 50 ) {
            log.info( "Fetch vectors for " + cs2gene.size() + " probes in " + ( ees == null ? "(?)" : ees.size() )
                    + "ees : " + timer.getTime() + "ms" );
        }
        // this.thaw( dedv2genes.keySet() );
        return dedv2genes;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleThaw(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleThaw( final Collection<T> designElementDataVectors ) throws Exception {

        if ( designElementDataVectors == null ) return;

        Session session = this.getSessionFactory().getCurrentSession();

        FlushMode oldFlushMode = session.getFlushMode();
        CacheMode oldCacheMode = session.getCacheMode();
        session.setCacheMode( CacheMode.IGNORE ); // Don't hit the secondary cache
        session.setFlushMode( FlushMode.MANUAL ); // We're READ-ONLY so this is okay.
        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
        Collection<DesignElement> cs = new HashSet<DesignElement>();
        for ( DesignElementDataVector vector : ( Collection<DesignElementDataVector> ) designElementDataVectors ) {
            session.lock( vector, LockMode.NONE );
            Hibernate.initialize( vector );
            Hibernate.initialize( vector.getExpressionExperiment() );
            dims.add( vector.getBioAssayDimension() );
            cs.add( vector.getDesignElement() );
            ees.add( vector.getExpressionExperiment() );
            session.evict( vector.getQuantitationType() );
            session.evict( vector );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw phase 1," + designElementDataVectors.size() + " vectors in " + timer.getTime()
                    + "ms elapsed" );
        }

        // lightly thaw the EEs we saw
        for ( ExpressionExperiment ee : ees ) {
            Hibernate.initialize( ee );
            session.evict( ee );
        }

        if ( timer.getTime() > 2000 ) {
            log.info( "Thaw phase 2," + designElementDataVectors.size() + " vectors in " + timer.getTime()
                    + "ms elapsed total" );
        }

        // thaw the bioassaydimensions we saw
        for ( BioAssayDimension bad : dims ) {
            Hibernate.initialize( bad );
            for ( BioAssay ba : bad.getBioAssays() ) {
                session.lock( ba, LockMode.NONE );
                Hibernate.initialize( ba );
                Hibernate.initialize( ba.getSamplesUsed() );

                Collection<BioAssay> bioAssaysUsedIn = null;
                for ( BioMaterial bm : ba.getSamplesUsed() ) {
                    // session.lock( bm, LockMode.NONE );
                    Hibernate.initialize( bm );
                    bioAssaysUsedIn = bm.getBioAssaysUsedIn();
                    Hibernate.initialize( bioAssaysUsedIn );
                    Hibernate.initialize( bm.getFactorValues() );
                    session.evict( bm );
                }

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
                // don't do this.
                // session.evict( ba );
            }
        }

        if ( timer.getTime() > 3000 ) {
            log.info( "Thaw phase 3," + designElementDataVectors.size() + " vectors in " + timer.getTime()
                    + "ms elapsed total" );
        }

        // thaw the designelements we saw.
        int lastTime = 0;
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
                if ( timer.getTime() - lastTime > 1000 ) {
                    log.info( "Thawed " + count + " vector-associated probes " + timer.getTime() + " ms" );
                }
            }
        }

        timer.stop();
        if ( designElementDataVectors.size() >= 2000 || timer.getTime() > 20 ) {
            log.info( "Done, thawed " + designElementDataVectors.size() + " vectors in " + timer.getTime() + "ms" );
        }
        session.setFlushMode( oldFlushMode );
        session.setCacheMode( oldCacheMode );

    }

    @Override
    protected void handleThaw( final T designElementDataVector ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                thaw( session, designElementDataVector );
                return null;
            }
        } );

    }

    /**
     * Thaw a single vector.
     * 
     * @param session
     * @param designElementDataVector
     */
    private void thaw( org.hibernate.Session session, T designElementDataVector ) {
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
