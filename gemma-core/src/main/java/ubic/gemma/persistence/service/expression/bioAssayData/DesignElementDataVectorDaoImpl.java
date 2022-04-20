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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.AbstractDao;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author pavlidis
 * @see    ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
public abstract class DesignElementDataVectorDaoImpl<T extends DesignElementDataVector> extends AbstractDao<T>
        implements DesignElementDataVectorDao<T> {

    DesignElementDataVectorDaoImpl( Class<T> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    @Override
    public final void removeRawAndProcessed( Collection<DesignElementDataVector> vectors ) {
        for ( DesignElementDataVector v : vectors ) {
            this.getSessionFactory().getCurrentSession().delete( v );
        }
    }

    @Override
    public final Collection<DesignElementDataVector> findRawAndProcessed( BioAssayDimension dim ) {
        return this.findRawAndProcessed( "bioAssayDimension", dim );
    }

    @Override
    public final Collection<DesignElementDataVector> findRawAndProcessed( QuantitationType qt ) {
        return this.findRawAndProcessed( "quantitationType", qt );
    }

    @Override
    public void thawRawAndProcessed( Collection<DesignElementDataVector> designElementDataVectors ) {
        if ( designElementDataVectors == null )
            return;

        Session session = this.getSessionFactory().getCurrentSession();

        Hibernate.initialize( designElementDataVectors );

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<ExpressionExperiment> ees = new HashSet<>();
        Map<BioAssayDimension, Collection<DesignElementDataVector>> dims = new HashMap<>();
        Collection<CompositeSequence> cs = new HashSet<>();
        for ( DesignElementDataVector vector : designElementDataVectors ) {
            session.buildLockRequest( LockOptions.NONE ).lock( vector );
            Hibernate.initialize( vector );
            Hibernate.initialize( vector.getQuantitationType() );

            BioAssayDimension bad = vector.getBioAssayDimension();
            if ( !dims.containsKey( bad ) ) {
                dims.put( bad, new HashSet<DesignElementDataVector>() );
            }

            dims.get( bad ).add( vector );
            cs.add( vector.getDesignElement() );
            ees.add( vector.getExpressionExperiment() );
            session.evict( vector.getQuantitationType() );
            session.evict( vector );
        }

        if ( timer.getTime() > designElementDataVectors.size() ) {
            AbstractDao.log
                    .info( "Thaw phase 1, " + designElementDataVectors.size() + " vectors initialized in " + timer
                            .getTime() + "ms " );
        }
        timer.reset();
        timer.start();

        // lightly thaw the EEs we saw
        for ( ExpressionExperiment ee : ees ) {
            Hibernate.initialize( ee );
            session.evict( ee );
        }

        if ( timer.getTime() > 200 ) {
            AbstractDao.log
                    .info( "Thaw phase 2, " + ees.size() + " vector-associated expression experiments in " + timer
                            .getTime() + "ms " );
        }

        timer.reset();
        timer.start();

        // thaw the bioassayDimensions we saw -- usually one, more rarely two.
        for ( BioAssayDimension bad : dims.keySet() ) {

            BioAssayDimension tbad = ( BioAssayDimension ) this.getSessionFactory().getCurrentSession().createQuery(
                    "select distinct bad from BioAssayDimension bad join fetch bad.bioAssays ba join fetch ba.sampleUsed "
                            + "bm join fetch ba.arrayDesignUsed left join fetch bm.factorValues fetch all properties where bad.id= :bad " )
                    .setParameter( "bad", bad.getId() ).uniqueResult();

            assert tbad != null;
            assert !dims.get( tbad ).isEmpty();

            for ( DesignElementDataVector v : designElementDataVectors ) {
                if ( v.getBioAssayDimension().getId().equals( tbad.getId() ) ) {
                    v.setBioAssayDimension( tbad );
                }
            }
        }

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Thaw phase 3, " + dims.size() + " vector-associated bioassaydimensions in " + timer
                    .getTime() + "ms " );
        }
        timer.reset();
        timer.start();

        // thaw the designelements we saw. SLOW
        long lastTime = 0;
        int count = 0;
        for ( CompositeSequence de : cs ) {
            BioSequence seq = de.getBiologicalCharacteristic();
            if ( seq == null )
                continue;
            session.buildLockRequest( LockOptions.NONE ).lock( seq );
            Hibernate.initialize( seq );

            // is this really necessary?
            ArrayDesign arrayDesign = de.getArrayDesign();
            Hibernate.initialize( arrayDesign );

            if ( ++count % 10000 == 0 ) {
                if ( timer.getTime() - lastTime > 1000 ) {
                    AbstractDao.log.info( "Thawed " + count + " vector-associated probes " + timer.getTime() + " ms" );
                }
                lastTime = timer.getTime();
            }
        }

        timer.stop();
        if ( designElementDataVectors.size() >= 2000 || timer.getTime() > 200 ) {
            AbstractDao.log.info( "Thaw phase 4 " + cs.size() + " vector-associated probes thawed in " + timer.getTime()
                    + "ms" );
        }
    }

    @Override
    public void thaw( Collection<T> designElementDataVectors ) {
        //noinspection unchecked // Doesnt matter which implementation it is
        this.thawRawAndProcessed( ( Collection<DesignElementDataVector> ) designElementDataVectors );
    }

    @Override
    public void thaw( T designElementDataVector ) {
        Session session = this.getSessionFactory().getCurrentSession();
        BioSequence seq = designElementDataVector.getDesignElement().getBiologicalCharacteristic();
        if ( seq != null ) {
            session.buildLockRequest( LockOptions.NONE ).lock( seq );
            Hibernate.initialize( seq );
        }

        ArrayDesign arrayDesign = designElementDataVector.getDesignElement().getArrayDesign();
        Hibernate.initialize( arrayDesign );

        // thaw the bioassays.
        for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
            ba = ( BioAssay ) session.get( BioAssay.class, ba.getId() );
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
            Hibernate.initialize( ba.getOriginalPlatform() );
        }
    }

    @Override
    public Collection<T> find( QuantitationType quantitationType ) {
        return new HashSet<>( this.findByProperty( "quantitationType", quantitationType ) );
    }

    @Override
    @Transactional
    public Collection<T> create( final Collection<T> entities ) {
        return super.create( entities );
    }

    @Override
    @Transactional
    public void update( final Collection<T> entities ) {
        super.update( entities );
    }

    /**
     * @param  ee      ee
     * @param  cs2gene Map of probes to genes.
     * @return         map of vectors to gene ids.
     */
    Map<T, Collection<Long>> getVectorsForProbesInExperiments( Long ee, Map<Long, Collection<Long>> cs2gene ) {

        // Do not do in clause for experiments, as it can't use the indices
        //language=HQL
        String queryString = "select dedv, dedv.designElement.id from ProcessedExpressionDataVector dedv fetch all properties"
                + " where dedv.designElement.id in ( :cs ) and dedv.expressionExperiment.id = :eeId ";

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        Map<T, Collection<Long>> dedv2genes = new HashMap<>();
        StopWatch timer = new StopWatch();
        timer.start();

        queryObject.setLong( "eeId", ee );

        int batchSize = 100;
        for ( Collection<Long> batch : new BatchIterator<>( cs2gene.keySet(), batchSize ) ) {
            this.getVectorsBatch( cs2gene, queryObject, dedv2genes, batch );
        }

        if ( timer.getTime() > Math.max( 200, 20 * dedv2genes.size() ) ) {
            AbstractDao.log
                    .info( "Fetched " + dedv2genes.size() + " vectors for " + cs2gene.size() + " probes in " + timer
                            .getTime() + "ms\n" + "Vector query was: " + queryString );

        }
        return dedv2genes;
    }

    Map<T, Collection<Long>> getVectorsForProbesInExperiments( Map<Long, Collection<Long>> cs2gene ) {

        //language=HQL
        String queryString = "select dedv, dedv.designElement.id from ProcessedExpressionDataVector dedv fetch all properties"
                + " where dedv.designElement.id in ( :cs ) ";

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        Map<T, Collection<Long>> dedv2genes = new HashMap<>();
        StopWatch timer = new StopWatch();
        timer.start();

        int batchSize = 100;
        for ( Collection<Long> batch : new BatchIterator<>( cs2gene.keySet(), batchSize ) ) {
            this.getVectorsBatch( cs2gene, queryObject, dedv2genes, batch );
        }

        if ( timer.getTime() > Math.max( 200, 20 * dedv2genes.size() ) ) {
            AbstractDao.log
                    .info( "Fetched " + dedv2genes.size() + " vectors for " + cs2gene.size() + " probes in " + timer
                            .getTime() + "ms\n" + "Vector query was: " + queryString );

        }
        return dedv2genes;
    }

    private Collection<DesignElementDataVector> findRawAndProcessed( String propName, Object value ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession()
                .createCriteria( DesignElementDataVector.class );
        criteria.add( Restrictions.eq( propName, value ) );
        //noinspection unchecked
        return criteria.list();
    }

    private void getVectorsBatch( Map<Long, Collection<Long>> cs2gene, org.hibernate.Query queryObject,
            Map<T, Collection<Long>> dedv2genes, Collection<Long> batch ) {
        queryObject.setParameterList( "cs", batch );
        queryObject.setFlushMode( FlushMode.MANUAL );
        queryObject.setReadOnly( true );
        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            @SuppressWarnings("unchecked")
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
